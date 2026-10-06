package com.gerenciadordearquivos.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Environment
import android.view.GestureDetector
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

        configurarTelaCheia()

        val caminho = intent.getStringExtra("arquivo")

        if (caminho.isNullOrEmpty()) {
            Toast.makeText(
                this@ImageViewerActivity,
                "Imagem não encontrada",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        arquivoAtual = File(caminho)

        if (!arquivoAtual.exists()) {
            Toast.makeText(
                this@ImageViewerActivity,
                "Arquivo não encontrado",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        criarInterface()
    }

    private fun configurarTelaCheia() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.setFlags(
            Window.FEATURE_NO_TITLE,
            Window.FEATURE_NO_TITLE
        )

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun criarInterface() {

        val raiz = LinearLayout(this@ImageViewerActivity)
        raiz.orientation = LinearLayout.VERTICAL
        raiz.setBackgroundColor(Color.BLACK)

        val barraSuperior = LinearLayout(this@ImageViewerActivity)
        barraSuperior.orientation = LinearLayout.HORIZONTAL
        barraSuperior.gravity = Gravity.CENTER_VERTICAL
        barraSuperior.setPadding(12, 8, 8, 8)
        barraSuperior.setBackgroundColor(Color.argb(210, 25, 25, 25))

        val voltar = ImageButton(this@ImageViewerActivity)
        voltar.setImageResource(android.R.drawable.ic_menu_revert)
        voltar.setBackgroundColor(Color.TRANSPARENT)
        voltar.setColorFilter(Color.WHITE)

        val parametrosVoltar = LinearLayout.LayoutParams(
            52,
            52
        )

        barraSuperior.addView(
            voltar,
            parametrosVoltar
        )

        voltar.setOnClickListener {
            finish()
        }

        val titulo = TextView(this@ImageViewerActivity)
        titulo.text = arquivoAtual.name
        titulo.setTextColor(Color.WHITE)
        titulo.textSize = 17f
        titulo.maxLines = 1
        titulo.ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
        titulo.gravity = Gravity.CENTER_VERTICAL

        val parametrosTitulo = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            1f
        )

        parametrosTitulo.setMargins(10, 0, 10, 0)

        barraSuperior.addView(
            titulo,
            parametrosTitulo
        )

        val menu = ImageButton(this@ImageViewerActivity)
        menu.setImageResource(android.R.drawable.ic_menu_more)
        menu.setBackgroundColor(Color.TRANSPARENT)
        menu.setColorFilter(Color.WHITE)

        val parametrosMenu = LinearLayout.LayoutParams(
            52,
            52
        )

        barraSuperior.addView(
            menu,
            parametrosMenu
        )

        menu.setOnClickListener {
            mostrarMenu()
        }

        raiz.addView(
            barraSuperior,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                68
            )
        )

        imageView = ZoomImageView(this@ImageViewerActivity)

        val parametrosImagem = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        raiz.addView(
            imageView,
            parametrosImagem
        )

        setContentView(raiz)

        carregarImagem()
    }

    private fun carregarImagem() {

        try {

            val opcoes = BitmapFactory.Options()
            opcoes.inJustDecodeBounds = true

            BitmapFactory.decodeFile(
                arquivoAtual.absolutePath,
                opcoes
            )

            if (opcoes.outWidth <= 0 || opcoes.outHeight <= 0) {
                Toast.makeText(
                    this@ImageViewerActivity,
                    "Não foi possível ler a imagem",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val larguraMaxima = 4096
            val alturaMaxima = 4096

            var amostra = 1

            while (
                opcoes.outWidth / amostra > larguraMaxima ||
                opcoes.outHeight / amostra > alturaMaxima
            ) {
                amostra *= 2
            }

            val opcoesFinal = BitmapFactory.Options()
            opcoesFinal.inSampleSize = amostra
            opcoesFinal.inPreferredConfig =
                android.graphics.Bitmap.Config.ARGB_8888

            val bitmap = BitmapFactory.decodeFile(
                arquivoAtual.absolutePath,
                opcoesFinal
            )

            if (bitmap == null) {
                Toast.makeText(
                    this@ImageViewerActivity,
                    "Não foi possível carregar a imagem",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            imageView.setImageBitmap(bitmap)

        } catch (e: Exception) {

            Toast.makeText(
                this@ImageViewerActivity,
                "Erro ao carregar imagem: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun mostrarMenu() {

        val popupLayout = LinearLayout(this@ImageViewerActivity)
        popupLayout.orientation = LinearLayout.VERTICAL
        popupLayout.setPadding(0, 8, 0, 8)
        popupLayout.setBackgroundColor(Color.WHITE)

        val largura = 240

        val popup = PopupWindow(
            popupLayout,
            largura,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        popup.setBackgroundDrawable(
            ColorDrawable(Color.WHITE)
        )

        popup.elevation = 12f

        adicionarOpcaoMenu(
            popupLayout,
            "ℹ  Informações"
        ) {
            popup.dismiss()
            mostrarInformacoes()
        }

        adicionarOpcaoMenu(
            popupLayout,
            "✎  Renomear"
        ) {
            popup.dismiss()
            renomearArquivo()
        }

        adicionarOpcaoMenu(
            popupLayout,
            "↗  Mover"
        ) {
            popup.dismiss()
            abrirSeletorDePasta(
                arquivoAtual.parentFile
                    ?: Environment.getExternalStorageDirectory()
            )
        }

        adicionarOpcaoMenu(
            popupLayout,
            "＋  Criar pasta"
        ) {
            popup.dismiss()
            criarPasta(
                arquivoAtual.parentFile
                    ?: Environment.getExternalStorageDirectory()
            )
        }

        adicionarOpcaoMenu(
            popupLayout,
            "🗑  Mover para lixeira"
        ) {
            popup.dismiss()
            moverParaLixeira()
        }

        popup.showAtLocation(
            window.decorView,
            Gravity.TOP or Gravity.END,
            12,
            72
        )
    }

    private fun adicionarOpcaoMenu(
        layout: LinearLayout,
        texto: String,
        acao: () -> Unit
    ) {

        val item = TextView(this@ImageViewerActivity)

        item.text = texto
        item.textSize = 16f
        item.setTextColor(Color.DKGRAY)
        item.gravity = Gravity.CENTER_VERTICAL
        item.setPadding(20, 18, 20, 18)

        layout.addView(
            item,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                58
            )
        )

        item.setOnClickListener {
            acao()
        }
    }

    private fun mostrarInformacoes() {

        val opcoes = BitmapFactory.Options()
        opcoes.inJustDecodeBounds = true

        BitmapFactory.decodeFile(
            arquivoAtual.absolutePath,
            opcoes
        )

        val tamanho = formatarTamanho(
            arquivoAtual.length()
        )

        val data = SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            Locale.getDefault()
        ).format(
            Date(arquivoAtual.lastModified())
        )

        val formato = arquivoAtual.extension
            .uppercase(Locale.getDefault())

        val dimensoes =
            if (opcoes.outWidth > 0 && opcoes.outHeight > 0) {
                "${opcoes.outWidth} × ${opcoes.outHeight} px"
            } else {
                "Desconhecida"
            }

        val mensagem = """
            Nome: ${arquivoAtual.name}
            
            Localização:
            ${arquivoAtual.absolutePath}
            
            Tamanho: $tamanho
            
            Resolução: $dimensoes
            
            Formato: $formato
            
            Modificado em: $data
        """.trimIndent()

        AlertDialog.Builder(
            this@ImageViewerActivity
        )
            .setTitle("Informações da imagem")
            .setMessage(mensagem)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun formatarTamanho(bytes: Long): String {

        if (bytes < 1024) {
            return "$bytes B"
        }

        if (bytes < 1024 * 1024) {
            return String.format(
                Locale.getDefault(),
                "%.1f KB",
                bytes / 1024.0
            )
        }

        if (bytes < 1024 * 1024 * 1024) {
            return String.format(
                Locale.getDefault(),
                "%.1f MB",
                bytes / (1024.0 * 1024.0)
            )
        }

        return String.format(
            Locale.getDefault(),
            "%.1f GB",
            bytes / (1024.0 * 1024.0 * 1024.0)
        )
    }

    private fun renomearArquivo() {

        val campo = EditText(this@ImageViewerActivity)

        campo.setText(
            arquivoAtual.nameWithoutExtension
        )

        campo.selectAll()

        AlertDialog.Builder(
            this@ImageViewerActivity
        )
            .setTitle("Renomear imagem")
            .setView(campo)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Renomear") { _, _ ->

                val novoNomeBase =
                    campo.text.toString().trim()

                if (novoNomeBase.isEmpty()) {
                    Toast.makeText(
                        this@ImageViewerActivity,
                        "Digite um nome",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val extensao =
                    arquivoAtual.extension

                val novoNome =
                    if (extensao.isEmpty()) {
                        novoNomeBase
                    } else {
                        "$novoNomeBase.$extensao"
                    }

                val novoArquivo = File(
                    arquivoAtual.parentFile,
                    novoNome
                )

                if (novoArquivo.exists()) {
                    Toast.makeText(
                        this@ImageViewerActivity,
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

                        arquivoAtual = novoArquivo

                        Toast.makeText(
                            this@ImageViewerActivity,
                            "Imagem renomeada",
                            Toast.LENGTH_SHORT
                        ).show()

                        recreate()

                    } else {

                        Toast.makeText(
                            this@ImageViewerActivity,
                            "Não foi possível renomear",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } catch (e: Exception) {

                    Toast.makeText(
                        this@ImageViewerActivity,
                        "Erro: ${e.message}",
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

        mostrarNavegadorDePastas(
            pasta,
            raiz
        )
    }

    private fun mostrarNavegadorDePastas(
        pastaAtual: File,
        raiz: File
    ) {

        val dialog = AlertDialog.Builder(
            this@ImageViewerActivity
        ).create()

        val layoutPrincipal =
            LinearLayout(this@ImageViewerActivity)

        layoutPrincipal.orientation =
            LinearLayout.VERTICAL

        layoutPrincipal.setPadding(
            10,
            10,
            10,
            10
        )

        val cabecalho =
            LinearLayout(this@ImageViewerActivity)

        cabecalho.orientation =
            LinearLayout.HORIZONTAL

        cabecalho.gravity =
            Gravity.CENTER_VERTICAL

        val titulo =
            TextView(this@ImageViewerActivity)

        titulo.text =
            "Mover para: ${pastaAtual.name.ifEmpty { "Armazenamento interno" }}"

        titulo.textSize = 18f
        titulo.setTextColor(Color.BLACK)
        titulo.setPadding(8, 8, 8, 8)

        val parametrosTitulo =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

        cabecalho.addView(
            titulo,
            parametrosTitulo
        )

        val criar =
            Button(this@ImageViewerActivity)

        criar.text = "＋"

        cabecalho.addView(
            criar,
            LinearLayout.LayoutParams(
                55,
                55
            )
        )

        layoutPrincipal.addView(
            cabecalho
        )

        val lista =
            ListView(this@ImageViewerActivity)

        layoutPrincipal.addView(
            lista,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val botoes =
            LinearLayout(this@ImageViewerActivity)

        botoes.orientation =
            LinearLayout.HORIZONTAL

        botoes.gravity =
            Gravity.CENTER

        val cancelar =
            Button(this@ImageViewerActivity)

        cancelar.text = "Cancelar"

        val selecionar =
            Button(this@ImageViewerActivity)

        selecionar.text = "Mover para esta pasta"

        botoes.addView(
            cancelar,
            LinearLayout.LayoutParams(
                0,
                55,
                1f
            )
        )

        botoes.addView(
            selecionar,
            LinearLayout.LayoutParams(
                0,
                55,
                1f
            )
        )

        layoutPrincipal.addView(
            botoes
        )

        dialog.setTitle("Escolher pasta")
        dialog.setView(layoutPrincipal)

        dialog.setOnShowListener {

            atualizarListaPastas(
                lista,
                pastaAtual,
                raiz,
                titulo,
                dialog
            )

            criar.setOnClickListener {

                criarPasta(
                    pastaAtual
                ) {

                    atualizarListaPastas(
                        lista,
                        pastaAtual,
                        raiz,
                        titulo,
                        dialog
                    )
                }
            }

            cancelar.setOnClickListener {
                dialog.dismiss()
            }

            selecionar.setOnClickListener {

                moverArquivoPara(
                    pastaAtual,
                    dialog
                )
            }
        }

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
    }

    private fun atualizarListaPastas(
        lista: ListView,
        pastaAtual: File,
        raiz: File,
        titulo: TextView,
        dialog: AlertDialog
    ) {

        titulo.text =
            "Mover para: ${
                if (pastaAtual.absolutePath ==
                    raiz.absolutePath
                ) {
                    "Armazenamento interno"
                } else {
                    pastaAtual.name
                }
            }"

        val itens =
            ArrayList<File>()

        if (
            pastaAtual.absolutePath !=
            raiz.absolutePath
        ) {

            val pai = pastaAtual.parentFile

            if (
                pai != null &&
                pai.exists() &&
                pai.absolutePath.startsWith(
                    raiz.absolutePath
                )
            ) {

                itens.add(
                    File(
                        pastaAtual,
                        ".."
                    )
                )
            }
        }

        val subpastas =
            pastaAtual.listFiles()
                ?.filter {
                    it.isDirectory &&
                    !it.isHidden
                }
                ?.sortedBy {
                    it.name.lowercase(Locale.getDefault())
                }
                ?: emptyList()

        itens.addAll(subpastas)

        val nomes =
            itens.map { arquivo ->

                if (arquivo.name == "..") {
                    "⬆  .."
                } else {
                    "📁  ${arquivo.name}"
                }

            }

        val adapter =
            ArrayAdapter(
                this@ImageViewerActivity,
                android.R.layout.simple_list_item_1,
                nomes
            )

        lista.adapter = adapter

        lista.setOnItemClickListener { _, _, posicao, _ ->

            val selecionada =
                itens[posicao]

            if (selecionada.name == "..") {

                val pai =
                    pastaAtual.parentFile

                if (pai != null) {

                    atualizarListaPastas(
                        lista,
                        pai,
                        raiz,
                        titulo,
                        dialog
                    )
                }

            } else {

                atualizarListaPastas(
                    lista,
                    selecionada,
                    raiz,
                    titulo,
                    dialog
                )
            }
        }
    }

    private fun criarPasta(
        pastaPai: File,
        depois: (() -> Unit)? = null
    ) {

        val campo =
            EditText(this@ImageViewerActivity)

        campo.hint = "Nome da pasta"

        AlertDialog.Builder(
            this@ImageViewerActivity
        )
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
                        this@ImageViewerActivity,
                        "Digite um nome",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                val novaPasta =
                    File(pastaPai, nome)

                if (novaPasta.exists()) {

                    Toast.makeText(
                        this@ImageViewerActivity,
                        "Essa pasta já existe",
                        Toast.LENGTH_LONG
                    ).show()

                    return@setPositiveButton
                }

                try {

                    if (novaPasta.mkdirs()) {

                        Toast.makeText(
                            this@ImageViewerActivity,
                            "Pasta criada",
                            Toast.LENGTH_SHORT
                        ).show()

                        depois?.invoke()

                    } else {

                        Toast.makeText(
                            this@ImageViewerActivity,
                            "Não foi possível criar a pasta",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } catch (e: Exception) {

                    Toast.makeText(
                        this@ImageViewerActivity,
                        "Erro: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .show()
    }

    private fun moverArquivoPara(
        destino: File,
        dialog: AlertDialog
    ) {

        if (
            destino.absolutePath ==
            arquivoAtual.parentFile?.absolutePath
        ) {

            Toast.makeText(
                this@ImageViewerActivity,
                "A imagem já está nessa pasta",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val arquivoDestino =
            File(
                destino,
                arquivoAtual.name
            )

        if (arquivoDestino.exists()) {

            AlertDialog.Builder(
                this@ImageViewerActivity
            )
                .setTitle("Arquivo já existe")
                .setMessage(
                    "Já existe uma imagem com esse nome nessa pasta."
                )
                .setNegativeButton(
                    "Cancelar",
                    null
                )
                .setPositiveButton(
                    "Renomear automaticamente"
                ) { _, _ ->

                    val novoDestino =
                        criarNomeUnico(
                            destino,
                            arquivoAtual.name
                        )

                    executarMovimentacao(
                        novoDestino,
                        dialog
                    )
                }
                .show()

            return
        }

        executarMovimentacao(
            arquivoDestino,
            dialog
        )
    }

    private fun criarNomeUnico(
        pasta: File,
        nomeOriginal: String
    ): File {

        val base =
            File(nomeOriginal).nameWithoutExtension

        val extensao =
            File(nomeOriginal).extension

        var contador = 1

        while (true) {

            val nome =

                if (extensao.isEmpty()) {
                    "$base ($contador)"
                } else {
                    "$base ($contador).$extensao"
                }

            val arquivo =
                File(pasta, nome)

            if (!arquivo.exists()) {
                return arquivo
            }

            contador++
        }
    }

    private fun executarMovimentacao(
        destino: File,
        dialog: AlertDialog
    ) {

        try {

            val origem =
                arquivoAtual

            if (origem.renameTo(destino)) {

                arquivoAtual =
                    destino

                dialog.dismiss()

                Toast.makeText(
                    this@ImageViewerActivity,
                    "Imagem movida com sucesso",
                    Toast.LENGTH_SHORT
                ).show()

                finish()

            } else {

                moverPorCopia(
                    origem,
                    destino,
                    dialog
                )
            }

        } catch (e: Exception) {

            Toast.makeText(
                this@ImageViewerActivity,
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

                    saida.flush()
                }
            }

            if (!destino.exists()) {

                Toast.makeText(
                    this@ImageViewerActivity,
                    "Falha ao copiar arquivo",
                    Toast.LENGTH_LONG
                ).show()

                return
            }

            if (origem.delete()) {

                dialog?.dismiss()

                Toast.makeText(
                    this@ImageViewerActivity,
                    "Imagem movida com sucesso",
                    Toast.LENGTH_SHORT
                ).show()

                finish()

            } else {

                destino.delete()

                Toast.makeText(
                    this@ImageViewerActivity,
                    "A imagem foi copiada, mas não foi possível remover a original",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {

            try {
                destino.delete()
            } catch (_: Exception) {
            }

            Toast.makeText(
                this@ImageViewerActivity,
                "Erro ao mover arquivo: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun moverParaLixeira() {

        val raiz =
            Environment.getExternalStorageDirectory()

        val pastaLixeira =
            File(
                raiz,
                ".GerenciadorArquivos/.Lixeira"
            )

        try {

            if (!pastaLixeira.exists()) {

                if (!pastaLixeira.mkdirs()) {

                    Toast.makeText(
                        this@ImageViewerActivity,
                        "Não foi possível criar a lixeira",
                        Toast.LENGTH_LONG
                    ).show()

                    return
                }
            }

            var destino =
                File(
                    pastaLixeira,
                    arquivoAtual.name
                )

            if (destino.exists()) {

                destino =
                    criarNomeUnico(
                        pastaLixeira,
                        arquivoAtual.name
                    )
            }

            AlertDialog.Builder(
                this@ImageViewerActivity
            )
                .setTitle("Mover para lixeira?")
                .setMessage(
                    "A imagem será movida para a lixeira do Gerenciador de Arquivos."
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
                            this@ImageViewerActivity,
                            "Imagem movida para a lixeira",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()

                    } else {

                        moverPorCopia(
                            arquivoAtual,
                            destino,
                            null
                        )
                    }
                }
                .show()

        } catch (e: Exception) {

            Toast.makeText(
                this@ImageViewerActivity,
                "Erro: ${e.message}",
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
 * ImageView personalizado com:
 *
 * - Zoom por dois dedos
 * - Arrastar imagem ampliada
 * - Ajuste automático para caber na tela
 * - Imagem sem CENTER_CROP
 */
class ZoomImageView(
    context: android.content.Context
) : androidx.appcompat.widget.AppCompatImageView(context) {

    private val escalaDetector =
        ScaleGestureDetector(
            context,
            EscalaListener()
        )

    private val gestoDetector =
        GestureDetector(
            context,
            GestoListener()
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

        scaleType =
            ImageView.ScaleType.MATRIX

        setBackgroundColor(Color.BLACK)

        isClickable = true

        setOnTouchListener { _, evento ->

            escalaDetector.onTouchEvent(evento)
            gestoDetector.onTouchEvent(evento)

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
                        evento.pointerCount == 1
                    ) {

                        val deltaX =
                            evento.x - ultimoX

                        val deltaY =
                            evento.y - ultimoY

                        deslocamentoX +=
                            deltaX

                        deslocamentoY +=
                            deltaY

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
        bitmap: android.graphics.Bitmap?
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

        val bitmap =
            drawable ?: return

        val larguraDesenhada =
            larguraImagem * escala

        val alturaDesenhada =
            alturaImagem * escala

        val centroX =
            width / 2f

        val centroY =
            height / 2f

        val esquerda =
            centroX -
                larguraDesenhada / 2f +
                deslocamentoX

        val topo =
            centroY -
                alturaDesenhada / 2f +
                deslocamentoY

        val matriz =
            android.graphics.Matrix()

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

        if (escala <= escalaMinima + 0.001f) {

            deslocamentoX = 0f
            deslocamentoY = 0f

            return
        }

        val larguraDesenhada =
            larguraImagem * escala

        val alturaDesenhada =
            alturaImagem * escala

        val limiteX =
            max(
                0f,
                (larguraDesenhada - width) / 2f
            )

        val limiteY =
            max(
                0f,
                (alturaDesenhada - height) / 2f
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

            val novaEscala =
                escala *
                    detector.scaleFactor

            escala =
                novaEscala.coerceIn(
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

    private inner class GestoListener :
        GestureDetector.SimpleOnGestureListener() {

        override fun onDoubleTap(
            e: MotionEvent
        ): Boolean {

            if (
                escala >
                escalaMinima + 0.01f
            ) {

                escala =
                    escalaMinima

                deslocamentoX = 0f
                deslocamentoY = 0f

            } else {

                escala =
                    min(
                        escalaMinima * 2.5f,
                        escalaMaxima
                    )
            }

            limitarDeslocamento()
            aplicarTransformacao()

            return true
        }

        override fun onDown(
            e: MotionEvent
        ): Boolean {
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
