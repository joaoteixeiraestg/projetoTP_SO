package utils;

import monitor.Alerta;
import monitor.Evento;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Classe para logging de eventos e alertas.
 * Grava informação em ficheiros para analise.
 */
public class Logger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    private final String pastaLogs;
    private final String ficheiroEventos;
    private final String ficheiroAlertas;
    private PrintWriter writerEventos;
    private PrintWriter writerAlertas;
    
    public Logger(String pastaLogs) {
        this.pastaLogs = pastaLogs;

        // Criar pasta de logs se não existir
        File pasta = new File(pastaLogs);
        if (!pasta.exists()) {
            pasta.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(FORMATTER);
        this.ficheiroEventos = pastaLogs + "/eventos_" + timestamp + ".log";
        this.ficheiroAlertas = pastaLogs + "/alertas_" + timestamp + ".log";

        inicializarFicheiros();
    }
    
    private void inicializarFicheiros() {
        try {
            writerEventos = new PrintWriter(new FileWriter(ficheiroEventos, true));
            writerAlertas = new PrintWriter(new FileWriter(ficheiroAlertas, true));
            
            // Cabeçalhos
            writerEventos.println("═══════════════════════════════════════════════════════════════");
            writerEventos.println("    LOG DE EVENTOS - Simulador de Concorrência eBPF");
            writerEventos.println("    Iniciado em: " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writerEventos.println("═══════════════════════════════════════════════════════════════");
            writerEventos.println();
            writerEventos.flush();
            
            writerAlertas.println("═══════════════════════════════════════════════════════════════");
            writerAlertas.println("    LOG DE ALERTAS DE SEGURANÇA - Monitor eBPF");
            writerAlertas.println("    Iniciado em: " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writerAlertas.println("═══════════════════════════════════════════════════════════════");
            writerAlertas.println();
            writerAlertas.flush();
            
            System.out.println("[Logger] Ficheiros de log inicializados:");
            System.out.println("  - Eventos: " + ficheiroEventos);
            System.out.println("  - Alertas: " + ficheiroAlertas);
            
        } catch (IOException e) {
            System.err.println("[Logger] Erro ao criar ficheiros de log: " + e.getMessage());
        }
    }
    
    /**
     * Regista um evento no ficheiro de log.
     */
    public synchronized void registarEvento(Evento evento) {
        if (writerEventos != null) {
            writerEventos.println(evento.toString());
            writerEventos.flush();
        }
    }
    
    /**
     * Regista um alerta no ficheiro de log.
     */
    public synchronized void registarAlerta(Alerta alerta) {
        if (writerAlertas != null) {
            writerAlertas.println(alerta.toLogFormat());
            writerAlertas.flush();
        }
    }
    
    /**
     * Regista uma mensagem.
     */
    public synchronized void log(String mensagem) {
        if (writerEventos != null) {
            writerEventos.println("[LOG] " + mensagem);
            writerEventos.flush();
        }
    }
    
    /**
     * Fecha os ficheiros de log.
     */
    public void fechar() {
        try {
            if (writerEventos != null) {
                writerEventos.println();
                writerEventos.println("═══════════════════════════════════════════════════════════════");
                writerEventos.println("    FIM DO LOG - " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writerEventos.println("═══════════════════════════════════════════════════════════════");
                writerEventos.close();
            }
            
            if (writerAlertas != null) {
                writerAlertas.println();
                writerAlertas.println("═══════════════════════════════════════════════════════════════");
                writerAlertas.println("    FIM DO LOG - " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writerAlertas.println("═══════════════════════════════════════════════════════════════");
                writerAlertas.close();
            }
            
            System.out.println("[Logger] Ficheiros de log fechados.");
        } catch (Exception e) {
            System.err.println("[Logger] Erro ao fechar ficheiros: " + e.getMessage());
        }
    }
    
    // Getters
    public String getFicheiroEventos() { return ficheiroEventos; }
    public String getFicheiroAlertas() { return ficheiroAlertas; }
}

