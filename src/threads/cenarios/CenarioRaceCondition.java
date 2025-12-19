package threads.cenarios;

import recursos.GestorRecursos;
import monitor.MonitoreBPF;
import monitor.TipoEvento;
import threads.ThreadTarefa;

/**
 * Cenário que demonstra Race Conditions.
 * 
 * Race Condition:
 * - Ocorre quando múltiplas threads acedem a dados partilhados sem sincronização
 * - O resultado depende da ordem de execução
 * - Leva a incongruência de dados
 * 
 * Implicações de Cibersegurança:
 * - Corrupção de memória
 * - Acesso indevido a dados
 * - Vulnerabilidades exploráveis por atacantes
 */
public class CenarioRaceCondition {
    
    private final MonitoreBPF monitor;
    private final GestorRecursos gestorRecursos;
    
    // Variável partilhada SEM sincronização (para demonstrar race condition)
    private int contadorNaoSincronizado = 0;
    
    // Variável partilhada COM sincronização (para comparação)
    private int contadorSincronizado = 0;
    
    public CenarioRaceCondition(MonitoreBPF monitor, GestorRecursos gestorRecursos) {
        this.monitor = monitor;
        this.gestorRecursos = gestorRecursos;
    }
    
    /**
     * Executa o cenário de race condition.
     */
    public void executar() throws InterruptedException {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║         CENÁRIO: RACE CONDITION (Condição de Corrida)        ║");
        System.out.println("║                                                              ║");
        System.out.println("║  Múltiplas threads vão incrementar um contador partilhado.   ║");
        System.out.println("║  Sem sincronização, o resultado final será inconsistente.    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
        
        // Criar recurso para versão sincronizada
        gestorRecursos.criarRecurso("contador", "Contador Partilhado");
        
        // Reset dos contadores
        contadorNaoSincronizado = 0;
        contadorSincronizado = 0;
        
        int numThreads = 5;
        int incrementosPorThread = 100; // Reduzido para output mais legível
        
        Thread[] threadsNaoSync = new Thread[numThreads];
        Thread[] threadsSync = new Thread[numThreads];
        
        // Criar threads NÃO sincronizadas
        for (int i = 0; i < numThreads; i++) {
            threadsNaoSync[i] = new ThreadNaoSincronizada(
                "Thread-NaoSync-" + i, monitor, gestorRecursos, incrementosPorThread);
        }
        
        // Criar threads sincronizadas
        for (int i = 0; i < numThreads; i++) {
            threadsSync[i] = new ThreadSincronizada(
                "Thread-Sync-" + i, monitor, gestorRecursos, incrementosPorThread);
        }
        
        System.out.println("Valor esperado: " + (numThreads * incrementosPorThread));
        System.out.println("\n--- Iniciando threads NÃO sincronizadas ---\n");
        
        // Executar threads não sincronizadas
        for (Thread t : threadsNaoSync) t.start();
        for (Thread t : threadsNaoSync) t.join();
        
        System.out.println("\n--- Iniciando threads SINCRONIZADAS ---\n");
        
        // Executar threads sincronizadas
        for (Thread t : threadsSync) t.start();
        for (Thread t : threadsSync) t.join();
        
        // Mostrar resultados
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    RESULTADOS                                ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Valor esperado:              %6d                         ║%n", 
            numThreads * incrementosPorThread);
        System.out.printf("║  Contador NÃO sincronizado:   %6d  %s              ║%n", 
            contadorNaoSincronizado, 
            contadorNaoSincronizado != numThreads * incrementosPorThread ? "⚠ RACE CONDITION!" : "✓");
        System.out.printf("║  Contador sincronizado:       %6d  %s                        ║%n", 
            contadorSincronizado,
            contadorSincronizado == numThreads * incrementosPorThread ? "✓" : "✗");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
        
        // Registar race condition se ocorreu
        if (contadorNaoSincronizado != numThreads * incrementosPorThread) {
            monitor.registarRaceCondition("contador", 
                new String[]{"Thread-NaoSync-0", "Thread-NaoSync-1", "Thread-NaoSync-2", 
                            "Thread-NaoSync-3", "Thread-NaoSync-4"},
                "Valor final (" + contadorNaoSincronizado + ") diferente do esperado (" + 
                    (numThreads * incrementosPorThread) + ") devido a acessos concorrentes não sincronizados");
        }
    }
    
    /**
     * Thread que incrementa contador SEM sincronização.
     */
    private class ThreadNaoSincronizada extends ThreadTarefa {
        private final int incrementos;
        
        public ThreadNaoSincronizada(String nome, MonitoreBPF monitor, 
                                     GestorRecursos gestorRecursos, int incrementos) {
            super(nome, monitor, gestorRecursos);
            this.incrementos = incrementos;
        }
        
        @Override
        protected void executarTarefa() throws InterruptedException {
            for (int i = 0; i < incrementos; i++) {
                // Acesso NÃO sincronizado - causa race condition!
                // Operação de leitura-modificação-escrita não é atómica
                contadorNaoSincronizado++;
            }
        }
    }
    
    /**
     * Thread que incrementa contador COM sincronização.
     */
    private class ThreadSincronizada extends ThreadTarefa {
        private final int incrementos;
        
        public ThreadSincronizada(String nome, MonitoreBPF monitor, 
                                  GestorRecursos gestorRecursos, int incrementos) {
            super(nome, monitor, gestorRecursos);
            this.incrementos = incrementos;
        }
        
        @Override
        protected void executarTarefa() throws InterruptedException {
            for (int i = 0; i < incrementos; i++) {
                // Acesso sincronizado usando recurso partilhado
                adquirirRecurso("contador");
                try {
                    contadorSincronizado++;
                } finally {
                    libertarRecurso("contador");
                }
            }
        }
    }
}

