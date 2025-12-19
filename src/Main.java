import monitor.MonitoreBPF;
import monitor.Estatisticas;
import recursos.GestorRecursos;
import threads.cenarios.CenarioRaceCondition;
import threads.cenarios.CenarioDeadlock;
import threads.cenarios.CenarioStarvation;
import java.util.Scanner;

/**
 * Classe principal do Simulador de Concorrência com Monitor eBPF.
 * * Este programa simula cenários de concorrência entre threads no acesso a
 * recursos partilhados, implementando um mecanismo de monitorização inspirado
 * no eBPF (extended Berkeley Packet Filter) para detetar:
 * - Race Conditions
 * - Deadlocks
 * - Starvation
 */
public class Main {

    private static MonitoreBPF monitor;
    private static GestorRecursos gestorRecursos;
    private static Estatisticas estatisticas;
    private static Scanner scanner;

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                          ║");
        System.out.println("║          SIMULADOR DE CONCORRÊNCIA COM MONITOR eBPF                      ║");
        System.out.println("║                                                                          ║");
        System.out.println("║          Trabalho Prático de Sistemas Operativos                         ║");
        System.out.println("║                                                                          ║");
        System.out.println("║        Desenvolvido por João Teixeira e Renato Barbosa                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝");

        // Inicializar componentes
        inicializar();

        // Mostrar menu
        menuPrincipal();

        // Finalizar
        finalizar();
    }

    private static void inicializar() {
        System.out.println("\n[Sistema] A inicializar componentes...\n");

        // Criar monitor eBPF
        monitor = new MonitoreBPF("logs");
        monitor.iniciar();

        // Criar gestor de recursos
        gestorRecursos = new GestorRecursos(monitor);

        // Criar módulo de estatísticas
        estatisticas = new Estatisticas(monitor);

        // Scanner para input do utilizador
        scanner = new Scanner(System.in);

        System.out.println("[Sistema] Componentes inicializados com sucesso!\n");
    }

    private static void menuPrincipal() {
        int opcao = -1;

        while (opcao != 0) {
            System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                      MENU PRINCIPAL                          ║");
            System.out.println("╠══════════════════════════════════════════════════════════════╣");
            System.out.println("║  --- CENÁRIOS COM PROBLEMAS (Demonstração de Erros) ---      ║");
            System.out.println("║  1. Executar Cenário RACE CONDITION                          ║");
            System.out.println("║  2. Executar Cenário DEADLOCK (Vai bloquear!)                ║");
            System.out.println("║  3. Executar Cenário STARVATION (Vai bloquear vítimas!)      ║");
            System.out.println("║                                                              ║");
            System.out.println("║  --- SOLUÇÕES (Correção dos Problemas) ---                   ║");
            System.out.println("║  7. Executar SOLUÇÃO Deadlock (Ordenação de Recursos)        ║");
            System.out.println("║  8. Executar SOLUÇÃO Starvation (Fairness/Cooperação)        ║");
            System.out.println("║                                                              ║");
            System.out.println("║  --- OUTROS ---                                              ║");
            System.out.println("║  4. Executar TODOS os cenários de erro                       ║");
            System.out.println("║  5. Ver ESTATÍSTICAS                                         ║");
            System.out.println("║  6. Ver estado dos RECURSOS                                  ║");
            System.out.println("║  0. SAIR                                                     ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
            System.out.print("\nEscolha uma opção: ");

            try {
                // Ler a linha toda e tentar converter para evitar erros de buffer
                String input = scanner.nextLine().trim();

                // Se o input não for vazio, tenta converter
                if (!input.isEmpty()) {
                    opcao = Integer.parseInt(input);

                    switch (opcao) {
                        case 1: executarCenarioRaceCondition(); break;
                        case 2: executarCenarioDeadlock(); break;
                        case 3: executarCenarioStarvation(); break;
                        case 4: executarTodosCenarios(); break;
                        case 5: estatisticas.mostrarEstatisticas(); break;
                        case 6: gestorRecursos.mostrarEstado(); break;
                        // NOVAS OPÇÕES DE SOLUÇÃO
                        case 7: executarSolucaoDeadlock(); break;
                        case 8: executarSolucaoStarvation(); break;
                        case 0: System.out.println("\nA terminar programa..."); break;
                        default: System.out.println("\nOpção inválida!"); break;
                    }
                }
            } catch (NumberFormatException e) {
                System.out.println("\nPor favor, introduza um número válido.");
            } catch (InterruptedException e) {
                System.out.println("\nExecução interrompida.");
                Thread.currentThread().interrupt();
            }

            if (opcao != 0) {
                System.out.print("\nPrima ENTER para continuar...");
                scanner.nextLine();
            }
        }
    }

    private static void executarCenarioRaceCondition() throws InterruptedException {
        CenarioRaceCondition cenario = new CenarioRaceCondition(monitor, gestorRecursos);
        cenario.executar();
    }

    private static void executarCenarioDeadlock() throws InterruptedException {
        CenarioDeadlock cenario = new CenarioDeadlock(monitor, gestorRecursos);
        cenario.executar();
    }

    private static void executarCenarioStarvation() throws InterruptedException {
        CenarioStarvation cenario = new CenarioStarvation(monitor, gestorRecursos);
        cenario.executar();
    }

    // --- MÉTODOS NOVOS PARA AS SOLUÇÕES ---

    private static void executarSolucaoDeadlock() throws InterruptedException {
        CenarioDeadlock cenario = new CenarioDeadlock(monitor, gestorRecursos);
        cenario.executarSolucao();
    }

    private static void executarSolucaoStarvation() throws InterruptedException {
        CenarioStarvation cenario = new CenarioStarvation(monitor, gestorRecursos);
        cenario.executarSolucao();
    }

    // --------------------------------------

    private static void executarTodosCenarios() throws InterruptedException {
        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("            EXECUÇÃO DE TODOS OS CENÁRIOS (ERROS)");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        executarCenarioRaceCondition();
        Thread.sleep(2000);

        executarCenarioDeadlock();
        Thread.sleep(2000);

        executarCenarioStarvation();

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("            TODOS OS CENÁRIOS CONCLUÍDOS");
        System.out.println("═══════════════════════════════════════════════════════════════\n");
    }

    private static void finalizar() {
        System.out.println("\n[Sistema] A finalizar...");

        // Mostrar estatísticas finais
        estatisticas.mostrarEstatisticas();

        // Parar monitor
        monitor.parar();

        // Fechar scanner
        scanner.close();

        System.out.println("[Sistema] Programa terminado. Verifique os ficheiros de log na pasta 'logs/'.");
    }
}