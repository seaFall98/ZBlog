package com.zblog.tools.infrastructure.http;

import com.zblog.common.exception.BusinessException;
import com.zblog.tools.application.port.RemoteResource;
import com.zblog.tools.application.port.RemoteResourceFetcher;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class JavaHttpRemoteResourceFetcher implements RemoteResourceFetcher {

  private final HttpClient httpClient;

  public JavaHttpRemoteResourceFetcher() {
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
  }

  public RemoteResource fetch(URI uri, int limit, String... allowedContentTypes) {
    HttpRequest request =
        HttpRequest.newBuilder(uri)
            .GET()
            .timeout(Duration.ofSeconds(10))
            .header("User-Agent", "ZBlogAdminTools/0.1")
            .build();
    try {
      HttpResponse<InputStream> response =
          httpClient.send(request, responseInfo -> HttpResponse.BodySubscribers.ofInputStream());
      byte[] body = readLimited(response.body(), limit);
      int status = response.statusCode();
      if (status < 200 || status >= 300) {
        throw new BusinessException(40031, "Remote URL returned status " + status, HttpStatus.BAD_REQUEST);
      }
      String contentType = response.headers().firstValue("content-type").orElse("").toLowerCase(Locale.ROOT);
      boolean allowed = false;
      for (String allowedContentType : allowedContentTypes) {
        if (contentType.startsWith(allowedContentType)) {
          allowed = true;
          break;
        }
      }
      if (!allowed) {
        throw new BusinessException(40032, "Unsupported remote content type", HttpStatus.BAD_REQUEST);
      }
      Map<String, String> headers =
          response.headers().map().entrySet().stream()
              .filter(entry -> !entry.getValue().isEmpty())
              .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getFirst()));
      return new RemoteResource(headers, body);
    } catch (BusinessException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new BusinessException(40033, "Remote URL fetch failed", HttpStatus.BAD_REQUEST);
    }
  }

  private byte[] readLimited(InputStream input, int limit) throws Exception {
    byte[] body = input.readNBytes(limit + 1);
    if (body.length > limit) {
      throw new BusinessException(40039, "Remote content is too large", HttpStatus.BAD_REQUEST);
    }
    return body;
  }
}
