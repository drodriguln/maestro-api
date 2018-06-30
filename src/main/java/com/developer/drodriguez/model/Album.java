package com.developer.drodriguez.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class Album implements Comparable<Album> {

    @Id
    private String id;
    private String name;
    private List<Song> songs;

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

