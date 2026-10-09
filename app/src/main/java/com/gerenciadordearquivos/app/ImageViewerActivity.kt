package com.gerenciadordearquivos.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.Window
import android.view.WindowManager
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


class ImageViewerActivity : Activity() {

    // Altura da barra de navegação do celular (px)
    private var espacoBaixoSistema = 0

    private lateinit var imageView: ZoomImageView
    private lateinit var arquivoAtual: File

    private var menuButton: ImageButton? = null
    private var favoritoButton: ImageButton? = null

    private val arquivos =
        ArrayList<File>()

    private var posicaoAtual = 0

    private lateinit var contadorText: TextView
    private lateinit var nomePastaText: TextView
    private lateinit var nomeArquivoText: TextView

    private var carregandoImagem = false


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        /*
         * Permite que o usuário escolha a posição
         * do aparelho:
         *
         * retrato = celular em pé
         * paisagem = celular de lado
         */
        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER

        /*
         * Evita que a tela desligue enquanto
         * a imagem estiver aberta.
         */
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val caminho =
            intent.getStringExtra("arquivo")

        if (caminho.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Imagem não encontrada",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        arquivoAtual =
            File(caminho)

        if (
            !arquivoAtual.exists() ||
            !arquivoAtual.isFile
        ) {

            Toast.makeText(
                this,
                "Arquivo não encontrado",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        val listaRecebida =
            intent.getStringArrayListExtra(
                "arquivos"
            )

        if (
            listaRecebida != null &&
            listaRecebida.isNotEmpty()
        ) {

            for (
                caminhoArquivo
                in listaRecebida
            ) {

                val arquivo =
                    File(caminhoArquivo)

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

            arquivos.add(
                arquivoAtual
            )
        }

        val posicaoRecebida =
            intent.getIntExtra(
                "posicao",
                -1
            )

        posicaoAtual =
            if (
                posicaoRecebida >= 0 &&
                posicaoRecebida < arquivos.size
            ) {

                posicaoRecebida

            } else {

                arquivos.indexOfFirst {
                    it.absolutePath ==
                        arquivoAtual.absolutePath
                }.coerceAtLeast(0)
            }

        arquivoAtual =
            arquivos[posicaoAtual]

        configurarTelaCheia()

        criarInterface()

        atualizarInformacoesDaTela()
    }


    /*
     * =========================================================
     * TELA CHEIA + ROTAÇÃO
     * =========================================================
     */

    private fun configurarTelaCheia() {

        try {

            requestWindowFeature(
                Window.FEATURE_NO_TITLE
            )

        } catch (_: Exception) {
        }

        /*
         * Gira junto com o celular, como na galeria:
         * respeita a "rotação automática" do sistema.
         */
        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER

        /*
         * Esconde só a barra de status. A barra de navegação
         * do celular fica visível e as barras do app ficam
         * acima dela (veja criarInterface).
         */
        BarrasDoSistema.configurarTelaCheia(
            this
        )
    }


    override fun onConfigurationChanged(
        newConfig: Configuration
    ) {

        super.onConfigurationChanged(
            newConfig
        )

        /*
         * A Activity não precisa ser recriada
         * quando o usuário gira o celular.
         *
         * A imagem continua aberta e o layout
         * simplesmente se adapta ao novo tamanho.
         */
        configurarTelaCheia()

        imageView.post {

            imageView.recalcularDepoisDaRotacao()
        }
    }


    /*
     * =========================================================
     * INTERFACE
     * =========================================================
     */

    private fun criarInterface() {

        val raiz =
            FrameLayoutCompat(this)

        raiz.setBackgroundColor(
            Color.BLACK
        )

        /*
         * =====================================================
         * IMAGEM
         * =====================================================
         */

        imageView =
            ZoomImageView(this)

        raiz.addView(
            imageView,
            FrameLayoutCompat.LayoutParams(
                FrameLayoutCompat.MATCH_PARENT,
                FrameLayoutCompat.MATCH_PARENT
            )
        )

        /*
         * =====================================================
         * TOPO
         * =====================================================
         */

        val topo =
            LinearLayout(this)

        topo.orientation =
            LinearLayout.HORIZONTAL

        topo.gravity =
            Gravity.CENTER_VERTICAL

        topo.setPadding(
            dp(8),
            dp(6),
            dp(8),
            dp(4)
        )

        topo.setBackgroundColor(
            Color.argb(
                175,
                0,
                0,
                0
            )
        )

        /*
         * VOLTAR
         */

        val voltar =
            ImageButton(this)

        voltar.setImageResource(
            android.R.drawable.ic_menu_revert
        )

        voltar.setColorFilter(
            Color.WHITE
        )

        voltar.setBackgroundColor(
            Color.TRANSPARENT
        )

        voltar.contentDescription =
            "Voltar"

        topo.addView(
            voltar,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

        voltar.setOnClickListener {
            finish()
        }

        /*
         * =====================================================
         * TEXTOS
         * =====================================================
         */

        val textos =
            LinearLayout(this)

        textos.orientation =
            LinearLayout.VERTICAL

        textos.gravity =
            Gravity.CENTER_VERTICAL

        textos.setPadding(
            dp(8),
            0,
            dp(8),
            0
        )

        /*
         * NOME DA PASTA
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

        configurarTextoRolante(
            nomePastaText
        )

        textos.addView(
            nomePastaText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(23)
            )
        )

        /*
         * NOME DO ARQUIVO
         */

        nomeArquivoText =
            TextView(this)

        nomeArquivoText.textSize =
            15f

        nomeArquivoText.setTextColor(
            Color.WHITE
        )

        nomeArquivoText.gravity =
            Gravity.CENTER_VERTICAL

        configurarTextoRolante(
            nomeArquivoText
        )

        textos.addView(
            nomeArquivoText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(27)
            )
        )

        /*
         * CONTADOR
         */

        contadorText =
            TextView(this)

        contadorText.textSize =
            11f

        contadorText.setTextColor(
            Color.LTGRAY
        )

        contadorText.gravity =
            Gravity.CENTER_VERTICAL

        textos.addView(
            contadorText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(18)
            )
        )

        topo.addView(
            textos,
            LinearLayout.LayoutParams(
                0,
                dp(68),
                1f
            )
        )

        /*
         * FAVORITO
         */

        favoritoButton =
            ImageButton(this)

        favoritoButton!!.setBackgroundColor(
            Color.TRANSPARENT
        )

        favoritoButton!!.setColorFilter(
            Color.WHITE
        )

        favoritoButton!!.contentDescription =
            "Favorito"

        topo.addView(
            favoritoButton,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

        favoritoButton!!.setOnClickListener {
            alternarFavorito()
        }

        /*
         * MENU
         */

        val menu =
            ImageButton(this)

        menuButton =
            menu

        menu.setImageResource(
            android.R.drawable.ic_menu_more
        )

        menu.setColorFilter(
            Color.WHITE
        )

        menu.setBackgroundColor(
            Color.TRANSPARENT
        )

        menu.contentDescription =
            "Mais opções"

        topo.addView(
            menu,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

        menu.setOnClickListener {
            mostrarMenu(menu)
        }

        val topoParams =
            FrameLayoutCompat.LayoutParams(
                FrameLayoutCompat.MATCH_PARENT,
                dp(76)
            )

        topoParams.gravity =
            Gravity.TOP

        raiz.addView(
            topo,
            topoParams
        )

        /*
         * =====================================================
         * BARRA INFERIOR
         * =====================================================
         */

        val inferior =
            LinearLayout(this)

        inferior.orientation =
            LinearLayout.HORIZONTAL

        inferior.gravity =
            Gravity.CENTER

        inferior.setPadding(
            dp(8),
            dp(4),
            dp(8),
            dp(8)
        )

        inferior.setBackgroundColor(
            Color.BLACK
        )

        adicionarBotaoInferior(
            inferior,
            "↗",
            "Compartilhar"
        ) {
            compartilharArquivo()
        }

        adicionarBotaoInferior(
            inferior,
            "⧉",
            "Copiar"
        ) {
            mostrarEscolhaDePastaParaCopiar()
        }

        adicionarBotaoInferior(
            inferior,
            "➜",
            "Mover"
        ) {
            abrirSeletorDePasta(
                arquivoAtual.parentFile
                    ?: Environment.getExternalStorageDirectory()
            )
        }

        adicionarBotaoInferior(
            inferior,
            "⋮",
            "Mais"
        ) {
            mostrarMenuInferior()
        }

        val inferiorParams =
            FrameLayoutCompat.LayoutParams(
                FrameLayoutCompat.MATCH_PARENT,
                dp(78)
            )

        inferiorParams.gravity =
            Gravity.BOTTOM

        raiz.addView(
            inferior,
            inferiorParams
        )

        /*
         * SWIPE
         */

        imageView.onSwipeLeft = {
            abrirProximaImagem()
        }

        imageView.onSwipeRight = {
            abrirImagemAnterior()
        }

        /*
         * Afasta as barras do app dos botões/gestos do celular
         */
        BarrasDoSistema.aoMudarEspacos(
            raiz
        ) { esquerda, topoSistema, direita, baixo ->

            topo.setPadding(
                dp(8) + esquerda,
                dp(6) + topoSistema,
                dp(8) + direita,
                dp(4)
            )

            topoParams.height =
                dp(76) + topoSistema

            topo.layoutParams =
                topoParams

            inferior.setPadding(
                dp(8) + esquerda,
                dp(4),
                dp(8) + direita,
                dp(8) + baixo
            )

            inferiorParams.height =
                dp(78) + baixo

            espacoBaixoSistema =
                baixo

            inferior.layoutParams =
                inferiorParams
        }

        setContentView(raiz)

        carregarImagem()
    }


    /*
     * Texto longo:
     * em vez de cortar com "...",
     * ele desliza horizontalmente.
     */

    private fun configurarTextoRolante(
        texto: TextView
    ) {

        texto.isSingleLine = true

        texto.maxLines = 1

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


    private fun adicionarBotaoInferior(
        layout: LinearLayout,
        icone: String,
        texto: String,
        acao: () -> Unit
    ) {

        val coluna =
            LinearLayout(this)

        coluna.orientation =
            LinearLayout.VERTICAL

        coluna.gravity =
            Gravity.CENTER

        coluna.setPadding(
            dp(8),
            0,
            dp(8),
            0
        )

        val iconeRes =
            MenuEscuro.iconeDaAcao(texto)

        if (iconeRes != null) {

            val iconeView =
                ImageView(this)

            iconeView.setImageResource(
                iconeRes
            )

            iconeView.setColorFilter(
                Color.WHITE
            )

            iconeView.setPadding(
                0,
                dp(4),
                0,
                dp(4)
            )

            coluna.addView(
                iconeView,
                LinearLayout.LayoutParams(
                    dp(42),
                    dp(34)
                )
            )

        } else {

            val iconeView =
                TextView(this)

            iconeView.text =
                icone

            iconeView.textSize =
                24f

            iconeView.gravity =
                Gravity.CENTER

            iconeView.setTextColor(
                Color.WHITE
            )

            coluna.addView(
                iconeView,
                LinearLayout.LayoutParams(
                    dp(42),
                    dp(34)
                )
            )
        }

        val textoView =
            TextView(this)

        textoView.text =
            texto

        textoView.textSize =
            13f

        textoView.typeface =
            android.graphics.Typeface.create(
                "sans-serif-medium",
                android.graphics.Typeface.NORMAL
            )

        textoView.gravity =
            Gravity.CENTER

        textoView.setTextColor(
            Color.WHITE
        )

        textoView.maxLines =
            1

        coluna.addView(
            textoView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(25)
            )
        )

        coluna.setOnClickListener {
            acao()
        }

        layout.addView(
            coluna,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        )
    }


    /*
     * =========================================================
     * INFORMAÇÕES DA TELA
     * =========================================================
     */

    private fun atualizarInformacoesDaTela() {

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

        nomePastaText.isSelected =
            true

        nomeArquivoText.isSelected =
            true

        atualizarIconeFavorito()
    }


    private fun atualizarIconeFavorito() {

        val favorito =
            isFavorito()

        favoritoButton?.setImageResource(
            if (favorito) {
                android.R.drawable.btn_star_big_on
            } else {
                android.R.drawable.btn_star_big_off
            }
        )

        favoritoButton?.setColorFilter(
            if (favorito) {
                Color.YELLOW
            } else {
                Color.WHITE
            }
        )
    }


    /*
     * =========================================================
     * CARREGAR IMAGEM
     * =========================================================
     */

    @android.annotation.TargetApi(28)
    private fun decodificarComImageDecoder(
        arquivo: File,
        amostra: Int
    ): Bitmap? {

        return try {

            android.graphics.ImageDecoder.decodeBitmap(
                android.graphics.ImageDecoder.createSource(arquivo)
            ) { decoder, _, _ ->

                decoder.setTargetSampleSize(amostra)

                // Software: permite o mipmap (foto lisa ao reduzir)
                decoder.allocator =
                    android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
            }

        } catch (
            _: Exception
        ) {
            null
        } catch (
            _: OutOfMemoryError
        ) {
            null
        }
    }


    private fun carregarImagem() {

        carregandoImagem = true

        imageView.resetarZoom()

        Thread {

            try {

                val bounds =
                    BitmapFactory.Options()

                bounds.inJustDecodeBounds =
                    true

                BitmapFactory.decodeFile(
                    arquivoAtual.absolutePath,
                    bounds
                )

                if (
                    bounds.outWidth <= 0 ||
                    bounds.outHeight <= 0
                ) {

                    runOnUiThread {

                        carregandoImagem =
                            false

                        Toast.makeText(
                            this,
                            "Formato de imagem inválido",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    return@Thread
                }

                /*
                 * Abre a foto na resolução original do arquivo.
                 * Só reduz quando ela passa do que a tela/memória
                 * do aparelho consegue desenhar (fotos enormes).
                 */
                var sample = 1

                val limiteLado =
                    if (
                        android.os.Build.VERSION.SDK_INT >=
                        android.os.Build.VERSION_CODES.P
                    ) 8192 else 4096

                val limiteBytes =
                    minOf(
                        90L * 1024 * 1024,
                        Runtime.getRuntime().maxMemory() / 3
                    )

                while (
                    bounds.outWidth / sample > limiteLado ||
                    bounds.outHeight / sample > limiteLado ||
                    (bounds.outWidth / sample).toLong() *
                        (bounds.outHeight / sample).toLong() *
                        4L > limiteBytes
                ) {

                    sample *= 2
                }

                val options =
                    BitmapFactory.Options()

                options.inSampleSize =
                    sample

                options.inPreferredConfig =
                    Bitmap.Config.ARGB_8888

                // No Android 9+ usa o mesmo decodificador da galeria:
                // foto na posição certa (EXIF), cores fiéis e HEIC
                val bitmap =
                    (if (
                        android.os.Build.VERSION.SDK_INT >=
                        android.os.Build.VERSION_CODES.P
                    ) {
                        decodificarComImageDecoder(
                            arquivoAtual,
                            sample
                        )
                    } else {
                        null
                    })
                        ?: BitmapFactory.decodeFile(
                            arquivoAtual.absolutePath,
                            options
                        )

                if (bitmap == null) {

                    runOnUiThread {

                        carregandoImagem =
                            false

                        Toast.makeText(
                            this,
                            "Não foi possível carregar a imagem",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    return@Thread
                }

                runOnUiThread {

                    if (
                        !isFinishing &&
                        !isDestroyed
                    ) {

                        // Mipmap: a foto reduzida para caber na tela
                        // fica lisa como na galeria, sem serrilhado
                        bitmap.setHasMipMap(
                            true
                        )

                        imageView.setImageBitmap(
                            bitmap
                        )

                        carregandoImagem =
                            false

                        atualizarInformacoesDaTela()
                    }
                }

            } catch (
                _: OutOfMemoryError
            ) {

                runOnUiThread {

                    carregandoImagem =
                        false

                    Toast.makeText(
                        this,
                        "Imagem muito grande para a memória do aparelho",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (
                _: Exception
            ) {

                runOnUiThread {

                    carregandoImagem =
                        false

                    Toast.makeText(
                        this,
                        "Erro ao abrir imagem",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.start()
    }


    /*
     * =========================================================
     * ANTERIOR / PRÓXIMA
     * =========================================================
     */

    private fun abrirProximaImagem() {

        if (carregandoImagem) {
            return
        }

        if (
            posicaoAtual >=
            arquivos.size - 1
        ) {

            Toast.makeText(
                this,
                "Última imagem",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        posicaoAtual++

        arquivoAtual =
            arquivos[posicaoAtual]

        atualizarInformacoesDaTela()

        carregarImagem()
    }


    private fun abrirImagemAnterior() {

        if (carregandoImagem) {
            return
        }

        if (
            posicaoAtual <= 0
        ) {

            Toast.makeText(
                this,
                "Primeira imagem",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        posicaoAtual--

        arquivoAtual =
            arquivos[posicaoAtual]

        atualizarInformacoesDaTela()

        carregarImagem()
    }


    /*
     * =========================================================
     * FAVORITOS
     * =========================================================
     */

    private fun preferenciasFavoritos() =
        getSharedPreferences(
            "favoritos_imagens",
            Context.MODE_PRIVATE
        )


    private fun isFavorito(): Boolean {

        return preferenciasFavoritos()
            .getBoolean(
                arquivoAtual.absolutePath,
                false
            )
    }


    private fun alternarFavorito() {

        val novoValor =
            !isFavorito()

        preferenciasFavoritos()
            .edit()
            .putBoolean(
                arquivoAtual.absolutePath,
                novoValor
            )
            .apply()

        atualizarIconeFavorito()

        Toast.makeText(
            this,
            if (novoValor) {
                "Imagem adicionada aos favoritos"
            } else {
                "Imagem removida dos favoritos"
            },
            Toast.LENGTH_SHORT
        ).show()
    }


    /*
     * =========================================================
     * MENU SUPERIOR
     * =========================================================
     */

    private fun mostrarMenu(
        ancora: View,
        acimaDaBarraInferior: Boolean = false
    ) {

        val layout =
            LinearLayout(this)

        MenuEscuro.prepararMenu(
            layout
        )

        val popup =
            PopupWindow(
                layout,
                dp(270),
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
            )

        popup.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )

        popup.isOutsideTouchable =
            true

        popup.elevation =
            16f

        adicionarOpcao(
            layout,
            "↗",
            "Abrir com"
        ) {

            popup.dismiss()

            abrirComAplicativo()
        }

        adicionarOpcao(
            layout,
            "ℹ",
            "Informações"
        ) {

            popup.dismiss()

            mostrarInformacoes()
        }

        adicionarOpcao(
            layout,
            "✎",
            "Renomear"
        ) {

            popup.dismiss()

            renomearArquivo()
        }

        adicionarOpcao(
            layout,
            "⧉",
            "Criar cópia"
        ) {

            popup.dismiss()

            mostrarEscolhaDePastaParaCopiar()
        }

        adicionarOpcao(
            layout,
            "＋",
            "Criar pasta"
        ) {

            popup.dismiss()

            criarPasta(
                arquivoAtual.parentFile
                    ?: Environment.getExternalStorageDirectory()
            )
        }

        adicionarOpcao(
            layout,
            "🗑",
            "Mover para lixeira"
        ) {

            popup.dismiss()

            moverParaLixeira()
        }

        if (acimaDaBarraInferior) {

            popup.showAtLocation(
                window.decorView,
                Gravity.BOTTOM or Gravity.END,
                dp(8),
                dp(86) + espacoBaixoSistema
            )

            return
        }

        ancora.post {

            val location =
                IntArray(2)

            ancora.getLocationOnScreen(
                location
            )

            val x =
                location[0] +
                    ancora.width -
                    dp(270)

            val y =
                location[1] +
                    ancora.height +
                    dp(4)

            popup.showAtLocation(
                window.decorView,
                Gravity.TOP or Gravity.START,
                max(
                    dp(6),
                    x
                ),
                y
            )
        }
    }


    private fun mostrarMenuInferior() {

        mostrarMenu(
            window.decorView,
            acimaDaBarraInferior = true
        )
    }


    private fun adicionarOpcao(
        layout: LinearLayout,
        icone: String,
        texto: String,
        acao: () -> Unit
    ) {

        layout.addView(
            MenuEscuro.criarLinha(
                this,
                icone,
                texto,
                acao
            )
        )
    }


    /*
     * =========================================================
     * COMPARTILHAR
     * =========================================================
     */

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
                )

            intent.type =
                obterMimeType()

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
                    "Compartilhar imagem"
                )
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Não foi possível compartilhar",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun obterMimeType(): String {

        return when (
            arquivoAtual.extension
                .lowercase(
                    Locale.getDefault()
                )
        ) {

            "jpg",
            "jpeg" ->
                "image/jpeg"

            "png" ->
                "image/png"

            "webp" ->
                "image/webp"

            "gif" ->
                "image/gif"

            "bmp" ->
                "image/bmp"

            "heic" ->
                "image/heic"

            "heif" ->
                "image/heif"

            else ->
                "image/*"
        }
    }


    /*
     * =========================================================
     * ABRIR COM
     * =========================================================
     */

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
                )

            intent.setDataAndType(
                uri,
                obterMimeType()
            )

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            startActivity(
                Intent.createChooser(
                    intent,
                    "Abrir imagem com"
                )
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Nenhum aplicativo compatível encontrado",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    /*
     * =========================================================
     * INFORMAÇÕES
     * =========================================================
     */

    private fun mostrarInformacoes() {

        val bounds =
            BitmapFactory.Options()

        bounds.inJustDecodeBounds =
            true

        BitmapFactory.decodeFile(
            arquivoAtual.absolutePath,
            bounds
        )

        val tamanho =
            formatarTamanho(
                arquivoAtual.length()
            )

        val data =
            SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss",
                Locale.getDefault()
            ).format(
                Date(
                    arquivoAtual.lastModified()
                )
            )

        val formato =
            arquivoAtual.extension
                .uppercase(
                    Locale.getDefault()
                )

        val resolucao =
            if (
                bounds.outWidth > 0 &&
                bounds.outHeight > 0
            ) {

                "${bounds.outWidth} × ${bounds.outHeight} px"

            } else {

                "Desconhecida"
            }

        val mensagem =
            """
            Nome: ${arquivoAtual.name}

            Localização:
            ${arquivoAtual.absolutePath}

            Tamanho: $tamanho

            Resolução: $resolucao

            Formato: $formato

            Modificado em: $data
            """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle(
                "Informações da imagem"
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


    private fun formatarTamanho(
        bytes: Long
    ): String {

        if (bytes < 1024) {
            return "$bytes B"
        }

        if (
            bytes <
            1024L * 1024L
        ) {

            return String.format(
                Locale.getDefault(),
                "%.1f KB",
                bytes / 1024.0
            )
        }

        if (
            bytes <
            1024L * 1024L * 1024L
        ) {

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
                (
                    1024.0 *
                    1024.0 *
                    1024.0
                )
        )
    }


    /*
     * =========================================================
     * RENOMEAR
     * =========================================================
     */

    private fun renomearArquivo() {

        val campo =
            EditText(this)

        campo.setText(
            arquivoAtual.nameWithoutExtension
        )

        campo.selectAll()

        AlertDialog.Builder(this)
            .setTitle(
                "Renomear imagem"
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

                val nomeBase =
                    campo.text
                        .toString()
                        .trim()

                if (
                    nomeBase.isEmpty()
                ) {

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
                    if (
                        extensao.isEmpty()
                    ) {

                        nomeBase

                    } else {

                        "$nomeBase.$extensao"
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

                        arquivoAtual =
                            novoArquivo

                        Toast.makeText(
                            this,
                            "Imagem renomeada",
                            Toast.LENGTH_SHORT
                        ).show()

                        atualizarInformacoesDaTela()

                    } else {

                        Toast.makeText(
                            this,
                            "Não foi possível renomear",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } catch (_: Exception) {

                    Toast.makeText(
                        this,
                        "Erro ao renomear",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .show()
    }


    /*
     * =========================================================
     * COPIAR
     * =========================================================
     */

    private fun mostrarEscolhaDePastaParaCopiar() {

        abrirSeletorDePasta(
            arquivoAtual.parentFile
                ?: Environment.getExternalStorageDirectory(),
            somenteCopiar = true
        )
    }


    /*
     * =========================================================
     * SELETOR DE PASTAS
     * =========================================================
     */

    private fun abrirSeletorDePasta(
        pastaInicial: File,
        somenteCopiar: Boolean = false
    ) {

        val raiz =
            Environment.getExternalStorageDirectory()

        val pasta =
            if (
                pastaInicial.exists()
            ) {

                pastaInicial

            } else {

                raiz
            }

        mostrarNavegador(
            pasta,
            raiz,
            somenteCopiar
        )
    }


    private fun mostrarNavegador(
        pastaAtual: File,
        raiz: File,
        somenteCopiar: Boolean
    ) {

        val dialog =
            AlertDialog.Builder(this)
                .create()

        val principal =
            LinearLayout(this)

        principal.orientation =
            LinearLayout.VERTICAL

        principal.setPadding(
            dp(10),
            dp(10),
            dp(10),
            dp(10)
        )

        val titulo =
            TextView(this)

        titulo.textSize =
            18f

        titulo.setTextColor(
            Color.BLACK
        )

        titulo.setPadding(
            dp(8),
            dp(8),
            dp(8),
            dp(8)
        )

        configurarTextoRolante(
            titulo
        )

        principal.addView(
            titulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(45)
            )
        )

        val lista =
            ListView(this)

        principal.addView(
            lista,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val botoes =
            LinearLayout(this)

        botoes.orientation =
            LinearLayout.HORIZONTAL

        val criar =
            Button(this)

        criar.text =
            "＋ Pasta"

        val cancelar =
            Button(this)

        cancelar.text =
            "Cancelar"

        val acao =
            Button(this)

        acao.text =
            if (somenteCopiar) {
                "Copiar aqui"
            } else {
                "Mover aqui"
            }

        botoes.addView(
            criar,
            LinearLayout.LayoutParams(
                0,
                dp(55),
                1f
            )
        )

        botoes.addView(
            cancelar,
            LinearLayout.LayoutParams(
                0,
                dp(55),
                1f
            )
        )

        botoes.addView(
            acao,
            LinearLayout.LayoutParams(
                0,
                dp(55),
                1f
            )
        )

        principal.addView(
            botoes
        )

        dialog.setTitle(
            if (somenteCopiar) {
                "Escolher pasta para copiar"
            } else {
                "Escolher pasta"
            }
        )

        dialog.setView(
            principal
        )

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

            acao.setOnClickListener {

                if (somenteCopiar) {

                    copiarArquivo(
                        pastaAtual,
                        dialog
                    )

                } else {

                    moverArquivo(
                        pastaAtual,
                        dialog
                    )
                }
            }
        }

        dialog.show()

        dialog.window?.setLayout(
            (
                resources.displayMetrics.widthPixels *
                    0.94
            ).toInt(),
            (
                resources.displayMetrics.heightPixels *
                    0.80
            ).toInt()
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

        titulo.isSelected =
            true

        val arquivos =
            ArrayList<File>()

        if (
            pasta.absolutePath !=
            raiz.absolutePath
        ) {

            arquivos.add(
                File(
                    pasta,
                    ".."
                )
            )
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

        arquivos.addAll(
            subpastas
        )

        val nomes =
            arquivos.map {

                if (
                    it.name == ".."
                ) {

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

            if (
                selecionada.name == ".."
            ) {

                pasta.parentFile?.let {
                    pai ->

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


    /*
     * =========================================================
     * CRIAR PASTA
     * =========================================================
     */

    private fun criarPasta(
        pastaPai: File,
        depois: (() -> Unit)? = null
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

                if (
                    pasta.exists()
                ) {

                    Toast.makeText(
                        this,
                        "Essa pasta já existe",
                        Toast.LENGTH_LONG
                    ).show()

                    return@setPositiveButton
                }

                try {

                    if (
                        pasta.mkdirs()
                    ) {

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

                } catch (_: Exception) {

                    Toast.makeText(
                        this,
                        "Erro ao criar pasta",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .show()
    }


    /*
     * =========================================================
     * COPIAR ARQUIVO
     * =========================================================
     */

    private fun copiarArquivo(
        destino: File,
        dialog: AlertDialog
    ) {

        val arquivoDestino =
            if (
                File(
                    destino,
                    arquivoAtual.name
                ).exists()
            ) {

                criarNomeUnico(
                    destino,
                    arquivoAtual.name
                )

            } else {

                File(
                    destino,
                    arquivoAtual.name
                )
            }

        Thread {

            try {

                copiarArquivoFisicamente(
                    arquivoAtual,
                    arquivoDestino
                )

                runOnUiThread {

                    dialog.dismiss()

                    Toast.makeText(
                        this,
                        "Imagem copiada com sucesso",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (_: Exception) {

                try {
                    arquivoDestino.delete()
                } catch (_: Exception) {
                }

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "Erro ao copiar imagem",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.start()
    }


    private fun copiarArquivoFisicamente(
        origem: File,
        destino: File
    ) {

        FileInputStream(
            origem
        ).use { entrada ->

            FileOutputStream(
                destino
            ).use { saida ->

                val buffer =
                    ByteArray(
                        64 * 1024
                    )

                while (true) {

                    val lidos =
                        entrada.read(
                            buffer
                        )

                    if (
                        lidos <= 0
                    ) {
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
            !destino.exists() ||
            destino.length() != origem.length()
        ) {

            destino.delete()

            throw Exception(
                "Falha na cópia"
            )
        }
    }


    /*
     * =========================================================
     * MOVER
     * =========================================================
     */

    private fun moverArquivo(
        destino: File,
        dialog: AlertDialog
    ) {

        if (
            destino.absolutePath ==
            arquivoAtual.parentFile
                ?.absolutePath
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

        if (
            arquivoDestino.exists()
        ) {

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
            File(
                nomeOriginal
            ).nameWithoutExtension

        val extensao =
            File(
                nomeOriginal
            ).extension

        var numero = 1

        while (true) {

            val nome =
                if (
                    extensao.isEmpty()
                ) {

                    "$base ($numero)"

                } else {

                    "$base ($numero).$extensao"
                }

            val arquivo =
                File(
                    pasta,
                    nome
                )

            if (
                !arquivo.exists()
            ) {

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

                arquivos[posicaoAtual] =
                    destino

                dialog.dismiss()

                Toast.makeText(
                    this,
                    "Imagem movida com sucesso",
                    Toast.LENGTH_SHORT
                ).show()

                atualizarInformacoesDaTela()

            } else {

                copiarEApagar(
                    arquivoAtual,
                    destino,
                    dialog
                )
            }

        } catch (_: Exception) {

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

            copiarArquivoFisicamente(
                origem,
                destino
            )

            if (
                origem.delete()
            ) {

                arquivoAtual =
                    destino

                if (
                    posicaoAtual <
                    arquivos.size
                ) {

                    arquivos[posicaoAtual] =
                        destino
                }

                dialog?.dismiss()

                Toast.makeText(
                    this,
                    "Imagem movida com sucesso",
                    Toast.LENGTH_SHORT
                ).show()

                atualizarInformacoesDaTela()

            } else {

                Toast.makeText(
                    this,
                    "Imagem copiada, mas a original não pôde ser removida",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (_: Exception) {

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


    /*
     * =========================================================
     * LIXEIRA
     * =========================================================
     */

    private fun moverParaLixeira() {

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

                if (
                    !lixeira.mkdirs()
                ) {

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
                .setTitle(
                    "Mover para lixeira?"
                )
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

                    val origem =
                        arquivoAtual

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

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Erro ao mover para lixeira",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    /*
     * =========================================================
     * UTILITÁRIO
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
}


/*
 * =============================================================
 * FRAME LAYOUT
 * =============================================================
 */

class FrameLayoutCompat(
    context: Context
) : android.widget.FrameLayout(context) {

    companion object {

        const val MATCH_PARENT =
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
    }

    class LayoutParams(
        width: Int,
        height: Int
    ) : android.widget.FrameLayout.LayoutParams(
        width,
        height
    )
}


/*
 * =============================================================
 * IMAGE VIEW COM ZOOM + SWIPE
 * =============================================================
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

    private var inicioX = 0f
    private var inicioY = 0f

    private var arrastando = false

    private var larguraImagem = 0
    private var alturaImagem = 0

    private var movimentoHorizontal =
        false

    private var distanciaMinimaSwipe =
        120f

    var onSwipeLeft:
        (() -> Unit)? = null

    var onSwipeRight:
        (() -> Unit)? = null


    init {

        setBackgroundColor(
            Color.BLACK
        )

        scaleType =
            ImageView.ScaleType.MATRIX

        isClickable =
            true

        distanciaMinimaSwipe =
            100f *
                resources.displayMetrics.density

        setOnTouchListener {
                _,
                evento ->

            escalaDetector.onTouchEvent(
                evento
            )

            when (
                evento.actionMasked
            ) {

                MotionEvent.ACTION_DOWN -> {

                    inicioX =
                        evento.x

                    inicioY =
                        evento.y

                    ultimoX =
                        evento.x

                    ultimoY =
                        evento.y

                    arrastando =
                        true

                    movimentoHorizontal =
                        false

                    true
                }


                MotionEvent.ACTION_MOVE -> {

                    val deltaX =
                        evento.x -
                            inicioX

                    val deltaY =
                        evento.y -
                            inicioY

                    if (
                        escala <=
                        escalaMinima + 0.001f &&
                        evento.pointerCount == 1
                    ) {

                        if (
                            abs(deltaX) >
                            abs(deltaY) &&
                            abs(deltaX) >
                            30f
                        ) {

                            movimentoHorizontal =
                                true
                        }
                    }

                    if (
                        arrastando &&
                        evento.pointerCount == 1 &&
                        !escalaDetector.isInProgress &&
                        escala >
                        escalaMinima + 0.001f
                    ) {

                        deslocamentoX +=
                            evento.x -
                            ultimoX

                        deslocamentoY +=
                            evento.y -
                            ultimoY

                        limitarDeslocamento()
                        aplicarTransformacao()

                        ultimoX =
                            evento.x

                        ultimoY =
                            evento.y
                    }

                    true
                }


                MotionEvent.ACTION_UP -> {

                    val deltaX =
                        evento.x -
                            inicioX

                    val deltaY =
                        evento.y -
                            inicioY

                    if (
                        escala <=
                        escalaMinima + 0.001f &&
                        movimentoHorizontal &&
                        abs(deltaX) >=
                        distanciaMinimaSwipe &&
                        abs(deltaX) >
                        abs(deltaY)
                    ) {

                        if (
                            deltaX < 0
                        ) {

                            onSwipeLeft?.invoke()

                        } else {

                            onSwipeRight?.invoke()
                        }
                    }

                    arrastando =
                        false

                    movimentoHorizontal =
                        false

                    true
                }


                MotionEvent.ACTION_CANCEL -> {

                    arrastando =
                        false

                    movimentoHorizontal =
                        false

                    true
                }

                else ->
                    true
            }
        }
    }


    override fun setImageBitmap(
        bitmap: Bitmap?
    ) {

        super.setImageBitmap(
            bitmap
        )

        if (
            bitmap != null
        ) {

            larguraImagem =
                bitmap.width

            alturaImagem =
                bitmap.height

            post {
                calcularEscalaInicial()
            }
        }
    }


    fun resetarZoom() {

        escala =
            escalaMinima

        deslocamentoX =
            0f

        deslocamentoY =
            0f

        aplicarTransformacao()
    }


    /*
     * Recalcula depois que o celular
     * muda de retrato para paisagem
     * ou vice-versa.
     */

    fun recalcularDepoisDaRotacao() {

        if (
            larguraImagem <= 0 ||
            alturaImagem <= 0
        ) {
            return
        }

        post {

            calcularEscalaInicial()
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

        if (
            escalaMinima <= 0f
        ) {

            escalaMinima =
                1f
        }

        escalaMaxima =
            max(
                escalaMinima * 5f,
                escalaMinima + 1f
            )

        escala =
            escalaMinima

        deslocamentoX =
            0f

        deslocamentoY =
            0f

        aplicarTransformacao()
    }


    private fun aplicarTransformacao() {

        if (
            drawable == null
        ) {

            return
        }

        val largura =
            larguraImagem *
                escala

        val altura =
            alturaImagem *
                escala

        val esquerda =
            width / 2f -
                largura / 2f +
                deslocamentoX

        val topo =
            height / 2f -
                altura / 2f +
                deslocamentoY

        val matriz =
            Matrix()

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

            deslocamentoX =
                0f

            deslocamentoY =
                0f

            return
        }

        val largura =
            larguraImagem *
                escala

        val altura =
            alturaImagem *
                escala

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

                deslocamentoX =
                    0f

                deslocamentoY =
                    0f
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
