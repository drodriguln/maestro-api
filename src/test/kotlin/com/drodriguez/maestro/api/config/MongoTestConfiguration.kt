package com.drodriguez.maestro.api.config

import com.drodriguez.maestro.api.domain.ArtistRepository
import com.mongodb.MongoClient
import com.mongodb.ServerAddress
import de.bwaldvogel.mongo.MongoServer
import de.bwaldvogel.mongo.backend.memory.MemoryBackend
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDbFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoDbFactory
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@EnableMongoRepositories(basePackageClasses = [ArtistRepository::class])
class MongoTestConfiguration {
    @Bean
    fun mongoTemplate(mongoClient: MongoClient): MongoTemplate {
        return MongoTemplate(mongoDbFactory(mongoClient))
    }

    @Bean
    fun mongoDbFactory(mongoClient: MongoClient): MongoDbFactory {
        return SimpleMongoDbFactory(mongoClient, "test")
    }

    @Bean(destroyMethod = "shutdown")
    fun mongoServer(): MongoServer {
        val mongoServer = MongoServer(MemoryBackend())
        mongoServer.bind()
        return mongoServer
    }

    @Bean(destroyMethod = "close")
    fun mongoClient(mongoServer: MongoServer): MongoClient {
        return MongoClient(ServerAddress(mongoServer.localAddress))
    }
}