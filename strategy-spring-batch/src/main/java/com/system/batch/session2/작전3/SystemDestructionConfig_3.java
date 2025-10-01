package com.system.batch.session2.작전3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
public class SystemDestructionConfig_3 {

  @Bean
  public Job killDashNineJob_3(JobRepository jobRepository, Step terminationStep_3) {
    return new JobBuilder("killDashNineJob_3", jobRepository)
        .listener(systemTerminationListener_3(null))
        .start(terminationStep_3)
        .build();
  }

  @Bean
  public Step terminationStep_3(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
    return new StepBuilder("terminationStep_3", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          log.info("시스템 제거 프로토콜 실행 중....");
          return RepeatStatus.FINISHED;
        }, platformTransactionManager)
        .build();
  }

  @Bean
  @JobScope
  public JobExecutionListener systemTerminationListener_3(
      @Value("#{jobParameters['terminationType']}") String terminationType
  ) {
    return new JobExecutionListener() {
      @Override
      public void beforeJob(JobExecution jobExecution) {
        log.info("시스템 제거 시작! 제거 방식: {}", terminationType);
      }

      @Override
      public void afterJob(JobExecution jobExecution) {
        log.info("작전 종료! 시스템 상태: {}", jobExecution.getStatus());
      }
    };
  }
}
