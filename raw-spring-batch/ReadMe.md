`gradle` 을 이용하여 프로젝트 생성
```shell
gradle init \
--type java-application \
--dsl groovy \
--test-framework junit-jupiter \
--java-version 17 \
--package com.system.batch.killbatchsystem \
--project-name kill-batch-system \
--no-split-project

```

`libs.versions.toml` 을 이용하여 프로젝트에서 사용한 의존성 명시
```toml
[versions]
guava = "33.2.1-jre"
junit-jupiter = "5.10.1"
spring-batch = "5.2.2"
h2 = "2.3.232"
junit-platform-launcher = "1.10.1"

[libraries]
guava = { module = "com.google.guava:guava", version.ref = "guava" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit-jupiter" }
spring-batch-core = { module = "org.springframework.batch:spring-batch-core", version.ref = "spring-batch" }
h2 = { module = "com.h2database:h2", version.ref = "h2" }
spring-batch-test = { module = "org.springframework.batch:spring-batch-test", version.ref = "spring-batch" }
junit-platform-launcher = { module = "org.junit.platform:junit-platform-launcher", version.ref = "junit-platform-launcher" }

```

`build.gradle` 은 기존 `toml`에서 명시한 의존성을 활용
```groovy
dependencies {
    implementation libs.spring.batch.core
    implementation libs.h2
    implementation libs.guava

    testImplementation libs.junit.jupiter
    testRuntimeOnly libs.junit.platform.launcher
    testImplementation libs.spring.batch.test
}

```
