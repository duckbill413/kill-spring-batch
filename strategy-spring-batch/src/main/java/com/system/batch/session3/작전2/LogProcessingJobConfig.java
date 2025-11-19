package com.system.batch.session3.작전2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.SystemCommandTasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Configuration
@RequiredArgsConstructor
public class LogProcessingJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  @Bean
  public Job logProcessingJob(
      Step createDirectoryStep,
      Step logCollectionStep,
      Step logProcessingStep
  ) {
    return new JobBuilder("logProcessingJob", jobRepository)
        .start(createDirectoryStep)
        .next(logCollectionStep)
        .next(logProcessingStep)
        .build();
  }

  @Bean
  public Step createDirectoryStep(SystemCommandTasklet mkdirTasklet) {
    return new StepBuilder("createDirectoryStep", jobRepository)
        .tasklet(mkdirTasklet, transactionManager)
        .build();
  }

  @Bean
  @StepScope
  public SystemCommandTasklet mkdirTasklet(
      @Value("#{jobParameters['date']}") String date) {
    SystemCommandTasklet tasklet = new SystemCommandTasklet();
    tasklet.setWorkingDirectory(System.getProperty("user.home"));

    String collectedLogsPath = "collected_ecommerce_logs/" + date;
    String processedLogsPath = "processed_logs/" + date;

    tasklet.setCommand("mkdir", "-p", collectedLogsPath, processedLogsPath, " && ls -al");
    tasklet.setTimeout(3000); // 3초 타임아웃
    return tasklet;
  }

  @Bean
  public Step logCollectionStep(SystemCommandTasklet scpTasklet) {
    return new StepBuilder("logCollectionStep", jobRepository)
        .tasklet(scpTasklet, transactionManager)
        .build();
  }

  @Bean
  @StepScope
  public SystemCommandTasklet scpTasklet(
      @Value("#{jobParameters['date']}") String date) {
    SystemCommandTasklet tasklet = new SystemCommandTasklet();
    tasklet.setWorkingDirectory(System.getProperty("user.home"));
    String processedLogsPath = "collected_ecommerce_logs/" + date;

    StringJoiner commandBuilder = new StringJoiner(" && ");
    for (String host : List.of("localhost")) {
      String command = String.format(
          "cp ~/ecommerce_logs/%s.log ./%s/%s.log",
          date, processedLogsPath, host
      );
//      String command = String.format("scp %s:~/ecommerce_logs/%s.log ./%s/%s.log",
//          host, date, processedLogsPath, host);

      commandBuilder.add(command);
    }

    tasklet.setCommand("/bin/sh", "-c", commandBuilder.toString());
    tasklet.setTimeout(10000); //10초 타임아웃
    return tasklet;
  }

  @Bean
  public Step logProcessingStep(
      MultiResourceItemReader<LogEntry> multiResourceLogItemReader,
      LogEntryProcessor logEntryProcessor,
      FlatFileItemWriter<ProcessedLogEntry> processedLogEntryJsonWriter
  ) {
    return new StepBuilder("logProcessingStep", jobRepository)
        .<LogEntry, ProcessedLogEntry>chunk(10, transactionManager)
        .reader(multiResourceLogItemReader)
        .processor(logEntryProcessor)
        .writer(processedLogEntryJsonWriter)
        .build();
  }

  @Bean
  @StepScope
  public MultiResourceItemReader<LogEntry> multiResourceLogItemReader(
      @Value("#{jobParameters['date']}") String date) {
    MultiResourceItemReader<LogEntry> resourceItemReader = new MultiResourceItemReader<>();
    resourceItemReader.setName("multiResourceLogItemReader");
    resourceItemReader.setResources(getResources(date));
    resourceItemReader.setDelegate(logFileReader());
    return resourceItemReader;
  }

  private Resource[] getResources(String date) {
    try {
      String userHome = System.getProperty("user.home");
      String location = "file:" + userHome + "/collected_ecommerce_logs/" + date + "/*.log";

      PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      return resolver.getResources(location);
    } catch (IOException e) {
      throw new RuntimeException("Failed to resolve log files", e);
    }
  }

  @Bean
  public FlatFileItemReader<LogEntry> logFileReader() {
    return new FlatFileItemReaderBuilder<LogEntry>()
        .name("logFileReader")
        .delimited()
        .delimiter(",")
        .names("dateTime", "level", "message")
        .targetType(LogEntry.class)
        .build();
  }

  @Bean
  public LogEntryProcessor logEntryProcessor() {
    return new LogEntryProcessor();
  }

  @Bean
  @StepScope
  public FlatFileItemWriter<ProcessedLogEntry> processedLogEntryJsonWriter(
      @Value("#{jobParameters['date']}") String date) {
    String userHome = System.getProperty("user.home");
    String outputPath = Paths.get(userHome, "processed_logs", date, "processed_logs.json").toString();

    ObjectMapper objectMapper = new ObjectMapper();
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addSerializer(LocalDateTime.class,
        new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
    objectMapper.registerModule(javaTimeModule);

    return new FlatFileItemWriterBuilder<ProcessedLogEntry>()
        .name("processedLogEntryJsonWriter")
        .resource(new FileSystemResource(outputPath))
        .lineAggregator(item -> {
          try {
            return objectMapper.writeValueAsString(item);
          } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting item to JSON", e);
          }
        })
        .build();
  }

  public enum LogLevel {
    INFO, WARN, ERROR, DEBUG, UNKNOWN;

    public static LogLevel fromString(String level) {
      if (level == null || level.trim().isEmpty()) {
        return UNKNOWN;
      }
      try {
        return valueOf(level.toUpperCase());
      } catch (IllegalArgumentException e) {
        return UNKNOWN;
      }
    }
  }

  @Data
  public static class LogEntry {
    private String dateTime;
    private String level;
    private String message;
  }

  @Data
  public static class ProcessedLogEntry {
    private LocalDateTime dateTime;
    private LogLevel level;
    private String message;
    private String errorCode;
  }

  public static class LogEntryProcessor implements ItemProcessor<LogEntry, ProcessedLogEntry> {
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("ERROR_CODE\\[(\\w+)]");

    @Override
    public ProcessedLogEntry process(LogEntry item) {
      ProcessedLogEntry processedEntry = new ProcessedLogEntry();
      processedEntry.setDateTime(parseDateTime(item.getDateTime()));
      processedEntry.setLevel(parseLevel(item.getLevel()));
      processedEntry.setMessage(item.getMessage());
      processedEntry.setErrorCode(extractErrorCode(item.getMessage()));
      return processedEntry;
    }

    private LocalDateTime parseDateTime(String dateTime) {
      return LocalDateTime.parse(dateTime, ISO_FORMATTER);
    }

    private LogLevel parseLevel(String level) {
      return LogLevel.fromString(level);
    }

    private String extractErrorCode(String message) {
      if (message == null) {
        return null;
      }

      Matcher matcher = ERROR_CODE_PATTERN.matcher(message);
      if (matcher.find()) {
        return matcher.group(1);
      }
      // ERROR 문자열이 포함되어 있지만 패턴이 일치하지 않는 경우
      if (message.contains("ERROR")) {
        return "UNKNOWN_ERROR";
      }
      return null;
    }
  }
}