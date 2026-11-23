package br.edu.unisenai.rastreabilidade.model;

public final class Evento {
    public static final String ETAPA = "etapa";
    public static final String REPROVACAO = "reprovacao";

    public final String tipo;
    public final String etapa;
    public final int ordem;
    public final String motivo;
    public final String autorUid;
    public final String autorNome;
    public final long registradoEm;

    private Evento(String tipo, String etapa, int ordem, String motivo, String autorUid,
                   String autorNome, long registradoEm) {
        this.tipo = tipo;
        this.etapa = etapa;
        this.ordem = ordem;
        this.motivo = motivo;
        this.autorUid = autorUid;
        this.autorNome = autorNome;
        this.registradoEm = registradoEm;
    }

    public static Evento etapa(String etapa, int ordem, String autorUid, String autorNome,
                               long registradoEm) {
        return new Evento(ETAPA, etapa, ordem, null, autorUid, autorNome, registradoEm);
    }

    public static Evento reprovacao(String motivo, String autorUid, String autorNome,
                                    long registradoEm) {
        return new Evento(REPROVACAO, null, 0, motivo, autorUid, autorNome, registradoEm);
    }
}
