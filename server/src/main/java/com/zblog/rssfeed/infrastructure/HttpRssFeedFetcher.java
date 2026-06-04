package com.zblog.rssfeed.infrastructure;

import com.zblog.rssfeed.application.FeedItem;
import com.zblog.rssfeed.application.port.RssFeedFetcher;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@Component
public class HttpRssFeedFetcher implements RssFeedFetcher {

  private static final int MAX_FEED_BYTES = 1024 * 1024;

  private final HttpClient httpClient;
  private final boolean allowPrivateNetwork;

  public HttpRssFeedFetcher(
      @Value("${zblog.rss.allow-private-network:false}") boolean allowPrivateNetwork) {
    this.allowPrivateNetwork = allowPrivateNetwork;
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
  }

  public List<FeedItem> fetchItems(String value) {
    URI uri = validatedHttpUri(value);
    HttpRequest request =
        HttpRequest.newBuilder(uri)
            .GET()
            .timeout(Duration.ofSeconds(10))
            .header("User-Agent", "ZBlogRssReader/0.1")
            .build();
    try {
      HttpResponse<InputStream> response =
          httpClient.send(request, responseInfo -> HttpResponse.BodySubscribers.ofInputStream());
      byte[] body = readLimited(response.body());
      int status = response.statusCode();
      if (status < 200 || status >= 300) {
        throw new IllegalStateException("HTTP " + status);
      }
      return parseFeed(body, uri);
    } catch (RuntimeException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException("RSS fetch failed");
    }
  }

  private List<FeedItem> parseFeed(byte[] body, URI feedUri) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setXIncludeAware(false);
      factory.setExpandEntityReferences(false);
      var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(body));
      List<FeedItem> items = new ArrayList<>();
      var rssItems = document.getElementsByTagName("item");
      for (int i = 0; i < rssItems.getLength(); i++) {
        items.add(parseRssItem((Element) rssItems.item(i), feedUri));
      }
      var atomEntries = document.getElementsByTagName("entry");
      for (int i = 0; i < atomEntries.getLength(); i++) {
        items.add(parseAtomEntry((Element) atomEntries.item(i), feedUri));
      }
      return items.stream().filter(item -> !item.title().isBlank() && !item.link().isBlank()).toList();
    } catch (Exception exception) {
      throw new IllegalStateException("RSS parse failed");
    }
  }

  private FeedItem parseRssItem(Element element, URI feedUri) {
    String title = childText(element, "title");
    String link = absoluteUrl(feedUri, childText(element, "link"));
    String description = childText(element, "description");
    LocalDateTime publishedAt = parsePublishedAt(childText(element, "pubDate"));
    return new FeedItem(title, link, description, publishedAt);
  }

  private FeedItem parseAtomEntry(Element element, URI feedUri) {
    String title = childText(element, "title");
    String link = "";
    var links = element.getElementsByTagName("link");
    for (int i = 0; i < links.getLength(); i++) {
      Element linkElement = (Element) links.item(i);
      String rel = linkElement.getAttribute("rel");
      if (rel.isBlank() || "alternate".equals(rel)) {
        link = linkElement.getAttribute("href");
        break;
      }
    }
    String description = childText(element, "summary");
    if (description.isBlank()) {
      description = childText(element, "content");
    }
    String published = childText(element, "published");
    if (published.isBlank()) {
      published = childText(element, "updated");
    }
    return new FeedItem(title, absoluteUrl(feedUri, link), description, parsePublishedAt(published));
  }

  private URI validatedHttpUri(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("RSS URL is required");
    }
    try {
      URI uri = URI.create(value.trim()).normalize();
      if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
        throw new IllegalStateException("Only http and https RSS URLs are supported");
      }
      String host = uri.getHost();
      if (host == null || host.isBlank()) {
        throw new IllegalStateException("Invalid RSS URL host");
      }
      String asciiHost = IDN.toASCII(host);
      if (!allowPrivateNetwork && isPrivateHost(asciiHost)) {
        throw new IllegalStateException("Private network RSS URLs are not allowed");
      }
      return new URI(
          uri.getScheme(),
          uri.getUserInfo(),
          asciiHost,
          uri.getPort(),
          uri.getPath(),
          uri.getQuery(),
          uri.getFragment());
    } catch (IllegalStateException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException("Invalid RSS URL");
    }
  }

  private byte[] readLimited(InputStream input) throws Exception {
    byte[] body = input.readNBytes(MAX_FEED_BYTES + 1);
    if (body.length > MAX_FEED_BYTES) {
      throw new IllegalStateException("RSS feed is too large");
    }
    return body;
  }

  private boolean isPrivateHost(String host) throws Exception {
    InetAddress address = InetAddress.getByName(host);
    return address.isAnyLocalAddress()
        || address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress()
        || address.isMulticastAddress();
  }

  private String childText(Element element, String tag) {
    var nodes = element.getElementsByTagName(tag);
    if (nodes.getLength() == 0) {
      return "";
    }
    Node node = nodes.item(0);
    return node == null || node.getTextContent() == null ? "" : decodeHtml(node.getTextContent().trim());
  }

  private String absoluteUrl(URI feedUri, String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return feedUri.resolve(value.trim()).toString();
  }

  private LocalDateTime parsePublishedAt(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return OffsetDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
          .withOffsetSameInstant(ZoneOffset.UTC)
          .toLocalDateTime();
    } catch (DateTimeParseException ignored) {
      try {
        return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
      } catch (DateTimeParseException ignoredAgain) {
        return null;
      }
    }
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
