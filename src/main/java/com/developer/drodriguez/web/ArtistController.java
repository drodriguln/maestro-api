package com.developer.drodriguez.web;

import com.developer.drodriguez.model.*;
import com.developer.drodriguez.domain.ArtistRepository;
import com.developer.drodriguez.response.MaestroResponseBody;
import com.developer.drodriguez.response.MaestroResponseManager;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@RestController
public class ArtistController {

    @Autowired private ArtistRepository artistRepository;
    @Autowired private GridFsTemplate gridFsTemplate;
    @Autowired private MaestroResponseManager maestroResponseManager;

    /*
     *  Artists
     */

    @GetMapping("/artists")
    public ResponseEntity<MaestroResponseBody> getAllArtists() {
        List<Artist> repoArtists = artistRepository.findAll(new Sort(Sort.Direction.DESC, "name"));
        return !repoArtists.isEmpty()
            ? maestroResponseManager.createGetSuccessResponse(repoArtists)
            : maestroResponseManager.createGetFailureResponse();
    }

    @GetMapping("/artists/{artistId}")
    public ResponseEntity<MaestroResponseBody> getArtist(@PathVariable String artistId) {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        return repoArtistOptional.isPresent()
            ? maestroResponseManager.createGetSuccessResponse(repoArtistOptional.get())
            : maestroResponseManager.createGetFailureResponse();
    }

    @PostMapping("/artists")
    public ResponseEntity<MaestroResponseBody> postArtist(@RequestBody Artist artist) {
        Artist repoArtist = artistRepository.save(artist);
        return maestroResponseManager.createSaveSuccessResponse(repoArtist);
    }

    @PutMapping("/artists/{artistId}")
    public ResponseEntity<MaestroResponseBody> putArtist(@PathVariable String artistId, @RequestBody Artist artist) {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        if (repoArtistOptional.isPresent() && artistId.equals(repoArtistOptional.get().getId())) {
            if (artist.getAlbums() == null)
                artist.setAlbums(repoArtistOptional.get().getAlbums());
            artistRepository.delete(repoArtistOptional.get().getId());
        }
        Artist newArtist = artistRepository.save(new Artist(artistId, artist.getName(), artist.getAlbums()));
        return maestroResponseManager.createSaveSuccessResponse(newArtist);
    }

    @DeleteMapping("/artists/{artistId}")
    public ResponseEntity<MaestroResponseBody> deleteArtist(@PathVariable String artistId) {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        if (!repoArtistOptional.isPresent())
            return maestroResponseManager.createDeleteFailureResponse();
        Optional<Album> repoAlbumOptional = repoArtistOptional.get().getAlbums().stream()
                .filter(repoAlbum -> repoAlbum.getSongs() != null)
                .findFirst();
        if (repoAlbumOptional.isPresent() && repoAlbumOptional.get().getSongs() != null) {
            for (Song song : repoAlbumOptional.get().getSongs()) {
                deleteFile(song.getArtworkFileId());
                deleteFile(song.getFileId());
            }
        }
        artistRepository.delete(artistId);
        return maestroResponseManager.createDeleteSuccessResponse();
    }

    /*
     *  Albums
     */

    @GetMapping("/artists/{artistId}/albums")
    public ResponseEntity<MaestroResponseBody> getAllAlbums(@PathVariable String artistId) {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        if (repoArtistOptional.isPresent() && repoArtistOptional.get().getAlbums() != null) {
            List<Album> repoAlbums = repoArtistOptional.get().getAlbums();
            Collections.sort(repoAlbums);
            return maestroResponseManager.createGetSuccessResponse(repoAlbums);
        }
        return maestroResponseManager.createGetFailureResponse();
    }

    @GetMapping("/artists/{artistId}/albums/{albumId}")
    public ResponseEntity<MaestroResponseBody> getAlbum(@PathVariable String artistId, @PathVariable String albumId) {
        Optional<Album> repoAlbum = findAlbum(artistId, albumId);
        return repoAlbum.isPresent()
                ? maestroResponseManager.createGetSuccessResponse(repoAlbum.get())
                : maestroResponseManager.createGetFailureResponse();
    }

    @PostMapping("/artists/{artistId}/albums")
    public ResponseEntity<MaestroResponseBody> postAlbum(@PathVariable String artistId, @RequestBody Album album) {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        if (!repoArtistOptional.isPresent())
            return maestroResponseManager.createSaveFailureResponse();
        if (repoArtistOptional.get().getAlbums() == null)
            repoArtistOptional.get().setAlbums(new ArrayList<>());
        Album newAlbum = new Album(UUID.randomUUID().toString(), album.getName(), album.getSongs());
        repoArtistOptional.get().getAlbums().add(newAlbum);
        artistRepository.save(repoArtistOptional.get());
        return maestroResponseManager.createSaveSuccessResponse(newAlbum);
    }

    @PutMapping("/artists/{artistId}/albums/{albumId}")
    public ResponseEntity<MaestroResponseBody> putAlbum(@PathVariable String artistId, @PathVariable String albumId, @RequestBody Album album) {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        if (!repoArtistOptional.isPresent() || repoArtistOptional.get().getAlbums() == null)
            return maestroResponseManager.createSaveFailureResponse();
        Optional<Album> repoAlbumOptional = repoArtistOptional.get().getAlbums().stream()
                .filter(repoAlbum -> albumId.equals(repoAlbum.getId()))
                .findFirst();
        if (repoAlbumOptional.isPresent() && album.getSongs() == null)
            album.setSongs(repoAlbumOptional.get().getSongs());
        repoArtistOptional.get().getAlbums().remove(repoAlbumOptional.get());
        Album albumToPut = new Album(albumId, album.getName(), album.getSongs());
        repoArtistOptional.get().getAlbums().add(albumToPut);
        artistRepository.save(repoArtistOptional.get());
        return maestroResponseManager.createSaveSuccessResponse(albumToPut);
    }

    @DeleteMapping("/artists/{artistId}/albums/{albumId}")
    public ResponseEntity<MaestroResponseBody> deleteAlbum(@PathVariable String artistId, @PathVariable String albumId) throws IOException {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        if (!repoArtistOptional.isPresent() || repoArtistOptional.get().getAlbums() == null)
            return maestroResponseManager.createDeleteFailureResponse();
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
        return maestroResponseManager.createDeleteSuccessResponse();
    }

    /*
     *  Songs
     */

    @GetMapping("/artists/{artistId}/albums/{albumId}/songs")
    public ResponseEntity<MaestroResponseBody> getAllSongs(@PathVariable String artistId, @PathVariable String albumId) {
        Optional<Album> repoAlbumOptional = findAlbum(artistId, albumId);
        if (repoAlbumOptional.isPresent() && repoAlbumOptional.get().getSongs() != null) {
            List<Song> repoSongs = repoAlbumOptional.get().getSongs();
            Collections.sort(repoSongs);
            return maestroResponseManager.createGetSuccessResponse(repoSongs);
        }
        return maestroResponseManager.createGetFailureResponse();
    }

    @GetMapping("/artists/{artistId}/albums/{albumId}/songs/{songId}")
    public ResponseEntity<MaestroResponseBody> getSong(@PathVariable String artistId, @PathVariable String albumId, @PathVariable String songId) {
        Optional<Song> repoSongOptional = findSong(artistId, albumId, songId);
        return repoSongOptional.isPresent()
                ? maestroResponseManager.createGetSuccessResponse(repoSongOptional.get())
                : maestroResponseManager.createGetFailureResponse();
    }

    @PostMapping("/artists/{artistId}/albums/{albumId}/songs")
    public ResponseEntity<MaestroResponseBody> addSong(@PathVariable String artistId, @PathVariable String albumId,
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
            return maestroResponseManager.createSaveFailureResponse();
        Optional<Album> repoAlbumOptional = repoArtistOptional.get().getAlbums().stream()
                .filter(repoAlbum -> albumId.equals(repoAlbum.getId()))
                .findFirst();
        if (!repoAlbumOptional.isPresent())
            return maestroResponseManager.createSaveFailureResponse();
        if (repoAlbumOptional.get().getSongs() == null)
            repoAlbumOptional.get().setSongs(new ArrayList<>());
        List<Song> repoSongs = repoAlbumOptional.get().getSongs();
        repoSongs.add(newSong);
        repoAlbumOptional.get().setSongs(repoSongs);
        artistRepository.save(repoArtistOptional.get());
        return maestroResponseManager.createSaveSuccessResponse(repoSongs);
    }

    @PutMapping("/artists/{artistId}/albums/{albumId}/songs/{songId}")
    public ResponseEntity<MaestroResponseBody> putSong(@PathVariable String artistId, @PathVariable String albumId, @PathVariable String songId,
                        @RequestBody Song song) {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        if (!repoArtistOptional.isPresent() || repoArtistOptional.get().getAlbums() == null)
            return maestroResponseManager.createSaveFailureResponse();
        Optional<Album> repoAlbumOptional = repoArtistOptional.get().getAlbums().stream()
                .filter(repoAlbum -> albumId.equals(repoAlbum.getId()))
                .findFirst();
        if (!repoAlbumOptional.isPresent() || repoAlbumOptional.get().getSongs() == null)
            return maestroResponseManager.createSaveFailureResponse();
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
        return maestroResponseManager.createSaveSuccessResponse(song);
    }

    @DeleteMapping("/artists/{artistId}/albums/{albumId}/songs/{songId}")
    public ResponseEntity<MaestroResponseBody> deleteSong(@PathVariable String artistId, @PathVariable String albumId, @PathVariable String songId) throws IOException {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        if (!repoArtistOptional.isPresent() || repoArtistOptional.get().getAlbums() == null)
            return maestroResponseManager.createDeleteFailureResponse();
        Optional<Album> repoAlbumOptional = repoArtistOptional.get().getAlbums().stream()
                .filter(repoAlbum -> albumId.equals(repoAlbum.getId()))
                .findFirst();
        if (!repoAlbumOptional.isPresent() || repoAlbumOptional.get().getSongs() == null)
            return maestroResponseManager.createDeleteFailureResponse();
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
        return maestroResponseManager.createDeleteSuccessResponse();
    }

    @GetMapping(value = "/artists/{artistId}/albums/{albumId}/songs/{songId}/file")
    public ResponseEntity<byte[]> getSongFile(@PathVariable String artistId, @PathVariable String albumId, @PathVariable String songId) throws IOException {
        Optional<Song> repoSong = findSong(artistId, albumId, songId);
        return repoSong.isPresent()
                ? findFile(repoSong.get().getFileId())
                : maestroResponseManager.createGetFileFailureResponse();
    }

    @GetMapping(value = "/artists/{artistId}/albums/{albumId}/songs/{songId}/artwork")
    public ResponseEntity<byte[]> getArtworkFile(@PathVariable String artistId, @PathVariable String albumId, @PathVariable String songId) throws IOException {
        Optional<Song> repoSong = findSong(artistId, albumId, songId);
        return repoSong.isPresent()
                ? findFile(repoSong.get().getArtworkFileId())
                : maestroResponseManager.createGetFileFailureResponse();
    }

    ResponseEntity<byte[]> findFile(String fileId) throws IOException {
        GridFSDBFile gridFSDBFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(fileId)));
        if (gridFSDBFile == null)
            return maestroResponseManager.createGetFileFailureResponse();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        gridFSDBFile.writeTo(outputStream);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(gridFSDBFile.getContentType()));
        byte[] bytes = outputStream.toByteArray();
        return maestroResponseManager.createGetFileSuccessResponse(headers, bytes);
    }

    ResponseEntity<MaestroResponseBody> deleteFile(String fileId) {
        gridFsTemplate.delete(new Query(Criteria.where("_id").is(fileId)));
        return maestroResponseManager.createDeleteSuccessResponse();
    }

    Optional<Artist> findArtist(String artistId) {
        Artist repoArtist = artistRepository.findOne(artistId);
        return repoArtist != null
                ? Optional.of(repoArtist)
                : Optional.empty();
    }

    Optional<Album> findAlbum(String artistId, String albumId) {
        Optional<Artist> repoArtistOptional = findArtist(artistId);
        return repoArtistOptional.isPresent() && repoArtistOptional.get().getAlbums() != null
                ? repoArtistOptional.get().getAlbums().stream()
                .filter(repoAlbum -> repoAlbum.getId().equals(albumId))
                .findFirst()
                : Optional.empty();
    }

    Optional<Song> findSong(String artistId, String albumId, String songId) {
        Optional<Album> repoAlbumOptional = findAlbum(artistId, albumId);
        return repoAlbumOptional.isPresent() && repoAlbumOptional.get().getSongs() != null
                ? repoAlbumOptional.get().getSongs().stream()
                .filter(repoSong -> songId.equals(repoSong.getId()))
                .findFirst()
                : Optional.empty();
    }

}
