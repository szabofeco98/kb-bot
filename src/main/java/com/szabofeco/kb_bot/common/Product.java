package com.szabofeco.kb_bot.common;

/**
 * Products managed in the knowledge base. The "product" metadata of documents and chunks comes from this set.
 */
public enum Product {
    LAKASHITEL("lakáshitel"),
    SZEMELYI_KOLCSON("személyi kölcsön"),
    BANKSZAMLA("bankszámla");

    private final String displayName;

    Product(String displayName) {
        this.displayName = displayName;
    }

    /** Human-readable (Hungarian) name, e.g. for prompts: the answer should say "lakáshitel", not "LAKASHITEL". */
    public String displayName() {
        return displayName;
    }
}
