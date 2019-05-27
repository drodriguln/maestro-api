package com.drodriguez.maestro.api.web

import com.drodriguez.maestro.api.Application
import com.drodriguez.maestro.api.config.FongoConfiguration
import com.drodriguez.maestro.api.domain.ArtistRepository
import com.drodriguez.maestro.api.model.Album
import com.drodriguez.maestro.api.model.Artist
import com.drodriguez.maestro.api.model.Song
import com.drodriguez.maestro.api.response.MaestroResponseManager
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.gridfs.model.GridFSFile
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders

import java.io.IOException
import java.util.*
import java.util.stream.Collectors

import junit.framework.TestCase.assertTrue
import org.bson.BsonObjectId
import org.bson.Document
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.*
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.springframework.data.mongodb.gridfs.GridFsOperations
import org.springframework.data.mongodb.gridfs.GridFsResource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@ActiveProfiles("fongo")
@ContextConfiguration(classes = [FongoConfiguration::class])
@SpringBootTest
@RunWith(SpringRunner::class)
class ArtistControllerTest {

    @Autowired
    private lateinit var artistController: ArtistController
    @Autowired
    private lateinit var artistRepository: ArtistRepository
    @Autowired
    private lateinit var maestroResponseManager: MaestroResponseManager
    @Mock
    private lateinit var gridFsFile: GridFSFile
    @Mock
    private lateinit var gridFsOperations: GridFsOperations
    @Mock
    private lateinit var artistRepositoryMock: ArtistRepository
    @Mock
    private lateinit var maestroResponseManagerMock: MaestroResponseManager
    @InjectMocks
    @Spy
    private lateinit var artistControllerSpy: ArtistController
    private lateinit var mockMvc: MockMvc

    @Before
    fun init() {
        artistRepository.save(ARTIST_ONE)
        artistRepository.save(ARTIST_TWO)
        artistRepository.save(ARTIST_THREE)
        artistRepository.save(ARTIST_FOUR)
        mockMvc = MockMvcBuilders.standaloneSetup(artistController).build()
    }

    @Test
    @Throws(Exception::class)
    fun getAllArtists() {
        val artists = listOf(ARTIST_TWO, ARTIST_THREE, ARTIST_ONE, ARTIST_FOUR)
        val response = maestroResponseManager.createGetSuccessResponse(artists)
        mockMvc.perform(get(String.format("/%s", ARTISTS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isOk)
    }

    @Test
    @Throws(Exception::class)
    fun getAllArtistsNoneExist() {
        artistRepository.deleteAll()
        val response = maestroResponseManager.createGetFailureResponse()
        mockMvc.perform(get(String.format("/%s", ARTISTS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isNotFound)
    }

    @Test
    @Throws(Exception::class)
    fun getArtist() {
        val response = maestroResponseManager.createGetSuccessResponse(ARTIST_ONE)
        mockMvc.perform(get(String.format("/%s/%s", ARTISTS, ARTIST_ONE.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isOk)
    }

    @Test
    @Throws(Exception::class)
    fun getArtistNoneExist() {
        val response = maestroResponseManager.createGetFailureResponse()
        mockMvc.perform(get(String.format("/%s/%s", ARTISTS, UNKNOWN_ID))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isNotFound)
    }

    @Test
    @Throws(Exception::class)
    fun postArtist() {
        val response = maestroResponseManager.createSaveSuccessResponse(ARTIST_ONE)
        mockMvc.perform(post(String.format("/%s", ARTISTS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(ARTIST_ONE)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isCreated)
        assertEquals(ARTIST_ONE, artistRepository.findById(ARTIST_ONE.id))
    }

    @Test
    @Throws(Exception::class)
    fun putArtist() {
        val artistToPut = cloneArtist(ARTIST_ONE)
        val response = maestroResponseManager.createSaveSuccessResponse(artistToPut)
        artistToPut.name = "NewArtistOne"
        mockMvc.perform(put(String.format("/%s/%s", ARTISTS, ARTIST_ONE.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(artistToPut)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isCreated)
        assertNotEquals(ARTIST_ONE, artistRepository.findById(ARTIST_ONE.id))
    }

    @Test
    @Throws(Exception::class)
    fun putArtistNoAlbumsExist() {
        val artistToPut = cloneArtist(ARTIST_FOUR)
        artistToPut.name = "NewArtistFour"
        artistToPut.albums = emptyList()
        val response = maestroResponseManager.createSaveSuccessResponse(artistToPut)
        mockMvc.perform(put(String.format("/%s/%s", ARTISTS, ARTIST_FOUR.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(artistToPut)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isCreated)
        assertNotEquals(ARTIST_FOUR, artistRepository.findById(ARTIST_FOUR.id))
    }

    @Test
    @Throws(Exception::class)
    fun deleteArtist() {
        val response = maestroResponseManager.createDeleteSuccessResponse()
        mockMvc.perform(delete(String.format("/%s/%s", ARTISTS, ARTIST_TWO.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isOk)
        val artistsAfterDelete = artistRepository.findAll()
        assertEquals(3, artistsAfterDelete.size.toLong())
        assertFalse(artistsAfterDelete.contains(ARTIST_TWO))
    }

    @Test
    @Throws(Exception::class)
    fun deleteArtistNoSongsExist() {
        val response = maestroResponseManager.createDeleteSuccessResponse()
        mockMvc.perform(delete(String.format("/%s/%s", ARTISTS, ARTIST_THREE.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isOk)
        val artistsAfterDelete = artistRepository.findAll()
        assertEquals(3, artistsAfterDelete.size.toLong())
        assertFalse(artistsAfterDelete.contains(ARTIST_THREE))
    }

    @Test
    @Throws(Exception::class)
    fun deleteArtistNoneExist() {
        val response = maestroResponseManager.createDeleteFailureResponse()
        mockMvc.perform(delete(String.format("/%s/%s", ARTISTS, UNKNOWN_ID))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isUnprocessableEntity)
    }

    @Test
    @Throws(Exception::class)
    fun getAllAlbums() {
        val response = maestroResponseManager.createGetSuccessResponse(ARTIST_ONE.albums!!)
        mockMvc.perform(get(String.format("/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isOk)
    }

    @Test
    @Throws(Exception::class)
    fun getAllAlbumsNoArtistExists() {
        val response = maestroResponseManager.createGetFailureResponse()
        mockMvc.perform(get(String.format("/%s/%s/%s", ARTISTS, UNKNOWN_ID, ALBUMS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isNotFound)
    }

    @Test
    @Throws(Exception::class)
    fun getAllAlbumsNoneExist() {
        val response = maestroResponseManager.createGetFailureResponse()
        mockMvc.perform(get(String.format("/%s/%s/%s", ARTISTS, ARTIST_FOUR.id, ALBUMS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isNotFound)
    }

    @Test
    @Throws(Exception::class)
    fun getAlbum() {
        val response = maestroResponseManager.createGetSuccessResponse(ALBUM_TWO)
        mockMvc.perform(get(String.format("/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_TWO.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isOk)
    }

    @Test
    @Throws(Exception::class)
    fun getAlbumNoneExist() {
        val response = maestroResponseManager.createGetFailureResponse()
        mockMvc.perform(get(String.format("/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, UNKNOWN_ID))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isNotFound)
    }

    @Test
    @Throws(Exception::class)
    fun postAlbum() {
        val response = maestroResponseManager.createSaveSuccessResponse(ALBUM_FOUR)
        mockMvc.perform(post(String.format("/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(ALBUM_FOUR)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.message").value(response.getBody()!!.message))
                .andExpect(content().string(containsString("\"name\":\"AlbumFour\"")))
                .andExpect(status().isCreated)
        assertEquals(3, artistRepository.findById(ARTIST_ONE.id).get().albums!!.size)
        assertNotEquals(ARTIST_ONE, artistRepository.findById(ARTIST_ONE.id))
    }

    @Test
    @Throws(Exception::class)
    fun postAlbumNoneExist() {
        val response = maestroResponseManager.createSaveSuccessResponse(ALBUM_FOUR)
        mockMvc.perform(post(String.format("/%s/%s/%s", ARTISTS, ARTIST_FOUR.id, ALBUMS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(ALBUM_FOUR)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.message").value(response.getBody()!!.message))
                .andExpect(content().string(containsString("\"name\":\"AlbumFour\"")))
                .andExpect(status().isCreated)
        assertEquals(1, artistRepository.findById(ARTIST_FOUR.id).get().albums!!.size)
        assertNotEquals(ALBUM_FOUR, artistRepository.findById(ARTIST_FOUR.id))
    }

    @Test
    @Throws(Exception::class)
    fun postAlbumNoArtistExists() {
        val response = maestroResponseManager.createSaveFailureResponse()
        mockMvc.perform(post(String.format("/%s/%s/%s", ARTISTS, UNKNOWN_ID, ALBUMS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(ALBUM_FOUR)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isUnprocessableEntity)
    }

    @Test
    @Throws(Exception::class)
    fun putAlbum() {
        val albumToPut = cloneAlbum(ALBUM_TWO)
        albumToPut.name = "NewAlbumTwo"
        val response = maestroResponseManager.createSaveSuccessResponse(albumToPut)
        mockMvc.perform(put(String.format("/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_TWO.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(albumToPut)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isCreated)
        assertNotEquals(ARTIST_ONE, artistRepository.findById(ARTIST_ONE.id))
    }

    @Test
    @Throws(Exception::class)
    fun putAlbumNoArtistExists() {
        val albumToPut = cloneAlbum(ALBUM_TWO)
        albumToPut.name = "NewAlbumTwo"
        val response = maestroResponseManager.createSaveFailureResponse()
        mockMvc.perform(put(String.format("/%s/%s/%s/%s", ARTISTS, UNKNOWN_ID, ALBUMS, ALBUM_TWO.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(albumToPut)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isUnprocessableEntity)
        assertEquals(ARTIST_ONE, artistRepository.findById(ARTIST_ONE.id))
    }

    @Test
    @Throws(Exception::class)
    fun putAlbumNoSongsExist() {
        val albumToPut = cloneAlbum(ALBUM_FOUR)
        albumToPut.name = "NewAlbumTwo"
        val response = maestroResponseManager.createSaveSuccessResponse(albumToPut)
        mockMvc.perform(put(String.format("/%s/%s/%s/%s", ARTISTS, ARTIST_THREE.id, ALBUMS, ALBUM_FOUR.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(albumToPut)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isCreated)
        assertNotEquals(ARTIST_THREE, artistRepository.findById(ARTIST_THREE.id))
    }

    @Test
    @Throws(Exception::class)
    fun deleteAlbum() {
        val response = maestroResponseManager.createDeleteSuccessResponse()
        mockMvc.perform(delete(String.format("/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_TWO.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isOk)
        val albumsAfterDelete = artistRepository.findById(ARTIST_ONE.id).get().albums!!
        assertEquals(1, albumsAfterDelete.size.toLong())
        assertFalse(albumsAfterDelete.contains(ALBUM_TWO))
    }

    @Test
    @Throws(Exception::class)
    fun deleteAlbumNoArtistExists() {
        val response = maestroResponseManager.createDeleteFailureResponse()
        mockMvc.perform(delete(String.format("/%s/%s/%s/%s", ARTISTS, UNKNOWN_ID, ALBUMS, ALBUM_TWO.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isUnprocessableEntity)
    }

    @Test
    @Throws(Exception::class)
    fun getAllSongs() {
        val response = maestroResponseManager.createGetSuccessResponse(ALBUM_ONE.songs!!)
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_ONE.id, SONGS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isOk)
    }

    @Test
    @Throws(Exception::class)
    fun getAllSongsNoArtistExists() {
        val response = maestroResponseManager.createGetFailureResponse()
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s", ARTISTS, UNKNOWN_ID, ALBUMS, UNKNOWN_ID, SONGS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isNotFound)
    }

    @Test
    @Throws(Exception::class)
    fun getAllSongsNoAlbumExists() {
        val response = maestroResponseManager.createGetFailureResponse()
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, UNKNOWN_ID, SONGS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isNotFound)
    }

    @Test
    @Throws(Exception::class)
    fun getSong() {
        val response = maestroResponseManager.createGetSuccessResponse(SONG_TWO)
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_ONE.id, SONGS, SONG_TWO.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isOk)
    }

    @Test
    @Throws(Exception::class)
    fun getSongNoneExist() {
        val response = maestroResponseManager.createGetFailureResponse()
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_FOUR.id, ALBUMS, ALBUM_FOUR.id, SONGS, UNKNOWN_ID))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isNotFound)
    }

    @Test
    @Throws(Exception::class)
    fun postSong() {
        val response = maestroResponseManager.createSaveSuccessResponse(SONG_THREE)
        mockMvc.perform(fileUpload(String.format("/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_TWO.id, SONGS))
                .file(SONG_FILE_ONE)
                .file(ARTWORK_FILE_ONE)
                .param("songName", SONG_THREE.name)
                .param("trackNumber", SONG_THREE.trackNumber)
                .param("year", SONG_THREE.year)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.message").value(response.getBody()!!.message))
                .andExpect(content().string(containsString("\"name\":\"SongThree\"")))
                .andExpect(status().isCreated)
    }

    @Test
    @Throws(Exception::class)
    fun postSongNoNameGiven() {
        val response = maestroResponseManager.createSaveSuccessResponse(SONG_THREE)
        mockMvc.perform(fileUpload(String.format("/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_TWO.id, SONGS))
                .file(SONG_FILE_ONE)
                .file(ARTWORK_FILE_ONE)
                .param("songName", "")
                .param("trackNumber", SONG_THREE.trackNumber)
                .param("year", SONG_THREE.year)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.message").value(response.getBody()!!.message))
                .andExpect(content().string(containsString("\"name\":\"song-file-one\"")))
                .andExpect(status().isCreated)
    }

    @Test
    @Throws(Exception::class)
    fun postSongNoTrackNumberGiven() {
        val response = maestroResponseManager.createSaveSuccessResponse(SONG_THREE)
        mockMvc.perform(fileUpload(String.format("/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_TWO.id, SONGS))
                .file(SONG_FILE_ONE)
                .file(ARTWORK_FILE_ONE)
                .param("songName", SONG_THREE.name)
                .param("trackNumber", "")
                .param("year", SONG_THREE.year)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.message").value(response.getBody()!!.message))
                .andExpect(content().string(containsString("\"trackNumber\":\"0\"")))
                .andExpect(status().isCreated)
    }

    @Test
    @Throws(Exception::class)
    fun postSongNoArtistExists() {
        val response = maestroResponseManager.createSaveFailureResponse()
        mockMvc.perform(fileUpload(String.format("/%s/%s/%s/%s/%s", ARTISTS, UNKNOWN_ID, ALBUMS, ALBUM_TWO.id, SONGS))
                .file(SONG_FILE_ONE)
                .file(ARTWORK_FILE_ONE)
                .param("songName", SONG_THREE.name)
                .param("trackNumber", SONG_THREE.trackNumber)
                .param("year", SONG_THREE.year)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isUnprocessableEntity)
    }

    @Test
    @Throws(Exception::class)
    fun postSongNoAlbumExists() {
        val response = maestroResponseManager.createSaveFailureResponse()
        mockMvc.perform(fileUpload(String.format("/%s/%s/%s/%s/%s", ARTISTS, ARTIST_FOUR.id, ALBUMS, UNKNOWN_ID, SONGS))
                .file(SONG_FILE_ONE)
                .file(ARTWORK_FILE_ONE)
                .param("songName", SONG_FOUR.name)
                .param("trackNumber", SONG_FOUR.trackNumber)
                .param("year", SONG_FOUR.year)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isUnprocessableEntity)
    }

    @Test
    @Throws(Exception::class)
    fun postSongEmptyAlbumList() {
        val artistWithEmptyAlbumList = cloneArtist(ARTIST_FOUR)
        artistWithEmptyAlbumList.albums = emptyList()
        artistRepository.save(artistWithEmptyAlbumList)
        val response = maestroResponseManager.createSaveFailureResponse()
        mockMvc.perform(fileUpload(String.format("/%s/%s/%s/%s/%s", ARTISTS, ARTIST_FOUR.id, ALBUMS, ALBUM_FOUR.id, SONGS))
                .file(SONG_FILE_ONE)
                .file(ARTWORK_FILE_ONE)
                .param("songName", SONG_FOUR.name)
                .param("trackNumber", SONG_FOUR.trackNumber)
                .param("year", SONG_FOUR.year)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(toJson(response.getBody())))
                .andExpect(status().isUnprocessableEntity)
    }

    @Test
    @Throws(Exception::class)
    fun postSongNoneExist() {
        val response = maestroResponseManager.createSaveSuccessResponse(SONG_FOUR)
        mockMvc.perform(fileUpload(String.format("/%s/%s/%s/%s/%s", ARTISTS, ARTIST_THREE.id, ALBUMS, ALBUM_FOUR.id, SONGS))
                .file(SONG_FILE_ONE)
                .file(ARTWORK_FILE_ONE)
                .param("songName", SONG_FOUR.name)
                .param("trackNumber", SONG_FOUR.trackNumber)
                .param("year", SONG_FOUR.year)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.message").value(response.getBody()!!.message))
                .andExpect(content().string(containsString("\"name\":\"SongFour\"")))
                .andExpect(status().isCreated)
    }

    @Test
    @Throws(Exception::class)
    fun putSong() {
        val songToPut = cloneSong(SONG_THREE)
        songToPut.name = "NewSongThree"
        val response = maestroResponseManager.createSaveSuccessResponse(songToPut)
        mockMvc.perform(put(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_TWO.id, SONGS, SONG_THREE.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(songToPut)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isCreated)
        assertNotEquals(ARTIST_ONE, artistRepository.findById(ARTIST_ONE.id))
    }

    @Test
    @Throws(Exception::class)
    fun putSongNoArtistExists() {
        val songToPut = cloneSong(SONG_THREE)
        songToPut.name = "NewSongThree"
        val response = maestroResponseManager.createSaveFailureResponse()
        mockMvc.perform(put(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, UNKNOWN_ID, ALBUMS, ALBUM_TWO.id, SONGS, SONG_THREE.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(songToPut)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isUnprocessableEntity)
        assertEquals(ARTIST_ONE, artistRepository.findById(ARTIST_ONE.id))
    }

    @Test
    @Throws(Exception::class)
    fun putSongNoAlbumExists() {
        val songToPut = cloneSong(SONG_FOUR)
        songToPut.name = "NewSongFour"
        val response = maestroResponseManager.createSaveFailureResponse()
        mockMvc.perform(put(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_THREE.id, ALBUMS, UNKNOWN_ID, SONGS, SONG_FOUR.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(songToPut)))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isUnprocessableEntity)
        assertEquals(ARTIST_THREE, artistRepository.findById(ARTIST_THREE.id))
    }

    @Test
    @Throws(Exception::class)
    fun putSongNoFileId() {
        val songToPut = cloneSong(SONG_THREE)
        songToPut.fileId = ""
        val response = maestroResponseManager.createSaveSuccessResponse(SONG_THREE)
        mockMvc.perform(put(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_TWO.id, SONGS, SONG_THREE.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(songToPut)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isCreated)
        assertNotEquals(songToPut, artistRepository.findById(ARTIST_ONE.id))
    }

    @Test
    @Throws(Exception::class)
    fun putSongNoArtworkFileId() {
        val songToPut = cloneSong(SONG_THREE)
        songToPut.artworkFileId = null
        val response = maestroResponseManager.createSaveSuccessResponse(SONG_THREE)
        mockMvc.perform(put(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_TWO.id, SONGS, SONG_THREE.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(songToPut)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isCreated)
        assertNotEquals(songToPut, artistRepository.findById(ARTIST_ONE.id))
    }

    @Test
    @Throws(Exception::class)
    fun deleteSong() {
        val response = maestroResponseManager.createDeleteSuccessResponse()
        mockMvc.perform(delete(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_ONE.id, SONGS, SONG_ONE.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isOk)
        val albumAfterDelete = artistRepository.findById(ARTIST_ONE.id).get().albums!!
                .stream()
                .filter { album -> album.id.equals(ALBUM_ONE.id) }
                .collect(Collectors.toList())
                .get(0)
        val songsAfterDelete = albumAfterDelete.songs!!
        assertEquals(1, songsAfterDelete.size.toLong())
        assertFalse(songsAfterDelete.contains(SONG_ONE))
        assertTrue(songsAfterDelete.contains(SONG_TWO))
    }

    @Test
    @Throws(Exception::class)
    fun deleteSongOneRemaining() {
        val response = maestroResponseManager.createDeleteSuccessResponse()
        mockMvc.perform(delete(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_TWO.id, SONGS, SONG_THREE.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isOk)
        val albumAfterDelete = artistRepository.findById(ARTIST_ONE.id).get().albums!!
                .stream()
                .filter { album -> album.id.equals(ALBUM_TWO.id) }
                .collect(Collectors.toList())
                .get(0)
        val songsAfterDelete = albumAfterDelete.songs!!
        assertTrue(songsAfterDelete.isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun deleteSongNoArtistExists() {
        val response = maestroResponseManager.createDeleteFailureResponse()
        mockMvc.perform(delete(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, UNKNOWN_ID, ALBUMS, ALBUM_ONE.id, SONGS, SONG_ONE.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isUnprocessableEntity)
    }

    @Test
    @Throws(Exception::class)
    fun deleteSongNoAlbumExists() {
        val response = maestroResponseManager.createDeleteFailureResponse()
        mockMvc.perform(delete(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_FOUR.id, ALBUMS, UNKNOWN_ID, SONGS, SONG_FOUR.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isUnprocessableEntity)
    }

    @Test
    @Throws(Exception::class)
    fun deleteSongNoneExist() {
        val response = maestroResponseManager.createDeleteFailureResponse()
        mockMvc.perform(delete(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_THREE.id, ALBUMS, ALBUM_FOUR, SONGS, UNKNOWN_ID))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(toJson(response.getBody())))
                .andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun findArtist() {
        assertTrue(artistController.findArtist(ARTIST_ONE.id).isPresent())
        assertEquals(artistRepository.findById(ARTIST_ONE.id), artistController.findArtist(ARTIST_ONE.id).get())
    }

    @Test
    fun findAlbum() {
        assertTrue(artistController.findAlbum(ARTIST_ONE.id, ALBUM_ONE.id).isPresent())
        assertEquals(ALBUM_ONE, artistController.findAlbum(ARTIST_ONE.id, ALBUM_ONE.id).get())
    }

    @Test
    fun findAlbumNull() {
        assertFalse(artistController.findAlbum(ARTIST_FOUR.id, UNKNOWN_ID).isPresent())
    }

    @Test
    fun findAlbumNoMatch() {
        assertFalse(artistController.findAlbum(ARTIST_ONE.id, UNKNOWN_ID).isPresent())
    }

    @Test
    fun findSong() {
        assertTrue(artistController.findSong(ARTIST_ONE.id, ALBUM_ONE.id, SONG_ONE.id).isPresent())
        assertEquals(artistController.findSong(ARTIST_ONE.id, ALBUM_ONE.id, SONG_ONE.id).get(), SONG_ONE)
    }

    @Test
    fun findSongNull() {
        assertFalse(artistController.findSong(ARTIST_THREE.id, ALBUM_FOUR.id, UNKNOWN_ID).isPresent())
    }

    @Test
    fun findSongNoMatch() {
        assertFalse(artistController.findSong(ARTIST_TWO.id, ALBUM_THREE.id, UNKNOWN_ID).isPresent())
    }

    @Test
    @Throws(Exception::class)
    fun getSongFile() {
        setupGridFsDbFileMock(FILE_ID, FILE_NAME, FILE_CONTENT_TYPE)
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(FILE_CONTENT_TYPE)
        val response = maestroResponseManager.createGetFileSuccessResponse(headers, SONG_FILE)
        `when`(maestroResponseManagerMock.createGetFileSuccessResponse(ArgumentMatchers.anyObject(), ArgumentMatchers.anyObject())).thenReturn(response)
        `when`(artistRepositoryMock.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(ARTIST_ONE))
        mockMvc = MockMvcBuilders.standaloneSetup(artistControllerSpy).build()
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_ONE.id, SONGS, SONG_ONE.id, FILE))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(FILE_CONTENT_TYPE))
                .andExpect(status().isOk)
    }

    @Test
    @Throws(Exception::class)
    fun getSongFileNull() {
        setupGridFsDbFileMock(FILE_ID, FILE_NAME, FILE_CONTENT_TYPE)
        val response = maestroResponseManager.createGetFileFailureResponse()
        `when`(maestroResponseManagerMock.createGetFileFailureResponse()).thenReturn(response)
        `when`(artistRepositoryMock.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(ARTIST_TWO))
        mockMvc = MockMvcBuilders.standaloneSetup(artistControllerSpy).build()
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_TWO.id, ALBUMS, ALBUM_TWO.id, SONGS, SONG_THREE.id, FILE))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(""))
                .andExpect(status().isNotFound)
    }

    @Test
    @Throws(Exception::class)
    fun getSongArtwork() {
        setupGridFsDbFileMock(ARTWORK_ID, ARTWORK_NAME, ARTWORK_CONTENT_TYPE)
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(ARTWORK_CONTENT_TYPE)
        val response = maestroResponseManager.createGetFileSuccessResponse(headers, ARTWORK_FILE)
        `when`(maestroResponseManagerMock.createGetFileSuccessResponse(ArgumentMatchers.anyObject(), ArgumentMatchers.anyObject())).thenReturn(response)
        `when`(artistRepositoryMock.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(ARTIST_ONE))
        mockMvc = MockMvcBuilders.standaloneSetup(artistControllerSpy).build()
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_ONE.id, SONGS, SONG_ONE.id, ARTWORK))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(ARTWORK_CONTENT_TYPE))
                .andExpect(status().isOk)
    }

    @Test
    @Throws(Exception::class)
    fun getSongArtworkNull() {
        setupGridFsDbFileMock(ARTWORK_ID, ARTWORK_NAME, ARTWORK_CONTENT_TYPE)
        val response = maestroResponseManager.createGetFileFailureResponse()
        `when`(maestroResponseManagerMock.createGetFileFailureResponse()).thenReturn(response)
        `when`(artistRepositoryMock.findById(ArgumentMatchers.anyString())).thenReturn(Optional.of(ARTIST_TWO))
        mockMvc = MockMvcBuilders.standaloneSetup(artistControllerSpy).build()
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_TWO.id, ALBUMS, ALBUM_TWO.id, SONGS, SONG_THREE.id, ARTWORK))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().json(""))
                .andExpect(status().isNotFound)
    }

    @Test
    @Throws(IOException::class)
    fun findFile() {
        setupGridFsDbFileMock(FILE_ID, FILE_NAME, FILE_CONTENT_TYPE)
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(FILE_CONTENT_TYPE)
        val expectedResponse = maestroResponseManager.createGetFileSuccessResponse(headers, SONG_FILE)
        `when`(maestroResponseManagerMock.createGetFileSuccessResponse(ArgumentMatchers.anyObject(), ArgumentMatchers.anyObject())).thenReturn(expectedResponse)
        val actualResponse = artistControllerSpy.findFile(FILE_ID)
        assertEquals(expectedResponse.getStatusCode(), actualResponse.statusCode)
        assertEquals(expectedResponse.getBody(), expectedResponse.getBody())
    }

    @Test
    @Throws(IOException::class)
    fun findFileNull() {
        `when`<GridFSFile>(gridFsOperations.findOne(ArgumentMatchers.anyObject())).thenReturn(null)
        val expectedResponse = maestroResponseManager.createGetFileFailureResponse()
        `when`(maestroResponseManagerMock.createGetFileFailureResponse()).thenReturn(expectedResponse)
        val actualResponse = artistControllerSpy.findFile(FILE_ID)
        assertEquals(expectedResponse.getStatusCode(), actualResponse.statusCode)
        assertEquals(expectedResponse.getBody(), expectedResponse.getBody())
    }

    @Test
    fun deleteFile() {
        val expectedResponse = maestroResponseManager.createDeleteSuccessResponse()
        `when`(maestroResponseManagerMock.createDeleteSuccessResponse()).thenReturn(expectedResponse)
        val actualResponse = artistControllerSpy.deleteFile(FILE_ID)
        assertEquals(expectedResponse.getStatusCode(), actualResponse.getStatusCode())
        assertEquals(actualResponse.getBody(), expectedResponse.getBody())
    }

    private fun cloneArtist(artist: Artist): Artist {
        return Artist(artist.id, artist.name, artist.albums)
    }

    private fun cloneAlbum(album: Album): Album {
        return Album(album.id, album.name, album.songs)
    }

    private fun cloneSong(song: Song): Song {
        return Song(song.id, song.name, song.trackNumber, song.year, song.fileId, song.artworkFileId)
    }

    @Throws(JsonProcessingException::class)
    private fun toJson(`object`: Any?): String {
        return ObjectMapper().writeValueAsString(`object`)
    }

    private fun setupGridFsDbFileMock(id: String, name: String, contentType: String) {
        `when`(gridFsFile.filename).thenReturn(name)
        `when`(gridFsFile.contentType).thenReturn(contentType)
        `when`(gridFsFile.length).thenReturn(FILE_LENGTH)
        `when`(gridFsFile.uploadDate).thenReturn(FILE_UPLOAD_DATE)
        `when`<GridFSFile>(gridFsOperations.findOne(ArgumentMatchers.anyObject())).thenReturn(gridFsFile)
    }

    companion object {
        private val SONG_FILE = GridFsResource(
                GridFSFile(BsonObjectId(), "foo", 0, 0, Date(), "foo",
                    Document("_contentType", "audio/mp3")
                )
        )
        private val ARTWORK_FILE = GridFsResource(
                GridFSFile(BsonObjectId(), "foo", 0, 0, Date(), "foo",
                        Document("_contentType", "image/png")
                )
        )
        private val ARTWORK_FILE_ONE = MockMultipartFile("artwork", "artwork-file-one.png", "image/png", "Some song file".toByteArray())
        private val SONG_FILE_ONE = MockMultipartFile("song", "song-file-one.mp3", "audio/mp3", "Some image file".toByteArray())
        private val SONG_ONE = Song(UUID.randomUUID().toString(), "SongOne", "1", "2018", UUID.randomUUID().toString(), UUID.randomUUID().toString())
        private val SONG_TWO = Song(UUID.randomUUID().toString(), "SongTwo", "2", "2018", UUID.randomUUID().toString(), UUID.randomUUID().toString())
        private val SONG_THREE = Song(UUID.randomUUID().toString(), "SongThree", "1", "2018", UUID.randomUUID().toString(), UUID.randomUUID().toString())
        private val SONG_FOUR = Song(UUID.randomUUID().toString(), "SongFour", "1", "2018", UUID.randomUUID().toString(), UUID.randomUUID().toString())
        private val ALBUM_ONE = Album(UUID.randomUUID().toString(), "AlbumOne", listOf(SONG_ONE, SONG_TWO))
        private val ALBUM_TWO = Album(UUID.randomUUID().toString(), "AlbumTwo", listOf(SONG_THREE))
        private val ALBUM_THREE = Album(UUID.randomUUID().toString(), "AlbumThree", listOf(SONG_FOUR))
        private val ALBUM_FOUR = Album(UUID.randomUUID().toString(), "AlbumFour", emptyList())
        private val ARTIST_ONE = Artist(UUID.randomUUID().toString(), "ArtistOne", listOf(ALBUM_ONE, ALBUM_TWO))
        private val ARTIST_TWO = Artist(UUID.randomUUID().toString(), "ArtistTwo", listOf(ALBUM_THREE))
        private val ARTIST_THREE = Artist(UUID.randomUUID().toString(), "ArtistThree", listOf(ALBUM_FOUR))
        private val ARTIST_FOUR = Artist(UUID.randomUUID().toString(), "ArtistFour", emptyList())
        private val FILE_ID = SONG_ONE.fileId
        private const val FILE_NAME = "test.mp3"
        private const val FILE_CONTENT_TYPE = "media/mp3"
        private const val FILE_LENGTH = 42L
        private val FILE_UPLOAD_DATE = GregorianCalendar(2018, 6, 24).time
        private val ARTWORK_ID = SONG_ONE.fileId
        private const val ARTWORK_NAME = "test.png"
        private const val ARTWORK_CONTENT_TYPE = "image/png"
        private const val SONGS = "songs"
        private const val ALBUMS = "albums"
        private const val ARTISTS = "artists"
        private const val FILE = "file"
        private const val ARTWORK = "artwork"
        private const val UNKNOWN_ID = "unknownId"
    }

}
