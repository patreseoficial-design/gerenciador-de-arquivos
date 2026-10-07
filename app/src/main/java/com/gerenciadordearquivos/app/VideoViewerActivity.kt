package com.gerenciadordearquivos.app

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.abs

class VideoViewerActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView

    private lateinit var nomePastaText: TextView
    private lateinit var nomeArquivoText: TextView
    private lateinit var contadorText: TextView
    private lateinit var favoritoButton: TextView
    private lateinit var carregandoVideo: ProgressBar

    private val arquivos =
        ArrayList<File>()

    private var posicaoAtual = 0

    private var controlador: MediaController? = null

    private var toqueInicialX = 0f
    private var toqueInicialY = 0f

    private val preferenciasNome =
        "favoritos_videos"

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        // ========================================================
        // NÃO FORÇA NENHUMA ORIENTAÇÃO.
        // O USUÁRIO DECIDE GIRANDO O CELULAR.
        // ========================================================

        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        // ========================================================
        // TELA CHEIA
        // ========================================================

        ativarTelaCheia()

        carregarListaRecebida()

        if (arquivos.isEmpty()) {

            val caminho =
                intent.getStringExtra("arquivo")

            if (!caminho.isNullOrEmpty()) {

                val arquivo =
                    File(caminho)

                if (
                    arquivo.exists() &&
                    arquivo.isFile
                ) {

                    arquivos.add(
                        arquivo
                    )
                }
            }
        }

        if (arquivos.isEmpty()) {

            Toast.makeText(
                this,
                "Vídeo não encontrado",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        posicaoAtual =
            intent.getIntExtra(
                "posicao",
                0
            )

        if (
            posicaoAtual < 0 ||
            posicaoAtual >= arquivos.size
        ) {

            posicaoAtual = 0
        }

        criarInterface()

        carregarVideoAtual()
    }

    // ============================================================
    // TELA CHEIA / IMERSIVO
    // ============================================================

    private fun ativarTelaCheia() {

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    // ============================================================
    // QUANDO O CELULAR GIRA
    // ============================================================

    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {

        super.onWindowFocusChanged(
            hasFocus
        )

        if (hasFocus) {

            ativarTelaCheia()
        }
    }

    // ============================================================
    // RECEBER LISTA DE VÍDEOS
    // ============================================================

    private fun carregarListaRecebida() {

        val lista =
            intent.getStringArrayListExtra(
                "arquivos"
            )

        if (lista != null) {

            lista.forEach { caminho ->

                if (
                    caminho.isNotEmpty()
                ) {

                    val arquivo =
                        File(caminho)

                    if (
                        arquivo.exists() &&
                        arquivo.isFile
                    ) {

                        arquivos.add(
                            arquivo
                        )
                    }
                }
            }
        }
    }

    // ============================================================
    // INTERFACE
    // ============================================================

    private fun criarInterface() {

        val raiz =
            FrameLayout(this)

        raiz.setBackgroundColor(
            Color.BLACK
        )

        // ========================================================
        // VÍDEO
        // ========================================================

        videoView =
            VideoView(this)

        val parametrosVideo =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

        parametrosVideo.gravity =
            Gravity.CENTER

        raiz.addView(
            videoView,
            parametrosVideo
        )

        // ========================================================
        // LOADING
        // ========================================================

        carregandoVideo =
            ProgressBar(this)

        val parametrosProgresso =
            FrameLayout.LayoutParams(
                60.dp(),
                60.dp()
            )

        parametrosProgresso.gravity =
            Gravity.CENTER

        raiz.addView(
            carregandoVideo,
            parametrosProgresso
        )

        carregandoVideo.visibility =
            View.GONE

        // ========================================================
        // BARRA SUPERIOR
        // ========================================================

        criarBarraSuperior(
            raiz
        )

        // ========================================================
        // BARRA INFERIOR
        // ========================================================

        criarBarraInferior(
            raiz
        )

        setContentView(
            raiz
        )

        // ========================================================
        // SWIPE
        // ========================================================

        videoView.setOnTouchListener {
                _,
                evento ->

            tratarToque(
                evento
            )

            true
        }
    }

    // ============================================================
    // BARRA SUPERIOR
    // ============================================================

    private fun criarBarraSuperior(
        raiz: FrameLayout
    ) {

        val barra =
            LinearLayout(this)

        barra.orientation =
            LinearLayout.HORIZONTAL

        barra.gravity =
            Gravity.CENTER_VERTICAL

        barra.setPadding(
            8.dp(),
            4.dp(),
            8.dp(),
            4.dp()
        )

        barra.setBackgroundColor(
            Color.argb(
                190,
                0,
                0,
                0
            )
        )

        val parametros =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                64.dp()
            )

        parametros.gravity =
            Gravity.TOP

        raiz.addView(
            barra,
            parametros
        )

        // ========================================================
        // VOLTAR
        // ========================================================

        val voltar =
            TextView(this)

        voltar.text =
            "‹"

        voltar.textSize =
            38f

        voltar.setTextColor(
            Color.WHITE
        )

        voltar.gravity =
            Gravity.CENTER

        voltar.setOnClickListener {

            finish()
        }

        barra.addView(
            voltar,
            LinearLayout.LayoutParams(
                50.dp(),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // ========================================================
        // BLOCO DOS NOMES
        // ========================================================

        val bloco =
            LinearLayout(this)

        bloco.orientation =
            LinearLayout.VERTICAL

        bloco.gravity =
            Gravity.CENTER_VERTICAL

        barra.addView(
            bloco,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        nomePastaText =
            TextView(this)

        nomePastaText.textSize =
            11f

        nomePastaText.setTextColor(
            Color.LTGRAY
        )

        nomePastaText.maxLines =
            1

        nomePastaText.ellipsize =
            android.text.TextUtils.TruncateAt.MIDDLE

        bloco.addView(
            nomePastaText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                21.dp()
            )
        )

        nomeArquivoText =
            TextView(this)

        nomeArquivoText.textSize =
            14f

        nomeArquivoText.setTextColor(
            Color.WHITE
        )

        nomeArquivoText.maxLines =
            1

        nomeArquivoText.ellipsize =
            android.text.TextUtils.TruncateAt.MIDDLE

        bloco.addView(
            nomeArquivoText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                27.dp()
            )
        )

        // ========================================================
        // CONTADOR
        // ========================================================

        contadorText =
            TextView(this)

        contadorText.textSize =
            13f

        contadorText.setTextColor(
            Color.WHITE
        )

        contadorText.gravity =
            Gravity.CENTER

        barra.addView(
            contadorText,
            LinearLayout.LayoutParams(
                55.dp(),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // ========================================================
        // FAVORITO
        // ========================================================

        favoritoButton =
            TextView(this)

        favoritoButton.textSize =
            25f

        favoritoButton.gravity =
            Gravity.CENTER

        favoritoButton.setTextColor(
            Color.WHITE
        )

        favoritoButton.setOnClickListener {

            alternarFavorito()
        }

        barra.addView(
            favoritoButton,
            LinearLayout.LayoutParams(
                48.dp(),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // ========================================================
        // MENU
        // ========================================================

        val menu =
            TextView(this)

        menu.text =
            "⋮"

        menu.textSize =
            30f

        menu.gravity =
            Gravity.CENTER

        menu.setTextColor(
            Color.WHITE
        )

        menu.setOnClickListener {

            mostrarMenu()
        }

        barra.addView(
            menu,
            LinearLayout.LayoutParams(
                42.dp(),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    // ============================================================
    // BARRA INFERIOR
    // ============================================================

    private fun criarBarraInferior(
        raiz: FrameLayout
    ) {

        val barra =
            LinearLayout(this)

        barra.orientation =
            LinearLayout.HORIZONTAL

        barra.gravity =
            Gravity.CENTER

        barra.setBackgroundColor(
            Color.argb(
                215,
                0,
                0,
                0
            )
        )

        val parametros =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                66.dp()
            )

        parametros.gravity =
            Gravity.BOTTOM

        raiz.addView(
            barra,
            parametros
        )

        adicionarBotaoInferior(
            barra,
            "↗",
            "Compartilhar"
        ) {

            compartilharVideo()
        }

        adicionarBotaoInferior(
            barra,
            "▣",
            "Copiar"
        ) {

            copiarVideo()
        }

        adicionarBotaoInferior(
            barra,
            "⇆",
            "Mover"
        ) {

            moverVideo()
        }

        adicionarBotaoInferior(
            barra,
            "⋯",
            "Mais"
        ) {

            mostrarMenu()
        }
    }

    private fun adicionarBotaoInferior(
        barra: LinearLayout,
        icone: String,
        descricao: String,
        acao: () -> Unit
    ) {

        val botao =
            TextView(this)

        botao.text =
            "$icone\n$descricao"

        botao.textSize =
            11f

        botao.setTextColor(
            Color.WHITE
        )

        botao.gravity =
            Gravity.CENTER

        botao.setOnClickListener {

            acao()
        }

        barra.addView(
            botao,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        )
    }

    // ============================================================
    // CARREGAR VÍDEO
    // ============================================================

    private fun carregarVideoAtual() {

        if (
            posicaoAtual < 0 ||
            posicaoAtual >= arquivos.size
        ) {

            return
        }

        val arquivo =
            arquivos[posicaoAtual]

        if (
            !arquivo.exists() ||
            !arquivo.isFile
        ) {

            Toast.makeText(
                this,
                "Vídeo não encontrado",
                Toast.LENGTH_SHORT
            ).show()

            irParaProximoVideo()

            return
        }

        val pasta =
            arquivo.parentFile?.name
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Armazenamento"

        nomePastaText.text =
            "📁 $pasta"

        nomeArquivoText.text =
            arquivo.name

        contadorText.text =
            "${posicaoAtual + 1} / ${arquivos.size}"

        atualizarIconeFavorito()

        carregandoVideo.visibility =
            View.VISIBLE

        try {

            controlador?.hide()

            controlador =
                MediaController(this)

            controlador?.setAnchorView(
                videoView
            )

            videoView.setMediaController(
                controlador
            )

            val uri =
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    arquivo
                )

            videoView.stopPlayback()

            videoView.setVideoURI(
                uri
            )

            videoView.setOnPreparedListener {

                carregandoVideo.visibility =
                    View.GONE

                videoView.start()

                controlador?.show(
                    3000
                )
            }

            videoView.setOnCompletionListener {

                controlador?.show()
            }

            videoView.setOnErrorListener {
                    _,
                    _,
                    _ ->

                carregandoVideo.visibility =
                    View.GONE

                Toast.makeText(
                    this,
                    "Este formato ou codec de vídeo não é compatível",
                    Toast.LENGTH_LONG
                ).show()

                true
            }

        } catch (
            _: Exception
        ) {

            carregandoVideo.visibility =
                View.GONE

            Toast.makeText(
                this,
                "Não foi possível reproduzir o vídeo",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ============================================================
    // SWIPE
    // ============================================================

    private fun tratarToque(
        evento: MotionEvent
    ) {

        when (
            evento.actionMasked
        ) {

            MotionEvent.ACTION_DOWN -> {

                toqueInicialX =
                    evento.x

                toqueInicialY =
                    evento.y
            }

            MotionEvent.ACTION_UP -> {

                val deslocamentoX =
                    evento.x -
                            toqueInicialX

                val deslocamentoY =
                    evento.y -
                            toqueInicialY

                if (
                    abs(deslocamentoX) > 120 &&
                    abs(deslocamentoX) >
                    abs(deslocamentoY) * 1.3f
                ) {

                    if (
                        deslocamentoX < 0
                    ) {

                        irParaProximoVideo()

                    } else {

                        irParaVideoAnterior()
                    }
                }
            }
        }
    }

    // ============================================================
    // PRÓXIMO
    // ============================================================

    private fun irParaProximoVideo() {

        if (
            posicaoAtual <
            arquivos.size - 1
        ) {

            posicaoAtual++

            carregarVideoAtual()

        } else {

            Toast.makeText(
                this,
                "Este é o último vídeo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================================
    // ANTERIOR
    // ============================================================

    private fun irParaVideoAnterior() {

        if (
            posicaoAtual > 0
        ) {

            posicaoAtual--

            carregarVideoAtual()

        } else {

            Toast.makeText(
                this,
                "Este é o primeiro vídeo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================================
    // FAVORITO
    // ============================================================

    private fun alternarFavorito() {

        val arquivo =
            arquivos[posicaoAtual]

        val preferencias =
            getSharedPreferences(
                preferenciasNome,
                MODE_PRIVATE
            )

        val favorito =
            preferencias.getBoolean(
                arquivo.absolutePath,
                false
            )

        preferencias.edit()
            .putBoolean(
                arquivo.absolutePath,
                !favorito
            )
            .apply()

        atualizarIconeFavorito()
    }

    private fun atualizarIconeFavorito() {

        if (
            posicaoAtual < 0 ||
            posicaoAtual >= arquivos.size
        ) {

            return
        }

        val arquivo =
            arquivos[posicaoAtual]

        val preferencias =
            getSharedPreferences(
                preferenciasNome,
                MODE_PRIVATE
            )

        val favorito =
            preferencias.getBoolean(
                arquivo.absolutePath,
                false
            )

        favoritoButton.text =
            if (favorito) {
                "★"
            } else {
                "☆"
            }
    }

    // ============================================================
    // COMPARTILHAR
    // ============================================================

    private fun compartilharVideo() {

        val arquivo =
            arquivos[posicaoAtual]

        try {

            val uri =
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    arquivo
                )

            val intent =
                Intent(
                    Intent.ACTION_SEND
                )

            intent.type =
                obterMimeType(
                    arquivo
                )

            intent.putExtra(
                Intent.EXTRA_STREAM,
                uri
            )

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            startActivity(
                Intent.createChooser(
                    intent,
                    "Compartilhar vídeo"
                )
            )

        } catch (
            _: Exception
        ) {

            Toast.makeText(
                this,
                "Não foi possível compartilhar",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================================
    // ABRIR COM
    // ============================================================

    private fun abrirComAplicativo() {

        val arquivo =
            arquivos[posicaoAtual]

        try {

            val uri =
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    arquivo
                )

            val intent =
                Intent(
                    Intent.ACTION_VIEW
                )

            intent.setDataAndType(
                uri,
                obterMimeType(
                    arquivo
                )
            )

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            startActivity(
                Intent.createChooser(
                    intent,
                    "Abrir vídeo com"
                )
            )

        } catch (
            _: Exception
        ) {

            Toast.makeText(
                this,
                "Nenhum aplicativo pode abrir este vídeo",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ============================================================
    // MENU
    // ============================================================

    private fun mostrarMenu() {

        val arquivo =
            arquivos[posicaoAtual]

        val opcoes =
            arrayOf(
                "Abrir com outro aplicativo",
                "Informações",
                "Renomear",
                "Mover",
                "Copiar",
                "Compartilhar",
                "Enviar para lixeira"
            )

        AlertDialog.Builder(this)
            .setTitle(
                arquivo.name
            )
            .setItems(
                opcoes
            ) { _, escolha ->

                when (escolha) {

                    0 ->
                        abrirComAplicativo()

                    1 ->
                        mostrarInformacoes()

                    2 ->
                        renomearVideo()

                    3 ->
                        moverVideo()

                    4 ->
                        copiarVideo()

                    5 ->
                        compartilharVideo()

                    6 ->
                        enviarParaLixeira()
                }
            }
            .show()
    }

    // ============================================================
    // INFORMAÇÕES
    // ============================================================

    private fun mostrarInformacoes() {

        val arquivo =
            arquivos[posicaoAtual]

        val pasta =
            arquivo.parentFile?.name
                ?: "Armazenamento"

        val tamanho =
            formatarTamanho(
                arquivo.length()
            )

        val data =
            SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
            ).format(
                Date(
                    arquivo.lastModified()
                )
            )

        var duracao =
            "Não disponível"

        var resolucao =
            "Não disponível"

        try {

            val retriever =
                MediaMetadataRetriever()

            retriever.setDataSource(
                this,
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    arquivo
                )
            )

            val duracaoMs =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull()

            if (
                duracaoMs != null
            ) {

                duracao =
                    formatarDuracao(
                        duracaoMs
                    )
            }

            val largura =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                )

            val altura =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                )

            if (
                !largura.isNullOrEmpty() &&
                !altura.isNullOrEmpty()
            ) {

                resolucao =
                    "$largura x $altura"
            }

            retriever.release()

        } catch (
            _: Exception
        ) {
        }

        val mensagem =
            """
            Pasta:
            $pasta
            
            Nome:
            ${arquivo.name}
            
            Local:
            ${arquivo.parent ?: arquivo.absolutePath}
            
            Tamanho:
            $tamanho
            
            Resolução:
            $resolucao
            
            Duração:
            $duracao
            
            Formato:
            ${arquivo.extension.uppercase(
                Locale.getDefault()
            )}
            
            Modificado:
            $data
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

    // ============================================================
    // RENOMEAR
    // ============================================================

    private fun renomearVideo() {

        val arquivo =
            arquivos[posicaoAtual]

        val campo =
            EditText(this)

        campo.setText(
            arquivo.name
        )

        campo.selectAll()

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Renomear vídeo"
                )
                .setView(
                    campo
                )
                .setNegativeButton(
                    "Cancelar",
                    null
                )
                .setPositiveButton(
                    "Renomear",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val novoNome =
                    campo.text
                        .toString()
                        .trim()

                if (
                    novoNome.isEmpty() ||
                    novoNome.contains("/")
                ) {

                    Toast.makeText(
                        this,
                        "Nome inválido",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                val pai =
                    arquivo.parentFile
                        ?: return@setOnClickListener

                val novoArquivo =
                    File(
                        pai,
                        novoNome
                    )

                if (
                    novoArquivo.exists()
                ) {

                    Toast.makeText(
                        this,
                        "Já existe um arquivo com esse nome",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                if (
                    arquivo.renameTo(
                        novoArquivo
                    )
                ) {

                    arquivos[posicaoAtual] =
                        novoArquivo

                    dialog.dismiss()

                    carregarVideoAtual()

                    Toast.makeText(
                        this,
                        "Vídeo renomeado",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this,
                        "Não foi possível renomear",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        dialog.show()
    }

    // ============================================================
    // COPIAR
    // ============================================================

    private fun copiarVideo() {

        val arquivo =
            arquivos[posicaoAtual]

        val inicial =
            arquivo.parentFile
                ?: Environment.getExternalStorageDirectory()

        mostrarSeletorCopiar(
            arquivo,
            inicial
        )
    }

    private fun mostrarSeletorCopiar(
        arquivo: File,
        pastaAtual: File
    ) {

        val raiz =
            Environment.getExternalStorageDirectory()

        val pastas =
            listarPastas(
                pastaAtual
            )

        val nomes =
            ArrayList<String>()

        if (
            pastaAtual.absolutePath !=
            raiz.absolutePath
        ) {

            nomes.add(
                "⬆  .."
            )
        }

        nomes.add(
            "📋  Copiar aqui"
        )

        nomes.add(
            "＋  Criar pasta aqui"
        )

        pastas.forEach {

            nomes.add(
                "📁  ${it.name}"
            )
        }

        val lista =
            ListView(this)

        lista.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                nomes
            )

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Copiar para"
                )
                .setView(
                    lista
                )
                .setNegativeButton(
                    "Cancelar",
                    null
                )
                .create()

        lista.setOnItemClickListener {
                _,
                _,
                position,
                _ ->

            val escolhido =
                nomes[position]

            when {

                escolhido.startsWith("⬆") -> {

                    pastaAtual.parentFile?.let {

                        dialog.dismiss()

                        mostrarSeletorCopiar(
                            arquivo,
                            it
                        )
                    }
                }

                escolhido.startsWith("📋") -> {

                    dialog.dismiss()

                    executarCopia(
                        arquivo,
                        pastaAtual
                    )
                }

                escolhido.startsWith("＋") -> {

                    dialog.dismiss()

                    criarPasta(
                        pastaAtual
                    )
                }

                else -> {

                    val deslocamento =
                        if (
                            pastaAtual.absolutePath !=
                            raiz.absolutePath
                        ) 3 else 2

                    val indice =
                        position -
                                deslocamento

                    if (
                        indice >= 0 &&
                        indice < pastas.size
                    ) {

                        dialog.dismiss()

                        executarCopia(
                            arquivo,
                            pastas[indice]
                        )
                    }
                }
            }
        }

        dialog.show()
    }

    private fun executarCopia(
        arquivo: File,
        destinoPasta: File
    ) {

        var destino =
            File(
                destinoPasta,
                arquivo.name
            )

        if (
            destino.exists()
        ) {

            val base =
                arquivo.nameWithoutExtension

            val extensao =
                arquivo.extension

            var numero =
                1

            do {

                val nome =
                    if (
                        extensao.isEmpty()
                    ) {

                        "${base}_copia_$numero"

                    } else {

                        "${base}_copia_$numero.$extensao"
                    }

                destino =
                    File(
                        destinoPasta,
                        nome
                    )

                numero++

            } while (
                destino.exists()
            )
        }

        thread {

            val sucesso =
                try {

                    FileInputStream(
                        arquivo
                    ).use { entrada ->

                        FileOutputStream(
                            destino
                        ).use { saida ->

                            val buffer =
                                ByteArray(
                                    1024 * 1024
                                )

                            var lidos =
                                entrada.read(
                                    buffer
                                )

                            while (
                                lidos != -1
                            ) {

                                saida.write(
                                    buffer,
                                    0,
                                    lidos
                                )

                                lidos =
                                    entrada.read(
                                        buffer
                                    )
                            }
                        }
                    }

                    true

                } catch (
                    _: Exception
                ) {

                    false
                }

            runOnUiThread {

                Toast.makeText(
                    this,
                    if (sucesso)
                        "Vídeo copiado com sucesso"
                    else
                        "Não foi possível copiar",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ============================================================
    // MOVER
    // ============================================================

    private fun moverVideo() {

        val arquivo =
            arquivos[posicaoAtual]

        val inicial =
            arquivo.parentFile
                ?: Environment.getExternalStorageDirectory()

        mostrarSeletorMover(
            arquivo,
            inicial
        )
    }

    private fun mostrarSeletorMover(
        arquivo: File,
        pastaAtual: File
    ) {

        val raiz =
            Environment.getExternalStorageDirectory()

        val pastas =
            listarPastas(
                pastaAtual
            )

        val nomes =
            ArrayList<String>()

        if (
            pastaAtual.absolutePath !=
            raiz.absolutePath
        ) {

            nomes.add(
                "⬆  .."
            )
        }

        nomes.add(
            "＋  Criar pasta aqui"
        )

        pastas.forEach {

            nomes.add(
                "📁  ${it.name}"
            )
        }

        val lista =
            ListView(this)

        lista.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                nomes
            )

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Mover para"
                )
                .setView(
                    lista
                )
                .setNegativeButton(
                    "Cancelar",
                    null
                )
                .create()

        lista.setOnItemClickListener {
                _,
                _,
                position,
                _ ->

            val escolhido =
                nomes[position]

            when {

                escolhido.startsWith("⬆") -> {

                    pastaAtual.parentFile?.let {

                        dialog.dismiss()

                        mostrarSeletorMover(
                            arquivo,
                            it
                        )
                    }
                }

                escolhido.startsWith("＋") -> {

                    dialog.dismiss()

                    criarPasta(
                        pastaAtual
                    )
                }

                else -> {

                    val deslocamento =
                        if (
                            pastaAtual.absolutePath !=
                            raiz.absolutePath
                        ) 2 else 1

                    val indice =
                        position -
                                deslocamento

                    if (
                        indice >= 0 &&
                        indice < pastas.size
                    ) {

                        dialog.dismiss()

                        confirmarMover(
                            arquivo,
                            pastas[indice]
                        )
                    }
                }
            }
        }

        dialog.show()
    }

    private fun confirmarMover(
        arquivo: File,
        destino: File
    ) {

        if (
            arquivo.parentFile?.absolutePath ==
            destino.absolutePath
        ) {

            Toast.makeText(
                this,
                "O vídeo já está nessa pasta",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        AlertDialog.Builder(this)
            .setTitle(
                "Mover vídeo"
            )
            .setMessage(
                "Mover \"${arquivo.name}\" para \"${destino.name}\"?"
            )
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Mover"
            ) { _, _ ->

                val destinoArquivo =
                    File(
                        destino,
                        arquivo.name
                    )

                if (
                    destinoArquivo.exists()
                ) {

                    Toast.makeText(
                        this,
                        "Já existe um vídeo com esse nome",
                        Toast.LENGTH_LONG
                    ).show()

                    return@setPositiveButton
                }

                if (
                    arquivo.renameTo(
                        destinoArquivo
                    )
                ) {

                    arquivos.removeAt(
                        posicaoAtual
                    )

                    if (
                        arquivos.isEmpty()
                    ) {

                        finish()

                    } else {

                        if (
                            posicaoAtual >=
                            arquivos.size
                        ) {

                            posicaoAtual =
                                arquivos.size - 1
                        }

                        carregarVideoAtual()
                    }

                } else {

                    Toast.makeText(
                        this,
                        "Não foi possível mover",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .show()
    }

    // ============================================================
    // CRIAR PASTA
    // ============================================================

    private fun criarPasta(
        local: File
    ) {

        val campo =
            EditText(this)

        campo.hint =
            "Nome da pasta"

        AlertDialog.Builder(this)
            .setTitle(
                "Criar pasta"
            )
            .setView(
                campo
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
                    nome.isEmpty()
                ) return@setPositiveButton

                val pasta =
                    File(
                        local,
                        nome
                    )

                if (
                    pasta.mkdirs()
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
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    // ============================================================
    // LIXEIRA
    // ============================================================

    private fun enviarParaLixeira() {

        val arquivo =
            arquivos[posicaoAtual]

        AlertDialog.Builder(this)
            .setTitle(
                "Mover para lixeira?"
            )
            .setMessage(
                arquivo.name
            )
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Mover"
            ) { _, _ ->

                executarLixeira(
                    arquivo
                )
            }
            .show()
    }

    private fun executarLixeira(
        arquivo: File
    ) {

        val raiz =
            Environment.getExternalStorageDirectory()

        val lixeira =
            File(
                raiz,
                ".GerenciadorArquivos/.Lixeira"
            )

        try {

            if (
                !lixeira.exists()
            ) {

                lixeira.mkdirs()
            }

            var destino =
                File(
                    lixeira,
                    arquivo.name
                )

            if (
                destino.exists()
            ) {

                val base =
                    arquivo.nameWithoutExtension

                val extensao =
                    arquivo.extension

                var numero =
                    1

                do {

                    val novoNome =
                        if (
                            extensao.isEmpty()
                        ) {

                            "${base}_$numero"

                        } else {

                            "${base}_$numero.$extensao"
                        }

                    destino =
                        File(
                            lixeira,
                            novoNome
                        )

                    numero++

                } while (
                    destino.exists()
                )
            }

            if (
                arquivo.renameTo(
                    destino
                )
            ) {

                arquivos.removeAt(
                    posicaoAtual
                )

                if (
                    arquivos.isEmpty()
                ) {

                    finish()

                    return
                }

                if (
                    posicaoAtual >=
                    arquivos.size
                ) {

                    posicaoAtual =
                        arquivos.size - 1
                }

                carregarVideoAtual()

                Toast.makeText(
                    this,
                    "Vídeo enviado para a lixeira",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "Não foi possível mover para a lixeira",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "Erro: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ============================================================
    // LISTAR PASTAS
    // ============================================================

    private fun listarPastas(
        pasta: File
    ): List<File> {

        return try {

            pasta.listFiles()
                ?.filter {
                    it.isDirectory &&
                            !it.name.startsWith(".")
                }
                ?.sortedBy {
                    it.name.lowercase(
                        Locale.getDefault()
                    )
                }
                ?: emptyList()

        } catch (
            _: Exception
        ) {

            emptyList()
        }
    }

    // ============================================================
    // MIME
    // ============================================================

    private fun obterMimeType(
        arquivo: File
    ): String {

        return when (
            arquivo.extension.lowercase(
                Locale.getDefault()
            )
        ) {

            "mp4",
            "m4v" ->
                "video/mp4"

            "mkv" ->
                "video/x-matroska"

            "avi" ->
                "video/x-msvideo"

            "mov" ->
                "video/quicktime"

            "3gp" ->
                "video/3gpp"

            "webm" ->
                "video/webm"

            "ts" ->
                "video/mp2t"

            else ->
                "video/*"
        }
    }

    // ============================================================
    // TAMANHO
    // ============================================================

    private fun formatarTamanho(
        tamanho: Long
    ): String {

        if (
            tamanho <= 0
        ) return "0 B"

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

        var indice =
            0

        while (
            valor >= 1024 &&
            indice <
            unidades.size - 1
        ) {

            valor /= 1024

            indice++
        }

        return if (
            indice == 0
        ) {

            "${valor.toLong()} ${unidades[indice]}"

        } else {

            String.format(
                Locale.getDefault(),
                "%.1f %s",
                valor,
                unidades[indice]
            )
        }
    }

    // ============================================================
    // DURAÇÃO
    // ============================================================

    private fun formatarDuracao(
        milissegundos: Long
    ): String {

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

    // ============================================================
    // PAUSE
    // ============================================================

    override fun onPause() {

        if (
            ::videoView.isInitialized
        ) {

            try {
                videoView.pause()
            } catch (
                _: Exception
            ) {
            }
        }

        super.onPause()
    }

    // ============================================================
    // RESUME
    // ============================================================

    override fun onResume() {

        super.onResume()

        ativarTelaCheia()

        if (
            ::videoView.isInitialized
        ) {

            try {

                if (
                    !videoView.isPlaying
                ) {

                    videoView.start()
                }

            } catch (
                _: Exception
            ) {
            }
        }
    }

    // ============================================================
    // DP
    // ============================================================

    private fun Int.dp(): Int {

        return (
            this *
                    resources.displayMetrics.density
            ).toInt()
    }
}
