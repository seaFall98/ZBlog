package com.zblog.search.domain;

public enum SearchStrategy {
  DB("db"),
  ELASTICSEARCH("elasticsearch");

  private final String value;

  SearchStrategy(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
