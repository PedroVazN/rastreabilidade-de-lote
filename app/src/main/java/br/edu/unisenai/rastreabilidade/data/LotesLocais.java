package br.edu.unisenai.rastreabilidade.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import br.edu.unisenai.rastreabilidade.model.Lote;

public final class LotesLocais {
    private final SharedPreferences dados;

    public LotesLocais(Context context) {
        dados = context.getSharedPreferences("lotes", Context.MODE_PRIVATE);
    }

    public boolean salvar(Lote lote) {
        try {
            JSONObject objeto = new JSONObject();
            objeto.put("produto", lote.produto);
            objeto.put("quantidade", lote.quantidade);
            objeto.put("criadoEm", lote.criadoEm);
            return dados.edit().putString(lote.id, objeto.toString())
                    .putString("ultimo", lote.id).commit();
        } catch (JSONException e) {
            return false;
        }
    }

    public Lote ultimo() {
        String id = dados.getString("ultimo", null);
        return id == null ? null : buscar(id);
    }

    public Lote buscar(String id) {
        if ("ultimo".equals(id) || !dados.contains(id)) {
            return null;
        }
        String salvo = dados.getString(id, "{}");
        try {
            JSONObject objeto = new JSONObject(salvo);
            return new Lote(id, objeto.getString("produto"), objeto.getInt("quantidade"),
                    objeto.getLong("criadoEm"));
        } catch (JSONException e) {
            return null;
        }
    }
}
