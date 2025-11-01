package com.northstar.funding.discovery.infrastructure.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Spring Data JDBC converters for Set<String> ↔ PostgreSQL TEXT[]
 *
 * Handles bidirectional conversion between Java Set<String> and PostgreSQL TEXT[] column.
 * Engine types are stored as TEXT[] array: {"SEARXNG", "TAVILY", "PERPLEXITY"}
 *
 * @author NorthStar Funding Team
 */
public class SearchEngineTypeSetConverter {

    /**
     * Converts PostgreSQL TEXT[] to Set<String>
     */
    @ReadingConverter
    public static class SearchEngineTypeSetReadingConverter implements Converter<String[], Set<String>> {
        @Override
        public Set<String> convert(String[] source) {
            if (source == null || source.length == 0) {
                return new HashSet<>();
            }

            return new HashSet<>(Arrays.asList(source));
        }
    }

    /**
     * Converts Set<String> to PostgreSQL TEXT[]
     */
    @WritingConverter
    public static class SearchEngineTypeSetWritingConverter implements Converter<Set<String>, String[]> {
        @Override
        public String[] convert(Set<String> source) {
            if (source == null || source.isEmpty()) {
                return new String[0];
            }

            return source.toArray(new String[0]);
        }
    }
}
