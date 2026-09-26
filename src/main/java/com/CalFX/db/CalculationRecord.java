package com.CalFX.db;

/**
 * One saved row of history: a calculator evaluation or a currency conversion.
 * Plain getters only (no JavaFX properties) - the TableView reads them by reflection
 * through PropertyValueFactory, and the list is loaded fresh each time, so nothing needs
 * to be observable.
 */
public class CalculationRecord {

    private final long id;
    private final String type;
    private final String expression;
    private final String result;
    private final String timestamp;

    public CalculationRecord(long id, String type, String expression, String result, String timestamp) {
        this.id = id;
        this.type = type;
        this.expression = expression;
        this.result = result;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getExpression() {
        return expression;
    }

    public String getResult() {
        return result;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
