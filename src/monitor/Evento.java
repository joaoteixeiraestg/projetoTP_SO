package monitor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Classe que representa um evento monitorizado no sistema.
 * Cada evento regista uma ação de uma thread sobre um recurso.
 */
public class Evento {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    private final LocalDateTime timestamp;
    private final TipoEvento tipo;
    private final String threadId;
    private final String recursoId;
    private final String descricao;
    private final long tempoEspera; // em milissegundos
    
    public Evento(TipoEvento tipo, String threadId, String recursoId, String descricao) {
        this.timestamp = LocalDateTime.now();
        this.tipo = tipo;
        this.threadId = threadId;
        this.recursoId = recursoId;
        this.descricao = descricao;
        this.tempoEspera = 0;
    }
    
    public Evento(TipoEvento tipo, String threadId, String recursoId, String descricao, long tempoEspera) {
        this.timestamp = LocalDateTime.now();
        this.tipo = tipo;
        this.threadId = threadId;
        this.recursoId = recursoId;
        this.descricao = descricao;
        this.tempoEspera = tempoEspera;
    }
    
    // Getters
    public LocalDateTime getTimestamp() { return timestamp; }
    public TipoEvento getTipo() { return tipo; }
    public String getThreadId() { return threadId; }
    public String getRecursoId() { return recursoId; }
    public String getDescricao() { return descricao; }
    public long getTempoEspera() { return tempoEspera; }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(timestamp.format(FORMATTER)).append("] ");
        sb.append("[").append(tipo).append("] ");
        sb.append("Thread: ").append(threadId);
        if (recursoId != null && !recursoId.isEmpty()) {
            sb.append(" | Recurso: ").append(recursoId);
        }
        if (tempoEspera > 0) {
            sb.append(" | Tempo espera: ").append(tempoEspera).append("ms");
        }
        if (descricao != null && !descricao.isEmpty()) {
            sb.append(" | ").append(descricao);
        }
        return sb.toString();
    }
}

