package br.uff.chess.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import br.uff.chess.repository.AvatarRepository;
import br.uff.chess.repository.UserRepository;

@SpringBootTest
class ProfileControllerTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    UserRepository users;

    @Autowired
    AvatarRepository avatars;

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
