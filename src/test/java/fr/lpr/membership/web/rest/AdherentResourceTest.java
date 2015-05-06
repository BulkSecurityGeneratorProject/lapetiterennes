package fr.lpr.membership.web.rest;

import fr.lpr.membership.Application;
import fr.lpr.membership.domain.Adherent;
import fr.lpr.membership.repository.AdherentRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.Matchers.hasItem;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the AdherentResource REST controller.
 *
 * @see AdherentResource
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest
public class AdherentResourceTest {

    private static final String DEFAULT_PRENOM = "SAMPLE_TEXT";
    private static final String UPDATED_PRENOM = "UPDATED_TEXT";
    private static final String DEFAULT_NOM = "SAMPLE_TEXT";
    private static final String UPDATED_NOM = "UPDATED_TEXT";

    private static final Boolean DEFAULT_BENEVOLE = false;
    private static final Boolean UPDATED_BENEVOLE = true;
    private static final String DEFAULT_REMARQUE_BENEVOLAT = "SAMPLE_TEXT";
    private static final String UPDATED_REMARQUE_BENEVOLAT = "UPDATED_TEXT";
    private static final String DEFAULT_GENRE = "SAMPLE_TEXT";
    private static final String UPDATED_GENRE = "UPDATED_TEXT";
    private static final String DEFAULT_AUTRE_REMARQUE = "SAMPLE_TEXT";
    private static final String UPDATED_AUTRE_REMARQUE = "UPDATED_TEXT";

    @Inject
    private AdherentRepository adherentRepository;

    private MockMvc restAdherentMockMvc;

    private Adherent adherent;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        AdherentResource adherentResource = new AdherentResource();
        ReflectionTestUtils.setField(adherentResource, "adherentRepository", adherentRepository);
        this.restAdherentMockMvc = MockMvcBuilders.standaloneSetup(adherentResource).build();
    }

    @Before
    public void initTest() {
        adherent = new Adherent();
        adherent.setPrenom(DEFAULT_PRENOM);
        adherent.setNom(DEFAULT_NOM);
        adherent.setBenevole(DEFAULT_BENEVOLE);
        adherent.setRemarqueBenevolat(DEFAULT_REMARQUE_BENEVOLAT);
        adherent.setGenre(DEFAULT_GENRE);
        adherent.setAutreRemarque(DEFAULT_AUTRE_REMARQUE);
    }

    @Test
    @Transactional
    public void createAdherent() throws Exception {
        int databaseSizeBeforeCreate = adherentRepository.findAll().size();

        // Create the Adherent
        restAdherentMockMvc.perform(post("/api/adherents")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(adherent)))
                .andExpect(status().isCreated());

        // Validate the Adherent in the database
        List<Adherent> adherents = adherentRepository.findAll();
        assertThat(adherents).hasSize(databaseSizeBeforeCreate + 1);
        Adherent testAdherent = adherents.get(adherents.size() - 1);
        assertThat(testAdherent.getPrenom()).isEqualTo(DEFAULT_PRENOM);
        assertThat(testAdherent.getNom()).isEqualTo(DEFAULT_NOM);
        assertThat(testAdherent.getBenevole()).isEqualTo(DEFAULT_BENEVOLE);
        assertThat(testAdherent.getRemarqueBenevolat()).isEqualTo(DEFAULT_REMARQUE_BENEVOLAT);
        assertThat(testAdherent.getGenre()).isEqualTo(DEFAULT_GENRE);
        assertThat(testAdherent.getAutreRemarque()).isEqualTo(DEFAULT_AUTRE_REMARQUE);
    }

    @Test
    @Transactional
    public void checkPrenomIsRequired() throws Exception {
        // Validate the database is empty
        assertThat(adherentRepository.findAll()).hasSize(0);
        // set the field null
        adherent.setPrenom(null);

        // Create the Adherent, which fails.
        restAdherentMockMvc.perform(post("/api/adherents")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(adherent)))
                .andExpect(status().isBadRequest());

        // Validate the database is still empty
        List<Adherent> adherents = adherentRepository.findAll();
        assertThat(adherents).hasSize(0);
    }

    @Test
    @Transactional
    public void checkNomIsRequired() throws Exception {
        // Validate the database is empty
        assertThat(adherentRepository.findAll()).hasSize(0);
        // set the field null
        adherent.setNom(null);

        // Create the Adherent, which fails.
        restAdherentMockMvc.perform(post("/api/adherents")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(adherent)))
                .andExpect(status().isBadRequest());

        // Validate the database is still empty
        List<Adherent> adherents = adherentRepository.findAll();
        assertThat(adherents).hasSize(0);
    }

    @Test
    @Transactional
    public void getAllAdherents() throws Exception {
        // Initialize the database
        adherentRepository.saveAndFlush(adherent);

        // Get all the adherents
        restAdherentMockMvc.perform(get("/api/adherents"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.[*].id").value(hasItem(adherent.getId().intValue())))
                .andExpect(jsonPath("$.[*].prenom").value(hasItem(DEFAULT_PRENOM.toString())))
                .andExpect(jsonPath("$.[*].nom").value(hasItem(DEFAULT_NOM.toString())))
                .andExpect(jsonPath("$.[*].benevole").value(hasItem(DEFAULT_BENEVOLE.booleanValue())))
                .andExpect(jsonPath("$.[*].remarqueBenevolat").value(hasItem(DEFAULT_REMARQUE_BENEVOLAT.toString())))
                .andExpect(jsonPath("$.[*].genre").value(hasItem(DEFAULT_GENRE.toString())))
                .andExpect(jsonPath("$.[*].autreRemarque").value(hasItem(DEFAULT_AUTRE_REMARQUE.toString())));
    }

    @Test
    @Transactional
    public void getAdherent() throws Exception {
        // Initialize the database
        adherentRepository.saveAndFlush(adherent);

        // Get the adherent
        restAdherentMockMvc.perform(get("/api/adherents/{id}", adherent.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(adherent.getId().intValue()))
            .andExpect(jsonPath("$.prenom").value(DEFAULT_PRENOM.toString()))
            .andExpect(jsonPath("$.nom").value(DEFAULT_NOM.toString()))
            .andExpect(jsonPath("$.benevole").value(DEFAULT_BENEVOLE.booleanValue()))
            .andExpect(jsonPath("$.remarqueBenevolat").value(DEFAULT_REMARQUE_BENEVOLAT.toString()))
            .andExpect(jsonPath("$.genre").value(DEFAULT_GENRE.toString()))
            .andExpect(jsonPath("$.autreRemarque").value(DEFAULT_AUTRE_REMARQUE.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingAdherent() throws Exception {
        // Get the adherent
        restAdherentMockMvc.perform(get("/api/adherents/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateAdherent() throws Exception {
        // Initialize the database
        adherentRepository.saveAndFlush(adherent);

		int databaseSizeBeforeUpdate = adherentRepository.findAll().size();

        // Update the adherent
        adherent.setPrenom(UPDATED_PRENOM);
        adherent.setNom(UPDATED_NOM);
        adherent.setBenevole(UPDATED_BENEVOLE);
        adherent.setRemarqueBenevolat(UPDATED_REMARQUE_BENEVOLAT);
        adherent.setGenre(UPDATED_GENRE);
        adherent.setAutreRemarque(UPDATED_AUTRE_REMARQUE);
        restAdherentMockMvc.perform(put("/api/adherents")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(adherent)))
                .andExpect(status().isOk());

        // Validate the Adherent in the database
        List<Adherent> adherents = adherentRepository.findAll();
        assertThat(adherents).hasSize(databaseSizeBeforeUpdate);
        Adherent testAdherent = adherents.get(adherents.size() - 1);
        assertThat(testAdherent.getPrenom()).isEqualTo(UPDATED_PRENOM);
        assertThat(testAdherent.getNom()).isEqualTo(UPDATED_NOM);
        assertThat(testAdherent.getBenevole()).isEqualTo(UPDATED_BENEVOLE);
        assertThat(testAdherent.getRemarqueBenevolat()).isEqualTo(UPDATED_REMARQUE_BENEVOLAT);
        assertThat(testAdherent.getGenre()).isEqualTo(UPDATED_GENRE);
        assertThat(testAdherent.getAutreRemarque()).isEqualTo(UPDATED_AUTRE_REMARQUE);
    }

    @Test
    @Transactional
    public void deleteAdherent() throws Exception {
        // Initialize the database
        adherentRepository.saveAndFlush(adherent);

		int databaseSizeBeforeDelete = adherentRepository.findAll().size();

        // Get the adherent
        restAdherentMockMvc.perform(delete("/api/adherents/{id}", adherent.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<Adherent> adherents = adherentRepository.findAll();
        assertThat(adherents).hasSize(databaseSizeBeforeDelete - 1);
    }
}
