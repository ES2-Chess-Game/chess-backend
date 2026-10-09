package br.uff.chess.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import br.uff.chess.model.GameStatus;
import br.uff.chess.repository.UserRepository;
import br.uff.chess.service.GameService;

@SpringBootTest
class GameControllerResumeTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    UserRepository users;

    @Autowired
    GameService gameService;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        users.deleteAll();
    }

    private MockHttpSession logar(String username) throws Exception {
        String cadastro = "{\"username\":\"" + username + "\",\"email\":\"" + username
                + "@teste.com\",\"senha\":\"senha1234\"}";
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(cadastro))
                .andExpect(status().isCreated());
        String login = "{\"username\":\"" + username + "\",\"senha\":\"senha1234\"}";
        return (MockHttpSession) mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(login)).andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private String criarPartida(MockHttpSession sessao) throws Exception {
        String body = mvc.perform(post("/api/partidas").session(sessao)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void semSessaoRetorna401() throws Exception {
        mvc.perform(get("/api/partidas/em-andamento")).andExpect(status().isUnauthorized());
    }

    @Test
    void logadoSemPartidaRetorna404() throws Exception {
        MockHttpSession sessao = logar("ana");

        mvc.perform(get("/api/partidas/em-andamento").session(sessao)).andExpect(status().isNotFound());
    }

    @Test
    void logadoComPartidaRetornaEstadoAtualComRelogio() throws Exception {
        MockHttpSession sessao = logar("bia");
        String id = criarPartida(sessao);

        mvc.perform(get("/api/partidas/em-andamento").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.turnoAtual", is("BRANCA")))
                .andExpect(jsonPath("$.tabuleiro.length()", is(8)))
                .andExpect(jsonPath("$.relogio.pretasMs", is(600000)));
    }

    @Test
    void partidaEmXequeContinuaAparecendo() throws Exception {
        MockHttpSession sessao = logar("caio");
        String id = criarPartida(sessao);
        gameService.getGame(id).setStatus(GameStatus.XEQUE);

        mvc.perform(get("/api/partidas/em-andamento").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)));
    }

    @Test
    void partidaEncerradaNaoAparece() throws Exception {
        MockHttpSession sessao = logar("duda");
        String id = criarPartida(sessao);
        gameService.getGame(id).setStatus(GameStatus.XEQUE_MATE);

        mvc.perform(get("/api/partidas/em-andamento").session(sessao)).andExpect(status().isNotFound());
    }

    @Test
    void partidaComTempoEsgotadoNaoAparece() throws Exception {
        MockHttpSession sessao = logar("edu");
        String id = criarPartida(sessao);
        gameService.getGame(id).setStatus(GameStatus.TEMPO_ESGOTADO);

        mvc.perform(get("/api/partidas/em-andamento").session(sessao)).andExpect(status().isNotFound());
    }

    @Test
    void comDuasPartidasDevolveAMaisRecente() throws Exception {
        MockHttpSession sessao = logar("fabi");
        criarPartida(sessao);
        Thread.sleep(5);
        String recente = criarPartida(sessao);

        mvc.perform(get("/api/partidas/em-andamento").session(sessao))
                .andExpect(jsonPath("$.id", is(recente)));
    }

    @Test
    void usuarioNaoRecebePartidaDeOutro() throws Exception {
        MockHttpSession a = logar("gui");
        criarPartida(a);
        MockHttpSession b = logar("hugo");

        mvc.perform(get("/api/partidas/em-andamento").session(b)).andExpect(status().isNotFound());
    }
}
