package br.edu.unisenai.rastreabilidade;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.edu.unisenai.rastreabilidade.model.Evento;

final class HistoricoAdapter extends RecyclerView.Adapter<HistoricoAdapter.Linha> {
    private static final int ETAPA = 0xFF1B6C80;
    private static final int REPROVACAO = 0xFFB3261E;

    private final DateFormat formato = DateFormat.getDateTimeInstance(DateFormat.SHORT,
            DateFormat.SHORT, Locale.forLanguageTag("pt-BR"));
    private final List<Evento> eventos = new ArrayList<>();

    @SuppressLint("NotifyDataSetChanged")
    void mostrar(List<Evento> novos) {
        eventos.clear();
        eventos.addAll(novos);
        notifyDataSetChanged();
    }

    @Override
    public Linha onCreateViewHolder(ViewGroup pai, int tipo) {
        return new Linha(LayoutInflater.from(pai.getContext())
                .inflate(R.layout.item_evento, pai, false));
    }

    @Override
    public void onBindViewHolder(Linha linha, int posicao) {
        Evento evento = eventos.get(posicao);
        Context context = linha.itemView.getContext();
        boolean reprovacao = Evento.REPROVACAO.equals(evento.tipo);
        if (reprovacao) {
            linha.titulo.setText(R.string.evento_reprovacao);
            linha.motivo.setText(context.getString(R.string.evento_motivo, evento.motivo));
            linha.autor.setText(context.getString(R.string.evento_reprovado_por, evento.autorNome));
        } else {
            linha.titulo.setText(context.getString(R.string.evento_etapa, evento.ordem, evento.etapa));
            linha.autor.setText(context.getString(R.string.evento_registrado_por, evento.autorNome));
        }
        linha.titulo.setTextColor(reprovacao ? REPROVACAO : 0xFF131416);
        linha.marcador.setBackgroundTintList(ColorStateList.valueOf(reprovacao ? REPROVACAO : ETAPA));
        linha.motivo.setVisibility(reprovacao ? View.VISIBLE : View.GONE);
        linha.horario.setText(formato.format(new Date(evento.registradoEm)));
    }

    @Override
    public int getItemCount() {
        return eventos.size();
    }

    static final class Linha extends RecyclerView.ViewHolder {
        final View marcador;
        final TextView titulo;
        final TextView motivo;
        final TextView autor;
        final TextView horario;

        Linha(View item) {
            super(item);
            marcador = item.findViewById(R.id.marcador);
            titulo = item.findViewById(R.id.titulo);
            motivo = item.findViewById(R.id.motivo);
            autor = item.findViewById(R.id.autor);
            horario = item.findViewById(R.id.horario);
        }
    }
}
