package br.uff.chess.service;

import static br.uff.chess.TestBoards.pos;
import static br.uff.chess.TestBoards.put;
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
import br.uff.chess.model.Piece;
import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;
import br.uff.chess.service.rules.DefaultCheckDetector;
import br.uff.chess.service.rules.DefaultLegalMoveGenerator;

class LegalMoveServiceTest {

    private final LegalMoveService legalMoveService = new LegalMoveService();

    @Test
    void rejectsMoveThatLeavesOwnKingInCheck() {
        Board board = emptyBoard();
        board.set(7, 4, new Piece(PieceType.REI, Color.BRANCA));
        board.set(6, 4, new Piece(PieceType.TORRE, Color.BRANCA));
        board.set(0, 4, new Piece(PieceType.TORRE, Color.PRETA));
        board.set(0, 0, new Piece(PieceType.REI, Color.PRETA));

        assertFalse(legalMoveService.isLegalMove(board, new Position(6, 4), new Position(6, 3), Color.BRANCA));
    }

    @Test
    void permitsMoveThatGetsKingOutOfCheck() {
        Board board = emptyBoard();
        board.set(7, 4, new Piece(PieceType.REI, Color.BRANCA));
        board.set(0, 4, new Piece(PieceType.TORRE, Color.PRETA));
        board.set(0, 0, new Piece(PieceType.REI, Color.PRETA));

        assertTrue(legalMoveService.isLegalMove(board, new Position(7, 4), new Position(7, 5), Color.BRANCA));
    }

    @Test
    void rejectsMoveIntoSquareAttackedByEnemyKing() {
        Board board = emptyBoard();
        board.set(7, 4, new Piece(PieceType.REI, Color.BRANCA));
        board.set(5, 5, new Piece(PieceType.REI, Color.PRETA));

        assertFalse(legalMoveService.isLegalMove(board, new Position(7, 4), new Position(6, 5), Color.BRANCA));
    }

    @Test
    void doesNotAllowCapturingEnemyKing() {
        Board board = emptyBoard();
        board.set(7, 4, new Piece(PieceType.REI, Color.BRANCA));
        board.set(0, 0, new Piece(PieceType.REI, Color.PRETA));
        board.set(1, 0, new Piece(PieceType.TORRE, Color.BRANCA));

        assertFalse(legalMoveService.isLegalMove(board, new Position(1, 0), new Position(0, 0), Color.BRANCA));
    }

    @Test
    void posicaoInicialTemVinteLancesParaCadaCor() {
        Board board = new Board();
        assertEquals(20, legalMoveService.getLegalMoves(board, Color.BRANCA).size());
        assertEquals(20, legalMoveService.getLegalMoves(board, Color.PRETA).size());
    }

    @Test
    void naoGeraLancesQuandoReiEstaAfogado() {
        Board board = emptyBoard();
        put(board, "h1", PieceType.REI, Color.BRANCA);
        put(board, "g3", PieceType.RAINHA, Color.PRETA);
        put(board, "a8", PieceType.REI, Color.PRETA);

        assertTrue(legalMoveService.getLegalMoves(board, Color.BRANCA).isEmpty());
        assertFalse(legalMoveService.isKingInCheck(board, Color.BRANCA));
    }

    static Stream<Arguments> posicoes() {
        Board inicial = new Board();

        Board cravada = new Board();
        cravada.set(pos("e2"), null);
        cravada.set(pos("d7"), null);
        put(cravada, "b4", PieceType.BISPO, Color.PRETA); // crava o peão de d2
        put(cravada, "e5", PieceType.RAINHA, Color.BRANCA);

        Board xeque = new Board();
        xeque.set(pos("f2"), null);
        xeque.set(pos("e7"), null);
        put(xeque, "h4", PieceType.RAINHA, Color.PRETA); // xeque no rei branco pela diagonal

        return Stream.of(Arguments.of("inicial", inicial), Arguments.of("peça cravada", cravada),
                Arguments.of("rei em xeque", xeque));
    }

    /**
     * A IA gera lances com LegalMoveService e o GameService valida com
     * DefaultLegalMoveGenerator: se discordarem, a IA escolhe um lance que a partida recusa.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("posicoes")
    void concordaComOGeradorUsadoPeloGameService(String nome, Board board) {
        DefaultLegalMoveGenerator generator = new DefaultLegalMoveGenerator(new DefaultCheckDetector());
        for (Color color : Color.values()) {
            for (int fromRow = 0; fromRow < Board.SIZE; fromRow++) {
                for (int fromCol = 0; fromCol < Board.SIZE; fromCol++) {
                    for (int toRow = 0; toRow < Board.SIZE; toRow++) {
                        for (int toCol = 0; toCol < Board.SIZE; toCol++) {
                            Position from = new Position(fromRow, fromCol);
                            Position to = new Position(toRow, toCol);
                            assertEquals(generator.isLegalMove(board, from, to, color),
                                    legalMoveService.isLegalMove(board, from, to, color),
                                    () -> color + " " + from + " -> " + to);
                        }
                    }
                }
            }
        }
    }

    private Board emptyBoard() {
        Board board = new Board();
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                board.set(row, col, null);
            }
        }
        return board;
    }
}