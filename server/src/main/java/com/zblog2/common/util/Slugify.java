package com.zblog2.common.util;

import java.text.Normalizer;
import java.util.Locale;

public final class Slugify {

  private Slugify() {}

  public static String from(String value) {
    String normalized =
        Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKD)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "-")
            .replaceAll("^-+|-+$", "");
    return normalized.isBlank() ? "untitled" : normalized;
  }
}
