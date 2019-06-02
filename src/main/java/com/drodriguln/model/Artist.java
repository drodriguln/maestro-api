package com.drodriguln.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class Artist {

    @Id
    private String id;
    private String name;
    private List<Album> albums;

    public Artist(String name, List<Album> albums) {
        this.name = name;
        this.albums = albums;
    }

}

