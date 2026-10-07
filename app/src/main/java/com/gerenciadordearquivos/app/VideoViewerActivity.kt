package com.gerenciadordearquivos.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.abs


class VideoViewerActivity : Activity() {

    private lateinit var videoView: VideoView

    private lateinit var nomePastaText: TextView
    private lateinit var nomeArquivoText: TextView
    private lateinit var contadorText: TextView
    private lateinit var favoritoButton: TextView
    private lateinit var carregandoVideo: ProgressBar

    private val arquivos =
        ArrayList<File>()

    private var posicaoAtual = 0

    private var toqueInicialX = 0f
    private var toqueInicialY = 0f

    private val preferenciasNome =
        "favoritos_videos"

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        /*
         * =====================================================
         * ORIENTAÇÃO
         * =====================================================
         *
         * Não força retrato nem paisagem.
         *
         * O próprio usuário decide a posição do celular.
         */
        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        configurarTelaCheia()

        carregarListaRecebida()

        if (arquivos.isEmpty()) {

            val caminho =
                intent.getStringExtra(
                    "arquivo"
                )

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


    /*
     * =========================================================
     * TELA CHEIA
     * =========================================================
     */

    private fun configurarTelaCheia() {

        try {

            requestWindowFeature(
                Window.FEATURE_NO_TITLE
            )

        } catch (_: Exception) {
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }


    /*
     * =========================================================
     * LISTA RECEBIDA
     * =========================================================
     */

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


    /*
     * =========================================================
     * INTERFACE
     * =========================================================
     */

    private fun criarInterface() {

        val raiz =
            FrameLayout(this)

        raiz.setBackgroundColor(
            Color.BLACK
        )

        /*
         * =====================================================
         * VÍDEO
         * =====================================================
         */

        videoView =
            VideoView(this)

        val videoParametros =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

        videoParametros.gravity =
            Gravity.CENTER

        raiz.addView(
            videoView,
            videoParametros
        )

        /*
         * =====================================================
         * LOADING
         * =====================================================
         */

        carregandoVideo =
            ProgressBar(this)

        val progressoParametros =
            FrameLayout.LayoutParams(
                dp(64),
                dp(64)
            )

        progressoParametros.gravity =
            Gravity.CENTER

        raiz.addView(
            carregandoVideo,
            progressoParametros
        )

        carregandoVideo.visibility =
            View.GONE

        /*
         * =====================================================
         * BARRA SUPERIOR
         * =====================================================
         */

        criarBarraSuperior(
            raiz
        )

        /*
         * =====================================================
         * BARRA INFERIOR
         * =====================================================
         */

        criarBarraInferior(
            raiz
        )

        setContentView(
            raiz
        )

        /*
         * Swipe.
         */
        videoView.setOnTouchListener {
                _,
                evento ->

            tratarToque(
                evento
            )

            true
        }
    }


    /*
     * =========================================================
     * TOPO
     * =========================================================
     */

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
            dp(6),
            dp(4),
            dp(6),
            dp(4)
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
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(82)
            )

        parametros.gravity =
            Gravity.TOP

        raiz.addView(
            barra,
            parametros
        )

        /*
         * =====================================================
         * VOLTAR
         * =====================================================
         */

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

        voltar.setPadding(
            dp(4),
            0,
            dp(10),
            0
        )

        voltar.setOnClickListener {

            finish()
        }

        barra.addView(
            voltar,
            LinearLayout.LayoutParams(
                dp(48),
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        /*
         * =====================================================
         * BLOCO DE NOMES
         * =====================================================
         */

        val bloco =
            LinearLayout(this)

        bloco.orientation =
            LinearLayout.VERTICAL

        bloco.gravity =
            Gravity.CENTER_VERTICAL

        val blocoParametros =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )

        barra.addView(
            bloco,
            blocoParametros
        )

        /*
         * =====================================================
         * PASTA
         * =====================================================
         */

        nomePastaText =
            TextView(this)

        nomePastaText.textSize =
            12f

        nomePastaText.setTextColor(
            Color.LTGRAY
        )

        nomePastaText.gravity =
            Gravity.CENTER_VERTICAL

        nomePastaText.singleLine =
            true

        /*
         * NÃO corta o nome.
         *
         * Se for muito grande, ele desliza
         * horizontalmente.
         */
        nomePastaText.ellipsize =
            android.text.TextUtils.TruncateAt.MARQUEE

        nomePastaText.isSelected =
            true

        nomePastaText.marqueeRepeatLimit =
            -1

        nomePastaText.setHorizontallyScrolling(
            true
        )

        bloco.addView(
            nomePastaText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(28)
            )
        )

        /*
         * =====================================================
         * NOME DO ARQUIVO
         * =====================================================
         */

        nomeArquivoText =
            TextView(this)

        nomeArquivoText.textSize =
            14f

        nomeArquivoText.setTextColor(
            Color.WHITE
        )

        nomeArquivoText.gravity =
            Gravity.CENTER_VERTICAL

        nomeArquivoText.singleLine =
            true

        nomeArquivoText.ellipsize =
            android.text.TextUtils.TruncateAt.MARQUEE

        nomeArquivoText.isSelected =
            true

        nomeArquivoText.marqueeRepeatLimit =
            -1

        nomeArquivoText.setHorizontallyScrolling(
            true
        )

        bloco.addView(
            nomeArquivoText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(30)
            )
        )

        /*
         * =====================================================
         * CONTADOR
         * =====================================================
         */

        contadorText =
            TextView(this)

        contadorText.textSize =
            12f

        contadorText.setTextColor(
            Color.WHITE
        )

        contadorText.gravity =
            Gravity.CENTER

        barra.addView(
            contadorText,
            LinearLayout.LayoutParams(
                dp(55),
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        /*
         * =====================================================
         * FAVORITO
         * =====================================================
         */

        favoritoButton =
            TextView(this)

        favoritoButton.textSize =
            26f

        favoritoButton.setTextColor(
            Color.WHITE
        )

        favoritoButton.gravity =
            Gravity.CENTER

        favoritoButton.setPadding(
            dp(4),
            0,
            dp(4),
            0
        )

        favoritoButton.setOnClickListener {

            alternarFavorito()
        }

        barra.addView(
            favoritoButton,
            LinearLayout.LayoutParams(
                dp(48),
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        /*
         * =====================================================
         * MENU
         * =====================================================
         */

        val menu =
            TextView(this)

        menu.text =
            "⋮"

        menu.textSize =
            30f

        menu.setTextColor(
            Color.WHITE
        )

        menu.gravity =
            Gravity.CENTER

        menu.setPadding(
            dp(4),
            0,
            dp(4),
            0
        )

        menu.setOnClickListener {

            mostrarMenu()
        }

        barra.addView(
            menu,
            LinearLayout.LayoutParams(
                dp(42),
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )
    }


    /*
     * =========================================================
     * BARRA INFERIOR
     * =========================================================
     */

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
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(76)
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
            "⧉",
            "Copiar"
        ) {

            copiarVideo()
        }

        adicionarBotaoInferior(
            barra,
            "➜",
            "Mover"
        ) {

            moverVideo()
        }

        adicionarBotaoInferior(
            barra,
            "⋮",
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

        botao.setPadding(
            dp(4),
            0,
            dp(4),
            0
        )

        botao.setOnClickListener {

            acao()
        }

        barra.addView(
            botao,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        )
    }


    /*
     * =========================================================
     * CARREGAR VÍDEO
     * =========================================================
     */

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

            return
        }

        /*
         * =====================================================
         * PASTA
         * =====================================================
         */

        val pasta =
            arquivo.parentFile?.name
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Armazenamento interno"

        /*
         * Coloca o caminho relativo para ajudar
         * quando existem pastas com o mesmo nome.
         */
        val nomePastaCompleto =
            arquivo.parentFile?.let {

                obterCaminhoDaPasta(
                    it
                )

            } ?: "Armazenamento interno"

        nomePastaText.text =
            "📁 $nomePastaCompleto"

        nomePastaText.isSelected =
            true

        /*
         * =====================================================
         * NOME DO ARQUIVO
         * =====================================================
         */

        nomeArquivoText.text =
            arquivo.name

        nomeArquivoText.isSelected =
            true

        contadorText.text =
            "${posicaoAtual + 1} / ${arquivos.size}"

        atualizarIconeFavorito()

        carregandoVideo.visibility =
            View.VISIBLE

        try {

            videoView.stopPlayback()

            val uri =
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    arquivo
                )

            videoView.setVideoURI(
                uri
            )

            videoView.setOnPreparedListener { mediaPlayer ->

                carregandoVideo.visibility =
                    View.GONE

                /*
                 * O VideoView permanece preenchendo
                 * toda a área disponível.
                 *
                 * O próprio Android mantém a proporção
                 * do vídeo.
                 */
                mediaPlayer.isLooping =
                    false

                videoView.start()
            }

            videoView.setOnCompletionListener {

                /*
                 * Mantém os controles disponíveis.
                 */
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


    /*
     * =========================================================
     * CAMINHO DA PASTA
     * =========================================================
     */

    private fun obterCaminhoDaPasta(
        pasta: File
    ): String {

        val raiz =
            Environment.getExternalStorageDirectory()

        val caminhoRaiz =
            raiz.absolutePath

        val caminho =
            pasta.absolutePath

        return if (
            caminho.startsWith(
                caminhoRaiz
            )
        ) {

            val relativo =
                caminho.removePrefix(
                    caminhoRaiz
                ).trimStart(
                    File.separatorChar
                )

            if (
                relativo.isBlank()
            ) {

                "Armazenamento interno"

            } else {

                relativo
            }

        } else {

            pasta.name
        }
    }


    /*
     * =========================================================
     * SWIPE
     * =========================================================
     */

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
                    abs(deslocamentoX) > dp(100) &&
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


    /*
     * =========================================================
     * PRÓXIMO / ANTERIOR
     * =========================================================
     */

    private fun irParaProximoVideo() {

        if (
            arquivos.size <= 1
        ) {

            return
        }

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


    private fun irParaVideoAnterior() {

        if (
            arquivos.size <= 1
        ) {

            return
        }

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


    /*
     * =========================================================
     * FAVORITO
     * =========================================================
     */

    private fun alternarFavorito() {

        val arquivo =
            arquivos[posicaoAtual]

        val preferencias =
            getSharedPreferences(
                preferenciasNome,
                MODE_PRIVATE
            )

        val atual =
            preferencias.getBoolean(
                arquivo.absolutePath,
                false
            )

        preferencias.edit()
            .putBoolean(
                arquivo.absolutePath,
                !atual
            )
            .apply()

        atualizarIconeFavorito()

        Toast.makeText(
            this,
            if (!atual) {
                "Adicionado aos favoritos"
            } else {
                "Removido dos favoritos"
            },
            Toast.LENGTH_SHORT
        ).show()
    }


    private fun atualizarIconeFavorito() {

        if (
            !::favoritoButton.isInitialized
        ) {

            return
        }

        val arquivo =
            arquivos[posicaoAtual]

        val favorito =
            getSharedPreferences(
                preferenciasNome,
                MODE_PRIVATE
            ).getBoolean(
                arquivo.absolutePath,
                false
            )

        favoritoButton.text =
            if (favorito) {
                "★"
            } else {
                "☆"
            }

        favoritoButton.setTextColor(
            if (favorito) {
                Color.YELLOW
            } else {
                Color.WHITE
            }
        )
    }


    /*
     * =========================================================
     * COMPARTILHAR
     * =========================================================
     */

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
                Toast.LENGTH_LONG
            ).show()
        }
    }


    /*
     * =========================================================
     * ABRIR COM
     * =========================================================
     */

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


    /*
     * =========================================================
     * MENU
     * =========================================================
     */

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


    /*
     * =========================================================
     * INFORMAÇÕES
     * =========================================================
     */

    private fun mostrarInformacoes() {

        val arquivo =
            arquivos[posicaoAtual]

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

        val pasta =
            arquivo.parentFile?.absolutePath
                ?: "Armazenamento interno"

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


    /*
     * =========================================================
     * RENOMEAR
     * =========================================================
     */

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
                    novoNome.isEmpty()
                ) {

                    Toast.makeText(
                        this,
                        "Digite um nome válido",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                if (
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

                if (pai == null) {
                    return@setOnClickListener
                }

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

                val sucesso =
                    try {

                        arquivo.renameTo(
                            novoArquivo
                        )

                    } catch (
                        _: Exception
                    ) {

                        false
                    }

                if (sucesso) {

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


    /*
     * =========================================================
     * COPIAR
     * =========================================================
     */

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

                escolhido.startsWith(
                    "⬆"
                ) -> {

                    pastaAtual.parentFile?.let {

                        dialog.dismiss()

                        mostrarSeletorCopiar(
                            arquivo,
                            it
                        )
                    }
                }

                escolhido.startsWith(
                    "📋"
                ) -> {

                    dialog.dismiss()

                    executarCopia(
                        arquivo,
                        pastaAtual
                    )
                }

                escolhido.startsWith(
                    "＋"
                ) -> {

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
                        ) {
                            3
                        } else {
                            2
                        }

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

        if (
            arquivo.parentFile?.absolutePath ==
            destinoPasta.absolutePath
        ) {

            Toast.makeText(
                this,
                "O vídeo já está nessa pasta",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

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

            var numero = 1

            do {

                destino =
                    File(
                        destinoPasta,
                        if (extensao.isEmpty()) {
                            "${base}_copia_$numero"
                        } else {
                            "${base}_copia_$numero.$extensao"
                        }
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

                            saida.flush()
                        }
                    }

                    true

                } catch (
                    _: Exception
                ) {

                    try {
                        destino.delete()
                    } catch (_: Exception) {
                    }

                    false
                }

            runOnUiThread {

                Toast.makeText(
                    this,
                    if (sucesso) {
                        "Vídeo copiado com sucesso"
                    } else {
                        "Não foi possível copiar o vídeo"
                    },
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    /*
     * =========================================================
     * MOVER
     * =========================================================
     */

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

                escolhido.startsWith(
                    "⬆"
                ) -> {

                    pastaAtual.parentFile?.let {

                        dialog.dismiss()

                        mostrarSeletorMover(
                            arquivo,
                            it
                        )
                    }
                }

                escolhido.startsWith(
                    "＋"
                ) -> {

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
                        ) {
                            2
                        } else {
                            1
                        }

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

                executarMover(
                    arquivo,
                    destino
                )
            }
            .show()
    }


    private fun executarMover(
        arquivo: File,
        destinoPasta: File
    ) {

        val destino =
            File(
                destinoPasta,
                arquivo.name
            )

        if (
            destino.exists()
        ) {

            Toast.makeText(
                this,
                "Já existe um vídeo com esse nome no destino",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val sucesso =
            try {

                arquivo.renameTo(
                    destino
                )

            } catch (
                _: Exception
            ) {

                false
            }

        if (sucesso) {

            arquivos.removeAt(
                posicaoAtual
            )

            if (
                arquivos.isEmpty()
            ) {

                Toast.makeText(
                    this,
                    "Vídeo movido",
                    Toast.LENGTH_SHORT
                ).show()

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
                "Vídeo movido com sucesso",
                Toast.LENGTH_SHORT
            ).show()

        } else {

            Toast.makeText(
                this,
                "Não foi possível mover o vídeo",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    /*
     * =========================================================
     * CRIAR PASTA
     * =========================================================
     */

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
                ) {

                    return@setPositiveButton
                }

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


    /*
     * =========================================================
     * LIXEIRA
     * =========================================================
     */

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

                var numero = 1

                do {

                    destino =
                        File(
                            lixeira,
                            if (extensao.isEmpty()) {
                                "${base}_$numero"
                            } else {
                                "${base}_$numero.$extensao"
                            }
                        )

                    numero++

                } while (
                    destino.exists()
                )
            }

            val sucesso =
                arquivo.renameTo(
                    destino
                )

            if (sucesso) {

                arquivos.removeAt(
                    posicaoAtual
                )

                if (
                    arquivos.isEmpty()
                ) {

                    Toast.makeText(
                        this,
                        "Vídeo enviado para a lixeira",
                        Toast.LENGTH_SHORT
                    ).show()

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


    /*
     * =========================================================
     * PASTAS
     * =========================================================
     */

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


    /*
     * =========================================================
     * MIME
     * =========================================================
     */

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


    /*
     * =========================================================
     * TAMANHO
     * =========================================================
     */

    private fun formatarTamanho(
        tamanho: Long
    ): String {

        if (
            tamanho <= 0
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


    /*
     * =========================================================
     * DURAÇÃO
     * =========================================================
     */

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


    /*
     * =========================================================
     * PAUSA / RETORNO
     * =========================================================
     */

    override fun onPause() {

        if (
            ::videoView.isInitialized
        ) {

            try {

                if (
                    videoView.isPlaying
                ) {

                    videoView.pause()
                }

            } catch (_: Exception) {
            }
        }

        super.onPause()
    }


    override fun onResume() {

        super.onResume()

        configurarTelaCheia()

        if (
            ::videoView.isInitialized
        ) {

            try {

                if (
                    !videoView.isPlaying
                ) {

                    videoView.start()
                }

            } catch (_: Exception) {
            }
        }
    }


    /*
     * =========================================================
     * MANTER TELA CHEIA AO GIRAR
     * =========================================================
     */

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


    /*
     * =========================================================
     * DP
     * =========================================================
     */

    private fun dp(
        valor: Int
    ): Int {

        return (
            valor *
                resources.displayMetrics.density
            ).toInt()
    }
}
