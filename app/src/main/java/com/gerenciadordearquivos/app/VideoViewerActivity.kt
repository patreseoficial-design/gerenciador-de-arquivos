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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.abs

@UnstableApi
class VideoViewerActivity : Activity() {

    private lateinit var playerView: GesturePlayerView

    private lateinit var nomePastaText: TextView
    private lateinit var nomeArquivoText: TextView
    private lateinit var contadorText: TextView

    private lateinit var raiz: FrameLayout
    private lateinit var barraSuperior: LinearLayout
    private lateinit var barraInferior: LinearLayout

    private val arquivos = ArrayList<String>()

    /*
     * Lista EXATA dos arquivos que realmente entraram
     * no ExoPlayer.
     */
    private val arquivosDoPlayer =
        ArrayList<String>()

    private var posicaoAtual = 0

    private lateinit var arquivoAtual: File

    private var player: ExoPlayer? = null

    private var playerPreparado = false

    private var reproduzirAoRetornar = true

    private var popupAtual: PopupWindow? = null

    private var ultimaPosicao = 0L

    private var ultimoIndicePlayer = 0

    private var dialogoErroAberto = false

    private val pastaLixeira =
        File(
            Environment.getExternalStorageDirectory(),
            ".GerenciadorArquivos/.Lixeira"
        )

    private val preferenciasDiagnostico =
        "diagnostico_video"

    private val chaveErro =
        "ultimo_erro"

    private val chaveData =
        "data_erro"

    companion object {

        private var capturadorInstalado = false
    }

    // =========================================================
    // CICLO DE VIDA
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        try {

            instalarCapturaDeErroFatal()

            requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR

            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )

            configurarTelaCheia()

            carregarDadosIntent()

            if (isFinishing) {
                return
            }

            criarInterface()

            mostrarErroFatalAnteriorSeExistir()

            restaurarEstado(
                savedInstanceState
            )

            inicializarPlayer()

            configurarGestos()

        } catch (e: Throwable) {

            tratarErroFatal(
                "Erro durante a abertura do vídeo",
                e
            )
        }
    }

    override fun onResume() {
        super.onResume()

        try {

            configurarTelaCheia()

            if (::playerView.isInitialized) {
                playerView.requestLayout()
            }

            player?.let {

                if (
                    playerPreparado &&
                    reproduzirAoRetornar
                ) {

                    try {
                        it.play()
                    } catch (e: Exception) {

                        mostrarErroDiagnostico(
                            "Erro ao continuar a reprodução",
                            e
                        )
                    }
                }
            }

        } catch (e: Exception) {

            mostrarErroDiagnostico(
                "Erro no onResume",
                e
            )
        }
    }

    override fun onPause() {

        try {

            player?.let {

                try {
                    ultimaPosicao =
                        it.currentPosition
                } catch (_: Exception) {
                }

                try {

                    reproduzirAoRetornar =
                        it.isPlaying

                    if (it.isPlaying) {
                        it.pause()
                    }

                } catch (_: Exception) {
                }
            }

        } catch (_: Exception) {
        }

        super.onPause()
    }

    override fun onSaveInstanceState(
        outState: Bundle
    ) {

        try {

            player?.let {

                outState.putLong(
                    "posicao_video",
                    it.currentPosition
                )

                val indicePlayer =
                    it.currentMediaItemIndex

                if (
                    indicePlayer >= 0 &&
                    indicePlayer < arquivosDoPlayer.size
                ) {

                    outState.putString(
                        "caminho_video",
                        arquivosDoPlayer[indicePlayer]
                    )
                }

                outState.putInt(
                    "indice_video",
                    posicaoAtual
                )

                outState.putBoolean(
                    "reproduzindo",
                    it.isPlaying
                )
            }

        } catch (_: Exception) {
        }

        super.onSaveInstanceState(
            outState
        )
    }

    override fun onDestroy() {

        try {
            popupAtual?.dismiss()
        } catch (_: Exception) {
        }

        popupAtual = null

        liberarPlayer()

        super.onDestroy()
    }

    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {

        super.onWindowFocusChanged(
            hasFocus
        )

        if (hasFocus) {
            configurarTelaCheia()
        }
    }

    override fun onConfigurationChanged(
        newConfig: android.content.res.Configuration
    ) {

        super.onConfigurationChanged(
            newConfig
        )

        try {

            configurarTelaCheia()

            if (::playerView.isInitialized) {
                playerView.requestLayout()
                playerView.invalidate()
            }

            if (::barraSuperior.isInitialized) {

                barraSuperior.post {
                    ajustarTextos()
                }
            }

        } catch (e: Exception) {

            mostrarErroDiagnostico(
                "Erro ao mudar orientação da tela",
                e
            )
        }
    }

    // =========================================================
    // CAPTURA DE ERRO FATAL
    // =========================================================

    private fun instalarCapturaDeErroFatal() {

        if (capturadorInstalado) {
            return
        }

        capturadorInstalado = true

        val contexto =
            applicationContext

        val handlerAnterior =
            Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler {
                thread,
                throwable ->

            try {

                salvarErroFatal(
                    contexto,
                    thread,
                    throwable
                )

            } catch (_: Exception) {
            }

            try {

                handlerAnterior?.uncaughtException(
                    thread,
                    throwable
                )

            } catch (_: Exception) {
            }
        }
    }

    private fun salvarErroFatal(
        contexto: Context,
        thread: Thread,
        throwable: Throwable
    ) {

        try {

            val sb =
                StringBuilder()

            sb.append(
                "ERRO FATAL DO GERENCIADOR DE ARQUIVOS+\n\n"
            )

            sb.append(
                "Thread: "
            )

            sb.append(
                thread.name
            )

            sb.append(
                "\n\n"
            )

            sb.append(
                "Tipo: "
            )

            sb.append(
                throwable.javaClass.name
            )

            sb.append(
                "\n\n"
            )

            sb.append(
                "Mensagem:\n"
            )

            sb.append(
                throwable.message
                    ?: "Sem mensagem"
            )

            sb.append(
                "\n\n"
            )

            sb.append(
                "Causa:\n"
            )

            sb.append(
                throwable.cause?.toString()
                    ?: "Nenhuma"
            )

            sb.append(
                "\n\n"
            )

            if (::arquivoAtual.isInitialized) {

                sb.append(
                    "Arquivo:\n"
                )

                sb.append(
                    arquivoAtual.absolutePath
                )

                sb.append(
                    "\n\n"
                )
            }

            sb.append(
                "STACKTRACE:\n"
            )

            sb.append(
                throwable.stackTraceToString()
            )

            contexto
                .getSharedPreferences(
                    preferenciasDiagnostico,
                    Context.MODE_PRIVATE
                )
                .edit()
                .putString(
                    chaveErro,
                    sb.toString()
                )
                .putLong(
                    chaveData,
                    System.currentTimeMillis()
                )
                .commit()

        } catch (_: Exception) {
        }
    }

    // =========================================================
    // ERRO FATAL
    // =========================================================

    private fun tratarErroFatal(
        local: String,
        erro: Throwable
    ) {

        try {

            val sb =
                StringBuilder()

            sb.append(
                "LOCAL DO ERRO:\n"
            )

            sb.append(
                local
            )

            sb.append(
                "\n\n"
            )

            sb.append(
                "TIPO:\n"
            )

            sb.append(
                erro.javaClass.name
            )

            sb.append(
                "\n\n"
            )

            sb.append(
                "MENSAGEM:\n"
            )

            sb.append(
                erro.message
                    ?: "Sem mensagem"
            )

            sb.append(
                "\n\n"
            )

            sb.append(
                "CAUSA:\n"
            )

            sb.append(
                erro.cause?.toString()
                    ?: "Nenhuma"
            )

            sb.append(
                "\n\n"
            )

            if (::arquivoAtual.isInitialized) {

                sb.append(
                    "ARQUIVO:\n"
                )

                sb.append(
                    arquivoAtual.absolutePath
                )

                sb.append(
                    "\n\n"
                )
            }

            sb.append(
                "STACKTRACE:\n"
            )

            sb.append(
                erro.stackTraceToString()
            )

            getSharedPreferences(
                preferenciasDiagnostico,
                Context.MODE_PRIVATE
            )
                .edit()
                .putString(
                    chaveErro,
                    sb.toString()
                )
                .putLong(
                    chaveData,
                    System.currentTimeMillis()
                )
                .commit()

            if (::raiz.isInitialized) {

                raiz.post {

                    mostrarCaixaDiagnostico(
                        "ERRO AO ABRIR O VÍDEO",
                        sb.toString()
                    )
                }

            } else {

                Toast.makeText(
                    this,
                    "Erro ao abrir vídeo. O diagnóstico foi salvo.",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (_: Exception) {

            try {

                Toast.makeText(
                    this,
                    "Ocorreu um erro ao abrir o vídeo.",
                    Toast.LENGTH_LONG
                ).show()

            } catch (_: Exception) {
            }
        }
    }

    // =========================================================
    // DIAGNÓSTICO NORMAL
    // =========================================================

    private fun mostrarErroDiagnostico(
        local: String,
        erro: Throwable
    ) {

        try {

            val sb =
                StringBuilder()

            sb.append(
                "LOCAL DO ERRO:\n"
            )

            sb.append(
                local
            )

            sb.append(
                "\n\n"
            )

            sb.append(
                "TIPO:\n"
            )

            sb.append(
                erro.javaClass.name
            )

            sb.append(
                "\n\n"
            )

            sb.append(
                "MENSAGEM:\n"
            )

            sb.append(
                erro.message
                    ?: "Sem mensagem"
            )

            sb.append(
                "\n\n"
            )

            sb.append(
                "CAUSA:\n"
            )

            sb.append(
                erro.cause?.toString()
                    ?: "Nenhuma"
            )

            sb.append(
                "\n\n"
            )

            if (::arquivoAtual.isInitialized) {

                sb.append(
                    "ARQUIVO:\n"
                )

                sb.append(
                    arquivoAtual.absolutePath
                )

                sb.append(
                    "\n\n"
                )

                sb.append(
                    "EXISTE: "
                )

                sb.append(
                    arquivoAtual.exists()
                )

                sb.append(
                    "\n"
                )

                sb.append(
                    "É ARQUIVO: "
                )

                sb.append(
                    arquivoAtual.isFile
                )

                sb.append(
                    "\n"
                )

                sb.append(
                    "PODE LER: "
                )

                sb.append(
                    arquivoAtual.canRead()
                )

                sb.append(
                    "\n"
                )

                sb.append(
                    "TAMANHO: "
                )

                sb.append(
                    arquivoAtual.length()
                )

                sb.append(
                    " bytes\n"
                )
            }

            sb.append(
                "\nSTACKTRACE:\n"
            )

            sb.append(
                erro.stackTraceToString()
            )

            mostrarCaixaDiagnostico(
                "ERRO NO VÍDEO",
                sb.toString()
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Erro ao reproduzir vídeo",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================================================
    // ERRO FATAL ANTERIOR
    // =========================================================

    private fun mostrarErroFatalAnteriorSeExistir() {

        val prefs =
            getSharedPreferences(
                preferenciasDiagnostico,
                Context.MODE_PRIVATE
            )

        val erro =
            prefs.getString(
                chaveErro,
                null
            )

        if (
            erro.isNullOrBlank()
        ) {
            return
        }

        prefs.edit()
            .remove(chaveErro)
            .remove(chaveData)
            .apply()

        raiz.postDelayed(
            {
                mostrarCaixaDiagnostico(
                    "ERRO ANTERIOR DETECTADO",
                    erro
                )
            },
            500
        )
    }

    // =========================================================
    // CAIXA DE DIAGNÓSTICO
    // =========================================================

    private fun mostrarCaixaDiagnostico(
        titulo: String,
        detalhes: String
    ) {

        if (isFinishing) {
            return
        }

        if (dialogoErroAberto) {
            return
        }

        dialogoErroAberto = true

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(18),
                    dp(5),
                    dp(18),
                    dp(5)
                )
            }

        val aviso =
            TextView(this).apply {

                text =
                    "O aplicativo encontrou um erro ao tentar reproduzir o vídeo. " +
                            "Abaixo estão os detalhes técnicos para identificarmos exatamente o problema."

                textSize =
                    14f

                setTextColor(
                    Color.DKGRAY
                )

                setPadding(
                    0,
                    dp(5),
                    0,
                    dp(12)
                )
            }

        container.addView(
            aviso,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val scroll =
            ScrollView(this).apply {

                setFillViewport(
                    true
                )
            }

        val texto =
            TextView(this).apply {

                text =
                    detalhes

                textSize =
                    12f

                setTextColor(
                    Color.BLACK
                )

                setPadding(
                    dp(10),
                    dp(10),
                    dp(10),
                    dp(10)
                )

                setBackgroundColor(
                    Color.rgb(
                        240,
                        240,
                        240
                    )
                )

                setTextIsSelectable(
                    true
                )
            }

        /*
         * CORREÇÃO:
         * Não usamos LayoutParams diretamente.
         * O ScrollView recebe o TextView normalmente.
         */
        scroll.addView(
            texto
        )

        container.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(280)
            )
        )

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    titulo
                )
                .setView(
                    container
                )
                .setPositiveButton(
                    "TENTAR NOVAMENTE"
                ) { dialogInterface, _ ->

                    dialogoErroAberto =
                        false

                    try {
                        dialogInterface.dismiss()
                    } catch (_: Exception) {
                    }

                    raiz.postDelayed(
                        {
                            tentarReproduzirNovamente()
                        },
                        200
                    )
                }
                .setNeutralButton(
                    "COPIAR ERRO"
                ) { _, _ ->

                    copiarDiagnostico(
                        detalhes
                    )

                    dialogoErroAberto =
                        false
                }
                .setNegativeButton(
                    "FECHAR"
                ) { _, _ ->

                    dialogoErroAberto =
                        false
                }
                .create()

        dialog.setOnDismissListener {

            dialogoErroAberto =
                false
        }

        dialog.show()
    }

    private fun tentarReproduzirNovamente() {

        try {

            dialogoErroAberto =
                false

            playerPreparado =
                false

            ultimaPosicao =
                0L

            reproduzirAoRetornar =
                true

            inicializarPlayer()

        } catch (
            e: Exception
        ) {

            mostrarErroDiagnostico(
                "TENTAR REPRODUZIR NOVAMENTE",
                e
            )
        }
    }

    private fun copiarDiagnostico(
        texto: String
    ) {

        try {

            val clipboard =
                getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager

            val clip =
                ClipData.newPlainText(
                    "Diagnóstico do vídeo",
                    texto
                )

            clipboard.setPrimaryClip(
                clip
            )

            Toast.makeText(
                this,
                "Erro copiado. Agora pode colar aqui.",
                Toast.LENGTH_LONG
            ).show()

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Não foi possível copiar o erro.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================================================
    // ESTADO
    // =========================================================

    private fun restaurarEstado(
        savedInstanceState: Bundle?
    ) {

        if (
            savedInstanceState == null
        ) {
            return
        }

        val caminhoSalvo =
            savedInstanceState.getString(
                "caminho_video"
            )

        if (
            !caminhoSalvo.isNullOrBlank()
        ) {

            val indice =
                arquivos.indexOf(
                    caminhoSalvo
                )

            if (indice >= 0) {

                posicaoAtual =
                    indice

                atualizarArquivoAtual()
            }

        } else {

            val indice =
                savedInstanceState.getInt(
                    "indice_video",
                    posicaoAtual
                )

            if (
                indice >= 0 &&
                indice < arquivos.size
            ) {

                posicaoAtual =
                    indice

                atualizarArquivoAtual()
            }
        }

        ultimaPosicao =
            savedInstanceState.getLong(
                "posicao_video",
                0L
            )

        reproduzirAoRetornar =
            savedInstanceState.getBoolean(
                "reproduzindo",
                true
            )
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
            intent.getStringArrayListExtra(
                "arquivos"
            )

        if (
            listaRecebida != null &&
            listaRecebida.isNotEmpty()
        ) {

            arquivos.clear()

            listaRecebida
                .filter {
                    it.isNotBlank()
                }
                .forEach {
                    arquivos.add(it)
                }
        }

        val caminhoRecebido =
            intent.getStringExtra(
                "arquivo"
            )

        val posicaoRecebida =
            intent.getIntExtra(
                "posicao",
                -1
            )

        if (
            arquivos.isEmpty() &&
            !caminhoRecebido.isNullOrBlank()
        ) {

            arquivos.add(
                caminhoRecebido
            )
        }

        if (
            arquivos.isEmpty()
        ) {

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

                posicaoRecebida in
                        arquivos.indices ->

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

        if (
            arquivos.isEmpty()
        ) {

            finish()

            return
        }

        if (
            posicaoAtual < 0 ||
            posicaoAtual >= arquivos.size
        ) {

            posicaoAtual =
                0
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

                setBackgroundColor(
                    Color.BLACK
                )
            }

        playerView =
            GesturePlayerView(
                this
            ).apply {

                layoutParams =
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                setBackgroundColor(
                    Color.BLACK
                )

                useController =
                    true

                controllerShowTimeoutMs =
                    3000

                controllerHideOnTouch =
                    true

                keepScreenOn =
                    true
            }

        raiz.addView(
            playerView
        )

        criarBarraSuperior()

        criarBarraInferior()

        setContentView(
            raiz
        )

        playerView.bringToFront()

        barraSuperior.bringToFront()

        barraInferior.bringToFront()
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

                text =
                    "‹"

                textSize =
                    38f

                setTextColor(
                    Color.WHITE
                )

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

                textSize =
                    13f

                setTextColor(
                    Color.LTGRAY
                )

                gravity =
                    Gravity.CENTER_VERTICAL

                configurarTextoRolante(
                    this
                )
            }

        nomeArquivoText =
            TextView(this).apply {

                textSize =
                    15f

                setTextColor(
                    Color.WHITE
                )

                gravity =
                    Gravity.CENTER_VERTICAL

                configurarTextoRolante(
                    this
                )
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

                textSize =
                    14f

                setTextColor(
                    Color.WHITE
                )

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

                text =
                    "⋮"

                textSize =
                    30f

                setTextColor(
                    Color.WHITE
                )

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

        if (
            !::arquivoAtual.isInitialized
        ) {
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

        if (
            ::nomePastaText.isInitialized
        ) {

            configurarTextoRolante(
                nomePastaText
            )
        }

        if (
            ::nomeArquivoText.isInitialized
        ) {

            configurarTextoRolante(
                nomeArquivoText
            )
        }
    }

    private fun configurarTextoRolante(
        texto: TextView
    ) {

        texto.setSingleLine(
            true
        )

        texto.maxLines =
            1

        texto.ellipsize =
            TextUtils.TruncateAt.MARQUEE

        texto.marqueeRepeatLimit =
            -1

        texto.isSelected =
            true

        texto.isFocusable =
            true

        texto.isFocusableInTouchMode =
            true

        texto.setHorizontallyScrolling(
            true
        )
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

                text =
                    icone

                textSize =
                    22f

                setTextColor(
                    Color.WHITE
                )

                gravity =
                    Gravity.CENTER
            }

        val textoDescricao =
            TextView(this).apply {

                text =
                    descricao

                textSize =
                    9f

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
    // EXOPLAYER
    // =========================================================

    private fun inicializarPlayer() {

        try {

            if (
                !::arquivoAtual.isInitialized
            ) {
                return
            }

            if (
                !arquivoAtual.exists()
            ) {

                mostrarErroDiagnostico(
                    "VERIFICAÇÃO DO ARQUIVO",
                    IllegalStateException(
                        "O arquivo não existe:\n${arquivoAtual.absolutePath}"
                    )
                )

                return
            }

            if (
                !arquivoAtual.isFile
            ) {

                mostrarErroDiagnostico(
                    "VERIFICAÇÃO DO ARQUIVO",
                    IllegalStateException(
                        "O caminho não aponta para um arquivo:\n${arquivoAtual.absolutePath}"
                    )
                )

                return
            }

            if (
                !arquivoAtual.canRead()
            ) {

                mostrarErroDiagnostico(
                    "VERIFICAÇÃO DE PERMISSÃO",
                    SecurityException(
                        "O aplicativo não consegue ler este arquivo:\n${arquivoAtual.absolutePath}"
                    )
                )

                return
            }

            liberarPlayer()

            arquivosDoPlayer.clear()

            val renderersFactory =
                DefaultRenderersFactory(
                    this
                )
                    .setEnableDecoderFallback(
                        true
                    )

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

                        try {

                            when (
                                playbackState
                            ) {

                                Player.STATE_READY -> {

                                    playerPreparado =
                                        true

                                    if (
                                        ultimaPosicao > 0
                                    ) {

                                        try {

                                            novoPlayer.seekTo(
                                                ultimaPosicao
                                            )

                                        } catch (
                                            e: Exception
                                        ) {

                                            mostrarErroDiagnostico(
                                                "ERRO AO RESTAURAR POSIÇÃO",
                                                e
                                            )
                                        }

                                        ultimaPosicao =
                                            0L
                                    }

                                    if (
                                        reproduzirAoRetornar
                                    ) {

                                        try {
                                            novoPlayer.play()
                                        } catch (
                                            e: Exception
                                        ) {

                                            mostrarErroDiagnostico(
                                                "ERRO AO INICIAR PLAY",
                                                e
                                            )
                                        }
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

                                    playerPreparado =
                                        true

                                    reproduzirAoRetornar =
                                        false
                                }
                            }

                        } catch (
                            e: Exception
                        ) {

                            mostrarErroDiagnostico(
                                "ERRO NO ESTADO DO PLAYER",
                                e
                            )
                        }
                    }

                    override fun onMediaItemTransition(
                        mediaItem: MediaItem?,
                        reason: Int
                    ) {

                        try {

                            val indice =
                                novoPlayer.currentMediaItemIndex

                            if (
                                indice >= 0 &&
                                indice < arquivosDoPlayer.size
                            ) {

                                val caminho =
                                    arquivosDoPlayer[indice]

                                val indiceOriginal =
                                    arquivos.indexOf(
                                        caminho
                                    )

                                if (
                                    indiceOriginal >= 0
                                ) {

                                    ultimoIndicePlayer =
                                        indice

                                    posicaoAtual =
                                        indiceOriginal

                                    arquivoAtual =
                                        File(
                                            caminho
                                        )

                                    atualizarCabecalho()
                                }
                            }

                        } catch (
                            e: Exception
                        ) {

                            mostrarErroDiagnostico(
                                "ERRO AO TROCAR VÍDEO",
                                e
                            )
                        }
                    }

                    override fun onPlayerError(
                        error: PlaybackException
                    ) {

                        playerPreparado =
                            false

                        reproduzirAoRetornar =
                            false

                        mostrarErroMedia3(
                            error
                        )
                    }
                }
            )

            val listaMediaItems =
                ArrayList<MediaItem>()

            /*
             * Reprodução INTERNA:
             * usamos diretamente o arquivo.
             *
             * FileProvider fica somente para
             * compartilhar/abrir externamente.
             */
            arquivos.forEach { caminho ->

                try {

                    val arquivo =
                        File(caminho)

                    if (
                        arquivo.exists() &&
                        arquivo.isFile &&
                        arquivo.canRead()
                    ) {

                        val uri =
                            Uri.fromFile(
                                arquivo
                            )

                        val mediaItem =
                            MediaItem.Builder()
                                .setUri(uri)
                                .setMimeType(
                                    obterMimeType(
                                        arquivo
                                    )
                                )
                                .build()

                        listaMediaItems.add(
                            mediaItem
                        )

                        arquivosDoPlayer.add(
                            arquivo.absolutePath
                        )
                    }

                } catch (
                    e: Exception
                ) {

                    mostrarErroDiagnostico(
                        "ERRO AO PREPARAR ARQUIVO PARA O PLAYER",
                        e
                    )
                }
            }

            if (
                listaMediaItems.isEmpty()
            ) {

                mostrarErroDiagnostico(
                    "LISTA DE VÍDEOS",
                    IllegalStateException(
                        "Nenhum vídeo conseguiu ser preparado para reprodução."
                    )
                )

                return
            }

            var indiceInicial =
                arquivosDoPlayer.indexOf(
                    arquivoAtual.absolutePath
                )

            if (
                indiceInicial < 0 ||
                indiceInicial >= listaMediaItems.size
            ) {

                indiceInicial =
                    0
            }

            if (
                arquivosDoPlayer.isNotEmpty()
            ) {

                val caminhoInicial =
                    arquivosDoPlayer[
                        indiceInicial
                    ]

                val indiceOriginal =
                    arquivos.indexOf(
                        caminhoInicial
                    )

                if (
                    indiceOriginal >= 0
                ) {

                    posicaoAtual =
                        indiceOriginal

                    arquivoAtual =
                        File(
                            caminhoInicial
                        )

                    atualizarCabecalho()
                }
            }

            ultimoIndicePlayer =
                indiceInicial

            novoPlayer.setMediaItems(
                listaMediaItems,
                indiceInicial,
                C.TIME_UNSET
            )

            novoPlayer.prepare()

            novoPlayer.playWhenReady =
                reproduzirAoRetornar

        } catch (
            e: Throwable
        ) {

            tratarErroFatal(
                "ERRO AO INICIALIZAR EXOPLAYER",
                e
            )
        }
    }

    // =========================================================
    // ERRO MEDIA3
    // =========================================================

    private fun mostrarErroMedia3(
        error: PlaybackException
    ) {

        val detalhes =
            StringBuilder()

        detalhes.append(
            "CÓDIGO DO ERRO:\n"
        )

        detalhes.append(
            error.errorCode
        )

        detalhes.append(
            "\n\n"
        )

        detalhes.append(
            "NOME DO CÓDIGO:\n"
        )

        detalhes.append(
            nomeCodigoErro(
                error.errorCode
            )
        )

        detalhes.append(
            "\n\n"
        )

        detalhes.append(
            "MENSAGEM:\n"
        )

        detalhes.append(
            error.message
                ?: "Sem mensagem"
        )

        detalhes.append(
            "\n\n"
        )

        detalhes.append(
            "TIPO DA CAUSA:\n"
        )

        detalhes.append(
            error.cause?.javaClass?.name
                ?: "Nenhuma"
        )

        detalhes.append(
            "\n\n"
        )

        detalhes.append(
            "MENSAGEM DA CAUSA:\n"
        )

        detalhes.append(
            error.cause?.message
                ?: "Sem mensagem"
        )

        detalhes.append(
            "\n\n"
        )

        if (
            ::arquivoAtual.isInitialized
        ) {

            detalhes.append(
                "ARQUIVO:\n"
            )

            detalhes.append(
                arquivoAtual.absolutePath
            )

            detalhes.append(
                "\n\n"
            )

            detalhes.append(
                "EXISTE: "
            )

            detalhes.append(
                arquivoAtual.exists()
            )

            detalhes.append(
                "\n"
            )

            detalhes.append(
                "É ARQUIVO: "
            )

            detalhes.append(
                arquivoAtual.isFile
            )

            detalhes.append(
                "\n"
            )

            detalhes.append(
                "PODE LER: "
            )

            detalhes.append(
                arquivoAtual.canRead()
            )

            detalhes.append(
                "\n"
            )

            detalhes.append(
                "TAMANHO: "
            )

            detalhes.append(
                arquivoAtual.length()
            )

            detalhes.append(
                " bytes\n"
            )

            detalhes.append(
                "EXTENSÃO: "
            )

            detalhes.append(
                arquivoAtual.extension
            )

            detalhes.append(
                "\n"
            )

            detalhes.append(
                "MIME: "
            )

            detalhes.append(
                obterMimeType(
                    arquivoAtual
                )
            )

            detalhes.append(
                "\n\n"
            )
        }

        detalhes.append(
            "STACKTRACE:\n"
        )

        detalhes.append(
            error.stackTraceToString()
        )

        mostrarCaixaDiagnostico(
            "ERRO DO MEDIA3 / EXOPLAYER",
            detalhes.toString()
        )
    }

    /*
     * Não usamos constantes específicas do Media3
     * que podem variar entre versões.
     *
     * Assim evitamos:
     * ERROR_CODE_PARSING_UNSUPPORTED
     * ERROR_CODE_DECODER_UNSPECIFIED
     */
    private fun nomeCodigoErro(
        codigo: Int
    ): String {

        return "CÓDIGO MEDIA3: $codigo"
    }

    // =========================================================
    // LIBERAR PLAYER
    // =========================================================

    private fun liberarPlayer() {

        try {

            player?.let {

                try {
                    ultimaPosicao =
                        it.currentPosition
                } catch (_: Exception) {
                }

                try {
                    it.stop()
                } catch (_: Exception) {
                }

                try {
                    it.release()
                } catch (_: Exception) {
                }
            }

        } catch (_: Exception) {
        }

        player = null

        if (
            ::playerView.isInitialized
        ) {

            try {
                playerView.player =
                    null
            } catch (_: Exception) {
            }
        }

        playerPreparado =
            false
    }

    // =========================================================
    // GESTOS
    // =========================================================

    private fun configurarGestos() {

        playerView.onSwipeLeft = {
            abrirProximoVideo()
        }

        playerView.onSwipeRight = {
            abrirVideoAnterior()
        }
    }

    private fun abrirProximoVideo() {

        if (
            arquivos.size <= 1
        ) {
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

        trocarVideo(
            posicaoAtual + 1
        )
    }

    private fun abrirVideoAnterior() {

        if (
            arquivos.size <= 1
        ) {
            return
        }

        if (
            posicaoAtual <= 0
        ) {

            Toast.makeText(
                this,
                "Este é o primeiro vídeo",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        trocarVideo(
            posicaoAtual - 1
        )
    }

    private fun trocarVideo(
        novoIndice: Int
    ) {

        try {

            if (
                novoIndice < 0 ||
                novoIndice >= arquivos.size
            ) {
                return
            }

            val caminho =
                arquivos[novoIndice]

            val indicePlayer =
                arquivosDoPlayer.indexOf(
                    caminho
                )

            if (
                indicePlayer < 0
            ) {

                mostrarErroDiagnostico(
                    "TROCA DE VÍDEO",
                    IllegalStateException(
                        "O vídeo existe na lista principal, mas não está na lista do player.\n\nArquivo:\n$caminho"
                    )
                )

                return
            }

            posicaoAtual =
                novoIndice

            atualizarArquivoAtual()

            ultimaPosicao =
                0L

            reproduzirAoRetornar =
                true

            player?.let {

                it.seekTo(
                    indicePlayer,
                    0L
                )

                it.play()

                ultimoIndicePlayer =
                    indicePlayer

                atualizarCabecalho()

                return
            }

            inicializarPlayer()

            atualizarCabecalho()

        } catch (
            e: Exception
        ) {

            mostrarErroDiagnostico(
                "ERRO AO TROCAR VÍDEO",
                e
            )
        }
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

                elevation =
                    dp(8).toFloat()

                isOutsideTouchable =
                    true

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

                text =
                    texto

                textSize =
                    15f

                setTextColor(
                    Color.WHITE
                )

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

        } catch (
            e: Exception
        ) {

            mostrarErroDiagnostico(
                "COMPARTILHAMENTO",
                e
            )
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

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            startActivity(
                intent
            )

        } catch (
            _: ActivityNotFoundException
        ) {

            Toast.makeText(
                this,
                "Nenhum aplicativo pode abrir este vídeo",
                Toast.LENGTH_LONG
            ).show()

        } catch (
            e: Exception
        ) {

            mostrarErroDiagnostico(
                "ABRIR COM APLICATIVO EXTERNO",
                e
            )
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

        } catch (
            e: Exception
        ) {

            mostrarErroDiagnostico(
                "LEITURA DAS INFORMAÇÕES DO VÍDEO",
                e
            )

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

                "$largura × $altura"

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
            .setTitle(
                "Informações do vídeo"
            )
            .setMessage(
                mensagem
            )
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

    // =========================================================
    // RENOMEAR
    // =========================================================

    private fun renomearArquivo() {

        val campo =
            EditText(this).apply {

                setSingleLine(
                    true
                )

                setText(
                    arquivoAtual.name
                )

                setSelection(
                    text.length
                )

                hint =
                    "Nome do arquivo"
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
            .setTitle(
                "Renomear vídeo"
            )
            .setView(
                container
            )
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

                if (
                    novoNome.isBlank()
                ) {
                    return@setPositiveButton
                }

                val arquivoAntigo =
                    arquivoAtual

                val novoArquivo =
                    File(
                        arquivoAntigo.parentFile,
                        novoNome
                    )

                if (
                    novoArquivo.exists() &&
                    novoArquivo.absolutePath !=
                    arquivoAntigo.absolutePath
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
                        arquivoAntigo.renameTo(
                            novoArquivo
                        )
                    ) {

                        arquivos[
                            posicaoAtual
                        ] =
                            novoArquivo.absolutePath

                        val indicePlayer =
                            arquivosDoPlayer.indexOf(
                                arquivoAntigo.absolutePath
                            )

                        if (
                            indicePlayer >= 0
                        ) {

                            arquivosDoPlayer[
                                indicePlayer
                            ] =
                                novoArquivo.absolutePath
                        }

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

                } catch (
                    e: Exception
                ) {

                    mostrarErroDiagnostico(
                        "RENOMEAR ARQUIVO",
                        e
                    )
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

                if (
                    destino.exists()
                ) {

                    Toast.makeText(
                        this,
                        "Já existe um arquivo com esse nome",
                        Toast.LENGTH_LONG
                    ).show()

                    return@mostrarEscolhaPasta
                }

                arquivoAtual
                    .inputStream()
                    .use { entrada ->

                        destino
                            .outputStream()
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

            } catch (
                e: Exception
            ) {

                mostrarErroDiagnostico(
                    "COPIAR ARQUIVO",
                    e
                )
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

                if (
                    destino.exists()
                ) {

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

                    arquivosDoPlayer.clear()

                    if (
                        arquivos.isEmpty()
                    ) {

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

                    ultimaPosicao =
                        0L

                    inicializarPlayer()

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

            } catch (
                e: Exception
            ) {

                mostrarErroDiagnostico(
                    "MOVER ARQUIVO",
                    e
                )
            }
        }
    }

    // =========================================================
    // CRIAR PASTA
    // =========================================================

    private fun criarNovaPasta() {

        val campo =
            EditText(this).apply {

                setSingleLine(
                    true
                )

                hint =
                    "Nome da pasta"
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
            .setTitle(
                "Nova pasta"
            )
            .setView(
                container
            )
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

                if (
                    nome.isBlank()
                ) {
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

                } catch (
                    e: Exception
                ) {

                    mostrarErroDiagnostico(
                        "CRIAR PASTA",
                        e
                    )
                }
            }
            .show()
    }

    // =========================================================
    // LIXEIRA
    // =========================================================

    private fun enviarParaLixeira() {

        AlertDialog.Builder(this)
            .setTitle(
                "Enviar para lixeira?"
            )
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

                    if (
                        !pastaLixeira.exists()
                    ) {
                        pastaLixeira.mkdirs()
                    }

                    var destino =
                        File(
                            pastaLixeira,
                            arquivoAtual.name
                        )

                    var contador =
                        1

                    while (
                        destino.exists()
                    ) {

                        val nome =
                            arquivoAtual
                                .nameWithoutExtension

                        val extensao =
                            arquivoAtual.extension

                        destino =
                            File(
                                pastaLixeira,
                                if (
                                    extensao.isNotBlank()
                                ) {

                                    "${nome}_$contador.$extensao"

                                } else {

                                    "${nome}_$contador"
                                }
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

                } catch (
                    e: Exception
                ) {

                    mostrarErroDiagnostico(
                        "ENVIAR PARA LIXEIRA",
                        e
                    )
                }
            }
            .show()
    }

    // =========================================================
    // ESCOLHA DE PASTA
    // =========================================================

    private fun mostrarEscolhaPasta(
        titulo: String,
        aoSelecionar: (File) -> Unit
    ) {

        try {

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

            if (
                pastas.isEmpty()
            ) {

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
                .setTitle(
                    titulo
                )
                .setItems(
                    nomes
                ) { _, indice ->

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

        } catch (
            e: Exception
        ) {

            mostrarErroDiagnostico(
                "SELEÇÃO DE PASTA",
                e
            )
        }
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

        resultado.add(
            pasta
        )

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

        if (
            bytes <= 0
        ) {
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

        var indice =
            0

        while (
            tamanho >= 1024 &&
            indice < unidades.size - 1
        ) {

            tamanho /= 1024

            indice++
        }

        return DecimalFormat(
            "#,##0.##"
        ).format(
            tamanho
        ) +
                " " +
                unidades[indice]
    }

    private fun formatarDuracao(
        milissegundos: Long
    ): String {

        if (
            milissegundos <= 0
        ) {
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

        return if (
            horas > 0
        ) {

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
    // PLAYER VIEW / GESTOS
    // =========================================================

    @UnstableApi
    class GesturePlayerView(
        context: Context
    ) : PlayerView(context) {

        var onSwipeLeft:
                (() -> Unit)? = null

        var onSwipeRight:
                (() -> Unit)? = null

        private var toqueX =
            0f

        private var toqueY =
            0f

        private var movimentoDetectado =
            false

        private val distanciaMinima =
            120f

        override fun onTouchEvent(
            event: MotionEvent
        ): Boolean {

            try {

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
                            abs(distanciaX) >
                            distanciaMinima &&
                            abs(distanciaX) >
                            abs(distanciaY)
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
                            abs(distanciaX) >
                            distanciaMinima &&
                            abs(distanciaX) >
                            abs(distanciaY)
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

                            return true
                        }
                    }

                    MotionEvent.ACTION_CANCEL -> {

                        movimentoDetectado =
                            false
                    }
                }

            } catch (_: Exception) {
            }

            return try {

                super.onTouchEvent(
                    event
                )

            } catch (_: Exception) {

                true
            }
        }
    }
}
