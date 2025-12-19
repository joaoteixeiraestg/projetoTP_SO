package recursos;

import monitor.MonitoreBPF;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;

/**
 * Classe que gere todos os recursos partilhados do sistema.
 */
public class GestorRecursos {
    private final Map<String, RecursoPartilhado> recursos;
    private final MonitoreBPF monitor;
    
    public GestorRecursos(MonitoreBPF monitor) {
        this.recursos = new HashMap<>();
        this.monitor = monitor;
    }
    
    /**
     * Cria um novo recurso partilhado no sistema.
     * @param id Identificador único do recurso
     * @param nome Nome descritivo do recurso
     * @return O recurso criado
     */
    public RecursoPartilhado criarRecurso(String id, String nome) {
        RecursoPartilhado recurso = new RecursoPartilhado(id, nome, monitor);
        recursos.put(id, recurso);
        System.out.println("[GestorRecursos] Recurso criado: " + id + " (" + nome + ")");
        return recurso;
    }
    
    /**
     * Obtém um recurso pelo seu ID.
     * @param id Identificador do recurso
     * @return O recurso ou null se não existir
     */
    public RecursoPartilhado obterRecurso(String id) {
        return recursos.get(id);
    }
    
    /**
     * Verifica se um recurso existe.
     * @param id Identificador do recurso
     * @return true se existir, false caso contrário
     */
    public boolean existeRecurso(String id) {
        return recursos.containsKey(id);
    }
    
    /**
     * Obtém todos os recursos do sistema.
     * @return Coleção imutável de recursos
     */
    public Collection<RecursoPartilhado> obterTodosRecursos() {
        return Collections.unmodifiableCollection(recursos.values());
    }
    
    /**
     * Obtém o número total de recursos.
     * @return Número de recursos
     */
    public int getNumeroRecursos() {
        return recursos.size();
    }
    
    /**
     * Remove um recurso do sistema.
     * @param id Identificador do recurso a remover
     */
    public void removerRecurso(String id) {
        recursos.remove(id);
    }
    
    /**
     * Mostra o estado atual de todos os recursos.
     */
    public void mostrarEstado() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║            ESTADO DOS RECURSOS PARTILHADOS                   ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        
        for (RecursoPartilhado r : recursos.values()) {
            String estado = r.estaOcupado() ? "OCUPADO por " + r.getThreadAtual() : "LIVRE";
            System.out.printf("║ %-10s | %-15s | %-25s ║%n", 
                r.getId(), r.getNome(), estado);
            
            if (!r.getFilaEspera().isEmpty()) {
                System.out.printf("║            | Fila espera: %-30s ║%n", 
                    r.getFilaEspera().toString());
            }
        }
        
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
    }
    
    /**
     * Obtém informação sobre recursos ocupados (para deteção de deadlock).
     * @return Mapa de threadId -> lista de recursos que detém
     */
    public Map<String, String> getRecursosOcupados() {
        Map<String, String> ocupados = new HashMap<>();
        for (RecursoPartilhado r : recursos.values()) {
            if (r.estaOcupado()) {
                ocupados.put(r.getThreadAtual(), r.getId());
            }
        }
        return ocupados;
    }
}

