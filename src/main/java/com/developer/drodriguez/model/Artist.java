package com.developer.drodriguez.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document
public class Artist {

    @Id
    private String id;
    private String name;
    private List<Album> albums;

    public Artist() {}

    public Artist(String id, String name, List<Album> albums) {
        this.id = id;
        this.name = name;
        this.albums = albums;
    }

    public Artist(String name, List<Album> albums) {
        this.name = name;
        this.albums = albums;
    }

}

