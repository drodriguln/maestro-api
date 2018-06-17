package com.developer.drodriguez.config;

import com.developer.drodriguez.domain.ArtistRepository;
import com.github.fakemongo.Fongo;
import com.mongodb.Mongo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;

@ComponentScan
@Profile("fongo")
@ComponentScan(basePackages = "com.developer.drodriguez")
public class FongoConfiguration extends AbstractMongoConfiguration {

    @Override
    protected String getDatabaseName() {
        return "fongo-test-db";
    }

    @Override
    @Bean
    public Mongo mongo() {
        return new Fongo("Fongo").getMongo();
    }

}