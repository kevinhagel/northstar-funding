package com.northstar.funding.discovery.infrastructure.converters;

import com.northstar.funding.discovery.domain.CandidateStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

/**
 * Spring Data JDBC converters for CandidateStatus enum mapping to PostgreSQL varchar type
 */
public class CandidateStatusConverter {

    @WritingConverter
    public static class CandidateStatusWritingConverter implements Converter<CandidateStatus, String> {
        @Override
        public String convert(CandidateStatus source) {
            return source != null ? source.name() : null;
        }
    }

    @ReadingConverter
    public static class CandidateStatusReadingConverter implements Converter<String, CandidateStatus> {
        @Override
        public CandidateStatus convert(String source) {
            return source != null ? CandidateStatus.valueOf(source.toUpperCase()) : null;
        }
    }
}
