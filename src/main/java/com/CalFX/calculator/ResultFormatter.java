package com.CalFX.calculator;

import java.math.BigDecimal;
import java.math.MathContext;

/** Formats a double for display: at most 12 significant digits, no trailing zeros. */
public final class ResultFormatter {

    private static final MathContext PRECISION = new MathContext(12);

    private ResultFormatter() {
    }

    public static String format(double value) {
        double abs = Math.abs(value);
        if (abs < 1e-12) {
            return "0"; // hides floating-point noise such as sin(180) = 1.2e-16
        }
        BigDecimal rounded = new BigDecimal(value).round(PRECISION).stripTrailingZeros();
        if (abs >= 1e12 || abs < 1e-6) {
            return rounded.toString().replace("E+", "E");
        }
        return rounded.toPlainString();
    }
}
