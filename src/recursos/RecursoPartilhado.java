package recursos;

import monitor.MonitoreBPF;
import monitor.TipoEvento;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.TimeUnit;
import java.util.Queue;
import java.util.LinkedList;

/**
 * Classe que representa um recurso partilhado no sistema.
 * Implementa o conceito de secção crítica (slides T-04):
 * - Exclusão mútua: apenas uma thread pode aceder ao recurso de cada vez
 * - Progresso: se nenhuma thread está na secção crítica, qualquer uma pode entrar
 * - Espera limitada: deve existir um limite para o tempo de espera
 */
public class RecursoPartilhado {
    private final String id;
    private final String nome;
    private final ReentrantLock lock;
    private final Condition condicao;
    private final MonitoreBPF monitor;
    
    // Informação para monitorização
    private volatile String threadAtual;        // Thread que detém o recurso
    private volatile long timestampAquisicao;   // Quando foi adquirido
    private final Queue<String> filaEspera;     // Threads à espera (FIFO - slides T-02)
    private int totalAcessos;                   // Contador de acessos
    
    public RecursoPartilhado(String id, String nome, MonitoreBPF monitor) {
        this.id = id;
        this.nome = nome;
        this.monitor = monitor;
        this.lock = new ReentrantLock(true); // Fair lock para evitar starvation
        this.condicao = lock.newCondition();
        this.filaEspera = new LinkedList<>();
        this.threadAtual = null;
        this.totalAcessos = 0;
    }
    
    /**
     * Adquire o recurso (equivalente a wait()/down() em semáforos - slides T-04).
     * Implementa espera bloqueante (slides T-03).
     */
    public void adquirir() throws InterruptedException {
        String threadId = Thread.currentThread().getName();
        long inicioEspera = System.currentTimeMillis();
        
        // Registar pedido de recurso
        monitor.registarEvento(TipoEvento.PEDIDO_RECURSO, threadId, id, 
            "Thread solicita acesso ao recurso " + nome);
        
        lock.lock();
        try {
            // Adicionar à fila de espera
            filaEspera.add(threadId);
            monitor.registarEvento(TipoEvento.INICIO_ESPERA, threadId, id,
                "Thread adicionada à fila de espera. Posição: " + filaEspera.size());
            
            // Esperar enquanto o recurso estiver ocupado
            while (threadAtual != null) {
                // Verificar starvation (tempo de espera excessivo)
                monitor.verificarStarvation(threadId, id, inicioEspera);
                
                // Esperar com timeout para permitir verificações periódicas
                condicao.await(100, TimeUnit.MILLISECONDS);
            }
            
            // Remover da fila de espera
            filaEspera.remove(threadId);
            
            // Adquirir o recurso
            threadAtual = threadId;
            timestampAquisicao = System.currentTimeMillis();
            totalAcessos++;
            
            long tempoEspera = System.currentTimeMillis() - inicioEspera;
            monitor.registarEvento(TipoEvento.RECURSO_ADQUIRIDO, threadId, id,
                "Thread adquiriu o recurso " + nome, tempoEspera);
            monitor.registarEvento(TipoEvento.LOCK_ACQUIRE, threadId, id,
                "Lock adquirido (entrada na secção crítica)");
                
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Liberta o recurso (equivalente a signal()/up() em semáforos - slides T-04).
     */
    public void libertar() {
        String threadId = Thread.currentThread().getName();
        
        lock.lock();
        try {
            if (threadAtual != null && threadAtual.equals(threadId)) {
                long tempoUso = System.currentTimeMillis() - timestampAquisicao;
                
                monitor.registarEvento(TipoEvento.LOCK_RELEASE, threadId, id,
                    "Lock libertado (saída da secção crítica). Tempo de uso: " + tempoUso + "ms");
                monitor.registarEvento(TipoEvento.RECURSO_LIBERTADO, threadId, id,
                    "Thread libertou o recurso " + nome);
                
                threadAtual = null;
                timestampAquisicao = 0;
                
                // Sinalizar threads em espera
                condicao.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Tenta adquirir o recurso sem bloquear (não bloqueante - slides T-03).
     * @return true se conseguiu adquirir, false caso contrário
     */
    public boolean tentarAdquirir() {
        String threadId = Thread.currentThread().getName();
        
        if (lock.tryLock()) {
            try {
                if (threadAtual == null) {
                    threadAtual = threadId;
                    timestampAquisicao = System.currentTimeMillis();
                    totalAcessos++;
                    monitor.registarEvento(TipoEvento.RECURSO_ADQUIRIDO, threadId, id,
                        "Thread adquiriu recurso (não bloqueante)");
                    return true;
                }
            } finally {
                lock.unlock();
            }
        }
        return false;
    }
    
    // Getters para monitorização
    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getThreadAtual() { return threadAtual; }
    public boolean estaOcupado() { return threadAtual != null; }
    public Queue<String> getFilaEspera() { return new LinkedList<>(filaEspera); }
    public int getTotalAcessos() { return totalAcessos; }
    public long getTempoAquisicao() { return timestampAquisicao; }
}

