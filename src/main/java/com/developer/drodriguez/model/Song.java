package com.developer.drodriguez.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public class Song implements Comparable<Song> {

    @Id
    private String id;
    private String name;
    private String trackNumber;
    private String year;
    private String fileId;
    private String artworkFileId;

    public Song(String name, String trackNumber, String year, String fileId, String artworkFileId) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.trackNumber = trackNumber;
        this.year = year;
        this.fileId = fileId;
        this.artworkFileId = artworkFileId;
    }

    @Override
    public int compareTo(Song song) {
        return Integer.parseInt(this.trackNumber) - Integer.parseInt(song.trackNumber);
    }

}

