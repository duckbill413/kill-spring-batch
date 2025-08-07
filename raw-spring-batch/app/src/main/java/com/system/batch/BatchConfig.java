package com.system.batch;

import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * DefaultBatchConfiguration 은 Spring Batch 5 부터 도입된 기본 설정 클래스
 * JobRepository, JobLauncher 등 Spring Batch 의 핵심 컴포넌트를 자동으로 구성해줌
 */
@Configuration
public class BatchConfig extends DefaultBatchConfiguration {

  /**
   * Spring Batch 는 Job과 Step의 메타데이터를 데이터베이스에 저장한다.
   * 따라서 메타데이터 저장을 위한 DataSource 설정이 필요하다
   */
  @Bean
  public DataSource dataSource() {
    return new EmbeddedDatabaseBuilder()
        .setType(EmbeddedDatabaseType.H2)
        .addScript("org/springframework/batch/core/schema-h2.sql")
        .build();
  }

  /**
   * 메타데이터 저장과 배치 작업 실행 등 Spring Batch 의 모든 작업은 트랜잭션 내에서 처리된다.
   * 이를 위해 배치 코어 컴포넌트와 우리 Job 에서 공통으로 사용할 PlatformTransactionManager Bean 을 등록해야 한다.
   */
  @Bean
  public PlatformTransactionManager transactionManager() {
    return new DataSourceTransactionManager(dataSource());
  }
}
