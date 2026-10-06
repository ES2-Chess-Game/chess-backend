package br.uff.chess.service;

import static br.uff.chess.model.Color.BRANCA;
import static br.uff.chess.model.Color.PRETA;
import static br.uff.chess.model.PieceType.BISPO;
import static br.uff.chess.model.PieceType.CAVALO;
import static br.uff.chess.model.PieceType.PEAO;
import static br.uff.chess.model.PieceType.REI;
import static br.uff.chess.model.PieceType.RAINHA;
import static br.uff.chess.model.PieceType.TORRE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.model.Game;
import br.uff.chess.model.Piece;
import br.uff.chess.model.PieceType;
import br.uff.chess.model.Position;
import br.uff.chess.service.exceptions.IllegalMoveException;
import br.uff.chess.service.rules.DefaultCheckDetector;
import br.uff.chess.service.rules.DefaultLegalMoveGenerator;

class SpecialMovesTest {

    private GameService service;
    private Game game;
    private Board board;

    @BeforeEach
    void setUp() {
        DefaultCheckDetector detector = new DefaultCheckDetector();
        service = new GameService(detector, new DefaultLegalMoveGenerator(detector));
        game = service.createGame();
        board = game.getBoard();
    }

    // ---------- helpers: notação algébrica (e2 -> linha 6, coluna 4) ----------

    private static Position pos(String square) {
        return new Position(8 - (square.charAt(1) - '0'), square.charAt(0) - 'a');
    }

    private void put(String square, PieceType type, Color color) {
        board.set(pos(square), new Piece(type, color));
    }

    private void clear(String... squares) {
        for (String s : squares) {
            board.set(pos(s), null);
        }
    }

    private void move(String from, String to) {
        service.move(game.getId(), pos(from), pos(to));
    }

    private void move(String from, String to, PieceType promotion) {
        service.move(game.getId(), pos(from), pos(to), promotion);
    }

    private void assertPiece(String square, PieceType type, Color color) {
        assertEquals(new Piece(type, color), board.get(pos(square)), square);
    }

    private void assertEmpty(String square) {
        assertNull(board.get(pos(square)), square);
    }

    // ------------------------------------------------------------------ roque

    @Test
    void whiteKingSideCastling() {
        clear("f1", "g1");
        move("e1", "g1");
        assertPiece("g1", REI, BRANCA);
        assertPiece("f1", TORRE, BRANCA);
        assertEmpty("e1");
        assertEmpty("h1");
        assertEquals(PRETA, game.getTurnoAtual());
    }

    @Test
    void whiteQueenSideCastling() {
        clear("b1", "c1", "d1");
        move("e1", "c1");
        assertPiece("c1", REI, BRANCA);
        assertPiece("d1", TORRE, BRANCA);
        assertEmpty("a1");
        assertEmpty("e1");
    }

    @Test
    void blackCastlingBothSides() {
        clear("f8", "g8");
        move("a2", "a3");
        move("e8", "g8");
        assertPiece("g8", REI, PRETA);
        assertPiece("f8", TORRE, PRETA);

        // outro jogo: lado da dama
        game = service.createGame();
        board = game.getBoard();
        clear("b8", "c8", "d8");
        move("a2", "a3");
        move("e8", "c8");
        assertPiece("c8", REI, PRETA);
        assertPiece("d8", TORRE, PRETA);
    }

    @Test
    void castlingBlockedByPieceBetweenKingAndRook() {
        // posição inicial: cavalo e bispo ainda no caminho
        assertThrows(IllegalMoveException.class, () -> move("e1", "g1"));
    }

    @Test
    void castlingDeniedAfterKingHasMoved() {
        clear("f1", "g1");
        move("e1", "f1");
        move("a7", "a6");
        move("f1", "e1");
        move("a6", "a5");
        assertThrows(IllegalMoveException.class, () -> move("e1", "g1"));
    }

    @Test
    void castlingDeniedAfterRookHasMoved() {
        clear("f1", "g1");
        move("h1", "g1");
        move("a7", "a6");
        move("g1", "h1");
        move("a6", "a5");
        assertThrows(IllegalMoveException.class, () -> move("e1", "g1"));
    }

    @Test
    void castlingDeniedWhenKingIsInCheck() {
        clear("f1", "g1", "e2");
        put("e5", TORRE, PRETA); // ataca e1 pela coluna e
        assertThrows(IllegalMoveException.class, () -> move("e1", "g1"));
    }

    @Test
    void castlingDeniedWhenKingCrossesAttackedSquare() {
        clear("f1", "g1", "f2");
        put("f5", TORRE, PRETA); // ataca f1 (casa que o rei atravessa)
        assertThrows(IllegalMoveException.class, () -> move("e1", "g1"));
    }

    @Test
    void castlingDeniedWhenKingLandsOnAttackedSquare() {
        clear("f1", "g1", "g2");
        put("g5", TORRE, PRETA); // ataca g1 (destino do rei)
        assertThrows(IllegalMoveException.class, () -> move("e1", "g1"));
    }

    @Test
    void queenSideCastlingAllowedWhenOnlyRookPassesThroughAttackedSquare() {
        clear("b1", "c1", "d1", "b2");
        put("b5", TORRE, PRETA); // ataca b1, mas o rei não passa por b1
        move("e1", "c1");
        assertPiece("c1", REI, BRANCA);
        assertPiece("d1", TORRE, BRANCA);
    }

    // ------------------------------------------------------------- en passant

    @Test
    void whiteCapturesEnPassant() {
        clear("e2");
        put("e5", PEAO, BRANCA);
        move("a2", "a3");
        move("d7", "d5");
        move("e5", "d6");
        assertPiece("d6", PEAO, BRANCA);
        assertEmpty("d5");
        assertEmpty("e5");
    }

    @Test
    void blackCapturesEnPassant() {
        clear("e7");
        put("e4", PEAO, PRETA);
        move("d2", "d4");
        move("e4", "d3");
        assertPiece("d3", PEAO, PRETA);
        assertEmpty("d4");
    }

    @Test
    void enPassantOnlyAvailableImmediatelyAfterDoublePush() {
        clear("e2");
        put("e5", PEAO, BRANCA);
        move("a2", "a3");
        move("d7", "d5");
        move("a3", "a4");
        move("a7", "a6");
        assertThrows(IllegalMoveException.class, () -> move("e5", "d6"));
    }

    @Test
    void enPassantNotAllowedWithoutPrecedingDoublePush() {
        clear("e2", "d7");
        put("e5", PEAO, BRANCA);
        put("d5", PEAO, PRETA); // peão preto já estava em d5: nenhum avanço duplo no lance anterior
        assertThrows(IllegalMoveException.class, () -> move("e5", "d6"));
    }

    @Test
    void enPassantRejectedWhenItExposesOwnKing() {
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                board.set(row, col, null);
            }
        }
        put("a5", REI, BRANCA);
        put("b5", PEAO, BRANCA);
        put("h2", PEAO, BRANCA);
        put("e8", REI, PRETA);
        put("c7", PEAO, PRETA);
        put("h5", TORRE, PRETA); // alinhada com rei e peões na quinta fileira
        move("h2", "h3");
        move("c7", "c5");
        // b5xc6 remove b5 e c5 da fileira e abriria o xeque da torre em h5
        assertThrows(IllegalMoveException.class, () -> move("b5", "c6"));
        assertPiece("b5", PEAO, BRANCA);
        assertPiece("c5", PEAO, PRETA);
    }

    // --------------------------------------------------------------- promoção

    @Test
    void promotionDefaultsToQueen() {
        clear("a8");
        put("a7", PEAO, BRANCA);
        move("a7", "a8");
        assertPiece("a8", RAINHA, BRANCA);
    }

    @Test
    void promotionToChosenPiece() {
        clear("a8");
        put("a7", PEAO, BRANCA);
        move("a7", "a8", CAVALO);
        assertPiece("a8", CAVALO, BRANCA);
    }

    @Test
    void promotionByCapture() {
        put("a7", PEAO, BRANCA);
        move("a7", "b8", TORRE); // captura o cavalo preto em b8
        assertPiece("b8", TORRE, BRANCA);
        assertEmpty("a7");
    }

    @Test
    void blackPromotion() {
        clear("h1");
        put("h2", PEAO, PRETA);
        move("a2", "a3");
        move("h2", "h1", BISPO);
        assertPiece("h1", BISPO, PRETA);
    }

    @Test
    void promotionToKingOrPawnIsRejected() {
        clear("a8");
        put("a7", PEAO, BRANCA);
        assertThrows(IllegalMoveException.class, () -> move("a7", "a8", REI));
        assertThrows(IllegalMoveException.class, () -> move("a7", "a8", PEAO));
        assertPiece("a7", PEAO, BRANCA); // lance rejeitado não altera o tabuleiro
    }

    @Test
    void promotionPieceOnNonPromotingMoveIsRejected() {
        assertThrows(IllegalMoveException.class, () -> move("e2", "e4", RAINHA));
    }
}
