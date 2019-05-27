package com.drodriguez.maestro.api.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

import java.util.UUID

@Document
class Song(@Id val id: String, var name: String, var trackNumber: String?, var year: String?, var fileId: String, var artworkFileId: String?) : Comparable<Song> {

    constructor(name: String, trackNumber: String, year: String, fileId: String, artworkFileId: String)
        : this(UUID.randomUUID().toString(), name, trackNumber, year, fileId, artworkFileId)

    override fun compareTo(song: Song): Int {
        return Integer.parseInt(this.trackNumber) - Integer.parseInt(song.trackNumber)
    }
}

