package com.gerenciadordearquivos.app

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import java.io.File
import kotlin.concurrent.thread

/*
 * =============================================================
 * LIMPEZA DO WHATSAPP
 * Mostra quanto ocupam fotos, vídeos, áudios, figurinhas etc.
 * recebidos e enviados pelo WhatsApp e permite limpar.
 * =============================================================
 */

class LimpezaWhatsAppActivity : TelaBase() {

    private class Categoria(
        val nome: String,
        val pastas: List<String>,
        val icone: Int,
        val arquivos: MutableList<File> = mutableListOf()
    ) {
        val tamanho: Long
            get() = arquivos.sumOf { it.length() }
    }

    private val categorias =
        listOf(
            Categoria("Fotos", listOf("WhatsApp Images"), R.drawable.imagens),
            Categoria("Vídeos", listOf("WhatsApp Video", "WhatsApp Video Notes"), R.drawable.videos),
            Categoria("Áudios e mensagens de voz", listOf("WhatsApp Audio", "WhatsApp Voice Notes"), R.drawable.audios),
            Categoria("Documentos", listOf("WhatsApp Documents"), R.drawable.documentos),
            Categoria("Figurinhas", listOf("WhatsApp Stickers"), R.drawable.ic_tipo_arquivo),
            Categoria("GIFs", listOf("WhatsApp Animated Gifs"), R.drawable.ic_tipo_arquivo),
            Categoria("Status salvos", listOf(".Statuses"), R.drawable.ic_tipo_arquivo)
        )

    private var categoriaAberta: Categoria? = null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        titulo.text = "Limpeza do WhatsApp"

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (categoriaAberta != null) {
                        mostrarCategorias()
                    } else {
                        finish()
                    }
                }
            }
        )

        analisar()
    }

    // Pastas de mídia do WhatsApp e do WhatsApp Business
    // (local novo no Android 11+ e local antigo)
    private fun pastasDeMidia(): List<File> {

        val raiz = Armazenamento.raizCelular

        return listOf(
            "Android/media/com.whatsapp/WhatsApp/Media",
            "Android/media/com.whatsapp.w4b/WhatsApp Business/Media",
            "WhatsApp/Media",
            "WhatsApp Business/Media"
        )
            .map { File(raiz, it) }
            .filter { it.isDirectory }
    }

    private fun analisar() {

        status.text = "Procurando arquivos do WhatsApp..."

        acoes.removeAllViews()
        rodape.visibility = View.GONE
        lista.adapter = null

        thread {

            categorias.forEach { it.arquivos.clear() }

            val midias = pastasDeMidia()

            for (midia in midias) {
                for (categoria in categorias) {
                    for (nomePasta in categoria.pastas) {
                        File(midia, nomePasta)
                            .walkTopDown()
                            .filter { it.isFile && it.name != ".nomedia" }
                            .forEach { categoria.arquivos.add(it) }
                    }
                }
            }

            runOnUiThread {

                if (isFinishing) return@runOnUiThread

                if (midias.isEmpty()) {
                    status.text = "Não encontramos a pasta do WhatsApp neste celular."
                    return@runOnUiThread
                }

                mostrarCategorias()
            }
        }
    }

    private fun mostrarCategorias() {

        categoriaAberta = null

        titulo.text = "Limpeza do WhatsApp"

        val total = categorias.sumOf { it.tamanho }

        status.text = "O WhatsApp ocupa ${formatarBytes(total)} no seu celular"

        acoes.removeAllViews()
        acoes.addView(
            criarTexto("Toque em uma categoria para ver e limpar os arquivos.")
        )

        rodape.visibility = View.GONE

        val visiveis =
            categorias
                .filter { it.arquivos.isNotEmpty() }
                .sortedByDescending { it.tamanho }

        lista.adapter =
            object : BaseAdapter() {
                override fun getCount() = visiveis.size
                override fun getItem(position: Int) = visiveis[position]
                override fun getItemId(position: Int) = position.toLong()

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

                    val categoria = visiveis[position]

                    val linha = LinearLayout(this@LimpezaWhatsAppActivity)
                    linha.orientation = LinearLayout.HORIZONTAL
                    linha.gravity = Gravity.CENTER_VERTICAL
                    linha.setPadding(dp(16), dp(12), dp(16), dp(12))

                    val icone = ImageView(this@LimpezaWhatsAppActivity)
                    icone.setImageResource(categoria.icone)
                    linha.addView(icone, LinearLayout.LayoutParams(dp(44), dp(44)))

                    val textos = LinearLayout(this@LimpezaWhatsAppActivity)
                    textos.orientation = LinearLayout.VERTICAL
                    textos.addView(criarTexto(categoria.nome, 17f, COR_TEXTO, true))
                    textos.addView(criarTexto("${categoria.arquivos.size} arquivo(s)", 14f))

                    linha.addView(
                        textos,
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            marginStart = dp(14)
                        }
                    )

                    linha.addView(criarTexto(formatarBytes(categoria.tamanho), 16f, COR_VERMELHO, true))

                    return linha
                }
            }

        lista.setOnItemClickListener { _, _, position, _ ->
            visiveis.getOrNull(position)?.let { abrirCategoria(it) }
        }

        if (visiveis.isEmpty()) {
            status.text = "Nada para limpar no WhatsApp 🎉"
        }
    }

    private fun abrirCategoria(
        categoria: Categoria
    ) {

        categoriaAberta = categoria

        titulo.text = categoria.nome

        val itens =
            categoria.arquivos
                .filter { it.exists() }
                .sortedByDescending { it.length() }
                .map { ItemArquivo(it) }

        status.text = "${itens.size} arquivo(s) • ${formatarBytes(categoria.tamanho)}"

        val botaoLimpar =
            criarBotao("Mover para a lixeira", R.drawable.ic_acao_lixeira, COR_VERMELHO) {
                limpar(itens)
            }

        fun atualizarBotao() {
            val marcados = itens.filter { it.marcado }
            mudarTextoBotao(
                botaoLimpar,
                if (marcados.isEmpty()) "Marque o que quer apagar"
                else "Mover ${marcados.size} para a lixeira (${formatarBytes(marcados.sumOf { it.arquivo!!.length() })})"
            )
        }

        val adapter = SelecaoAdapter(itens) { atualizarBotao() }

        acoes.removeAllViews()

        val linhaBotoes = LinearLayout(this)
        linhaBotoes.orientation = LinearLayout.HORIZONTAL

        linhaBotoes.addView(
            criarBotao("Marcar +30 dias", null, COR_AZUL) {
                val limite = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                itens.forEach { it.marcado = it.arquivo!!.lastModified() < limite }
                adapter.notifyDataSetChanged()
                atualizarBotao()
            },
            LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(4) }
        )

        linhaBotoes.addView(
            criarBotao("Marcar todos", null, COR_AZUL) {
                val marcar = itens.any { !it.marcado }
                itens.forEach { it.marcado = marcar }
                adapter.notifyDataSetChanged()
                atualizarBotao()
            },
            LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(4) }
        )

        acoes.addView(
            linhaBotoes,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(6)
                bottomMargin = dp(6)
            }
        )

        lista.adapter = adapter

        lista.setOnItemClickListener { _, _, position, _ ->
            itens.getOrNull(position)?.arquivo?.let { abrirArquivo(it) }
        }

        rodape.removeAllViews()
        adicionarBotao(rodape, botaoLimpar)
        rodape.visibility = View.VISIBLE

        atualizarBotao()
    }

    private fun limpar(
        itens: List<ItemArquivo>
    ) {

        val marcados =
            itens.filter { it.marcado }.mapNotNull { it.arquivo }

        if (marcados.isEmpty()) {
            Toast.makeText(this, "Marque os arquivos que quer apagar", Toast.LENGTH_SHORT).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Limpar ${marcados.size} arquivo(s)?")
            .setMessage("Eles vão para a lixeira do Faxina e podem ser restaurados.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Limpar") { _, _ ->

                status.text = "Limpando..."

                moverParaLixeira(marcados) { movidos, bytes ->

                    Toast.makeText(
                        this,
                        "Pronto! ${formatarBytes(bytes)} liberados ($movidos arquivo(s))",
                        Toast.LENGTH_LONG
                    ).show()

                    Anuncios.mostrarAposLimpeza(this)

                    analisar()
                }
            }
            .show()
    }
}
