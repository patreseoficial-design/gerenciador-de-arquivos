package com.gerenciadordearquivos.app

import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.Date
import kotlin.math.max
import kotlin.math.min

class ImageViewerActivity : Activity() {

    private lateinit var imageView: ZoomImageView
    private lateinit var nomeArquivo: TextView

    private var arquivo: File? = null

    private val azul = Color.rgb(33, 150, 243)
    private val azulEscuro = Color.rgb(25, 118, 210)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        entrarTelaCheia()

        val caminho = intent.getStringExtra("arquivo")

        if (caminho.isNullOrEmpty()) {
            Toast.makeText(this, "Imagem não encontrada", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        arquivo = File(caminho)

        if (!arquivo!!.exists()) {
            Toast.makeText(this, "Arquivo não encontrado", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        criarInterface()
        carregarImagem()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            entrarTelaCheia()
        }
    }

    private fun entrarTelaCheia() {
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

        // =========================
        // BARRA SUPERIOR
        // =========================

        val barra = LinearLayout(this)
        barra.orientation = LinearLayout.HORIZONTAL
        barra.gravity = Gravity.CENTER_VERTICAL
        barra.setPadding(8, 0, 8, 0)
        barra.setBackgroundColor(azul)

        val voltar = TextView(this)
        voltar.text = "‹"
        voltar.textSize = 42f
        voltar.setTextColor(Color.WHITE)
        voltar.gravity = Gravity.CENTER
        voltar.setOnClickListener {
            finish()
        }

        barra.addView(
            voltar,
            LinearLayout.LayoutParams(
                52,
                64
            )
        )

        nomeArquivo = TextView(this)
        nomeArquivo.text = arquivo?.name ?: "Imagem"
        nomeArquivo.textSize = 18f
        nomeArquivo.setTextColor(Color.WHITE)
        nomeArquivo.setTypeface(null, Typeface.BOLD)
        nomeArquivo.gravity = Gravity.CENTER_VERTICAL
        nomeArquivo.maxLines = 1
        nomeArquivo.ellipsize = android.text.TextUtils.TruncateAt.MIDDLE

        val paramsNome = LinearLayout.LayoutParams(
            0,
            64,
            1f
        )

        barra.addView(nomeArquivo, paramsNome)

        val menu = TextView(this)
        menu.text = "⋮"
        menu.textSize = 32f
        menu.setTextColor(Color.WHITE)
        menu.gravity = Gravity.CENTER
        menu.setOnClickListener {
            mostrarMenu()
        }

        barra.addView(
            menu,
            LinearLayout.LayoutParams(
                52,
                64
            )
        )

        raiz.addView(barra)

        // =========================
        // VISUALIZADOR
        // =========================

        imageView = ZoomImageView(this)

        val imagemParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        raiz.addView(imageView, imagemParams)

        // =========================
        // INFORMAÇÃO INFERIOR
        // =========================

        val info = TextView(this)
        info.setTextColor(Color.WHITE)
        info.textSize = 13f
        info.gravity = Gravity.CENTER
        info.setPadding(8, 8, 8, 8)
        info.setBackgroundColor(Color.argb(190, 0, 0, 0))

        raiz.addView(
            info,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                42
            )
        )

        val arquivoAtual = arquivo

        if (arquivoAtual != null && arquivoAtual.exists()) {

            val bitmap = BitmapFactory.decodeFile(arquivoAtual.absolutePath)

            if (bitmap != null) {
                info.text =
                    "${bitmap.width} × ${bitmap.height}    •    ${formatarTamanho(arquivoAtual.length())}"
                bitmap.recycle()
            } else {
                info.text = formatarTamanho(arquivoAtual.length())
            }
        }

        setContentView(raiz)
    }

    private fun carregarImagem() {

        val file = arquivo ?: return

        try {

            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true

            BitmapFactory.decodeFile(
                file.absolutePath,
                options
            )

            val largura = options.outWidth
            val altura = options.outHeight

            if (largura <= 0 || altura <= 0) {
                Toast.makeText(
                    this,
                    "Não foi possível abrir a imagem",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val maxDimensao = 4096

            var sample = 1

            while (
                largura / sample > maxDimensao ||
                altura / sample > maxDimensao
            ) {
                sample *= 2
            }

            val optionsFinal = BitmapFactory.Options()
            optionsFinal.inSampleSize = sample
            optionsFinal.inPreferredConfig = Bitmap.Config.ARGB_8888

            val bitmap = BitmapFactory.decodeFile(
                file.absolutePath,
                optionsFinal
            )

            if (bitmap == null) {
                Toast.makeText(
                    this,
                    "Erro ao carregar imagem",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            imageView.setImageBitmap(bitmap)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Erro ao abrir imagem: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ============================================================
    // MENU
    // ============================================================

    private fun mostrarMenu() {

        val opcoes = arrayOf(
            "✏️  Renomear",
            "📁  Mover para...",
            "📂  Criar pasta",
            "ⓘ  Informações",
            "🗑️  Mover para lixeira"
        )

        AlertDialog.Builder(this)
            .setItems(opcoes) { _, qual ->

                when (qual) {

                    0 -> renomearArquivo()

                    1 -> mostrarNavegadorDePastas()

                    2 -> criarPasta(arquivo?.parentFile)

                    3 -> mostrarInformacoes()

                    4 -> moverParaLixeira()
                }
            }
            .show()
    }

    // ============================================================
    // RENOMEAR
    // ============================================================

    private fun renomearArquivo() {

        val atual = arquivo ?: return

        val entrada = EditText(this)
        entrada.setSingleLine(true)
        entrada.setText(atual.nameWithoutExtension)
        entrada.setSelection(0, entrada.text.length)

        val extensao = atual.extension

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 10, 40, 0)
        layout.addView(entrada)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Renomear arquivo")
            .setView(layout)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Renomear", null)
            .create()

        dialog.setOnShowListener {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    var novoNome = entrada.text.toString().trim()

                    if (novoNome.isEmpty()) {
                        entrada.error = "Digite um nome"
                        return@setOnClickListener
                    }

                    if (extensao.isNotEmpty()) {

                        if (!novoNome.lowercase()
                                .endsWith(".${extensao.lowercase()}")
                        ) {
                            novoNome += ".$extensao"
                        }
                    }

                    val novoArquivo = File(
                        atual.parentFile,
                        novoNome
                    )

                    if (novoArquivo.exists()) {

                        entrada.error = "Já existe um arquivo com esse nome"
                        return@setOnClickListener
                    }

                    if (atual.renameTo(novoArquivo)) {

                        arquivo = novoArquivo
                        nomeArquivo.text = novoArquivo.name

                        dialog.dismiss()

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
                }
        }

        dialog.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        )

        dialog.show()
    }

    // ============================================================
    // CRIAR PASTA
    // ============================================================

    private fun criarPasta(pastaPai: File?) {

        if (pastaPai == null) {
            Toast.makeText(
                this,
                "Pasta inválida",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val entrada = EditText(this)
        entrada.hint = "Nome da pasta"
        entrada.setSingleLine(true)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Criar pasta")
            .setView(entrada)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Criar", null)
            .create()

        dialog.setOnShowListener {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val nome = entrada.text.toString().trim()

                    if (nome.isEmpty()) {
                        entrada.error = "Digite um nome"
                        return@setOnClickListener
                    }

                    val novaPasta = File(
                        pastaPai,
                        nome
                    )

                    if (novaPasta.exists()) {

                        entrada.error = "Essa pasta já existe"
                        return@setOnClickListener
                    }

                    if (novaPasta.mkdirs()) {

                        dialog.dismiss()

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
                }
        }

        dialog.show()
    }

    // ============================================================
    // NAVEGADOR DE PASTAS
    // ============================================================

    private fun mostrarNavegadorDePastas() {

        val atual = arquivo ?: return

        val raiz = Environment.getExternalStorageDirectory()

        var pastaAtual = atual.parentFile ?: raiz

        val dialog = AlertDialog.Builder(this)
            .setTitle("Mover para...")
            .create()

        val layoutPrincipal = LinearLayout(this)
        layoutPrincipal.orientation = LinearLayout.VERTICAL

        // Caminho atual

        val caminhoTexto = TextView(this)
        caminhoTexto.textSize = 13f
        caminhoTexto.setTextColor(Color.DKGRAY)
        caminhoTexto.setPadding(24, 14, 24, 14)
        caminhoTexto.setBackgroundColor(Color.rgb(245, 245, 245))

        layoutPrincipal.addView(
            caminhoTexto,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // Lista de pastas

        val lista = ListView(this)

        layoutPrincipal.addView(
            lista,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // Botões

        val botoes = LinearLayout(this)
        botoes.orientation = LinearLayout.HORIZONTAL
        botoes.gravity = Gravity.CENTER_VERTICAL
        botoes.setPadding(10, 8, 10, 8)

        val criar = Button(this)
        criar.text = "＋ Pasta"
        criar.isAllCaps = false

        val mover = Button(this)
        mover.text = "Mover para esta pasta"
        mover.isAllCaps = false

        botoes.addView(
            criar,
            LinearLayout.LayoutParams(
                0,
                52,
                1f
            )
        )

        botoes.addView(
            mover,
            LinearLayout.LayoutParams(
                0,
                52,
                1f
            )
        )

        layoutPrincipal.addView(botoes)

        dialog.setView(layoutPrincipal)

        fun atualizarLista() {

            caminhoTexto.text = pastaAtual.absolutePath

            val itens = ArrayList<File>()

            // Pasta pai

            if (pastaAtual.absolutePath != raiz.absolutePath) {
                itens.add(
                    File("⬆️  ..")
                )
            }

            val subpastas = pastaAtual
                .listFiles()
                ?.filter {
                    it.isDirectory &&
                    !it.name.startsWith(".")
                }
                ?.sortedBy {
                    it.name.lowercase()
                }
                ?: emptyList()

            itens.addAll(subpastas)

            val nomes = itens.map { file ->

                if (file.path == "⬆️  ..") {
                    "⬆️  .."
                } else {
                    "📁  ${file.name}"
                }

            }

            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                nomes
            )

            lista.adapter = adapter

            lista.setOnItemClickListener { _, _, position, _ ->

                val selecionado = itens[position]

                if (selecionado.path == "⬆️  ..") {

                    pastaAtual = pastaAtual.parentFile ?: raiz

                    atualizarLista()

                } else {

                    pastaAtual = selecionado

                    atualizarLista()
                }
            }
        }

        criar.setOnClickListener {

            val entrada = EditText(this)
            entrada.hint = "Nome da pasta"

            val criarDialog = AlertDialog.Builder(this)
                .setTitle("Nova pasta")
                .setView(entrada)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Criar", null)
                .create()

            criarDialog.setOnShowListener {

                criarDialog
                    .getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener {

                        val nome = entrada.text.toString().trim()

                        if (nome.isEmpty()) {
                            entrada.error = "Digite um nome"
                            return@setOnClickListener
                        }

                        val novaPasta = File(
                            pastaAtual,
                            nome
                        )

                        if (novaPasta.exists()) {

                            entrada.error = "Essa pasta já existe"
                            return@setOnClickListener
                        }

                        if (novaPasta.mkdirs()) {

                            criarDialog.dismiss()

                            atualizarLista()

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
                    }
            }

            criarDialog.show()
        }

        mover.setOnClickListener {

            if (pastaAtual.absolutePath ==
                atual.parentFile?.absolutePath
            ) {

                Toast.makeText(
                    this,
                    "O arquivo já está nesta pasta",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            moverArquivoParaPasta(
                atual,
                pastaAtual,
                dialog
            )
        }

        atualizarLista()

        dialog.setOnShowListener {
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.94).toInt(),
                (resources.displayMetrics.heightPixels * 0.85).toInt()
            )
        }

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
    }

    // ============================================================
    // MOVER ARQUIVO
    // ============================================================

    private fun moverArquivoParaPasta(
        origem: File,
        destinoPasta: File,
        dialog: AlertDialog
    ) {

        val destino = File(
            destinoPasta,
            origem.name
        )

        if (destino.exists()) {

            AlertDialog.Builder(this)
                .setTitle("Arquivo já existe")
                .setMessage(
                    "Já existe um arquivo chamado:\n\n${origem.name}\n\n" +
                    "na pasta escolhida."
                )
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Renomear") { _, _ ->

                    renomearAntesDeMover(
                        origem,
                        destinoPasta,
                        dialog
                    )
                }
                .show()

            return
        }

        executarMovimentacao(
            origem,
            destino,
            dialog
        )
    }

    private fun renomearAntesDeMover(
        origem: File,
        pastaDestino: File,
        dialog: AlertDialog
    ) {

        val entrada = EditText(this)

        entrada.setSingleLine(true)
        entrada.setText(origem.nameWithoutExtension)
        entrada.setSelection(0, entrada.text.length)

        val extensao = origem.extension

        val renameDialog = AlertDialog.Builder(this)
            .setTitle("Novo nome")
            .setView(entrada)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Mover", null)
            .create()

        renameDialog.setOnShowListener {

            renameDialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    var nome = entrada.text.toString().trim()

                    if (nome.isEmpty()) {
                        entrada.error = "Digite um nome"
                        return@setOnClickListener
                    }

                    if (
                        extensao.isNotEmpty() &&
                        !nome.lowercase()
                            .endsWith(".${extensao.lowercase()}")
                    ) {
                        nome += ".$extensao"
                    }

                    val novoDestino = File(
                        pastaDestino,
                        nome
                    )

                    if (novoDestino.exists()) {

                        entrada.error = "Esse nome já existe"
                        return@setOnClickListener
                    }

                    executarMovimentacao(
                        origem,
                        novoDestino,
                        dialog
                    )

                    renameDialog.dismiss()
                }
        }

        renameDialog.show()
    }

    private fun executarMovimentacao(
        origem: File,
        destino: File,
        dialog: AlertDialog?
    ) {

        try {

            if (origem.renameTo(destino)) {

                dialog?.dismiss()

                arquivo = destino

                Toast.makeText(
                    this,
                    "Arquivo movido com sucesso",
                    Toast.LENGTH_SHORT
                ).show()

                finish()

                return
            }

            // Se renameTo falhar, tenta copiar e apagar.

            moverPorCopia(
                origem,
                destino,
                dialog
            )

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Erro ao mover: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun moverPorCopia(
        origem: File,
        destino: File,
        dialog: AlertDialog?
    ) {

        try {

            FileInputStream(origem).use { input ->

                FileOutputStream(destino).use { output ->

                    val buffer = ByteArray(1024 * 1024)

                    var lidos: Int

                    while (
                        input.read(buffer).also {
                            lidos = it
                        } > 0
                    ) {

                        output.write(
                            buffer,
                            0,
                            lidos
                        )
                    }

                    output.flush()
                }
            }

            if (destino.exists() && destino.length() == origem.length()) {

                if (origem.delete()) {

                    dialog?.dismiss()

                    arquivo = destino

                    Toast.makeText(
                        this,
                        "Arquivo movido com sucesso",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()

                } else {

                    destino.delete()

                    Toast.makeText(
                        this,
                        "Não foi possível remover o arquivo original",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } else {

                destino.delete()

                Toast.makeText(
                    this,
                    "Falha ao copiar o arquivo",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {

            destino.delete()

            Toast.makeText(
                this,
                "Erro ao mover: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ============================================================
    // LIXEIRA
    // ============================================================

    private fun moverParaLixeira() {

        val atual = arquivo ?: return

        val pastaLixeira = File(
            Environment.getExternalStorageDirectory(),
            ".GerenciadorArquivos/.Lixeira"
        )

        try {

            if (!pastaLixeira.exists()) {
                pastaLixeira.mkdirs()
            }

            var destino = File(
                pastaLixeira,
                atual.name
            )

            if (destino.exists()) {

                val base = atual.nameWithoutExtension
                val extensao = atual.extension

                var contador = 1

                do {

                    val novoNome =
                        if (extensao.isNotEmpty()) {
                            "${base}_$contador.$extensao"
                        } else {
                            "${base}_$contador"
                        }

                    destino = File(
                        pastaLixeira,
                        novoNome
                    )

                    contador++

                } while (destino.exists())
            }

            executarMovimentacao(
                atual,
                destino,
                null
            )

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Erro ao mover para lixeira: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ============================================================
    // INFORMAÇÕES
    // ============================================================

    private fun mostrarInformacoes() {

        val atual = arquivo ?: return

        if (!atual.exists()) {

            Toast.makeText(
                this,
                "Arquivo não encontrado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val bitmap = BitmapFactory.decodeFile(
            atual.absolutePath
        )

        val tamanho = formatarTamanho(
            atual.length()
        )

        val data = DateFormat
            .getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.SHORT
            )
            .format(Date(atual.lastModified()))

        val resolucao =
            if (bitmap != null) {
                "${bitmap.width} × ${bitmap.height} pixels"
            } else {
                "Não disponível"
            }

        bitmap?.recycle()

        val mensagem = """
            Nome:
            ${atual.name}

            Localização:
            ${atual.absolutePath}

            Tamanho:
            $tamanho

            Resolução:
            $resolucao

            Formato:
            ${if (atual.extension.isEmpty()) "Desconhecido" else atual.extension.uppercase()}

            Modificado em:
            $data
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Informações")
            .setMessage(mensagem)
            .setPositiveButton("OK", null)
            .show()
    }

    // ============================================================
    // TAMANHO
    // ============================================================

    private fun formatarTamanho(bytes: Long): String {

        if (bytes < 1024) {
            return "$bytes B"
        }

        if (bytes < 1024 * 1024) {
            return String.format(
                "%.1f KB",
                bytes / 1024.0
            )
        }

        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(
                "%.1f MB",
                bytes / (1024.0 * 1024.0)
            )
        }

        return String.format(
            "%.2f GB",
            bytes / (1024.0 * 1024.0 * 1024.0)
        )
    }
}


// ================================================================
// VISUALIZADOR COM ZOOM
// ================================================================

class ZoomImageView(context: Context) : View(context) {

    private var bitmap: Bitmap? = null

    private val matrixImagem = Matrix()

    private val paint = Paint(
        Paint.ANTI_ALIAS_FLAG or
        Paint.FILTER_BITMAP_FLAG
    )

    private val detector =
        android.view.ScaleGestureDetector(
            context,
            object : android.view.ScaleGestureDetector
                .SimpleOnScaleGestureListener() {

                override fun onScale(
                    detector: android.view.ScaleGestureDetector
                ): Boolean {

                    val fator = detector.scaleFactor

                    val novoScale =
                        scale * fator

                    val limitado = novoScale.coerceIn(
                        minScale,
                        maxScale
                    )

                    val fatorReal =
                        limitado / scale

                    scale = limitado

                    matrixImagem.postScale(
                        fatorReal,
                        fatorReal,
                        detector.focusX,
                        detector.focusY
                    )

                    corrigirPosicao()

                    invalidate()

                    return true
                }
            }
        )

    private var escalaInicial = 1f

    private var scale = 1f

    private var minScale = 1f

    private var maxScale = 5f

    private var posX = 0f

    private var posY = 0f

    private var ultimoX = 0f

    private var ultimoY = 0f

    private var arrastando = false

    fun setImageBitmap(novaImagem: Bitmap) {

        bitmap = novaImagem

        post {
            ajustarImagemInicial()
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)

        canvas.drawColor(Color.BLACK)

        val imagem = bitmap ?: return

        val larguraView = width.toFloat()
        val alturaView = height.toFloat()

        if (larguraView <= 0 || alturaView <= 0) {
            return
        }

        val larguraImagem = imagem.width.toFloat()
        val alturaImagem = imagem.height.toFloat()

        val escalaX =
            larguraView / larguraImagem

        val escalaY =
            alturaView / alturaImagem

        val escala =
            min(escalaX, escalaY)

        val larguraFinal =
            larguraImagem * escala

        val alturaFinal =
            alturaImagem * escala

        val esquerda =
            (larguraView - larguraFinal) / 2f

        val topo =
            (alturaView - alturaFinal) / 2f

        canvas.save()

        canvas.translate(
            esquerda + posX,
            topo + posY
        )

        canvas.scale(
            escala * scale,
            escala * scale
        )

        canvas.drawBitmap(
            imagem,
            0f,
            0f,
            paint
        )

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        detector.onTouchEvent(event)

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {

                ultimoX = event.x
                ultimoY = event.y

                arrastando = true

                return true
            }

            MotionEvent.ACTION_MOVE -> {

                if (
                    event.pointerCount == 1 &&
                    scale > minScale
                ) {

                    val dx =
                        event.x - ultimoX

                    val dy =
                        event.y - ultimoY

                    posX += dx
                    posY += dy

                    corrigirPosicao()

                    ultimoX = event.x
                    ultimoY = event.y

                    invalidate()
                }

                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {

                arrastando = false

                return true
            }
        }

        return true
    }

    private fun ajustarImagemInicial() {

        val imagem = bitmap ?: return

        if (width <= 0 || height <= 0) {
            return
        }

        val escalaX =
            width.toFloat() /
                    imagem.width.toFloat()

        val escalaY =
            height.toFloat() /
                    imagem.height.toFloat()

        escalaInicial =
            min(escalaX, escalaY)

        minScale = 1f
        maxScale = 5f

        scale = 1f

        posX = 0f
        posY = 0f

        matrixImagem.reset()

        invalidate()
    }

    private fun corrigirPosicao() {

        val imagem = bitmap ?: return

        if (width <= 0 || height <= 0) {
            return
        }

        val escalaX =
            width.toFloat() /
                    imagem.width.toFloat()

        val escalaY =
            height.toFloat() /
                    imagem.height.toFloat()

        val base =
            min(escalaX, escalaY)

        val largura =
            imagem.width * base * scale

        val altura =
            imagem.height * base * scale

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

        posX =
            posX.coerceIn(
                -limiteX,
                limiteX
            )

        posY =
            posY.coerceIn(
                -limiteY,
                limiteY
            )

        if (scale <= minScale) {
            posX = 0f
            posY = 0f
        }
    }
}
