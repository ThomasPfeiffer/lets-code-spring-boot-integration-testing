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
