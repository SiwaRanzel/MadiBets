package com.bloodline.madibets.controller;

import com.bloodline.madibets.model.NmuEvent;
import com.bloodline.madibets.service.EventScraperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST controller exposing scraped NMU event data.
 *
 * GET /api/events          → returns JSON list of {name, date, url}
 * GET /api/events/print    → prints events to console and returns them
 */
@RestController
@RequestMapping("/api/events")
public class EventScraperController {

    private final EventScraperService scraperService;

    public EventScraperController(EventScraperService scraperService) {
        this.scraperService = scraperService;
    }

    /**
     * Scrapes https://events.mandela.ac.za/ and returns all found events as JSON.
     *
     * Example response:
     * [
     *   { "name": "Digital Dome Festival", "date": "21/08/2026 00:00:00", "url": "https://..." },
     *   ...
     * ]
     */
    @GetMapping
    public ResponseEntity<?> getEvents() {
        try {
            List<NmuEvent> events = scraperService.scrapeEvents();
            if (events.isEmpty()) {
                return ResponseEntity.ok(
                    Map.of("message", "No events found – the page structure may have changed.",
                           "events", events));
            }
            return ResponseEntity.ok(events);
        } catch (IOException e) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Could not reach events.mandela.ac.za: " + e.getMessage()));
        }
    }

    /**
     * Same as GET /api/events but also pretty-prints each event to the server console.
     * Useful for quick manual testing.
     */
    @GetMapping("/print")
    public ResponseEntity<?> printEvents() {
        try {
            List<NmuEvent> events = scraperService.scrapeEvents();

            System.out.println("==========================================================");
            System.out.println("  NMU EVENTS – scraped from https://events.mandela.ac.za");
            System.out.println("==========================================================");
            if (events.isEmpty()) {
                System.out.println("  (no events found)");
            } else {
                int i = 1;
                for (NmuEvent e : events) {
                    System.out.printf("  %d. %-50s | %s%n", i++, e.getName(), e.getDate());
                }
            }
            System.out.println("==========================================================");

            return ResponseEntity.ok(events);
        } catch (IOException ex) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Could not reach events.mandela.ac.za: " + ex.getMessage()));
        }
    }
}
