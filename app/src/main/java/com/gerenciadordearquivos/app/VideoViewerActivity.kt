package com.gerenciadordearquivos.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.core.content.FileProvider
import java.io.File
import java.text.DecimalFormat
import java.util.Locale

class VideoViewerActivity : Activity() {

    private lateinit var videoView: GestureVideoView

    private lateinit var nomePastaText: TextView
    private lateinit var nomeArquivoText: TextView
    private lateinit var contadorText: TextView

    private lateinit var raiz: FrameLayout
    private lateinit var barraSuperior: LinearLayout
    private lateinit var barraInferior: LinearLayout

    private val arquivos = ArrayList<String>()

    private var posicaoAtual = 0

    private lateinit var arquivoAtual: File

    private var videoPreparado = false
    private var videoEstavaReproduzindoAntesDaPausa = false

    private var popupAtual: PopupWindow? = null

    private val pastaLixeira =
        File(
            Environment.getExternalStorageDirectory(),
            ".GerenciadorArquivos/.Lixeira"
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        configurarTelaCheia()

        carregarDadosIntent()

        criarInterface()

        carregarVideo()

        configurarGestos()
    }

    // =========================================================
    // CICLO DE VIDA
    // =========================================================

    override fun onResume() {
        super.onResume()

        configurarTelaCheia()

        if (
            videoPreparado &&
            videoEstavaReproduzindoAntesDaPausa
        ) {
            videoView.start()
        }
    }

    override fun onPause() {

        if (::videoView.isInitialized) {

            videoEstavaReproduzindoAntesDaPausa =
                videoView.isPlaying

            if (videoView.isPlaying) {
                videoView.pause()
            }
        }

        super.onPause()
    }

    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            configurarTelaCheia()
        }
    }

    override fun onConfigurationChanged(
        newConfig: android.content.res.Configuration
    ) {
        super.onConfigurationChanged(newConfig)

        configurarTelaCheia()

        if (::videoView.isInitialized) {
            videoView.requestLayout()
            videoView.invalidate()
        }

        barraSuperior.post {
            ajustarTextos()
        }
    }

    // =========================================================
    // TELA CHEIA
    // =========================================================

    private fun configurarTelaCheia() {

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    // =========================================================
    // DADOS RECEBIDOS
    // =========================================================

    private fun carregarDadosIntent() {

        val listaRecebida =
            intent.getStringArrayListExtra("arquivos")

        if (
            listaRecebida != null &&
            listaRecebida.isNotEmpty()
        ) {
            arquivos.clear()
            arquivos.addAll(listaRecebida)
        }

        val caminhoRecebido =
            intent.getStringExtra("arquivo")

        val posicaoRecebida =
            intent.getIntExtra("posicao", -1)

        if (
            arquivos.isEmpty() &&
            !caminhoRecebido.isNullOrBlank()
        ) {
            arquivos.add(caminhoRecebido)
        }

        if (arquivos.isEmpty()) {
            Toast.makeText(
                this,
                "Nenhum vídeo encontrado",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        posicaoAtual =
            when {
                posicaoRecebida in arquivos.indices ->
                    posicaoRecebida

                !caminhoRecebido.isNullOrBlank() -> {
                    val indice =
                        arquivos.indexOf(
                            caminhoRecebido
                        )

                    if (indice >= 0) {
                        indice
                    } else {
                        0
                    }
                }

                else -> 0
            }

        atualizarArquivoAtual()
    }

    private fun atualizarArquivoAtual() {

        if (arquivos.isEmpty()) {
            finish()
            return
        }

        if (
            posicaoAtual < 0 ||
            posicaoAtual >= arquivos.size
        ) {
            posicaoAtual = 0
        }

        arquivoAtual =
            File(
                arquivos[posicaoAtual]
            )

        atualizarCabecalho()
    }

    // =========================================================
    // INTERFACE
    // =========================================================

    private fun criarInterface() {

        raiz =
            FrameLayout(this).apply {
                setBackgroundColor(Color.BLACK)
            }

        videoView =
            GestureVideoView(this).apply {
                layoutParams =
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                setBackgroundColor(Color.BLACK)
            }

        raiz.addView(videoView)

        criarBarraSuperior()

        criarBarraInferior()

        setContentView(raiz)
    }

    // =========================================================
    // BARRA SUPERIOR
    // =========================================================

    private fun criarBarraSuperior() {

        barraSuperior =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(14),
                    dp(7),
                    dp(14),
                    dp(7)
                )

                setBackgroundColor(
                    Color.argb(
                        205,
                        0,
                        0,
                        0
                    )
                )

                layoutParams =
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(82),
                        Gravity.TOP
                    )
            }

        val primeiraLinha =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
            }

        val voltar =
            TextView(this).apply {

                text = "‹"

                textSize = 38f

                setTextColor(Color.WHITE)

                gravity =
                    Gravity.CENTER

                setPadding(
                    0,
                    0,
                    dp(12),
                    dp(4)
                )

                setOnClickListener {
                    finish()
                }
            }

        primeiraLinha.addView(
            voltar,
            LinearLayout.LayoutParams(
                dp(45),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val colunaTitulo =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_VERTICAL

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1f
                    )
            }

        nomePastaText =
            TextView(this).apply {

                textSize = 13f

                setTextColor(
                    Color.LTGRAY
                )

                gravity =
                    Gravity.CENTER_VERTICAL

                configurarTextoRolante(this)
            }

        nomeArquivoText =
            TextView(this).apply {

                textSize = 15f

                setTextColor(Color.WHITE)

                gravity =
                    Gravity.CENTER_VERTICAL

                configurarTextoRolante(this)
            }

        colunaTitulo.addView(
            nomePastaText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        colunaTitulo.addView(
            nomeArquivoText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        primeiraLinha.addView(
            colunaTitulo
        )

        contadorText =
            TextView(this).apply {

                textSize = 14f

                setTextColor(Color.WHITE)

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(8),
                    0,
                    dp(8),
                    0
                )
            }

        primeiraLinha.addView(
            contadorText,
            LinearLayout.LayoutParams(
                dp(65),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val menu =
            TextView(this).apply {

                text = "⋮"

                textSize = 30f

                setTextColor(Color.WHITE)

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(5),
                    0,
                    0,
                    0
                )

                setOnClickListener {
                    mostrarMenu()
                }
            }

        primeiraLinha.addView(
            menu,
            LinearLayout.LayoutParams(
                dp(40),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        barraSuperior.addView(
            primeiraLinha
        )

        raiz.addView(
            barraSuperior
        )

        atualizarCabecalho()
    }

    private fun atualizarCabecalho() {

        if (!::arquivoAtual.isInitialized) {
            return
        }

        val pasta =
            arquivoAtual.parentFile
                ?.name
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Armazenamento"

        nomePastaText.text =
            "📁 $pasta"

        nomeArquivoText.text =
            arquivoAtual.name

        contadorText.text =
            "${posicaoAtual + 1} / ${arquivos.size}"

        ajustarTextos()
    }

    private fun ajustarTextos() {

        if (::nomePastaText.isInitialized) {
            configurarTextoRolante(
                nomePastaText
            )
        }

        if (::nomeArquivoText.isInitialized) {
            configurarTextoRolante(
                nomeArquivoText
            )
        }
    }

    private fun configurarTextoRolante(
        texto: TextView
    ) {

        texto.setSingleLine(true)

        texto.maxLines = 1

        texto.ellipsize =
            TextUtils.TruncateAt.MARQUEE

        texto.marqueeRepeatLimit = -1

        texto.isSelected = true

        texto.isFocusable = true

        texto.isFocusableInTouchMode = true

        texto.setHorizontallyScrolling(true)
    }

    // =========================================================
    // BARRA INFERIOR
    // =========================================================

    private fun criarBarraInferior() {

        barraInferior =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(8),
                    dp(5),
                    dp(8),
                    dp(5)
                )

                setBackgroundColor(
                    Color.argb(
                        220,
                        0,
                        0,
                        0
                    )
                )

                layoutParams =
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(62),
                        Gravity.BOTTOM
                    )
            }

        adicionarBotaoInferior(
            "↗",
            "Compartilhar"
        ) {
            compartilharArquivo()
        }

        adicionarBotaoInferior(
            "♡",
            "Favorito"
        ) {
            alternarFavorito()
        }

        adicionarBotaoInferior(
            "⧉",
            "Copiar"
        ) {
            copiarArquivo()
        }

        adicionarBotaoInferior(
            "⇄",
            "Mover"
        ) {
            moverArquivo()
        }

        adicionarBotaoInferior(
            "⋮",
            "Mais"
        ) {
            mostrarMenu()
        }

        raiz.addView(
            barraInferior
        )
    }

    private fun adicionarBotaoInferior(
        icone: String,
        descricao: String,
        acao: () -> Unit
    ) {

        val coluna =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                setOnClickListener {
                    acao()
                }
            }

        val textoIcone =
            TextView(this).apply {

                text = icone

                textSize = 22f

                setTextColor(Color.WHITE)

                gravity =
                    Gravity.CENTER
            }

        val textoDescricao =
            TextView(this).apply {

                text = descricao

                textSize = 9f

                setTextColor(
                    Color.LTGRAY
                )

                gravity =
                    Gravity.CENTER
            }

        coluna.addView(
            textoIcone,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(30)
            )
        )

        coluna.addView(
            textoDescricao,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(20)
            )
        )

        barraInferior.addView(
            coluna,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        )
    }

    // =========================================================
    // VÍDEO
    // =========================================================

    private fun carregarVideo() {

        if (!arquivoAtual.exists()) {

            Toast.makeText(
                this,
                "Vídeo não encontrado",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        videoPreparado = false

        val controlador =
            MediaController(this)

        controlador.setAnchorView(
            videoView
        )

        videoView.setMediaController(
            controlador
        )

        videoView.setVideoPath(
            arquivoAtual.absolutePath
        )

        videoView.setOnPreparedListener {

            videoPreparado = true

            it.isLooping = false

            videoView.start()

            configurarTelaCheia()
        }

        videoView.setOnCompletionListener {

            controlador.show()

        }

        videoView.setOnErrorListener { _, _, _ ->

            Toast.makeText(
                this,
                "Não foi possível reproduzir este vídeo",
                Toast.LENGTH_LONG
            ).show()

            true
        }
    }

    // =========================================================
    // GESTOS
    // =========================================================

    private fun configurarGestos() {

        videoView.onSwipeLeft = {

            abrirProximoVideo()
        }

        videoView.onSwipeRight = {

            abrirVideoAnterior()
        }
    }

    private fun abrirProximoVideo() {

        if (arquivos.size <= 1) {
            return
        }

        if (
            posicaoAtual >=
            arquivos.size - 1
        ) {

            Toast.makeText(
                this,
                "Este é o último vídeo",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        posicaoAtual++

        trocarVideo()
    }

    private fun abrirVideoAnterior() {

        if (arquivos.size <= 1) {
            return
        }

        if (posicaoAtual <= 0) {

            Toast.makeText(
                this,
                "Este é o primeiro vídeo",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        posicaoAtual--

        trocarVideo()
    }

    private fun trocarVideo() {

        atualizarArquivoAtual()

        videoEstavaReproduzindoAntesDaPausa =
            true

        carregarVideo()

        atualizarCabecalho()
    }

    // =========================================================
    // MENU
    // =========================================================

    private fun mostrarMenu() {

        popupAtual?.dismiss()

        val layout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(8),
                    dp(8),
                    dp(8),
                    dp(8)
                )

                setBackgroundColor(
                    Color.rgb(
                        35,
                        35,
                        35
                    )
                )
            }

        adicionarItemMenu(
            layout,
            "↗  Compartilhar"
        ) {
            compartilharArquivo()
        }

        adicionarItemMenu(
            layout,
            "♡  Favorito"
        ) {
            alternarFavorito()
        }

        adicionarItemMenu(
            layout,
            "▣  Abrir com..."
        ) {
            abrirComAplicativo()
        }

        adicionarItemMenu(
            layout,
            "ⓘ  Informações"
        ) {
            mostrarInformacoes()
        }

        adicionarItemMenu(
            layout,
            "✎  Renomear"
        ) {
            renomearArquivo()
        }

        adicionarItemMenu(
            layout,
            "⧉  Copiar"
        ) {
            copiarArquivo()
        }

        adicionarItemMenu(
            layout,
            "⇄  Mover"
        ) {
            moverArquivo()
        }

        adicionarItemMenu(
            layout,
            "＋  Criar pasta"
        ) {
            criarNovaPasta()
        }

        adicionarItemMenu(
            layout,
            "🗑  Enviar para lixeira"
        ) {
            enviarParaLixeira()
        }

        popupAtual =
            PopupWindow(
                layout,
                dp(240),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            ).apply {

                setBackgroundDrawable(
                    android.graphics.drawable.ColorDrawable(
                        Color.rgb(
                            35,
                            35,
                            35
                        )
                    )
                )

                elevation = dp(8).toFloat()

                isOutsideTouchable = true

                showAtLocation(
                    raiz,
                    Gravity.TOP or Gravity.END,
                    dp(10),
                    dp(65)
                )
            }
    }

    private fun adicionarItemMenu(
        layout: LinearLayout,
        texto: String,
        acao: () -> Unit
    ) {

        val item =
            TextView(this).apply {

                text = texto

                textSize = 15f

                setTextColor(Color.WHITE)

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(15),
                    dp(13),
                    dp(15),
                    dp(13)
                )

                setOnClickListener {

                    popupAtual?.dismiss()

                    acao()
                }
            }

        layout.addView(
            item,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
            )
        )
    }

    // =========================================================
    // COMPARTILHAR
    // =========================================================

    private fun compartilharArquivo() {

        try {

            val uri =
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    arquivoAtual
                )

            val intent =
                Intent(
                    Intent.ACTION_SEND
                ).apply {

                    type =
                        obterMimeType(
                            arquivoAtual
                        )

                    putExtra(
                        Intent.EXTRA_STREAM,
                        uri
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

            startActivity(
                Intent.createChooser(
                    intent,
                    "Compartilhar vídeo"
                )
            )

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Não foi possível compartilhar",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================================================
    // ABRIR COM
    // =========================================================

    private fun abrirComAplicativo() {

        try {

            val uri =
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    arquivoAtual
                )

            val intent =
                Intent(
                    Intent.ACTION_VIEW
                ).apply {

                    setDataAndType(
                        uri,
                        obterMimeType(
                            arquivoAtual
                        )
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

            startActivity(intent)

        } catch (
            e: ActivityNotFoundException
        ) {

            Toast.makeText(
                this,
                "Nenhum aplicativo pode abrir este vídeo",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Não foi possível abrir o vídeo",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================================================
    // FAVORITO
    // =========================================================

    private fun alternarFavorito() {

        val prefs =
            getSharedPreferences(
                "favoritos",
                Context.MODE_PRIVATE
            )

        val chave =
            arquivoAtual.absolutePath

        val atual =
            prefs.getBoolean(
                chave,
                false
            )

        prefs.edit()
            .putBoolean(
                chave,
                !atual
            )
            .apply()

        Toast.makeText(
            this,
            if (!atual)
                "Adicionado aos favoritos"
            else
                "Removido dos favoritos",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // INFORMAÇÕES
    // =========================================================

    private fun mostrarInformacoes() {

        var largura = 0
        var altura = 0
        var duracao = 0L

        val retriever =
            MediaMetadataRetriever()

        try {

            retriever.setDataSource(
                arquivoAtual.absolutePath
            )

            largura =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                )?.toIntOrNull()
                    ?: 0

            altura =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                )?.toIntOrNull()
                    ?: 0

            duracao =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull()
                    ?: 0L

        } catch (_: Exception) {

        } finally {

            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        val tamanho =
            formatarTamanho(
                arquivoAtual.length()
            )

        val duracaoTexto =
            formatarDuracao(
                duracao
            )

        val resolucao =
            if (
                largura > 0 &&
                altura > 0
            ) {
                "${largura} × ${altura}"
            } else {
                "Desconhecida"
            }

        val mensagem =
            """
            Nome: ${arquivoAtual.name}
            
            Pasta: ${arquivoAtual.parentFile?.name ?: "Armazenamento"}
            
            Tamanho: $tamanho
            
            Resolução: $resolucao
            
            Duração: $duracaoTexto
            
            Caminho:
            ${arquivoAtual.absolutePath}
            """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Informações do vídeo")
            .setMessage(mensagem)
            .setPositiveButton("OK", null)
            .show()
    }

    // =========================================================
    // RENOMEAR
    // =========================================================

    private fun renomearArquivo() {

        val campo =
            EditText(this).apply {

                setSingleLine(true)

                setText(
                    arquivoAtual.name
                )

                setSelection(
                    text.length
                )

                hint = "Nome do arquivo"
            }

        val container =
            LinearLayout(this).apply {

                setPadding(
                    dp(20),
                    0,
                    dp(20),
                    0
                )

                addView(
                    campo,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            }

        AlertDialog.Builder(this)
            .setTitle("Renomear vídeo")
            .setView(container)
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Renomear"
            ) { _, _ ->

                val novoNome =
                    campo.text
                        .toString()
                        .trim()

                if (novoNome.isBlank()) {
                    return@setPositiveButton
                }

                val novoArquivo =
                    File(
                        arquivoAtual.parentFile,
                        novoNome
                    )

                if (
                    novoArquivo.exists() &&
                    novoArquivo.absolutePath !=
                    arquivoAtual.absolutePath
                ) {

                    Toast.makeText(
                        this,
                        "Já existe um arquivo com esse nome",
                        Toast.LENGTH_LONG
                    ).show()

                    return@setPositiveButton
                }

                try {

                    if (
                        arquivoAtual.renameTo(
                            novoArquivo
                        )
                    ) {

                        arquivos[posicaoAtual] =
                            novoArquivo.absolutePath

                        arquivoAtual =
                            novoArquivo

                        atualizarCabecalho()

                        Toast.makeText(
                            this,
                            "Arquivo renomeado",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        Toast.makeText(
                            this,
                            "Não foi possível renomear",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } catch (e: Exception) {

                    Toast.makeText(
                        this,
                        "Erro ao renomear: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .show()
    }

    // =========================================================
    // COPIAR
    // =========================================================

    private fun copiarArquivo() {

        Toast.makeText(
            this,
            "Escolha a pasta de destino",
            Toast.LENGTH_SHORT
        ).show()

        mostrarEscolhaPasta(
            "Copiar para"
        ) { pastaDestino ->

            try {

                val destino =
                    File(
                        pastaDestino,
                        arquivoAtual.name
                    )

                if (destino.exists()) {

                    Toast.makeText(
                        this,
                        "Já existe um arquivo com esse nome",
                        Toast.LENGTH_LONG
                    ).show()

                    return@mostrarEscolhaPasta
                }

                arquivoAtual.inputStream()
                    .use { entrada ->

                        destino.outputStream()
                            .use { saida ->

                            entrada.copyTo(
                                saida
                            )
                        }
                    }

                Toast.makeText(
                    this,
                    "Arquivo copiado",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Erro ao copiar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =========================================================
    // MOVER
    // =========================================================

    private fun moverArquivo() {

        Toast.makeText(
            this,
            "Escolha a pasta de destino",
            Toast.LENGTH_SHORT
        ).show()

        mostrarEscolhaPasta(
            "Mover para"
        ) { pastaDestino ->

            try {

                val destino =
                    File(
                        pastaDestino,
                        arquivoAtual.name
                    )

                if (destino.exists()) {

                    Toast.makeText(
                        this,
                        "Já existe um arquivo com esse nome",
                        Toast.LENGTH_LONG
                    ).show()

                    return@mostrarEscolhaPasta
                }

                if (
                    arquivoAtual.renameTo(
                        destino
                    )
                ) {

                    arquivos.removeAt(
                        posicaoAtual
                    )

                    if (arquivos.isEmpty()) {

                        Toast.makeText(
                            this,
                            "Vídeo movido",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()
                        return@mostrarEscolhaPasta
                    }

                    if (
                        posicaoAtual >=
                        arquivos.size
                    ) {
                        posicaoAtual =
                            arquivos.size - 1
                    }

                    atualizarArquivoAtual()

                    carregarVideo()

                    Toast.makeText(
                        this,
                        "Arquivo movido",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this,
                        "Não foi possível mover o arquivo",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Erro ao mover: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =========================================================
    // CRIAR PASTA
    // =========================================================

    private fun criarNovaPasta() {

        val campo =
            EditText(this).apply {

                setSingleLine(true)

                hint = "Nome da pasta"
            }

        val container =
            LinearLayout(this).apply {

                setPadding(
                    dp(20),
                    0,
                    dp(20),
                    0
                )

                addView(
                    campo,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            }

        AlertDialog.Builder(this)
            .setTitle("Nova pasta")
            .setView(container)
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Criar"
            ) { _, _ ->

                val nome =
                    campo.text
                        .toString()
                        .trim()

                if (nome.isBlank()) {
                    return@setPositiveButton
                }

                try {

                    val novaPasta =
                        File(
                            arquivoAtual.parentFile,
                            nome
                        )

                    if (
                        novaPasta.mkdirs()
                    ) {

                        Toast.makeText(
                            this,
                            "Pasta criada",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        Toast.makeText(
                            this,
                            "Não foi possível criar a pasta",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } catch (e: Exception) {

                    Toast.makeText(
                        this,
                        "Erro: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .show()
    }

    // =========================================================
    // LIXEIRA
    // =========================================================

    private fun enviarParaLixeira() {

        AlertDialog.Builder(this)
            .setTitle("Enviar para lixeira?")
            .setMessage(
                "O vídeo será movido para a lixeira."
            )
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Enviar"
            ) { _, _ ->

                try {

                    if (!pastaLixeira.exists()) {
                        pastaLixeira.mkdirs()
                    }

                    var destino =
                        File(
                            pastaLixeira,
                            arquivoAtual.name
                        )

                    var contador = 1

                    while (destino.exists()) {

                        val nome =
                            arquivoAtual.nameWithoutExtension

                        val extensao =
                            arquivoAtual.extension

                        destino =
                            File(
                                pastaLixeira,
                                "${nome}_$contador.$extensao"
                            )

                        contador++
                    }

                    if (
                        arquivoAtual.renameTo(
                            destino
                        )
                    ) {

                        Toast.makeText(
                            this,
                            "Vídeo enviado para a lixeira",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()

                    } else {

                        Toast.makeText(
                            this,
                            "Não foi possível enviar para a lixeira",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } catch (e: Exception) {

                    Toast.makeText(
                        this,
                        "Erro: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .show()
    }

    // =========================================================
    // SELEÇÃO DE PASTA
    // =========================================================

    private fun mostrarEscolhaPasta(
        titulo: String,
        aoSelecionar: (File) -> Unit
    ) {

        val pastaInicial =
            arquivoAtual.parentFile
                ?: Environment
                    .getExternalStorageDirectory()

        val pastas =
            ArrayList<File>()

        coletarPastas(
            pastaInicial,
            pastas,
            0
        )

        if (pastas.isEmpty()) {

            Toast.makeText(
                this,
                "Nenhuma pasta encontrada",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val nomes =
            pastas.map {
                it.absolutePath
            }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setItems(nomes) { _, indice ->

                if (
                    indice >= 0 &&
                    indice < pastas.size
                ) {

                    aoSelecionar(
                        pastas[indice]
                    )
                }
            }
            .setNegativeButton(
                "Cancelar",
                null
            )
            .show()
    }

    private fun coletarPastas(
        pasta: File,
        resultado: ArrayList<File>,
        nivel: Int
    ) {

        if (
            nivel > 2 ||
            !pasta.exists() ||
            !pasta.isDirectory
        ) {
            return
        }

        resultado.add(pasta)

        try {

            val filhos =
                pasta.listFiles()

            filhos
                ?.filter {
                    it.isDirectory &&
                    !it.name.startsWith(".")
                }
                ?.sortedBy {
                    it.name.lowercase()
                }
                ?.forEach {

                    coletarPastas(
                        it,
                        resultado,
                        nivel + 1
                    )
                }

        } catch (_: Exception) {
        }
    }

    // =========================================================
    // UTILITÁRIOS
    // =========================================================

    private fun obterMimeType(
        arquivo: File
    ): String {

        return when (
            arquivo.extension.lowercase(
                Locale.getDefault()
            )
        ) {

            "mp4" ->
                "video/mp4"

            "mkv" ->
                "video/x-matroska"

            "avi" ->
                "video/x-msvideo"

            "mov" ->
                "video/quicktime"

            "webm" ->
                "video/webm"

            "3gp" ->
                "video/3gpp"

            "mpeg",
            "mpg" ->
                "video/mpeg"

            "m4v" ->
                "video/x-m4v"

            else ->
                "video/*"
        }
    }

    private fun formatarTamanho(
        bytes: Long
    ): String {

        if (bytes <= 0) {
            return "0 B"
        }

        val unidades =
            arrayOf(
                "B",
                "KB",
                "MB",
                "GB",
                "TB"
            )

        var tamanho =
            bytes.toDouble()

        var indice = 0

        while (
            tamanho >= 1024 &&
            indice < unidades.size - 1
        ) {

            tamanho /= 1024

            indice++
        }

        return DecimalFormat(
            "#,##0.##"
        ).format(tamanho) +
                " " +
                unidades[indice]
    }

    private fun formatarDuracao(
        milissegundos: Long
    ): String {

        if (milissegundos <= 0) {
            return "Desconhecida"
        }

        val totalSegundos =
            milissegundos / 1000

        val horas =
            totalSegundos / 3600

        val minutos =
            (totalSegundos % 3600) / 60

        val segundos =
            totalSegundos % 60

        return if (horas > 0) {

            String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d",
                horas,
                minutos,
                segundos
            )

        } else {

            String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutos,
                segundos
            )
        }
    }

    private fun dp(
        valor: Int
    ): Int {

        return (
            valor *
                resources.displayMetrics.density
            ).toInt()
    }

    // =========================================================
    // VIDEO VIEW COM GESTO
    // =========================================================

    class GestureVideoView(
        context: Context
    ) : VideoView(context) {

        var onSwipeLeft:
            (() -> Unit)? = null

        var onSwipeRight:
            (() -> Unit)? = null

        private var toqueX = 0f
        private var toqueY = 0f

        private var movimentoDetectado =
            false

        private val distanciaMinima =
            120f

        override fun onTouchEvent(
            event: MotionEvent
        ): Boolean {

            when (
                event.actionMasked
            ) {

                MotionEvent.ACTION_DOWN -> {

                    toqueX =
                        event.x

                    toqueY =
                        event.y

                    movimentoDetectado =
                        false
                }

                MotionEvent.ACTION_MOVE -> {

                    val distanciaX =
                        event.x - toqueX

                    val distanciaY =
                        event.y - toqueY

                    if (
                        kotlin.math.abs(
                            distanciaX
                        ) >
                        distanciaMinima &&
                        kotlin.math.abs(
                            distanciaX
                        ) >
                        kotlin.math.abs(
                            distanciaY
                        )
                    ) {

                        movimentoDetectado =
                            true
                    }
                }

                MotionEvent.ACTION_UP -> {

                    val distanciaX =
                        event.x - toqueX

                    val distanciaY =
                        event.y - toqueY

                    if (
                        kotlin.math.abs(
                            distanciaX
                        ) >
                        distanciaMinima &&
                        kotlin.math.abs(
                            distanciaX
                        ) >
                        kotlin.math.abs(
                            distanciaY
                        )
                    ) {

                        if (
                            distanciaX < 0
                        ) {

                            onSwipeLeft?.invoke()

                        } else {

                            onSwipeRight?.invoke()
                        }

                        movimentoDetectado =
                            true
                    }
                }
            }

            return super.onTouchEvent(
                event
            )
        }
    }
}
