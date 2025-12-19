package monitor;

/**
 * Enumeração dos tipos de eventos monitorizados pelo sistema.
 */
public enum TipoEvento {
    // Eventos de acesso a recursos
    PEDIDO_RECURSO,         // Thread solicita acesso a um recurso
    RECURSO_ADQUIRIDO,      // Thread obteve acesso ao recurso (entrou na secção crítica)
    RECURSO_LIBERTADO,      // Thread libertou o recurso (saiu da secção crítica)
    
    // Eventos de espera
    INICIO_ESPERA,          // Thread começou a aguardar por um recurso
    FIM_ESPERA,             // Thread terminou de aguardar
    
    // Eventos de sincronização
    LOCK_ACQUIRE,
    LOCK_RELEASE,
    
    // Eventos de problemas detetados
    RACE_CONDITION,         // Acesso concorrente não sincronizado detetado
    DEADLOCK_POTENCIAL,     // Possível situação de deadlock detetada
    DEADLOCK_CONFIRMADO,    // Deadlock confirmado
    STARVATION,             // Thread em espera por tempo excessivo
    
    // Eventos de ciclo de vida da thread
    THREAD_INICIADA,        // Thread começou execução
    THREAD_TERMINADA,       // Thread terminou execução
    THREAD_BLOQUEADA,       // Thread ficou bloqueada
    THREAD_DESBLOQUEADA     // Thread foi desbloqueada
}

