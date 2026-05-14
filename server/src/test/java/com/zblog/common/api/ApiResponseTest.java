package com.zblog.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

  @Test
  void okWrapsDataWithStableCodeAndMessage() {
    ApiResponse<String> response = ApiResponse.ok("alive");

    assertThat(response.code()).isEqualTo(0);
    assertThat(response.message()).isEqualTo("success");
    assertThat(response.data()).isEqualTo("alive");
  }

  @Test
  void failureWrapsErrorCodeAndMessageWithoutData() {
    ApiResponse<Object> response = ApiResponse.failure(40001, "bad request");

    assertThat(response.code()).isEqualTo(40001);
    assertThat(response.message()).isEqualTo("bad request");
    assertThat(response.data()).isNull();
  }
}
