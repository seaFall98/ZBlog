package com.zblog.common.util;

import com.zblog.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import org.springframework.http.HttpStatus;

public record AdminDateRange(LocalDateTime startInclusive, LocalDateTime endExclusive) {

  public static AdminDateRange parse(String start, String end) {
    return new AdminDateRange(parseBoundary(start, false), parseBoundary(end, true));
  }

  private static LocalDateTime parseBoundary(String value, boolean end) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String trimmed = value.trim();
    try {
      if (trimmed.length() == 10) {
        LocalDate date = LocalDate.parse(trimmed);
        return end ? date.plusDays(1).atStartOfDay() : date.atStartOfDay();
      }
      return LocalDateTime.parse(trimmed);
    } catch (DateTimeParseException exception) {
      throw new BusinessException(40001, "invalid date range", HttpStatus.BAD_REQUEST);
    }
  }
}
