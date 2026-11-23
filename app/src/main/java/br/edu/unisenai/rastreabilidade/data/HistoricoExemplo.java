package br.edu.unisenai.rastreabilidade.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.edu.unisenai.rastreabilidade.model.Evento;

/** Histórico de exemplo em memória até a integração com o Firestore. */
public final class HistoricoExemplo {
    public static final String ATIVO = "ativo";
    public static final String REPROVADO = "reprovado";

    public interface Ouvinte {
        void aoAtualizar(String status, List<Evento> eventos);
    }

    private static final Map<String, String> status = new HashMap<>();
    private static final Map<String, List<Evento>> historicos = new HashMap<>();
    private static final Map<Ouvinte, String> ouvintes = new HashMap<>();

    static {
        incluir("EXEMPLO-ATIVO");
        etapa("EXEMPLO-ATIVO", "Corte", 1, "operador-ana", "Ana Souza", horario(8, 10));
        etapa("EXEMPLO-ATIVO", "Solda", 2, "operador-bruno", "Bruno Lima", horario(9, 25));

        incluir("EXEMPLO-REPROVADO");
        etapa("EXEMPLO-REPROVADO", "Corte", 1, "operador-ana", "Ana Souza", horario(7, 40));
        etapa("EXEMPLO-REPROVADO", "Solda", 2, "operador-bruno", "Bruno Lima", horario(8, 55));
        etapa("EXEMPLO-REPROVADO", "Pintura", 3, "operador-davi", "Davi Rocha", horario(10, 5));
        historicos.get("EXEMPLO-REPROVADO").add(Evento.reprovacao(
                "Pintura descascando na borda das peças.", "qualidade-carla", "Carla Mendes",
                horario(11, 30)));
        status.put("EXEMPLO-REPROVADO", REPROVADO);
    }

    private HistoricoExemplo() {
    }

    public static boolean existe(String loteId) {
        return status.containsKey(loteId);
    }

    /** Inclui um lote ativo sem eventos, como o lote recém-criado no aparelho. */
    public static void incluir(String loteId) {
        if (!existe(loteId)) {
            status.put(loteId, ATIVO);
            historicos.put(loteId, new ArrayList<>());
        }
    }

    public static void ouvir(String loteId, Ouvinte ouvinte) {
        ouvintes.put(ouvinte, loteId);
        avisar(ouvinte, loteId);
    }

    public static void parar(Ouvinte ouvinte) {
        ouvintes.remove(ouvinte);
    }

    private static void etapa(String loteId, String etapa, int ordem, String autorUid,
                              String autorNome, long registradoEm) {
        historicos.get(loteId).add(Evento.etapa(etapa, ordem, autorUid, autorNome, registradoEm));
    }

    private static void avisar(Ouvinte ouvinte, String loteId) {
        List<Evento> eventos = new ArrayList<>(historicos.get(loteId));
        eventos.sort(Comparator.comparingLong(evento -> evento.registradoEm));
        ouvinte.aoAtualizar(status.get(loteId), eventos);
    }

    private static long horario(int hora, int minuto) {
        return new GregorianCalendar(2026, Calendar.NOVEMBER, 23, hora, minuto).getTimeInMillis();
    }
}
