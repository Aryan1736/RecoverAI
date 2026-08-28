package com.recoverai.backend.dto.analytics;

import com.recoverai.backend.exception.InvalidDateRangeException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record DateRange(Instant from, Instant to) {

    public static final long MAX_RANGE_DAYS = 365L;
    public static final long DEFAULT_DAYS_BACK = 30L;

    public DateRange {
        Objects.requireNonNull(from, "from date cannot be null");
        Objects.requireNonNull(to, "to date cannot be null");

        if (from.isAfter(to)) {
            throw new InvalidDateRangeException("The 'from' date (" + from + ") must be before or equal to the 'to' date (" + to + ")");
        }

        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new InvalidDateRangeException("Date range cannot exceed " + MAX_RANGE_DAYS + " days");
        }
    }

    public static DateRange of(Instant from, Instant to) {
        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(DEFAULT_DAYS_BACK, ChronoUnit.DAYS);
        return new DateRange(effectiveFrom, effectiveTo);
    }

    public static DateRange fromStrings(String fromStr, String toStr) {
        Instant fromInstant = parseDateString(fromStr, false);
        Instant toInstant = parseDateString(toStr, true);

        if (fromInstant == null && toInstant == null) {
            Instant now = Instant.now();
            return new DateRange(now.minus(DEFAULT_DAYS_BACK, ChronoUnit.DAYS), now);
        } else if (fromInstant == null) {
            return new DateRange(toInstant.minus(DEFAULT_DAYS_BACK, ChronoUnit.DAYS), toInstant);
        } else if (toInstant == null) {
            return new DateRange(fromInstant, Instant.now());
        } else {
            return new DateRange(fromInstant, toInstant);
        }
    }

    private static Instant parseDateString(String dateStr, boolean isEndExclusiveOrEndOfDay) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        String trimmed = dateStr.trim();
        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDate localDate = LocalDate.parse(trimmed);
            if (isEndExclusiveOrEndOfDay) {
                return localDate.atTime(23, 59, 59, 999_999_999).toInstant(ZoneOffset.UTC);
            } else {
                return localDate.atStartOfDay().toInstant(ZoneOffset.UTC);
            }
        } catch (DateTimeParseException e) {
            throw new InvalidDateRangeException("Invalid date format: '" + trimmed + "'. Expected ISO-8601 format (e.g., YYYY-MM-DD or YYYY-MM-DDTHH:mm:ssZ)");
        }
    }
}
