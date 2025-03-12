package com.example.demo.cats;

import com.example.demo.IntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@IntegrationTest
class CatTests {

    @Autowired
    private CatRepository catRepository;

    @Test
    public void savesCat() {
        NewCat felix = new NewCat(
                "Felix",
                3
        );

        given().contentType(ContentType.JSON)
                .body(felix)
                .when()
                .post("/cats")
                .then().statusCode(200);

        assertThat(catRepository.findAll()).singleElement().satisfies(
                savedCat -> {
                    assertThat(savedCat.name()).isEqualTo(felix.name());
                    assertThat(savedCat.age()).isEqualTo(felix.age());
                }
        );
    }

	@Test
	public void returnsCats() {
		Cat whiskers = new Cat(
				"aldsknfkle",
				"Whiskers",
				4,
				"https://cataas.com/cat/ngfJ6lSzEdJQ9GO8",
				Instant.parse("2021-01-01T00:00:00Z")
		);
		catRepository.save(whiskers);

		given().contentType(ContentType.JSON)
				.when()
				.get("/cats")
				.then()
				.statusCode(200)
				.body("$", hasSize(1))
				.body("[0].name", equalTo(whiskers.name()))
				.body("[0].age", equalTo(whiskers.age()))
		;
	}
}
