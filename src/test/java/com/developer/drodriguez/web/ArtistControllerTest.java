package com.developer.drodriguez.web;

import com.developer.drodriguez.config.FongoConfiguration;
import com.developer.drodriguez.domain.ArtistRepository;
import com.developer.drodriguez.model.AjaxResponseBody;
import com.developer.drodriguez.model.Album;
import com.developer.drodriguez.model.Artist;
import com.developer.drodriguez.model.Song;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("fongo")
@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {FongoConfiguration.class})
public class ArtistControllerTest {

    @Autowired ArtistController artistController;
    @Autowired ArtistRepository artistRepository;

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
    private static final String ARTISTS = "artists";
    private static final String ALBUMS = "albums";
    private static final String SONGS = "songs";
    private static final String RESPONSE_ENTITY_SAVE_SUCCESSFUL = "Object saved successfully.";
    private static final String RESPONSE_ENTITY_SAVE_UNSUCCESSFUL = "Could not successfully save the object.";
    private static final String RESPONSE_ENTITY_DELETE_SUCCESSFUL = "Object deleted successfully.";
    private static final String RESPONSE_ENTITY_ARTIST_NOT_FOUND = "Could not find an existing object to save to.";
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
        List<Artist> artists = new LinkedList<>(Arrays.asList(ARTIST_ONE, ARTIST_TWO, ARTIST_THREE, ARTIST_FOUR));
        Collections.sort(artists);
        mockMvc.perform(get(String.format("/%s", ARTISTS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(new ObjectMapper().writeValueAsString(artists)))
                .andExpect(status().isOk());
    }

    @Test
    public void getArtist() throws Exception {
        mockMvc.perform(get(String.format("/%s/%s", ARTISTS, ARTIST_ONE.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(new ObjectMapper().writeValueAsString(ARTIST_ONE)))
                .andExpect(status().isOk());
    }

    @Test
    public void postArtist() throws Exception {
        AjaxResponseBody ajaxResponseBody = new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, ARTIST_ONE);
        mockMvc.perform(post(String.format("/%s", ARTISTS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(new ObjectMapper().writeValueAsString(ARTIST_ONE)))
                .andExpect(content().string(new ObjectMapper().writeValueAsString(ajaxResponseBody)))
                .andExpect(status().isCreated());
        assertEquals(ARTIST_ONE, artistRepository.findOne(ARTIST_ONE.id));
    }

    @Test
    public void putArtist() throws Exception {
        Artist artistToPut = cloneArtist(ARTIST_ONE);
        artistToPut.name = "NewArtistOne";
        AjaxResponseBody ajaxResponseBody = new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, artistToPut);
        mockMvc.perform(put(String.format("/%s/%s", ARTISTS, ARTIST_ONE.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(new ObjectMapper().writeValueAsString(artistToPut)))
                .andExpect(content().string(new ObjectMapper().writeValueAsString(ajaxResponseBody)))
                .andExpect(status().isCreated());
        assertNotEquals(ARTIST_ONE, artistRepository.findOne(ARTIST_ONE.id));
    }

    @Test
    public void deleteArtist() throws Exception {
        mockMvc.perform(delete(String.format("/%s/%s", ARTISTS, ARTIST_TWO.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(RESPONSE_ENTITY_DELETE_SUCCESSFUL))
                .andExpect(status().isOk());
        List<Artist> artistsAfterDelete = artistRepository.findAll();
        assertEquals(3, artistsAfterDelete.size());
        assertFalse(artistsAfterDelete.contains(ARTIST_TWO));
    }

    @Test
    public void getAllAlbums() throws Exception {
        mockMvc.perform(get(String.format("/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(new ObjectMapper().writeValueAsString(ARTIST_ONE.albums)))
                .andExpect(status().isOk());
    }

    @Test
    public void getAlbum() throws Exception {
        mockMvc.perform(get(String.format("/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_TWO.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(new ObjectMapper().writeValueAsString(ALBUM_TWO)))
                .andExpect(status().isOk());
    }

    @Test
    public void postAlbum() throws Exception {
        mockMvc.perform(post(String.format("/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(new ObjectMapper().writeValueAsString(ALBUM_FOUR)))
                .andExpect(content().string(containsString("\"name\":\"AlbumFour\"")))
                .andExpect(status().isCreated());
        assertEquals(3, artistRepository.findOne(ARTIST_ONE.id).albums.size());
        assertNotEquals(ARTIST_ONE, artistRepository.findOne(ARTIST_ONE.id));
    }

    @Test
    public void putAlbum() throws Exception {
        Album albumToPut = cloneAlbum(ALBUM_TWO);
        albumToPut.name = "NewAlbumTwo";
        AjaxResponseBody ajaxResponseBody = new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, albumToPut);
        mockMvc.perform(put(String.format("/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_TWO.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(new ObjectMapper().writeValueAsString(albumToPut)))
                .andExpect(content().string(new ObjectMapper().writeValueAsString(ajaxResponseBody)))
                .andExpect(status().isCreated());
        assertNotEquals(ARTIST_ONE, artistRepository.findOne(ARTIST_ONE.id));
    }

    @Test
    public void deleteAlbum() throws Exception {
        mockMvc.perform(delete(String.format("/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_TWO.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(RESPONSE_ENTITY_DELETE_SUCCESSFUL))
                .andExpect(status().isOk());
        Artist artistAfterDelete = artistRepository.findOne(ARTIST_ONE.id);
        List<Album> albumsAfterDelete = artistAfterDelete.albums;
        assertEquals(1, albumsAfterDelete.size());
        assertFalse(albumsAfterDelete.contains(ALBUM_TWO));
    }

    @Test
    public void getAllSongs() throws Exception {
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_ONE.id, SONGS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(new ObjectMapper().writeValueAsString(ALBUM_ONE.songs)))
                .andExpect(status().isOk());
    }

    @Test
    public void getSong() throws Exception {
        mockMvc.perform(get(String.format("/%s/%s/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_ONE.id, SONGS, SONG_TWO.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(new ObjectMapper().writeValueAsString(SONG_TWO)))
                .andExpect(status().isOk());
    }

    private Artist cloneArtist(Artist artist) {
        return new Artist(artist.id, artist.name, artist.albums);
    }

    private Album cloneAlbum(Album album) {
        return new Album(album.id, album.name, album.songs);
    }

}
