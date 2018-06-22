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
        if (repoArtist != null && artistId.equals(repoArtist.getId())) {
            if (artist.getAlbums() == null)
                artist.setAlbums(repoArtist.getAlbums());
            artistRepository.delete(repoArtist.getId());
        }
        Artist newArtist = artistRepository.save(new Artist(artistId, artist.getName(), artist.getAlbums()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, newArtist));
    }

    @DeleteMapping("/artists/{artistId}")
    public ResponseEntity<String> deleteArtist(@PathVariable String artistId) throws IOException {
        Artist repoArtist = findArtist(artistId);
        if (repoArtist.getAlbums() != null)
            for (Album album : repoArtist.getAlbums())
                if (album.getSongs() != null)
                    for (Song song : album.getSongs()) {
                        deleteFile(song.getArtworkFileId());
                        deleteFile(song.getFileId());
                    }
        artistRepository.delete(artistId);
        return ResponseEntity.ok(RESPONSE_ENTITY_DELETE_SUCCESSFUL);
    }

    /*
     *  Album
     */

    @GetMapping("/artists/{artistId}/albums")
    public ResponseEntity<List<Album>> getAllAlbums(@PathVariable String artistId) {
        List<Album> repoAlbums = artistRepository.findOne(artistId).getAlbums();
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
        if (repoArtist.getAlbums() == null)
            repoArtist.setAlbums(new ArrayList<>());
        Album newAlbum = new Album(UUID.randomUUID().toString(), album.getName(), album.getSongs());
        repoArtist.getAlbums().add(newAlbum);
        artistRepository.save(repoArtist);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, newAlbum));
    }

    @PutMapping("/artists/{artistId}/albums/{albumId}")
    public ResponseEntity<AjaxResponseBody> putAlbum(@PathVariable String artistId, @PathVariable String albumId, @RequestBody Album album) {
        Artist repoArtist = findArtist(artistId);
        if (repoArtist == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AjaxResponseBody(RESPONSE_ENTITY_ARTIST_NOT_FOUND));
        for (Album repoAlbum : repoArtist.getAlbums())
            if (albumId.equals(repoAlbum.getId())) {
                if (album.getSongs() == null)
                    album.setSongs(new ArrayList<>(repoAlbum.getSongs()));
                repoArtist.getAlbums().remove(repoAlbum);
                break;
            }
        Album newAlbum = new Album(albumId, album.getName(), album.getSongs());
        repoArtist.getAlbums().add(newAlbum);
        artistRepository.save(repoArtist);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, newAlbum));
    }

    @DeleteMapping("/artists/{artistId}/albums/{albumId}")
    public ResponseEntity<String> deleteAlbum(@PathVariable String artistId, @PathVariable String albumId) throws IOException {
        Artist repoArtist = findArtist(artistId);
        if (repoArtist == null)
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(RESPONSE_ENTITY_ARTIST_NOT_FOUND);
        if (repoArtist.getAlbums() != null)
            for (Album album : repoArtist.getAlbums())
                if (albumId.equals(album.getId()) && album.getSongs() != null) {
                    for (Song song : album.getSongs()) {
                        deleteFile(song.getArtworkFileId());
                        deleteFile(song.getFileId());
                    }
                    repoArtist.getAlbums().remove(album);
                    break;
                }
        artistRepository.save(repoArtist);
        return ResponseEntity.ok(RESPONSE_ENTITY_DELETE_SUCCESSFUL);
    }

    /*
     *  Songs
     */

    @GetMapping("/artists/{artistId}/albums/{albumId}/songs")
    public ResponseEntity<List<Song>> getAllSongs(@PathVariable String artistId, @PathVariable String albumId) {
        Album repoAlbum = findAlbum(artistId, albumId);
        if (repoAlbum != null && albumId.equals(repoAlbum.getId())) {
            Collections.sort(repoAlbum.getSongs());
            return ResponseEntity.ok(repoAlbum.getSongs());
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
        for (Album repoAlbum : repoArtist.getAlbums()) {
            if (albumId.equals(repoAlbum.getId())) {
                if (repoAlbum.getSongs() == null)
                    repoAlbum.setSongs(new ArrayList<>());
                List<Song> repoSongs = repoAlbum.getSongs();
                repoSongs.add(newSong);
                repoAlbum.setSongs(repoSongs);
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
        for (Album repoAlbum : repoArtist.getAlbums())
            if (albumId.equals(repoAlbum.getId())) {
                for (Song repoSong : repoAlbum.getSongs())
                    if (songId.equals(repoSong.getId())) {
                        if (song.getFileId() == null)
                            song.setFileId(repoSong.getFileId());
                        if (song.getArtworkFileId() == null)
                            song.setArtworkFileId(repoSong.getArtworkFileId());
                        List<Song> repoSongs = repoAlbum.getSongs();
                        repoSongs.remove(repoSong);
                        repoAlbum.setSongs(repoSongs);
                        break;
                    }
                List<Song> repoSongs = repoAlbum.getSongs();
                repoSongs.add(song);
                repoAlbum.setSongs(repoSongs);
            }
        artistRepository.save(repoArtist);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, song));
    }

    @DeleteMapping("/artists/{artistId}/albums/{albumId}/songs/{songId}")
    public ResponseEntity<String> deleteSong(@PathVariable String artistId, @PathVariable String albumId, @PathVariable String songId) throws IOException {
        Artist repoArtist = findArtist(artistId);
        if (repoArtist == null)
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(RESPONSE_ENTITY_ARTIST_NOT_FOUND);
        if (repoArtist.getAlbums() != null)
            for (Album album : repoArtist.getAlbums())
                if (albumId.equals(album.getId()) && album.getSongs() != null)
                    for (Song song : album.getSongs())
                        if (songId.equals(song.getId())) {
                            deleteFile(song.getArtworkFileId());
                            deleteFile(song.getFileId());
                            List<Song> repoSongs = album.getSongs();
                            repoSongs.remove(song);
                            album.setSongs(repoSongs);
                            break;
                        }
        artistRepository.save(repoArtist);
        return ResponseEntity.ok(RESPONSE_ENTITY_DELETE_SUCCESSFUL);
    }

    @GetMapping(value = "/artists/{artistId}/albums/{albumId}/songs/{songId}/file")
    public @ResponseBody ResponseEntity<byte[]> getSongFile(@PathVariable String artistId, @PathVariable String albumId, @PathVariable String songId) throws IOException {
        Song song = findSong(artistId, albumId, songId);
        return (song != null) ? findFile(song.getFileId()) : ResponseEntity.ok(null);
    }

    @GetMapping(value = "/artists/{artistId}/albums/{albumId}/songs/{songId}/artwork")
    public @ResponseBody ResponseEntity<byte[]> getArtworkFile(@PathVariable String artistId, @PathVariable String albumId, @PathVariable String songId) throws IOException {
        Song song = findSong(artistId, albumId, songId);
        return (song != null) ? findFile(song.getArtworkFileId()) : ResponseEntity.ok(null);
    }

    private Artist findArtist(String artistId) {
        return artistRepository.findOne(artistId);
    }

    private Album findAlbum(String artistId, String albumId) {
        Artist repoArtist = findArtist(artistId);
        if (repoArtist != null)
            for (Album repoAlbum : repoArtist.getAlbums())
                if (repoAlbum.getId().equals(albumId))
                    return repoAlbum;
        return null;
    }

    private Song findSong(String artistId, String albumId, String songId) {
        Album repoAlbum = findAlbum(artistId, albumId);
        if (repoAlbum != null)
            for (Song repoSong : repoAlbum.getSongs())
                if (repoSong.getId().equals(songId))
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
