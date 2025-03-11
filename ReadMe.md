# Lets Code - Spring Boot Integration Testing

This Repository contains exercises to set up an Integration Test Suite in Spring Boot Projects.

## Prerequisites

- Docker
- JDK
- Java IDE

## Part 1 - Setup Excersises

### TASK 1 - Setup a Spring Project

- Set up a Spring Project by going to [Spring Initializr](https://start.spring.io/).

- Add the following dependencies:

  - Spring Web
  - Spring Data MongoDB
  - Testcontainers

- Open the project in your IDE and run the preconfigured tests.

> [!NOTE]  
> [Testcontainers](https://testcontainers.com/) is a library which automatically starts Docker Containers before test execution and removes them afterwards.
> Because Testcontainers requires a Docker Daemon, it will not work out of the Box in Kubernetes (including lise Jenkins). You can start a Docker in Docker image as described in the [Service Desk](https://lise.atlassian.net/servicedesk/customer/portal/3/article/1995603973).

- Add the following class to a CatFeature.java file:

```java
import org.bson.types.ObjectId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
class CatFeature  {
    private final CatRepository catRepository;
    private final CataasClient cataasClient;

    public CatFeature(CatRepository catRepository, CataasClient cataasClient) {
        this.catRepository = catRepository;
        this.cataasClient = cataasClient;
    }

    @GetMapping("/cats")
    public Iterable<Cat> getCats() {
        return catRepository.findAll();
    }

    @GetMapping("/cats/{id}")
    public Cat getCat(@PathVariable String id) {
            return catRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/cats")
    public Cat createCat(@RequestBody NewCat newCat) {
        Cat cat = new Cat(
                ObjectId.get().toHexString(),
                newCat.name(),
                newCat.age(),
                cataasClient.getRandomCatInfo().url(),
                Instant.now()
        );
        return catRepository.save(cat);
    }

    @DeleteMapping("/cats/{id}")
    public void deleteCat(@PathVariable String id) {
        catRepository.deleteById(id);
    }
}

interface CataasClient {
    CataasResponse getRandomCatInfo();
}

@Component
class DefaultCataasClient implements CataasClient {
    private final RestClient restClient = RestClient.create();

    public CataasResponse getRandomCatInfo() {
        return restClient.get().uri("https://cataas.com/cat?json=true").retrieve().toEntity(CataasResponse.class).getBody();
    }
}

record CataasResponse(List<String> tags, String url) {}

interface CatRepository extends CrudRepository<Cat, String> {}

record Cat(
        String id,
        String name,
        int age,
        String imageUrl,
        Instant createdAt
) {}

record NewCat(
        String name,
        int age
) {}
```

- Write a Test that calls createCat to save a new Cat and checks if the Cat saved in the Database has the expected name and age, somthing like this:

```java
    @Test
	public void savesCat() {
		NewCat felix = new NewCat(
				"Felix",
				3
		);

		catController.createCat(felix);

		assertThat(catRepository.findAll()).singleElement().satisfies(
				savedCat -> {
					assertThat(savedCat.name()).isEqualTo(felix.name());
					assertThat(savedCat.age()).isEqualTo(felix.age());
				}
		);
	}
```

### TASK 2 - Cleaning up the Database after each Test

You generally want to keep tests as isolated as possible, e.g. by dropping the database before each test or resetting Fakes of external services.

- Implement a TestExecutionListener

```java
package com.example.demo;

import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

public class GlobalAfterEach implements TestExecutionListener {
    @Override
    public void afterTestMethod(TestContext testContext) {
        ApplicationContext applicationContext = testContext.getApplicationContext();
        MongoTemplate mongoTemplate = applicationContext.getBean(MongoTemplate.class);
        mongoTemplate.getDb().drop();
    }
}
```

Register your Listener on your test class:

```java
@TestExecutionListeners(
		listeners = {GlobalAfterEach.class},
		inheritListeners = false,
		mergeMode = MERGE_WITH_DEFAULTS)
```

### TASK 3 - Creating a custom Test Annotation

Repeating all annotations required for configuration can be cumbersome, unless you want to differentiate between each test, you can create a custom Annotation to avoid repeating yourself.

- Implement a custom "IntegrationTest" Annotation:

```java
package com.example.demo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@SpringBootTest
@TestExecutionListeners(
        listeners = {GlobalAfterEach.class},
        inheritListeners = false,
        mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
public @interface IntegrationTest {}
```

### TASK 4 - Setting up RestAssured

The current tests are directly invoking Controller Methods. That way, serialization and deserialization is not tested. To test the whole stack, you can use Rest Assured to perform http requests in a readable fashion.

- Set up Rest Assured
  - Gradle dependency `testImplementation("io.rest-assured:rest-assured:5.4.0")`
  - Configuration Class

```java
package com.example.demo;

import io.restassured.RestAssured;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.util.Objects;

@TestConfiguration
public class RestAssuredConfiguration implements TestExecutionListener {

	@Override
	public void beforeTestExecution(TestContext testContext) {
		RestAssured.port = Integer.parseInt(Objects.requireNonNull(testContext.getApplicationContext().getEnvironment().getProperty("local.server.port")));
	}
}
```

- Register the Configuration Class in your Test Annotation

```java
@TestExecutionListeners(
        listeners = {GlobalAfterEach.class, RestAssuredConfiguration.class},
        inheritListeners = false,
        mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
```

- Use Rest Assured in your tests, so that you do not directly use the Controller class in your test anymore

## Part 2 - Open Exercises

## TASK A - Write additional tests

Write additional tests for the Get by Id and delete endpoints.

## TASK B - Clock

The tests are currently only checking for the correct name and age of your Cat. The "createdAt" field should be set to the current time the moment a Cat is created. Right now, this is difficult to verify because you have no control over the current time.

You can fix this by using a java.time.Clock to get the current time and use a fixed Clock in your tests:

- Provide an instance of a Clock (Clock.systemUTC()) as a Bean in your Spring Application.
- Inject a Clock into your CatFeature class and use it to set the createdAt field (`Instant.now(clock)`).
- Override the Clock Bean in your Test Configuration with a fixed Clock (Clock.fixed(Instant.parse(".."))).
- Compare the createdAt field of the Cat with the "current" time of your fixed Test Clock.

## TASK C - File System

Using your local file system in tests can lead to problems, such as permission issues, missing clean ups or conflicts.
To avoid this, you can use an in memory file system such as [JimFS](https://github.com/google/jimfs).

- Provide an instance of java.nio.file.FileSystem as a Bean in your Spring Application.
- Write a feature that injects the FileSystem instance and uses it to write files to the file system.
- Replace the FileSystem instance with a JimFS instance in your Test Configuration.
- Write a test that checks if the file is written to the file system.

Use an in memory file system such as JimFS to avoid writing to the file system in tests. To do so, inject java.nio.file.FileSystem into your services. Replace the FileSystem instance with a JimFS instance for integration Tests.

## TASK D - Webservice Client

- The CatFeature is using a CataasClient to get a random Cat Image. This is an example for a dependency you cannot get in a controlled state. Therefore you should replace the CataasClient with a Fake in your tests.

- Create a Test implementation of the CataasClient interface
- Override the CataasClient Bean in your Test Configuration with the Test implementation
- Write a test that checks if the CatFeature is saving the returned Cat Image URL to the Cat Entity.
