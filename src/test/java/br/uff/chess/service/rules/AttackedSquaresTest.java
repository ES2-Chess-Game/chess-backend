package br.uff.chess.service.rules;

import static br.uff.chess.TestBoards.emptyBoard;
import static br.uff.chess.TestBoards.pos;
import static br.uff.chess.TestBoards.put;
import static br.uff.chess.model.Color.BRANCA;
import static br.uff.chess.model.Color.PRETA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;

class AttackedSquaresTest {

    private final Board board = emptyBoard();

    private boolean attacked(String square, Color by) {
        return AttackedSquares.isAttacked(board, pos(square), by);
    }

    @Test
    void peaoBrancoAtacaAsDiagonaisParaCimaENaoAFrente() {
        put(board, "e4", PieceType.PEAO, BRANCA);

        assertTrue(attacked("d5", BRANCA));
        assertTrue(attacked("f5", BRANCA));
        assertFalse(attacked("e5", BRANCA));
        assertFalse(attacked("d3", BRANCA));
    }

    @Test
    void peaoPretoAtacaAsDiagonaisParaBaixo() {
        put(board, "e5", PieceType.PEAO, PRETA);

        assertTrue(attacked("d4", PRETA));
        assertTrue(attacked("f4", PRETA));
        assertFalse(attacked("d6", PRETA));
    }

    @Test
    void cavaloAtacaEmLMesmoComPecasNoCaminho() {
        put(board, "g1", PieceType.CAVALO, BRANCA);
        // bloqueadores pretos: não atacam por BRANCA, então só o cavalo explica os ataques
        put(board, "g2", PieceType.PEAO, PRETA);
        put(board, "f2", PieceType.PEAO, PRETA);
        put(board, "f1", PieceType.PEAO, PRETA);

        assertTrue(attacked("f3", BRANCA));
        assertTrue(attacked("h3", BRANCA));
        assertTrue(attacked("e2", BRANCA));
        assertFalse(attacked("g3", BRANCA));
    }

    @Test
    void reiAtacaAsCasasVizinhas() {
        put(board, "e1", PieceType.REI, BRANCA);

        assertTrue(attacked("d2", BRANCA));
        assertTrue(attacked("f1", BRANCA));
        assertFalse(attacked("e3", BRANCA));
    }

    @Test
    void torreEBispoParamNaPrimeiraPecaDoCaminho() {
        put(board, "a1", PieceType.TORRE, PRETA);
        put(board, "a4", PieceType.PEAO, BRANCA);
        put(board, "h8", PieceType.BISPO, PRETA);
        put(board, "e5", PieceType.CAVALO, BRANCA);

        assertTrue(attacked("a4", PRETA));
        assertFalse(attacked("a5", PRETA));
        assertTrue(attacked("e5", PRETA));
        assertFalse(attacked("d4", PRETA));
    }

    @Test
    void rainhaAtacaComoTorreEComoBispo() {
        put(board, "d4", PieceType.RAINHA, BRANCA);

        assertTrue(attacked("d8", BRANCA));
        assertTrue(attacked("h4", BRANCA));
        assertTrue(attacked("g7", BRANCA));
        assertTrue(attacked("a1", BRANCA));
        assertFalse(attacked("e6", BRANCA));
    }

    @Test
    void torreNaoAtacaNaDiagonalNemBispoEmLinhaReta() {
        put(board, "d4", PieceType.TORRE, BRANCA);
        put(board, "h1", PieceType.BISPO, BRANCA);

        assertFalse(attacked("e5", BRANCA));
        assertFalse(attacked("h4", PRETA));
        assertFalse(attacked("h3", BRANCA));
    }

    @Test
    void pecasDaOutraCorNaoContam() {
        put(board, "e4", PieceType.RAINHA, PRETA);
        put(board, "d2", PieceType.CAVALO, PRETA);

        assertFalse(attacked("e8", BRANCA));
        assertFalse(attacked("f3", BRANCA));
    }

    @Test
    void casasDaBordaNaoQuebramABusca() {
        put(board, "a8", PieceType.PEAO, BRANCA);
        put(board, "h1", PieceType.PEAO, PRETA);

        assertFalse(attacked("a1", BRANCA));
        assertFalse(attacked("h8", PRETA));
        assertFalse(attacked("a1", PRETA));
    }

    static Stream<Arguments> posicoes() {
        Board inicial = new Board();

        Board meioJogo = new Board();
        meioJogo.set(pos("e2"), null);
        put(meioJogo, "e4", PieceType.PEAO, BRANCA);
        meioJogo.set(pos("g1"), null);
        put(meioJogo, "f3", PieceType.CAVALO, BRANCA);
        meioJogo.set(pos("d7"), null);
        put(meioJogo, "d5", PieceType.PEAO, PRETA);
        meioJogo.set(pos("c8"), null);
        put(meioJogo, "g4", PieceType.BISPO, PRETA);

        Board final_ = emptyBoard();
        put(final_, "g1", PieceType.REI, BRANCA);
        put(final_, "d1", PieceType.TORRE, BRANCA);
        put(final_, "b3", PieceType.RAINHA, BRANCA);
        put(final_, "g8", PieceType.REI, PRETA);
        put(final_, "c6", PieceType.CAVALO, PRETA);
        put(final_, "f7", PieceType.PEAO, PRETA);
        put(final_, "a2", PieceType.PEAO, BRANCA);

        return Stream.of(Arguments.of("inicial", inicial), Arguments.of("meio de jogo", meioJogo),
                Arguments.of("final", final_));
    }

    /** O roque usa AttackedSquares e o xeque usa DefaultCheckDetector: os dois têm que concordar. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("posicoes")
    void concordaComODetectorDeXequeEmTodasAsCasas(String nome, Board posicao) {
        DefaultCheckDetector detector = new DefaultCheckDetector();
        for (Color by : Color.values()) {
            for (int row = 0; row < Board.SIZE; row++) {
                for (int col = 0; col < Board.SIZE; col++) {
                    Position square = new Position(row, col);
                    assertEquals(detector.isSquareAttacked(posicao, square, by),
                            AttackedSquares.isAttacked(posicao, square, by),
                            () -> "casa " + square + " atacada por " + by);
                }
            }
        }
    }
}
