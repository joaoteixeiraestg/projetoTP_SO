package monitor;

/**
 * Enumeração dos tipos de alertas de segurança.
 * Baseado nos problemas de sincronização (slides T-04) e deadlocks (slides T-05).
 */
public enum TipoAlerta {
    INFO,       // Informação geral
    AVISO,      // Aviso de situação potencialmente problemática
    PERIGO,     // Situação perigosa detetada (ex: deadlock potencial)
    CRITICO     // Situação crítica confirmada (ex: deadlock confirmado, starvation)
}

