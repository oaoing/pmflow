package com.oaoing.pmflow.common.domain;

public enum Unit {
    DAY("일"),
    WEEK("주"),
    MONTH("월"),
    YEAR( "년");

    private final String label;
    public String getLabel() {
        return label;
    }

    Unit(String label) {
        this.label = label;
    }
}