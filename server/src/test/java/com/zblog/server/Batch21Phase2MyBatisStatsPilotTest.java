package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.zblog.stats.application.port.StatsRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class Batch21Phase2MyBatisStatsPilotTest {

  @Autowired private StatsRepository statsRepository;

  @Test
  void statsRepositoryPilotUsesMyBatisInfrastructureAdapter() throws Exception {
    assertThat(statsRepository.getClass().getName())
        .contains("MyBatisStatsRepository");
    assertThat(Class.forName("com.zblog.stats.infrastructure.mybatis.StatsMapper")).isNotNull();
    assertThat(
            Files.exists(
                Path.of(
                    "src/main/resources/mapper/stats/StatsMapper.xml")))
        .isTrue();
    assertThat(statsRepository.countPublishedArticles()).isGreaterThanOrEqualTo(0);
  }
}
