package com.system.batch;

import org.springframework.batch.core.converter.JsonJobParametersConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class BatchConfig {

  @Bean
  public JsonJobParametersConverter jobParameterConverter() {
    return new JsonJobParametersConverter();
  }
}

