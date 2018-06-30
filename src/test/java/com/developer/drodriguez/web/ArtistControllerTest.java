package com.developer.drodriguez.web;

import com.developer.drodriguez.config.FongoConfiguration;
import com.developer.drodriguez.domain.ArtistRepository;
import com.developer.drodriguez.model.AjaxResponseBody;
import com.developer.drodriguez.model.Album;
import com.developer.drodriguez.model.Artist;
import com.developer.drodriguez.model.Song;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.gridfs.GridFSDBFile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("fongo")
@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {FongoConfiguration.class})
public class ArtistControllerTest {

    @Mock private GridFSDBFile gridFsDbFile;
    @Mock private GridFsTemplate gridFsTemplate;
    @Mock private ArtistRepository artistRepositoryMock;
    @InjectMocks @Spy private ArtistController artistControllerSpy;
    @Autowired private ArtistController artistController;
    @Autowired private ArtistRepository artistRepository;

    private static final MockMultipartFile ARTWORK_FILE_ONE = new MockMultipartFile("artwork", "artwork-file-one.png", "image/png", "Some image".getBytes());
    private static final MockMultipartFile SONG_FILE_ONE = new MockMultipartFile("song", "song-file-one.mp3", "audio/mp3", "Some song".getBytes());
    private static final Song SONG_ONE = new Song(UUID.randomUUID().toString(), "SongOne", "1", "2018", UUID.randomUUID().toString(), UUID.randomUUID().toString());
    private static final Song SONG_TWO = new Song(UUID.randomUUID().toString(), "SongTwo", "2", "2018", UUID.randomUUID().toString(), UUID.randomUUID().toString());
    private static final Song SONG_THREE = new Song(UUID.randomUUID().toString(), "SongThree", "1", "2018", UUID.randomUUID().toString(), UUID.randomUUID().toString());
    private static final Song SONG_FOUR = new Song(UUID.randomUUID().toString(), "SongFour", "1", "2018", UUID.randomUUID().toString(), UUID.randomUUID().toString());
    private static final Album ALBUM_ONE = new Album(UUID.randomUUID().toString(), "AlbumOne", new ArrayList<>(Arrays.asList(SONG_ONE, SONG_TWO)));
    private static final Album ALBUM_TWO = new Album(UUID.randomUUID().toString(), "AlbumTwo", new ArrayList<>(Arrays.asList(SONG_THREE)));
    private static final Album ALBUM_THREE = new Album(UUID.randomUUID().toString(), "AlbumThree", new ArrayList<>(Arrays.asList(SONG_FOUR)));
    private static final Album ALBUM_FOUR = new Album(UUID.randomUUID().toString(), "AlbumFour", null);
    private static final Artist ARTIST_ONE = new Artist(UUID.randomUUID().toString(), "ArtistOne", new ArrayList<>(Arrays.asList(ALBUM_ONE, ALBUM_TWO)));
    private static final Artist ARTIST_TWO = new Artist(UUID.randomUUID().toString(), "ArtistTwo", new ArrayList<>(Arrays.asList(ALBUM_THREE)));
    private static final Artist ARTIST_THREE = new Artist(UUID.randomUUID().toString(), "ArtistThree", new ArrayList<>(Arrays.asList(ALBUM_FOUR)));
    private static final Artist ARTIST_FOUR = new Artist(UUID.randomUUID().toString(), "ArtistFour", null);
    private static final String FILE_ID = SONG_ONE.getFileId();
    private static final String FILE_NAME = "test.mp3";
    private static final String FILE_CONTENT_TYPE = "media/mp3";
    private static final long FILE_LENGTH = 42L;
    private static final Date FILE_UPLOAD_DATE = new GregorianCalendar(2018, 6, 24).getTime();
    private static final String ARTWORK_ID = SONG_ONE.getFileId();
    private static final String ARTWORK_NAME = "test.png";
    private static final String ARTWORK_CONTENT_TYPE = "image/png";
    private static final byte[] EMPTY_CONTENT = new byte[0];
    private static final String SONGS = "songs";
    private static final String ALBUMS = "albums";
    private static final String ARTISTS = "artists";
    private static final String FILE = "file";
    private static final String ARTWORK = "artwork";
    private static final String UNKNOWN_ID = "unknownId";
    private static final String RESPONSE_ENTITY_SAVE_SUCCESSFUL = "Object saved successfully.";
    private static final String RESPONSE_ENTITY_SAVE_UNSUCCESSFUL = "Could not successfully save the object.";
    private static final String RESPONSE_ENTITY_DELETE_SUCCESSFUL = "Object deleted successfully.";
    private static final String RESPONSE_ENTITY_DELETE_UNSUCCESSFUL = "Could not successfully delete the object.";
    private MockMvc mockMvc;

    @Before
    public void init() {
        artistRepository.save(ARTIST_ONE);
        artistRepository.save(ARTIST_TWO);
        artistRepository.save(ARTIST_THREE);
        artistRepository.save(ARTIST_FOUR);
        mockMvc = MockMvcBuilders.standaloneSetup(artistController).build();
    }

    @Test
    public void getAllArtists() throws Exception {
        List<Artist> artists = new LinkedList<>(Arrays.asList(ARTIST_TWO, ARTIST_THREE, ARTIST_ONE, ARTIST_FOUR));
        mockMvc.perform(get(String.format("/%s", ARTISTS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(toJson(artists)))
                .andExpect(status().isOk());
    }

    @Test
    public void getAllArtistsNoneExist() throws Exception {
        artistRepository.deleteAll();
        mockMvc.perform(get(String.format("/%s", ARTISTS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(toJson(Collections.EMPTY_LIST)))
                .andExpect(status().isOk());
    }

    @Test
    public void getArtist() throws Exception {
        mockMvc.perform(get(String.format("/%s/%s", ARTISTS, ARTIST_ONE.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(toJson(ARTIST_ONE)))
                .andExpect(status().isOk());
    }

    @Test
    public void getArtistNoneExist() throws Exception {
        mockMvc.perform(get(String.format("/%s/%s", ARTISTS, UNKNOWN_ID))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().bytes(EMPTY_CONTENT))
                .andExpect(status().isOk());
    }

    @Test
    public void postArtist() throws Exception {
        mockMvc.perform(post(String.format("/%s", ARTISTS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(ARTIST_ONE)))
                .andExpect(content().string(toJson(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, ARTIST_ONE))))
                .andExpect(status().isCreated());
        assertEquals(ARTIST_ONE, artistRepository.findOne(ARTIST_ONE.getId()));
    }

    @Test
    public void putArtist() throws Exception {
        Artist artistToPut = cloneArtist(ARTIST_ONE);
        artistToPut.setName("NewArtistOne");
        mockMvc.perform(put(String.format("/%s/%s", ARTISTS, ARTIST_ONE.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(artistToPut)))
                .andExpect(content().string(toJson(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, artistToPut))))
                .andExpect(status().isCreated());
        assertNotEquals(ARTIST_ONE, artistRepository.findOne(ARTIST_ONE.getId()));
    }

    @Test
    public void putArtistNoAlbumsExist() throws Exception {
        Artist artistToPut = cloneArtist(ARTIST_FOUR);
        artistToPut.setName("NewArtistFour");
        artistToPut.setAlbums(null);
        mockMvc.perform(put(String.format("/%s/%s", ARTISTS, ARTIST_FOUR.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(artistToPut)))
                .andExpect(content().string(toJson(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, artistToPut))))
                .andExpect(status().isCreated());
        assertNotEquals(ARTIST_FOUR, artistRepository.findOne(ARTIST_FOUR.getId()));
    }

    @Test
    public void deleteArtist() throws Exception {
        mockMvc.perform(delete(String.format("/%s/%s", ARTISTS, ARTIST_TWO.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(RESPONSE_ENTITY_DELETE_SUCCESSFUL))
                .andExpect(status().isOk());
        List<Artist> artistsAfterDelete = artistRepository.findAll();
        assertEquals(3, artistsAfterDelete.size());
        assertFalse(artistsAfterDelete.contains(ARTIST_TWO));
    }

    @Test
    public void deleteArtistNoSongsExist() throws Exception {
        mockMvc.perform(delete(String.format("/%s/%s", ARTISTS, ARTIST_THREE.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(RESPONSE_ENTITY_DELETE_SUCCESSFUL))
                .andExpect(status().isOk());
        List<Artist> artistsAfterDelete = artistRepository.findAll();
        assertEquals(3, artistsAfterDelete.size());
        assertFalse(artistsAfterDelete.contains(ARTIST_THREE));
    }

    @Test
    public void deleteArtistNoneExist() throws Exception {
        mockMvc.perform(delete(String.format("/%s/%s", ARTISTS, UNKNOWN_ID))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(RESPONSE_ENTITY_DELETE_UNSUCCESSFUL))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void getAllAlbums() throws Exception {
        mockMvc.perform(get(String.format("/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(toJson(ARTIST_ONE.getAlbums())))
                .andExpect(status().isOk());
    }

    @Test
    public void getAllAlbumsNoArtistExists() throws Exception {
        mockMvc.perform(get(String.format("/%s/%s/%s", ARTISTS, UNKNOWN_ID, ALBUMS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().bytes(EMPTY_CONTENT))
                .andExpect(status().isOk());
    }

    @Test
    public void getAllAlbumsNoneExist() throws Exception {
        mockMvc.perform(get(String.format("/%s/%s/%s", ARTISTS, ARTIST_FOUR.getId(), ALBUMS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().bytes(EMPTY_CONTENT))
                .andExpect(status().isOk());
    }

    @Test
    public void getAlbum() throws Exception {
        mockMvc.perform(get(String.format("/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS, ALBUM_TWO.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(toJson(ALBUM_TWO)))
                .andExpect(status().isOk());
    }

    @Test
    public void getAlbumNoneExist() throws Exception {
        mockMvc.perform(get(String.format("/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS, UNKNOWN_ID))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().bytes(EMPTY_CONTENT))
                .andExpect(status().isOk());
    }

    @Test
    public void postAlbum() throws Exception {
        mockMvc.perform(post(String.format("/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(ALBUM_FOUR)))
                .andExpect(content().string(containsString("\"name\":\"AlbumFour\"")))
                .andExpect(status().isCreated());
        assertEquals(3, artistRepository.findOne(ARTIST_ONE.getId()).getAlbums().size());
        assertNotEquals(ARTIST_ONE, artistRepository.findOne(ARTIST_ONE.getId()));
    }

    @Test
    public void postAlbumNoneExist() throws Exception {
        mockMvc.perform(post(String.format("/%s/%s/%s", ARTISTS, ARTIST_FOUR.getId(), ALBUMS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(ALBUM_FOUR)))
                .andExpect(content().string(containsString("\"name\":\"AlbumFour\"")))
                .andExpect(status().isCreated());
        assertEquals(1, artistRepository.findOne(ARTIST_FOUR.getId()).getAlbums().size());
        assertNotEquals(ALBUM_FOUR, artistRepository.findOne(ARTIST_FOUR.getId()));
    }

    @Test
    public void postAlbumNoArtistExists() throws Exception {
        AjaxResponseBody ajaxResponseBody = new AjaxResponseBody(RESPONSE_ENTITY_SAVE_UNSUCCESSFUL);
        mockMvc.perform(post(String.format("/%s/%s/%s", ARTISTS, UNKNOWN_ID, ALBUMS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(ALBUM_FOUR)))
                .andExpect(content().string(toJson(ajaxResponseBody)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void putAlbum() throws Exception {
        Album albumToPut = cloneAlbum(ALBUM_TWO);
        albumToPut.setName("NewAlbumTwo");
        AjaxResponseBody ajaxResponseBody = new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, albumToPut);
        mockMvc.perform(put(String.format("/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS, ALBUM_TWO.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(albumToPut)))
                .andExpect(content().string(toJson(ajaxResponseBody)))
                .andExpect(status().isCreated());
        assertNotEquals(ARTIST_ONE, artistRepository.findOne(ARTIST_ONE.getId()));
    }

    @Test
    public void putAlbumNoArtistExists() throws Exception {
        Album albumToPut = cloneAlbum(ALBUM_TWO);
        albumToPut.setName("NewAlbumTwo");
        AjaxResponseBody ajaxResponseBody = new AjaxResponseBody(RESPONSE_ENTITY_SAVE_UNSUCCESSFUL);
        mockMvc.perform(put(String.format("/%s/%s/%s/%s", ARTISTS, UNKNOWN_ID, ALBUMS, ALBUM_TWO.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(albumToPut)))
                .andExpect(content().string(toJson(ajaxResponseBody)))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(ARTIST_ONE, artistRepository.findOne(ARTIST_ONE.getId()));
    }

    @Test
    public void putAlbumNoSongsExist() throws Exception {
        Album albumToPut = cloneAlbum(ALBUM_FOUR);
        albumToPut.setName("NewAlbumTwo");
        AjaxResponseBody ajaxResponseBody = new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, albumToPut);
        mockMvc.perform(put(String.format("/%s/%s/%s/%s", ARTISTS, ARTIST_THREE.getId(), ALBUMS, ALBUM_FOUR.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(albumToPut)))
                .andExpect(content().string(toJson(ajaxResponseBody)))
                .andExpect(status().isCreated());
        assertNotEquals(ARTIST_THREE, artistRepository.findOne(ARTIST_THREE.getId()));
    }

    @Test
    public void deleteAlbum() throws Exception {
        mockMvc.perform(delete(String.format("/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS, ALBUM_TWO.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(RESPONSE_ENTITY_DELETE_SUCCESSFUL))
                .andExpect(status().isOk());
        Artist artistAfterDelete = artistRepository.findOne(ARTIST_ONE.getId());
        List<Album> albumsAfterDelete = artistAfterDelete.getAlbums();
        assertEquals(1, albumsAfterDelete.size());
        assertFalse(albumsAfterDelete.contains(ALBUM_TWO));
    }

    @Test
    public void deleteAlbumNoArtistExists() throws Exception {
        mockMvc.perform(delete(String.format("/%s/%s/%s/%s", ARTISTS, UNKNOWN_ID, ALBUMS, ALBUM_TWO.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(RESPONSE_ENTITY_DELETE_UNSUCCESSFUL))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void getAllSongs() throws Exception {
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS, ALBUM_ONE.getId(), SONGS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(toJson(ALBUM_ONE.getSongs())))
                .andExpect(status().isOk());
    }

    @Test
    public void getAllSongsNoArtistExists() throws Exception {
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s", ARTISTS, UNKNOWN_ID, ALBUMS, UNKNOWN_ID, SONGS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().bytes(EMPTY_CONTENT))
                .andExpect(status().isOk());
    }

    @Test
    public void getAllSongsNoAlbumExists() throws Exception {
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS, UNKNOWN_ID, SONGS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().bytes(EMPTY_CONTENT))
                .andExpect(status().isOk());
    }

    @Test
    public void getSong() throws Exception {
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS, ALBUM_ONE.getId(), SONGS, SONG_TWO.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(toJson(SONG_TWO)))
                .andExpect(status().isOk());
    }

    @Test
    public void getSongNoneExist() throws Exception {
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_FOUR.getId(), ALBUMS, ALBUM_FOUR.getId(), SONGS, UNKNOWN_ID))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().bytes(EMPTY_CONTENT))
                .andExpect(status().isOk());
    }

    @Test
    public void postSong() throws Exception {
        mockMvc.perform(fileUpload(String.format("/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS, ALBUM_TWO.getId(), SONGS))
                .file(SONG_FILE_ONE)
                .file(ARTWORK_FILE_ONE)
                .param("songName", SONG_THREE.getName())
                .param("trackNumber", SONG_THREE.getTrackNumber())
                .param("year", SONG_THREE.getYear())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(containsString("\"name\":\"SongThree\"")))
                .andExpect(status().isCreated());
    }

    @Test
    public void postSongNoNameGiven() throws Exception {
        mockMvc.perform(fileUpload(String.format("/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS, ALBUM_TWO.getId(), SONGS))
                .file(SONG_FILE_ONE)
                .file(ARTWORK_FILE_ONE)
                .param("songName", "")
                .param("trackNumber", SONG_THREE.getTrackNumber())
                .param("year", SONG_THREE.getYear())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(containsString("\"name\":\"song-file-one\"")))
                .andExpect(status().isCreated());
    }

    @Test
    public void postSongNoTrackNumberGiven() throws Exception {
        mockMvc.perform(fileUpload(String.format("/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS, ALBUM_TWO.getId(), SONGS))
                .file(SONG_FILE_ONE)
                .file(ARTWORK_FILE_ONE)
                .param("songName", SONG_THREE.getName())
                .param("trackNumber", "")
                .param("year", SONG_THREE.getYear())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(containsString("\"trackNumber\":\"0\"")))
                .andExpect(status().isCreated());
    }

    @Test
    public void postSongNoArtistExists() throws Exception {
        AjaxResponseBody ajaxResponseBody = new AjaxResponseBody(RESPONSE_ENTITY_SAVE_UNSUCCESSFUL);
        mockMvc.perform(fileUpload(String.format("/%s/%s/%s/%s/%s", ARTISTS, UNKNOWN_ID, ALBUMS, ALBUM_TWO.getId(), SONGS))
                .file(SONG_FILE_ONE)
                .file(ARTWORK_FILE_ONE)
                .param("songName", SONG_THREE.getName())
                .param("trackNumber", SONG_THREE.getTrackNumber())
                .param("year", SONG_THREE.getYear())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(toJson(ajaxResponseBody)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void postSongNoAlbumExists() throws Exception {
        AjaxResponseBody ajaxResponseBody = new AjaxResponseBody(RESPONSE_ENTITY_SAVE_UNSUCCESSFUL);
        mockMvc.perform(fileUpload(String.format("/%s/%s/%s/%s/%s", ARTISTS, ARTIST_FOUR.getId(), ALBUMS, UNKNOWN_ID, SONGS))
                .file(SONG_FILE_ONE)
                .file(ARTWORK_FILE_ONE)
                .param("songName", SONG_FOUR.getName())
                .param("trackNumber", SONG_FOUR.getTrackNumber())
                .param("year", SONG_FOUR.getYear())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(toJson(ajaxResponseBody)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void postSongEmptyAlbumList() throws Exception {
        Artist artistWithEmptyAlbumList = cloneArtist(ARTIST_FOUR);
        artistWithEmptyAlbumList.setAlbums(new ArrayList<>());
        artistRepository.save(artistWithEmptyAlbumList);
        mockMvc.perform(fileUpload(String.format("/%s/%s/%s/%s/%s", ARTISTS, ARTIST_FOUR.getId(), ALBUMS, ALBUM_FOUR.getId(), SONGS))
                .file(SONG_FILE_ONE)
                .file(ARTWORK_FILE_ONE)
                .param("songName", SONG_FOUR.getName())
                .param("trackNumber", SONG_FOUR.getTrackNumber())
                .param("year", SONG_FOUR.getYear())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(toJson(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_UNSUCCESSFUL))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void postSongNoneExist() throws Exception {
        mockMvc.perform(fileUpload(String.format("/%s/%s/%s/%s/%s", ARTISTS, ARTIST_THREE.getId(), ALBUMS, ALBUM_FOUR.getId(), SONGS))
                .file(SONG_FILE_ONE)
                .file(ARTWORK_FILE_ONE)
                .param("songName", SONG_FOUR.getName())
                .param("trackNumber", SONG_FOUR.getTrackNumber())
                .param("year", SONG_FOUR.getYear())
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(containsString("\"name\":\"SongFour\"")))
                .andExpect(status().isCreated());
    }

    @Test
    public void putSong() throws Exception {
        Song songToPut = cloneSong(SONG_THREE);
        songToPut.setName("NewSongThree");
        AjaxResponseBody ajaxResponseBody = new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, songToPut);
        mockMvc.perform(put(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS, ALBUM_TWO.getId(), SONGS, SONG_THREE.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(songToPut)))
                .andExpect(content().string(toJson(ajaxResponseBody)))
                .andExpect(status().isCreated());
        assertNotEquals(ARTIST_ONE, artistRepository.findOne(ARTIST_ONE.getId()));
    }

    @Test
    public void putSongNoArtistExists() throws Exception {
        Song songToPut = cloneSong(SONG_THREE);
        songToPut.setName("NewSongThree");
        AjaxResponseBody ajaxResponseBody = new AjaxResponseBody(RESPONSE_ENTITY_SAVE_UNSUCCESSFUL);
        mockMvc.perform(put(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, UNKNOWN_ID, ALBUMS, ALBUM_TWO.getId(), SONGS, SONG_THREE.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(songToPut)))
                .andExpect(content().string(toJson(ajaxResponseBody)))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(ARTIST_ONE, artistRepository.findOne(ARTIST_ONE.getId()));
    }

    @Test
    public void putSongNoAlbumExists() throws Exception {
        Song songToPut = cloneSong(SONG_FOUR);
        songToPut.setName("NewSongFour");
        AjaxResponseBody ajaxResponseBody = new AjaxResponseBody(RESPONSE_ENTITY_SAVE_UNSUCCESSFUL);
        mockMvc.perform(put(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_THREE.getId(), ALBUMS, UNKNOWN_ID, SONGS, SONG_FOUR.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(songToPut)))
                .andExpect(content().string(toJson(ajaxResponseBody)))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(ARTIST_THREE, artistRepository.findOne(ARTIST_THREE.getId()));
    }

    @Test
    public void putSongNoFileId() throws Exception {
        Song songToPut = cloneSong(SONG_THREE);
        songToPut.setFileId(null);
        mockMvc.perform(put(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS, ALBUM_TWO.getId(), SONGS, SONG_THREE.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(songToPut)))
                .andExpect(content().string(toJson(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, SONG_THREE))))
                .andExpect(status().isCreated());
        assertNotEquals(songToPut, artistRepository.findOne(ARTIST_ONE.getId()));
    }

    @Test
    public void putSongNoArtworkFileId() throws Exception {
        Song songToPut = cloneSong(SONG_THREE);
        songToPut.setArtworkFileId(null);
        mockMvc.perform(put(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS, ALBUM_TWO.getId(), SONGS, SONG_THREE.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(toJson(songToPut)))
                .andExpect(content().string(toJson(new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, SONG_THREE))))
                .andExpect(status().isCreated());
        assertNotEquals(songToPut, artistRepository.findOne(ARTIST_ONE.getId()));
    }

    @Test
    public void deleteSong() throws Exception {
        mockMvc.perform(delete(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS, ALBUM_ONE.getId(), SONGS, SONG_ONE.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(RESPONSE_ENTITY_DELETE_SUCCESSFUL))
                .andExpect(status().isOk());
        Artist artistAfterDelete = artistRepository.findOne(ARTIST_ONE.getId());
        Album albumAfterDelete = artistAfterDelete.getAlbums().stream()
                .filter(album -> album.getId().equals(ALBUM_ONE.getId()))
                .collect(Collectors.toList())
                .get(0);
        List<Song> songsAfterDelete = albumAfterDelete.getSongs();
        assertEquals(1, songsAfterDelete.size());
        assertFalse(songsAfterDelete.contains(SONG_ONE));
        assertTrue(songsAfterDelete.contains(SONG_TWO));
    }

    @Test
    public void deleteSongOneRemaining() throws Exception {
        mockMvc.perform(delete(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS, ALBUM_TWO.getId(), SONGS, SONG_THREE.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(RESPONSE_ENTITY_DELETE_SUCCESSFUL))
                .andExpect(status().isOk());
        Artist artistAfterDelete = artistRepository.findOne(ARTIST_ONE.getId());
        Album albumAfterDelete = artistAfterDelete.getAlbums().stream()
                .filter(album -> album.getId().equals(ALBUM_TWO.getId()))
                .collect(Collectors.toList())
                .get(0);
        List<Song> songsAfterDelete = albumAfterDelete.getSongs();
        assertTrue(songsAfterDelete.isEmpty());
    }

    @Test
    public void deleteSongNoArtistExists() throws Exception {
        mockMvc.perform(delete(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, UNKNOWN_ID, ALBUMS, ALBUM_ONE.getId(), SONGS, SONG_ONE.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(RESPONSE_ENTITY_DELETE_UNSUCCESSFUL))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void deleteSongNoAlbumExists() throws Exception {
        mockMvc.perform(delete(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_FOUR.getId(), ALBUMS, UNKNOWN_ID, SONGS, SONG_FOUR.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(RESPONSE_ENTITY_DELETE_UNSUCCESSFUL))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void deleteSongNoneExist() throws Exception {
        mockMvc.perform(delete(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_THREE.getId(), ALBUMS, ALBUM_FOUR, SONGS, UNKNOWN_ID))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(RESPONSE_ENTITY_DELETE_UNSUCCESSFUL))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void findArtist() {
        assertTrue(artistController.findArtist(ARTIST_ONE.getId()).isPresent());
        assertEquals(artistRepository.findOne(ARTIST_ONE.getId()), artistController.findArtist(ARTIST_ONE.getId()).get());
    }

    @Test
    public void findAlbum() {
        assertTrue(artistController.findAlbum(ARTIST_ONE.getId(), ALBUM_ONE.getId()).isPresent());
        assertEquals(ALBUM_ONE, artistController.findAlbum(ARTIST_ONE.getId(), ALBUM_ONE.getId()).get());
    }

    @Test
    public void findAlbumNull() {
        assertFalse(artistController.findAlbum(ARTIST_FOUR.getId(), UNKNOWN_ID).isPresent());
    }

    @Test
    public void findAlbumNoMatch() {
        assertFalse(artistController.findAlbum(ARTIST_ONE.getId(), UNKNOWN_ID).isPresent());
    }

    @Test
    public void findSong() {
        assertTrue(artistController.findSong(ARTIST_ONE.getId(), ALBUM_ONE.getId(), SONG_ONE.getId()).isPresent());
        assertEquals(artistController.findSong(ARTIST_ONE.getId(), ALBUM_ONE.getId(), SONG_ONE.getId()).get(), SONG_ONE);
    }

    @Test
    public void findSongNull() {
        assertFalse(artistController.findSong(ARTIST_THREE.getId(), ALBUM_FOUR.getId(), UNKNOWN_ID).isPresent());
    }

    @Test
    public void findSongNoMatch() {
        assertFalse(artistController.findSong(ARTIST_TWO.getId(), ALBUM_THREE.getId(), UNKNOWN_ID).isPresent());
    }

    @Test
    public void getSongFile() throws Exception {
        setupGridFsDbFileMock(FILE_ID, FILE_NAME, FILE_CONTENT_TYPE);
        when(artistRepositoryMock.findOne(anyString())).thenReturn(ARTIST_ONE);
        mockMvc = MockMvcBuilders.standaloneSetup(artistControllerSpy).build();
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS, ALBUM_ONE.getId(), SONGS, SONG_ONE.getId(), FILE))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(FILE_CONTENT_TYPE))
                .andExpect(status().isOk());
    }

    @Test
    public void getSongFileNull() throws Exception {
        setupGridFsDbFileMock(FILE_ID, FILE_NAME, FILE_CONTENT_TYPE);
        when(artistRepositoryMock.findOne(anyString())).thenReturn(ARTIST_TWO);
        mockMvc = MockMvcBuilders.standaloneSetup(artistControllerSpy).build();
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_TWO.getId(), ALBUMS, ALBUM_TWO.getId(), SONGS, SONG_THREE.getId(), FILE))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(header().doesNotExist("Content-Type"))
                .andExpect(status().isOk());
    }

    @Test
    public void getSongArtwork() throws Exception {
        setupGridFsDbFileMock(ARTWORK_ID, ARTWORK_NAME, ARTWORK_CONTENT_TYPE);
        when(artistRepositoryMock.findOne(anyString())).thenReturn(ARTIST_ONE);
        mockMvc = MockMvcBuilders.standaloneSetup(artistControllerSpy).build();
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.getId(), ALBUMS, ALBUM_ONE.getId(), SONGS, SONG_ONE.getId(), ARTWORK))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(ARTWORK_CONTENT_TYPE))
                .andExpect(status().isOk());
    }

    @Test
    public void getSongArtworkNull() throws Exception {
        setupGridFsDbFileMock(ARTWORK_ID, ARTWORK_NAME, ARTWORK_CONTENT_TYPE);
        when(artistRepositoryMock.findOne(anyString())).thenReturn(ARTIST_TWO);
        mockMvc = MockMvcBuilders.standaloneSetup(artistControllerSpy).build();
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_TWO.getId(), ALBUMS, ALBUM_TWO.getId(), SONGS, SONG_THREE.getId(), ARTWORK))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(header().doesNotExist("Content-Type"))
                .andExpect(status().isOk());
    }

    @Test
    public void findFile() throws IOException {
        setupGridFsDbFileMock(FILE_ID, FILE_NAME, FILE_CONTENT_TYPE);
        assertEquals(HttpStatus.OK, artistControllerSpy.findFile(FILE_ID).getStatusCode());
        assertTrue(artistControllerSpy.findFile(FILE_ID).hasBody());
    }

    @Test
    public void findFileNull() throws IOException {
        when(gridFsTemplate.findOne(anyObject())).thenReturn(null);
        assertEquals(HttpStatus.OK, artistControllerSpy.findFile(FILE_ID).getStatusCode());
        assertFalse(artistControllerSpy.findFile(FILE_ID).hasBody());
    }

    @Test
    public void deleteFile() {
        assertEquals(HttpStatus.OK, artistControllerSpy.deleteFile(FILE_ID).getStatusCode());
        assertFalse(artistControllerSpy.deleteFile(FILE_ID).hasBody());
    }

    private Artist cloneArtist(Artist artist) {
        return new Artist(artist.getId(), artist.getName(), artist.getAlbums());
    }

    private Album cloneAlbum(Album album) {
        return new Album(album.getId(), album.getName(), album.getSongs());
    }

    private Song cloneSong(Song song) {
        return new Song(song.getId(), song.getName(), song.getTrackNumber(), song.getYear(), song.getFileId(), song.getArtworkFileId());
    }

    private String toJson(Object object) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(object);
    }

    private void setupGridFsDbFileMock(String id, String name, String contentType) {
        when(gridFsDbFile.getId()).thenReturn(id);
        when(gridFsDbFile.getFilename()).thenReturn(name);
        when(gridFsDbFile.getContentType()).thenReturn(contentType);
        when(gridFsDbFile.getLength()).thenReturn(FILE_LENGTH);
        when(gridFsDbFile.getUploadDate()).thenReturn(FILE_UPLOAD_DATE);
        when(gridFsTemplate.findOne(anyObject())).thenReturn(gridFsDbFile);
    }

}
