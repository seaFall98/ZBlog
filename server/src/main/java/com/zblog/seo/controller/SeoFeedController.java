package com.zblog.seo.controller;

import com.zblog.seo.application.SeoFeedService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeoFeedController {

  private final SeoFeedService seoFeedService;

  public SeoFeedController(SeoFeedService seoFeedService) {
    this.seoFeedService = seoFeedService;
  }

  @GetMapping(value = "/rss.xml", produces = MediaType.APPLICATION_XML_VALUE)
  public String rss() {
    return seoFeedService.rss();
  }

  @GetMapping(value = "/atom.xml", produces = MediaType.APPLICATION_XML_VALUE)
  public String atom() {
    return seoFeedService.atom();
  }

  @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
  public String sitemap() {
    return seoFeedService.sitemap();
  }
}
