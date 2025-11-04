package com.northstar.funding.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryLanguageTest {

    @Test
    void shouldHaveNineValues() {
        assertThat(QueryLanguage.values()).hasSize(9);
    }

    @Test
    void bulgarianShouldHaveCorrectCodes() {
        QueryLanguage bg = QueryLanguage.BULGARIAN;
        assertThat(bg.getLanguageCode()).isEqualTo("bg");
        assertThat(bg.getNativeName()).isEqualTo("български");
    }

    @Test
    void englishShouldHaveCorrectCodes() {
        QueryLanguage en = QueryLanguage.ENGLISH;
        assertThat(en.getLanguageCode()).isEqualTo("en");
        assertThat(en.getNativeName()).isEqualTo("English");
    }

    @Test
    void germanShouldHaveCorrectCodes() {
        QueryLanguage de = QueryLanguage.GERMAN;
        assertThat(de.getLanguageCode()).isEqualTo("de");
        assertThat(de.getNativeName()).isEqualTo("Deutsch");
    }

    @Test
    void romanianShouldHaveCorrectCodes() {
        QueryLanguage ro = QueryLanguage.ROMANIAN;
        assertThat(ro.getLanguageCode()).isEqualTo("ro");
        assertThat(ro.getNativeName()).isEqualTo("română");
    }

    @Test
    void frenchShouldHaveCorrectCodes() {
        QueryLanguage fr = QueryLanguage.FRENCH;
        assertThat(fr.getLanguageCode()).isEqualTo("fr");
        assertThat(fr.getNativeName()).isEqualTo("français");
    }

    @Test
    void russianShouldHaveCorrectCodes() {
        QueryLanguage ru = QueryLanguage.RUSSIAN;
        assertThat(ru.getLanguageCode()).isEqualTo("ru");
        assertThat(ru.getNativeName()).isEqualTo("русский");
    }

    @Test
    void greekShouldHaveCorrectCodes() {
        QueryLanguage el = QueryLanguage.GREEK;
        assertThat(el.getLanguageCode()).isEqualTo("el");
        assertThat(el.getNativeName()).isEqualTo("ελληνικά");
    }

    @Test
    void turkishShouldHaveCorrectCodes() {
        QueryLanguage tr = QueryLanguage.TURKISH;
        assertThat(tr.getLanguageCode()).isEqualTo("tr");
        assertThat(tr.getNativeName()).isEqualTo("Türkçe");
    }

    @Test
    void serbianShouldHaveCorrectCodes() {
        QueryLanguage sr = QueryLanguage.SERBIAN;
        assertThat(sr.getLanguageCode()).isEqualTo("sr");
        assertThat(sr.getNativeName()).isEqualTo("српски");
    }

    @Test
    void allLanguageCodesShouldBeTwoCharacters() {
        for (QueryLanguage lang : QueryLanguage.values()) {
            assertThat(lang.getLanguageCode())
                .hasSize(2)
                .isLowerCase();
        }
    }
}
