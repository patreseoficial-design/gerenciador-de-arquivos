package com.gerenciadordearquivos.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
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
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.abs

class VideoViewerActivity : Activity() {

    private lateinit var playerView: GesturePlayerView
    private lateinit var nomePastaText: TextView
    private lateinit var nomeArquivoText: TextView
    private lateinit var contadorText: TextView

    private lateinit var raiz: FrameLayout
    private lateinit var barraSuperior: LinearLayout

    private val arquivos = ArrayList<String>()
    private val arquivosDoPlayer = ArrayList<String>()

    private var posicaoAtual = 0
    private var player: ExoPlayer? = null
    private var playerPreparado = false
    private var reproduzirAoRetornar = true

    private var popupAtual: PopupWindow? = null

    private var ultimaPosicao = 0L
    private var ultimoIndicePlayer = 0

    private var dialogoErroAberto = false
    private var fallbackExternoTentado = false

    private lateinit var arquivoAtual: File

    private val pastaLixeira = File(
        Environment.getExternalStorageDirectory(),
        ".GerenciadorArquivos/.Lixeira"
    )

    private val preferenciasDiagnostico = "diagnostico_video"
    private val chaveErro = "ultimo_erro"
    private val chaveData = "data_erro"

    companion object {
        private var capturadorInstalado = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instalarCapturadorErros()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        carregarDadosDaIntent()

        if (isFinishing) {
            return
        }

        criarInterface()
        restaurarEstado(savedInstanceState)

        mostrarErroFatalAnterior()

        inicializarPlayer()
        configurarGestos()
    }

    override fun onResume() {
        super.onResume()

        if (
            player != null &&
            playerPreparado &&
            reproduzirAoRetornar
        ) {
            player?.playWhenReady = true
        }
    }

    override fun onPause() {
        super.onPause()

        player?.let {
            ultimaPosicao = it.currentPosition
            ultimoIndicePlayer = it.currentMediaItemIndex
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(
            "ultima_posicao",
            ultimaPosicao
        )

        outState.putInt(
            "ultimo_indice",
            ultimoIndicePlayer
        )

        outState.putInt(
            "posicao_atual",
            posicaoAtual
        )

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        popupAtual?.dismiss()
        popupAtual = null

        liberarPlayer()

        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            // Barra de navegação do celular fica visível e
            // as barras do player ficam afastadas dela
            BarrasDoSistema.configurarTelaCheia(
                this
            )
        }
    }

    private fun instalarCapturadorErros() {
        if (capturadorInstalado) {
            return
        }

        capturadorInstalado = true

        val handlerAnterior =
            Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->

            try {
                getSharedPreferences(
                    preferenciasDiagnostico,
                    MODE_PRIVATE
                )
                    .edit()
                    .putString(
                        chaveErro,
                        gerarDiagnostico(
                            "ERRO FATAL",
                            throwable
                        )
                    )
                    .putLong(
                        chaveData,
                        System.currentTimeMillis()
                    )
                    .apply()

            } catch (_: Exception) {
            }

            handlerAnterior?.uncaughtException(
                thread,
                throwable
            )
        }
    }

    private fun mostrarErroFatalAnterior() {
        val prefs =
            getSharedPreferences(
                preferenciasDiagnostico,
                MODE_PRIVATE
            )

        val erro =
            prefs.getString(
                chaveErro,
                null
            ) ?: return

        prefs.edit()
            .remove(chaveErro)
            .remove(chaveData)
            .apply()

        AlertDialog.Builder(this)
            .setTitle("Erro anterior")
            .setMessage(
                "O aplicativo registrou um erro na última execução.\n\n$erro"
            )
            .setPositiveButton(
                "OK",
                null
            )
            .setNeutralButton(
                "COPIAR"
            ) { _, _ ->
                copiarTexto(erro)
            }
            .show()
    }

    private fun restaurarEstado(
        savedInstanceState: Bundle?
    ) {
        if (savedInstanceState == null) {
            return
        }

        ultimaPosicao =
            savedInstanceState.getLong(
                "ultima_posicao",
                0L
            )

        ultimoIndicePlayer =
            savedInstanceState.getInt(
                "ultimo_indice",
                0
            )

        posicaoAtual =
            savedInstanceState.getInt(
                "posicao_atual",
                posicaoAtual
            )

        if (posicaoAtual !in arquivos.indices) {
            posicaoAtual = 0
        }

        if (arquivos.isNotEmpty()) {
            arquivoAtual =
                File(
                    arquivos[posicaoAtual]
                )
        }

        atualizarCabecalho()
    }

    private fun carregarDadosDaIntent() {
        arquivos.clear()

        val lista =
            intent.getStringArrayListExtra(
                "arquivos"
            )

        if (lista != null) {
            arquivos.addAll(lista)
        }

        var caminho =
            intent.getStringExtra(
                "arquivo"
            )

        if (caminho.isNullOrBlank()) {
            caminho =
                intent.data?.path
        }

        if (
            !caminho.isNullOrBlank() &&
            !arquivos.contains(caminho)
        ) {
            arquivos.add(caminho)
        }

        posicaoAtual =
            intent.getIntExtra(
                "posicao",
                0
            )

        if (arquivos.isEmpty()) {

            Toast.makeText(
                this,
                "Nenhum vídeo foi recebido.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        if (posicaoAtual !in arquivos.indices) {
            posicaoAtual = 0
        }

        arquivoAtual =
            File(
                arquivos[posicaoAtual]
            )
    }

    private fun criarInterface() {

        raiz =
            FrameLayout(this)

        raiz.setBackgroundColor(
            Color.BLACK
        )

        val principal =
            LinearLayout(this)

        principal.orientation =
            LinearLayout.VERTICAL

        principal.setBackgroundColor(
            Color.BLACK
        )

        raiz.addView(
            principal,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        criarBarraSuperior(principal)

        playerView =
            GesturePlayerView(this)

        playerView.setBackgroundColor(
            Color.BLACK
        )

        playerView.useController = true

        playerView.controllerShowTimeoutMs = 3000

        playerView.controllerHideOnTouch = true

        playerView.setShowBuffering(
            PlayerView.SHOW_BUFFERING_WHEN_PLAYING
        )

        playerView.onSwipeLeft = {
            trocarVideo(1)
        }

        playerView.onSwipeRight = {
            trocarVideo(-1)
        }

        principal.addView(
            playerView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // Sem barra inferior com setas: para trocar de vídeo
        // basta deslizar na tela, como na galeria

        BarrasDoSistema.aoMudarEspacos(
            raiz
        ) { esquerda, topo, direita, baixo ->

            principal.setPadding(
                esquerda,
                topo,
                direita,
                baixo
            )
        }

        setContentView(raiz)

        atualizarCabecalho()
    }

    private fun criarBarraSuperior(
        principal: LinearLayout
    ) {
        barraSuperior =
            LinearLayout(this)

        barraSuperior.orientation =
            LinearLayout.HORIZONTAL

        barraSuperior.gravity =
            Gravity.CENTER_VERTICAL

        barraSuperior.setPadding(
            dp(12),
            dp(8),
            dp(8),
            dp(8)
        )

        barraSuperior.setBackgroundColor(
            Color.rgb(25, 25, 25)
        )

        val voltar =
            criarBotao(
                "‹",
                34
            )

        voltar.setOnClickListener {
            finish()
        }

        barraSuperior.addView(
            voltar,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

        val blocoTitulo =
            LinearLayout(this)

        blocoTitulo.orientation =
            LinearLayout.VERTICAL

        blocoTitulo.gravity =
            Gravity.CENTER_VERTICAL

        nomePastaText =
            TextView(this)

        nomePastaText.setTextColor(
            Color.LTGRAY
        )

        nomePastaText.textSize = 12f

        nomePastaText.maxLines = 1

        nomePastaText.ellipsize =
            TextUtils.TruncateAt.END

        nomeArquivoText =
            TextView(this)

        nomeArquivoText.setTextColor(
            Color.WHITE
        )

        nomeArquivoText.textSize = 16f

        nomeArquivoText.maxLines = 1

        nomeArquivoText.ellipsize =
            TextUtils.TruncateAt.END

        blocoTitulo.addView(
            nomePastaText
        )

        blocoTitulo.addView(
            nomeArquivoText
        )

        barraSuperior.addView(
            blocoTitulo,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        contadorText =
            TextView(this)

        contadorText.setTextColor(
            Color.WHITE
        )

        contadorText.textSize = 13f

        contadorText.gravity =
            Gravity.CENTER

        contadorText.setPadding(
            dp(8),
            0,
            dp(8),
            0
        )

        barraSuperior.addView(
            contadorText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(48)
            )
        )

        val menu =
            criarBotao(
                "⋮",
                30
            )

        menu.setOnClickListener {
            mostrarMenu()
        }

        barraSuperior.addView(
            menu,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

        principal.addView(
            barraSuperior,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(64)
            )
        )
    }

    private fun criarBotao(
        texto: String,
        tamanho: Int
    ): TextView {

        val botao =
            TextView(this)

        botao.text =
            texto

        botao.textSize =
            tamanho.toFloat()

        botao.setTextColor(
            Color.WHITE
        )

        botao.gravity =
            Gravity.CENTER

        botao.isClickable = true

        botao.isFocusable = true

        return botao
    }

    private fun atualizarCabecalho() {
        if (!::arquivoAtual.isInitialized) {
            return
        }

        nomeArquivoText.text =
            arquivoAtual.name

        nomePastaText.text =
            arquivoAtual.parentFile?.name ?: ""

        contadorText.text =
            "${posicaoAtual + 1}/${arquivos.size}"
    }

    private fun inicializarPlayer() {

        if (!::arquivoAtual.isInitialized) {
            return
        }

        if (!arquivoAtual.exists()) {
            mostrarDiagnostico(
                "O arquivo não existe:\n\n${arquivoAtual.absolutePath}"
            )
            return
        }

        if (!arquivoAtual.isFile) {
            mostrarDiagnostico(
                "O caminho informado não é um arquivo."
            )
            return
        }

        if (!arquivoAtual.canRead()) {
            mostrarDiagnostico(
                "O arquivo não pode ser lido:\n\n${arquivoAtual.absolutePath}"
            )
            return
        }

        dialogoErroAberto = false
        playerPreparado = false

        liberarPlayer()

        arquivosDoPlayer.clear()

        try {

            val renderersFactory =
                DefaultRenderersFactory(this)
                    .setEnableDecoderFallback(true)

            val novoPlayer =
                ExoPlayer.Builder(
                    this,
                    renderersFactory
                ).build()

            player =
                novoPlayer

            playerView.player =
                novoPlayer

            novoPlayer.addListener(
                object : Player.Listener {

                    override fun onPlaybackStateChanged(
                        playbackState: Int
                    ) {
                        when (playbackState) {

                            Player.STATE_READY -> {

                                playerPreparado =
                                    true

                                if (
                                    ultimaPosicao > 0L
                                ) {
                                    try {
                                        novoPlayer.seekTo(
                                            ultimaPosicao
                                        )
                                    } catch (_: Exception) {
                                    }
                                }

                                if (
                                    reproduzirAoRetornar
                                ) {
                                    novoPlayer.playWhenReady =
                                        true
                                }
                            }

                            Player.STATE_BUFFERING -> {
                                playerPreparado =
                                    false
                            }

                            Player.STATE_IDLE -> {
                                playerPreparado =
                                    false
                            }

                            Player.STATE_ENDED -> {
                                reproduzirAoRetornar =
                                    false
                            }
                        }
                    }

                    override fun onMediaItemTransition(
                        mediaItem: MediaItem?,
                        reason: Int
                    ) {
                        val indice =
                            novoPlayer.currentMediaItemIndex

                        if (
                            indice in arquivosDoPlayer.indices
                        ) {

                            val caminho =
                                arquivosDoPlayer[indice]

                            val novoArquivo =
                                File(caminho)

                            if (novoArquivo.exists()) {

                                arquivoAtual =
                                    novoArquivo

                                val originalIndex =
                                    arquivos.indexOf(
                                        caminho
                                    )

                                if (
                                    originalIndex >= 0
                                ) {
                                    posicaoAtual =
                                        originalIndex
                                }

                                atualizarCabecalho()
                            }
                        }
                    }

                    override fun onPlayerError(
                        error: PlaybackException
                    ) {
                        playerPreparado =
                            false

                        tratarFalhaReproducaoInterna(
                            error
                        )
                    }
                }
            )

            for (caminho in arquivos) {

                val arquivo =
                    File(caminho)

                if (!arquivo.exists()) {
                    continue
                }

                if (!arquivo.isFile) {
                    continue
                }

                if (!arquivo.canRead()) {
                    continue
                }

                arquivosDoPlayer.add(
                    arquivo.absolutePath
                )

                val mime =
                    obterMimeType(arquivo)

                val builder =
                    MediaItem.Builder()
                        .setUri(
                            Uri.fromFile(arquivo)
                        )

                if (mime != "video/*") {
                    builder.setMimeType(mime)
                }

                novoPlayer.addMediaItem(
                    builder.build()
                )
            }

            if (arquivosDoPlayer.isEmpty()) {

                mostrarDiagnostico(
                    "Nenhum vídeo válido foi encontrado."
                )

                liberarPlayer()
                return
            }

            var indiceInicial =
                arquivosDoPlayer.indexOf(
                    arquivoAtual.absolutePath
                )

            if (indiceInicial < 0) {
                indiceInicial = 0
            }

            ultimoIndicePlayer =
                indiceInicial

            novoPlayer.seekTo(
                indiceInicial,
                ultimaPosicao
            )

            novoPlayer.prepare()

            novoPlayer.playWhenReady =
                reproduzirAoRetornar

        } catch (e: Throwable) {

            tratarFalhaInicializacaoPlayer(
                e
            )
        }
    }

    private fun tratarFalhaReproducaoInterna(
        erro: PlaybackException
    ) {

        if (fallbackExternoTentado) {
            mostrarErroMedia3(erro)
            return
        }

        fallbackExternoTentado = true

        try {
            ultimaPosicao =
                player?.currentPosition
                    ?: ultimaPosicao
        } catch (_: Exception) {
        }

        liberarPlayer()

        raiz.post {

            if (!isFinishing) {
                abrirVideoExternamenteAutomatico(
                    erro
                )
            }
        }
    }

    private fun tratarFalhaInicializacaoPlayer(
        erro: Throwable
    ) {

        playerPreparado = false

        reproduzirAoRetornar = false

        if (fallbackExternoTentado) {

            mostrarDiagnostico(
                gerarDiagnostico(
                    "Falha ao iniciar o player",
                    erro
                )
            )

            return
        }

        fallbackExternoTentado = true

        liberarPlayer()

        raiz.post {

            if (!isFinishing) {
                abrirVideoExternamenteAutomatico(
                    erro
                )
            }
        }
    }

    private fun abrirVideoExternamenteAutomatico(
        erroInterno: Throwable
    ) {

        if (isFinishing) {
            return
        }

        var erroExterno: Throwable? =
            null

        try {

            if (!::arquivoAtual.isInitialized) {
                mostrarFalhaSemPlayerExterno(
                    erroInterno
                )
                return
            }

            if (
                !arquivoAtual.exists() ||
                !arquivoAtual.isFile
            ) {
                mostrarFalhaSemPlayerExterno(
                    erroInterno
                )
                return
            }

            val uri =
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    arquivoAtual
                )

            val mime =
                obterMimeType(
                    arquivoAtual
                )

            val intentExato =
                criarIntentVideoExterno(
                    uri,
                    mime
                )

            val resolvedExato =
                packageManager.queryIntentActivities(
                    intentExato,
                    0
                )

            if (resolvedExato.isNotEmpty()) {

                concederPermissaoUri(
                    resolvedExato,
                    uri
                )

                try {
                    startActivity(
                        intentExato
                    )
                    return
                } catch (e: Exception) {
                    erroExterno = e
                }
            }

            if (mime != "video/*") {

                val intentGenerico =
                    criarIntentVideoExterno(
                        uri,
                        "video/*"
                    )

                val resolvedGenerico =
                    packageManager.queryIntentActivities(
                        intentGenerico,
                        0
                    )

                if (resolvedGenerico.isNotEmpty()) {

                    concederPermissaoUri(
                        resolvedGenerico,
                        uri
                    )

                    try {
                        startActivity(
                            intentGenerico
                        )
                        return
                    } catch (e: Exception) {
                        erroExterno = e
                    }
                }
            }

            mostrarFalhaSemPlayerExterno(
                erroInterno,
                erroExterno
            )

        } catch (e: Exception) {

            erroExterno = e

            mostrarFalhaSemPlayerExterno(
                erroInterno,
                erroExterno
            )
        }
    }

    private fun criarIntentVideoExterno(
        uri: Uri,
        mime: String
    ): Intent {

        return Intent(
            Intent.ACTION_VIEW
        ).apply {

            setDataAndType(
                uri,
                mime
            )

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            clipData =
                ClipData.newRawUri(
                    "Vídeo",
                    uri
                )
        }
    }

    private fun concederPermissaoUri(
        resolved: List<android.content.pm.ResolveInfo>,
        uri: Uri
    ) {

        for (info in resolved) {

            try {

                grantUriPermission(
                    info.activityInfo.packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

            } catch (_: Exception) {
            }
        }
    }

    private fun mostrarFalhaSemPlayerExterno(
        erroInterno: Throwable,
        erroExterno: Throwable? = null
    ) {

        if (isFinishing) {
            return
        }

        if (dialogoErroAberto) {
            return
        }

        dialogoErroAberto = true

        val detalhes =
            StringBuilder()

        detalhes.append(
            "Não foi possível reproduzir este vídeo.\n\n"
        )

        detalhes.append(
            "Arquivo:\n"
        )

        detalhes.append(
            "${arquivoAtual.absolutePath}\n\n"
        )

        detalhes.append(
            "Tamanho:\n"
        )

        detalhes.append(
            formatarTamanho(
                arquivoAtual.length()
            )
        )

        detalhes.append(
            "\n\nMIME:\n"
        )

        detalhes.append(
            obterMimeType(
                arquivoAtual
            )
        )

        detalhes.append(
            "\n\nPlayer interno:\n"
        )

        detalhes.append(
            erroInterno.message
                ?: erroInterno.javaClass.simpleName
        )

        if (erroExterno != null) {

            detalhes.append(
                "\n\nPlayer externo:\n"
            )

            detalhes.append(
                erroExterno.message
                    ?: erroExterno.javaClass.simpleName
            )
        }

        detalhes.append(
            "\n\nPossíveis causas:\n"
        )

        detalhes.append(
            "• formato ou codec não suportado\n"
        )

        detalhes.append(
            "• arquivo corrompido\n"
        )

        detalhes.append(
            "• extensão incorreta\n"
        )

        detalhes.append(
            "• arquivo protegido ou sem permissão\n"
        )

        detalhes.append(
            "• nenhum aplicativo compatível instalado"
        )

        AlertDialog.Builder(this)
            .setTitle(
                "Não foi possível abrir o vídeo"
            )
            .setMessage(
                detalhes.toString()
            )
            .setPositiveButton(
                "TENTAR NOVAMENTE"
            ) { _, _ ->

                dialogoErroAberto = false

                fallbackExternoTentado = false

                reproduzirAoRetornar = true

                inicializarPlayer()
            }
            .setNeutralButton(
                "COPIAR ERRO"
            ) { _, _ ->

                copiarTexto(
                    detalhes.toString()
                )

                dialogoErroAberto = false
            }
            .setNegativeButton(
                "FECHAR"
            ) { _, _ ->

                dialogoErroAberto = false

                finish()
            }
            .setOnDismissListener {
                dialogoErroAberto = false
            }
            .show()
    }

    private fun mostrarErroMedia3(
        erro: PlaybackException
    ) {

        if (isFinishing) {
            return
        }

        if (dialogoErroAberto) {
            return
        }

        dialogoErroAberto = true

        val diagnostico =
            gerarDiagnostico(
                "ERRO MEDIA3",
                erro
            )

        AlertDialog.Builder(this)
            .setTitle(
                "Erro ao reproduzir vídeo"
            )
            .setMessage(
                diagnostico
            )
            .setPositiveButton(
                "TENTAR NOVAMENTE"
            ) { _, _ ->

                dialogoErroAberto = false

                fallbackExternoTentado = false

                reproduzirAoRetornar = true

                inicializarPlayer()
            }
            .setNeutralButton(
                "COPIAR ERRO"
            ) { _, _ ->

                copiarTexto(
                    diagnostico
                )

                dialogoErroAberto = false
            }
            .setNegativeButton(
                "FECHAR"
            ) { _, _ ->

                dialogoErroAberto = false

                finish()
            }
            .setOnDismissListener {
                dialogoErroAberto = false
            }
            .show()
    }

    private fun mostrarDiagnostico(
        mensagem: String
    ) {

        if (isFinishing) {
            return
        }

        if (dialogoErroAberto) {
            return
        }

        dialogoErroAberto = true

        AlertDialog.Builder(this)
            .setTitle("Diagnóstico")
            .setMessage(mensagem)
            .setPositiveButton(
                "OK"
            ) { _, _ ->
                dialogoErroAberto = false
            }
            .setNeutralButton(
                "COPIAR"
            ) { _, _ ->

                copiarTexto(
                    mensagem
                )

                dialogoErroAberto = false
            }
            .setOnDismissListener {
                dialogoErroAberto = false
            }
            .show()
    }

    private fun gerarDiagnostico(
        titulo: String,
        erro: Throwable
    ): String {

        val texto =
            StringBuilder()

        texto.append(
            titulo
        )

        texto.append(
            "\n\n"
        )

        if (::arquivoAtual.isInitialized) {

            texto.append(
                "Arquivo:\n"
            )

            texto.append(
                arquivoAtual.absolutePath
            )

            texto.append(
                "\n\nTamanho:\n"
            )

            texto.append(
                formatarTamanho(
                    arquivoAtual.length()
                )
            )

            texto.append(
                "\n\nMIME:\n"
            )

            texto.append(
                obterMimeType(
                    arquivoAtual
                )
            )
        }

        texto.append(
            "\n\nClasse:\n"
        )

        texto.append(
            erro.javaClass.name
        )

        texto.append(
            "\n\nMensagem:\n"
        )

        texto.append(
            erro.message
                ?: "Sem mensagem"
        )

        if (erro is PlaybackException) {

            texto.append(
                "\n\nCódigo Media3:\n"
            )

            texto.append(
                erro.errorCode
            )

            texto.append(
                "\n\nNome do código:\n"
            )

            texto.append(
                PlaybackException.getErrorCodeName(
                    erro.errorCode
                )
            )
        }

        erro.cause?.let { causa ->

            texto.append(
                "\n\nCausa:\n"
            )

            texto.append(
                causa.javaClass.name
            )

            texto.append(
                "\n"
            )

            texto.append(
                causa.message ?: ""
            )
        }

        texto.append(
            "\n\nStacktrace:\n"
        )

        texto.append(
            erro.stackTraceToString()
        )

        return texto.toString()
    }

    private fun copiarTexto(
        texto: String
    ) {

        try {

            val clipboard =
                getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager

            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "Diagnóstico",
                    texto
                )
            )

            Toast.makeText(
                this,
                "Diagnóstico copiado.",
                Toast.LENGTH_SHORT
            ).show()

        } catch (_: Exception) {
        }
    }

    private fun liberarPlayer() {

        try {

            player?.let {

                ultimaPosicao =
                    it.currentPosition

                ultimoIndicePlayer =
                    it.currentMediaItemIndex

                it.stop()
                it.release()
            }

        } catch (_: Exception) {
        }

        player = null

        playerPreparado = false

        if (::playerView.isInitialized) {
            playerView.player = null
        }
    }

    private fun configurarGestos() {
        // Os gestos são tratados pelo GesturePlayerView.
    }

    private fun trocarVideo(
        deslocamento: Int
    ) {

        if (arquivos.isEmpty()) {
            return
        }

        val novoIndice =
            posicaoAtual + deslocamento

        if (novoIndice !in arquivos.indices) {

            Toast.makeText(
                this,
                if (deslocamento > 0) "Este é o último vídeo"
                else "Este é o primeiro vídeo",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        posicaoAtual =
            novoIndice

        arquivoAtual =
            File(
                arquivos[posicaoAtual]
            )

        ultimaPosicao = 0L

        ultimoIndicePlayer = 0

        fallbackExternoTentado = false

        reproduzirAoRetornar = true

        atualizarCabecalho()

        inicializarPlayer()
    }

    private fun mostrarMenu() {

        if (isFinishing) {
            return
        }

        val layout =
            LinearLayout(this)

        layout.orientation =
            LinearLayout.VERTICAL

        MenuEscuro.prepararMenu(
            layout
        )

        val opcoes =
            arrayOf(
                "Compartilhar",
                "Abrir com...",
                "Informações",
                "Renomear",
                "Copiar",
                "Mover",
                "Criar pasta",
                "Enviar para lixeira"
            )

        val popup =
            PopupWindow(
                layout,
                dp(270),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )

        popupAtual =
            popup

        for (opcao in opcoes) {

            val item =
                MenuEscuro.criarLinha(
                    this,
                    "",
                    opcao
                ) {}

            item.setOnClickListener {

                popup.dismiss()

                popupAtual = null

                when (opcao) {

                    "Compartilhar" ->
                        compartilharArquivo()

                    "Abrir com..." ->
                        abrirComAplicativo()

                    "Informações" ->
                        mostrarInformacoes()

                    "Renomear" ->
                        renomearArquivo()

                    "Copiar" ->
                        copiarArquivo()

                    "Mover" ->
                        moverArquivo()

                    "Criar pasta" ->
                        criarPasta()

                    "Enviar para lixeira" ->
                        enviarParaLixeira()
                }
            }

            layout.addView(
                item
            )
        }

        popup.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(
                Color.TRANSPARENT
            )
        )

        popup.isOutsideTouchable =
            true

        popup.elevation =
            dp(8).toFloat()

        popup.setOnDismissListener {
            popupAtual = null
        }

        popup.showAtLocation(
            raiz,
            Gravity.TOP or Gravity.END,
            dp(8),
            dp(72)
        )
    }

    private fun compartilharArquivo() {

        if (!arquivoAtual.exists()) {
            return
        }

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

                    clipData =
                        ClipData.newRawUri(
                            "Vídeo",
                            uri
                        )
                }

            startActivity(
                Intent.createChooser(
                    intent,
                    "Compartilhar vídeo"
                )
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Não foi possível compartilhar.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun abrirComAplicativo() {

        if (!arquivoAtual.exists()) {
            return
        }

        try {

            val uri =
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    arquivoAtual
                )

            val mime =
                obterMimeType(
                    arquivoAtual
                )

            val intentExato =
                criarIntentVideoExterno(
                    uri,
                    mime
                )

            val resolved =
                packageManager.queryIntentActivities(
                    intentExato,
                    0
                )

            if (resolved.isNotEmpty()) {

                concederPermissaoUri(
                    resolved,
                    uri
                )

                startActivity(
                    intentExato
                )

                return
            }

            if (mime != "video/*") {

                val generico =
                    criarIntentVideoExterno(
                        uri,
                        "video/*"
                    )

                val resolvedGenerico =
                    packageManager.queryIntentActivities(
                        generico,
                        0
                    )

                if (resolvedGenerico.isNotEmpty()) {

                    concederPermissaoUri(
                        resolvedGenerico,
                        uri
                    )

                    startActivity(
                        generico
                    )

                    return
                }
            }

            Toast.makeText(
                this,
                "Nenhum aplicativo compatível foi encontrado.",
                Toast.LENGTH_LONG
            ).show()

        } catch (_: ActivityNotFoundException) {

            Toast.makeText(
                this,
                "Nenhum aplicativo consegue abrir este vídeo.",
                Toast.LENGTH_LONG
            ).show()

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Erro ao abrir o vídeo.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun mostrarInformacoes() {

        if (!arquivoAtual.exists()) {
            return
        }

        val info =
            StringBuilder()

        info.append(
            "Nome:\n"
        )

        info.append(
            arquivoAtual.name
        )

        info.append(
            "\n\nCaminho:\n"
        )

        info.append(
            arquivoAtual.absolutePath
        )

        info.append(
            "\n\nTamanho:\n"
        )

        info.append(
            formatarTamanho(
                arquivoAtual.length()
            )
        )

        info.append(
            "\n\nTipo:\n"
        )

        info.append(
            obterMimeType(
                arquivoAtual
            )
        )

        try {

            val retriever =
                MediaMetadataRetriever()

            retriever.setDataSource(
                arquivoAtual.absolutePath
            )

            val duracao =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )

            val largura =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                )

            val altura =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                )

            val codec =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_MIMETYPE
                )

            if (!duracao.isNullOrBlank()) {

                info.append(
                    "\n\nDuração:\n"
                )

                info.append(
                    formatarDuracao(
                        duracao.toLongOrNull()
                            ?: 0L
                    )
                )
            }

            if (
                !largura.isNullOrBlank() &&
                !altura.isNullOrBlank()
            ) {

                info.append(
                    "\n\nResolução:\n"
                )

                info.append(
                    "$largura x $altura"
                )
            }

            if (!codec.isNullOrBlank()) {

                info.append(
                    "\n\nFormato detectado:\n"
                )

                info.append(
                    codec
                )
            }

            retriever.release()

        } catch (_: Exception) {
        }

        AlertDialog.Builder(this)
            .setTitle(
                "Informações do vídeo"
            )
            .setMessage(
                info.toString()
            )
            .setPositiveButton(
                "OK",
                null
            )
            .setNeutralButton(
                "COPIAR"
            ) { _, _ ->
                copiarTexto(
                    info.toString()
                )
            }
            .show()
    }

    private fun renomearArquivo() {

        if (!arquivoAtual.exists()) {
            return
        }

        val campo =
            EditText(this)

        campo.setText(
            arquivoAtual.name
        )

        campo.selectAll()

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Renomear"
                )
                .setView(campo)
                .setNegativeButton(
                    "CANCELAR",
                    null
                )
                .setPositiveButton(
                    "RENOMEAR",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val novoNome =
                    campo.text.toString().trim()

                if (novoNome.isBlank()) {

                    campo.error =
                        "Digite um nome."

                    return@setOnClickListener
                }

                val pai =
                    arquivoAtual.parentFile

                if (pai == null) {

                    campo.error =
                        "Pasta de origem inválida."

                    return@setOnClickListener
                }

                val destino =
                    File(
                        pai,
                        novoNome
                    )

                if (destino.exists()) {

                    campo.error =
                        "Já existe um arquivo com esse nome."

                    return@setOnClickListener
                }

                try {

                    if (
                        arquivoAtual.renameTo(
                            destino
                        )
                    ) {

                        arquivos[posicaoAtual] =
                            destino.absolutePath

                        arquivoAtual =
                            destino

                        atualizarCabecalho()

                        Toast.makeText(
                            this,
                            "Arquivo renomeado.",
                            Toast.LENGTH_SHORT
                        ).show()

                        dialog.dismiss()

                    } else {

                        campo.error =
                            "Não foi possível renomear."
                    }

                } catch (e: Exception) {

                    campo.error =
                        e.message
                            ?: "Erro ao renomear."
                }
            }
        }

        dialog.show()
    }

    private fun copiarArquivo() {

        selecionarPastaDestino(
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
                        "Já existe um arquivo com esse nome.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@selecionarPastaDestino
                }

                arquivoAtual.copyTo(
                    destino,
                    overwrite = false
                )

                Toast.makeText(
                    this,
                    "Arquivo copiado.",
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

    private fun moverArquivo() {

        selecionarPastaDestino(
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
                        "Já existe um arquivo com esse nome.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@selecionarPastaDestino
                }

                if (
                    arquivoAtual.renameTo(
                        destino
                    )
                ) {

                    arquivos[posicaoAtual] =
                        destino.absolutePath

                    arquivoAtual =
                        destino

                    atualizarCabecalho()

                    Toast.makeText(
                        this,
                        "Arquivo movido.",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    arquivoAtual.copyTo(
                        destino,
                        overwrite = false
                    )

                    if (arquivoAtual.delete()) {

                        arquivos[posicaoAtual] =
                            destino.absolutePath

                        arquivoAtual =
                            destino

                        atualizarCabecalho()
                    }
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

    private fun selecionarPastaDestino(
        titulo: String,
        callback: (File) -> Unit
    ) {

        val raizStorage =
            Environment.getExternalStorageDirectory()

        val pastas =
            ArrayList<File>()

        adicionarPastasRecursivamente(
            raizStorage,
            pastas,
            0
        )

        pastas.add(
            0,
            raizStorage
        )

        val nomes =
            pastas.map { pasta ->

                if (
                    pasta.absolutePath ==
                    raizStorage.absolutePath
                ) {

                    "Armazenamento principal"

                } else {

                    pasta.absolutePath
                        .removePrefix(
                            raizStorage.absolutePath
                        )
                        .trim('/')
                }

            }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setItems(
                nomes
            ) { _, which ->

                callback(
                    pastas[which]
                )
            }
            .setNegativeButton(
                "CANCELAR",
                null
            )
            .show()
    }

    private fun adicionarPastasRecursivamente(
        pasta: File,
        lista: ArrayList<File>,
        nivel: Int
    ) {

        if (nivel >= 3) {
            return
        }

        try {

            val filhos =
                pasta.listFiles()
                    ?: return

            for (filho in filhos) {

                if (!filho.isDirectory) {
                    continue
                }

                if (filho.name.startsWith(".")) {
                    continue
                }

                lista.add(filho)

                adicionarPastasRecursivamente(
                    filho,
                    lista,
                    nivel + 1
                )
            }

        } catch (_: Exception) {
        }
    }

    private fun criarPasta() {

        val campo =
            EditText(this)

        campo.hint =
            "Nome da pasta"

        AlertDialog.Builder(this)
            .setTitle(
                "Criar pasta"
            )
            .setView(campo)
            .setNegativeButton(
                "CANCELAR",
                null
            )
            .setPositiveButton(
                "CRIAR"
            ) { _, _ ->

                val nome =
                    campo.text.toString().trim()

                if (nome.isBlank()) {

                    Toast.makeText(
                        this,
                        "Digite um nome.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                try {

                    val pasta =
                        File(
                            arquivoAtual.parentFile,
                            nome
                        )

                    if (pasta.exists()) {

                        Toast.makeText(
                            this,
                            "A pasta já existe.",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setPositiveButton
                    }

                    if (pasta.mkdirs()) {

                        Toast.makeText(
                            this,
                            "Pasta criada.",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        Toast.makeText(
                            this,
                            "Não foi possível criar a pasta.",
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

    private fun enviarParaLixeira() {

        if (!arquivoAtual.exists()) {
            return
        }

        AlertDialog.Builder(this)
            .setTitle(
                "Enviar para lixeira?"
            )
            .setMessage(
                "O arquivo será movido para a lixeira do Faxina."
            )
            .setNegativeButton(
                "CANCELAR",
                null
            )
            .setPositiveButton(
                "MOVER"
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

                        val extensao =
                            arquivoAtual.extension

                        destino =
                            if (extensao.isBlank()) {

                                File(
                                    pastaLixeira,
                                    "${arquivoAtual.nameWithoutExtension} ($contador)"
                                )

                            } else {

                                File(
                                    pastaLixeira,
                                    "${arquivoAtual.nameWithoutExtension} ($contador).$extensao"
                                )
                            }

                        contador++
                    }

                    val origem =
                        arquivoAtual

                    // Libera o arquivo antes de mover
                    liberarPlayer()

                    if (
                        Armazenamento.mover(
                            origem,
                            destino
                        )
                    ) {

                        Armazenamento.registrarNaLixeira(
                            this,
                            destino,
                            origem
                        )

                        arquivos.removeAt(
                            posicaoAtual
                        )

                        if (arquivos.isEmpty()) {

                            Toast.makeText(
                                this,
                                "Arquivo enviado para a lixeira.",
                                Toast.LENGTH_SHORT
                            ).show()

                            finish()

                            return@setPositiveButton
                        }

                        if (
                            posicaoAtual >= arquivos.size
                        ) {
                            posicaoAtual =
                                arquivos.size - 1
                        }

                        arquivoAtual =
                            File(
                                arquivos[posicaoAtual]
                            )

                        atualizarCabecalho()

                        fallbackExternoTentado = false

                        reproduzirAoRetornar = true

                        inicializarPlayer()

                        Toast.makeText(
                            this,
                            "Arquivo enviado para a lixeira.",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        // Não moveu: volta a tocar o vídeo
                        inicializarPlayer()

                        Toast.makeText(
                            this,
                            "Não foi possível mover o arquivo.",
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

            "ts" ->
                "video/mp2t"

            "flv" ->
                "video/x-flv"

            else ->
                "video/*"
        }
    }

    private fun formatarTamanho(
        tamanho: Long
    ): String {

        if (tamanho <= 0L) {
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

        var valor =
            tamanho.toDouble()

        var indice = 0

        while (
            valor >= 1024.0 &&
            indice < unidades.lastIndex
        ) {

            valor /= 1024.0

            indice++
        }

        val decimal =
            DecimalFormat(
                "0.##"
            )

        return "${
            decimal.format(valor)
        } ${unidades[indice]}"
    }

    private fun formatarDuracao(
        milissegundos: Long
    ): String {

        if (milissegundos <= 0L) {
            return "0:00"
        }

        val segundos =
            milissegundos / 1000

        val horas =
            segundos / 3600

        val minutos =
            (segundos % 3600) / 60

        val segundosRestantes =
            segundos % 60

        return if (horas > 0) {

            String.format(
                Locale.getDefault(),
                "%d:%02d:%02d",
                horas,
                minutos,
                segundosRestantes
            )

        } else {

            String.format(
                Locale.getDefault(),
                "%d:%02d",
                minutos,
                segundosRestantes
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

    class GesturePlayerView(
        context: Context
    ) : PlayerView(context) {

        var onSwipeLeft:
                (() -> Unit)? = null

        var onSwipeRight:
                (() -> Unit)? = null

        private var inicioX = 0f
        private var inicioY = 0f
        private var podeDeslizar = false

        /*
         * Deslizar para o lado troca de vídeo, como na galeria.
         * Usa dispatchTouchEvent para funcionar mesmo com os
         * controles do player na tela. Toques que começam na
         * parte de baixo (barra de tempo) não trocam de vídeo,
         * para não atrapalhar quem está avançando o vídeo.
         */
        override fun dispatchTouchEvent(
            event: MotionEvent
        ): Boolean {

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    inicioX =
                        event.x

                    inicioY =
                        event.y

                    podeDeslizar =
                        event.y < height - dpGesture(110)
                }

                MotionEvent.ACTION_POINTER_DOWN -> {

                    podeDeslizar = false
                }

                MotionEvent.ACTION_UP -> {

                    val distanciaX =
                        event.x - inicioX

                    val distanciaY =
                        event.y - inicioY

                    if (
                        podeDeslizar &&
                        abs(distanciaX) >
                        dpGesture(70) &&
                        abs(distanciaX) >
                        abs(distanciaY) * 1.5f
                    ) {

                        podeDeslizar = false

                        // Cancela o toque nos controles
                        val cancelar =
                            MotionEvent.obtain(event)

                        cancelar.action =
                            MotionEvent.ACTION_CANCEL

                        super.dispatchTouchEvent(
                            cancelar
                        )

                        cancelar.recycle()

                        if (distanciaX < 0) {

                            onSwipeLeft?.invoke()

                        } else {

                            onSwipeRight?.invoke()
                        }

                        return true
                    }
                }
            }

            return super.dispatchTouchEvent(
                event
            )
        }

        private fun dpGesture(
            valor: Int
        ): Float {

            return valor *
                    resources.displayMetrics.density
        }
    }
}
