package br.edu.unisenai.rastreabilidade.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SequenciaEtapas {
    private static final int MAX_ETAPAS = 100;
    private static final int MAX_NOME = 100;

    private SequenciaEtapas() {
    }

    public static List<Etapa> analisar(String texto) {
        if (texto == null || texto.isEmpty()) {
            throw new IllegalArgumentException("Cadastre pelo menos uma etapa.");
        }
        String[] linhas = texto.split("\\R", -1);
        if (linhas.length > MAX_ETAPAS) {
            throw new IllegalArgumentException("Cadastre no máximo 100 etapas.");
        }
        List<Etapa> etapas = new ArrayList<>(linhas.length);
        for (int indice = 0; indice < linhas.length; indice++) {
            String nome = linhas[indice].trim();
            if (nome.isEmpty() || nome.length() > MAX_NOME) {
                throw new IllegalArgumentException("Cada etapa deve ter um nome de até 100 caracteres.");
            }
            etapas.add(new Etapa(nome, indice + 1));
        }
        return validar(etapas);
    }

    public static List<Etapa> validar(List<Etapa> etapas) {
        if (etapas == null || etapas.isEmpty() || etapas.size() > MAX_ETAPAS) {
            throw new IllegalArgumentException("Cadastre entre 1 e 100 etapas.");
        }
        List<Etapa> ordenadas = new ArrayList<>(etapas.size());
        Set<Integer> posicoes = new HashSet<>();
        for (Etapa etapa : etapas) {
            if (etapa == null || etapa.nome == null || etapa.nome.trim().isEmpty()
                    || etapa.nome.length() > MAX_NOME) {
                throw new IllegalArgumentException("Cada etapa deve ter um nome de até 100 caracteres.");
            }
            if (etapa.ordem < 1 || !posicoes.add(etapa.ordem)) {
                throw new IllegalArgumentException("As posições das etapas devem ser únicas e positivas.");
            }
            ordenadas.add(new Etapa(etapa.nome.trim(), etapa.ordem));
        }
        ordenadas.sort(Comparator.comparingInt(etapa -> etapa.ordem));
        for (int indice = 0; indice < ordenadas.size(); indice++) {
            if (ordenadas.get(indice).ordem != indice + 1) {
                throw new IllegalArgumentException("As posições devem ser sequenciais, começando em 1.");
            }
        }
        return ordenadas;
    }

    public static List<Etapa> deFirestore(Object valor) {
        if (!(valor instanceof List<?>)) {
            throw new IllegalArgumentException("A sequência de etapas no Firestore é inválida.");
        }
        List<Etapa> etapas = new ArrayList<>();
        for (Object item : (List<?>) valor) {
            if (!(item instanceof Map<?, ?>)) {
                throw new IllegalArgumentException("A sequência de etapas no Firestore é inválida.");
            }
            Map<?, ?> dados = (Map<?, ?>) item;
            Object nome = dados.get("nome");
            Object ordem = dados.get("ordem");
            if (!(nome instanceof String) || !(ordem instanceof Number)) {
                throw new IllegalArgumentException("A sequência de etapas no Firestore é inválida.");
            }
            Number numero = (Number) ordem;
            int posicao = numero.intValue();
            if (numero.doubleValue() != posicao) {
                throw new IllegalArgumentException("A posição de uma etapa no Firestore é inválida.");
            }
            etapas.add(new Etapa((String) nome, posicao));
        }
        return validar(etapas);
    }

    public static List<Map<String, Object>> paraFirestore(List<Etapa> etapas) {
        List<Map<String, Object>> dados = new ArrayList<>();
        for (Etapa etapa : validar(etapas)) {
            dados.add(Map.of("nome", etapa.nome, "ordem", etapa.ordem));
        }
        return dados;
    }

    public static String paraTexto(List<Etapa> etapas) {
        StringBuilder texto = new StringBuilder();
        for (Etapa etapa : validar(etapas)) {
            if (texto.length() > 0) {
                texto.append('\n');
            }
            texto.append(etapa.nome);
        }
        return texto.toString();
    }
}
