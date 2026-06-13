package com.zblog.media.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proxies audio streams from Meting API (which resolves to NetEase/QQ/etc CDN URLs)
 * through the server.  This avoids browser CORS issues and CDN token expiry problems
 * that occur when the browser follows 302 redirects to cross-origin CDN URLs.
 */
@RestController
@RequestMapping("/api/v1")
public class AudioProxyController {

  private static final Logger log = LoggerFactory.getLogger(AudioProxyController.class);

  private final HttpClient httpClient;

  public AudioProxyController() {
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
  }

  @GetMapping("/audio/stream")
  public void stream(
      @RequestParam String server,
      @RequestParam(defaultValue = "song") String type,
      @RequestParam String id,
      HttpServletRequest request,
      HttpServletResponse response) {

    // Build the Meting API URL — internal Docker hostname
    String metingUrl =
        "http://meting:3000/api?server="
            + URLEncoder.encode(server, StandardCharsets.UTF_8)
            + "&type=url&id="
            + URLEncoder.encode(id, StandardCharsets.UTF_8);

    log.debug("Audio proxy: {}", metingUrl);

    try {
      HttpRequest.Builder requestBuilder =
          HttpRequest.newBuilder(URI.create(metingUrl))
              .GET()
              .timeout(Duration.ofSeconds(30));

      // Forward client Range header for seeking support
      String rangeHeader = request.getHeader("Range");
      if (rangeHeader != null && !rangeHeader.isEmpty()) {
        requestBuilder.header("Range", rangeHeader);
      }

      HttpResponse<InputStream> upstream =
          httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());

      int status = upstream.statusCode();
      response.setStatus(status);

      // Copy response headers (skip hop-by-hop and HTTP/2 pseudo-headers)
      Map<String, List<String>> headers = upstream.headers().map();
      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        String name = entry.getKey();
        if (name == null || name.startsWith(":") || name.isEmpty()
            || "content-encoding".equalsIgnoreCase(name)
            || "transfer-encoding".equalsIgnoreCase(name)
            || "connection".equalsIgnoreCase(name)
            || "keep-alive".equalsIgnoreCase(name)) {
          continue;
        }
        for (String value : entry.getValue()) {
          response.addHeader(name, value);
        }
      }

      // Ensure CORS headers for media playback
      response.setHeader("Access-Control-Allow-Origin", "*");
      response.setHeader("Accept-Ranges", "bytes");

      // Stream the audio to the client
      try (InputStream in = upstream.body();
          OutputStream out = response.getOutputStream()) {
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
          out.write(buffer, 0, n);
          if (out != null) {
            out.flush();
          }
        }
      }
    } catch (Exception e) {
      log.warn("Audio proxy failed for {}: {}", metingUrl, e.getMessage());
      if (!response.isCommitted()) {
        try {
          response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Audio proxy error");
        } catch (Exception ignored) {
          // Response already committed or unavailable
        }
      }
    }
  }
}
