package com.drodriguez.maestro.api.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document
data class Album(@Id val id: String = UUID.randomUUID().toString(), var name: String, var songs: List<Song>) : Comparable<Album> {
    override fun compareTo(album: Album): Int {
        return this.name.compareTo(album.name)
    }
}

