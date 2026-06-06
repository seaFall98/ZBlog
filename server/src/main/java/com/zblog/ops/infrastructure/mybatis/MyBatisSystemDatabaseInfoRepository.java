package com.zblog.ops.infrastructure.mybatis;

import com.zblog.ops.application.port.SystemDatabaseInfoRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisSystemDatabaseInfoRepository implements SystemDatabaseInfoRepository {

  private final SystemDatabaseInfoMapper systemDatabaseInfoMapper;

  public MyBatisSystemDatabaseInfoRepository(SystemDatabaseInfoMapper systemDatabaseInfoMapper) {
    this.systemDatabaseInfoMapper = systemDatabaseInfoMapper;
  }

  public String productName() {
    return systemDatabaseInfoMapper.productName();
  }

  public long tableCount() {
    return systemDatabaseInfoMapper.tableCount();
  }

  public String status() {
    return systemDatabaseInfoMapper.ping() == 1 ? "UP" : "DOWN";
  }
}
