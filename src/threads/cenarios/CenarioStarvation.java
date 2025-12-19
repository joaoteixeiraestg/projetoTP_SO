package threads.cenarios;

import recursos.GestorRecursos;
import recursos.RecursoPartilhado;
import monitor.MonitoreBPF;
import monitor.TipoEvento;
import threads.ThreadTarefa;

/**
 * Cenário que demonstra Starvation..
 * 
 * Starvation:
 * Ocorre quando um processo/thread continua ativo no sistema mas não tem acesso
 * aos recursos computacionais de que necessita, devido ao favorecimento de outras
 * threads (ex: threads de maior prioridade).
 * 
 * Causas comuns:
 * - Prioridades mal configuradas
 * - Algoritmos de escalonamento injustos
 * - Threads "gananciosas" que monopolizam recursos
 *
 * Solução típica: Envelhecimento (aging) - aumentar prioridade com tempo de espera
 * 
 * Implicações de Cibersegurança:
 * - Negação de serviço a utilizadores legítimos
 * - Exploração por atacantes para degradar serviços
 */
public class CenarioStarvation {
    
    private final MonitoreBPF monitor;
    private final GestorRecursos gestorRecursos;
    
    public CenarioStarvation(MonitoreBPF monitor, GestorRecursos gestorRecursos) {
        this.monitor = monitor;
        this.gestorRecursos = gestorRecursos;
    }
    
    /**
     * Executa o cenário de starvation.
     */
    public void executar() throws InterruptedException {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║            CENÁRIO: STARVATION (Míngua/Inanição)             ║");
        System.out.println("║                                                              ║");
        System.out.println("║  Uma thread 'gananciosa' vai monopolizar um recurso,         ║");
        System.out.println("║  impedindo que outras threads consigam aceder.               ║");
        System.out.println("║                                                              ║");
        System.out.println("║  Thread-Gananciosa: mantém recurso por longos períodos       ║");
        System.out.println("║  Thread-Vitima: tenta aceder mas fica constantemente à espera║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
        
        // Criar o recurso
        gestorRecursos.criarRecurso("RecursoCritico", "Recurso Crítico");
        
        // Criar as threads
        Thread threadGananciosa = new ThreadGananciosa("Thread-Gananciosa", monitor, gestorRecursos);
        Thread threadVitima1 = new ThreadVitima("Thread-Vitima-1", monitor, gestorRecursos);
        Thread threadVitima2 = new ThreadVitima("Thread-Vitima-2", monitor, gestorRecursos);
        Thread threadVitima3 = new ThreadVitima("Thread-Vitima-3", monitor, gestorRecursos);
        
        System.out.println("--- Iniciando threads (starvation esperada) ---\n");
        
        // Iniciar a thread "gananciosa" primeiro
        threadGananciosa.start();
        
        // Pequena pausa para a thread "gananciosa" adquirir o recurso
        Thread.sleep(100);
        
        // Iniciar as vítimas
        threadVitima1.start();
        threadVitima2.start();
        threadVitima3.start();
        
        // Aguardar execução (com timeout)
        threadGananciosa.join(15000);
        threadVitima1.join(15000);
        threadVitima2.join(15000);
        threadVitima3.join(15000);
        
        // Verificar threads ainda bloqueadas
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    ANÁLISE DE STARVATION                     ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        
        if (threadVitima1.isAlive() || threadVitima2.isAlive() || threadVitima3.isAlive()) {
            System.out.println("║  ⚠ STARVATION DETETADA!                                      ║");
            System.out.println("║  Threads vítima ainda estão bloqueadas à espera do recurso.  ║");
        } else {
            System.out.println("║  Todas as threads conseguiram aceder ao recurso.             ║");
            System.out.println("║  (Verifique os tempos de espera nas estatísticas)            ║");
        }
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
        
        // Interromper threads que ainda estejam à espera
        threadGananciosa.interrupt();
        threadVitima1.interrupt();
        threadVitima2.interrupt();
        threadVitima3.interrupt();
    }
    
    /**
     * Thread que monopoliza o recurso.
     */
    private class ThreadGananciosa extends ThreadTarefa {
        
        public ThreadGananciosa(String nome, MonitoreBPF monitor, GestorRecursos gestorRecursos) {
            super(nome, monitor, gestorRecursos);
        }
        
        @Override
        protected void executarTarefa() throws InterruptedException {
            // Ciclo que monopoliza o recurso repetidamente
            for (int i = 0; i < 3 && executar; i++) {
                System.out.println("[Thread-Gananciosa] Tentando adquirir recurso (iteração " + (i+1) + ")...");
                adquirirRecurso("RecursoCritico");
                
                System.out.println("[Thread-Gananciosa] Recurso adquirido! A trabalhar por 3 segundos...");
                // Mantém o recurso por muito tempo
                simularTrabalho(3000);
                
                libertarRecurso("RecursoCritico");
                System.out.println("[Thread-Gananciosa] Recurso libertado.");
                
                // Pequena pausa antes de tentar novamente
                simularTrabalho(50);
            }
        }
    }
    
    /**
     * Thread que sofre starvation.
     */
    private class ThreadVitima extends ThreadTarefa {
        
        public ThreadVitima(String nome, MonitoreBPF monitor, GestorRecursos gestorRecursos) {
            super(nome, monitor, gestorRecursos);
        }
        
        @Override
        protected void executarTarefa() throws InterruptedException {
            System.out.println("[" + getName() + "] Tentando adquirir recurso...");
            
            // Esta thread vai esperar muito tempo devido à thread gananciosa
            adquirirRecurso("RecursoCritico");
            
            System.out.println("[" + getName() + "] Finalmente consegui o recurso!");
            simularTrabalho(100); // Usa rapidamente
            
            libertarRecurso("RecursoCritico");
            System.out.println("[" + getName() + "] Recurso libertado.");
        }
    }

    /**
     * Executa a SOLUÇÃO para Starvation usando "Polidez" (Cooperação).
     */
    public void executarSolucao() throws InterruptedException {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          SOLUÇÃO STARVATION: COOPERAÇÃO (Fairness)           ║");
        System.out.println("║                                                              ║");
        System.out.println("║  A thread 'Cooperativa' liberta o recurso e aguarda tempo    ║");
        System.out.println("║  suficiente para dar oportunidade às outras threads.         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        gestorRecursos.criarRecurso("RecursoCritico", "Recurso Crítico");

        Thread threadCooperativa = new ThreadCooperativa("Thread-Cooperativa", monitor, gestorRecursos);
        Thread threadVitima1 = new ThreadVitima("Thread-Vitima-1", monitor, gestorRecursos);
        Thread threadVitima2 = new ThreadVitima("Thread-Vitima-2", monitor, gestorRecursos);

        threadCooperativa.start();
        Thread.sleep(50);
        threadVitima1.start();
        threadVitima2.start();

        threadCooperativa.join();
        threadVitima1.join();
        threadVitima2.join();

        System.out.println("\n Todas as threads executaram com sucesso!");
    }

    /**
     * Thread Cooperativa: Usa o recurso mas espera tempo suficiente antes de voltar a tentar.
     */
    private class ThreadCooperativa extends ThreadTarefa {
        public ThreadCooperativa(String nome, MonitoreBPF monitor, GestorRecursos gestorRecursos) {
            super(nome, monitor, gestorRecursos);
        }

        @Override
        protected void executarTarefa() throws InterruptedException {
            for (int i = 0; i < 3; i++) {
                System.out.println("[" + getName() + "] A pedir recurso...");
                adquirirRecurso("RecursoCritico");

                System.out.println("[" + getName() + "] A usar recurso (Rapidamente)...");
                simularTrabalho(500);

                libertarRecurso("RecursoCritico");

                System.out.println("[" + getName() + "] Em pausa (Dando vez aos outros)...");
                // Espera tempo suficiente para as outras threads entrarem
                simularTrabalho(1000);
            }
        }
    }
}

