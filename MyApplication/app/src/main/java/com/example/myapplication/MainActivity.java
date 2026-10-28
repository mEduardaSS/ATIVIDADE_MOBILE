package com.example.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private EditText edtNome, edtAno, edtNota, edtBusca;
    private Spinner spTipo, spGenero, spFiltroTipo;
    private CheckBox chkAssistido, chkSomenteAssistidos;
    private Button btnSalvar, btnLimpar;
    private ListView listObras;
    private TextView txtQuantidade;

    private FirebaseFirestore db;
    private ListenerRegistration listener;

    private final List<Obra> obras = new ArrayList<>();
    private final List<Obra> obrasFiltradas = new ArrayList<>();
    private final List<String> linhas = new ArrayList<>();

    private ArrayAdapter<String> adapterLista;
    private String obraEmEdicaoId = null;

    private final String[] tipos = {
            "Selecione o tipo",
            "Filme",
            "Série",
            "Documentário",
            "Animação"
    };

    private final String[] generos = {
            "Selecione o gênero",
            "Ação",
            "Comédia",
            "Drama",
            "Terror",
            "Ficção Científica",
            "Romance",
            "Aventura",
            "Suspense",
            "Fantasia",
            "Animação",
            "Documentário",
            "Outro"
    };

    private final String[] filtros = {
            "Todos os tipos",
            "Filme",
            "Série",
            "Documentário",
            "Animação"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inicializarComponentes();
        configurarSpinners();

        db = FirebaseFirestore.getInstance();

        configurarEventos();
        ouvirObrasDoFirebase();
    }

    private void inicializarComponentes() {
        edtNome = findViewById(R.id.edtNome);
        edtAno = findViewById(R.id.edtAno);
        edtNota = findViewById(R.id.edtNota);
        edtBusca = findViewById(R.id.edtBusca);

        spTipo = findViewById(R.id.spTipo);
        spGenero = findViewById(R.id.spGenero);
        spFiltroTipo = findViewById(R.id.spFiltroTipo);

        chkAssistido = findViewById(R.id.chkAssistido);
        chkSomenteAssistidos =
                findViewById(R.id.chkSomenteAssistidos);

        btnSalvar = findViewById(R.id.btnSalvar);
        btnLimpar = findViewById(R.id.btnLimpar);

        listObras = findViewById(R.id.listObras);
        txtQuantidade = findViewById(R.id.txtQuantidade);

        adapterLista = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                linhas
        );

        listObras.setAdapter(adapterLista);
    }

    private void configurarSpinners() {
        ArrayAdapter<String> adapterTipos =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, tipos);
        adapterTipos.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spTipo.setAdapter(adapterTipos);

        ArrayAdapter<String> adapterGeneros =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, generos);
        adapterGeneros.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spGenero.setAdapter(adapterGeneros);

        ArrayAdapter<String> adapterFiltros =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, filtros);
        adapterFiltros.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spFiltroTipo.setAdapter(adapterFiltros);
    }

    private void configurarEventos() {
        btnSalvar.setOnClickListener(v -> salvarObra());

        btnLimpar.setOnClickListener(v -> limparFormulario());

        spFiltroTipo.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view, int position, long id) {
                        aplicarFiltros();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {
                    }
                });

        chkSomenteAssistidos.setOnCheckedChangeListener(
                (buttonView, isChecked) -> aplicarFiltros());

        edtBusca.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                aplicarFiltros();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        listObras.setOnItemClickListener((parent, view, position, id) -> {
            if (position < obrasFiltradas.size()) {
                carregarParaEdicao(obrasFiltradas.get(position));
            }
        });

        listObras.setOnItemLongClickListener(
                (parent, view, position, id) -> {
                    if (position < obrasFiltradas.size()) {
                        confirmarExclusao(obrasFiltradas.get(position));
                    }
                    return true;
                });
    }

    private void ouvirObrasDoFirebase() {
        listener = db.collection("obras")
                .addSnapshotListener((snapshot, erro) -> {
                    if (erro != null) {
                        Toast.makeText(this,
                                "Erro ao consultar Firebase: "
                                        + erro.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    obras.clear();

                    for (DocumentSnapshot documento
                            : snapshot.getDocuments()) {
                        Obra obra = documento.toObject(Obra.class);

                        if (obra != null) {
                            obra.setId(documento.getId());
                            obras.add(obra);
                        }
                    }

                    aplicarFiltros();
                });
    }

    private void salvarObra() {
        String nome = edtNome.getText().toString().trim();
        String anoTexto = edtAno.getText().toString().trim();
        String notaTexto = edtNota.getText().toString()
                .trim().replace(',', '.');

        if (nome.isEmpty()) {
            edtNome.setError("Informe o nome");
            edtNome.requestFocus();
            return;
        }

        if (spTipo.getSelectedItemPosition() == 0) {
            Toast.makeText(this,
                    "Selecione o tipo da obra", Toast.LENGTH_SHORT).show();
            return;
        }

        if (spGenero.getSelectedItemPosition() == 0) {
            Toast.makeText(this,
                    "Selecione o gênero", Toast.LENGTH_SHORT).show();
            return;
        }

        if (anoTexto.isEmpty()) {
            edtAno.setError("Informe o ano de lançamento");
            edtAno.requestFocus();
            return;
        }

        if (notaTexto.isEmpty()) {
            edtNota.setError("Informe sua nota");
            edtNota.requestFocus();
            return;
        }

        int ano;
        double nota;

        try {
            ano = Integer.parseInt(anoTexto);
            nota = Double.parseDouble(notaTexto);
        } catch (NumberFormatException e) {
            Toast.makeText(this,
                    "Informe um ano e uma nota válidos",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int anoAtual = java.util.Calendar.getInstance()
                .get(java.util.Calendar.YEAR);

        if (ano < 1888 || ano > anoAtual + 10) {
            edtAno.setError("Informe um ano válido");
            edtAno.requestFocus();
            return;
        }

        if (nota < 0 || nota > 10) {
            edtNota.setError("A nota deve estar entre 0 e 10");
            edtNota.requestFocus();
            return;
        }

        Obra obra = new Obra(
                nome,
                spTipo.getSelectedItem().toString(),
                spGenero.getSelectedItem().toString(),
                ano,
                nota,
                chkAssistido.isChecked()
        );

        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", obra.getNome());
        dados.put("tipo", obra.getTipo());
        dados.put("genero", obra.getGenero());
        dados.put("ano", obra.getAno());
        dados.put("nota", obra.getNota());
        dados.put("assistido", obra.isAssistido());

        boolean editando = obraEmEdicaoId != null;

        if (editando) {
            db.collection("obras")
                    .document(obraEmEdicaoId)
                    .update(dados)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this,
                                "Obra atualizada com sucesso!",
                                Toast.LENGTH_SHORT).show();
                        limparFormulario();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Erro ao atualizar: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
        } else {
            db.collection("obras")
                    .add(dados)
                    .addOnSuccessListener(documento -> {
                        Toast.makeText(this,
                                "Obra cadastrada com sucesso!",
                                Toast.LENGTH_SHORT).show();
                        limparFormulario();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Erro ao salvar: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
        }
    }

    private void aplicarFiltros() {
        String busca = edtBusca.getText().toString()
                .trim().toLowerCase(Locale.ROOT);

        String tipoFiltro = spFiltroTipo.getSelectedItem() == null
                ? "Todos os tipos"
                : spFiltroTipo.getSelectedItem().toString();

        boolean somenteAssistidos =
                chkSomenteAssistidos.isChecked();

        obrasFiltradas.clear();
        linhas.clear();

        for (Obra obra : obras) {
            if (obra.getNome() == null) continue;

            boolean correspondeNome = obra.getNome()
                    .toLowerCase(Locale.ROOT).contains(busca);

            boolean correspondeTipo =
                    tipoFiltro.equals("Todos os tipos")
                            || tipoFiltro.equals(obra.getTipo());

            boolean correspondeAssistido =
                    !somenteAssistidos || obra.isAssistido();

            if (correspondeNome && correspondeTipo
                    && correspondeAssistido) {
                obrasFiltradas.add(obra);

                String status = obra.isAssistido()
                        ? "Assistido" : "Não assistido";

                linhas.add(
                        obra.getNome()
                                + "\nTipo: " + obra.getTipo()
                                + " | Gênero: " + obra.getGenero()
                                + "\nAno: " + obra.getAno()
                                + " | Nota: " + obra.getNota()
                                + "\nStatus: " + status
                );
            }
        }

        adapterLista.notifyDataSetChanged();

        txtQuantidade.setText(
                "Obras encontradas: " + obrasFiltradas.size());
    }

    private void carregarParaEdicao(Obra obra) {
        obraEmEdicaoId = obra.getId();

        edtNome.setText(obra.getNome());
        edtAno.setText(String.valueOf(obra.getAno()));
        edtNota.setText(String.valueOf(obra.getNota()));

        spTipo.setSelection(buscarPosicao(tipos, obra.getTipo()));
        spGenero.setSelection(buscarPosicao(generos, obra.getGenero()));

        chkAssistido.setChecked(obra.isAssistido());

        btnSalvar.setText("Atualizar obra");

        Toast.makeText(this,
                "Edite os campos e toque em Atualizar obra",
                Toast.LENGTH_SHORT).show();
    }

    private int buscarPosicao(String[] opcoes, String valor) {
        for (int i = 0; i < opcoes.length; i++) {
            if (opcoes[i].equals(valor)) {
                return i;
            }
        }
        return 0;
    }

    private void confirmarExclusao(Obra obra) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir obra")
                .setMessage("Deseja excluir \"" + obra.getNome() + "\"?")
                .setNegativeButton("Cancelar", (dialog, which) ->
                        dialog.dismiss())
                .setPositiveButton("Excluir", (dialog, which) ->
                        db.collection("obras")
                                .document(obra.getId())
                                .delete()
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(this,
                                                "Obra excluída!",
                                                Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "Erro ao excluir: "
                                                        + e.getMessage(),
                                                Toast.LENGTH_LONG).show()))
                .show();
    }

    private void limparFormulario() {
        obraEmEdicaoId = null;

        edtNome.setText("");
        edtAno.setText("");
        edtNota.setText("");

        spTipo.setSelection(0);
        spGenero.setSelection(0);

        chkAssistido.setChecked(false);

        btnSalvar.setText("Salvar obra");
        edtNome.requestFocus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (listener != null) {
            listener.remove();
        }
    }
}
