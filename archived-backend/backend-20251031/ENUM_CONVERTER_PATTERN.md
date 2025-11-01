# Spring Data JDBC Enum Converter Pattern

## The Problem

PostgreSQL stores enums as VARCHAR with CHECK constraints. Spring Data JDBC needs explicit converters to map Java enums to PostgreSQL VARCHAR and back.

## The Solution Pattern

Follow this exact pattern for every enum used in domain classes:

### Step 1: Create Enum Converter Class

```java
package com.northstar.funding.discovery.infrastructure.converters;

import com.northstar.funding.discovery.domain.YourEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

/**
 * Spring Data JDBC converters for YourEnum enum mapping to PostgreSQL varchar type
 */
public class YourEnumConverter {

    @WritingConverter
    public static class YourEnumWritingConverter implements Converter<YourEnum, String> {
        @Override
        public String convert(YourEnum source) {
            return source != null ? source.name() : null;
        }
    }

    @ReadingConverter
    public static class YourEnumReadingConverter implements Converter<String, YourEnum> {
        @Override
        public YourEnum convert(String source) {
            return source != null ? YourEnum.valueOf(source.toUpperCase()) : null;
        }
    }
}
```

### Step 2: Register Converter in JdbcConfiguration

```java
package com.northstar.funding.discovery.infrastructure.config;

import com.northstar.funding.discovery.infrastructure.converters.YourEnumConverter;
// ... other imports

@Configuration
@EnableJdbcRepositories(basePackages = "com.northstar.funding.discovery.infrastructure")
public class JdbcConfiguration extends AbstractJdbcConfiguration {

    @Override
    public JdbcCustomConversions jdbcCustomConversions() {
        return new JdbcCustomConversions(Arrays.asList(
            // Add your converters here
            new YourEnumConverter.YourEnumReadingConverter(),
            new YourEnumConverter.YourEnumWritingConverter()
        ));
    }
}
```

### Step 3: Use Enum in Domain Class

```java
@Table("your_table")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YourDomainClass {
    
    @Id
    private UUID id;
    
    private YourEnum status; // No special annotation needed!
    
    // ... other fields
}
```

### Step 4: Test in Integration Test

```java
@Test
@DisplayName("Should handle enum values as VARCHAR with CHECK constraints")
void shouldHandleEnumValuesAsVarchar() {
    // When: Creating entity with all possible enum values
    var entity = YourDomainClass.builder()
        .status(YourEnum.VALUE1)
        .build();
        
    var saved = repository.save(entity);
    var retrieved = repository.findById(saved.getId()).orElseThrow();
    
    // Then: Enum values should be preserved correctly
    assertThat(retrieved.getStatus()).isEqualTo(YourEnum.VALUE1);
}
```

## Example: CandidateStatus Converter

### File: `CandidateStatusConverter.java`
```java
package com.northstar.funding.discovery.infrastructure.converters;

import com.northstar.funding.discovery.domain.CandidateStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

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
```

### Registration in `JdbcConfiguration.java`
```java
@Override
public JdbcCustomConversions jdbcCustomConversions() {
    return new JdbcCustomConversions(Arrays.asList(
        // AdminRole enum converters
        new AdminRoleConverter.AdminRoleReadingConverter(),
        new AdminRoleConverter.AdminRoleWritingConverter(),
        // CandidateStatus enum converters
        new CandidateStatusConverter.CandidateStatusReadingConverter(),
        new CandidateStatusConverter.CandidateStatusWritingConverter()
    ));
}
```

## When Do You Need This?

### ✅ ALWAYS CREATE CONVERTERS FOR:
- Any enum used in domain classes mapped to PostgreSQL VARCHAR
- Ensures explicit, predictable behavior
- Provides better error messages
- Makes the mapping intention clear

### ⚠️ MAY WORK WITHOUT CONVERTERS:
- Spring Data JDBC has default enum handling
- But it can be inconsistent across versions
- Better to be explicit!

## Troubleshooting

### Error: "Failed to convert from type [java.lang.String] to type [YourEnum]"
**Solution**: Create converter following the pattern above and register it in JdbcConfiguration

### Error: "No converter found capable of converting from type [YourEnum] to type [java.lang.String]"
**Solution**: Missing WritingConverter - add both reading AND writing converters

### Error: Enum values saved as NULL or incorrect values
**Solution**: Check that:
1. Converter uses `source.name()` not `source.toString()`
2. Converter is registered in JdbcConfiguration
3. Database column is VARCHAR not enum type

## Quick Checklist

- [ ] Create converter class in `converters` package
- [ ] Implement both ReadingConverter and WritingConverter
- [ ] Use `source.name()` for writing, `YourEnum.valueOf(source.toUpperCase())` for reading
- [ ] Handle null values (return null if source is null)
- [ ] Register both converters in JdbcConfiguration
- [ ] Test enum persistence in integration test

## All Current Converters

1. ✅ `AdminRoleConverter` - AdminRole enum
2. ✅ `CandidateStatusConverter` - CandidateStatus enum
3. ✅ `SpecializationsConverter` - Set<String> ↔ TEXT[] (for AdminUser.specializations)

## Enums That May Need Converters (If Issues Arise)

- ContactType
- AuthorityLevel
- SessionType
- SessionStatus
- EnhancementType

## Database Side: PostgreSQL CHECK Constraint

The database enforces valid enum values with CHECK constraints:

```sql
CREATE TABLE your_table (
    id UUID PRIMARY KEY,
    status VARCHAR(50) NOT NULL CHECK (status IN ('VALUE1', 'VALUE2', 'VALUE3')),
    -- ... other columns
);
```

This ensures:
1. Only valid enum values are stored
2. Database-level validation
3. Clear error messages for invalid values

## Best Practices

1. **Always use `.name()` not `.toString()`** - More explicit and reliable
2. **Handle nulls** - Both reading and writing converters should handle null
3. **Use toUpperCase()** - Makes reading converter case-insensitive
4. **Register immediately** - Add to JdbcConfiguration as soon as converter is created
5. **Test thoroughly** - Include enum testing in integration tests

## Final Notes

This pattern has proven to work across:
- AdminUser with AdminRole enum ✅
- FundingSourceCandidate with CandidateStatus enum ✅
- All integration tests pass with this approach ✅

**When in doubt, create the converter!** It's better to be explicit than to rely on default behavior.
