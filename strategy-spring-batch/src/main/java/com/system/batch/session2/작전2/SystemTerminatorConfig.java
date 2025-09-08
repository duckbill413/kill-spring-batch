package com.system.batch.session2.작전2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
public class SystemTerminatorConfig {
  @Bean
  public Job processTerminatorJob(JobRepository jobRepository, Step processTerminationStep) {
    return new JobBuilder("processTerminatorJob", jobRepository)
        .start(processTerminationStep)
        .build();
  }

  @Bean
  public Step processTerminationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet processTerminationTasklet) {
    return new StepBuilder("processTerminationStep", jobRepository)
        .tasklet(processTerminationTasklet, transactionManager)
        .build();
  }

  @Bean
  @StepScope
  public Tasklet processTerminationTasklet(
      @Value("#{jobParameters['terminatorId']}") String terminatorId,
      @Value("#{jobParameters['targetCount']}") int targetCount
  ) {
    return (contribution, chunkContext) -> {
      log.info("시스템 종결자 정보");
      log.info("ID: {}", terminatorId);
      log.info("제거 대상 수: {}", targetCount);
      log.info("⚡️ SYSTEM TERMINATOR {} 작전을 개시 합니다.", terminatorId);

      for (int i = 0; i < targetCount; i++) {
        log.info("💀 프로세스 {} 종료 완료!", i + 1);
      }
      log.info("🎯 임무 완료: 모든 대상 프로세스가 종료되었습니다.");
      return RepeatStatus.FINISHED;
    };
  }
}
