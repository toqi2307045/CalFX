package com.CalFX.calculator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds the text the user is typing on the calculator.
 * Contains only text-editing rules, no JavaFX code.
 */
public class ExpressionBuffer {

    /** A token starting with one of these continues a displayed result (Ans op ...). */
    private static final String CONTINUATION_CHARS = "+\u2212\u00D7\u00F7^!%)";

    /** "sin(", "log(" or the square-root sign followed by "(" at the very end of the text. */
    private static final Pattern TRAILING_FUNCTION = Pattern.compile("([a-z]+|\u221A)\\($");

    private final StringBuilder text = new StringBuilder();
    private boolean showingResult;

    public void append(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        if (showingResult) {
            boolean continuesResult = CONTINUATION_CHARS.indexOf(token.charAt(0)) >= 0;
            if (!continuesResult) {
                text.setLength(0);
            }
            showingResult = false;
        }
        text.append(token);
    }

    /** Removes the last character, or a whole "sin(" style function name in one step. */
    public void backspace() {
        showingResult = false;
        String current = text.toString();
        int cut = Math.min(1, current.length());
        Matcher matcher = TRAILING_FUNCTION.matcher(current);
        if (matcher.find()) {
            cut = matcher.group().length();
        }
        text.setLength(current.length() - cut);
    }

    public void clear() {
        text.setLength(0);
        showingResult = false;
    }

    /** Replaces the text with a computed result so that the next key can continue from it. */
    public void showResult(String result) {
        text.setLength(0);
        text.append(result);
        showingResult = true;
    }

    public boolean isShowingResult() {
        return showingResult;
    }

    public boolean isEmpty() {
        return text.length() == 0;
    }

    public String text() {
        return text.toString();
    }
}
