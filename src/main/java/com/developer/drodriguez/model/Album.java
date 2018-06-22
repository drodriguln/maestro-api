package com.developer.drodriguez.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Data
@Document
public class Album implements Comparable<Album> {

    @Id
    private String id;
    private String name;
    private List<Song> songs;

    public Album() {}

    public Album(String id, String name, List<Song> songs) {
        this.id = id;
        this.name = name;
        this.songs = songs;
    }

    public Album(String name, List<Song> songs) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.songs = songs;
    }

    @Override
    public int compareTo(Album album) {
        return this.name.compareTo(album.name);
    }


}

