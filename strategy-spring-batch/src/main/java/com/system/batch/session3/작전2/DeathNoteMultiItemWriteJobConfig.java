package com.system.batch.session3.작전2;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.MultiResourceItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemWriterBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DeathNoteMultiItemWriteJobConfig {

  @Bean
  public Job deathNoteMultiItemWriteJob(
      JobRepository jobRepositor, Step deathNoteMultiItemWriteStep
  ) {
    return new JobBuilder("deathNoteMultiItemWriteJob", jobRepositor)
        .start(deathNoteMultiItemWriteStep)
        .build();
  }

  @Bean
  public Step deathNoteMultiItemWriteStep(
      JobRepository jobRepositor,
      PlatformTransactionManager transactionManager,
      ListItemReader<DeathNote> deathNoteMultiItemListReader,
      MultiResourceItemWriter<DeathNote> multiResourceItemWriter
  ) {
    return new StepBuilder("deathNoteWriteStep", jobRepositor)
        .<DeathNote, DeathNote>chunk(10, transactionManager)
        .reader(deathNoteMultiItemListReader)
        .writer(multiResourceItemWriter)
        .build();
  }

  @Bean
  public ListItemReader<DeathNote> deathNoteMultiItemListReader() {
    List<DeathNote> deathNoteList = new ArrayList<>();
    for (int i = 1; i <= 15; i++) {
      String id = String.format("KILL-%03d", i);
      LocalDate date = LocalDate.now().plusDays(i);
      deathNoteList.add(new DeathNote(
          id,
          "피해자" + i,
          date.format(DateTimeFormatter.ISO_DATE),
          "처형사유" + i
      ));
    }
    return new ListItemReader<>(deathNoteList);
  }

  @Bean
  public FlatFileItemWriter<DeathNote> delegateItemWriter() {
    return new FlatFileItemWriterBuilder<DeathNote>()
        .name("delegateItemWriter")
        .formatted()
        .format("처형 ID: %s | 처형일자: %s | 피해자: %s | 사인: %s")
        .sourceType(DeathNote.class)
        .names("victimId", "executionDate", "victimName", "causeOfDeath")
        .headerCallback(writer -> writer.write("================= 처형 기록부 ================="))
        .footerCallback(writer -> writer.write("================= 처형 완료 =================="))
        .build();
  }

  @Bean
  @StepScope
  public MultiResourceItemWriter<DeathNote> multiResourceItemWriter(
      @Value("#{jobParameters['outputDir']}") String outputDir
  ) {
    return new MultiResourceItemWriterBuilder<DeathNote>()
        .name("multiResourceItemWriter")
        .resource(new FileSystemResource(outputDir + "/death_note"))
        .itemCountLimitPerResource(10)
        .delegate(delegateItemWriter())
        .resourceSuffixCreator(index -> String.format("_%03d.csv", index)) // index start from 1
        .build();
  }

  @Data
  @AllArgsConstructor
  public static class DeathNote {
    private String victimId;
    private String victimName;
    private String executionDate;
    private String causeOfDeath;
  }
}

