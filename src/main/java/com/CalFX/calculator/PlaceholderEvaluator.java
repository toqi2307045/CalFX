package com.CalFX.calculator;

import com.CalFX.exception.ExpressionException;

import java.util.Set;

/**
 * TEMPORARY evaluator so the interface can be run and tested today.
 * Phase 3 (lexer -> parser -> expression tree) replaces this class; only the
 * ExpressionEvaluator interface is used by the UI, so nothing else has to change.
 *
 * Supports: + - * / % ^ !, parentheses, implicit multiplication (2x, 3(4+1)),
 * x, pi, e, sin cos tan asin acos atan log ln sqrt abs exp.
 * The symbols used on the keypad (multiply, divide, minus, pi, square root) are accepted too.
 */
public class PlaceholderEvaluator implements ExpressionEvaluator {

    private static final char MINUS_SIGN = '\u2212';
    private static final char MULTIPLY_SIGN = '\u00D7';
    private static final char DIVIDE_SIGN = '\u00F7';
    private static final char PI_SIGN = '\u03C0';
    private static final char SQRT_SIGN = '\u221A';

    private static final Set<String> FUNCTIONS =
            Set.of("sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "sqrt", "abs", "exp");

    @Override
    public double evaluate(String expression, double x, AngleMode angleMode) {
        return new Parser(expression, x, angleMode).parse();
    }

    /** One Parser instance per evaluation, so no shared mutable state. */
    private static final class Parser {

        private final String text;
        private final double x;
        private final AngleMode mode;
        private int pos;

        Parser(String text, double x, AngleMode mode) {
            this.text = text;
            this.x = x;
            this.mode = mode;
        }

        double parse() {
            if (text.isBlank()) {
                throw new ExpressionException("Empty expression");
            }
            double value = expression();
            skipSpaces();
            if (pos < text.length()) {
                throw new ExpressionException("Unexpected '" + text.charAt(pos) + "'");
            }
            return value;
        }

        // expression := term (('+' | '-') term)*
        private double expression() {
            double value = term();
            while (true) {
                if (match('+')) {
                    value += term();
                } else if (match('-') || match(MINUS_SIGN)) {
                    value -= term();
                } else {
                    return value;
                }
            }
        }

        // term := unary (('*' | '/' | '%' | implicit) unary)*
        private double term() {
            double value = unary();
            while (true) {
                if (match('*') || match(MULTIPLY_SIGN)) {
                    value *= unary();
                } else if (match('/') || match(DIVIDE_SIGN)) {
                    value /= unary();
                } else if (match('%')) {
                    value %= unary();
                } else if (startsImplicitFactor()) {
                    value *= unary();
                } else {
                    return value;
                }
            }
        }

        // unary := ('-' | '+') unary | power
        private double unary() {
            if (match('-') || match(MINUS_SIGN)) {
                return -unary();
            }
            if (match('+')) {
                return unary();
            }
            return power();
        }

        // power := postfix ('^' unary)?     (right-associative, so 2^3^2 = 2^9)
        private double power() {
            double base = postfix();
            if (match('^')) {
                return Math.pow(base, unary());
            }
            return base;
        }

        // postfix := primary '!'*
        private double postfix() {
            double value = primary();
            while (match('!')) {
                value = factorial(value);
            }
            return value;
        }

        private double primary() {
            skipSpaces();
            if (pos >= text.length()) {
                throw new ExpressionException("Unexpected end of expression");
            }
            char c = text.charAt(pos);
            if (Character.isDigit(c) || c == '.') {
                return number();
            }
            if (c == '(') {
                pos++;
                double value = expression();
                closeParenthesis();
                return value;
            }
            if (c == PI_SIGN) {
                pos++;
                return Math.PI;
            }
            if (c == SQRT_SIGN) {
                pos++;
                return apply("sqrt", argument());
            }
            if (Character.isLetter(c)) {
                return identifier();
            }
            throw new ExpressionException("Unexpected '" + c + "'");
        }

        private double number() {
            int start = pos;
            while (pos < text.length()
                    && (Character.isDigit(text.charAt(pos)) || text.charAt(pos) == '.')) {
                pos++;
            }
            try {
                return Double.parseDouble(text.substring(start, pos));
            } catch (NumberFormatException e) {
                throw new ExpressionException("Invalid number '" + text.substring(start, pos) + "'");
            }
        }

        private double identifier() {
            int start = pos;
            while (pos < text.length()
                    && Character.isLetter(text.charAt(pos))
                    && text.charAt(pos) != PI_SIGN) {
                pos++;
            }
            String name = text.substring(start, pos);
            return switch (name) {
                case "x" -> x;
                case "e" -> Math.E;
                case "pi" -> Math.PI;
                default -> functionCall(name);
            };
        }

        private double functionCall(String name) {
            if (!FUNCTIONS.contains(name)) {
                throw new ExpressionException("Unknown name '" + name + "'");
            }
            return apply(name, argument());
        }

        private double argument() {
            if (!match('(')) {
                throw new ExpressionException("Expected '('");
            }
            double value = expression();
            closeParenthesis();
            return value;
        }

        /** A missing ')' at the very end is tolerated, so "sin(30" still previews while typing. */
        private void closeParenthesis() {
            skipSpaces();
            if (pos >= text.length()) {
                return;
            }
            if (!match(')')) {
                throw new ExpressionException("Expected ')'");
            }
        }

        private double apply(String name, double v) {
            return switch (name) {
                case "sin" -> Math.sin(toRadians(v));
                case "cos" -> Math.cos(toRadians(v));
                case "tan" -> Math.tan(toRadians(v));
                case "asin" -> fromRadians(Math.asin(v));
                case "acos" -> fromRadians(Math.acos(v));
                case "atan" -> fromRadians(Math.atan(v));
                case "log" -> Math.log10(v);
                case "ln" -> Math.log(v);
                case "sqrt" -> Math.sqrt(v);
                case "abs" -> Math.abs(v);
                case "exp" -> Math.exp(v);
                default -> throw new ExpressionException("Unknown function '" + name + "'");
            };
        }

        private double toRadians(double value) {
            return mode == AngleMode.DEGREES ? Math.toRadians(value) : value;
        }

        private double fromRadians(double value) {
            return mode == AngleMode.DEGREES ? Math.toDegrees(value) : value;
        }

        private static double factorial(double n) {
            if (n < 0 || n != Math.floor(n) || n > 170) {
                return Double.NaN;
            }
            double result = 1;
            for (int i = 2; i <= (int) n; i++) {
                result *= i;
            }
            return result;
        }

        private boolean startsImplicitFactor() {
            skipSpaces();
            if (pos >= text.length()) {
                return false;
            }
            char c = text.charAt(pos);
            return c == '(' || c == SQRT_SIGN || Character.isLetter(c);
        }

        private boolean match(char expected) {
            skipSpaces();
            if (pos < text.length() && text.charAt(pos) == expected) {
                pos++;
                return true;
            }
            return false;
        }

        private void skipSpaces() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
        }
    }
}
