package com.system.batch.session2.작전1;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 좀비 프로세스 정리 배치 Tasklet
 * ./gradlew bootRun --args='--spring.batch.job.name=zombieCleanupJob'
 */
@Configuration
@RequiredArgsConstructor
public class ZombieBatchConfig {
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  @Bean
  public Tasklet zombieProcessCleanupTasklet() {
    return new ZombieProcessCleanupTasklet();
  }

  /**
   * Tasklet 에서 DB 트랜잭션 관리가 필요한 경우는 많지 않음.</br>
   * 단순히, 파일을 정리하거나 알림의 보내는 작업의 경우 DB 트랜잭션을 고려할 필요가 없다.
   * 이럴경우, ResourcelessTransactionManager 라는 옵션을 고려할 수 있음
   */
  @Bean
  public Step zombieCleanupStep() {
    return new StepBuilder("zombieCleanupStep", jobRepository)
//        .tasklet(zombieProcessCleanupTasklet(), transactionManager)
        .tasklet(zombieProcessCleanupTasklet(), new ResourcelessTransactionManager())
        .build();
  }

  @Bean
  public Job zombieCleanupJob() {
    return new JobBuilder("zombieCleanupJob", jobRepository)
        .start(zombieCleanupStep())
        .build();
  }
}
