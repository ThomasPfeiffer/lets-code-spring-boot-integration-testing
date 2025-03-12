package com.example.demo.cats;

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