package br.uff.chess.controller;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
class GameControllerTest {

    private static final String PARTIDAS = "/api/partidas";

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void configurarMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void lanceLegalAtualizaTabuleiroETurnoNaConsulta() throws Exception {
        String id = criarPartida();

        mover(id, 6, 4, 4, 4).andExpect(status().isOk());

        mvc.perform(get(PARTIDAS + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnoAtual").value("PRETA"))
                .andExpect(jsonPath("$.tabuleiro[6][4]").doesNotExist())
                .andExpect(jsonPath("$.tabuleiro[4][4].tipo").value("PEAO"))
                .andExpect(jsonPath("$.tabuleiro[4][4].cor").value("BRANCA"));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 8})
    void linhaForaDoTabuleiroNaoAlteraAPartida(int destino) throws Exception {
        String id = criarPartida();

        mover(id, 6, 4, destino, 4)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").isNotEmpty());

        mvc.perform(get(PARTIDAS + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnoAtual").value("BRANCA"))
                .andExpect(jsonPath("$.tabuleiro[6][4].tipo").value("PEAO"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "peça fora do turno, 1, 4, 3, 4",
            "origem vazia, 4, 4, 3, 4",
            "destino ocupado por peça da mesma cor, 7, 1, 6, 3"
    })
    void lanceInvalidoTemErroDeDominio(String caso, int origemLinha, int origemColuna,
            int destinoLinha, int destinoColuna) throws Exception {
        String id = criarPartida();

        mover(id, origemLinha, origemColuna, destinoLinha, destinoColuna)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").isNotEmpty());
    }

    @Test
    void partidaDesconhecidaTemErroDeDominio() throws Exception {
        mvc.perform(get(PARTIDAS + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").isNotEmpty());
    }

    @Test
    void respostaDaMaquinaAlteraTabuleiroEDevolveOTurno() throws Exception {
        String id = criarPartida();
        String antes = mover(id, 6, 4, 4, 4).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String depois = mvc.perform(post(PARTIDAS + "/{id}/ia", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnoAtual").value("BRANCA"))
                .andReturn().getResponse().getContentAsString();

        Object tabuleiroAntes = JsonPath.read(antes, "$.tabuleiro");
        Object tabuleiroDepois = JsonPath.read(depois, "$.tabuleiro");
        assertNotEquals(tabuleiroAntes, tabuleiroDepois);
    }

    @Test
    void xequeMateDeclaraVencedorEImpedeOutroLance() throws Exception {
        String id = criarPartida();
        mover(id, 6, 5, 5, 5).andExpect(status().isOk()); // f3
        mover(id, 1, 4, 3, 4).andExpect(status().isOk()); // e5
        mover(id, 6, 6, 4, 6).andExpect(status().isOk()); // g4
        mover(id, 0, 3, 4, 7)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("XEQUE_MATE"))
                .andExpect(jsonPath("$.vencedor").value("PRETA")); // Dh4#

        mover(id, 7, 1, 5, 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").isNotEmpty());
    }

    private String criarPartida() throws Exception {
        String json = mvc.perform(post(PARTIDAS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.turnoAtual").value("BRANCA"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private ResultActions mover(String id, int linha, int coluna, int destinoLinha, int destinoColuna)
            throws Exception {
        return mvc.perform(post(PARTIDAS + "/{id}/lances", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "origemLinha": %d, "origemColuna": %d,
                          "destinoLinha": %d, "destinoColuna": %d
                        }
                        """.formatted(linha, coluna, destinoLinha, destinoColuna)));
    }
}
