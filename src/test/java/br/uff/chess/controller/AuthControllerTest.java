package br.uff.chess.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import br.uff.chess.model.User;
import br.uff.chess.repository.UserRepository;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
class AuthControllerTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    UserRepository users;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        users.deleteAll();
    }

    private String json(String... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) {
            sb.append(i > 0 ? "," : "").append("\"").append(kv[i]).append("\":\"").append(kv[i + 1]).append("\"");
        }
        return sb.append("}").toString();
    }

    private void cadastrar(String username, String email, String senha) throws Exception {
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(json("username", username, "email", email, "senha", senha)))
                .andExpect(status().isCreated());
    }

    private MockHttpSession login(String username, String senha) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json("username", username, "senha", senha)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void cadastroValidoRetorna201SemExporSenha() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(json("username", "alice", "email", "alice@uff.br", "senha", "segredo123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@uff.br"))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertTrue(!body.contains("senha") && !body.contains("passwordHash") && !body.contains("segredo123"));
    }

    @Test
    void senhaEhGravadaComBcrypt() throws Exception {
        cadastrar("alice", "alice@uff.br", "segredo123");
        User user = users.findByUsername("alice").orElseThrow();
        assertNotEquals("segredo123", user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("$2"));
    }

    @Test
    void usernameRepetidoRetorna409() throws Exception {
        cadastrar("alice", "alice@uff.br", "segredo123");
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(json("username", "alice", "email", "outra@uff.br", "senha", "segredo123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void emailRepetidoRetorna409() throws Exception {
        cadastrar("alice", "alice@uff.br", "segredo123");
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(json("username", "bob", "email", "alice@uff.br", "senha", "segredo123")))
                .andExpect(status().isConflict());
    }

    @Test
    void dadosInvalidosRetornam400() throws Exception {
        String[] corpos = {
                json("username", "alice", "email", "nao-eh-email", "senha", "segredo123"),
                json("username", "alice", "email", "alice@uff.br", "senha", "curta"),
                json("username", "", "email", "", "senha", "")
        };
        for (String corpo : corpos) {
            mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").exists());
        }
    }

    @Test
    void loginCorretoPermiteAcessarMe() throws Exception {
        cadastrar("alice", "alice@uff.br", "segredo123");
        MockHttpSession session = login("alice", "segredo123");
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void senhaErradaRetorna401() throws Exception {
        cadastrar("alice", "alice@uff.br", "segredo123");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json("username", "alice", "senha", "errada123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void usuarioInexistenteTemMesmaMensagemDaSenhaErrada() throws Exception {
        cadastrar("alice", "alice@uff.br", "segredo123");
        String senhaErrada = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json("username", "alice", "senha", "errada123")))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String inexistente = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json("username", "ninguem", "senha", "errada123")))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertEquals(senhaErrada, inexistente);
    }

    @Test
    void semSessaoMeRetorna401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void aposLogoutMeRetorna401() throws Exception {
        cadastrar("alice", "alice@uff.br", "segredo123");
        MockHttpSession session = login("alice", "segredo123");
        mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void partidaAutenticadaPreencheDono() throws Exception {
        cadastrar("alice", "alice@uff.br", "segredo123");
        MockHttpSession session = login("alice", "segredo123");
        Long id = users.findByUsername("alice").orElseThrow().getId();
        mvc.perform(post("/api/partidas").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(id));
    }

    @Test
    void partidaAnonimaContinuaFuncionando() throws Exception {
        mvc.perform(post("/api/partidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").doesNotExist());
    }
}
