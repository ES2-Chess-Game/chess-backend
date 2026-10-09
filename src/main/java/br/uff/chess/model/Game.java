package br.uff.chess.model;

import java.time.Duration;
import java.time.Instant;

public class Game {

    private final String id;
    private final Board board;
    private final CastlingRights castlingRights = new CastlingRights();
    private Position enPassantTarget;
    private Color turnoAtual;
    private GameStatus status;
    private Color vencedor;
    private Long userId;
    private long brancasMs;
    private long pretasMs;
    private Instant turnoIniciadoEm;
    private Instant criadaEm;

    public Game(String id, Board board, Color turnoAtual) {
        this.id = id;
        this.board = board;
        this.turnoAtual = turnoAtual;
        this.status = GameStatus.EM_ANDAMENTO;
    }

    public String getId() {
        return id;
    }

    public Board getBoard() {
        return board;
    }

    public Color getTurnoAtual() {
        return turnoAtual;
    }

    public CastlingRights getCastlingRights() {
        return castlingRights;
    }

    /** Casa "pulada" por um peão que acabou de avançar duas casas; null se não houver. */
    public Position getEnPassantTarget() {
        return enPassantTarget;
    }

    public void setEnPassantTarget(Position enPassantTarget) {
        this.enPassantTarget = enPassantTarget;
    }

    public GameStatus getStatus() {
        return status;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public Color getVencedor() {
        return vencedor;
    }

    public void setVencedor(Color vencedor) {
        this.vencedor = vencedor;
    }

    public boolean isFinalizada() {
        return status == GameStatus.XEQUE_MATE || status == GameStatus.EMPATE
                || status == GameStatus.TEMPO_ESGOTADO;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public void iniciarRelogio(long tempoPorJogadorMs, Instant agora) {
        this.brancasMs = tempoPorJogadorMs;
        this.pretasMs = tempoPorJogadorMs;
        this.criadaEm = agora;
        this.turnoIniciadoEm = agora;
    }

    /** Tempo restante da cor, descontando o turno em curso (sem alterar o estado salvo). */
    public long restanteMs(Color cor, Instant agora) {
        long salvo = cor == Color.BRANCA ? brancasMs : pretasMs;
        if (cor != turnoAtual || isFinalizada() || turnoIniciadoEm == null) {
            return salvo;
        }
        return Math.max(0, salvo - Duration.between(turnoIniciadoEm, agora).toMillis());
    }

    /** Desconta o turno em curso do jogador da vez e reinicia a contagem em {@code agora}. */
    public void descontarTurno(Instant agora) {
        long restante = restanteMs(turnoAtual, agora);
        if (turnoAtual == Color.BRANCA) {
            brancasMs = restante;
        } else {
            pretasMs = restante;
        }
        turnoIniciadoEm = agora;
    }

    /** Encerra a partida por tempo se o jogador da vez zerou o relógio. */
    public boolean verificarTempoEsgotado(Instant agora) {
        if (isFinalizada() || turnoIniciadoEm == null || restanteMs(turnoAtual, agora) > 0) {
            return false;
        }
        descontarTurno(agora);
        status = GameStatus.TEMPO_ESGOTADO;
        vencedor = turnoAtual == Color.BRANCA ? Color.PRETA : Color.BRANCA;
        return true;
    }

    public void alternarTurno() {
        turnoAtual = turnoAtual == Color.BRANCA ? Color.PRETA : Color.BRANCA;
    }
}
