package com.northstar.funding.discovery.infrastructure.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Spring Data JDBC converters for Set<String> specializations field.
 * Handles conversion between PostgreSQL JSONB and Java Set<String>.
 */
public class SpecializationsConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Reads JSONB from PostgreSQL and converts to Set<String>
     */
    @Component
    @ReadingConverter
    public static class SpecializationsReadingConverter implements Converter<String, Set<String>> {
        @Override
        public Set<String> convert(String source) {
            if (source == null || source.isEmpty() || "[]".equals(source)) {
                return new HashSet<>();
            }
            try {
                return objectMapper.readValue(source, new TypeReference<Set<String>>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to convert JSONB to Set<String>: " + source, e);
            }
        }
    }

    /**
     * Writes Set<String> to PostgreSQL JSONB
     */
    @Component
    @WritingConverter
    public static class SpecializationsWritingConverter implements Converter<Set<String>, String> {
        @Override
        public String convert(Set<String> source) {
            if (source == null || source.isEmpty()) {
                return "[]";
            }
            try {
                return objectMapper.writeValueAsString(source);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to convert Set<String> to JSONB", e);
            }
        }
    }
}
