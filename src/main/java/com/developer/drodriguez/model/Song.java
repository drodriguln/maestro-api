package com.developer.drodriguez.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Data
@Document
public class Song implements Comparable<Song> {

    @Id
    public String id;
    public String name;
    public String trackNumber;
    public String year;
    public String fileId;
    public String artworkFileId;

    public Song() {}

    public Song(String id, String name, String trackNumber, String year, String fileId, String artworkFileId) {
        this.id = id;
        this.name = name;
        this.trackNumber = trackNumber;
        this.year = year;
        this.fileId = fileId;
        this.artworkFileId = artworkFileId;
    }

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
        if (Integer.parseInt(this.trackNumber) < Integer.parseInt(song.trackNumber))
            return -1;
        if (Integer.parseInt(this.trackNumber) > Integer.parseInt(song.trackNumber))
            return 1;
        return 0;
    }

}

