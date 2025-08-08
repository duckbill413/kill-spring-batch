package com.system.batch.config.session2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FileCleanupBatchConfig {
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final JdbcTemplate jdbcTemplate;

  @Bean
  public Tasklet deleteOldFilesTasklet() {
    // "temp" 디렉토리에서 30일 이상 지난 파일 삭제
    return new DeleteOldFilesTasklet("/path/to/temp", 30);
  }

  @Bean
  public Step deleteOldFilesStep() {
    return new StepBuilder("deleteOldFilesStep", jobRepository)
        .tasklet(deleteOldFilesTasklet(), transactionManager)
        .build();
  }

  /**
   * 배치를 이용하여 오래된 파일을 삭제하는 Tasklet
   */
  @Bean
  public Job deleteOldFilesJob() {
    return new JobBuilder("deleteOldFilesJob", jobRepository)
        .start(deleteOldFilesStep())
        .build();
  }

  @Bean
  public Step deleteOldRecordsStep() {
    return new StepBuilder("deleteOldRecordsStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          int deleted = jdbcTemplate.update("DELETE FROM logs WHERE created < NOW() - INTERVAL 7 DAY");
          log.info("🗑️ {}개의 오래된 레코드가 삭제되었습니다.", deleted);
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }

  /**
   * 매일 밤 7일이 지난 레코드를 삭제하는 배치 </br>
   * <span>created</span> 컬럼을 기준으로 삭제하는 작업의 예시
   *
   * @return
   */
  @Bean
  public Job deleteOldRecordsJob() {
    return new JobBuilder("deleteOldRecordsJob", jobRepository)
        .start(deleteOldRecordsStep())  // Step을 Job에 등록
        .build();
  }

}
