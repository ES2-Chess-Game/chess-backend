package br.uff.chess.service;

import br.uff.chess.model.Board;
import br.uff.chess.model.Color;
import br.uff.chess.service.rules.CheckDetector;
import br.uff.chess.service.rules.LegalMoveGenerator;

/**
 * Decide o estado da partida (xeque, xeque-mate, afogamento ou em andamento)
 * para a cor que está prestes a jogar, combinando detecção de xeque e
 * verificação de lances legais. Não implementa nenhuma das duas — apenas
 * consome os contratos definidos em {@link CheckDetector} e
 * {@link LegalMoveGenerator}.
 */
public class EndGameEvaluator {

    private final CheckDetector checkDetector;
    private final LegalMoveGenerator legalMoveGenerator;

    public EndGameEvaluator(CheckDetector checkDetector, LegalMoveGenerator legalMoveGenerator) {
        this.checkDetector = checkDetector;
        this.legalMoveGenerator = legalMoveGenerator;
    }

    public EndGameResult evaluate(Board board, Color colorToMove) {
        boolean emXeque = checkDetector.isKingInCheck(board, colorToMove);
        boolean haLanceLegal = legalMoveGenerator.hasLegalMove(board, colorToMove);

        if (emXeque && !haLanceLegal) {
            return EndGameResult.xequeMate(oponente(colorToMove));
        }
        if (!emXeque && !haLanceLegal) {
            return EndGameResult.empate();
        }
        if (emXeque) {
            return EndGameResult.xeque();
        }
        return EndGameResult.emAndamento();
    }

    private Color oponente(Color color) {
        return color == Color.BRANCA ? Color.PRETA : Color.BRANCA;
    }
}
