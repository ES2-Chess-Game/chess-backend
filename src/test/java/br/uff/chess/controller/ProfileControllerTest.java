package br.uff.chess.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
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
import br.uff.chess.repository.GameStore;
import br.uff.chess.model.Color;
import br.uff.chess.model.GameStatus;
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

    @Autowired
    GameStore games;

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
    void desistirDaPartidaMaisRecenteNaoReabreUmaPartidaAntiga() throws Exception {
        MockHttpSession session = cadastrarELogar("alice", "alice@uff.br");
        String older = criarPartida(session, "local");
        jdbc.update("UPDATE chess_game SET created_at = 1 WHERE id = ?", older);
        String current = criarPartida(session, "ai");

        mvc.perform(get("/api/partidas/em-andamento").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(current));
        desistir(session, current);
        mvc.perform(get("/api/partidas/em-andamento").session(session)).andExpect(status().isNotFound());
        mvc.perform(get("/api/partidas/historico/{id}", current).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DESISTENCIA"));
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
    void rankingIncluiTodasAsContasEUsaResultadosValidosContraIA() throws Exception {
        MockHttpSession alice = cadastrarELogar("alice", "alice@uff.br");
        MockHttpSession bob = cadastrarELogar("bob", "bob@uff.br");
        cadastrarELogar("caio", "caio@uff.br");
        cadastrarELogar("dora", "dora@uff.br");
        Long avatarId = avatars.findAll().getFirst().getId();
        mvc.perform(put("/api/perfil").session(alice).contentType(MediaType.APPLICATION_JSON)
                .content("{\"avatarId\":" + avatarId + "}")).andExpect(status().isOk());

        encerrar(criarPartida(alice, "ai"), GameStatus.XEQUE_MATE, Color.BRANCA);
        String antigo = criarPartida(alice, "ai");
        encerrar(antigo, GameStatus.EMPATE, null);
        jdbc.update("UPDATE chess_game SET mode = NULL, winner = NULL, settings = NULL WHERE id = ?", antigo);
        desistir(alice, criarPartida(alice, "ai"));
        encerrar(criarPartida(bob, "ai"), GameStatus.EMPATE, null);
        encerrar(criarPartida(bob, "ai"), GameStatus.TEMPO_ESGOTADO, Color.BRANCA);
        encerrar(criarPartida(bob, "local"), GameStatus.XEQUE_MATE, Color.BRANCA);
        criarPartida(bob, "ai");

        mvc.perform(get("/api/partidas/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].avatar.id").value(avatarId))
                .andExpect(jsonPath("$[0].pontos").value(4))
                .andExpect(jsonPath("$[0].derrotas").value(1))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[1].username").value("bob"))
                .andExpect(jsonPath("$[1].pontos").value(1))
                .andExpect(jsonPath("$[2:4].username", containsInAnyOrder("caio", "dora")))
                .andExpect(jsonPath("$[2:4].pontos", containsInAnyOrder(0, 0)));
    }

    @Test
    void rankingDesempataPelaMenorQuantidadeDeDerrotas() throws Exception {
        MockHttpSession alice = cadastrarELogar("alice", "alice@uff.br");
        MockHttpSession zara = cadastrarELogar("zara", "zara@uff.br");

        encerrar(criarPartida(alice, "ai"), GameStatus.XEQUE_MATE, Color.BRANCA);
        desistir(alice, criarPartida(alice, "ai"));
        for (int partida = 0; partida < 3; partida++) {
            encerrar(criarPartida(zara, "ai"), GameStatus.EMPATE, null);
        }

        mvc.perform(get("/api/partidas/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("zara"))
                .andExpect(jsonPath("$[0].pontos").value(3))
                .andExpect(jsonPath("$[0].derrotas").value(0))
                .andExpect(jsonPath("$[1].username").value("alice"))
                .andExpect(jsonPath("$[1].pontos").value(3))
                .andExpect(jsonPath("$[1].derrotas").value(1));
    }

    @Test
    void jogadorNaoPodeFazerLanceNemDesistirPelaIA() throws Exception {
        MockHttpSession alice = cadastrarELogar("alice", "alice@uff.br");
        String id = criarPartida(alice, "ai");
        mvc.perform(post("/api/partidas/{id}/ia", id).session(alice)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/partidas/{id}/desistencia", id).session(alice).param("cor", "PRETA"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/partidas/{id}/lances", id).session(alice).contentType(MediaType.APPLICATION_JSON)
                .content("{\"origemLinha\":6,\"origemColuna\":4,\"destinoLinha\":4,\"destinoColuna\":4}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/partidas/{id}/lances", id).session(alice).contentType(MediaType.APPLICATION_JSON)
                .content("{\"origemLinha\":1,\"origemColuna\":4,\"destinoLinha\":3,\"destinoColuna\":4}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/partidas/{id}/ia", id).session(alice)).andExpect(status().isOk());
    }

    private void encerrar(String id, GameStatus status, Color vencedor) {
        var game = games.find(id, false).orElseThrow();
        game.end(status, vencedor, System.currentTimeMillis());
        games.update(game);
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
