package com.system.batch.session2.작전2;

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
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
public class PojoTerminatorConfig {

  @Bean
  public Job pojoTerminationJob(JobRepository jobRepository, Step pojoTerminationStep) {
    return new JobBuilder("pojoTerminationJob", jobRepository)
        .start(pojoTerminationStep)
        .build();
  }

  @Bean
  public Step pojoTerminationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet pojoTerminatorTasklet) {
    return new StepBuilder("pojoTerminationStep", jobRepository)
        .tasklet(pojoTerminatorTasklet, transactionManager)
        .build();
  }

  @Bean
  public Tasklet pojoTerminatorTasklet(SystemInfiltrationParameters infiltrationParameters) {
    return (contribution, chunkContext) -> {
      log.info("⚔️ 시스템 침투 작전 초기화!");
      log.info("임무 코드네임: {}", infiltrationParameters.getMissionName());
      log.info("보안 레벨: {}", infiltrationParameters.getSecurityLevel());
      log.info("작전 지휘관: {}", infiltrationParameters.getOperationCommander());

      // 보안 레벨에 따른 침투 난이도 계산
      int baseInfiltrationTime = 60;
      int infiltrationMultiplier = switch (infiltrationParameters.getSecurityLevel()) {
        case 1 -> 1;
        case 2 -> 2;
        case 3 -> 4;
        case 4 -> 8;
        default -> 1;
      };

      int totalInfiltrationTime = baseInfiltrationTime * infiltrationMultiplier;
      log.info("🧨 시스템 해킹 난이도 분석중.....");
      log.info("🕖 예상 침투 시간: {}분", totalInfiltrationTime);
      log.info("🏆 시스템 장악 준비 완료!");
      return RepeatStatus.FINISHED;
    };
  }
}
