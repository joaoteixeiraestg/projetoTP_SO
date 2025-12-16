package threads;

import recursos.RecursoPartilhado;
import recursos.GestorRecursos;
import monitor.MonitoreBPF;
import monitor.TipoEvento;

/**
 * Classe base para threads que executam tarefas no simulador.
 * Baseado no conceito de processos/threads dos slides T-02:
 * - Ciclos de CPU intercalados com operações de I/O (simulado com sleep)
 * - Estados: Nova, Pronta, Em Execução, Bloqueada, Terminada
 */
public abstract class ThreadTarefa extends Thread {
    
    protected final MonitoreBPF monitor;
    protected final GestorRecursos gestorRecursos;
    protected volatile boolean executar;
    
    public ThreadTarefa(String nome, MonitoreBPF monitor, GestorRecursos gestorRecursos) {
        super(nome);
        this.monitor = monitor;
        this.gestorRecursos = gestorRecursos;
        this.executar = true;
    }
    
    @Override
    public void run() {
        monitor.registarEvento(TipoEvento.THREAD_INICIADA, getName(), "", 
            "Thread iniciou execução");
        
        try {
            executarTarefa();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[" + getName() + "] Thread interrompida.");
        } catch (Exception e) {
            System.err.println("[" + getName() + "] Erro: " + e.getMessage());
        } finally {
            monitor.registarEvento(TipoEvento.THREAD_TERMINADA, getName(), "", 
                "Thread terminou execução");
        }
    }
    
    /**
     * Método abstrato que deve ser implementado por cada tipo de thread.
     * Define o comportamento específico da tarefa.
     */
    protected abstract void executarTarefa() throws InterruptedException;
    
    /**
     * Simula trabalho de CPU (processamento).
     * @param duracao Duração em milissegundos
     */
    protected void simularTrabalho(long duracao) throws InterruptedException {
        Thread.sleep(duracao);
    }
    
    /**
     * Adquire um recurso pelo seu ID.
     * @param recursoId ID do recurso a adquirir
     */
    protected void adquirirRecurso(String recursoId) throws InterruptedException {
        RecursoPartilhado recurso = gestorRecursos.obterRecurso(recursoId);
        if (recurso != null) {
            recurso.adquirir();
        } else {
            System.err.println("[" + getName() + "] Recurso não encontrado: " + recursoId);
        }
    }
    
    /**
     * Liberta um recurso pelo seu ID.
     * @param recursoId ID do recurso a libertar
     */
    protected void libertarRecurso(String recursoId) {
        RecursoPartilhado recurso = gestorRecursos.obterRecurso(recursoId);
        if (recurso != null) {
            recurso.libertar();
        }
    }
    
    /**
     * Para a execução da thread.
     */
    public void parar() {
        executar = false;
        this.interrupt();
    }
}

