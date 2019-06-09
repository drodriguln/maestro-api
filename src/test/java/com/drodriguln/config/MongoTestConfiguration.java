package com.drodriguln.config;

import com.drodriguln.domain.ArtistRepository;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackageClasses = {ArtistRepository.class})
public class MongoTestConfiguration {
    @Bean
    MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoDbFactory(mongoClient));
    }

    @Bean
    MongoDbFactory mongoDbFactory(MongoClient mongoClient) {
        return new SimpleMongoDbFactory(mongoClient, "test");
    }

    @Bean(destroyMethod = "shutdown")
    MongoServer mongoServer() {
        MongoServer mongoServer = new MongoServer(new MemoryBackend());
        mongoServer.bind();
        return mongoServer;
    }

    @Bean(destroyMethod = "close")
    MongoClient mongoClient(MongoServer mongoServer) {
        return new MongoClient(new ServerAddress(mongoServer.getLocalAddress()));
    }
}