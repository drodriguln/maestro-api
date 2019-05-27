package com.drodriguez.maestro.api.config
import com.drodriguez.maestro.api.domain.ArtistRepository
import com.github.fakemongo.Fongo
import com.mongodb.Mongo
import com.mongodb.MongoClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.config.AbstractMongoConfiguration
import org.springframework.data.mongodb.core.MongoTemplate

@Profile("fongo")
@ComponentScan(basePackages = ["com.drodriguez"])
class FongoConfiguration : AbstractMongoConfiguration() {

    override fun getDatabaseName(): String {
        return "mockdb"
    }

    @Bean
    override fun mongoClient(): MongoClient {
        return Fongo("mockdb").mongo
    }

}