package com.mathx.calculator;

/**
 * Turns the text typed by the user into a number.
 * The UI only depends on this interface, so the real parser can replace the placeholder later.
 */
public interface ExpressionEvaluator {

    /**
     * @param expression the text to evaluate, for example "2x + sin(30)"
     * @param x          value used for the variable x (graphing)
     * @param angleMode  whether trig functions work in degrees or radians
     * @throws com.mathx.exception.ExpressionException if the syntax is invalid
     */
    double evaluate(String expression, double x, AngleMode angleMode);
}
