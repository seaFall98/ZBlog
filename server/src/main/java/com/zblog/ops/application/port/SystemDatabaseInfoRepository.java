package com.zblog.ops.application.port;

public interface SystemDatabaseInfoRepository {

  String productName();

  long tableCount();

  String status();
}
