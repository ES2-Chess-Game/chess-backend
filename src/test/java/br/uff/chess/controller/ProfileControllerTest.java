package br.uff.chess.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import br.uff.chess.repository.AvatarRepository;
import br.uff.chess.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class ProfileControllerTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    UserRepository users;

    @Autowired
    AvatarRepository avatars;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper json;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        users.deleteAll();
    }

    private MockHttpSession cadastrarELogar(String username, String email) throws Exception {
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"email\":\"" + email + "\",\"senha\":\"segredo123\"}"))
                .andExpect(status().isCreated());
        return (MockHttpSession) mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"senha\":\"segredo123\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    void consultaPerfilComEstatisticasZeradas() throws Exception {
        MockHttpSession sessao = cadastrarELogar("alice", "alice@uff.br");

        mvc.perform(get("/api/perfil").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@uff.br"))
                .andExpect(jsonPath("$.avatar").doesNotExist())
                .andExpect(jsonPath("$.estatisticas.vitorias").value(0))
                .andExpect(jsonPath("$.estatisticas.derrotas").value(0))
                .andExpect(jsonPath("$.estatisticas.empates").value(0))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void historicoResumePartidasERecuperaRegistrosAntigosSemMisturarContas() throws Exception {
        MockHttpSession alice = cadastrarELogar("alice", "alice@uff.br");
        MockHttpSession bob = cadastrarELogar("bob", "bob@uff.br");
        String id = criarPartida(alice, "ai");
        mvc.perform(post("/api/partidas/{id}/lances", id).session(alice).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"origemLinha":6,"origemColuna":4,"destinoLinha":4,"destinoColuna":4}
                        """)).andExpect(status().isOk());
        desistir(alice, id);

        String completo = mvc.perform(get("/api/partidas/historico").session(alice))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].lances[0]").exists())
                .andReturn().getResponse().getContentAsString();
        String resumo = mvc.perform(get("/api/partidas/historico?resumo=true").session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].configuracao.modo").value("ai"))
                .andExpect(jsonPath("$[0].tabuleiro").doesNotExist())
                .andExpect(jsonPath("$[0].lances").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertTrue(resumo.length() * 2 < completo.length());

        assertEquals(1, jdbc.update("UPDATE chess_game SET mode = NULL, winner = NULL, settings = NULL WHERE id = ?", id));
        mvc.perform(get("/api/partidas/historico?resumo=true").session(alice))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].vencedor").value("PRETA"));
        mvc.perform(get("/api/partidas/historico/{id}", id).session(alice))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lances[0]").exists());
        mvc.perform(get("/api/partidas/historico?resumo=true").session(bob))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/partidas/historico/{id}", id).session(bob)).andExpect(status().isNotFound());
        mvc.perform(get("/api/partidas/{id}", id).session(bob)).andExpect(status().isNotFound());
        mvc.perform(post("/api/partidas/{id}/ia", id)).andExpect(status().isUnauthorized());
    }

    @Test
    void estatisticasContamPartidasEncerradasContraIaComResumosNovosEAntigos() throws Exception {
        MockHttpSession alice = cadastrarELogar("alice", "alice@uff.br");
        MockHttpSession bob = cadastrarELogar("bob", "bob@uff.br");
        String legacy = criarPartida(alice, "ai");
        desistir(alice, legacy);
        jdbc.update("UPDATE chess_game SET mode = NULL, winner = NULL, settings = NULL WHERE id = ?", legacy);
        desistir(alice, criarPartida(alice, "ai"));
        String fromOlderRelease = criarPartida(alice, "ai");
        desistir(alice, fromOlderRelease);
        jdbc.update("UPDATE chess_game SET winner = NULL WHERE id = ?", fromOlderRelease);
        desistir(alice, criarPartida(alice, "local"));
        desistir(bob, criarPartida(bob, "ai"));
        criarPartida(alice, "ai");

        mvc.perform(get("/api/perfil").session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estatisticas.vitorias").value(0))
                .andExpect(jsonPath("$.estatisticas.derrotas").value(3))
                .andExpect(jsonPath("$.estatisticas.empates").value(0));
    }

    private String criarPartida(MockHttpSession session, String mode) throws Exception {
        String response = mvc.perform(post("/api/partidas").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"modo\":\"" + mode + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asString();
    }

    private void desistir(MockHttpSession session, String id) throws Exception {
        mvc.perform(post("/api/partidas/{id}/desistencia", id).session(session).param("cor", "BRANCA"))
                .andExpect(status().isOk());
    }

    @Test
    void consultaESemSessaoRetorna401() throws Exception {
        mvc.perform(get("/api/perfil")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void selecionaAvatarDoCatalogoEPersiste() throws Exception {
        MockHttpSession sessao = cadastrarELogar("alice", "alice@uff.br");
        Long avatarId = avatars.findAll().get(1).getId();

        mvc.perform(put("/api/perfil").session(sessao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"avatarId\":" + avatarId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar.id").value(avatarId));

        mvc.perform(get("/api/perfil").session(sessao))
                .andExpect(jsonPath("$.avatar.id").value(avatarId));
    }

    @Test
    void avatarInexistenteRetorna404ComMensagem() throws Exception {
        MockHttpSession sessao = cadastrarELogar("alice", "alice@uff.br");

        mvc.perform(put("/api/perfil").session(sessao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"avatarId\":999999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Avatar não encontrado: 999999"));
    }

    @Test
    void editaUsernameEEmailMantendoOAvatar() throws Exception {
        MockHttpSession sessao = cadastrarELogar("alice", "alice@uff.br");
        Long avatarId = avatars.findAll().get(0).getId();
        mvc.perform(put("/api/perfil").session(sessao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"avatarId\":" + avatarId + "}")).andExpect(status().isOk());

        mvc.perform(put("/api/perfil").session(sessao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice2\",\"email\":\"alice2@uff.br\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice2"))
                .andExpect(jsonPath("$.email").value("alice2@uff.br"))
                .andExpect(jsonPath("$.avatar.id").value(avatarId));
    }

    @Test
    void usernameOuEmailDeOutroUsuarioRetorna409() throws Exception {
        cadastrarELogar("bob", "bob@uff.br");
        MockHttpSession sessao = cadastrarELogar("alice", "alice@uff.br");

        mvc.perform(put("/api/perfil").session(sessao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"bob\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/perfil").session(sessao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"bob@uff.br\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void dadosInvalidosRetornam400() throws Exception {
        MockHttpSession sessao = cadastrarELogar("alice", "alice@uff.br");

        mvc.perform(put("/api/perfil").session(sessao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ab\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/perfil").session(sessao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nao-e-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cadaUsuarioSoVeEEditaOProprioPerfil() throws Exception {
        MockHttpSession sessaoAlice = cadastrarELogar("alice", "alice@uff.br");
        MockHttpSession sessaoBob = cadastrarELogar("bob", "bob@uff.br");

        mvc.perform(put("/api/perfil").session(sessaoAlice).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice2\"}")).andExpect(status().isOk());

        mvc.perform(get("/api/perfil").session(sessaoBob))
                .andExpect(jsonPath("$.username").value("bob"));
        assertEquals("bob", users.findByEmail("bob@uff.br").orElseThrow().getUsername());
    }
}
