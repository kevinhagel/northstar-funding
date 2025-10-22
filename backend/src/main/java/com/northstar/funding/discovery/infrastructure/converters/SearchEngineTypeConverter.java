package com.northstar.funding.discovery.infrastructure.converters;

import com.northstar.funding.discovery.search.domain.SearchEngineType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

/**
 * Spring Data JDBC converters for SearchEngineType enum
 *
 * Handles bidirectional conversion between PostgreSQL VARCHAR and Java enum.
 * Spring Data JDBC stores enums as VARCHAR by default, these converters ensure proper mapping.
 *
 * @author NorthStar Funding Team
 */
public class SearchEngineTypeConverter {

    /**
     * Converts database VARCHAR to SearchEngineType enum
     */
    @ReadingConverter
    public static class SearchEngineTypeReadingConverter implements Converter<String, SearchEngineType> {
        @Override
        public SearchEngineType convert(String source) {
            if (source == null || source.isBlank()) {
                return null;
            }
            return SearchEngineType.valueOf(source.toUpperCase());
        }
    }

    /**
     * Converts SearchEngineType enum to database VARCHAR
     */
    @WritingConverter
    public static class SearchEngineTypeWritingConverter implements Converter<SearchEngineType, String> {
        @Override
        public String convert(SearchEngineType source) {
            return source != null ? source.name() : null;
        }
    }
}
