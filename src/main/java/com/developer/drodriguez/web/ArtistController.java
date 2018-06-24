package com.developer.drodriguez.web;

import com.developer.drodriguez.model.*;
import com.developer.drodriguez.domain.ArtistRepository;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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
import java.util.*;

import static org.apache.tomcat.util.codec.binary.Base64.decodeBase64;
import static org.apache.tomcat.util.codec.binary.Base64.encodeBase64String;

@RestController
public class ArtistController {

    @Autowired private ArtistRepository artistRepository;
    @Autowired private GridFsTemplate gridFsTemplate;

    private static final String RESPONSE_ENTITY_SAVE_SUCCESSFUL = "Object saved successfully.";
    private static final String RESPONSE_ENTITY_SAVE_UNSUCCESSFUL = "Could not successfully save the object.";
    private static final String RESPONSE_ENTITY_DELETE_SUCCESSFUL = "Object deleted successfully.";
    private static final String RESPONSE_ENTITY_DELETE_UNSUCCESSFUL = "Could not successfully delete the object.";

    /*
     *  Artists
     */

    @GetMapping("/artists")
    public ResponseEntity<List<Artist>> getAllArtists() {
        return ResponseEntity.ok(artistRepository.findAll(new Sort(Sort.Direction.DESC, "name")));
    }

    @GetMapping("/artists/{artistId}")
    public ResponseEntity<Artist> getArtist(@PathVariable String artistId) {
        Optional<Artist> repoArtist = findArtist(artistId);
        return ResponseEntity.ok(repoArtist.isPresent() ? repoArtist.get() : null);
    }

    @PostMapping("/artists")
    public ResponseEntity<AjaxResponseBody> postArtist(@RequestBody Artist artist) {
        Artist newArtist = artistRepository.save(artist);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, newArtist));
    }

    @PutMapping("/artists/{artistId}")
    public ResponseEntity<AjaxResponseBody> putArtist(@PathVariable String artistId, @RequestBody Artist artist) {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        if (repoArtistOptional.isPresent() && artistId.equals(repoArtistOptional.get().getId())) {
            if (artist.getAlbums() == null)
                artist.setAlbums(repoArtistOptional.get().getAlbums());
            artistRepository.delete(repoArtistOptional.get().getId());
        }
        Artist newArtist = artistRepository.save(new Artist(artistId, artist.getName(), artist.getAlbums()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, newArtist));
    }

    @DeleteMapping("/artists/{artistId}")
    public ResponseEntity<String> deleteArtist(@PathVariable String artistId) {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        if (!repoArtistOptional.isPresent())
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(RESPONSE_ENTITY_DELETE_UNSUCCESSFUL);
        Optional<Album> repoAlbumOptional = repoArtistOptional.get().getAlbums().stream()
                .filter(repoAlbum -> repoAlbum.getSongs() != null)
                .findFirst();
        if (repoAlbumOptional.isPresent() && repoAlbumOptional.get().getSongs() != null) {
            for (Song song : repoAlbumOptional.get().getSongs()) {
                try {
                    deleteFile(song.getArtworkFileId());
                    deleteFile(song.getFileId());
                } catch (IOException e) {
                    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(RESPONSE_ENTITY_DELETE_UNSUCCESSFUL);
                }
            }
        }
        artistRepository.delete(artistId);
        return ResponseEntity.ok(RESPONSE_ENTITY_DELETE_SUCCESSFUL);
    }

    /*
     *  Albums
     */

    @GetMapping("/artists/{artistId}/albums")
    public ResponseEntity<List<Album>> getAllAlbums(@PathVariable String artistId) {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        if (repoArtistOptional.isPresent() && repoArtistOptional.get().getAlbums() != null) {
            List<Album> repoAlbums = repoArtistOptional.get().getAlbums();
            Collections.sort(repoAlbums);
            return ResponseEntity.ok(repoAlbums);
        }
        return ResponseEntity.ok(null);
    }

    @GetMapping("/artists/{artistId}/albums/{albumId}")
    public ResponseEntity<Album> getAlbum(@PathVariable String artistId, @PathVariable String albumId) {
        Optional<Album> repoAlbum = findAlbum(artistId, albumId);
        return ResponseEntity.ok(repoAlbum.isPresent() ? repoAlbum.get() : null);
    }

    @PostMapping("/artists/{artistId}/albums")
    public ResponseEntity<AjaxResponseBody> postAlbum(@PathVariable String artistId, @RequestBody Album album) {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        if (!repoArtistOptional.isPresent())
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_UNSUCCESSFUL));
        if (repoArtistOptional.get().getAlbums() == null)
            repoArtistOptional.get().setAlbums(new ArrayList<>());
        Album newAlbum = new Album(UUID.randomUUID().toString(), album.getName(), album.getSongs());
        repoArtistOptional.get().getAlbums().add(newAlbum);
        artistRepository.save(repoArtistOptional.get());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, newAlbum));
    }

    @PutMapping("/artists/{artistId}/albums/{albumId}")
    public ResponseEntity<AjaxResponseBody> putAlbum(@PathVariable String artistId, @PathVariable String albumId, @RequestBody Album album) {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        if (!repoArtistOptional.isPresent() || repoArtistOptional.get().getAlbums() == null)
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_UNSUCCESSFUL));
        Optional<Album> repoAlbumOptional = repoArtistOptional.get().getAlbums().stream()
                    .filter(repoAlbum -> albumId.equals(repoAlbum.getId()))
                    .findFirst();
        if (repoAlbumOptional.isPresent() && album.getSongs() == null)
            album.setSongs(repoAlbumOptional.get().getSongs());
        repoArtistOptional.get().getAlbums().remove(repoAlbumOptional.get());
        Album albumToPut = new Album(albumId, album.getName(), album.getSongs());
        repoArtistOptional.get().getAlbums().add(albumToPut);
        artistRepository.save(repoArtistOptional.get());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, albumToPut));
    }

    @DeleteMapping("/artists/{artistId}/albums/{albumId}")
    public ResponseEntity<String> deleteAlbum(@PathVariable String artistId, @PathVariable String albumId) throws IOException {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        if (!repoArtistOptional.isPresent() || repoArtistOptional.get().getAlbums() == null)
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(RESPONSE_ENTITY_DELETE_UNSUCCESSFUL);
        Optional<Album> repoAlbumOptional = repoArtistOptional.get().getAlbums().stream()
                    .filter(repoAlbum -> albumId.equals(repoAlbum.getId()))
                    .findFirst();
        if (repoAlbumOptional.isPresent() && repoAlbumOptional.get().getSongs() != null) {
            for (Song song : repoAlbumOptional.get().getSongs()) {
                deleteFile(song.getArtworkFileId());
                deleteFile(song.getFileId());
            }
            repoArtistOptional.get().getAlbums().remove(repoAlbumOptional.get());
        }
        artistRepository.save(repoArtistOptional.get());
        return ResponseEntity.ok(RESPONSE_ENTITY_DELETE_SUCCESSFUL);
    }

    /*
     *  Songs
     */

    @GetMapping("/artists/{artistId}/albums/{albumId}/songs")
    public ResponseEntity<List<Song>> getAllSongs(@PathVariable String artistId, @PathVariable String albumId) {
        Optional<Album> repoAlbumOptional = findAlbum(artistId, albumId);
        if (repoAlbumOptional.isPresent() && repoAlbumOptional.get().getSongs() != null) {
            List<Song> repoSongs = repoAlbumOptional.get().getSongs();
            Collections.sort(repoSongs);
            return ResponseEntity.ok(repoSongs);
        }
        return ResponseEntity.ok(null);
    }

    @GetMapping("/artists/{artistId}/albums/{albumId}/songs/{songId}")
    public ResponseEntity<Song> getSong(@PathVariable String artistId, @PathVariable String albumId, @PathVariable String songId) {
        Optional<Song> repoSongOptional = findSong(artistId, albumId, songId);
        return ResponseEntity.ok(repoSongOptional.isPresent() ? repoSongOptional.get() : null);
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
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        if (!repoArtistOptional.isPresent() || repoArtistOptional.get().getAlbums() == null)
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_UNSUCCESSFUL));
        Optional<Album> repoAlbumOptional = repoArtistOptional.get().getAlbums().stream()
                .filter(repoAlbum -> albumId.equals(repoAlbum.getId()))
                .findFirst();
        if (!repoAlbumOptional.isPresent())
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_UNSUCCESSFUL));
        if (repoAlbumOptional.get().getSongs() == null)
            repoAlbumOptional.get().setSongs(new ArrayList<>());
        List<Song> repoSongs = repoAlbumOptional.get().getSongs();
        if (repoSongs == null)
            repoSongs = new ArrayList<Song>();
        repoSongs.add(newSong);
        repoAlbumOptional.get().setSongs(repoSongs);
        artistRepository.save(repoArtistOptional.get());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, newSong));
    }

    @PutMapping("/artists/{artistId}/albums/{albumId}/songs/{songId}")
    public ResponseEntity<AjaxResponseBody> putSong(@PathVariable String artistId, @PathVariable String albumId, @PathVariable String songId,
                        @RequestBody Song song) {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        if (!repoArtistOptional.isPresent() || repoArtistOptional.get().getAlbums() == null)
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_UNSUCCESSFUL));
        Optional<Album> repoAlbumOptional = repoArtistOptional.get().getAlbums().stream()
                .filter(repoAlbum -> albumId.equals(repoAlbum.getId()))
                .filter(repoAlbum -> repoAlbum.getSongs() != null)
                .findFirst();
        if (!repoAlbumOptional.isPresent() || repoAlbumOptional.get().getSongs() == null)
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_UNSUCCESSFUL));
        Optional<Song> repoSongOptional = repoAlbumOptional.get().getSongs().stream()
                    .filter(repoSong -> songId.equals(repoSong.getId()))
                    .findFirst();
        if (repoSongOptional.isPresent()) {
            if(song.getFileId() == null)
                song.setFileId(repoSongOptional.get().getFileId());
            if (song.getArtworkFileId() == null)
                song.setArtworkFileId(repoSongOptional.get().getArtworkFileId());
            List<Song> repoSongs = repoAlbumOptional.get().getSongs();
            repoSongs.remove(repoSongOptional.get());
            repoAlbumOptional.get().setSongs(repoSongs);
        }
        artistRepository.save(repoArtistOptional.get());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, song));
    }

    @DeleteMapping("/artists/{artistId}/albums/{albumId}/songs/{songId}")
    public ResponseEntity<String> deleteSong(@PathVariable String artistId, @PathVariable String albumId, @PathVariable String songId) throws IOException {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        if (!repoArtistOptional.isPresent() || repoArtistOptional.get().getAlbums() == null)
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(RESPONSE_ENTITY_DELETE_UNSUCCESSFUL);
        Optional<Album> repoAlbumOptional = repoArtistOptional.get().getAlbums().stream()
                .filter(repoAlbum -> albumId.equals(repoAlbum.getId()))
                .findFirst();
        if (!repoAlbumOptional.isPresent() || repoAlbumOptional.get().getSongs() == null)
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(RESPONSE_ENTITY_SAVE_UNSUCCESSFUL);
        Optional<Song> repoSongOptional = repoAlbumOptional.get().getSongs().stream()
                    .filter(repoSong -> songId.equals(repoSong.getId()))
                    .findFirst();
        if (repoSongOptional.isPresent() && songId.equals(repoSongOptional.get().getId())) {
            deleteFile(repoSongOptional.get().getArtworkFileId());
            deleteFile(repoSongOptional.get().getFileId());
            List<Song> repoSongs = repoAlbumOptional.get().getSongs();
            repoSongs.remove(repoSongOptional.get());
            repoAlbumOptional.get().setSongs(repoSongs);
        }
        artistRepository.save(repoArtistOptional.get());
        return ResponseEntity.ok(RESPONSE_ENTITY_DELETE_SUCCESSFUL);
    }

    @GetMapping(value = "/artists/{artistId}/albums/{albumId}/songs/{songId}/file")
    public @ResponseBody ResponseEntity<byte[]> getSongFile(@PathVariable String artistId, @PathVariable String albumId, @PathVariable String songId) throws IOException {
        Optional<Song> repoSong = findSong(artistId, albumId, songId);
        return repoSong.isPresent() ? findFile(repoSong.get().getFileId()) : ResponseEntity.ok(null);
    }

    @GetMapping(value = "/artists/{artistId}/albums/{albumId}/songs/{songId}/artwork")
    public @ResponseBody ResponseEntity<byte[]> getArtworkFile(@PathVariable String artistId, @PathVariable String albumId, @PathVariable String songId) throws IOException {
        Optional<Song> repoSong = findSong(artistId, albumId, songId);
        return repoSong.isPresent() ? findFile(repoSong.get().getArtworkFileId()) : ResponseEntity.ok(null);
    }

    protected Optional<Artist> findArtist(String artistId) {
        Artist repoArtist = artistRepository.findOne(artistId);
        return repoArtist != null ? Optional.of(repoArtist) : Optional.empty();
    }

    protected Optional<Album> findAlbum(String artistId, String albumId) {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        return repoArtistOptional.isPresent() && repoArtistOptional.get().getAlbums() != null
                ? repoArtistOptional.get().getAlbums().stream()
                     .filter(repoAlbum -> repoAlbum.getId().equals(albumId))
                     .findFirst()
                : Optional.empty();
    }

    protected Optional<Song> findSong(String artistId, String albumId, String songId) {
        Optional<Album> repoAlbumOptional = findAlbum(artistId, albumId);
        return repoAlbumOptional.isPresent() && repoAlbumOptional.get().getSongs() != null
                ? repoAlbumOptional.get().getSongs().stream()
                    .filter(repoSong -> songId.equals(repoSong.getId()))
                    .findFirst()
                : Optional.empty();
    }

    protected ResponseEntity<byte[]> findFile(String fileId) throws IOException {
        GridFSDBFile gridFSDBFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(fileId)));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        gridFSDBFile.writeTo(outputStream);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.parseMediaType(gridFSDBFile.getContentType()));
        if (gridFSDBFile != null)
            return new ResponseEntity<>(outputStream.toByteArray(), responseHeaders, HttpStatus.OK);
        return ResponseEntity.ok(null);
    }

    protected ResponseEntity<byte[]> deleteFile(String fileId) throws IOException {
        gridFsTemplate.delete(new Query(Criteria.where("_id").is(fileId)));
        return ResponseEntity.ok(null);
    }

}
