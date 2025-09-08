package com.system.batch.session2.작전2;

import lombok.Getter;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@StepScope
@Component
public class SystemInfiltrationParameters {
  private final String operationCommander;
  @Value("#{jobParameters[missionName]}")
  private String missionName;
  private int securityLevel;

  public SystemInfiltrationParameters(
      @Value("#{jobParameters['operationCommander']}") String operationCommander
  ) {
    this.operationCommander = operationCommander;
  }

  @Value("#{jobParameters[securityLevel]}")
  public void setSecurityLevel(int securityLevel) {
    this.securityLevel = securityLevel;
  }
}
