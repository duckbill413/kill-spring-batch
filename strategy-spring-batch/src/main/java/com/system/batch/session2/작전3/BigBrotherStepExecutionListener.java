package com.system.batch.session2.작전3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

/**
 * StepExecutionListener 인터페이스 구현
 */
@Slf4j
@Component
public class BigBrotherStepExecutionListener implements StepExecutionListener {
  @Override
  public void beforeStep(StepExecution stepExecution) {
    log.info("Step 구역 감시 시작. 모든 행동이 기록된다.");
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    log.info("Step 감시 종료. 모든 행동이 기록되었다.");
    log.info("Big Brother 의 감시망에서 벗어날 수 없을 것이다.");
    return ExitStatus.COMPLETED;
  }
}
