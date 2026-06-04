package com.zblog.tools.application;

import com.zblog.common.exception.BusinessException;
import com.zblog.tools.application.port.RemoteResource;
import com.zblog.tools.application.port.RemoteResourceFetcher;
import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AdminToolsService {

  private static final int MAX_HTML_BYTES = 512 * 1024;
  private static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024;
  private static final Pattern TITLE_PATTERN =
      Pattern.compile("(?is)<title[^>]*>(.*?)</title>");
  private static final Pattern DESCRIPTION_PATTERN =
      Pattern.compile(
          "(?is)<meta[^>]+(?:name|property)\\s*=\\s*[\"'](?:description|og:description)[\"'][^>]+content\\s*=\\s*[\"']([^\"']*)[\"'][^>]*>");
  private static final Pattern DESCRIPTION_REVERSED_PATTERN =
      Pattern.compile(
          "(?is)<meta[^>]+content\\s*=\\s*[\"']([^\"']*)[\"'][^>]+(?:name|property)\\s*=\\s*[\"'](?:description|og:description)[\"'][^>]*>");
  private static final Pattern ICON_PATTERN =
      Pattern.compile(
          "(?is)<link[^>]+rel\\s*=\\s*[\"'][^\"']*(?:icon|shortcut icon)[^\"']*[\"'][^>]+href\\s*=\\s*[\"']([^\"']*)[\"'][^>]*>");
  private static final Pattern ICON_REVERSED_PATTERN =
      Pattern.compile(
          "(?is)<link[^>]+href\\s*=\\s*[\"']([^\"']*)[\"'][^>]+rel\\s*=\\s*[\"'][^\"']*(?:icon|shortcut icon)[^\"']*[\"'][^>]*>");

  private final RemoteResourceFetcher remoteResourceFetcher;
  private final boolean allowPrivateNetwork;

  public AdminToolsService(
      RemoteResourceFetcher remoteResourceFetcher,
      @Value("${zblog.tools.allow-private-network:false}") boolean allowPrivateNetwork) {
    this.remoteResourceFetcher = remoteResourceFetcher;
    this.allowPrivateNetwork = allowPrivateNetwork;
  }

  public Map<String, Object> fetchLinkMeta(Map<String, String> request) {
    URI uri = validatedHttpUri(request.get("url"));
    RemoteResource response = remoteResourceFetcher.fetch(uri, MAX_HTML_BYTES, "text/html", "application/xhtml+xml");
    String html = new String(response.body(), java.nio.charset.StandardCharsets.UTF_8);
    String favicon = firstMatch(html, ICON_PATTERN).or(() -> firstMatch(html, ICON_REVERSED_PATTERN))
        .map(value -> uri.resolve(value).toString())
        .orElse(uri.resolve("/favicon.ico").toString());
    return Map.of(
        "url", uri.toString(),
        "title", firstMatch(html, TITLE_PATTERN).map(this::compact).orElse(""),
        "description",
            firstMatch(html, DESCRIPTION_PATTERN)
                .or(() -> firstMatch(html, DESCRIPTION_REVERSED_PATTERN))
                .map(this::compact)
                .orElse(""),
        "favicon", favicon);
  }

  public Map<String, String> parseVideo(Map<String, String> request) {
    URI uri = validatedHttpUri(request.get("url"));
    String host = uri.getHost().toLowerCase(Locale.ROOT);
    String path = uri.getPath() == null ? "" : uri.getPath();
    String query = uri.getQuery() == null ? "" : uri.getQuery();

    if (host.endsWith("bilibili.com") || host.equals("b23.tv")) {
      Matcher matcher = Pattern.compile("(?i)/(?:video/)?(BV[0-9A-Za-z]+|av\\d+)").matcher(path);
      if (matcher.find()) {
        return Map.of("platform", "bilibili", "video_id", matcher.group(1));
      }
    }

    if (host.equals("youtu.be")) {
      String id = path.replaceFirst("^/", "");
      if (!id.isBlank()) {
        return Map.of("platform", "youtube", "video_id", id);
      }
    }

    if (host.endsWith("youtube.com")) {
      Optional<String> id = queryParam(query, "v");
      if (id.isPresent()) {
        return Map.of("platform", "youtube", "video_id", id.get());
      }
      Matcher shorts = Pattern.compile("^/(?:shorts|embed)/([^/?#]+)").matcher(path);
      if (shorts.find()) {
        return Map.of("platform", "youtube", "video_id", shorts.group(1));
      }
    }

    throw new BusinessException(40030, "Unsupported video URL", HttpStatus.BAD_REQUEST);
  }

  public Map<String, Object> downloadImage(Map<String, String> request) {
    URI uri = validatedHttpUri(request.get("url"));
    RemoteResource response = remoteResourceFetcher.fetch(uri, MAX_IMAGE_BYTES, "image/");
    String contentType = response.headers().getOrDefault("content-type", "application/octet-stream");
    byte[] body = response.body();
    return Map.of(
        "content_type", contentType.split(";")[0],
        "content_length", body.length,
        "data", Base64.getEncoder().encodeToString(body));
  }

  private URI validatedHttpUri(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(40034, "url is required", HttpStatus.BAD_REQUEST);
    }
    try {
      URI uri = URI.create(value.trim()).normalize();
      if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
        throw new BusinessException(40035, "Only http and https URLs are supported", HttpStatus.BAD_REQUEST);
      }
      String host = uri.getHost();
      if (host == null || host.isBlank()) {
        throw new BusinessException(40036, "Invalid URL host", HttpStatus.BAD_REQUEST);
      }
      String asciiHost = IDN.toASCII(host);
      if (!allowPrivateNetwork && isPrivateHost(asciiHost)) {
        throw new BusinessException(40037, "Private network URLs are not allowed", HttpStatus.BAD_REQUEST);
      }
      return new URI(uri.getScheme(), uri.getUserInfo(), asciiHost, uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
    } catch (BusinessException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new BusinessException(40038, "Invalid URL", HttpStatus.BAD_REQUEST);
    }
  }

  private boolean isPrivateHost(String host) throws Exception {
    InetAddress address = InetAddress.getByName(host);
    return address.isAnyLocalAddress()
        || address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress()
        || address.isMulticastAddress();
  }

  private Optional<String> firstMatch(String html, Pattern pattern) {
    Matcher matcher = pattern.matcher(html);
    return matcher.find() ? Optional.of(decodeHtml(matcher.group(1))) : Optional.empty();
  }

  private Optional<String> queryParam(String query, String name) {
    for (String part : query.split("&")) {
      int index = part.indexOf('=');
      if (index > 0 && part.substring(0, index).equals(name)) {
        return Optional.of(part.substring(index + 1));
      }
    }
    return Optional.empty();
  }

  private String compact(String value) {
    return value.replaceAll("\\s+", " ").trim();
  }

  private String decodeHtml(String value) {
    return value
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'");
  }
}
