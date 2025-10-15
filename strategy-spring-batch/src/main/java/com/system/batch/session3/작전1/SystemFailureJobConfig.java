package com.system.batch.session3.작전1;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SystemFailureJobConfig {

  private final JobRepository jobRepository;

  private final PlatformTransactionManager transactionManager;

  @Bean
  public Job systemFailureJob(Step systemFailureStep) {
    return new JobBuilder("systemFailureJob", jobRepository)
        .start(systemFailureStep)
        .build();
  }

  @Bean
  public Step systemFailureStep(
      FlatFileItemReader<SystemFailure> systemFailureItemReader,
      SystemFailureStdoutItemWriter systemFailureStdoutItemWriter
  ) {
    return new StepBuilder("systemFailureStep", jobRepository)
        .<SystemFailure, SystemFailure>chunk(10, transactionManager)
        .reader(systemFailureItemReader)
        .writer(systemFailureStdoutItemWriter)
        .build();
  }

  @Bean
  @StepScope
  public FlatFileItemReader<SystemFailure> systemFailureItemReader(
      @Value("#{jobParameters['inputFile']}") String inputFile) {

    return new FlatFileItemReaderBuilder<SystemFailure>()
        .name("systemFailureItemReader")
        .comments("#")
        .resource(new FileSystemResource(inputFile))
        .delimited()
        .delimiter(",")
        .names("errorId", "errorDateTime", "severity", "processId", "errorMessage")
        .targetType(SystemFailure.class)
        .linesToSkip(1) // 헤더 라인은 실제 처리할 데이터가 아니므로 건너뜀
        .strict(true) // 파일과 데이터 검증의 강도를 설정하는 메서드로, 기본값은 true 이다. 이 경우 파일 누락 시 예외를 발생시켜 배치를 중단하고, false면 파일이 존재하지 않아도 경고만 남기고 진행한다
        .build();
  }

  @Bean
  public SystemFailureStdoutItemWriter systemFailureStdoutItemWriter() {
    return new SystemFailureStdoutItemWriter();
  }

  public static class SystemFailureStdoutItemWriter implements ItemWriter<SystemFailure> {

    @Override
    public void write(Chunk<? extends SystemFailure> chunk) throws Exception {
      for (SystemFailure failure : chunk) {
        log.info("Processing system failure: {}", failure);
      }
    }
  }

  @Data
  public static class SystemFailure {
    private String errorId;
    private String errorDateTime;
    private String severity;
    private Integer processId;
    private String errorMessage;
  }
}
