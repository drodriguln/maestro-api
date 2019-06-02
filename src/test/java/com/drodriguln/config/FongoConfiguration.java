package com.drodriguln.config;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;

@ComponentScan
@Profile("fongo")
@ComponentScan(basePackages = "com.drodriguln")
public class FongoConfiguration extends AbstractMongoConfiguration {

    @Override
    protected String getDatabaseName() {
        return "testdb";
    }

    @Override
    @Bean
    public MongoClient mongoClient() {
        return new Fongo("Fongo").getMongo();
    }

}