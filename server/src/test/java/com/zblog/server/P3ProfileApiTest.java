package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zblog.common.exception.BusinessException;
import com.zblog.media.application.FileService;
import com.zblog.media.controller.FileController;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import static org.mockito.Mockito.mock;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class P3ProfileApiTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void profileStoresBioAndRejectsReservedBadge() {
    String email = "p3-profile-" + System.nanoTime() + "@example.com";
    HttpHeaders headers = registerAndAuth(email, "Profile Reader", "reader123456");

    ResponseEntity<Map> updateResponse =
        restTemplate.exchange(
            "/api/v1/user/profile",
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of(
                    "nickname", "Profile Reader",
                    "email", email,
                    "website", "https://example.com",
                    "bio", "写一点安静的文字",
                    "badge", "读者"),
                headers),
            Map.class);

    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> profile = data(updateResponse);
    assertThat(profile.get("bio")).isEqualTo("写一点安静的文字");
    assertThat(profile.get("badge")).isEqualTo("读者");

    ResponseEntity<Map> reservedBadgeResponse =
        restTemplate.exchange(
            "/api/v1/user/profile",
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("nickname", "Profile Reader", "email", email, "badge", "管理员"), headers),
            Map.class);

    assertThat(reservedBadgeResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void changingEmailRequiresCurrentPasswordAndNewEmailCanLogin() {
    String email = "p3-email-" + System.nanoTime() + "@example.com";
    String newEmail = "p3-email-new-" + System.nanoTime() + "@example.com";
    HttpHeaders headers = registerAndAuth(email, "Email Reader", "reader123456");

    ResponseEntity<Map> missingPasswordResponse =
        restTemplate.exchange(
            "/api/v1/user/profile",
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("nickname", "Email Reader", "email", newEmail), headers),
            Map.class);
    assertThat(missingPasswordResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Map> updateResponse =
        restTemplate.exchange(
            "/api/v1/user/profile",
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of(
                    "nickname", "Email Reader",
                    "email", newEmail,
                    "current_password", "reader123456"),
                headers),
            Map.class);
    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(data(updateResponse).get("email")).isEqualTo(newEmail);

    ResponseEntity<Map> loginResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            Map.of("email", newEmail, "password", "reader123456"),
            Map.class);
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void avatarUploadRequiresAuthenticationAndRestrictsFileType() {
    FileController controller = new FileController(mock(FileService.class));
    assertThatThrownBy(
            () ->
                controller.uploadPublic(
                    new MockMultipartFile("file", "avatar.png", "image/png", pngBytes()),
                    "用户头像",
                    null,
                    new MockHttpServletRequest()))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).status())
        .isEqualTo(HttpStatus.UNAUTHORIZED);

    HttpHeaders authHeaders =
        registerAndAuth("p3-avatar-" + System.nanoTime() + "@example.com", "Avatar Reader", "reader123456");
    authHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

    MultiValueMap<String, Object> invalidBody = new LinkedMultiValueMap<>();
    invalidBody.add("file", resource("avatar.txt", "not image".getBytes(), MediaType.TEXT_PLAIN));
    invalidBody.add("type", "用户头像");
    ResponseEntity<Map> invalid =
        restTemplate.exchange("/api/v1/upload", HttpMethod.POST, new HttpEntity<>(invalidBody, authHeaders), Map.class);
    assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    MultiValueMap<String, Object> validBody = new LinkedMultiValueMap<>();
    validBody.add("file", resource("avatar.png", pngBytes(), MediaType.IMAGE_PNG));
    validBody.add("type", "用户头像");
    ResponseEntity<Map> valid =
        restTemplate.exchange("/api/v1/upload", HttpMethod.POST, new HttpEntity<>(validBody, authHeaders), Map.class);
    assertThat(valid.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(data(valid).get("file_url")).asString().startsWith("/uploads/");
  }

  private HttpHeaders registerAndAuth(String email, String nickname, String password) {
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "/api/v1/auth/register",
            Map.of("email", email, "nickname", nickname, "password", password),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(data(response).get("access_token").toString());
    return headers;
  }

  private HttpEntity<ByteArrayResource> resource(String filename, byte[] bytes, MediaType mediaType) {
    ByteArrayResource body =
        new ByteArrayResource(bytes) {
      @Override
      public String getFilename() {
        return filename;
      }

      @Override
      public long contentLength() {
        return bytes.length;
      }
    };
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    return new HttpEntity<>(body, headers);
  }

  private byte[] pngBytes() {
    return new byte[] {(byte) 0x89, 'P', 'N', 'G', 0, 1, 2, 3};
  }

  private Map<?, ?> data(ResponseEntity<Map> response) {
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("code")).isEqualTo(0);
    return (Map<?, ?>) response.getBody().get("data");
  }
}
