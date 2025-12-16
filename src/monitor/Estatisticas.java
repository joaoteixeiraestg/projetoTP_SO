package monitor;

import java.util.List;
import java.util.Map;

/**
 * Classe para apresentação das estatísticas recolhidas pelo monitor.
 * Mostra informações sobre:
 * - Número de acessos por thread
 * - Ordem de obtenção de exclusividade
 * - Tempo médio de espera para entrada em secções críticas
 */
public class Estatisticas {
    
    private final MonitoreBPF monitor;
    
    public Estatisticas(MonitoreBPF monitor) {
        this.monitor = monitor;
    }
    
    /**
     * Mostra todas as estatísticas recolhidas.
     */
    public void mostrarEstatisticas() {
        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    ESTATÍSTICAS DO SIMULADOR eBPF                        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝");
        
        mostrarAcessosPorThread();
        mostrarTemposEspera();
        mostrarOrdemExclusividade();
        mostrarResumoAlertas();
        mostrarEstadoThreads();
    }
    
    /**
     * Mostra o número de acessos a recursos por thread.
     */
    private void mostrarAcessosPorThread() {
        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                    NÚMERO DE ACESSOS POR THREAD                          │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
        
        Map<String, Integer> acessos = monitor.getAcessosPorThread();
        
        if (acessos.isEmpty()) {
            System.out.println("│  Nenhum acesso registado.                                                │");
        } else {
            int totalAcessos = 0;
            for (Map.Entry<String, Integer> entry : acessos.entrySet()) {
                System.out.printf("│  %-25s : %5d acessos                               │%n", 
                    entry.getKey(), entry.getValue());
                totalAcessos += entry.getValue();
            }
            System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
            System.out.printf("│  TOTAL                      : %5d acessos                               │%n", totalAcessos);
        }
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘");
    }
    
    /**
     * Mostra os tempos de espera por thread.
     */
    private void mostrarTemposEspera() {
        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│               TEMPO DE ESPERA EM SECÇÕES CRÍTICAS                        │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
        
        Map<String, Long> tempos = monitor.getTempoTotalEsperaPorThread();
        Map<String, Integer> acessos = monitor.getAcessosPorThread();
        
        if (tempos.isEmpty()) {
            System.out.println("│  Nenhum tempo de espera registado.                                       │");
        } else {
            long tempoTotal = 0;
            for (Map.Entry<String, Long> entry : tempos.entrySet()) {
                String threadId = entry.getKey();
                long tempo = entry.getValue();
                int numAcessos = acessos.getOrDefault(threadId, 1);
                long tempoMedio = numAcessos > 0 ? tempo / numAcessos : 0;
                
                System.out.printf("│  %-20s | Total: %6dms | Médio: %6dms                   │%n", 
                    threadId, tempo, tempoMedio);
                tempoTotal += tempo;
            }
            System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
            System.out.printf("│  TEMPO TOTAL DE ESPERA: %6dms                                          │%n", tempoTotal);
        }
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘");
    }
    
    /**
     * Mostra a ordem em que as threads obtiveram exclusividade.
     */
    private void mostrarOrdemExclusividade() {
        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                  ORDEM DE OBTENÇÃO DE EXCLUSIVIDADE                      │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
        
        List<String> ordem = monitor.getOrdemExclusividade();
        
        if (ordem.isEmpty()) {
            System.out.println("│  Nenhuma aquisição registada.                                            │");
        } else {
            int i = 1;
            for (String aquisicao : ordem) {
                System.out.printf("│  %3d. %-65s    │%n", i++, aquisicao);
            }
        }
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘");
    }
    
    /**
     * Mostra resumo dos alertas gerados.
     */
    private void mostrarResumoAlertas() {
        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                      RESUMO DE ALERTAS DE SEGURANÇA                      │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
        
        List<Alerta> alertas = monitor.getHistoricoAlertas();
        
        int raceConditions = 0, deadlocks = 0, starvation = 0;
        
        for (Alerta a : alertas) {
            switch (a.getEventoOrigem()) {
                case RACE_CONDITION: raceConditions++; break;
                case DEADLOCK_POTENCIAL:
                case DEADLOCK_CONFIRMADO: deadlocks++; break;
                case STARVATION: starvation++; break;
            }
        }
        
        System.out.printf("│  Race Conditions detetadas:    %3d                                        │%n", raceConditions);
        System.out.printf("│  Deadlocks detetados:          %3d                                        │%n", deadlocks);
        System.out.printf("│  Situações de Starvation:      %3d                                        │%n", starvation);
        System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
        System.out.printf("│  TOTAL DE ALERTAS:             %3d                                        │%n", alertas.size());
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘");
    }
    
    /**
     * Mostra o estado atual das threads.
     */
    private void mostrarEstadoThreads() {
        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                        ESTADO FINAL DAS THREADS                          │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
        
        Map<String, EstadoThread> estados = monitor.getEstadoThreads();
        
        for (Map.Entry<String, EstadoThread> entry : estados.entrySet()) {
            System.out.printf("│  %-25s : %-20s                       │%n", 
                entry.getKey(), entry.getValue());
        }
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");
    }
}

