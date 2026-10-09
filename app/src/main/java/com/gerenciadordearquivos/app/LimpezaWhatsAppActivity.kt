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
            Categoria(tr("Fotos"), listOf("WhatsApp Images"), R.drawable.cat_imagens),
            Categoria(tr("Vídeos"), listOf("WhatsApp Video", "WhatsApp Video Notes"), R.drawable.cat_videos),
            Categoria(tr("Áudios e mensagens de voz"), listOf("WhatsApp Audio", "WhatsApp Voice Notes"), R.drawable.cat_audios),
            Categoria(tr("Documentos"), listOf("WhatsApp Documents"), R.drawable.cat_documentos),
            Categoria(tr("Figurinhas"), listOf("WhatsApp Stickers"), R.drawable.ic_tipo_arquivo),
            Categoria(tr("GIFs"), listOf("WhatsApp Animated Gifs"), R.drawable.ic_tipo_arquivo),
            Categoria(tr("Status salvos"), listOf(".Statuses"), R.drawable.ic_tipo_arquivo)
        )

    private var categoriaAberta: Categoria? = null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        titulo.text = tr("Limpeza do WhatsApp")

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

        status.text = tr("Procurando arquivos do WhatsApp...")

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
                    status.text = tr("Não encontramos a pasta do WhatsApp neste celular.")
                    return@runOnUiThread
                }

                mostrarCategorias()
            }
        }
    }

    private fun mostrarCategorias() {

        categoriaAberta = null

        titulo.text = tr("Limpeza do WhatsApp")

        val total = categorias.sumOf { it.tamanho }

        status.text = tr("O WhatsApp ocupa {0} no seu celular", formatarBytes(total))

        acoes.removeAllViews()
        acoes.addView(
            criarTexto(tr("Toque em uma categoria para ver e limpar os arquivos."))
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
                    textos.addView(criarTexto(tr("{0} arquivo(s)", categoria.arquivos.size), 14f))

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
            status.text = tr("Nada para limpar no WhatsApp 🎉")
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

        status.text = tr("{0} arquivo(s) • {1}", itens.size, formatarBytes(categoria.tamanho))

        val botaoLimpar =
            criarBotao(tr("Mover para a lixeira"), R.drawable.ic_acao_lixeira, COR_VERMELHO) {
                limpar(itens)
            }

        fun atualizarBotao() {
            val marcados = itens.filter { it.marcado }
            mudarTextoBotao(
                botaoLimpar,
                if (marcados.isEmpty()) tr("Marque o que quer apagar")
                else tr("Mover {0} para a lixeira ({1})", marcados.size, formatarBytes(marcados.sumOf { it.arquivo!!.length() }))
            )
        }

        val adapter = SelecaoAdapter(itens) { atualizarBotao() }

        acoes.removeAllViews()

        val linhaBotoes = LinearLayout(this)
        linhaBotoes.orientation = LinearLayout.HORIZONTAL

        linhaBotoes.addView(
            criarBotao(tr("Marcar +30 dias"), null, COR_AZUL) {
                val limite = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                itens.forEach { it.marcado = it.arquivo!!.lastModified() < limite }
                adapter.notifyDataSetChanged()
                atualizarBotao()
            },
            LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(4) }
        )

        linhaBotoes.addView(
            criarBotao(tr("Marcar todos"), null, COR_AZUL) {
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
            Toast.makeText(this, tr("Marque os arquivos que quer apagar"), Toast.LENGTH_SHORT).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(tr("Limpar {0} arquivo(s)?", marcados.size))
            .setMessage(tr("Eles vão para a lixeira do Arquivos Pro e podem ser restaurados."))
            .setNegativeButton(tr("Cancelar"), null)
            .setPositiveButton(tr("Limpar")) { _, _ ->

                status.text = tr("Limpando...")

                moverParaLixeira(marcados) { movidos, bytes ->

                    Toast.makeText(
                        this,
                        tr("Pronto! {0} liberados ({1} arquivo(s))", formatarBytes(bytes), movidos),
                        Toast.LENGTH_LONG
                    ).show()

                    Anuncios.mostrarAposLimpeza(this)

                    analisar()
                }
            }
            .show()
    }
}
