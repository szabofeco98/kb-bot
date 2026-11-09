package com.szabofeco.kb_bot.common;

public enum Product {
    LAKASHITEL("lakáshitel"),
    SZEMELYI_KOLCSON("személyi kölcsön"),
    BANKSZAMLA("bankszámla");

    private final String displayName;

    Product(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
