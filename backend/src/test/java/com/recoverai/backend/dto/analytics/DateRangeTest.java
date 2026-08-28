package com.recoverai.backend.dto.analytics;

import com.recoverai.backend.exception.InvalidDateRangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateRangeTest {

    @Test
    @DisplayName("DateRange.fromStrings with null or empty defaults to last 30 days")
    void testFromStringsDefault() {
        DateRange range = DateRange.fromStrings(null, null);
        assertNotNull(range.from());
        assertNotNull(range.to());
        assertTrue(range.from().isBefore(range.to()));
        long days = ChronoUnit.DAYS.between(range.from(), range.to());
        assertTrue(days >= 29 && days <= 31);
    }

    @Test
    @DisplayName("DateRange.fromStrings parses ISO-8601 LocalDate strings correctly")
    void testFromStringsLocalDate() {
        DateRange range = DateRange.fromStrings("2026-08-01", "2026-08-15");
        assertEquals(LocalDate.of(2026, 8, 1).atStartOfDay().toInstant(ZoneOffset.UTC), range.from());
        assertEquals(LocalDate.of(2026, 8, 15).atTime(23, 59, 59, 999_999_999).toInstant(ZoneOffset.UTC), range.to());
    }

    @Test
    @DisplayName("DateRange.fromStrings parses ISO-8601 Instant timestamps correctly")
    void testFromStringsInstant() {
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-15T12:00:00Z");
        DateRange range = DateRange.fromStrings(from.toString(), to.toString());
        assertEquals(from, range.from());
        assertEquals(to, range.to());
    }

    @Test
    @DisplayName("DateRange.fromStrings with only 'from' defaults 'to' to now")
    void testFromStringsOnlyFrom() {
        Instant from = Instant.now().minus(10, ChronoUnit.DAYS);
        DateRange range = DateRange.fromStrings(from.toString(), null);
        assertEquals(from, range.from());
        assertTrue(range.to().isAfter(from));
    }

    @Test
    @DisplayName("DateRange.fromStrings with only 'to' defaults 'from' to 30 days prior")
    void testFromStringsOnlyTo() {
        Instant to = Instant.now();
        DateRange range = DateRange.fromStrings(null, to.toString());
        assertEquals(to, range.to());
        assertTrue(range.from().isBefore(to));
    }

    @Test
    @DisplayName("DateRange throws InvalidDateRangeException when 'from' is after 'to'")
    void testFromAfterTo() {
        InvalidDateRangeException ex = assertThrows(InvalidDateRangeException.class, () ->
                DateRange.fromStrings("2026-08-20", "2026-08-10")
        );
        assertTrue(ex.getMessage().contains("must be before or equal to"));
    }

    @Test
    @DisplayName("DateRange throws InvalidDateRangeException when range exceeds 365 days")
    void testRangeExceedsMaxDays() {
        InvalidDateRangeException ex = assertThrows(InvalidDateRangeException.class, () ->
                DateRange.fromStrings("2024-01-01", "2026-01-01")
        );
        assertTrue(ex.getMessage().contains("cannot exceed 365 days"));
    }

    @Test
    @DisplayName("DateRange throws InvalidDateRangeException on unparseable date strings")
    void testInvalidDateFormat() {
        InvalidDateRangeException ex = assertThrows(InvalidDateRangeException.class, () ->
                DateRange.fromStrings("invalid-date", "2026-08-10")
        );
        assertTrue(ex.getMessage().contains("Invalid date format"));
    }
}
