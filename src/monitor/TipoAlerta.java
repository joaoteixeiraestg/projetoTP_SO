package monitor;

/**
 * Enumeração dos tipos de alertas de segurança.
 */
public enum TipoAlerta {
    INFO,       // Informação geral
    AVISO,      // Aviso de situação potencialmente problemática
    PERIGO,     // Situação perigosa detetada (ex: deadlock potencial)
    CRITICO     // Situação crítica confirmada (ex: deadlock confirmado, starvation)
}

