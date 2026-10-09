package com.gerenciadordearquivos.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.ThumbnailUtils
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.LruCache
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.concurrent.thread

/*
 * =============================================================
 * TELA BASE DAS FERRAMENTAS
 * Cabeçalho, texto de status, área de botões, lista e rodapé,
 * no mesmo visual da tela inicial.
 * =============================================================
 */

open class TelaBase : AppCompatActivity() {

    protected lateinit var titulo: TextView
    protected lateinit var status: TextView
    protected lateinit var acoes: LinearLayout
    protected lateinit var lista: ListView
    protected lateinit var rodape: LinearLayout

    private val executor =
        Executors.newFixedThreadPool(2)

    private val miniaturas =
        LruCache<String, Bitmap>(80)

    companion object {

        val COR_TEXTO = Color.rgb(20, 20, 20)
        val COR_TEXTO_2 = Color.rgb(85, 85, 85)
        val COR_AZUL = Color.rgb(30, 136, 229)
        val COR_VERMELHO = Color.rgb(229, 57, 53)
        val COR_VERDE = Color.rgb(46, 125, 50)
        val COR_DOURADO = Color.rgb(245, 166, 35)

        val TIPO_IMAGEM =
            setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")

        val TIPO_VIDEO =
            setOf("mp4", "mkv", "avi", "mov", "3gp", "webm", "m4v")

        val TIPO_AUDIO =
            setOf("mp3", "wav", "ogg", "m4a", "aac", "flac", "opus", "amr")
    }

    override fun attachBaseContext(
        novoContexto: android.content.Context
    ) {
        super.attachBaseContext(ModoSimples.contexto(novoContexto))
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        val raiz =
            LinearLayout(this)

        raiz.orientation =
            LinearLayout.VERTICAL

        raiz.setBackgroundColor(Color.WHITE)

        // Cabeçalho
        val topo =
            LinearLayout(this)

        topo.orientation =
            LinearLayout.HORIZONTAL

        topo.gravity =
            Gravity.CENTER_VERTICAL

        topo.setBackgroundColor(Color.rgb(36, 36, 36))

        val voltar =
            TextView(this)

        voltar.text = "‹"
        voltar.textSize = 40f
        voltar.gravity = Gravity.CENTER
        voltar.setTextColor(Color.WHITE)
        voltar.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        topo.addView(
            voltar,
            LinearLayout.LayoutParams(dp(56), dp(64))
        )

        titulo =
            TextView(this)

        titulo.textSize = 20f
        titulo.typeface = Typeface.DEFAULT_BOLD
        titulo.setTextColor(Color.WHITE)
        titulo.isSingleLine = true
        titulo.ellipsize = TextUtils.TruncateAt.END

        topo.addView(
            titulo,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        raiz.addView(
            topo,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64))
        )

        status =
            TextView(this)

        status.textSize = 15f
        status.setTextColor(Color.rgb(51, 51, 51))
        status.setBackgroundColor(Color.rgb(245, 245, 245))
        status.setPadding(dp(16), dp(10), dp(16), dp(10))

        raiz.addView(status)

        acoes =
            LinearLayout(this)

        acoes.orientation =
            LinearLayout.VERTICAL

        acoes.setPadding(dp(16), dp(4), dp(16), dp(4))

        raiz.addView(acoes)

        lista =
            ListView(this)

        lista.dividerHeight = 1

        raiz.addView(
            lista,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        rodape =
            LinearLayout(this)

        rodape.orientation =
            LinearLayout.VERTICAL

        rodape.setPadding(dp(16), dp(8), dp(16), dp(12))

        rodape.visibility = View.GONE

        raiz.addView(rodape)

        setContentView(raiz)
    }

    override fun onDestroy() {

        executor.shutdownNow()

        super.onDestroy()
    }

    // ------------------------------------------------------------
    // AJUDANTES
    // ------------------------------------------------------------

    fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density).toInt()

    fun formatarBytes(bytes: Long): String {

        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            bytes >= gb -> String.format(Locale.getDefault(), "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.getDefault(), "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.getDefault(), "%.0f KB", bytes / kb)
            else -> "$bytes B"
        }
    }

    fun formatarData(ms: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(ms))

    fun criarBotao(
        texto: String,
        iconeRes: Int?,
        cor: Int,
        acao: () -> Unit
    ): LinearLayout {

        val botao =
            LinearLayout(this)

        botao.orientation = LinearLayout.HORIZONTAL
        botao.gravity = Gravity.CENTER
        botao.isClickable = true
        botao.setPadding(dp(14), 0, dp(14), 0)

        val fundo = GradientDrawable()
        fundo.setColor(cor)
        fundo.cornerRadius = dp(24).toFloat()
        botao.background = fundo

        if (iconeRes != null) {

            val icone = ImageView(this)
            icone.setImageResource(iconeRes)
            icone.setColorFilter(Color.WHITE)

            botao.addView(icone, LinearLayout.LayoutParams(dp(20), dp(20)))
        }

        val nome = TextView(this)
        nome.text = texto
        nome.textSize = 15f
        nome.typeface = Typeface.DEFAULT_BOLD
        nome.setTextColor(Color.WHITE)
        nome.tag = "texto"

        botao.addView(
            nome,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                if (iconeRes != null) marginStart = dp(8)
            }
        )

        botao.setOnClickListener { acao() }

        return botao
    }

    fun mudarTextoBotao(botao: LinearLayout, texto: String) {
        (botao.findViewWithTag<TextView>("texto"))?.text = texto
    }

    fun adicionarBotao(
        destino: LinearLayout,
        botao: View
    ) {
        destino.addView(
            botao,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply {
                topMargin = dp(6)
                bottomMargin = dp(2)
            }
        )
    }

    fun criarTexto(
        texto: String,
        tamanho: Float = 14f,
        cor: Int = COR_TEXTO_2,
        negrito: Boolean = false
    ): TextView {

        val t = TextView(this)
        t.text = texto
        t.textSize = tamanho
        t.setTextColor(cor)
        if (negrito) t.typeface = Typeface.DEFAULT_BOLD
        t.setPadding(0, dp(4), 0, dp(4))
        return t
    }

    fun iconeDoArquivo(arquivo: File): Int {

        if (arquivo.isDirectory) return R.drawable.ic_folder

        return when (arquivo.extension.lowercase(Locale.getDefault())) {
            in TIPO_IMAGEM -> R.drawable.imagens
            in TIPO_VIDEO -> R.drawable.videos
            in TIPO_AUDIO -> R.drawable.audios
            "apk" -> R.drawable.aplicativos
            "pdf" -> R.drawable.ic_tipo_pdf
            "doc", "docx", "odt", "rtf" -> R.drawable.ic_tipo_word
            "xls", "xlsx", "ods", "csv" -> R.drawable.ic_tipo_excel
            "ppt", "pptx", "odp" -> R.drawable.ic_tipo_ppt
            "txt" -> R.drawable.ic_tipo_texto
            "zip", "rar", "7z" -> R.drawable.ic_zip
            else -> R.drawable.ic_tipo_arquivo
        }
    }

    fun abrirArquivo(arquivo: File) {

        try {

            val uri =
                FileProvider.getUriForFile(this, "$packageName.fileprovider", arquivo)

            val tipo =
                MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(arquivo.extension.lowercase(Locale.getDefault()))
                    ?: "*/*"

            startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, tipo)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )

        } catch (_: Exception) {

            Toast.makeText(this, tr("Nenhum app pode abrir este arquivo"), Toast.LENGTH_SHORT).show()
        }
    }

    // Move para a lixeira do app (pode restaurar depois)
    fun moverParaLixeira(
        arquivos: List<File>,
        aoTerminar: (movidos: Int, bytes: Long) -> Unit
    ) {

        thread {

            var movidos = 0
            var bytes = 0L

            for (arquivo in arquivos) {

                val tamanho = arquivo.length()

                val destino =
                    Armazenamento.nomeLivre(Armazenamento.pastaLixeira, arquivo.name)

                if (Armazenamento.mover(arquivo, destino)) {

                    Armazenamento.registrarNaLixeira(this, destino, arquivo)

                    movidos++
                    bytes += tamanho
                }
            }

            runOnUiThread {
                if (!isFinishing) aoTerminar(movidos, bytes)
            }
        }
    }

    // ------------------------------------------------------------
    // LISTA DE ARQUIVOS COM SELEÇÃO
    // ------------------------------------------------------------

    class ItemArquivo(
        val arquivo: File?,
        var marcado: Boolean = false,
        val rotulo: String? = null,
        val cabecalho: String? = null
    )

    inner class SelecaoAdapter(
        val itens: List<ItemArquivo>,
        private val aoMudar: () -> Unit
    ) : BaseAdapter() {

        override fun getCount() = itens.size
        override fun getItem(position: Int) = itens[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun isEnabled(position: Int) = itens[position].arquivo != null

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val item = itens[position]

            val cabecalho = item.cabecalho

            if (item.arquivo == null && cabecalho != null) {

                val t = criarTexto(cabecalho, 14f, Color.rgb(21, 101, 192), true)
                t.setBackgroundColor(Color.rgb(245, 245, 245))
                t.setPadding(dp(16), dp(10), dp(16), dp(10))
                return t
            }

            val arquivo = item.arquivo!!

            val linha = LinearLayout(this@TelaBase)
            linha.orientation = LinearLayout.HORIZONTAL
            linha.gravity = Gravity.CENTER_VERTICAL
            linha.setPadding(dp(12), dp(8), dp(8), dp(8))

            val imagem = ImageView(this@TelaBase)
            imagem.scaleType = ImageView.ScaleType.CENTER_CROP
            imagem.setImageResource(iconeDoArquivo(arquivo))
            linha.addView(imagem, LinearLayout.LayoutParams(dp(56), dp(56)))

            carregarMiniatura(arquivo, imagem)

            val textos = LinearLayout(this@TelaBase)
            textos.orientation = LinearLayout.VERTICAL

            val nome = criarTexto(arquivo.name, 16f, COR_TEXTO)
            nome.maxLines = 1
            nome.ellipsize = TextUtils.TruncateAt.MIDDLE
            nome.setPadding(0, 0, 0, 0)
            textos.addView(nome)

            val detalhe =
                listOfNotNull(
                    formatarBytes(arquivo.length()),
                    formatarData(arquivo.lastModified()),
                    item.rotulo
                ).joinToString(" • ")

            val info = criarTexto(detalhe, 13f)
            info.setPadding(0, dp(2), 0, 0)
            if (item.rotulo != null) info.setTextColor(COR_VERDE)
            textos.addView(info)

            linha.addView(
                textos,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(12)
                }
            )

            val caixa = CheckBox(this@TelaBase)
            caixa.isChecked = item.marcado
            caixa.setOnClickListener {
                item.marcado = caixa.isChecked
                aoMudar()
            }
            linha.addView(caixa)

            return linha
        }
    }

    private fun carregarMiniatura(arquivo: File, imagem: ImageView) {

        val extensao = arquivo.extension.lowercase(Locale.getDefault())

        if (extensao !in TIPO_IMAGEM && extensao !in TIPO_VIDEO) return

        val chave = arquivo.absolutePath

        imagem.tag = chave

        miniaturas.get(chave)?.let {
            imagem.setImageBitmap(it)
            return
        }

        executor.execute {

            val bitmap =
                try {
                    if (extensao in TIPO_VIDEO) {
                        @Suppress("DEPRECATION")
                        ThumbnailUtils.createVideoThumbnail(
                            arquivo.absolutePath,
                            MediaStore.Images.Thumbnails.MINI_KIND
                        )
                    } else {
                        val limites = BitmapFactory.Options()
                        limites.inJustDecodeBounds = true
                        BitmapFactory.decodeFile(arquivo.absolutePath, limites)

                        var amostra = 1
                        while (
                            limites.outWidth / (amostra * 2) >= 160 &&
                            limites.outHeight / (amostra * 2) >= 160
                        ) amostra *= 2

                        val opcoes = BitmapFactory.Options()
                        opcoes.inSampleSize = amostra
                        BitmapFactory.decodeFile(arquivo.absolutePath, opcoes)
                    }
                } catch (_: Throwable) {
                    null
                }

            if (bitmap != null) {

                miniaturas.put(chave, bitmap)

                runOnUiThread {
                    if (imagem.tag == chave) imagem.setImageBitmap(bitmap)
                }
            }
        }
    }
}
