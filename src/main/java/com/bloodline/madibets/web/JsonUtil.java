package com.bloodline.madibets.web;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Owner: Kieran (B-series). Minimal JSON support for the API handlers — kept
 * dependency-free on purpose: the team has not agreed to add libraries to the
 * shared pom.xml, and the JDK ships no JSON parser.
 *
 * Scope is deliberately tiny: request bodies are FLAT objects of strings,
 * numbers, booleans and nulls (all the B-series endpoints need). Nested
 * objects/arrays in requests are rejected. Responses may nest (maps/lists).
 */
public final class JsonUtil {

    private JsonUtil() {}

    /* ── writing ─────────────────────────────────────────────── */

    /** Serializes maps, iterables, strings, numbers, booleans, null; anything
     *  else (LocalDateTime etc.) becomes its quoted toString — ISO-8601 for dates. */
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(value, sb);
        return sb.toString();
    }

    private static void writeValue(Object v, StringBuilder sb) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String s) {
            writeString(s, sb);
        } else if (v instanceof Number || v instanceof Boolean) {
            sb.append(v);
        } else if (v instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeString(String.valueOf(e.getKey()), sb);
                sb.append(':');
                writeValue(e.getValue(), sb);
            }
            sb.append('}');
        } else if (v instanceof Iterable<?> it) {
            sb.append('[');
            boolean first = true;
            for (Object o : it) {
                if (!first) sb.append(',');
                first = false;
                writeValue(o, sb);
            }
            sb.append(']');
        } else {
            writeString(v.toString(), sb);
        }
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    /* ── parsing (flat objects only) ─────────────────────────── */

    /** Parses one flat JSON object. Values become String, BigDecimal, Boolean or null. */
    public static Map<String, Object> parseObject(String json) {
        Parser p = new Parser(json);
        p.skipWs();
        p.expect('{');
        Map<String, Object> map = new LinkedHashMap<>();
        p.skipWs();
        if (p.peek() == '}') {
            return map;
        }
        while (true) {
            p.skipWs();
            String key = p.readString();
            p.skipWs();
            p.expect(':');
            p.skipWs();
            map.put(key, p.readValue());
            p.skipWs();
            char c = p.next();
            if (c == ',') continue;
            if (c == '}') return map;
            throw p.malformed();
        }
    }

    /* ── typed accessors used by the handlers ────────────────── */

    public static int requireInt(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v instanceof BigDecimal d) {
            try {
                return d.intValueExact();
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("'" + key + "' must be a whole number.");
            }
        }
        throw new IllegalArgumentException("Missing or invalid '" + key + "'.");
    }

    /** Like requireInt, but a missing key or JSON null returns null. */
    public static Integer optInt(Map<String, Object> body, String key) {
        return body.get(key) == null ? null : requireInt(body, key);
    }

    public static BigDecimal requireDecimal(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v instanceof BigDecimal d) return d;
        throw new IllegalArgumentException("Missing or invalid '" + key + "'.");
    }

    public static String requireString(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v instanceof String s) return s;
        throw new IllegalArgumentException("Missing or invalid '" + key + "'.");
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) { this.s = s; }

        void skipWs()  { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        char peek()    { if (i >= s.length()) throw malformed(); return s.charAt(i); }
        char next()    { if (i >= s.length()) throw malformed(); return s.charAt(i++); }
        void expect(char c) { if (next() != c) throw malformed(); }

        IllegalArgumentException malformed() {
            return new IllegalArgumentException("Malformed JSON request body.");
        }

        String readString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') return sb.toString();
                if (c != '\\') { sb.append(c); continue; }
                char esc = next();
                switch (esc) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/'  -> sb.append('/');
                    case 'b'  -> sb.append('\b');
                    case 'f'  -> sb.append('\f');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    case 'u'  -> {
                        if (i + 4 > s.length()) throw malformed();
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> throw malformed();
                }
            }
        }

        Object readValue() {
            char c = peek();
            if (c == '"') return readString();
            if (c == 't') { literal("true");  return Boolean.TRUE; }
            if (c == 'f') { literal("false"); return Boolean.FALSE; }
            if (c == 'n') { literal("null");  return null; }
            if (c == '-' || Character.isDigit(c)) {
                int start = i;
                while (i < s.length() && "+-.eE0123456789".indexOf(s.charAt(i)) >= 0) i++;
                try {
                    return new BigDecimal(s.substring(start, i));
                } catch (NumberFormatException e) {
                    throw malformed();
                }
            }
            throw new IllegalArgumentException("Nested JSON structures are not supported.");
        }

        void literal(String lit) {
            if (!s.startsWith(lit, i)) throw malformed();
            i += lit.length();
        }
    }
}
