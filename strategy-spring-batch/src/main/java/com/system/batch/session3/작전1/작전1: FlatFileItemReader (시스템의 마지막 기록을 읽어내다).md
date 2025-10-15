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

---
