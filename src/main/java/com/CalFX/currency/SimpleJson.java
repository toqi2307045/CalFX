package com.CalFX.currency;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small, dependency-free JSON reader.
 * Only what ExchangeRateService needs: objects, arrays, strings, numbers, booleans, null.
 * Numbers are always returned as Double, objects as Map&lt;String,Object&gt;, arrays as List&lt;Object&gt;.
 */
public final class SimpleJson {

    private SimpleJson() {
    }

    public static Object parse(String text) {
        Parser parser = new Parser(text);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new IllegalArgumentException("Unexpected trailing content in JSON");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object value = parse(text);
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("Expected a JSON object at the top level");
        }
        return (Map<String, Object>) value;
    }

    /** One Parser per call: it holds only its own position, so it is safe to use from any thread. */
    private static final class Parser {

        private final String text;
        private int pos;

        Parser(String text) {
            this.text = text;
        }

        Object parseValue() {
            skipWhitespace();
            char c = peek();
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't', 'f' -> parseBoolean();
                case 'n' -> parseNull();
                default -> parseNumber();
            };
        }

        Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                map.put(key, parseValue());
                skipWhitespace();
                char c = consume();
                if (c == '}') {
                    return map;
                }
                if (c != ',') {
                    throw new IllegalArgumentException("Expected ',' or '}' at position " + pos);
                }
            }
        }

        List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                char c = consume();
                if (c == ']') {
                    return list;
                }
                if (c != ',') {
                    throw new IllegalArgumentException("Expected ',' or ']' at position " + pos);
                }
            }
        }

        String parseString() {
            skipWhitespace();
            expect('"');
            StringBuilder result = new StringBuilder();
            while (true) {
                char c = consume();
                if (c == '"') {
                    return result.toString();
                }
                if (c == '\\') {
                    char escaped = consume();
                    switch (escaped) {
                        case '"' -> result.append('"');
                        case '\\' -> result.append('\\');
                        case '/' -> result.append('/');
                        case 'n' -> result.append('\n');
                        case 't' -> result.append('\t');
                        case 'r' -> result.append('\r');
                        case 'b' -> result.append('\b');
                        case 'f' -> result.append('\f');
                        case 'u' -> {
                            result.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                            pos += 4;
                        }
                        default -> throw new IllegalArgumentException("Invalid escape at position " + pos);
                    }
                } else {
                    result.append(c);
                }
            }
        }

        Double parseNumber() {
            int start = pos;
            if (peek() == '-') {
                pos++;
            }
            while (!atEnd() && isNumberChar(text.charAt(pos))) {
                pos++;
            }
            String token = text.substring(start, pos);
            if (token.isEmpty() || token.equals("-")) {
                throw new IllegalArgumentException("Invalid number at position " + start);
            }
            return Double.parseDouble(token);
        }

        Boolean parseBoolean() {
            if (text.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (text.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Invalid literal at position " + pos);
        }

        Object parseNull() {
            if (text.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new IllegalArgumentException("Invalid literal at position " + pos);
        }

        void skipWhitespace() {
            while (!atEnd() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
        }

        boolean atEnd() {
            return pos >= text.length();
        }

        char peek() {
            if (atEnd()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            return text.charAt(pos);
        }

        char consume() {
            char c = peek();
            pos++;
            return c;
        }

        void expect(char expected) {
            char c = consume();
            if (c != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at position " + (pos - 1));
            }
        }

        private static boolean isNumberChar(char c) {
            return Character.isDigit(c) || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-';
        }
    }
}
