package com.system.batch.session3.작전1;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class SystemFailureMultiFileConfig {

  private final JobRepository jobRepository;

  private final PlatformTransactionManager transactionManager;

  @Bean
  public Job systemFailureMultiFileJob(Step systemFailureMultiFileStep) {
    return new JobBuilder("systemFailureMultiFileJob", jobRepository)
        .start(systemFailureMultiFileStep)
        .build();
  }

  @Bean
  public Step systemFailureMultiFileStep(
      MultiResourceItemReader<SystemFailureJobConfig.SystemFailure> multiResourceItemReader,
      SystemFailureJobConfig.SystemFailureStdoutItemWriter systemFailureStdoutItemWriter
  ) {
    return new StepBuilder("systemFailureMultiFileStep", jobRepository)
        .<SystemFailureJobConfig.SystemFailure, SystemFailureJobConfig.SystemFailure>chunk(10, transactionManager)
        .reader(multiResourceItemReader)
        .writer(systemFailureStdoutItemWriter)
        .build();
  }

  @Bean
  @StepScope
  public MultiResourceItemReader<SystemFailureJobConfig.SystemFailure> multiResourceItemReader(
      @Value("#{jobParameters['inputFilePath']}") String inputFilePath
  ) {
    return new MultiResourceItemReaderBuilder<SystemFailureJobConfig.SystemFailure>()
        .name("multiResourceItemReader")
        .resources(
            new FileSystemResource(inputFilePath + "/critical-failure.csv")
            , new FileSystemResource(inputFilePath + "/normal-failure.csv")
        )
        .delegate(systemFailureFileReader())
        .build();
  }

  @Bean
  public FlatFileItemReader<SystemFailureJobConfig.SystemFailure> systemFailureFileReader() {
    return new FlatFileItemReaderBuilder<SystemFailureJobConfig.SystemFailure>()
        .name("systemFailureFileReader")
        .delimited()
        .delimiter(",")
        .names("errorId", "errorDateTime", "severity", "processId", "errorMessage")
        .targetType(SystemFailureJobConfig.SystemFailure.class)
        .linesToSkip(1)
        .build();
  }
}
