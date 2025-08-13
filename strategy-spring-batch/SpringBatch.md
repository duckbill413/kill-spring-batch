전략적으로 Spring Boot 프로젝트 구성하기

Spring Initializer 를 활용하여 프로젝트 구성

```shell
curl 'https://start.spring.io/starter.tgz' \
    -d type=gradle-project \
    -d language=java \
    -d bootVersion=3.4.7 \
    -d groupId=com.system.batch \
    -d artifactId=kill-batch-system \
    -d name=kill-batch-system \
    -d packageName=com.system.batch \
    -d packaging=jar \
    -d javaVersion=17 \
    -d dependencies=batch,h2 \
    | tar -xzvf -
```

Spring Batch

## 섹션 1. SYSTEM INIT: 스프링 배치 종결의 서막

### Spring Batch Job & Step 실행

```shell
./gradlew bootRun --args='--spring.batch.job.name=systemTerminationSimulationJob'
```

## 섹션 2. SYSTEM BUILD: 스프링 배치 종결의 첫 걸음

### Tasklet Batch 실행

```shell
./gradlew bootRun --args='--spring.batch.job.name=zombieCleanupJob'
```

> `PlatformTransactionManager` 빈 직접 정의할 땐 주의가 필요하다.  
> Spring Batch 는 내부적으로 Job 과 Step 의 상태와 같은 메타데이터를 DB를 통해 관리한다.  
> 이 때도 트랜잭션이 사용되는데, 별도의 구성 변경 없이 PlatformTransactionManager 를 빈으로 정의할 경우,  
> Step 의 비즈니스 로직 처리를 위한 트랜잭션과 메타데이터 관리를 위한 트랜잭션이 서로 다른 성격임에도 불구하고 같은
> PlatformTransactionManager 빈을 사용하게 되어 의도치 않은 문제가 발생할 수 있음

### JobParameters

1. 커맨드라인에서 잡 파라미터 전달하기

- 스프링 부트 3 과 스프링 배치 5를 기준으로 커맨드라인에서 Job 을 실행할 때의 파라미터 전달 방식

   ```shell
   ./gradlew bootRun --args='--spring.batch.job.name=dataProcessingJob inputFilePath=/data/input/users.csv,java.lang.String'
  ```
  `key=value,type` 으로 이루어진 잡 파라미터 전달 형태

**JobParameters** 기본 표기법  
`parameterName=parameterValue,parameterType,identificationFlag`

- `parameterName`: 배치 Job 에서 파라미터를 찾을 때 사용할 key 값
- `parameterValue`: 파라미터의 실제 값
- `parameterType`: 파라미터의 타입 (`java.lang.String`, `java.lang.Integer` 와 같은 fully qualified name 사용). 파라미터 타입을 사용하지 않을 경우
  `String` 타입으로 가정
- `identificationFlag`: Spring Batch 에게 해당 파라미터가 JobInstance 식별에 사용될 파라미터인지 여부를 전달하는 것으로 `true` 이면 식별에 사용. 생략하는 경우
  `true` 가 default

```shell
./gradlew bootRun --args='--spring.batch.job.name=dataProcessingJob inputFilePath=/data/input/users.csv,java.lang.String userCount=5,java.lang.Integer,false'
```

- 커맨드라인을 이용한 JobParameters 전달 방법
- 여러 파라미터 전달시 공백을 활용

### 다양한 타입의 Job 파라미터 지배하기

1. 기본 데이터 타입 파라미터 전달

- SystemTerminatorConfig > processTerminatorJob 실행
  ```shell
  ./gradlew bootRun --args='--spring.batch.job.name=processTerminatorJob terminatorId=KILL-9,java.lang.String targetCount=5,java.lang.Integer'
  ```
- `@Value` 를 사용해서 잡 파라미터를 전달받기 위해서는 `@StepScope` 와 같은 어노테이션이 필수

2. 날짜와 시간 파라미터 전달 (`LocalDate`와 `LocalDateTime` 파라미터 전달)

- TerminatorConfig > terminatorJob 실행
  ```shell
  ./gradlew bootRun --args='--spring.batch.job.name=terminatorJob executionDate=2024-01-01,java.time.LocalDate startTime=2024-01-01T14:30:00,java.time.LocalDateTime'
  ```
- `executionDate`는 `yyyy-MM-dd`
- `startTime`은 `yyyy-MM-ddThh:mm:ss` 형식을 사용

3. `Enum` 파라미터 전달

- 먼저, Enum 을 정의

   ```java
   public enum QuestDifficulty {EASY, NORMAL, HARD, EXTREME}
   ```

- 커맨드라인 실행
  ```shell
  ./gradlew bootRun --args='--spring.batch.job.name=enumTerminationJob questDifficulty=HARD,com.system.batch.session2.EnumTerminatorConfig$QuestDifficulty'
  ```
  - `Enum` 클래스인 `QuestDifficulty` 를 `$` 기호를 통해 외부/내부 클래스를 구분

4. `POJO` 를 활용한 Job 파라미터 주입
    ```java
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
        
        @Value("#{jobParameters[securityLevel]")
        public void setSecurityLevel(int securityLevel) {
        this.securityLevel = securityLevel;
        }
    }
    ```

- `@Component` 애노테이션으로 Spring 빈 등록된다.
- 잡 파라미터를 전달 받기 위해서는 `@StepScope` 와 같은 애노테이션이 필요하다
- `@Value("#{jobParameters[...]}")` 어노테이션을 사용하여 다양한 방식으로 Job 을 주입받을 수 있다.
- POJO 스프링 배치 커맨드 실행
  ```shell
  ./gradlew bootRun --args='--spring.batch.job.name=pojoTerminationJob missionName=안산_데이터센터_침투,java.lang.String operationCommander=KILL-9 securityLevel=3,java.lang.Integer,false'
  ```

### 기존 파라미터 표기법의 한계

- Spring Batch 의 기본 파라미터 표기법에는 한계가 있다. 예를 들어, 다음과 같은 파라미터를 보자.
  - `infiltrationTargets=판교_서버실,안산_데이터센터,java.lang.String`
  - 파라미터 값에 `,` 가 포함되면 어떻게 될까?
  - `ClassNotFoundException` 에러가 발생한다.
  - **Spring Batch 5** 부터는 `JSON` 기반의 파라미터 표기법을 새롭게 제공한다.

### JobParameters 의 JSON 기반 표기법

```text
infiltrationTargets='{"value": "판교_서버실,안산_데이터센터", "type": "java.lang.String"}'
```

- 표기법 구성 요소들 `value`, `type`, `identifying` 은 기본 표기법과 동일한 의미를 가진다.
- `identifying` 은 생략이 가능하다.

### JSON 기반 파리미터 표기법 사용을 위한 준비

```groovy
dependencies {
    // 기존 다른 의존성들...
    implementation 'org.springframework.boot:spring-boot-starter-json'
}
```

- Configuration 클래스에 JobParameterConverter Bean 추가

```java

@Configuration
public class BatchConfig {

  @Bean
  public JsonJobParametersConverter jobParameterConverter() {
    return new JsonJobParametersConverter();
  }
}
```

- `@EnableBatchProcessing` 은 스프링 배치 5 이상에서는 필요하지 않음.

```shell
./gradlew bootRun --args="--spring.batch.job.name=jsonTerminationJob infiltrationTargets='{\"value\":\"판교서버실,안산데이터센터\",\"type\":\"java.lang.String\"}'"
```

```shell
java -jar kill-batch-system-0.0.1-SNAPSHOT.jar --spring.batch.job.name=jsonTerminationJob infiltrationTargets='{"value":"판교_서버실,안산_데이터센터","type":"java.lang.String"}'
```