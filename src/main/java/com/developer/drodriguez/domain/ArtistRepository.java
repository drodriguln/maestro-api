package com.developer.drodriguez.domain;

import com.developer.drodriguez.model.Artist;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ArtistRepository extends MongoRepository<Artist, String> {
    boolean existsByName(String name);
    Artist findByName(String name);
}