package br.uff.chess.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class AvatarControllerTest {

    @Autowired
    WebApplicationContext context;

    @Test
    void listaCatalogoCompleto() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(get("/api/avatares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].name").value("Rei"));
    }

    @Test
    void imagemDeCadaAvatarEServidaSemAutenticacao() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for (String image : new String[] { "king", "queen", "rook", "bishop", "knight", "pawn" }) {
            mvc.perform(get("/avatares/" + image + ".svg"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("image/svg+xml"));
        }
    }
}
