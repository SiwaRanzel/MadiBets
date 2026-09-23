package com.bloodline.madibets.service;

import com.bloodline.madibets.model.NmuEvent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scrapes upcoming events from https://events.mandela.ac.za/ using Jsoup.
 *
 * The NMU events homepage renders dates via JavaScript, so Jsoup (which
 * does not execute JS) cannot read them from the listing page.
 *
 * Strategy:
 *   1. Fetch the homepage and collect all /Events/ deep links + titles.
 *   2. For each event link, fetch the detail page in parallel (up to 6 threads).
 *   3. On the detail page the date appears in static HTML as plain text
 *      after the label "Event date and time:".
 *
 * Results are cached for 5 minutes so the NMU site isn't hammered on every
 * page load, and scraping is done asynchronously so requests never block
 * when the external site is slow or unreachable.
 *
 * Call {@link #scrapeEvents()} to get a (possibly cached) list.
 */
@Service
public class EventScraperService {

    private static final Logger log = LoggerFactory.getLogger(EventScraperService.class);

    private static final String BASE_URL   = "https://events.mandela.ac.za";
    private static final String EVENTS_URL = BASE_URL + "/";

    // Matches "Event date and time: 21/09/2026 13:30:00"
    private static final Pattern DATE_PATTERN =
            Pattern.compile("Event date and time:\\s*([\\d/: ]+)", Pattern.CASE_INSENSITIVE);

    // Browser-like User-Agent to avoid 403 blocks
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
          + "AppleWebKit/537.36 (KHTML, like Gecko) "
          + "Chrome/124.0 Safari/537.36";

    /** How long cached results stay valid (5 minutes). */
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    /** Maximum time the entire scrape operation can take (30 seconds). */
    private static final long SCRAPE_TIMEOUT_SECONDS = 30;

    // ─── cache state ───────────────────────────────────────────────────────
    private volatile List<NmuEvent> cachedEvents = Collections.emptyList();
    private volatile long cacheTimestamp = 0;
    private volatile boolean refreshInProgress = false;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "event-scraper-bg");
                t.setDaemon(true);
                return t;
            });

    /** Kick off the first background scrape shortly after startup. */
    @PostConstruct
    public void init() {
        scheduler.schedule(this::backgroundRefresh, 5, TimeUnit.SECONDS);
    }

    /**
     * Returns the most recently cached events.  If the cache is stale or
     * empty, a background refresh is triggered (but this call returns
     * immediately with whatever we have).
     *
     * @return List of NmuEvent objects (may be empty, never null)
     */
    public List<NmuEvent> scrapeEvents() throws IOException {
        if (isCacheStale()) {
            triggerBackgroundRefresh();
        }
        return new ArrayList<>(cachedEvents);
    }

    // ─── background refresh ────────────────────────────────────────────────

    private boolean isCacheStale() {
        return System.currentTimeMillis() - cacheTimestamp > CACHE_TTL_MS;
    }

    private synchronized void triggerBackgroundRefresh() {
        if (refreshInProgress) return;
        refreshInProgress = true;
        scheduler.submit(this::backgroundRefresh);
    }

    private void backgroundRefresh() {
        try {
            log.info("Starting NMU events scrape from {}", EVENTS_URL);
            List<NmuEvent> fresh = doScrape();
            if (!fresh.isEmpty()) {
                cachedEvents = fresh;
                cacheTimestamp = System.currentTimeMillis();
                log.info("NMU events scrape complete – {} events cached", fresh.size());
            } else {
                log.warn("NMU events scrape returned 0 events (page structure may have changed)");
                // Still update timestamp so we don't retry every request
                cacheTimestamp = System.currentTimeMillis();
            }
        } catch (Exception e) {
            log.error("NMU events scrape failed: {}", e.getMessage());
            // Keep serving the old cache; update timestamp to avoid retry storm
            cacheTimestamp = System.currentTimeMillis();
        } finally {
            refreshInProgress = false;
        }
    }

    // ─── actual scraping logic ─────────────────────────────────────────────

    /**
     * Fetches the NMU events homepage, finds all event links, then fetches
     * each event's detail page to read its date from static HTML.
     * The whole operation is bounded by {@link #SCRAPE_TIMEOUT_SECONDS}.
     */
    private List<NmuEvent> doScrape() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            Future<List<NmuEvent>> task = pool.submit(this::doScrapeInner);
            return task.get(SCRAPE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("NMU events scrape timed out after {}s", SCRAPE_TIMEOUT_SECONDS);
            return Collections.emptyList();
        } finally {
            pool.shutdownNow();
        }
    }

    private List<NmuEvent> doScrapeInner() throws IOException {
        // ── Step 1: collect event links + names from the homepage ─────────
        Document homePage = Jsoup.connect(EVENTS_URL)
                .userAgent(USER_AGENT)
                .timeout(15_000)
                .get();

        // Collect deep event links (pattern: /Events/<category>/<slug>)
        // Skip top-level category pages like /Events/Webinars
        List<String[]> rawEvents = new ArrayList<>();
        for (Element link : homePage.select("a[href*=/Events/]")) {
            String name = link.text().trim();
            if (name.isEmpty()) continue;
            String href = link.attr("abs:href");
            if (!href.matches(".*Events/[^/?#]+/[^/?#]+.*")) continue;
            rawEvents.add(new String[]{name, href});
        }

        log.info("Found {} event links on homepage", rawEvents.size());

        if (rawEvents.isEmpty()) {
            return Collections.emptyList();
        }

        // ── Step 2: fetch detail pages in parallel to read dates ──────────
        int threads = Math.min(rawEvents.size(), 6);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<NmuEvent>> futures = new ArrayList<>();

        for (String[] entry : rawEvents) {
            final String name = entry[0];
            final String url  = entry[1];
            futures.add(executor.submit(() -> {
                String date = fetchDateFromDetailPage(url);
                return new NmuEvent(name, date, url);
            }));
        }

        executor.shutdown();

        List<NmuEvent> events = new ArrayList<>();
        for (Future<NmuEvent> f : futures) {
            try {
                events.add(f.get(20, TimeUnit.SECONDS));
            } catch (Exception e) {
                log.debug("Skipping event detail page ({})", e.getMessage());
            }
        }

        return events;
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    /**
     * Fetches an individual event detail page and extracts the date from
     * the static text label "Event date and time: DD/MM/YYYY HH:mm:ss".
     *
     * @param url absolute URL of the event detail page
     * @return date string, or "Date not listed" if not found
     */
    private String fetchDateFromDetailPage(String url) {
        try {
            Document detail = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(12_000)
                    .get();

            // Search the full visible text of the page for the date label
            String bodyText = detail.body().text();
            Matcher m = DATE_PATTERN.matcher(bodyText);
            if (m.find()) {
                return m.group(1).trim();
            }

            // Fallback: find any visible text node that looks like a timestamp
            for (Element el : detail.select("*")) {
                String own = el.ownText().trim();
                if (own.matches("\\d{2}/\\d{2}/\\d{4}.*")) {
                    return own;
                }
            }
        } catch (IOException e) {
            log.debug("Could not fetch detail page {}: {}", url, e.getMessage());
        }
        return "Date not listed";
    }
}
