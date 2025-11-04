package com.northstar.funding.domain;

/**
 * Defines languages for query generation (future translation support).
 *
 * This enum represents languages supported for generating funding search queries.
 * Each language includes ISO 639-1 code and native name for future translation features.
 *
 * NOTE: Translation service NOT implemented yet - enum structure only.
 */
public enum QueryLanguage {

    BULGARIAN("bg", "български"),
    ENGLISH("en", "English"),
    GERMAN("de", "Deutsch"),
    ROMANIAN("ro", "română"),
    FRENCH("fr", "français"),
    RUSSIAN("ru", "русский"),
    GREEK("el", "ελληνικά"),
    TURKISH("tr", "Türkçe"),
    SERBIAN("sr", "српски");

    private final String languageCode;    // ISO 639-1 code
    private final String nativeName;      // Display name in native script

    QueryLanguage(String languageCode, String nativeName) {
        this.languageCode = languageCode;
        this.nativeName = nativeName;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getNativeName() {
        return nativeName;
    }
}
