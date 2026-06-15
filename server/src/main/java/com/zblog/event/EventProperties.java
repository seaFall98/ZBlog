package com.zblog.event;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zblog.mq")
public class EventProperties {

  private String exchange = "zblog.events";
  private String articlePublishedRoutingKey = "article.published";
  private String articlePublishedQueue = "zblog.article.published";
  private String searchIndexRoutingKey = "search.index";
  private String searchIndexQueue = "zblog.search.index";
  private String commentReplyRoutingKey = "comment.reply";
  private String commentReplyQueue = "zblog.comment.reply";
  private long publishFixedDelayMs = 5000;

  public String getExchange() {
    return exchange;
  }

  public void setExchange(String exchange) {
    this.exchange = exchange;
  }

  public String getArticlePublishedRoutingKey() {
    return articlePublishedRoutingKey;
  }

  public void setArticlePublishedRoutingKey(String articlePublishedRoutingKey) {
    this.articlePublishedRoutingKey = articlePublishedRoutingKey;
  }

  public String getArticlePublishedQueue() {
    return articlePublishedQueue;
  }

  public void setArticlePublishedQueue(String articlePublishedQueue) {
    this.articlePublishedQueue = articlePublishedQueue;
  }

  public String getSearchIndexRoutingKey() {
    return searchIndexRoutingKey;
  }

  public void setSearchIndexRoutingKey(String searchIndexRoutingKey) {
    this.searchIndexRoutingKey = searchIndexRoutingKey;
  }

  public String getSearchIndexQueue() {
    return searchIndexQueue;
  }

  public void setSearchIndexQueue(String searchIndexQueue) {
    this.searchIndexQueue = searchIndexQueue;
  }

  public String getCommentReplyRoutingKey() {
    return commentReplyRoutingKey;
  }

  public void setCommentReplyRoutingKey(String commentReplyRoutingKey) {
    this.commentReplyRoutingKey = commentReplyRoutingKey;
  }

  public String getCommentReplyQueue() {
    return commentReplyQueue;
  }

  public void setCommentReplyQueue(String commentReplyQueue) {
    this.commentReplyQueue = commentReplyQueue;
  }

  public long getPublishFixedDelayMs() {
    return publishFixedDelayMs;
  }

  public void setPublishFixedDelayMs(long publishFixedDelayMs) {
    this.publishFixedDelayMs = publishFixedDelayMs;
  }
}
