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

