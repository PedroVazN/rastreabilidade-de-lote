package br.edu.unisenai.rastreabilidade.model;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SequenciaEtapasTest {
    @Test
    public void analisaEtapasNaOrdemDasLinhas() {
        List<Etapa> etapas = SequenciaEtapas.analisar("corte\nsolda\npintura\ninspeção final");

        assertEquals("corte", etapas.get(0).nome);
        assertEquals(1, etapas.get(0).ordem);
        assertEquals("inspeção final", etapas.get(3).nome);
        assertEquals(4, etapas.get(3).ordem);
    }

    @Test(expected = IllegalArgumentException.class)
    public void recusaNomeVazio() {
        SequenciaEtapas.analisar("corte\n \nsolda");
    }

    @Test(expected = IllegalArgumentException.class)
    public void recusaPosicoesRepetidas() {
        SequenciaEtapas.validar(List.of(new Etapa("corte", 1), new Etapa("solda", 1)));
    }

    @Test
    public void copiaSequenciaOrdenadaParaArmazenamento() {
        List<Etapa> etapas = SequenciaEtapas.validar(
                List.of(new Etapa("solda", 2), new Etapa("corte", 1)));

        assertEquals("corte\nsolda", SequenciaEtapas.paraTexto(etapas));
    }
}
