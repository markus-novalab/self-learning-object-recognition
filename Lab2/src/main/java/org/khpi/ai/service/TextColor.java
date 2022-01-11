package org.khpi.ai.service;

public enum TextColor {
    RESET("\033[0m"),
    GREEN_BRIGHT("\033[0;92m"),
    YELLOW_BRIGHT("\033[0;93m"),
    RED_BRIGHT("\033[0;91m");

    private final String attr;

    TextColor(String s) {
        attr = s;
    }

    public String getAttr() {
        return attr;
    }
}
