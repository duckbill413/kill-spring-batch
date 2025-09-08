package com.system.batch.session2.작전3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ServerRackControlListener {
  @BeforeStep
  public void accessServerRack(StepExecution stepExecution) {
    log.info("서버랙 접근 시작. 콘센트를 찾는중");
  }

  /**
   * `@AfterStep` 을 사용할 때는 반드시 Return 타입으로 ExitStatus 를 지켜야 한다.
   *
   * @param stepExecution stepExecution
   * @return ExitStatus
   */
  @AfterStep
  public ExitStatus leaveServerRack(StepExecution stepExecution) {
    log.info("코드를 뽑아버렸다.");
    return new ExitStatus("POWER_DOWN");
  }
}
