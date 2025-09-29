package com.northstar.funding.discovery.infrastructure.converters;

import com.northstar.funding.discovery.domain.AdminRole;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

/**
 * Spring Data JDBC converters for AdminRole enum mapping to PostgreSQL varchar type
 */
public class AdminRoleConverter {

    @WritingConverter
    public static class AdminRoleWritingConverter implements Converter<AdminRole, String> {
        @Override
        public String convert(AdminRole source) {
            return source != null ? source.name() : null;
        }
    }

    @ReadingConverter
    public static class AdminRoleReadingConverter implements Converter<String, AdminRole> {
        @Override
        public AdminRole convert(String source) {
            return source != null ? AdminRole.valueOf(source.toUpperCase()) : null;
        }
    }
}
