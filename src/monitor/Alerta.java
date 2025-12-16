package monitor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Classe que representa um alerta de segurança gerado pelo monitor.
 * Alertas são gerados quando são detetados problemas como:
 * - Race conditions (condições de corrida)
 * - Deadlocks (bloqueios - slides T-05)
 * - Starvation (míngua - threads que não conseguem acesso)
 */
public class Alerta {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private final LocalDateTime timestamp;
    private final TipoAlerta tipo;
    private final TipoEvento eventoOrigem;
    private final String mensagem;
    private final String[] threadsEnvolvidas;
    private final String[] recursosEnvolvidos;
    
    public Alerta(TipoAlerta tipo, TipoEvento eventoOrigem, String mensagem, 
                  String[] threadsEnvolvidas, String[] recursosEnvolvidos) {
        this.timestamp = LocalDateTime.now();
        this.tipo = tipo;
        this.eventoOrigem = eventoOrigem;
        this.mensagem = mensagem;
        this.threadsEnvolvidas = threadsEnvolvidas;
        this.recursosEnvolvidos = recursosEnvolvidos;
    }
    
    // Getters
    public LocalDateTime getTimestamp() { return timestamp; }
    public TipoAlerta getTipo() { return tipo; }
    public TipoEvento getEventoOrigem() { return eventoOrigem; }
    public String getMensagem() { return mensagem; }
    public String[] getThreadsEnvolvidas() { return threadsEnvolvidas; }
    public String[] getRecursosEnvolvidos() { return recursosEnvolvidos; }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════════════════════════╗\n");
        sb.append("║ ALERTA [").append(tipo).append("] - ").append(eventoOrigem).append("\n");
        sb.append("║ Timestamp: ").append(timestamp.format(FORMATTER)).append("\n");
        sb.append("║ Mensagem: ").append(mensagem).append("\n");
        
        if (threadsEnvolvidas != null && threadsEnvolvidas.length > 0) {
            sb.append("║ Threads envolvidas: ").append(String.join(", ", threadsEnvolvidas)).append("\n");
        }
        if (recursosEnvolvidos != null && recursosEnvolvidos.length > 0) {
            sb.append("║ Recursos envolvidos: ").append(String.join(", ", recursosEnvolvidos)).append("\n");
        }
        sb.append("╚══════════════════════════════════════════════════════════════╝");
        return sb.toString();
    }
    
    /**
     * Formato para escrita em ficheiro de log
     */
    public String toLogFormat() {
        return String.format("[%s] [%s] [%s] %s | Threads: [%s] | Recursos: [%s]",
            timestamp.format(FORMATTER),
            tipo,
            eventoOrigem,
            mensagem,
            threadsEnvolvidas != null ? String.join(",", threadsEnvolvidas) : "",
            recursosEnvolvidos != null ? String.join(",", recursosEnvolvidos) : ""
        );
    }
}

