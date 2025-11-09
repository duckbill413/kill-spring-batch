# FlatFileItemReader

- `FlatFileItemReader` 는 플랫 파일(CSV, TSV) 로 부터 데이터를 읽어오는 Spring Batch 의 핵심 무기
- `FlatFileItemReader` 의 `read()`0 메서드의 핵심 동작은 크게 두 단계로 이루어진다.
  - 파일에서 한 줄을 읽어온다.
  - 읽어온 한 줄의 문자열을 우리가 사용할 객체로 변화해 리턴한다.

LineMapper 는 파일의 한 줄을 우리가 사용할 개체로 변신시킨다.  
Spring JDBC 의 RowMapper 와 유사하다고 할 수 있다.  
RowMapper 가 ResultSet 을 객체로 매핑하듯, LineMapper 는 파일에서 읽어온 한 줄의 텍스트를 도메인 객체로 매핑한다.

```text
에러ID,발생시각,심각도,프로세스ID,에러메시지
ERR001,2024-01-19 10:15:23,CRITICAL,1234,SYSTEM_CRASH
ERR002,2024-01-19 10:15:25,FATAL,1235,MEMORY_OVERFLOW
```

이걸 SystemFailure 객체로 변환하려면 몇 가지 까다로운 과정을 거쳐야 한다.

1. 쉼표로 구분된 각 데이터를 정확하게 분리해야 한다.
2. 분리한 데이터를 SystemFailure 객체의 각 프로퍼티에 정확하게 매핑해야 한다.

```java
public interface LineMapper<T> {
  T mapLine(String line, int lineNumber) throws Exception;
}
```

`mapLine` 이라는 이름의 이 메서드는 한 줄의 문자열과 그 줄의 번호를 입력 받아 우리가 원하는 객체(T) 로 변환한다.

---

## FlatFileItemReader 실습

1. 파일 준비

```shell
echo -e "에러ID,발생시각,심각도,프로세스ID,에러메시지\nERR001,2024-01-19 10:15:23,CRITICAL,1234,SYSTEM_CRASH\nERR002,2024-01-19 10:15:25,FATAL,1235,MEMORY_OVERFLOW" > system-failures.csv
```

2. 고정 길이 형식의 파일 읽기

```shell
./gradlew bootRun --args='--spring.batch.job.name=systemFailureJob inputFile=./src/main/java/com/system/batch/session3/작전1/system-failures.csv'
```

### 고정 길이 형식의 파일 읽기

**고정 길이 형식의 파일이란?**  
고정 길이 형식의 파일은 각 필드가 고정된 길이로 딱 맞춰진 텍스트 파일을 의미한다.  
여기서는 구분자란 존재하지 않는다.

```text
ERR001  2024-01-19 10:15:23  CRITICAL  1234  SYSTEM  CRASH DETECT \n
ERR002  2024-01-19 10:15:25  FATAL     1235  MEMORY  OVERFLOW FAIL\n
```

```java

@Bean
@StepScope
public FlatFileItemReader<SystemFailure> systemFailureItemReader(
    @Value("#{jobParameters['inputFile']}") String inputFile) {
  return new FlatFileItemReaderBuilder<SystemFailure>()
      .name("systemFailureItemReader")
      .resource(new FileSystemResource(inputFile))
      .fixedLength()
      .columns(new Range[]{
          new Range(1, 8),     // errorId: ERR001 + 공백 2칸
          new Range(9, 29),    // errorDateTime: 날짜시간 + 공백 2칸
          new Range(30, 39),   // severity: CRITICAL/FATAL + 패딩
          new Range(40, 45),   // processId: 1234 + 공백 2칸
          new Range(46, 66)    // errorMessage: 메시지 + \n
      })
      .names("errorId", "errorDateTime", "severity", "processId", "errorMessage")
      .targetType(SystemFailure.class)
      .build();
}
```

- `fixedLength()`: FlatFileItemReader 에게 읽어들일 파일이 고정 길이 형식임을 알리는 설정. 해당 메서드를 호출하면 `LineTokenizer`의 구현체로
  `FixedLengthTokenizer` 가 지정된다.
- `columns()`: 고정 길이 파일을 읽기 위해서는 정확한 위치를 FixedLengthTokenizer 에 전달한다. Range 배열은 각 필드의 시작과 끝 위치를 의미한다.
- `strict()`: strict 모드에서는 FixedLengthTokenizer 는 파일에서 읽은 라인의 길이를 엄격하게 검증한다. 파일에서 읽은 라인의 길이가 Range 에 지정된 최대 길이와 다를 경우
  예외를 발생시킨다.
- `names()`와 `targetType()`: 구분자로 구분된 형식의 파일을 처리할 때와 동일하게 동작한다.

### 프로퍼티 타입에 LocalDateTime 을 쓰고 싶다면?

`BeanWrapperFieldSetMapper`는 기본적인 타입 변환을 지원하지만, LocalDateTime 과 같은 복잡한 타입은 별도의 변환기가 필요하다.  
다행히 FlatFileItemReaderBuilder 의 `customEditors()` 메서들르 사용하면 커스텀 PropertyEditor 를 등록할 수 있다.

```java

@Bean
@StepScope
public FlatFileItemReader<SystemFailure> systemFailureItemReader(
    @Value("#{jobParameters['inputFile']}") String inputFile) {
  return new FlatFileItemReaderBuilder<SystemFailure>()
      // ... 기존 설정 동일 ...
      .customEditors(Map.of(LocalDateTime.class, dateTimeEditor()))
      .build();
}

private PropertyEditor dateTimeEditor() {
  return new PropertyEditorSupport() {
    @Override
    public void setAsText(String text) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      setValue(LocalDateTime.parse(text, formatter));
    }
  };
}

```

### RegexLine Tokenizer 사용하기

RegexLineTokenizer 를 사용하여 로그 파일에서 Thread 번호와 에러 메시지를 추출하는 예제를 살펴보자  
[LogAnalysisJobConfig](LogAnalysisJobConfig.java)

RegexLineTokenizer 구성

- 구조가 복잡하고 라인의 길이도 가변적인 경우, 정규식을 활용한 RegexLineTokenizer 가 유용한 도구가 된다.

```text
tokenizer.setRegex("\\[\\w+\\]\\[Thread-(\\d+)\\]\\[CPU: \\d+%\\] (.+)");
```

- `\\[\\\\w+\\]`: 대괄호 안의 로그 레벨(예: WARNING, ERROR)을 패턴으로 매칭한다. 이 부분은 분석 대상에서 제외된다.
- `\\[Thread-(\\\\d+)\\]`: 스레드 번호에 해당하는 두 번째 대괄호 안에서 Thread- 뒤에 나오는 숫자가 첫 번째 그룹으로 캡처된다.
- `\\[CPU: \\\\d+%\\]`: CPU 사용량을 나타내는 부분이다. 이건 로그 메시지 파싱엔 필요 없으니 건너뛴다.
- `(.+)`: 마지막으로 로그 메시지를 전부 가져오는 부분이다. 이게 두 번째 그룹으로 캡처된다.

`fieldSetMapper()` 로 LogEntry 매핑

- 커스텀 LineTokenizer 설정뿐만 아니라 FlatFileItemReaderBuilder 는 커스텀 FieldSetMapper 를 구성할 수 있는 유연한 메서드를 제공한다.

실행)

1. 파일 생성

```shell
echo -e "[WARNING][Thread-156][CPU: 78%] Thread pool saturation detected - 45/50 threads in use...\n[ERROR][Thread-157][CPU: 92%] Thread deadlock detected between Thread-157 and Thread-159\n[FATAL][Thread-159][CPU: 95%] Thread dump initiated - system unresponsive for 30s" > log.txt
```

2. bootRun 실행

```shell
./gradlew bootRun --args='--spring.batch.job.name=logAnalysisJob inputFile=src/main/java/com/system/batch/session3/작전1/log.txt'
```

---

## PatternMatchingCompositeLineMapper 사용하기

```text
ERROR,mysql-prod,OOM,2024-01-24T09:30:00,heap space killing spree,85%,/var/log/mysql
ABORT,spring-batch,MemoryLeak,2024-01-24T10:15:30,forced termination,-1,/usr/apps/batch,TERMINATED
COLLECT,heap-dump,PID-9012,2024-01-24T11:00:15,/tmp/heapdump
ERROR,redis-cache,SocketTimeout,2024-01-24T13:45:00,connection timeout,92%,/var/log/redis
ABORT,zombie-process,Deadlock,2024-01-24T13:46:20,kill -9 executed,-1,/proc/dead,TERMINATED
```

- 이 로그 파일에는 세 가지 유형의 시스템 처리 기록이 있다.
  - ERROR: 장애 발생 이벤트 (시스템의 비정상 징후)
    - 형식: `타입,애플리케이션,장애유형,발생시각,메시지,리소스사용률,로그경로`
  - ABORT: 프로세스 중단 이벤트 (비정상 프로세스 제거)
    - 형식: `타입,애플리케이션,장애유형,중단시각,중단사유,종료코드,프로세스경로,상태`
  - COLLECT: 사후 분석 이벤트
    - 형식: `타입,덤프종류,프로세스ID,수집시각,저장경로`
- 기본으로 사용되는 LineMapper 구현체인 DefaultLineMapper 는 하나의 LineTokenizer 와 하나의 `FieldSetMapper` 만을 가지고 있어 이러한 다양한 형식의 파일을 처리할 수
  없다.
- 이러 상황에서 `PatternMatchingCompositeLineMapper` 를 사용할 수 있다. 이 특별한 LineMapper 구현체는 Ant 스타일의 패턴 매칭을 지원하며, 각 라인의 패턴에 따라 다른
  LineTokenizer 와 FieldSetMapper 를 적용할 수 있다. 예를 들어 ERROR 로 직하는 모든 라인에는 `"ERROR*" 패턴에 지정된 `LineTokenizer` 와 `
  FieldSetMapper` 가 적용된다.

1. 시스템 로그 파일 생성

```shell
echo -e "ERROR,mysql-prod,OOM,2024-01-24T09:30:00,heap space killing spree,85%,/var/log/mysql\nABORT,spring-batch,MemoryLeak,2024-01-24T10:15:30,forced termination,-1,/usr/apps/batch,TERMINATED\nCOLLECT,heap-dump,PID-9012,2024-01-24T11:00:15,/tmp/heapdump\nERROR,redis-cache,SocketTimeout,2024-01-24T13:45:00,connection timeout,92%,/var/log/redis\nABORT,zombie-process,Deadlock,2024-01-24T13:46:20,kill -9 executed,-1,/proc/dead,TERMINATED" > system-events.log
```

2. bootRun 실행

```shell
./gradlew bootRun --args='--spring.batch.job.name=systemLogJob inputFile=src/main/java/com/system/batch/session3/작전1/system-events.log'
```

```text
SystemLogJobConfig.ErrorLog(super=SystemLogJobConfig.SystemLog(type=ERROR, timestamp=2024-01-24T09:30:00), application=mysql-prod, errorType=OOM, message=heap space killing spree, resourceUsage=85%, logPath=/var/log/mysql)
SystemLogJobConfig.AbortLog(super=SystemLogJobConfig.SystemLog(type=ABORT, timestamp=2024-01-24T10:15:30), application=spring-batch, errorType=MemoryLeak, message=forced termination, exitCode=-1, processPath=/usr/apps/batch, status=TERMINATED)
SystemLogJobConfig.CollectLog(super=SystemLogJobConfig.SystemLog(type=COLLECT, timestamp=2024-01-24T11:00:15), dumpType=heap-dump, processId=PID-9012, dumpPath=/tmp/heapdump)
SystemLogJobConfig.ErrorLog(super=SystemLogJobConfig.SystemLog(type=ERROR, timestamp=2024-01-24T13:45:00), application=redis-cache, errorType=SocketTimeout, message=connection timeout, resourceUsage=92%, logPath=/var/log/redis)
SystemLogJobConfig.AbortLog(super=SystemLogJobConfig.SystemLog(type=ABORT, timestamp=2024-01-24T13:46:20), application=zombie-process, errorType=Deadlock, message=kill -9 executed, exitCode=-1, processPath=/proc/dead, status=TERMINATED)
```

## RecordFieldSetMapper: Record 매핑 지원

- 파일 데이터를 record 로 매핑하는 간단한 예제를 살펴보자

```text
command,cpu,status
destroy,99,memory overflow
explode,100,cpu meltdown
collapse,95,disk burnout
```

위와 같은 데이터를 아래에 매핑

```java
public record SystemDeath(String command, int cpu, String status) {
}
```

```java
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;

@Bean
@StepScope
public FlatFileItemReader<SystemDeath> systemDeathReader(
    @Value("#{jobParameters['inputFile']}") String inputFile) {
  return new FlatFileItemReaderBuilder<SystemDeath>()
      .name("systemDeathReader")
      .resource(new FileSystemResource(inputFile))
      .delimited()
      .names("command", "cpu", "status")
      .targetType(SystemDeath.class)
      .linesToSkip(1)
      .build();
}
```

- `FlatFileItemReaderBuilder` 의 `targetType()` 메서드 호출을 보자. `targetType()` 메서드에 record 를 전달하면 Spring Batch 는 내부적으로
  `BeanWrapperFieldSetMapper` 대신 `BeanFieldSetMapper` 를 사용한다.
- `BeanWrapperFieldSetMapper` 가 객체의 `setter` 를 사용하여 데이터를 바인딩하는 것과 달리, `RecordFieldSetMapper` 는 `record` 의 canonical
  constructor 를 사용해 매핑한다.

## MultiResourceItemReader: 여러 파일 읽기

- `MultiResourceItemReader` 는 여러 파일을 순차적으로 읽을 수 있게 해주는 ItemReader 구현체이다.
- [[실습 클래스] SystemFailureMultiFileConfig](SystemFailureMultiFileConfig.java)
- `MultiResourceItemReader` 는 기본적으로 파일명의 알파벳 순서로 파일을 읽는다.
  - 파일 처리 순서를 다르게 설정하고 싶은 경우 `comparator()` 메서드를 사용하여 정렬할 수 있다.
  ```java
  .comparator((r1, r2) -> r2.getFilename().compareTo(r1.getFilename())
  ```
- `delegate()`
  - `ItemReader` 구성에서 `resource()` 호출이 없어졌다. 이는 resource 를 MultiResourceItemReader 에서 리소스를 지정해 주기 때문이다.

1. MultiResourceItemReader가 systemFailureFileReader에게 critical-failures.csv 로부터 데이터를 읽도록 명령한다.(파일명 알파벳 순서로 인해
   critical-failures.csv 가 먼저 선택됨)
2. critical-failures.csv를 전부 읽어 더 이상 읽어들일 레코드가 없을 경우, MultiResourceItemReader는 normal-failures.csv를 읽도록
   systemFailureFileReader에 명령한다.
3. normal-failures.csv까지 모두 읽으면 모든 파일 처리가 완료된다.

이러한 방식으로 MultiResourceItemReader는 여러 파일을 마치 하나의 큰 파일처럼 연속적으로 처리할 수 있게 해준다.

### 실습

1. 실습 파일 준비

```shell
# critical-failures.csv 생성
echo -e "에러ID,발생시각,심각도,프로세스ID,에러메시지\nERR001,2024-01-19 10:15:23,CRITICAL,1234,SYSTEM_CRASH\nERR002,2024-01-19 10:15:25,FATAL,1235,MEMORY_OVERFLOW\nERR003,2024-01-19 10:16:10,CRITICAL,1236,DATABASE_CORRUPTION" > critical-failures.csv

# normal-failures.csv 생성
echo -e "에러ID,발생시각,심각도,프로세스ID,에러메시지\nERR101,2024-01-19 10:20:30,WARN,2001,HIGH_CPU_USAGE\nERR102,2024-01-19 10:21:15,INFO,2002,CACHE_MISS\nERR103,2024-01-19 10:22:45,WARN,2003,SLOW_QUERY_DETECTED" > normal-failures.csv
```

2. 실행

```shell
./gradlew bootRun --args='--spring.batch.job.name=systemFailureJob inputFilePath=/path-to-kill-batch-system'
```

- path 수정이 필요함