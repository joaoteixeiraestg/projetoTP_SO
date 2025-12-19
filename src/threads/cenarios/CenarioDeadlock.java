package threads.cenarios;

import recursos.GestorRecursos;
import recursos.RecursoPartilhado;
import monitor.MonitoreBPF;
import monitor.TipoEvento;
import threads.ThreadTarefa;

/**
 * Cenário que demonstra Deadlock (Bloqueio).
 *
 * Condições necessárias para deadlock:
 * 1. Exclusão mútua - recursos não podem ser partilhados
 * 2. Posse e espera - thread detém recurso enquanto espera por outro
 * 3. Não preempção - recursos só podem ser libertados voluntariamente
 * 4. Espera circular - T1 espera por T2, T2 espera por T1
 * 
 * Implicações de Cibersegurança:
 * - Ataques DoS (Denial of Service)
 * - Bloqueio de serviços críticos
 */
public class CenarioDeadlock {
    
    private final MonitoreBPF monitor;
    private final GestorRecursos gestorRecursos;
    
    public CenarioDeadlock(MonitoreBPF monitor, GestorRecursos gestorRecursos) {
        this.monitor = monitor;
        this.gestorRecursos = gestorRecursos;
    }
    
    /**
     * Executa o cenário de deadlock.
     */
    public void executar() throws InterruptedException {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║              CENÁRIO: DEADLOCK (Bloqueio)                    ║");
        System.out.println("║                                                              ║");
        System.out.println("║  Duas threads vão tentar adquirir dois recursos em ordem     ║");
        System.out.println("║  diferente, causando espera circular (deadlock).             ║");
        System.out.println("║                                                              ║");
        System.out.println("║  Thread-A: adquire R1, depois tenta R2                       ║");
        System.out.println("║  Thread-B: adquire R2, depois tenta R1                       ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
        
        // Criar os dois recursos
        gestorRecursos.criarRecurso("R1", "Recurso 1");
        gestorRecursos.criarRecurso("R2", "Recurso 2");
        
        // Avisar sobre deadlock potencial
        monitor.registarDeadlockPotencial(
            new String[]{"Thread-A", "Thread-B"},
            new String[]{"R1", "R2"},
            "Threads vão adquirir recursos em ordem diferente - risco de espera circular"
        );
        
        // Criar as threads que vão causar deadlock
        Thread threadA = new ThreadDeadlockA("Thread-A", monitor, gestorRecursos);
        Thread threadB = new ThreadDeadlockB("Thread-B", monitor, gestorRecursos);
        
        System.out.println("--- Iniciando threads (deadlock esperado em ~2 segundos) ---\n");
        
        threadA.start();
        threadB.start();
        
        // Aguardar um tempo limitado (as threads ficarão bloqueadas em deadlock)
        threadA.join(10000); // Timeout de 10 segundos
        threadB.join(10000);
        
        // Verificar se ainda estão vivas (indica deadlock)
        if (threadA.isAlive() || threadB.isAlive()) {
            System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║  ⚠ DEADLOCK DETETADO!                                        ║");
            System.out.println("║                                                              ║");
            System.out.println("║  As threads estão bloqueadas em espera circular.             ║");
            System.out.println("║  Isto demonstra as 4 condições de deadlock:                  ║");
            System.out.println("║  1. Exclusão mútua ✓                                         ║");
            System.out.println("║  2. Posse e espera ✓                                         ║");
            System.out.println("║  3. Não preempção ✓                                          ║");
            System.out.println("║  4. Espera circular ✓                                        ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
            
            // Forçar interrupção das threads para continuar a execução
            threadA.interrupt();
            threadB.interrupt();
        }
    }
    
    /**
     * Thread A: Adquire R1, depois R2.
     */
    private class ThreadDeadlockA extends ThreadTarefa {
        
        public ThreadDeadlockA(String nome, MonitoreBPF monitor, GestorRecursos gestorRecursos) {
            super(nome, monitor, gestorRecursos);
        }
        
        @Override
        protected void executarTarefa() throws InterruptedException {
            System.out.println("[Thread-A] Tentando adquirir R1...");
            adquirirRecurso("R1");
            System.out.println("[Thread-A] R1 adquirido!");
            
            // Pequena pausa para garantir que Thread-B também adquire R2
            simularTrabalho(100);
            
            System.out.println("[Thread-A] Tentando adquirir R2...");
            adquirirRecurso("R2"); // Vai bloquear aqui (Thread-B tem R2)
            System.out.println("[Thread-A] R2 adquirido!");
            
            // Libertar recursos (nunca chega aqui em caso de deadlock)
            libertarRecurso("R2");
            libertarRecurso("R1");
        }
    }
    
    /**
     * Thread B: Adquire R2, depois R1 (ordem inversa - causa deadlock).
     */
    private class ThreadDeadlockB extends ThreadTarefa {
        
        public ThreadDeadlockB(String nome, MonitoreBPF monitor, GestorRecursos gestorRecursos) {
            super(nome, monitor, gestorRecursos);
        }
        
        @Override
        protected void executarTarefa() throws InterruptedException {
            System.out.println("[Thread-B] Tentando adquirir R2...");
            adquirirRecurso("R2");
            System.out.println("[Thread-B] R2 adquirido!");
            
            // Pequena pausa para garantir que Thread-A também adquire R1
            simularTrabalho(100);
            
            System.out.println("[Thread-B] Tentando adquirir R1...");
            adquirirRecurso("R1"); // Vai bloquear aqui (Thread-A tem R1)
            System.out.println("[Thread-B] R1 adquirido!");
            
            // Libertar recursos (nunca chega aqui em caso de deadlock)
            libertarRecurso("R1");
            libertarRecurso("R2");
        }
    }
    public void executarSolucao() throws InterruptedException {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          SOLUÇÃO DEADLOCK: ORDENAÇÃO DE RECURSOS             ║");
        System.out.println("║                                                              ║");
        System.out.println("║  Para prevenir o deadlock, impomos uma ordem global:         ║");
        System.out.println("║  Todas as threads devem pedir R1 antes de R2.                ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        gestorRecursos.criarRecurso("R1", "Recurso 1");
        gestorRecursos.criarRecurso("R2", "Recurso 2");

        // Ambas usam a lógica segura (sempre R1 depois R2)
        Thread threadA = new ThreadSegura("Thread-Segura-A", monitor, gestorRecursos);
        Thread threadB = new ThreadSegura("Thread-Segura-B", monitor, gestorRecursos);

        threadA.start();
        threadB.start();

        threadA.join();
        threadB.join();

        System.out.println("\n[Sistema] Execução concluída sem Deadlock!");
    }

    /**
     * Thread Segura: Respeita a ordem dos recursos (Sempre R1 -> R2).
     */
    private class ThreadSegura extends ThreadTarefa {
        public ThreadSegura(String nome, MonitoreBPF monitor, GestorRecursos gestorRecursos) {
            super(nome, monitor, gestorRecursos);
        }

        @Override
        protected void executarTarefa() throws InterruptedException {
            // Ordem rigorosa: Sempre R1 primeiro
            System.out.println("[" + getName() + "] A pedir R1 (Ordem correta)...");
            adquirirRecurso("R1");

            simularTrabalho(50); // Simula processamento

            System.out.println("[" + getName() + "] A pedir R2 (Ordem correta)...");
            adquirirRecurso("R2");

            System.out.println("[" + getName() + "] Tenho ambos! A trabalhar...");
            simularTrabalho(500);

            // Libertar na ordem inversa
            libertarRecurso("R2");
            libertarRecurso("R1");
            System.out.println("[" + getName() + "] Recursos libertados.");
        }
    }
}

