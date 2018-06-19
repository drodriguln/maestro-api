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

    private static final Song SONG_ONE = new Song(UUID.randomUUID().toString(), "SongOne", "One", "2018", UUID.randomUUID().toString(), UUID.randomUUID().toString());
    private static final Song SONG_TWO = new Song(UUID.randomUUID().toString(), "SongTwo", "Two", "2018", UUID.randomUUID().toString(), UUID.randomUUID().toString());
    private static final Song SONG_THREE = new Song(UUID.randomUUID().toString(), "SongThree", "One", "2018", UUID.randomUUID().toString(), UUID.randomUUID().toString());
    private static final Song SONG_FOUR = new Song(UUID.randomUUID().toString(), "SongFour", "One", "2018", UUID.randomUUID().toString(), UUID.randomUUID().toString());
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
        List<Artist> repoArtists = artistRepository.findAll();
        Collections.sort(repoArtists);
        String repoArtistsJson = new ObjectMapper().writeValueAsString(repoArtists);
        mockMvc.perform(get(String.format("/%s", ARTISTS)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(repoArtistsJson))
                .andExpect(status().isOk());
    }

    @Test
    public void getArtist() throws Exception {
        Artist repoArtist = artistRepository.findOne(ARTIST_ONE.id);
        String repoArtistJson = new ObjectMapper().writeValueAsString(repoArtist);
        mockMvc.perform(get(String.format("/%s/%s", ARTISTS, ARTIST_ONE.id)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(repoArtistJson))
                .andExpect(status().isOk());
    }

    @Test
    public void postArtist() throws Exception {
        String artistJson = new ObjectMapper().writeValueAsString(ARTIST_ONE);
        AjaxResponseBody ajaxResponseBody = new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, ARTIST_ONE);
        String ajaxResponseBodyJson = new ObjectMapper().writeValueAsString(ajaxResponseBody);
        mockMvc.perform(post(String.format("/%s", ARTISTS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(artistJson))
                .andExpect(content().string(ajaxResponseBodyJson))
                .andExpect(status().isCreated());
        assertEquals(ARTIST_ONE, artistRepository.findOne(ARTIST_ONE.id));
    }

    @Test
    public void putArtist() throws Exception {
        Artist updatedArtist = artistRepository.findOne(ARTIST_ONE.id);
        updatedArtist.name = "NewArtistOne";
        artistRepository.save(updatedArtist);
        String artistJson = new ObjectMapper().writeValueAsString(updatedArtist);
        AjaxResponseBody ajaxResponseBody = new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, updatedArtist);
        String ajaxResponseBodyJson = new ObjectMapper().writeValueAsString(ajaxResponseBody);
        mockMvc.perform(put(String.format("/%s/%s", ARTISTS, ARTIST_ONE.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(artistJson))
                .andExpect(content().string(ajaxResponseBodyJson))
                .andExpect(status().isCreated());
        assertNotEquals(ARTIST_ONE, artistRepository.findOne(ARTIST_ONE.id));
    }

    @Test
    public void deleteArtist() throws Exception {
        List<Artist> artists = artistRepository.findAll();
        mockMvc.perform(delete(String.format("/%s/%s", ARTISTS, ARTIST_TWO.id))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(RESPONSE_ENTITY_DELETE_SUCCESSFUL))
                .andExpect(status().isOk());
        List<Artist> updatedArtists = artistRepository.findAll();
        assertEquals(3, updatedArtists.size());
        assertFalse(updatedArtists.contains(ARTIST_TWO));
    }

    @Test
    public void getAllAlbums() throws Exception {
        List<Album> repoAlbums = artistRepository.findOne(ARTIST_ONE.id).albums;
        Collections.sort(repoAlbums);
        String repoAlbumsJson = new ObjectMapper().writeValueAsString(repoAlbums);
        mockMvc.perform(get(String.format("/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(repoAlbumsJson))
                .andExpect(status().isOk());
    }

    @Test
    public void getAlbum() throws Exception {
        List<Album> repoAlbums = artistRepository.findOne(ARTIST_ONE.id).albums;
        Collections.sort(repoAlbums);
        Album repoAlbum = repoAlbums.stream()
                .filter(album -> album.id == ALBUM_TWO.id)
                .collect(Collectors.toList())
                .get(0);
        String repoAlbumsJson = new ObjectMapper().writeValueAsString(repoAlbum);
        mockMvc.perform(get(String.format("/%s/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS, ALBUM_TWO.id)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content().string(repoAlbumsJson))
                .andExpect(status().isOk());
    }

    @Test
    public void postAlbum() throws Exception {
        String albumJson = new ObjectMapper().writeValueAsString(ALBUM_FOUR);
        AjaxResponseBody ajaxResponseBody = new AjaxResponseBody(RESPONSE_ENTITY_SAVE_SUCCESSFUL, ALBUM_FOUR);
        String ajaxResponseBodyJson = new ObjectMapper().writeValueAsString(ajaxResponseBody);
        mockMvc.perform(post(String.format("/%s/%s/%s", ARTISTS, ARTIST_ONE.id, ALBUMS))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(albumJson))
                .andExpect(content().string(containsString("\"name\":\"AlbumFour\"")))
                .andExpect(status().isCreated());
        assertEquals(3, artistRepository.findOne(ARTIST_ONE.id).albums.size());
        assertNotEquals(ARTIST_ONE, artistRepository.findOne(ARTIST_ONE.id));
    }

}
