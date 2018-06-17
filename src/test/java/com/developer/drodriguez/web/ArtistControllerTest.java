package com.developer.drodriguez.web;

import com.developer.drodriguez.config.FongoConfiguration;
import com.developer.drodriguez.domain.ArtistRepository;
import com.developer.drodriguez.model.Album;
import com.developer.drodriguez.model.Artist;
import com.developer.drodriguez.model.Song;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

@ActiveProfiles("fongo")
@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {FongoConfiguration.class})
public class ArtistControllerTest {

    @Autowired ArtistRepository artistRepository;

    @Before
    public void init() {
        Song songOne = new Song(UUID.randomUUID().toString(), "songOne", "One", "2018", UUID.randomUUID().toString(), UUID.randomUUID().toString());
        Song songTwo = new Song(UUID.randomUUID().toString(), "SongTwo", "Two", "2018", UUID.randomUUID().toString(), UUID.randomUUID().toString());
        Song songThree = new Song(UUID.randomUUID().toString(), "SongThree", "One", "2018", UUID.randomUUID().toString(), UUID.randomUUID().toString());
        Song songFour = new Song(UUID.randomUUID().toString(), "SongFour", "One", "2018", UUID.randomUUID().toString(), UUID.randomUUID().toString());
        Album albumOne = new Album(UUID.randomUUID().toString(), "AlbumOne", new ArrayList<>(Arrays.asList(songOne, songTwo)));
        Album albumTwo = new Album(UUID.randomUUID().toString(), "AlbumTwo", new ArrayList<>(Arrays.asList(songThree)));
        Album albumThree = new Album(UUID.randomUUID().toString(), "AlbumThree", new ArrayList<>(Arrays.asList(songFour)));
        Album albumFour = new Album(UUID.randomUUID().toString(), "AlbumFour", null);
        Artist artistOne = new Artist(UUID.randomUUID().toString(), "ArtistOne", new ArrayList<>(Arrays.asList(albumOne, albumTwo)));
        Artist artistTwo = new Artist(UUID.randomUUID().toString(), "ArtistTwo", new ArrayList<>(Arrays.asList(albumThree)));
        Artist artistThree = new Artist(UUID.randomUUID().toString(), "ArtistThree", new ArrayList<>(Arrays.asList(albumFour)));
        Artist artistFour = new Artist(UUID.randomUUID().toString(), "ArtistFour", null);
        artistRepository.save(artistOne);
        artistRepository.save(artistTwo);
        artistRepository.save(artistThree);
        artistRepository.save(artistFour);
    }

    @Test
    public void test() {
        System.out.println(artistRepository.findAll());
    }

}
