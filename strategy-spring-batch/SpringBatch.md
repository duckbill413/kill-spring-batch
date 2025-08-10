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