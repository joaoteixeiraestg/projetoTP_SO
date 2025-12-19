package monitor;

import utils.Logger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Monitor de Concorrência inspirado no eBPF.
 * 
 * O eBPF permite monitorização de eventos do kernel sem alterar o seu código.
 * Esta classe simula esse conceito, monitorizando:
 * - Acessos a recursos partilhados
 * - Deteção de race conditions, deadlocks e starvation
 * - Geração de alertas e estatísticas
 */
public class MonitoreBPF {
    
    // Configurações
    private static final long LIMITE_STARVATION_MS = 5000; // 5 segundos para considerar starvation
    private static final long INTERVALO_VERIFICACAO_MS = 1000; // Verificar deadlocks a cada segundo
    
    // Estruturas de dados para monitorização
    private final List<Evento> historicoEventos;
    private final List<Alerta> historicoAlertas;
    private final Map<String, EstadoThread> estadoThreads;
    private final Map<String, Long> temposEspera; // threadId -> timestamp início espera
    private final Map<String, Set<String>> recursosDetidos; // threadId -> recursos que detém
    private final Map<String, String> recursosAguardados; // threadId -> recurso que aguarda
    
    // Estatísticas
    private final Map<String, Integer> acessosPorThread;
    private final Map<String, Long> tempoTotalEsperaPorThread;
    private final List<String> ordemExclusividade; // Ordem em que threads obtiveram recursos
    
    // Logger para ficheiro
    private final Logger logger;
    
    // Controlo
    private volatile boolean ativo;
    private Thread threadVerificacao;
    
    public MonitoreBPF(String pastaLogs) {
        this.historicoEventos = new CopyOnWriteArrayList<>();
        this.historicoAlertas = new CopyOnWriteArrayList<>();
        this.estadoThreads = new ConcurrentHashMap<>();
        this.temposEspera = new ConcurrentHashMap<>();
        this.recursosDetidos = new ConcurrentHashMap<>();
        this.recursosAguardados = new ConcurrentHashMap<>();
        this.acessosPorThread = new ConcurrentHashMap<>();
        this.tempoTotalEsperaPorThread = new ConcurrentHashMap<>();
        this.ordemExclusividade = new CopyOnWriteArrayList<>();
        this.logger = new Logger(pastaLogs);
        this.ativo = false;
    }
    

     // Inicia o monitor e a thread de verificação periódica.

    public void iniciar() {
        ativo = true;
        threadVerificacao = new Thread(this::verificacaoPeriodica, "Monitor-eBPF");
        threadVerificacao.setDaemon(true);
        threadVerificacao.start();
        System.out.println("[MonitoreBPF] Monitor iniciado. Verificação periódica ativa.");
    }
    

     // Para o monitor.

    public void parar() {
        ativo = false;
        if (threadVerificacao != null) {
            threadVerificacao.interrupt();
        }
        logger.fechar();
        System.out.println("[MonitoreBPF] Monitor parado.");
    }
    

     // Regista um evento no sistema.

    public void registarEvento(TipoEvento tipo, String threadId, String recursoId, String descricao) {
        registarEvento(tipo, threadId, recursoId, descricao, 0);
    }
    
    // Regista evento com tempo de espera.

    public void registarEvento(TipoEvento tipo, String threadId, String recursoId, 
                               String descricao, long tempoEspera) {
        Evento evento = new Evento(tipo, threadId, recursoId, descricao, tempoEspera);
        historicoEventos.add(evento);
        logger.registarEvento(evento);
        
        // Atualizar estruturas de monitorização
        processarEvento(evento);
        
        // Mostrar evento na consola (pode ser desativado)
        System.out.println(evento);
    }

    /**
     * Processa um evento e atualiza as estruturas de monitorização.
     */
    private void processarEvento(Evento evento) {
        String threadId = evento.getThreadId();
        String recursoId = evento.getRecursoId();
        
        switch (evento.getTipo()) {
            case THREAD_INICIADA:
                estadoThreads.put(threadId, EstadoThread.PRONTA);
                acessosPorThread.putIfAbsent(threadId, 0);
                tempoTotalEsperaPorThread.putIfAbsent(threadId, 0L);
                break;
                
            case PEDIDO_RECURSO:
                estadoThreads.put(threadId, EstadoThread.BLOQUEADA);
                recursosAguardados.put(threadId, recursoId);
                break;
                
            case INICIO_ESPERA:
                temposEspera.put(threadId, System.currentTimeMillis());
                break;
                
            case RECURSO_ADQUIRIDO:
                estadoThreads.put(threadId, EstadoThread.EM_EXECUCAO);
                recursosDetidos.computeIfAbsent(threadId, k -> new HashSet<>()).add(recursoId);
                recursosAguardados.remove(threadId);
                acessosPorThread.merge(threadId, 1, Integer::sum);
                ordemExclusividade.add(threadId + "->" + recursoId);
                
                // Atualizar tempo total de espera
                Long inicioEspera = temposEspera.remove(threadId);
                if (inicioEspera != null) {
                    long tempoEspera = System.currentTimeMillis() - inicioEspera;
                    tempoTotalEsperaPorThread.merge(threadId, tempoEspera, Long::sum);
                }
                break;
                
            case RECURSO_LIBERTADO:
                Set<String> recursos = recursosDetidos.get(threadId);
                if (recursos != null) {
                    recursos.remove(recursoId);
                }
                break;
                
            case THREAD_TERMINADA:
                estadoThreads.put(threadId, EstadoThread.TERMINADA);
                recursosDetidos.remove(threadId);
                recursosAguardados.remove(threadId);
                temposEspera.remove(threadId);
                break;
        }
    }

    /**
     * Verificação periódica de deadlocks e starvation.
     */
    private void verificacaoPeriodica() {
        while (ativo) {
            try {
                Thread.sleep(INTERVALO_VERIFICACAO_MS);
                verificarDeadlocks();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Ocorre quando uma thread espera por tempo excessivo.
     */
    public void verificarStarvation(String threadId, String recursoId, long inicioEspera) {
        long tempoEspera = System.currentTimeMillis() - inicioEspera;

        if (tempoEspera > LIMITE_STARVATION_MS) {
            Alerta alerta = new Alerta(
                TipoAlerta.CRITICO,
                TipoEvento.STARVATION,
                "STARVATION detetada! Thread " + threadId + " está à espera há " +
                    tempoEspera + "ms pelo recurso " + recursoId,
                new String[]{threadId},
                new String[]{recursoId}
            );
            historicoAlertas.add(alerta);
            logger.registarAlerta(alerta);
            System.out.println("\n" + alerta + "\n");
        }
    }

    /**
     * Verifica deadlocks usando Wait-for Graph (slides T-05).
     * Deadlock ocorre quando existe um ciclo no grafo de espera.
     */
    public void verificarDeadlocks() {
        // Construir grafo de espera (Wait-for Graph)
        // Aresta de Ti para Tj significa: Ti espera por recurso que Tj detém
        Map<String, Set<String>> grafoEspera = new HashMap<>();

        for (Map.Entry<String, String> entry : recursosAguardados.entrySet()) {
            String threadEspera = entry.getKey();
            String recursoAguardado = entry.getValue();

            // Encontrar quem detém o recurso
            for (Map.Entry<String, Set<String>> detido : recursosDetidos.entrySet()) {
                String threadDetentora = detido.getKey();
                if (detido.getValue().contains(recursoAguardado)) {
                    grafoEspera.computeIfAbsent(threadEspera, k -> new HashSet<>())
                              .add(threadDetentora);
                }
            }
        }

        // Detetar ciclos no grafo (indica deadlock)
        Set<String> ciclo = detetarCiclo(grafoEspera);

        if (!ciclo.isEmpty()) {
            // Encontrar recursos envolvidos
            Set<String> recursosEnvolvidos = new HashSet<>();
            for (String thread : ciclo) {
                String recurso = recursosAguardados.get(thread);
                if (recurso != null) {
                    recursosEnvolvidos.add(recurso);
                }
            }

            Alerta alerta = new Alerta(
                TipoAlerta.CRITICO,
                TipoEvento.DEADLOCK_CONFIRMADO,
                "DEADLOCK CONFIRMADO! Espera circular detetada entre threads.",
                ciclo.toArray(new String[0]),
                recursosEnvolvidos.toArray(new String[0])
            );
            historicoAlertas.add(alerta);
            logger.registarAlerta(alerta);
            System.out.println("\n" + alerta + "\n");
        }
    }

    /**
     * Deteta ciclos no grafo usando DFS.
     */
    private Set<String> detetarCiclo(Map<String, Set<String>> grafo) {
        Set<String> visitados = new HashSet<>();
        Set<String> emProcessamento = new HashSet<>();
        Set<String> ciclo = new HashSet<>();

        for (String no : grafo.keySet()) {
            if (dfsDetectarCiclo(no, grafo, visitados, emProcessamento, ciclo)) {
                return ciclo;
            }
        }
        return Collections.emptySet();
    }

    private boolean dfsDetectarCiclo(String no, Map<String, Set<String>> grafo,
                                     Set<String> visitados, Set<String> emProcessamento,
                                     Set<String> ciclo) {
        if (emProcessamento.contains(no)) {
            ciclo.add(no);
            return true;
        }
        if (visitados.contains(no)) {
            return false;
        }

        visitados.add(no);
        emProcessamento.add(no);

        Set<String> vizinhos = grafo.get(no);
        if (vizinhos != null) {
            for (String vizinho : vizinhos) {
                if (dfsDetectarCiclo(vizinho, grafo, visitados, emProcessamento, ciclo)) {
                    ciclo.add(no);
                    return true;
                }
            }
        }

        emProcessamento.remove(no);
        return false;
    }

    /**
     * Regista potencial race condition (acesso concorrente não sincronizado).
     */
    public void registarRaceCondition(String recursoId, String[] threadsEnvolvidas, String descricao) {
        Alerta alerta = new Alerta(
            TipoAlerta.PERIGO,
            TipoEvento.RACE_CONDITION,
            "RACE CONDITION detetada! " + descricao,
            threadsEnvolvidas,
            new String[]{recursoId}
        );
        historicoAlertas.add(alerta);
        logger.registarAlerta(alerta);
        System.out.println("\n" + alerta + "\n");
    }

    /**
     * Regista deadlock potencial (quando se deteta padrão perigoso).
     */
    public void registarDeadlockPotencial(String[] threads, String[] recursos, String descricao) {
        Alerta alerta = new Alerta(
            TipoAlerta.PERIGO,
            TipoEvento.DEADLOCK_POTENCIAL,
            "DEADLOCK POTENCIAL! " + descricao,
            threads,
            recursos
        );
        historicoAlertas.add(alerta);
        logger.registarAlerta(alerta);
        System.out.println("\n" + alerta + "\n");
    }

    // Getters para estatísticas
    public List<Evento> getHistoricoEventos() { return new ArrayList<>(historicoEventos); }
    public List<Alerta> getHistoricoAlertas() { return new ArrayList<>(historicoAlertas); }
    public Map<String, Integer> getAcessosPorThread() { return new HashMap<>(acessosPorThread); }
    public Map<String, Long> getTempoTotalEsperaPorThread() { return new HashMap<>(tempoTotalEsperaPorThread); }
    public List<String> getOrdemExclusividade() { return new ArrayList<>(ordemExclusividade); }
    public Map<String, EstadoThread> getEstadoThreads() { return new HashMap<>(estadoThreads); }
}

