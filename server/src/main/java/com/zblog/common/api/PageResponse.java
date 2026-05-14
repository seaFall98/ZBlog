package com.zblog.common.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PageResponse<T>(
    List<T> list, long total, int page, @JsonProperty("page_size") int pageSize) {}
