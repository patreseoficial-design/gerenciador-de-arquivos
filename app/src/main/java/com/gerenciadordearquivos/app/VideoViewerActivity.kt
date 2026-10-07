package com.gerenciadordearquivos.app

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class VideoViewerActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView

    private lateinit var contadorText: TextView
    private lateinit var nomeArquivoText: TextView
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

        carregarListaRecebida()

        if (arquivos.isEmpty()) {

            val caminho =
                intent.getStringExtra("arquivo")

            if (!caminho.isNullOrEmpty()) {
                arquivos.add(
                    File(caminho)
                )
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

    private fun carregarListaRecebida() {

        val lista =
            intent.getStringArrayListExtra(
                "arquivos"
            )

        if (lista != null) {

            lista.forEach { caminho ->

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

    private fun criarInterface() {

        val raiz =
            FrameLayout(this)

        raiz.setBackgroundColor(
            Color.BLACK
        )

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

        carregandoVideo =
            ProgressBar(this)

        val progressoParametros =
            FrameLayout.LayoutParams(
                70,
                70
            )

        progressoParametros.gravity =
            Gravity.CENTER

        raiz.addView(
            carregandoVideo,
            progressoParametros
        )

        carregandoVideo.visibility =
            View.GONE

        criarBarraSuperior(
            raiz
        )

        criarBarraInferior(
            raiz
        )

        setContentView(
            raiz
        )

        videoView.setOnTouchListener {
                _,
                evento ->

            tratarToque(
                evento
            )

            true
        }
    }

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
            12,
            10,
            12,
            10
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
                64
            )

        parametros.gravity =
            Gravity.TOP

        raiz.addView(
            barra,
            parametros
        )

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
            8,
            0,
            18,
            0
        )

        voltar.setOnClickListener {

            finish()
        }

        barra.addView(
            voltar,
            LinearLayout.LayoutParams(
                55,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        nomeArquivoText =
            TextView(this)

        nomeArquivoText.textSize =
            15f

        nomeArquivoText.setTextColor(
            Color.WHITE
        )

        nomeArquivoText.maxLines =
            1

        nomeArquivoText.ellipsize =
            android.text.TextUtils.TruncateAt.MIDDLE

        val nomeParametros =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )

        nomeParametros.gravity =
            Gravity.CENTER_VERTICAL

        barra.addView(
            nomeArquivoText,
            nomeParametros
        )

        contadorText =
            TextView(this)

        contadorText.textSize =
            14f

        contadorText.setTextColor(
            Color.WHITE
        )

        contadorText.gravity =
            Gravity.CENTER

        barra.addView(
            contadorText,
            LinearLayout.LayoutParams(
                55,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        favoritoButton =
            TextView(this)

        favoritoButton.textSize =
            25f

        favoritoButton.gravity =
            Gravity.CENTER

        favoritoButton.setTextColor(
            Color.WHITE
        )

        favoritoButton.setPadding(
            8,
            0,
            8,
            0
        )

        favoritoButton.setOnClickListener {

            alternarFavorito()
        }

        barra.addView(
            favoritoButton,
            LinearLayout.LayoutParams(
                50,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

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

        menu.setPadding(
            8,
            0,
            8,
            0
        )

        menu.setOnClickListener {

            mostrarMenu()
        }

        barra.addView(
            menu,
            LinearLayout.LayoutParams(
                45,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

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
                210,
                0,
                0,
                0
            )
        )

        val parametros =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                68
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
            "📁",
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

        botao.setPadding(
            5,
            3,
            5,
            3
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
            e: Exception
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

                return
            }

            MotionEvent.ACTION_UP -> {

                val deslocamentoX =
                    evento.x - toqueInicialX

                val deslocamentoY =
                    evento.y - toqueInicialY

                if (
                    abs(deslocamentoX) > 120 &&
                    abs(deslocamentoX) > abs(deslocamentoY) * 1.3f
                ) {

                    if (
                        deslocamentoX < 0
                    ) {

                        irParaProximoVideo()

                    } else {

                        irParaVideoAnterior()
                    }

                    return
                }

                videoView.performClick()
            }
        }
    }

    override fun onTouchEvent(
        event: MotionEvent
    ): Boolean {

        tratarToque(
            event
        )

        return true
    }

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

        Toast.makeText(
            this,
            if (!favorito)
                "Adicionado aos favoritos"
            else
                "Removido dos favoritos",
            Toast.LENGTH_SHORT
        ).show()
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
            if (favorito) "★" else "☆"
    }

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
            e: Exception
        ) {

            Toast.makeText(
                this,
                "Não foi possível compartilhar",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

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
                intent
            )

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "Nenhum aplicativo pode abrir este vídeo",
                Toast.LENGTH_LONG
            ).show()
        }
    }

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

        var duracao =
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

            retriever.release()

        } catch (
            _: Exception
        ) {
        }

        val texto =
            """
            Nome: ${arquivo.name}
            
            Caminho:
            ${arquivo.absolutePath}
            
            Tamanho: $tamanho
            
            Duração: $duracao
            
            Formato: ${arquivo.extension.uppercase()}
            
            Modificado:
            $data
            """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle(
                "Informações do vídeo"
            )
            .setMessage(
                texto
            )
            .setPositiveButton(
                "OK",
                null
            )
            .show()
    }

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

        val indice =
            min(
                unidades.size - 1,
                max(
                    0,
                    (Math.log(
                        bytes.toDouble()
                    ) / Math.log(1024.0)).toInt()
                )
            )

        val valor =
            bytes /
                    Math.pow(
                        1024.0,
                        indice.toDouble()
                    )

        return String.format(
            Locale.getDefault(),
            "%.2f %s",
            valor,
            unidades[indice]
        )
    }

    private fun renomearVideo() {

        val arquivoAtual =
            arquivos[posicaoAtual]

        val campo =
            EditText(this)

        campo.setText(
            arquivoAtual.nameWithoutExtension
        )

        campo.setSelection(
            campo.text.length
        )

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
                "Renomear"
            ) { _, _ ->

                val novoNomeBase =
                    campo.text
                        .toString()
                        .trim()

                if (
                    novoNomeBase.isEmpty()
                ) {

                    Toast.makeText(
                        this,
                        "Nome inválido",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                val extensao =
                    arquivoAtual.extension

                val novoNome =
                    if (
                        extensao.isNotEmpty()
                    ) {

                        "$novoNomeBase.$extensao"

                    } else {

                        novoNomeBase
                    }

                val novoArquivo =
                    File(
                        arquivoAtual.parentFile,
                        novoNome
                    )

                if (
                    novoArquivo.exists()
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
                            novoArquivo

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

                } catch (
                    e: Exception
                ) {

                    Toast.makeText(
                        this,
                        "Erro ao renomear: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .show()
    }

    private fun copiarVideo() {

        val arquivo =
            arquivos[posicaoAtual]

        escolherPasta(
            "Copiar vídeo"
        ) { destino ->

            try {

                val destinoFinal =
                    obterDestinoDisponivel(
                        destino,
                        arquivo.name
                    )

                FileInputStream(
                    arquivo
                ).use { entrada ->

                    FileOutputStream(
                        destinoFinal
                    ).use { saida ->

                        val buffer =
                            ByteArray(
                                8192
                            )

                        var quantidade: Int

                        while (
                            entrada.read(
                                buffer
                            ).also {
                                quantidade = it
                            } > 0
                        ) {

                            saida.write(
                                buffer,
                                0,
                                quantidade
                            )
                        }
                    }
                }

                Toast.makeText(
                    this,
                    "Vídeo copiado",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (
                e: Exception
            ) {

                Toast.makeText(
                    this,
                    "Erro ao copiar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun moverVideo() {

        val arquivo =
            arquivos[posicaoAtual]

        escolherPasta(
            "Mover vídeo"
        ) { destino ->

            try {

                val destinoFinal =
                    obterDestinoDisponivel(
                        destino,
                        arquivo.name
                    )

                if (
                    arquivo.renameTo(
                        destinoFinal
                    )
                ) {

                    arquivos.removeAt(
                        posicaoAtual
                    )

                    if (
                        arquivos.isEmpty()
                    ) {

                        finish()
                        return@escolherPasta
                    }

                    if (
                        posicaoAtual >= arquivos.size
                    ) {

                        posicaoAtual =
                            arquivos.size - 1
                    }

                    carregarVideoAtual()

                    Toast.makeText(
                        this,
                        "Vídeo movido",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this,
                        "Não foi possível mover",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (
                e: Exception
            ) {

                Toast.makeText(
                    this,
                    "Erro ao mover: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun escolherPasta(
        titulo: String,
        aoEscolher: (File) -> Unit
    ) {

        val raiz =
            android.os.Environment
                .getExternalStorageDirectory()

        val pastas =
            ArrayList<File>()

        fun adicionarPastas(
            pasta: File,
            nivel: Int
        ) {

            if (
                nivel > 2
            ) {
                return
            }

            try {

                val lista =
                    pasta.listFiles()
                        ?.filter {
                            it.isDirectory &&
                                    !it.name.startsWith(
                                        "."
                                    )
                        }
                        ?.sortedBy {
                            it.name.lowercase()
                        }

                lista?.forEach {

                    pastas.add(it)

                    adicionarPastas(
                        it,
                        nivel + 1
                    )
                }

            } catch (
                _: Exception
            ) {
            }
        }

        adicionarPastas(
            raiz,
            0
        )

        val nomes =
            ArrayList<String>()

        nomes.add(
            "Armazenamento principal"
        )

        pastas.forEach {
            nomes.add(
                it.absolutePath
                    .removePrefix(
                        raiz.absolutePath
                    )
                    .trim('/')
            )
        }

        AlertDialog.Builder(this)
            .setTitle(
                titulo
            )
            .setItems(
                nomes.toTypedArray()
            ) { _, posicao ->

                val destino =
                    if (
                        posicao == 0
                    ) {

                        raiz

                    } else {

                        pastas[posicao - 1]
                    }

                aoEscolher(
                    destino
                )
            }
            .setNegativeButton(
                "Cancelar",
                null
            )
            .show()
    }

    private fun obterDestinoDisponivel(
        pasta: File,
        nomeOriginal: String
    ): File {

        var destino =
            File(
                pasta,
                nomeOriginal
            )

        if (
            !destino.exists()
        ) {
            return destino
        }

        val base =
            File(
                nomeOriginal
            ).nameWithoutExtension

        val extensao =
            File(
                nomeOriginal
            ).extension

        var contador =
            1

        while (
            destino.exists()
        ) {

            val novoNome =
                if (
                    extensao.isNotEmpty()
                ) {

                    "$base ($contador).$extensao"

                } else {

                    "$base ($contador)"
                }

            destino =
                File(
                    pasta,
                    novoNome
                )

            contador++
        }

        return destino
    }

    private fun enviarParaLixeira() {

        val arquivo =
            arquivos[posicaoAtual]

        AlertDialog.Builder(this)
            .setTitle(
                "Enviar para a lixeira?"
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

                try {

                    val raiz =
                        android.os.Environment
                            .getExternalStorageDirectory()

                    val lixeira =
                        File(
                            raiz,
                            ".GerenciadorArquivos/.Lixeira"
                        )

                    if (
                        !lixeira.exists()
                    ) {

                        lixeira.mkdirs()
                    }

                    val destino =
                        obterDestinoDisponivel(
                            lixeira,
                            arquivo.name
                        )

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

                            Toast.makeText(
                                this,
                                "Vídeo enviado para a lixeira",
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

                        carregarVideoAtual()

                        Toast.makeText(
                            this,
                            "Vídeo enviado para a lixeira",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        Toast.makeText(
                            this,
                            "Não foi possível mover o vídeo",
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

            "3gp" ->
                "video/3gpp"

            "webm" ->
                "video/webm"

            "m4v" ->
                "video/mp4"

            else ->
                "video/*"
        }
    }

    override fun onResume() {

        super.onResume()

        if (
            ::videoView.isInitialized
        ) {

            if (
                !videoView.isPlaying
            ) {

                videoView.start()
            }
        }
    }

    override fun onPause() {

        super.onPause()

        if (
            ::videoView.isInitialized
        ) {

            videoView.pause()
        }
    }

    override fun onDestroy() {

        if (
            ::videoView.isInitialized
        ) {

            videoView.stopPlayback()
        }

        super.onDestroy()
    }
}
