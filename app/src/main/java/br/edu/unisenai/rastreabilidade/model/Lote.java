package br.edu.unisenai.rastreabilidade.model;

public final class Lote {
    public final String id;
    public final String produto;
    public final int quantidade;
    public final long criadoEm;

    public Lote(String id, String produto, int quantidade, long criadoEm) {
        this.id = id;
        this.produto = produto;
        this.quantidade = quantidade;
        this.criadoEm = criadoEm;
    }
}
