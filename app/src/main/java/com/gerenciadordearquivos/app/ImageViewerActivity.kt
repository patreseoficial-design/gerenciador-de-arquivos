package com.gerenciadordearquivos.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class ImageViewerActivity : Activity() {

    private lateinit var imageView: ZoomImageView
    private lateinit var arquivoAtual: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val caminho = intent.getStringExtra("arquivo")

        if (caminho.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Imagem não encontrada",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        arquivoAtual = File(caminho)

        if (!arquivoAtual.exists() || !arquivoAtual.isFile) {
            Toast.makeText(
                this,
                "Arquivo não encontrado",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        configurarTelaCheia()
        criarInterface()
    }

    private fun configurarTelaCheia() {

        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        } catch (_: Exception) {
        }

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun criarInterface() {

        val raiz = LinearLayout(this)
        raiz.orientation = LinearLayout.VERTICAL
        raiz.setBackgroundColor(Color.BLACK)

        val barra = LinearLayout(this)
        barra.orientation = LinearLayout.HORIZONTAL
        barra.gravity = Gravity.CENTER_VERTICAL
        barra.setPadding(8, 4, 8, 4)
        barra.setBackgroundColor(Color.rgb(28, 28, 28))

        val voltar = ImageButton(this)
        voltar.setImageResource(android.R.drawable.ic_menu_revert)
        voltar.setColorFilter(Color.WHITE)
        voltar.setBackgroundColor(Color.TRANSPARENT)

        barra.addView(
            voltar,
            LinearLayout.LayoutParams(52, 52)
        )

        voltar.setOnClickListener {
            finish()
        }

        val titulo = TextView(this)
        titulo.text = arquivoAtual.name
        titulo.textSize = 17f
        titulo.setTextColor(Color.WHITE)
        titulo.gravity = Gravity.CENTER_VERTICAL
        titulo.maxLines = 1
        titulo.ellipsize =
            android.text.TextUtils.TruncateAt.MIDDLE

        val tituloParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1f
        )

        tituloParams.setMargins(8, 0, 8, 0)

        barra.addView(
            titulo,
            tituloParams
        )

        val menu = ImageButton(this)
        menu.setImageResource(android.R.drawable.ic_menu_more)
        menu.setColorFilter(Color.WHITE)
        menu.setBackgroundColor(Color.TRANSPARENT)

        barra.addView(
            menu,
            LinearLayout.LayoutParams(52, 52)
        )

        menu.setOnClickListener {
            mostrarMenu()
        }

        raiz.addView(
            barra,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                60
            )
        )

        imageView = ZoomImageView(this)

        raiz.addView(
            imageView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(raiz)

        carregarImagem()
    }

    private fun carregarImagem() {

        Thread {

            try {

                val bounds = BitmapFactory.Options()
                bounds.inJustDecodeBounds = true

                BitmapFactory.decodeFile(
                    arquivoAtual.absolutePath,
                    bounds
                )

                if (
                    bounds.outWidth <= 0 ||
                    bounds.outHeight <= 0
                ) {

                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Formato de imagem inválido",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    return@Thread
                }

                var sample = 1

                val limite = 4096

                while (
                    bounds.outWidth / sample > limite ||
                    bounds.outHeight / sample > limite
                ) {
                    sample *= 2
                }

                val options = BitmapFactory.Options()
                options.inSampleSize = sample
                options.inPreferredConfig =
                    Bitmap.Config.ARGB_8888

                val bitmap =
                    BitmapFactory.decodeFile(
                        arquivoAtual.absolutePath,
                        options
                    )

                if (bitmap == null) {

                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Não foi possível carregar a imagem",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    return@Thread
                }

                runOnUiThread {

                    if (!isFinishing && !isDestroyed) {
                        imageView.setImageBitmap(bitmap)
                    }
                }

            } catch (e: OutOfMemoryError) {

                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Imagem muito grande para a memória do aparelho",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Erro ao abrir imagem",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.start()
    }

    private fun mostrarMenu() {

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(0, 6, 0, 6)
        layout.setBackgroundColor(Color.WHITE)

        val popup = PopupWindow(
            layout,
            250,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        popup.setBackgroundDrawable(
            ColorDrawable(Color.WHITE)
        )

        popup.elevation = 12f

        adicionarOpcao(
            layout,
            "ℹ  Informações"
        ) {
            popup.dismiss()
            mostrarInformacoes()
        }

        adicionarOpcao(
            layout,
            "✎  Renomear"
        ) {
            popup.dismiss()
            renomearArquivo()
        }

        adicionarOpcao(
            layout,
            "↗  Mover"
        ) {
            popup.dismiss()

            abrirSeletorDePasta(
                arquivoAtual.parentFile
                    ?: Environment.getExternalStorageDirectory()
            )
        }

        adicionarOpcao(
            layout,
            "＋  Criar pasta"
        ) {
            popup.dismiss()

            criarPasta(
                arquivoAtual.parentFile
                    ?: Environment.getExternalStorageDirectory()
            )
        }

        adicionarOpcao(
            layout,
            "🗑  Mover para lixeira"
        ) {
            popup.dismiss()
            moverParaLixeira()
        }

        popup.showAtLocation(
            window.decorView,
            Gravity.TOP or Gravity.END,
            8,
            64
        )
    }

    private fun adicionarOpcao(
        layout: LinearLayout,
        texto: String,
        acao: () -> Unit
    ) {

        val item = TextView(this)

        item.text = texto
        item.textSize = 16f
        item.setTextColor(Color.DKGRAY)
        item.gravity = Gravity.CENTER_VERTICAL
        item.setPadding(20, 16, 20, 16)

        layout.addView(
            item,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                56
            )
        )

        item.setOnClickListener {
            acao()
        }
    }

    private fun mostrarInformacoes() {

        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true

        BitmapFactory.decodeFile(
            arquivoAtual.absolutePath,
            bounds
        )

        val tamanho =
            formatarTamanho(arquivoAtual.length())

        val data =
            SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss",
                Locale.getDefault()
            ).format(
                Date(arquivoAtual.lastModified())
            )

        val formato =
            arquivoAtual.extension
                .uppercase(Locale.getDefault())

        val resolucao =
            if (
                bounds.outWidth > 0 &&
                bounds.outHeight > 0
            ) {
                "${bounds.outWidth} × ${bounds.outHeight} px"
            } else {
                "Desconhecida"
            }

        val mensagem = """
            Nome: ${arquivoAtual.name}

            Localização:
            ${arquivoAtual.absolutePath}

            Tamanho: $tamanho

            Resolução: $resolucao

            Formato: $formato

            Modificado em: $data
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Informações da imagem")
            .setMessage(mensagem)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun formatarTamanho(bytes: Long): String {

        if (bytes < 1024) {
            return "$bytes B"
        }

        if (bytes < 1024L * 1024L) {
            return String.format(
                Locale.getDefault(),
                "%.1f KB",
                bytes / 1024.0
            )
        }

        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(
                Locale.getDefault(),
                "%.1f MB",
                bytes /
                    (1024.0 * 1024.0)
            )
        }

        return String.format(
            Locale.getDefault(),
            "%.1f GB",
            bytes /
                (1024.0 * 1024.0 * 1024.0)
        )
    }

    private fun renomearArquivo() {

        val campo = EditText(this)

        campo.setText(
            arquivoAtual.nameWithoutExtension
        )

        campo.selectAll()

        AlertDialog.Builder(this)
            .setTitle("Renomear imagem")
            .setView(campo)
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Renomear"
            ) { _, _ ->

                val nomeBase =
                    campo.text.toString().trim()

                if (nomeBase.isEmpty()) {

                    Toast.makeText(
                        this,
                        "Digite um nome",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                val extensao =
                    arquivoAtual.extension

                val novoNome =
                    if (extensao.isEmpty()) {
                        nomeBase
                    } else {
                        "$nomeBase.$extensao"
                    }

                val novoArquivo =
                    File(
                        arquivoAtual.parentFile,
                        novoNome
                    )

                if (novoArquivo.exists()) {

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

                        arquivoAtual =
                            novoArquivo

                        Toast.makeText(
                            this,
                            "Imagem renomeada",
                            Toast.LENGTH_SHORT
                        ).show()

                        recreate()

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
                        "Erro ao renomear",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .show()
    }

    private fun abrirSeletorDePasta(
        pastaInicial: File
    ) {

        val raiz =
            Environment.getExternalStorageDirectory()

        val pasta =
            if (pastaInicial.exists()) {
                pastaInicial
            } else {
                raiz
            }

        mostrarNavegador(
            pasta,
            raiz
        )
    }

    private fun mostrarNavegador(
        pastaAtual: File,
        raiz: File
    ) {

        val dialog =
            AlertDialog.Builder(this).create()

        val principal = LinearLayout(this)
        principal.orientation =
            LinearLayout.VERTICAL

        principal.setPadding(
            10,
            10,
            10,
            10
        )

        val titulo = TextView(this)
        titulo.textSize = 18f
        titulo.setTextColor(Color.BLACK)
        titulo.setPadding(8, 8, 8, 8)

        principal.addView(
            titulo
        )

        val lista = ListView(this)

        principal.addView(
            lista,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val botoes = LinearLayout(this)
        botoes.orientation =
            LinearLayout.HORIZONTAL

        val criar = Button(this)
        criar.text = "＋ Pasta"

        val cancelar = Button(this)
        cancelar.text = "Cancelar"

        val mover = Button(this)
        mover.text = "Mover aqui"

        botoes.addView(
            criar,
            LinearLayout.LayoutParams(
                0,
                55,
                1f
            )
        )

        botoes.addView(
            cancelar,
            LinearLayout.LayoutParams(
                0,
                55,
                1f
            )
        )

        botoes.addView(
            mover,
            LinearLayout.LayoutParams(
                0,
                55,
                1f
            )
        )

        principal.addView(botoes)

        dialog.setTitle("Escolher pasta")
        dialog.setView(principal)

        dialog.setOnShowListener {

            atualizarPastas(
                lista,
                titulo,
                pastaAtual,
                raiz,
                dialog
            )

            criar.setOnClickListener {

                criarPasta(
                    pastaAtual
                ) {

                    atualizarPastas(
                        lista,
                        titulo,
                        pastaAtual,
                        raiz,
                        dialog
                    )
                }
            }

            cancelar.setOnClickListener {
                dialog.dismiss()
            }

            mover.setOnClickListener {

                moverArquivo(
                    pastaAtual,
                    dialog
                )
            }
        }

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            (resources.displayMetrics.heightPixels * 0.80).toInt()
        )
    }

    private fun atualizarPastas(
        lista: ListView,
        titulo: TextView,
        pasta: File,
        raiz: File,
        dialog: AlertDialog
    ) {

        titulo.text =
            if (
                pasta.absolutePath ==
                raiz.absolutePath
            ) {
                "Armazenamento interno"
            } else {
                "📁 ${pasta.name}"
            }

        val arquivos =
            ArrayList<File>()

        if (
            pasta.absolutePath !=
            raiz.absolutePath
        ) {

            pasta.parentFile?.let { pai ->

                arquivos.add(
                    File(
                        pasta,
                        ".."
                    )
                )
            }
        }

        val subpastas =
            pasta.listFiles()
                ?.filter {
                    it.isDirectory &&
                    !it.isHidden
                }
                ?.sortedBy {
                    it.name.lowercase(
                        Locale.getDefault()
                    )
                }
                ?: emptyList()

        arquivos.addAll(subpastas)

        val nomes =
            arquivos.map {

                if (it.name == "..") {
                    "⬆  .."
                } else {
                    "📁  ${it.name}"
                }
            }

        lista.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                nomes
            )

        lista.setOnItemClickListener {
                _,
                _,
                posicao,
                _ ->

            val selecionada =
                arquivos[posicao]

            if (selecionada.name == "..") {

                pasta.parentFile?.let { pai ->

                    atualizarPastas(
                        lista,
                        titulo,
                        pai,
                        raiz,
                        dialog
                    )
                }

            } else {

                atualizarPastas(
                    lista,
                    titulo,
                    selecionada,
                    raiz,
                    dialog
                )
            }
        }
    }

    private fun criarPasta(
        pastaPai: File,
        depois: (() -> Unit)? = null
    ) {

        val campo = EditText(this)
        campo.hint = "Nome da pasta"

        AlertDialog.Builder(this)
            .setTitle("Criar pasta")
            .setView(campo)
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Criar"
            ) { _, _ ->

                val nome =
                    campo.text.toString().trim()

                if (nome.isEmpty()) {

                    Toast.makeText(
                        this,
                        "Digite um nome",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                val pasta =
                    File(
                        pastaPai,
                        nome
                    )

                if (pasta.exists()) {

                    Toast.makeText(
                        this,
                        "Essa pasta já existe",
                        Toast.LENGTH_LONG
                    ).show()

                    return@setPositiveButton
                }

                try {

                    if (pasta.mkdirs()) {

                        Toast.makeText(
                            this,
                            "Pasta criada",
                            Toast.LENGTH_SHORT
                        ).show()

                        depois?.invoke()

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
                        "Erro ao criar pasta",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .show()
    }

    private fun moverArquivo(
        destino: File,
        dialog: AlertDialog
    ) {

        if (
            destino.absolutePath ==
            arquivoAtual.parentFile?.absolutePath
        ) {

            Toast.makeText(
                this,
                "A imagem já está nessa pasta",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        var arquivoDestino =
            File(
                destino,
                arquivoAtual.name
            )

        if (arquivoDestino.exists()) {

            arquivoDestino =
                criarNomeUnico(
                    destino,
                    arquivoAtual.name
                )
        }

        executarMovimento(
            arquivoDestino,
            dialog
        )
    }

    private fun criarNomeUnico(
        pasta: File,
        nomeOriginal: String
    ): File {

        val base =
            File(nomeOriginal)
                .nameWithoutExtension

        val extensao =
            File(nomeOriginal)
                .extension

        var numero = 1

        while (true) {

            val nome =
                if (extensao.isEmpty()) {
                    "$base ($numero)"
                } else {
                    "$base ($numero).$extensao"
                }

            val arquivo =
                File(pasta, nome)

            if (!arquivo.exists()) {
                return arquivo
            }

            numero++
        }
    }

    private fun executarMovimento(
        destino: File,
        dialog: AlertDialog
    ) {

        try {

            if (
                arquivoAtual.renameTo(
                    destino
                )
            ) {

                arquivoAtual =
                    destino

                dialog.dismiss()

                Toast.makeText(
                    this,
                    "Imagem movida com sucesso",
                    Toast.LENGTH_SHORT
                ).show()

                finish()

            } else {

                copiarEApagar(
                    arquivoAtual,
                    destino,
                    dialog
                )
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Erro ao mover imagem",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun copiarEApagar(
        origem: File,
        destino: File,
        dialog: AlertDialog?
    ) {

        try {

            FileInputStream(origem).use { entrada ->

                FileOutputStream(destino).use { saida ->

                    val buffer =
                        ByteArray(64 * 1024)

                    while (true) {

                        val lidos =
                            entrada.read(buffer)

                        if (lidos <= 0) {
                            break
                        }

                        saida.write(
                            buffer,
                            0,
                            lidos
                        )
                    }
                }
            }

            if (
                destino.exists() &&
                destino.length() == origem.length()
            ) {

                if (origem.delete()) {

                    dialog?.dismiss()

                    Toast.makeText(
                        this,
                        "Imagem movida com sucesso",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()

                } else {

                    Toast.makeText(
                        this,
                        "Imagem copiada, mas a original não pôde ser removida",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } else {

                destino.delete()

                Toast.makeText(
                    this,
                    "Falha ao copiar imagem",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {

            try {
                destino.delete()
            } catch (_: Exception) {
            }

            Toast.makeText(
                this,
                "Erro ao mover imagem",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun moverParaLixeira() {

        val raiz =
            Environment.getExternalStorageDirectory()

        val lixeira =
            File(
                raiz,
                ".GerenciadorArquivos/.Lixeira"
            )

        try {

            if (!lixeira.exists()) {

                if (!lixeira.mkdirs()) {

                    Toast.makeText(
                        this,
                        "Não foi possível criar a lixeira",
                        Toast.LENGTH_LONG
                    ).show()

                    return
                }
            }

            val destino =
                criarNomeUnico(
                    lixeira,
                    arquivoAtual.name
                )

            AlertDialog.Builder(this)
                .setTitle("Mover para lixeira?")
                .setMessage(
                    "A imagem será movida para a lixeira."
                )
                .setNegativeButton(
                    "Cancelar",
                    null
                )
                .setPositiveButton(
                    "Mover"
                ) { _, _ ->

                    if (
                        arquivoAtual.renameTo(
                            destino
                        )
                    ) {

                        Toast.makeText(
                            this,
                            "Imagem movida para a lixeira",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()

                    } else {

                        copiarEApagar(
                            arquivoAtual,
                            destino,
                            null
                        )
                    }
                }
                .show()

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Erro ao mover para lixeira",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {

        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            configurarTelaCheia()
        }
    }
}


/**
 * ImageView nativo com zoom de dois dedos.
 */
class ZoomImageView(
    context: Context
) : ImageView(context) {

    private val escalaDetector =
        ScaleGestureDetector(
            context,
            EscalaListener()
        )

    private var escala = 1f
    private var escalaMinima = 1f
    private var escalaMaxima = 5f

    private var deslocamentoX = 0f
    private var deslocamentoY = 0f

    private var ultimoX = 0f
    private var ultimoY = 0f

    private var arrastando = false

    private var larguraImagem = 0
    private var alturaImagem = 0

    init {

        setBackgroundColor(Color.BLACK)

        scaleType =
            ImageView.ScaleType.MATRIX

        isClickable = true

        setOnTouchListener { _, evento ->

            escalaDetector.onTouchEvent(evento)

            when (evento.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    ultimoX =
                        evento.x

                    ultimoY =
                        evento.y

                    arrastando = true

                    true
                }

                MotionEvent.ACTION_MOVE -> {

                    if (
                        arrastando &&
                        evento.pointerCount == 1 &&
                        !escalaDetector.isInProgress
                    ) {

                        deslocamentoX +=
                            evento.x - ultimoX

                        deslocamentoY +=
                            evento.y - ultimoY

                        limitarDeslocamento()
                        aplicarTransformacao()

                        ultimoX =
                            evento.x

                        ultimoY =
                            evento.y
                    }

                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    arrastando = false

                    true
                }

                else -> true
            }
        }
    }

    override fun setImageBitmap(
        bitmap: Bitmap?
    ) {

        super.setImageBitmap(bitmap)

        if (bitmap != null) {

            larguraImagem =
                bitmap.width

            alturaImagem =
                bitmap.height

            post {
                calcularEscalaInicial()
            }
        }
    }

    private fun calcularEscalaInicial() {

        if (
            larguraImagem <= 0 ||
            alturaImagem <= 0 ||
            width <= 0 ||
            height <= 0
        ) {
            return
        }

        val escalaLargura =
            width.toFloat() /
                larguraImagem.toFloat()

        val escalaAltura =
            height.toFloat() /
                alturaImagem.toFloat()

        escalaMinima =
            min(
                escalaLargura,
                escalaAltura
            )

        if (escalaMinima <= 0f) {
            escalaMinima = 1f
        }

        escalaMaxima =
            max(
                escalaMinima * 5f,
                escalaMinima + 1f
            )

        escala =
            escalaMinima

        deslocamentoX = 0f
        deslocamentoY = 0f

        aplicarTransformacao()
    }

    private fun aplicarTransformacao() {

        if (drawable == null) {
            return
        }

        val largura =
            larguraImagem * escala

        val altura =
            alturaImagem * escala

        val esquerda =
            width / 2f -
                largura / 2f +
                deslocamentoX

        val topo =
            height / 2f -
                altura / 2f +
                deslocamentoY

        val matriz = Matrix()

        matriz.setScale(
            escala,
            escala
        )

        matriz.postTranslate(
            esquerda,
            topo
        )

        imageMatrix =
            matriz
    }

    private fun limitarDeslocamento() {

        if (
            escala <=
            escalaMinima + 0.001f
        ) {

            deslocamentoX = 0f
            deslocamentoY = 0f

            return
        }

        val largura =
            larguraImagem * escala

        val altura =
            alturaImagem * escala

        val limiteX =
            max(
                0f,
                (largura - width) / 2f
            )

        val limiteY =
            max(
                0f,
                (altura - height) / 2f
            )

        deslocamentoX =
            deslocamentoX.coerceIn(
                -limiteX,
                limiteX
            )

        deslocamentoY =
            deslocamentoY.coerceIn(
                -limiteY,
                limiteY
            )
    }

    private inner class EscalaListener :
        ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(
            detector: ScaleGestureDetector
        ): Boolean {

            escala =
                (
                    escala *
                    detector.scaleFactor
                ).coerceIn(
                    escalaMinima,
                    escalaMaxima
                )

            if (
                escala <=
                escalaMinima + 0.001f
            ) {

                deslocamentoX = 0f
                deslocamentoY = 0f
            }

            limitarDeslocamento()
            aplicarTransformacao()

            return true
        }
    }

    override fun onSizeChanged(
        largura: Int,
        altura: Int,
        larguraAntiga: Int,
        alturaAntiga: Int
    ) {

        super.onSizeChanged(
            largura,
            altura,
            larguraAntiga,
            alturaAntiga
        )

        post {
            if (
                larguraImagem > 0 &&
                alturaImagem > 0
            ) {
                calcularEscalaInicial()
            }
        }
    }
}
