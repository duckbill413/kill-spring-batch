package com.system.batch.session2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
public class TerminatorConfig {

  @Bean
  public Job terminatorJob(JobRepository jobRepository, Step terminationStep) {
    return new JobBuilder("terminatorJob", jobRepository)
        .start(terminationStep)
        .build();
  }

  @Bean
  public Step terminationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet terminatorTasklet) {
  }
}
