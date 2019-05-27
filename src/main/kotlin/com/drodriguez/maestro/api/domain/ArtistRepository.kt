package com.drodriguez.maestro.api.domain

import com.drodriguez.maestro.api.model.Artist
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ArtistRepository : MongoRepository<Artist, String> {
    fun findByName(name: String): Artist
}