package com.drodriguez.maestro.api.web

import com.drodriguez.maestro.api.domain.ArtistRepository
import com.drodriguez.maestro.api.model.Album
import com.drodriguez.maestro.api.model.Artist
import com.drodriguez.maestro.api.model.Song
import com.drodriguez.maestro.api.response.MaestroResponseBody
import com.drodriguez.maestro.api.response.MaestroResponseManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.gridfs.GridFsOperations
import org.springframework.data.mongodb.gridfs.GridFsResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

import java.io.*
import java.util.*

@CrossOrigin
@RestController
class ArtistController {

    @Autowired
    private lateinit var artistRepository: ArtistRepository
    @Autowired
    private lateinit var gridFsOperations: GridFsOperations
    @Autowired
    private lateinit var maestroResponseManager: MaestroResponseManager

    /*
     *  Artists
     */

    val allArtists: ResponseEntity<MaestroResponseBody>
        @GetMapping("/artists")
        get() {
            val repoArtists = artistRepository.findAll(Sort(Sort.Direction.DESC, "name"))
            return if (repoArtists.isNotEmpty())
                maestroResponseManager.createGetSuccessResponse(repoArtists)
            else
                maestroResponseManager.createGetFailureResponse()
        }

    @GetMapping("/artists/{artistId}")
    fun getArtist(@PathVariable artistId: String): ResponseEntity<MaestroResponseBody> {
        val repoArtistOptional = findArtist(artistId)
        return if (repoArtistOptional.isPresent())
            maestroResponseManager.createGetSuccessResponse(repoArtistOptional.get())
        else
            maestroResponseManager.createGetFailureResponse()
    }

    @PostMapping("/artists")
    fun postArtist(@RequestBody artist: Artist): ResponseEntity<MaestroResponseBody> {
        val repoArtist = artistRepository.save(artist)
        return maestroResponseManager.createSaveSuccessResponse(repoArtist)
    }

    @PutMapping("/artists/{artistId}")
    fun putArtist(@PathVariable artistId: String, @RequestBody artist: Artist): ResponseEntity<MaestroResponseBody> {
        val repoArtistOptional = findArtist(artistId)
        if (repoArtistOptional.isPresent() && artistId == repoArtistOptional.get().id) {
            if (artist.albums == null)
                artist.albums = repoArtistOptional.get().albums
            artistRepository.deleteById(repoArtistOptional.get().id)
        }
        val newArtist = artistRepository.save(Artist(artistId, artist.name, artist.albums))
        return maestroResponseManager.createSaveSuccessResponse(newArtist)
    }

    @DeleteMapping("/artists/{artistId}")
    fun deleteArtist(@PathVariable artistId: String): ResponseEntity<MaestroResponseBody> {
        val repoArtistOptional = findArtist(artistId)
        if (!repoArtistOptional.isPresent())
            return maestroResponseManager.createDeleteFailureResponse()
        val repoAlbumOptional = repoArtistOptional.get().albums.stream()
                .filter { repoAlbum -> repoAlbum.songs != null }
                .findFirst()
        if (repoAlbumOptional.isPresent() && repoAlbumOptional.get().songs != null) {
            for (song in repoAlbumOptional.get().songs) {
                if (song.artworkFileId != null)
                    deleteFile(song.artworkFileId!!)
                deleteFile(song.fileId)
            }
        }
        artistRepository.deleteById(artistId)
        return maestroResponseManager.createDeleteSuccessResponse()
    }

    /*
     *  Albums
     */

    @GetMapping("/artists/{artistId}/albums")
    fun getAllAlbums(@PathVariable artistId: String): ResponseEntity<MaestroResponseBody> {
        val repoArtistOptional = findArtist(artistId)
        if (repoArtistOptional.isPresent() && repoArtistOptional.get().albums != null) {
            val repoAlbums = repoArtistOptional.get().albums
            Collections.sort(repoAlbums)
            return maestroResponseManager.createGetSuccessResponse(repoAlbums)
        }
        return maestroResponseManager.createGetFailureResponse()
    }

    @GetMapping("/artists/{artistId}/albums/{albumId}")
    fun getAlbum(@PathVariable artistId: String, @PathVariable albumId: String): ResponseEntity<MaestroResponseBody> {
        val repoAlbum = findAlbum(artistId, albumId)
        return if (repoAlbum.isPresent())
            maestroResponseManager.createGetSuccessResponse(repoAlbum.get())
        else
            maestroResponseManager.createGetFailureResponse()
    }

    @PostMapping("/artists/{artistId}/albums")
    fun postAlbum(@PathVariable artistId: String, @RequestBody album: Album): ResponseEntity<MaestroResponseBody> {
        val repoArtistOptional = findArtist(artistId)
        if (!repoArtistOptional.isPresent())
            return maestroResponseManager.createSaveFailureResponse()
        if (repoArtistOptional.get().albums == null)
            repoArtistOptional.get().albums = emptyList()
        val newAlbum = Album(UUID.randomUUID().toString(), album.name, album.songs)
        repoArtistOptional.get().albums += listOf(newAlbum)
        artistRepository.save(repoArtistOptional.get())
        return maestroResponseManager.createSaveSuccessResponse(newAlbum)
    }

    @PutMapping("/artists/{artistId}/albums/{albumId}")
    fun putAlbum(@PathVariable artistId: String, @PathVariable albumId: String, @RequestBody album: Album): ResponseEntity<MaestroResponseBody> {
        val repoArtistOptional = findArtist(artistId)
        if (!repoArtistOptional.isPresent() || repoArtistOptional.get().albums == null)
            return maestroResponseManager.createSaveFailureResponse()
        val repoAlbumOptional = repoArtistOptional.get().albums.stream()
                .filter { repoAlbum -> albumId == repoAlbum.id }
                .findFirst()
        if (repoAlbumOptional.isPresent() && album.songs == null)
            album.songs = repoAlbumOptional.get().songs
        repoArtistOptional.get().albums -= repoAlbumOptional.get()
        val albumToPut = Album(albumId, album.name, album.songs)
        repoArtistOptional.get().albums += albumToPut
        artistRepository.save(repoArtistOptional.get())
        return maestroResponseManager.createSaveSuccessResponse(albumToPut)
    }

    @DeleteMapping("/artists/{artistId}/albums/{albumId}")
    @Throws(IOException::class)
    fun deleteAlbum(@PathVariable artistId: String, @PathVariable albumId: String): ResponseEntity<MaestroResponseBody> {
        val repoArtistOptional = findArtist(artistId)
        if (!repoArtistOptional.isPresent() || repoArtistOptional.get().albums == null)
            return maestroResponseManager.createDeleteFailureResponse()
        val repoAlbumOptional = repoArtistOptional.get().albums.stream()
                .filter { repoAlbum -> albumId == repoAlbum.id }
                .findFirst()
        if (repoAlbumOptional.isPresent() && repoAlbumOptional.get().songs != null) {
            for (song in repoAlbumOptional.get().songs) {
                if (song.artworkFileId != null)
                    deleteFile(song.artworkFileId!!)
                deleteFile(song.fileId)
            }
            repoArtistOptional.get().albums -= repoAlbumOptional.get()
        }
        artistRepository.save(repoArtistOptional.get())
        return maestroResponseManager.createDeleteSuccessResponse()
    }

    /*
     *  Songs
     */

    @GetMapping("/artists/{artistId}/albums/{albumId}/songs")
    fun getAllSongs(@PathVariable artistId: String, @PathVariable albumId: String): ResponseEntity<MaestroResponseBody> {
        val repoAlbumOptional = findAlbum(artistId, albumId)
        if (repoAlbumOptional.isPresent() && repoAlbumOptional.get().songs != null) {
            val repoSongs = repoAlbumOptional.get().songs
            Collections.sort(repoSongs)
            return maestroResponseManager.createGetSuccessResponse(repoSongs)
        }
        return maestroResponseManager.createGetFailureResponse()
    }

    @GetMapping("/artists/{artistId}/albums/{albumId}/songs/{songId}")
    fun getSong(@PathVariable artistId: String, @PathVariable albumId: String, @PathVariable songId: String): ResponseEntity<MaestroResponseBody> {
        val repoSongOptional = findSong(artistId, albumId, songId)
        return if (repoSongOptional.isPresent())
            maestroResponseManager.createGetSuccessResponse(repoSongOptional.get())
        else
            maestroResponseManager.createGetFailureResponse()
    }

    @PostMapping("/artists/{artistId}/albums/{albumId}/songs")
    @Throws(IOException::class)
    fun addSong(@PathVariable artistId: String, @PathVariable albumId: String,
                @RequestParam songName: String?, @RequestParam trackNumber: String?,
                @RequestParam year: String, @RequestParam song: MultipartFile,
                @RequestParam artwork: MultipartFile): ResponseEntity<MaestroResponseBody> {
        var newSongName = songName
        var newTrackNumber = trackNumber
        val songFileName = song.originalFilename
        val songFileId = gridFsOperations.store(song.inputStream, songFileName, song.contentType)
        val artworkFileId = gridFsOperations.store(artwork.inputStream, artwork.originalFilename, artwork.contentType)
        if (newSongName == null || newSongName.isEmpty())
            newSongName = if (songFileName!!.contains(".")) songFileName.substring(0, songFileName.lastIndexOf('.')) else songFileName
        if (newTrackNumber == null || newTrackNumber.isEmpty())
            newTrackNumber = "0"
        val newSong = Song(newSongName!!, newTrackNumber, year, songFileId.toString(), artworkFileId.toString())
        val repoArtistOptional = findArtist(artistId)
        if (!repoArtistOptional.isPresent() || repoArtistOptional.get().albums == null)
            return maestroResponseManager.createSaveFailureResponse()
        val repoAlbumOptional = repoArtistOptional.get().albums.stream()
                .filter { repoAlbum -> albumId == repoAlbum.id }
                .findFirst()
        if (!repoAlbumOptional.isPresent())
            return maestroResponseManager.createSaveFailureResponse()
        if (repoAlbumOptional.get().songs == null)
            repoAlbumOptional.get().songs = emptyList()
        val repoSongs = repoAlbumOptional.get().songs
        repoAlbumOptional.get().songs += newSong
        artistRepository.save(repoArtistOptional.get())
        return maestroResponseManager.createSaveSuccessResponse(repoSongs)
    }

    @PutMapping("/artists/{artistId}/albums/{albumId}/songs/{songId}")
    fun putSong(@PathVariable artistId: String, @PathVariable albumId: String, @PathVariable songId: String,
                @RequestBody song: Song): ResponseEntity<MaestroResponseBody> {
        val repoArtistOptional = findArtist(artistId)
        if (!repoArtistOptional.isPresent() || repoArtistOptional.get().albums == null)
            return maestroResponseManager.createSaveFailureResponse()
        val repoAlbumOptional = repoArtistOptional.get().albums.stream()
                .filter { repoAlbum -> albumId == repoAlbum.id }
                .findFirst()
        if (!repoAlbumOptional.isPresent() || repoAlbumOptional.get().songs == null)
            return maestroResponseManager.createSaveFailureResponse()
        val repoSongOptional = repoAlbumOptional.get().songs.stream()
                .filter { repoSong -> songId == repoSong.id }
                .findFirst()
        if (repoSongOptional.isPresent()) {
            if (song.fileId == null)
                song.fileId = repoSongOptional.get().fileId
            if (song.fileId == null)
                song.artworkFileId = repoSongOptional.get().artworkFileId
            repoAlbumOptional.get().songs = repoAlbumOptional.get().songs - repoSongOptional.get()
        }
        artistRepository.save(repoArtistOptional.get())
        return maestroResponseManager.createSaveSuccessResponse(song)
    }

    @DeleteMapping("/artists/{artistId}/albums/{albumId}/songs/{songId}")
    @Throws(IOException::class)
    fun deleteSong(@PathVariable artistId: String, @PathVariable albumId: String, @PathVariable songId: String): ResponseEntity<MaestroResponseBody> {
        val repoArtistOptional = findArtist(artistId)
        if (!repoArtistOptional.isPresent() || repoArtistOptional.get().albums == null)
            return maestroResponseManager.createDeleteFailureResponse()
        val repoAlbumOptional = repoArtistOptional.get().albums.stream()
                .filter { repoAlbum -> albumId == repoAlbum.id }
                .findFirst()
        if (!repoAlbumOptional.isPresent() || repoAlbumOptional.get().songs == null)
            return maestroResponseManager.createDeleteFailureResponse()
        val repoSongOptional = repoAlbumOptional.get().songs.stream()
                .filter { repoSong -> songId == repoSong.id }
                .findFirst()
        if (repoSongOptional.isPresent() && songId == repoSongOptional.get().id) {
            if (repoSongOptional.get().artworkFileId != null)
                deleteFile(repoSongOptional.get().artworkFileId!!)
            deleteFile(repoSongOptional.get().fileId)
            repoAlbumOptional.get().songs = repoAlbumOptional.get().songs - repoSongOptional.get()
        }
        artistRepository.save(repoArtistOptional.get())
        return maestroResponseManager.createDeleteSuccessResponse()
    }

    @GetMapping("/artists/{artistId}/albums/{albumId}/songs/{songId}/file")
    @Throws(IOException::class)
    fun getSongFile(@PathVariable artistId: String, @PathVariable albumId: String, @PathVariable songId: String): ResponseEntity<GridFsResource> {
        val repoSong = findSong(artistId, albumId, songId)
        return if (repoSong.isPresent())
            findFile(repoSong.get().fileId)
        else
            maestroResponseManager.createGetFileFailureResponse()
    }

    @GetMapping("/artists/{artistId}/albums/{albumId}/songs/{songId}/artwork")
    @Throws(IOException::class)
    fun getArtworkFile(@PathVariable artistId: String, @PathVariable albumId: String, @PathVariable songId: String): ResponseEntity<GridFsResource> {
        val repoSong = findSong(artistId, albumId, songId)
        return if (repoSong.isPresent() && repoSong.get().artworkFileId != null)
            findFile(repoSong.get().artworkFileId!!)
        else
            maestroResponseManager.createGetFileFailureResponse()
    }

    @Throws(IOException::class)
    internal fun findFile(fileId: String): ResponseEntity<GridFsResource> {
        val gridFsFile = gridFsOperations.findOne(Query(Criteria.where("_id").`is`(fileId)))
                ?: return maestroResponseManager.createGetFileFailureResponse()
        val file = gridFsOperations.getResource(gridFsFile)
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(file.contentType)
        return maestroResponseManager.createGetFileSuccessResponse(headers, file)
    }

    internal fun deleteFile(fileId: String): ResponseEntity<MaestroResponseBody> {
        gridFsOperations.delete(Query(Criteria.where("_id").`is`(fileId)))
        return maestroResponseManager.createDeleteSuccessResponse()
    }

    internal fun findArtist(artistId: String): Optional<Artist> {
        return artistRepository.findById(artistId)
    }

    internal fun findAlbum(artistId: String, albumId: String): Optional<Album> {
        val repoArtistOptional = findArtist(artistId)
        return if (repoArtistOptional.isPresent() && repoArtistOptional.get().albums != null)
            repoArtistOptional.get().albums.stream()
                    .filter { repoAlbum -> repoAlbum.id.equals(albumId) }
                    .findFirst()
        else
            Optional.empty()
    }

    internal fun findSong(artistId: String, albumId: String, songId: String): Optional<Song> {
        val repoAlbumOptional = findAlbum(artistId, albumId)
        return if (repoAlbumOptional.isPresent() && repoAlbumOptional.get().songs != null)
            repoAlbumOptional.get().songs.stream()
                    .filter { repoSong -> songId == repoSong.id }
                    .findFirst()
        else
            Optional.empty()
    }

}
