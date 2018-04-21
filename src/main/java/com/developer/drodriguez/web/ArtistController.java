package com.developer.drodriguez.web;

import com.developer.drodriguez.model.*;
import com.developer.drodriguez.domain.ArtistRepository;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.apache.tomcat.util.codec.binary.Base64.decodeBase64;
import static org.apache.tomcat.util.codec.binary.Base64.encodeBase64String;

//TO DO:
/*
 * - Allow post for brand new artist and album when up (in other words, you can't right now since the url requires ids beforehand).
 * - Research and implement BSON, or otherwise find more efficient ways to store byte strings in mongoDB.
 * - Simplify UI logic. Decouple logic.
 */

@RestController
public class ArtistController {

    @Autowired private ArtistRepository artistRepository;
    @Autowired private GridFsTemplate gridFsTemplate;

    private static final String RESPONSE_ENTITY_SAVE_SUCCESSFUL = "Object saved successfully.";
    private static final String RESPONSE_ENTITY_SAVE_UNSUCCESSFUL = "Could not successfully save the object.";
    private static final String RESPONSE_ENTITY_DELETE_SUCCESSFUL = "Object deleted successfully.";
    private static final String RESPONSE_ENTITY_ARTIST_NOT_FOUND = "Could not find an existing object to save to.";

    /*
     *  Artist
     */

    @GetMapping("/artists")
    public ResponseEntity<List<Artist>> getAllArtists() {
        List<Artist> repoArtist = artistRepository.findAll();
        Collections.sort(repoArtist);
        return ResponseEntity.ok(repoArtist);
    }

    @GetMapping("/artists/{artistId}")
    public ResponseEntity<Artist> getArtist(@PathVariable String artistId) {
        return ResponseEntity.ok(findArtist(artistId));
    }

    @PostMapping("/artists")
    public ResponseEntity<AjaxResponseBody> postArtist(@RequestBody Artist artist) {
        Artist newArtist = artistRepository.save(artist);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, newArtist));
    }

    @PutMapping("/artists/{artistId}")
    public ResponseEntity<AjaxResponseBody> putArtist(@PathVariable String artistId, @RequestBody Artist artist) {
        Artist repoArtist = findArtist(artistId);
        if (repoArtist != null && artistId.equals(repoArtist.id)) {
            if (artist.albums == null)
                artist.albums = new ArrayList<>(repoArtist.albums);
            artistRepository.delete(repoArtist.id);
        }
        Artist newArtist = artistRepository.save(new Artist(artistId, artist.name, artist.albums));
        return ResponseEntity.status(HttpStatus.CREATED).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, newArtist));
    }

    @DeleteMapping("/artists/{artistId}")
    public ResponseEntity<String> deleteArtist(@PathVariable String artistId) throws IOException {
        Artist repoArtist = findArtist(artistId);
        if (repoArtist.albums != null)
            for (Album album : repoArtist.albums)
                if (album.songs != null)
                    for (Song song : album.songs) {
                        deleteFile(song.artworkFileId);
                        deleteFile(song.fileId);
                    }
        artistRepository.delete(artistId);
        return ResponseEntity.status(HttpStatus.OK).body(RESPONSE_ENTITY_DELETE_SUCCESSFUL);
    }

    /*
     *  Album
     */

    @GetMapping("/artists/{artistId}/albums")
    public ResponseEntity<List<Album>> getAllAlbums(@PathVariable String artistId) {
        List<Album> repoAlbums = artistRepository.findOne(artistId).albums;
        Collections.sort(repoAlbums);
        return ResponseEntity.ok(repoAlbums);
    }

    @GetMapping("/artists/{artistId}/albums/{albumId}")
    public ResponseEntity<Album> getAlbum(@PathVariable String artistId, @PathVariable String albumId) {
        return ResponseEntity.ok(findAlbum(artistId, albumId));
    }

    @PostMapping("/artists/{artistId}/albums")
    public ResponseEntity<AjaxResponseBody> postAlbum(@PathVariable String artistId, @RequestBody Album album) {
        Artist repoArtist = findArtist(artistId);
        if (repoArtist == null)
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new AjaxResponseBody(RESPONSE_ENTITY_ARTIST_NOT_FOUND));
        if (repoArtist.albums == null)
            repoArtist.albums = new ArrayList<>();
        Album newAlbum = new Album(UUID.randomUUID().toString(), album.name, album.songs);
        repoArtist.albums.add(newAlbum);
        artistRepository.save(repoArtist);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, newAlbum));
    }

    @PutMapping("/artists/{artistId}/albums/{albumId}")
    public ResponseEntity<AjaxResponseBody> putAlbum(@PathVariable String artistId, @PathVariable String albumId, @RequestBody Album album) {
        Artist repoArtist = findArtist(artistId);
        if (repoArtist == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AjaxResponseBody(RESPONSE_ENTITY_ARTIST_NOT_FOUND));
        for (Album repoAlbum : repoArtist.albums)
            if (albumId.equals(repoAlbum.id)) {
                if (album.songs == null)
                    album.songs = new ArrayList<>(repoAlbum.songs);
                repoArtist.albums.remove(repoAlbum);
                break;
            }
        Album newAlbum = new Album(albumId, album.name, album.songs);
        repoArtist.albums.add(newAlbum);
        artistRepository.save(repoArtist);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, newAlbum));
    }

    @DeleteMapping("/artists/{artistId}/albums/{albumId}")
    public ResponseEntity<String> deleteAlbum(@PathVariable String artistId, @PathVariable String albumId) throws IOException {
        Artist repoArtist = findArtist(artistId);
        if (repoArtist == null)
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(RESPONSE_ENTITY_ARTIST_NOT_FOUND);
        if (repoArtist.albums != null)
            for (Album album : repoArtist.albums)
                if (albumId.equals(album.id) && album.songs != null) {
                    for (Song song : album.songs) {
                        deleteFile(song.artworkFileId);
                        deleteFile(song.fileId);
                    }
                    repoArtist.albums.remove(album);
                    break;
                }
        artistRepository.save(repoArtist);
        return ResponseEntity.status(HttpStatus.OK).body(RESPONSE_ENTITY_DELETE_SUCCESSFUL);
    }

    /*
     *  Songs
     */

    @GetMapping("/artists/{artistId}/albums/{albumId}/songs")
    public ResponseEntity<List<Song>> getAllSongs(@PathVariable String artistId, @PathVariable String albumId) {
        Album repoAlbum = findAlbum(artistId, albumId);
        if (repoAlbum != null && albumId.equals(repoAlbum.id)) {
            Collections.sort(repoAlbum.songs);
            return ResponseEntity.ok(repoAlbum.songs);
        }
        return ResponseEntity.ok(null);
    }

    @GetMapping("/artists/{artistId}/albums/{albumId}/songs/{songId}")
    public ResponseEntity<Song> getSong(@PathVariable String artistId, @PathVariable String albumId, @PathVariable String songId) {
        return ResponseEntity.ok(findSong(artistId, albumId, songId));
    }

    @PostMapping("/artists/{artistId}/albums/{albumId}/songs")
    public ResponseEntity<AjaxResponseBody> addSong(@PathVariable String artistId, @PathVariable String albumId,
                            @RequestParam String songName, @RequestParam String trackNumber,
                            @RequestParam String year, @RequestParam MultipartFile song,
                            @RequestParam MultipartFile artwork) throws IOException {
        String songFileName = song.getOriginalFilename();
        GridFSFile songFile = gridFsTemplate.store(song.getInputStream(), songFileName, song.getContentType());
        GridFSFile artworkFile = gridFsTemplate.store(artwork.getInputStream(), artwork.getOriginalFilename(), artwork.getContentType());
        if (songName == null || songName.isEmpty())
            songName = songFileName.contains(".") ? songFileName.substring(0, songFileName.lastIndexOf('.')) : songFileName;
        if (trackNumber == null || trackNumber.isEmpty())
            trackNumber = "0";
        Song newSong = new Song(songName, trackNumber, year, songFile.getId().toString(), artworkFile.getId().toString());
        Artist repoArtist = findArtist(artistId);
        if (repoArtist == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_UNSUCCESSFUL));
        for (Album repoAlbum : repoArtist.albums) {
            if (albumId.equals(repoAlbum.id)) {
                if (repoAlbum == null)
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_UNSUCCESSFUL, null));
                if (repoAlbum.songs == null)
                    repoAlbum.songs = new ArrayList<>();
                repoAlbum.songs.add(newSong);
                artistRepository.save(repoArtist);
                return ResponseEntity.status(HttpStatus.CREATED).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, newSong));
            }
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_UNSUCCESSFUL));
    }

    @PutMapping("/artists/{artistId}/albums/{albumId}/songs/{songId}")
    public ResponseEntity<AjaxResponseBody> putSong(@PathVariable String artistId, @PathVariable String albumId, @PathVariable String songId,
                        @RequestBody Song song) {
        Artist repoArtist = findArtist(artistId);
        if (repoArtist == null)
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new AjaxResponseBody(RESPONSE_ENTITY_ARTIST_NOT_FOUND));
        for (Album repoAlbum : repoArtist.albums)
            if (albumId.equals(repoAlbum.id)) {
                for (Song repoSong : repoAlbum.songs)
                    if (songId.equals(repoSong.id)) {
                        if (song.fileId == null)
                            song.fileId = repoSong.fileId;
                        if (song.artworkFileId == null)
                            song.artworkFileId = repoSong.artworkFileId;
                        repoAlbum.songs.remove(repoSong);
                        break;
                    }
                repoAlbum.songs.add(song);
            }
        artistRepository.save(repoArtist);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, song));
    }

    @DeleteMapping("/artists/{artistId}/albums/{albumId}/songs/{songId}")
    public ResponseEntity<String> deleteSong(@PathVariable String artistId, @PathVariable String albumId, @PathVariable String songId) throws IOException {
        Artist repoArtist = findArtist(artistId);
        if (repoArtist == null)
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(RESPONSE_ENTITY_ARTIST_NOT_FOUND);
        if (repoArtist.albums != null)
            for (Album album : repoArtist.albums)
                if (albumId.equals(album.id) && album.songs != null)
                    for (Song song : album.songs)
                        if (songId.equals(song.id)) {
                            deleteFile(song.artworkFileId);
                            deleteFile(song.fileId);
                            album.songs.remove(song);
                            break;
                        }
        artistRepository.save(repoArtist);
        return ResponseEntity.status(HttpStatus.OK).body(RESPONSE_ENTITY_DELETE_SUCCESSFUL);
    }

    @GetMapping(value = "/artists/{artistId}/albums/{albumId}/songs/{songId}/file")
    public @ResponseBody ResponseEntity<byte[]> getSongFile(@PathVariable String artistId, @PathVariable String albumId, @PathVariable String songId) throws IOException {
        Song song = findSong(artistId, albumId, songId);
        return (song != null) ? findFile(song.fileId) : ResponseEntity.ok(null);
    }

    @GetMapping(value = "/artists/{artistId}/albums/{albumId}/songs/{songId}/artwork")
    public @ResponseBody ResponseEntity<byte[]> getArtworkFile(@PathVariable String artistId, @PathVariable String albumId, @PathVariable String songId) throws IOException {
        Song song = findSong(artistId, albumId, songId);
        return (song != null) ? findFile(song.artworkFileId) : ResponseEntity.ok(null);
    }

    private Artist findArtist(String artistId) {
        return artistRepository.findOne(artistId);
    }

    private Album findAlbum(String artistId, String albumId) {
        Artist repoArtist = findArtist(artistId);
        if (repoArtist != null)
            for (Album repoAlbum : repoArtist.albums)
                if (repoAlbum.id.equals(albumId))
                    return repoAlbum;
        return null;
    }

    private Song findSong(String artistId, String albumId, String songId) {
        Album repoAlbum = findAlbum(artistId, albumId);
        if (repoAlbum != null)
            for (Song repoSong : repoAlbum.songs)
                if (repoSong.id.equals(songId))
                    return repoSong;
        return null;
    }

    private ResponseEntity<byte[]> findFile(String fileId) throws IOException {
        GridFSDBFile gridFSDBFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(fileId)));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        gridFSDBFile.writeTo(outputStream);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.parseMediaType(gridFSDBFile.getContentType()));
        if (gridFSDBFile != null)
            return new ResponseEntity<>(outputStream.toByteArray(), responseHeaders, HttpStatus.OK);
        return ResponseEntity.ok(null);
    }

    private ResponseEntity<byte[]> deleteFile(String fileId) throws IOException {
        gridFsTemplate.delete(new Query(Criteria.where("_id").is(fileId)));
        return ResponseEntity.ok(null);
    }

}
