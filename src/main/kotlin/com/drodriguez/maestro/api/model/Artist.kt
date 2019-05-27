package com.drodriguez.maestro.api.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document data class Artist(@Id val id: String, var name: String, var albums: List<Album>)

