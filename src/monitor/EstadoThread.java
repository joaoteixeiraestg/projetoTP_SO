    package monitor;

/**
 * Enumeração dos estados possíveis de uma thread.
 * - Novo (new): Processo está a ser criado
 * - Execução (running): Código está a ser executado pelo processador
 * - Espera (waiting): Processo pediu uma operação e aguarda conclusão
 * - Pronto (ready): Processo está suspenso mas pronto para ser continuado
 * - Terminado (terminated): Processo acabou
 */
public enum EstadoThread {
    NOVA,           // Thread criada mas ainda não iniciada (new)
    PRONTA,         // Thread pronta para executar (ready)
    EM_EXECUCAO,    // Thread em execução (running)
    BLOQUEADA,      // Thread bloqueada à espera de recurso (waiting)
    TERMINADA       // Thread terminou execução (terminated)
}

