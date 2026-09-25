package com.mathx.currency;

/** The currencies offered in the two dropdowns, with a short human-readable name for display. */
public enum CurrencyCode {

    USD("US Dollar"),
    EUR("Euro"),
    GBP("British Pound"),
    BDT("Bangladeshi Taka"),
    INR("Indian Rupee"),
    JPY("Japanese Yen"),
    CNY("Chinese Yuan"),
    AUD("Australian Dollar"),
    CAD("Canadian Dollar"),
    CHF("Swiss Franc"),
    SGD("Singapore Dollar"),
    AED("UAE Dirham"),
    SAR("Saudi Riyal"),
    MYR("Malaysian Ringgit"),
    THB("Thai Baht"),
    KRW("South Korean Won"),
    RUB("Russian Ruble"),
    BRL("Brazilian Real"),
    ZAR("South African Rand"),
    NZD("New Zealand Dollar");

    private final String displayName;

    CurrencyCode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Shown in the ComboBox, e.g. "USD \u2013 US Dollar". */
    @Override
    public String toString() {
        return name() + " \u2013 " + displayName;
    }
}
