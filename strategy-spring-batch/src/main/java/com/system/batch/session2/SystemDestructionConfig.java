package com.system.batch.session2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Slf4j
@Configuration
public class SystemDestructionConfig {
  @Bean
  public Job systemDestructionJob(JobRepository jobRepository, Step systemDestructionStep, SystemDestructionValidator validator) {
    return new JobBuilder("systemDestructionJob", jobRepository)
        .validator(validator)
        .start(systemDestructionStep)
        .build();
  }

  @Bean
  public Job defaultSystemDestructionJob(JobRepository jobRepository, Step systemDestructionStep) {
    return new JobBuilder("defaultSystemDestructionJob", jobRepository)
        .validator(new DefaultJobParametersValidator(
            new String[]{"destructionPower"},  // 필수 파라미터
            new String[]{"targetSystem"}       // 선택적 파라미터
        )).start(systemDestructionStep)
        .build();
  }

  @Bean
  @JobScope
  public Step systemDestructionStep(
      @Value("#{jobParameters['destructionPower']}") Long destructionPower,
      JobRepository jobRepository, DataSourceTransactionManager transactionManager) {
    return new StepBuilder("systemDestructionStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          log.info("시스템 파괴 프로세스가 시작되었습니다: 파괴력: {}", destructionPower);
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }
}
