package com.gerenciadordearquivos.app

import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.LinearGradient
import android.graphics.Path
import android.graphics.PathEffect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var homeScroll: ScrollView
    private lateinit var fileScreen: LinearLayout
    private lateinit var fileScreenTitle: TextView
    private lateinit var currentPath: TextView
    private lateinit var fileList: ListView
    private lateinit var mediaGrid: GridView
    private lateinit var appTabs: LinearLayout
    private lateinit var barraAcoes: LinearLayout
    private lateinit var tabAppsNativos: TextView
    private lateinit var tabAppsBaixados: TextView

    private lateinit var storageInfo: TextView
    private lateinit var storageProgress: ProgressBar
    private lateinit var searchEdit: EditText

    private val rootPath: File =
        Environment.getExternalStorageDirectory()

    private var currentDirectory: File = rootPath

    private var pastaDeMidiaAtual: File? = null

    private var telaDePastasDeMidia = false

    private var tipoDeMidiaAtual: Set<String> = TIPO_IMAGEM

    private var categoriaMidiaAtual = ""

    private var aplicativosNativos: List<AppInfoItem> = emptyList()

    private var aplicativosBaixados: List<AppInfoItem> = emptyList()

    private var abaAppsNativos = true

    private var popupMenuAtual: PopupWindow? = null

    private val thumbnailExecutor =
        Executors.newFixedThreadPool(3)

    private val thumbnailCache =
        object : android.util.LruCache<String, Bitmap>(
            calcularTamanhoCache()
        ) {
            override fun sizeOf(
                key: String,
                value: Bitmap
            ): Int {
                return value.byteCount / 1024
            }
        }

    companion object {

        private const val URL_POLITICA_PRIVACIDADE =
            "https://github.com/patreseoficial-design/gerenciador-de-arquivos/blob/main/docs/politica-de-privacidade.md"

        private const val PEDIDO_PERMISSAO_ARQUIVOS = 10

        // Pasta onde ficam a lixeira e o cofre
        private const val PASTA_INTERNA = ".GerenciadorArquivos"

        private val COR_TEXTO_PRINCIPAL =
            Color.rgb(20, 20, 20)

        private val COR_TEXTO_SECUNDARIO =
            Color.rgb(85, 85, 85)

        private val TIPO_IMAGEM =
            setOf(
                "jpg",
                "jpeg",
                "png",
                "webp",
                "gif",
                "bmp",
                "heic",
                "heif"
            )

        private val TIPO_VIDEO =
            setOf(
                "mp4",
                "mkv",
                "avi",
                "mov",
                "3gp",
                "webm",
                "m4v"
            )

        private val TIPO_AUDIO =
            setOf(
                "mp3",
                "wav",
                "ogg",
                "m4a",
                "aac",
                "flac",
                "opus",
                "amr"
            )

        private val TIPO_DOCUMENTO =
            setOf(
                "pdf",
                "txt",
                "doc",
                "docx",
                "xls",
                "xlsx",
                "ppt",
                "pptx",
                "csv",
                "rtf",
                "odt",
                "ods",
                "odp",
                "zip",
                "rar",
                "7z"
            )
    }

    // Letras grandes (opção do menu ⋮)
    override fun attachBaseContext(
        novoContexto: android.content.Context
    ) {
        super.attachBaseContext(
            ModoSimples.contexto(novoContexto)
        )
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )

        inicializarViews()
        configurarBotoes()
        configurarPesquisa()

        atualizarArmazenamento()
        verificarPermissao()

        // Premium (Google Play) e anúncios
        Premium.iniciar(this)

        Premium.aoMudar(aoMudarPremium)

        // Aviso diário de celular quase cheio + widget
        AvisoArmazenamento.agendar(this)

        pedirPermissaoDeAviso()

        // Veio da notificação ou do widget
        if (
            intent?.getBooleanExtra(
                AvisoArmazenamento.EXTRA_ABRIR_ANALISE,
                false
            ) == true
        ) {
            analisarArmazenamento()
        }

        Anuncios.iniciar(this) {
            if (!isFinishing) {
                Anuncios.mostrarBanner(
                    this,
                    findViewById(R.id.bannerContainer)
                )
            }
        }
    }

    override fun onNewIntent(
        novo: Intent
    ) {

        super.onNewIntent(novo)

        if (
            novo.getBooleanExtra(
                AvisoArmazenamento.EXTRA_ABRIR_ANALISE,
                false
            )
        ) {
            analisarArmazenamento()
        }
    }

    // Android 13+: pede uma vez para mostrar o aviso de celular cheio
    private fun pedirPermissaoDeAviso() {

        if (android.os.Build.VERSION.SDK_INT < 33) {
            return
        }

        val prefs =
            getSharedPreferences("preferencias", MODE_PRIVATE)

        if (prefs.getBoolean("pediu_notificacao", false)) {
            return
        }

        if (
            checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        prefs.edit()
            .putBoolean("pediu_notificacao", true)
            .apply()

        requestPermissions(
            arrayOf(
                android.Manifest.permission.POST_NOTIFICATIONS
            ),
            PEDIDO_PERMISSAO_ARQUIVOS + 1
        )
    }

    private val aoMudarPremium: (Boolean) -> Unit = { ativo ->
        if (ativo) {
            Anuncios.esconderBanner(
                findViewById(R.id.bannerContainer)
            )
        }
    }

    override fun onResume() {

        super.onResume()

        atualizarCartaoNaHome()

        // Volta da tela de desinstalar: atualiza a lista
        if (
            ::fileScreen.isInitialized &&
            fileScreen.visibility == View.VISIBLE &&
            fileScreenTitle.text.toString() == "Aplicativos"
        ) {
            abrirAplicativos()
        }
    }

    private fun calcularTamanhoCache(): Int {

        val memoria =
            Runtime.getRuntime()
                .maxMemory() / 1024

        return (memoria / 8)
            .coerceAtMost(12 * 1024)
            .toInt()
    }

    private fun inicializarViews() {

        homeScroll =
            findViewById(R.id.homeScroll)

        fileScreen =
            findViewById(R.id.fileScreen)

        fileScreenTitle =
            findViewById(R.id.fileScreenTitle)

        currentPath =
            findViewById(R.id.currentPath)

        fileList =
            findViewById(R.id.fileList)

        mediaGrid =
            findViewById(R.id.mediaGrid)

        appTabs =
            findViewById(R.id.appTabs)

        barraAcoes =
            findViewById(R.id.barraAcoes)

        tabAppsNativos =
            findViewById(R.id.tabAppsNativos)

        tabAppsBaixados =
            findViewById(R.id.tabAppsBaixados)

        storageInfo =
            findViewById(R.id.storageInfo)

        storageProgress =
            findViewById(R.id.storageProgress)

        searchEdit =
            findViewById(R.id.searchEdit)
    }

    private fun configurarBotoes() {

        findViewById<View>(
            R.id.categoryStorage
        ).setOnClickListener {

            telaDePastasDeMidia = false
            pastaDeMidiaAtual = null

            abrirPasta(
                rootPath,
                "Armazenamento"
            )
        }

        findViewById<View>(
            R.id.categoryDownloads
        ).setOnClickListener {

            val pasta =
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )

            telaDePastasDeMidia = false
            pastaDeMidiaAtual = null

            abrirPasta(
                pasta,
                "Downloads"
            )
        }

        findViewById<View>(
            R.id.categoryImages
        ).setOnClickListener {

            abrirImagens()
        }

        findViewById<View>(
            R.id.categoryVideos
        ).setOnClickListener {

            abrirVideos()
        }

        findViewById<View>(
            R.id.categoryAudio
        ).setOnClickListener {

            abrirAudio()
        }

        findViewById<View>(
            R.id.categoryDocuments
        ).setOnClickListener {

            abrirDocumentos()
        }

        findViewById<View>(
            R.id.categoryApps
        ).setOnClickListener {

            abrirAplicativos()
        }

        findViewById<View>(
            R.id.categoryTrash
        ).setOnClickListener {

            telaDePastasDeMidia = false
            pastaDeMidiaAtual = null

            abrirLixeira()
        }

        findViewById<View>(
            R.id.categoryMemoryCard
        ).setOnClickListener {

            abrirCartao()
        }

        findViewById<View>(
            R.id.toolWhatsApp
        ).setOnClickListener {

            startActivity(
                Intent(this, LimpezaWhatsAppActivity::class.java)
            )
        }

        findViewById<View>(
            R.id.toolDuplicadas
        ).setOnClickListener {

            startActivity(
                Intent(this, DuplicadasActivity::class.java)
            )
        }

        findViewById<View>(
            R.id.toolCofre
        ).setOnClickListener {

            startActivity(
                Intent(this, CofreActivity::class.java)
            )
        }

        findViewById<View>(
            R.id.toolComprimir
        ).setOnClickListener {

            startActivity(
                Intent(this, CompressaoActivity::class.java)
            )
        }

        findViewById<View>(
            R.id.toolWifi
        ).setOnClickListener {

            startActivity(
                Intent(this, TransferenciaWifiActivity::class.java)
            )
        }

        findViewById<View>(
            R.id.toolPremium
        ).setOnClickListener {

            startActivity(
                Intent(this, PremiumActivity::class.java)
            )
        }

        findViewById<View>(
            R.id.menuButton
        ).setOnClickListener { botao ->

            mostrarMenuPrincipal(botao)
        }

        findViewById<View>(
            R.id.categoryAnalysis
        ).setOnClickListener {

            telaDePastasDeMidia = false
            pastaDeMidiaAtual = null

            analisarArmazenamento()
        }

        findViewById<View>(
            R.id.backButton
        ).setOnClickListener {

            voltar()
        }

        tabAppsNativos.setOnClickListener {

            mostrarAbaAplicativos(
                nativos = true
            )
        }

        tabAppsBaixados.setOnClickListener {

            mostrarAbaAplicativos(
                nativos = false
            )
        }
    }

    private fun configurarPesquisa() {

        searchEdit.addTextChangedListener(
            object : android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    val texto =
                        s?.toString()
                            ?.trim()
                            ?: ""

                    if (texto.length >= 2) {

                        pesquisarArquivos(
                            texto
                        )
                    }
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {
                }
            }
        )
    }

    private fun abrirPasta(
        pasta: File,
        titulo: String
    ) {

        if (!pasta.exists()) {

            Toast.makeText(
                this,
                "Pasta não encontrada",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        telaDePastasDeMidia = false
        pastaDeMidiaAtual = null

        currentDirectory = pasta

        homeScroll.visibility =
            View.GONE

        fileScreen.visibility =
            View.VISIBLE

        appTabs.visibility =
            View.GONE

        barraAcoes.visibility =
            View.GONE

        fileScreenTitle.text =
            titulo

        currentPath.text =
            pasta.absolutePath

        mediaGrid.visibility =
            View.GONE

        fileList.visibility =
            View.VISIBLE

        carregarArquivos(
            pasta
        )
    }

    // Em Downloads (e suas subpastas) as pastas vêm primeiro e
    // tudo aparece do mais recente para o mais antigo
    private fun estaEmDownloads(
        pasta: File
    ): Boolean {

        return try {

            val downloads =
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                ).canonicalPath

            val caminho =
                pasta.canonicalPath

            caminho == downloads ||
                caminho.startsWith(
                    downloads + File.separator
                )

        } catch (
            _: Exception
        ) {

            false
        }
    }

    private fun carregarArquivos(
        pasta: File
    ) {

        thread {

            val arquivos =
                try {
                    // A pasta interna do app (lixeira e cofre)
                    // não aparece na navegação
                    val lista =
                        pasta.listFiles()
                            ?.filter { it.name != PASTA_INTERNA }
                            ?: emptyList()

                    if (
                        estaEmDownloads(pasta)
                    ) {
                        lista.sortedWith(
                            compareBy<File> { !it.isDirectory }
                                .thenByDescending { it.lastModified() }
                        )
                    } else {
                        lista.sortedWith(
                            compareBy<File> { !it.isDirectory }
                                .thenBy {
                                    it.name.lowercase(
                                        Locale.getDefault()
                                    )
                                }
                        )
                    }
                } catch (
                    _: Exception
                ) {
                    emptyList()
                }

            runOnUiThread {

                val adapter =
                    FileListAdapter(
                        arquivos
                    )

                fileList.adapter =
                    adapter

                fileList.setOnItemClickListener {
                        _,
                        view,
                        position,
                        _ ->

                    if (
                        position < 0 ||
                        position >= arquivos.size
                    ) {
                        return@setOnItemClickListener
                    }

                    val arquivo =
                        arquivos[position]

                    if (
                        Armazenamento.estaNaLixeira(arquivo)
                    ) {

                        mostrarMenuLixeira(
                            arquivo,
                            view
                        )

                    } else if (
                        arquivo.isDirectory
                    ) {

                        abrirPasta(
                            arquivo,
                            arquivo.name
                        )

                    } else {

                        abrirArquivo(
                            arquivo
                        )
                    }
                }

                fileList.setOnItemLongClickListener {
                        _,
                        view,
                        position,
                        _ ->

                    if (
                        position < 0 ||
                        position >= arquivos.size
                    ) {
                        return@setOnItemLongClickListener false
                    }

                    val arquivo =
                        arquivos[position]

                    when {

                        Armazenamento.estaNaLixeira(arquivo) ->
                            mostrarMenuLixeira(arquivo, view)

                        arquivo.isDirectory ->
                            mostrarMenuPasta(arquivo, view)

                        else ->
                            mostrarMenuMidia(arquivo, view)
                    }

                    true
                }
            }
        }
    }

    // ============================================================
    // IMAGENS
    // ============================================================

    private fun abrirImagens() {

        abrirCategoriaDeMidia(
            "Imagens",
            TIPO_IMAGEM,
            listOf(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM
                ),
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                )
            )
        )
    }

    // ============================================================
    // VÍDEOS
    // ============================================================

    private fun abrirVideos() {

        abrirCategoriaDeMidia(
            "Vídeos",
            TIPO_VIDEO,
            listOf(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM
                ),
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MOVIES
                )
            )
        )
    }

    private fun abrirCategoriaDeMidia(
        titulo: String,
        tipos: Set<String>,
        pastas: List<File>
    ) {

        telaDePastasDeMidia = true
        pastaDeMidiaAtual = null

        tipoDeMidiaAtual = tipos
        categoriaMidiaAtual = titulo

        homeScroll.visibility =
            View.GONE

        fileScreen.visibility =
            View.VISIBLE

        appTabs.visibility =
            View.GONE

        barraAcoes.visibility =
            View.GONE

        fileScreenTitle.text =
            titulo

        currentPath.text =
            "Pastas de $titulo"

        fileList.visibility =
            View.GONE

        mediaGrid.visibility =
            View.VISIBLE

        buscarPastasDeMidia(
            tipos,
            pastas
        )
    }

    private fun abrirPastaDeMidia(
        pasta: File
    ) {

        telaDePastasDeMidia = false
        pastaDeMidiaAtual = pasta
        currentDirectory = pasta

        homeScroll.visibility =
            View.GONE

        fileScreen.visibility =
            View.VISIBLE

        appTabs.visibility =
            View.GONE

        barraAcoes.visibility =
            View.GONE

        fileScreenTitle.text =
            pasta.name

        currentPath.text =
            pasta.absolutePath

        fileList.visibility =
            View.GONE

        mediaGrid.visibility =
            View.VISIBLE

        buscarMidiasDaPasta(
            tipoDeMidiaAtual,
            pasta
        )
    }

    private fun buscarPastasDeMidia(
        tipos: Set<String>,
        pastas: List<File>
    ) {

        mediaGrid.adapter = null

        Toast.makeText(
            this,
            "Organizando por pasta...",
            Toast.LENGTH_SHORT
        ).show()

        thread {

            val grupos =
                LinkedHashMap<String, PastaMedia>()

            fun procurar(
                pasta: File
            ) {

                val lista =
                    try {
                        pasta.listFiles()
                    } catch (
                        _: Exception
                    ) {
                        null
                    }

                lista?.forEach { arquivo ->

                    if (arquivo.isDirectory) {

                        if (
                            arquivo.name == "Android" &&
                            pasta.absolutePath ==
                            rootPath.absolutePath
                        ) {
                            return@forEach
                        }

                        procurar(
                            arquivo
                        )

                    } else {

                        val extensao =
                            arquivo.extension
                                .lowercase(
                                    Locale.getDefault()
                                )

                        if (
                            tipos.contains(
                                extensao
                            )
                        ) {

                            val pai =
                                arquivo.parentFile
                                    ?: return@forEach

                            val chave =
                                pai.absolutePath

                            val grupo =
                                grupos[chave]

                            if (grupo == null) {

                                grupos[chave] =
                                    PastaMedia(
                                        pai,
                                        arrayListOf(
                                            arquivo
                                        )
                                    )

                            } else {

                                grupo.arquivos.add(
                                    arquivo
                                )
                            }
                        }
                    }
                }
            }

            pastas.forEach { pasta ->

                if (pasta.exists()) {

                    procurar(
                        pasta
                    )
                }
            }

            val resultado =
                grupos.values
                    .sortedBy {
                        it.pasta.name.lowercase(
                            Locale.getDefault()
                        )
                    }

            runOnUiThread {

                currentPath.text =
                    "${resultado.size} pasta(s)"

                mediaGrid.adapter =
                    PastaMediaAdapter(
                        resultado
                    )

                mediaGrid.setOnItemClickListener {
                        _,
                        _,
                        position,
                        _ ->

                    if (
                        position >= 0 &&
                        position < resultado.size
                    ) {

                        abrirPastaDeMidia(
                            resultado[position].pasta
                        )
                    }
                }

                mediaGrid.setOnItemLongClickListener {
                        _,
                        view,
                        position,
                        _ ->

                    if (
                        position >= 0 &&
                        position < resultado.size
                    ) {

                        mostrarMenuPasta(
                            resultado[position].pasta,
                            view
                        )

                        true

                    } else {

                        false
                    }
                }
            }
        }
    }

    private fun buscarMidiasDaPasta(
        tipos: Set<String>,
        pasta: File
    ) {

        mediaGrid.adapter = null

        thread {

            val resultado =
                ArrayList<File>()

            val lista =
                try {

                    pasta.listFiles()
                        ?.filter {
                            it.isFile &&
                                    tipos.contains(
                                        it.extension.lowercase(
                                            Locale.getDefault()
                                        )
                                    )
                        }
                        ?.sortedByDescending {
                            it.lastModified()
                        }

                } catch (
                    _: Exception
                ) {

                    null
                }

            if (lista != null) {

                resultado.addAll(
                    lista
                )
            }

            runOnUiThread {

                currentPath.text =
                    if (tipos == TIPO_VIDEO) {
                        "${resultado.size} vídeo(s)"
                    } else {
                        "${resultado.size} imagem(ns)"
                    }

                mediaGrid.adapter =
                    MediaAdapter(
                        resultado
                    )

                mediaGrid.setOnItemClickListener {
                        _,
                        _,
                        position,
                        _ ->

                    if (
                        position >= 0 &&
                        position < resultado.size
                    ) {

                        abrirMidiaDaPasta(
                            resultado,
                            position
                        )
                    }
                }

                mediaGrid.setOnItemLongClickListener {
                        _,
                        view,
                        position,
                        _ ->

                    if (
                        position >= 0 &&
                        position < resultado.size
                    ) {

                        mostrarMenuMidia(
                            resultado[position],
                            view
                        )

                        true

                    } else {

                        false
                    }
                }
            }
        }
    }

    // ============================================================
    // ABRIR MÍDIA DA PASTA
    // ============================================================

    private fun abrirMidiaDaPasta(
        arquivos: ArrayList<File>,
        posicao: Int
    ) {

        if (
            posicao < 0 ||
            posicao >= arquivos.size
        ) {
            return
        }

        val arquivo =
            arquivos[posicao]

        if (
            !arquivo.exists() ||
            !arquivo.isFile
        ) {

            Toast.makeText(
                this,
                "Arquivo não encontrado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val extensao =
            arquivo.extension.lowercase(
                Locale.getDefault()
            )

        val caminhos =
            ArrayList<String>()

        arquivos.forEach {

            caminhos.add(
                it.absolutePath
            )
        }

        when {

            TIPO_IMAGEM.contains(
                extensao
            ) -> {

                try {

                    val intent =
                        Intent(
                            this,
                            ImageViewerActivity::class.java
                        )

                    intent.putExtra(
                        "arquivo",
                        arquivo.absolutePath
                    )

                    intent.putStringArrayListExtra(
                        "arquivos",
                        caminhos
                    )

                    intent.putExtra(
                        "posicao",
                        posicao
                    )

                    startActivity(
                        intent
                    )

                } catch (
                    e: Exception
                ) {

                    Toast.makeText(
                        this,
                        "Erro ao abrir imagem: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            TIPO_VIDEO.contains(
                extensao
            ) -> {

                try {

                    val intent =
                        Intent(
                            this,
                            VideoViewerActivity::class.java
                        )

                    intent.putExtra(
                        "arquivo",
                        arquivo.absolutePath
                    )

                    intent.putStringArrayListExtra(
                        "arquivos",
                        caminhos
                    )

                    intent.putExtra(
                        "posicao",
                        posicao
                    )

                    startActivity(
                        intent
                    )

                } catch (
                    e: Exception
                ) {

                    Toast.makeText(
                        this,
                        "Erro ao abrir vídeo: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            else -> {

                abrirArquivo(
                    arquivo
                )
            }
        }
    }

    // ============================================================
    // ÁUDIO
    // ============================================================

    private fun abrirAudio() {

        telaDePastasDeMidia = false
        pastaDeMidiaAtual = null

        val musicas =
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC
            )

        val gravacoes =
            File(
                rootPath,
                "Recordings"
            )

        abrirListaPorExtensao(
            "Áudio",
            listOf(
                musicas,
                gravacoes
            ),
            TIPO_AUDIO
        )
    }

    // ============================================================
    // DOCUMENTOS
    // ============================================================

    private fun abrirDocumentos() {

        telaDePastasDeMidia = false
        pastaDeMidiaAtual = null

        abrirListaPorExtensao(
            "Documentos",
            listOf(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS
                ),
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
            ),
            TIPO_DOCUMENTO
        )
    }

    private fun abrirListaPorExtensao(
        titulo: String,
        pastas: List<File>,
        tipos: Set<String>
    ) {

        homeScroll.visibility =
            View.GONE

        fileScreen.visibility =
            View.VISIBLE

        appTabs.visibility =
            View.GONE

        barraAcoes.visibility =
            View.GONE

        fileScreenTitle.text =
            titulo

        currentPath.text =
            "Buscando arquivos..."

        mediaGrid.visibility =
            View.GONE

        fileList.visibility =
            View.VISIBLE

        thread {

            val resultado =
                ArrayList<File>()

            fun procurar(
                pasta: File
            ) {

                if (
                    resultado.size >= 500
                ) {
                    return
                }

                val lista =
                    try {
                        pasta.listFiles()
                    } catch (
                        _: Exception
                    ) {
                        null
                    }

                lista?.forEach { arquivo ->

                    if (
                        resultado.size >= 500
                    ) {
                        return@forEach
                    }

                    if (
                        arquivo.isDirectory
                    ) {

                        procurar(
                            arquivo
                        )

                    } else {

                        val extensao =
                            arquivo.extension
                                .lowercase(
                                    Locale.getDefault()
                                )

                        if (
                            tipos.contains(
                                extensao
                            )
                        ) {

                            resultado.add(
                                arquivo
                            )
                        }
                    }
                }
            }

            pastas.forEach { pasta ->

                if (pasta.exists()) {
                    procurar(pasta)
                }
            }

            resultado.sortBy {
                it.name.lowercase(
                    Locale.getDefault()
                )
            }

            runOnUiThread {

                currentPath.text =
                    "${resultado.size} arquivo(s)"

                fileList.adapter =
                    FileListAdapter(
                        resultado
                    )

                fileList.setOnItemClickListener {
                        _,
                        _,
                        position,
                        _ ->

                    if (
                        position >= 0 &&
                        position < resultado.size
                    ) {

                        abrirArquivo(
                            resultado[position]
                        )
                    }
                }

                fileList.setOnItemLongClickListener {
                        _,
                        view,
                        position,
                        _ ->

                    if (
                        position >= 0 &&
                        position < resultado.size
                    ) {

                        mostrarMenuMidia(
                            resultado[position],
                            view
                        )

                        true

                    } else {

                        false
                    }
                }
            }
        }
    }

    // ============================================================
    // APLICATIVOS
    // ============================================================

    private fun abrirAplicativos() {

        telaDePastasDeMidia = false
        pastaDeMidiaAtual = null

        homeScroll.visibility =
            View.GONE

        fileScreen.visibility =
            View.VISIBLE

        appTabs.visibility =
            View.GONE

        barraAcoes.visibility =
            View.GONE

        fileScreenTitle.text =
            "Aplicativos"

        currentPath.text =
            "Carregando aplicativos instalados..."

        mediaGrid.visibility =
            View.GONE

        fileList.visibility =
            View.VISIBLE

        thread {

            val pm =
                packageManager

            val aplicativos =
                try {

                    // Apps com ícone na tela inicial (não precisa
                    // da permissão QUERY_ALL_PACKAGES)
                    pm.queryIntentActivities(
                        Intent(Intent.ACTION_MAIN)
                            .addCategory(Intent.CATEGORY_LAUNCHER),
                        0
                    )
                        .map { it.activityInfo.applicationInfo }
                        .filter { aplicativo ->

                            aplicativo.packageName != packageName
                        }
                        .map { aplicativo ->

                            AppInfoItem(
                                aplicativo,
                                pm.getApplicationLabel(
                                    aplicativo
                                ).toString()
                            )
                        }
                        .distinctBy {
                            it.info.packageName
                        }
                        .sortedBy {
                            it.nome.lowercase(
                                Locale.getDefault()
                            )
                        }

                } catch (
                    _: Exception
                ) {

                    emptyList()
                }

            val (nativos, baixados) =
                aplicativos.partition {
                    ehAplicativoNativo(it.info)
                }

            runOnUiThread {

                if (
                    fileScreen.visibility != View.VISIBLE ||
                    fileScreenTitle.text.toString() != "Aplicativos"
                ) {
                    return@runOnUiThread
                }

                aplicativosNativos =
                    nativos

                aplicativosBaixados =
                    baixados

                appTabs.visibility =
                    View.VISIBLE

                mostrarAbaAplicativos(
                    abaAppsNativos
                )
            }
        }
    }

    // Apps que vêm com o celular (do sistema ou do fabricante),
    // inclusive os que já receberam atualização pela loja
    private fun ehAplicativoNativo(
        info: ApplicationInfo
    ): Boolean {

        return (
            info.flags and
                (ApplicationInfo.FLAG_SYSTEM or
                    ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)
            ) != 0
    }

    private fun mostrarAbaAplicativos(
        nativos: Boolean
    ) {

        abaAppsNativos =
            nativos

        val aplicativos =
            if (nativos) {
                aplicativosNativos
            } else {
                aplicativosBaixados
            }

        tabAppsNativos.setTextColor(
            if (nativos) Color.parseColor("#1E88E5")
            else Color.parseColor("#777777")
        )

        tabAppsBaixados.setTextColor(
            if (nativos) Color.parseColor("#777777")
            else Color.parseColor("#1E88E5")
        )

        currentPath.text =
            if (nativos) {
                "${aplicativos.size} aplicativos nativos"
            } else {
                "${aplicativos.size} aplicativos baixados"
            }

        fileList.adapter =
            AppListAdapter(
                aplicativos
            )

        fileList.setOnItemClickListener {
                _,
                _,
                position,
                _ ->

            if (
                position < 0 ||
                position >= aplicativos.size
            ) {
                return@setOnItemClickListener
            }

            abrirAplicativo(
                aplicativos[position]
            )
        }

        fileList.setOnItemLongClickListener {
                _,
                view,
                position,
                _ ->

            if (
                position >= 0 &&
                position < aplicativos.size
            ) {

                mostrarMenuAplicativo(
                    aplicativos[position],
                    view
                )

                true

            } else {

                false
            }
        }
    }

    private fun abrirAplicativo(
        aplicativo: AppInfoItem
    ) {

        try {

            val intent =
                packageManager.getLaunchIntentForPackage(
                    aplicativo.info.packageName
                )

            if (intent != null) {

                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                startActivity(
                    intent
                )

            } else {

                Toast.makeText(
                    this,
                    "Não foi possível abrir o aplicativo",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "Erro ao abrir aplicativo: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun mostrarMenuAplicativo(
        aplicativo: AppInfoItem,
        ancora: View
    ) {

        val menu =
            LinearLayout(this)

        MenuEscuro.prepararMenu(
            menu
        )

        adicionarOpcaoMenu(
            menu,
            "↗",
            "Abrir"
        ) {

            abrirAplicativo(
                aplicativo
            )
        }

        adicionarOpcaoMenu(
            menu,
            "ⓘ",
            "Informações"
        ) {

            try {

                val intent =
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    )

                intent.data =
                    Uri.parse(
                        "package:${aplicativo.info.packageName}"
                    )

                startActivity(
                    intent
                )

            } catch (
                _: Exception
            ) {

                Toast.makeText(
                    this,
                    "Não foi possível abrir as informações",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Apps que vêm com o celular não podem ser desinstalados
        if (!ehAplicativoNativo(aplicativo.info)) {

            adicionarOpcaoMenu(
                menu,
                "🗑",
                "Desinstalar"
            ) {

                try {

                    startActivity(
                        Intent(
                            Intent.ACTION_DELETE,
                            Uri.parse(
                                "package:${aplicativo.info.packageName}"
                            )
                        )
                    )

                } catch (
                    _: Exception
                ) {

                    Toast.makeText(
                        this,
                        "Não foi possível desinstalar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        mostrarPopup(
            menu,
            ancora
        )
    }

    // ============================================================
    // LIXEIRA
    // ============================================================

    private fun abrirLixeira() {

        val pasta =
            Armazenamento.pastaLixeira

        if (!pasta.exists()) {
            pasta.mkdirs()
        }

        abrirPasta(
            pasta,
            "Lixeira"
        )

        mostrarBarraLixeira()
    }

    private fun mostrarBarraLixeira() {

        barraAcoes.removeAllViews()

        barraAcoes.visibility =
            View.VISIBLE

        barraAcoes.addView(
            criarTextoAjuda(
                "Toque em um item para restaurar ou excluir de vez."
            )
        )

        val botoes =
            LinearLayout(this)

        botoes.orientation =
            LinearLayout.HORIZONTAL

        botoes.addView(
            criarBotaoAcao(
                "Restaurar tudo",
                R.drawable.ic_acao_restaurar,
                Color.rgb(30, 136, 229)
            ) {
                confirmarRestaurarTudo()
            },
            LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                marginEnd = dp(6)
            }
        )

        botoes.addView(
            criarBotaoAcao(
                "Esvaziar lixeira",
                R.drawable.ic_acao_lixeira,
                Color.rgb(229, 57, 53)
            ) {
                confirmarEsvaziarLixeira()
            },
            LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                marginStart = dp(6)
            }
        )

        barraAcoes.addView(
            botoes,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        )
    }

    private fun confirmarEsvaziarLixeira() {

        val itens =
            Armazenamento.itensDaLixeira()

        if (itens.isEmpty()) {

            Toast.makeText(
                this,
                "A lixeira já está vazia",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        AlertDialog.Builder(this)
            .setTitle("Esvaziar lixeira?")
            .setMessage(
                "${itens.size} item(ns) serão apagados para sempre. " +
                    "Isso não pode ser desfeito."
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Esvaziar") { _, _ ->

                thread {

                    val apagados =
                        Armazenamento.esvaziarLixeira(this)

                    runOnUiThread {

                        Toast.makeText(
                            this,
                            "$apagados item(ns) apagados",
                            Toast.LENGTH_SHORT
                        ).show()

                        Anuncios.mostrarAposLimpeza(this)

                        abrirLixeira()
                    }
                }
            }
            .show()
    }

    private fun confirmarRestaurarTudo() {

        val itens =
            Armazenamento.itensDaLixeira()

        if (itens.isEmpty()) {

            Toast.makeText(
                this,
                "A lixeira está vazia",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        AlertDialog.Builder(this)
            .setTitle("Restaurar tudo?")
            .setMessage(
                "${itens.size} item(ns) voltarão para as pastas de origem."
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Restaurar") { _, _ ->

                thread {

                    val restaurados =
                        itens.count {
                            Armazenamento.restaurar(this, it) != null
                        }

                    runOnUiThread {

                        Toast.makeText(
                            this,
                            "$restaurados item(ns) restaurados",
                            Toast.LENGTH_SHORT
                        ).show()

                        abrirLixeira()
                    }
                }
            }
            .show()
    }

    private fun mostrarMenuLixeira(
        arquivo: File,
        ancora: View
    ) {

        val menu =
            LinearLayout(this)

        MenuEscuro.prepararMenu(
            menu
        )

        adicionarOpcaoMenu(
            menu,
            "↺",
            "Restaurar"
        ) {

            val destino =
                Armazenamento.restaurar(
                    this,
                    arquivo
                )

            Toast.makeText(
                this,
                if (destino != null) {
                    "Restaurado em ${destino.parentFile?.name ?: ""}"
                } else {
                    "Não foi possível restaurar"
                },
                Toast.LENGTH_SHORT
            ).show()

            abrirLixeira()
        }

        adicionarOpcaoMenu(
            menu,
            "ⓘ",
            "Informações"
        ) {

            mostrarInformacoes(
                arquivo
            )
        }

        adicionarOpcaoMenu(
            menu,
            "🗑",
            "Excluir definitivamente"
        ) {

            AlertDialog.Builder(this)
                .setTitle("Excluir definitivamente?")
                .setMessage(
                    "\"${arquivo.name}\" será apagado para sempre."
                )
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir") { _, _ ->

                    if (
                        Armazenamento.excluirDefinitivamente(
                            this,
                            arquivo
                        )
                    ) {

                        Toast.makeText(
                            this,
                            "Excluído",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        Toast.makeText(
                            this,
                            "Não foi possível excluir",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    abrirLixeira()
                }
                .show()
        }

        mostrarPopup(
            menu,
            ancora
        )
    }

    // ============================================================
    // CARTÃO DE MEMÓRIA
    // ============================================================

    // O cartão só aparece na tela inicial se estiver no celular
    private fun atualizarCartaoNaHome() {

        val botao =
            findViewById<View>(
                R.id.categoryMemoryCard
            ) ?: return

        botao.visibility =
            if (Armazenamento.raizCartao(this) != null) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
    }

    private fun abrirCartao() {

        val cartao =
            Armazenamento.raizCartao(this)

        if (cartao == null) {

            Toast.makeText(
                this,
                "Nenhum cartão de memória encontrado",
                Toast.LENGTH_SHORT
            ).show()

            atualizarCartaoNaHome()

            return
        }

        telaDePastasDeMidia = false
        pastaDeMidiaAtual = null

        abrirPasta(
            cartao,
            "Cartão de memória"
        )

        mostrarBarraCartao(
            cartao
        )
    }

    private fun mostrarBarraCartao(
        cartao: File
    ) {

        barraAcoes.removeAllViews()

        barraAcoes.visibility =
            View.VISIBLE

        val espaco =
            Armazenamento.espaco(cartao)

        val titulo =
            TextView(this)

        titulo.textSize =
            16f

        titulo.setTextColor(
            COR_TEXTO_PRINCIPAL
        )

        titulo.typeface =
            android.graphics.Typeface.DEFAULT_BOLD

        titulo.text =
            if (espaco != null) {
                "${formatarBytes(espaco.usado)} usados de " +
                    "${formatarBytes(espaco.total)} • " +
                    "${formatarBytes(espaco.livre)} livres"
            } else {
                "Espaço não disponível"
            }

        barraAcoes.addView(titulo)

        val barra =
            ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
            )

        barra.max = 100

        barra.progress =
            if (espaco != null && espaco.total > 0) {
                (espaco.usado * 100 / espaco.total).toInt()
            } else {
                0
            }

        barraAcoes.addView(
            barra,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(8)
            ).apply {
                topMargin = dp(6)
            }
        )

        // Quanto cada tipo ocupa no cartão (calculado em segundo plano)
        val detalhes =
            criarTextoAjuda(
                "Calculando o que tem no cartão..."
            )

        barraAcoes.addView(detalhes)

        thread {

            val porTipo =
                tamanhosPorCategoria(cartao)

            runOnUiThread {

                detalhes.text =
                    porTipo
                        .filter { it.value > 0 }
                        .entries
                        .joinToString("   •   ") {
                            "${it.key}: ${formatarBytes(it.value)}"
                        }
                        .ifEmpty { "Cartão vazio" }
            }
        }

        barraAcoes.addView(
            criarBotaoAcao(
                "Formatar cartão",
                R.drawable.ic_acao_lixeira,
                Color.rgb(229, 57, 53)
            ) {
                mostrarOpcoesFormatar(cartao)
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
            ).apply {
                topMargin = dp(8)
            }
        )

        barraAcoes.addView(
            criarTextoAjuda(
                "Segure um arquivo ou pasta para mover entre o cartão e o celular."
            )
        )
    }

    /*
     * Formatar de verdade (escolher FAT32/exFAT) só o Android
     * pode fazer. Por isso:
     *  - "Formatar (apagar tudo)": o app apaga todos os arquivos
     *  - "Formatar FAT32 pelo sistema": abre a tela do Android.
     *    O Android usa FAT32 em cartões de até 32 GB e exFAT nos
     *    maiores.
     */
    private fun mostrarOpcoesFormatar(
        cartao: File
    ) {

        AlertDialog.Builder(this)
            .setTitle("Formatar cartão de memória")
            .setItems(
                arrayOf(
                    "Formatar normal (apagar tudo do cartão)",
                    "Formatar FAT32 pelo sistema do Android"
                )
            ) { _, qual ->

                when (qual) {

                    0 ->
                        confirmarApagarCartao(cartao)

                    1 ->
                        abrirConfiguracoesDoCartao()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarApagarCartao(
        cartao: File
    ) {

        AlertDialog.Builder(this)
            .setTitle("Apagar TUDO do cartão?")
            .setMessage(
                "Todas as fotos, vídeos, músicas e arquivos do cartão " +
                    "serão apagados para sempre. Isso não pode ser desfeito."
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Continuar") { _, _ ->

                AlertDialog.Builder(this)
                    .setTitle("Tem certeza?")
                    .setMessage(
                        "Última confirmação: apagar tudo do cartão de memória."
                    )
                    .setNegativeButton("Não", null)
                    .setPositiveButton("Sim, apagar tudo") { _, _ ->

                        Toast.makeText(
                            this,
                            "Formatando o cartão...",
                            Toast.LENGTH_SHORT
                        ).show()

                        thread {

                            var falhas = 0

                            cartao.listFiles()?.forEach { item ->

                                // A pasta Android do cartão é do sistema
                                if (item.name == "Android") {
                                    return@forEach
                                }

                                if (!item.deleteRecursively()) {
                                    falhas++
                                }
                            }

                            runOnUiThread {

                                Toast.makeText(
                                    this,
                                    if (falhas == 0) {
                                        "Cartão formatado"
                                    } else {
                                        "Alguns itens não puderam ser apagados ($falhas)"
                                    },
                                    Toast.LENGTH_LONG
                                ).show()

                                abrirCartao()
                            }
                        }
                    }
                    .show()
            }
            .show()
    }

    private fun abrirConfiguracoesDoCartao() {

        val tentativas =
            listOf(
                Intent(Settings.ACTION_MEMORY_CARD_SETTINGS),
                Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
            )

        for (intent in tentativas) {

            try {

                startActivity(intent)

                Toast.makeText(
                    this,
                    "Escolha o cartão SD e toque em Formatar",
                    Toast.LENGTH_LONG
                ).show()

                return

            } catch (
                _: Exception
            ) {
            }
        }

        Toast.makeText(
            this,
            "Não foi possível abrir as configurações",
            Toast.LENGTH_SHORT
        ).show()
    }

    // Opção "Mover para o cartão" / "Mover para o celular"
    private fun adicionarOpcoesCartao(
        menu: LinearLayout,
        arquivo: File
    ) {

        val cartao =
            Armazenamento.raizCartao(this) ?: return

        val noCartao =
            Armazenamento.estaNoCartao(this, arquivo)

        adicionarOpcaoMenu(
            menu,
            "→",
            if (noCartao) "Mover para o celular" else "Mover para o cartão"
        ) {

            val destinoPasta =
                if (noCartao) {
                    Armazenamento.pastaEquivalente(
                        arquivo,
                        cartao,
                        rootPath
                    )
                } else {
                    Armazenamento.pastaEquivalente(
                        arquivo,
                        rootPath,
                        cartao
                    )
                }

            moverEntreArmazenamentos(
                arquivo,
                destinoPasta,
                if (noCartao) "celular" else "cartão"
            )
        }
    }

    private fun moverEntreArmazenamentos(
        arquivo: File,
        destinoPasta: File,
        nomeDestino: String
    ) {

        Toast.makeText(
            this,
            "Movendo para o $nomeDestino...",
            Toast.LENGTH_SHORT
        ).show()

        thread {

            val ok =
                try {

                    destinoPasta.mkdirs()

                    Armazenamento.mover(
                        arquivo,
                        Armazenamento.nomeLivre(
                            destinoPasta,
                            arquivo.name
                        )
                    )

                } catch (
                    _: Exception
                ) {
                    false
                }

            runOnUiThread {

                Toast.makeText(
                    this,
                    if (ok) {
                        "Movido para o $nomeDestino"
                    } else {
                        "Não foi possível mover para o $nomeDestino"
                    },
                    Toast.LENGTH_LONG
                ).show()

                recarregarTelaAtual()
            }
        }
    }

    // ============================================================
    // MENU PRINCIPAL (⋮)
    // ============================================================

    private fun mostrarMenuPrincipal(
        ancora: View
    ) {

        val menu =
            LinearLayout(this)

        MenuEscuro.prepararMenu(
            menu
        )

        adicionarOpcaoMenu(
            menu,
            "★",
            if (Premium.ativo(this)) "Premium ativo ⭐" else "Faxina Premium"
        ) {
            startActivity(
                Intent(this, PremiumActivity::class.java)
            )
        }

        adicionarOpcaoMenu(
            menu,
            "A",
            if (ModoSimples.ativo(this)) "Letras grandes: ligado"
            else "Letras grandes: desligado"
        ) {

            ModoSimples.definir(
                this,
                !ModoSimples.ativo(this)
            )

            // Recria a tela com o novo tamanho de letra
            recreate()
        }

        adicionarOpcaoMenu(
            menu,
            "!",
            if (AvisoArmazenamento.ativo(this)) "Avisar celular cheio: ligado"
            else "Avisar celular cheio: desligado"
        ) {

            val ligar =
                !AvisoArmazenamento.ativo(this)

            AvisoArmazenamento.definir(
                this,
                ligar
            )

            Toast.makeText(
                this,
                if (ligar) "Vamos avisar quando o celular estiver quase cheio"
                else "Aviso de celular cheio desligado",
                Toast.LENGTH_SHORT
            ).show()
        }

        adicionarOpcaoMenu(
            menu,
            "ⓘ",
            "Política de privacidade"
        ) {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(URL_POLITICA_PRIVACIDADE)
                    )
                )
            } catch (
                _: Exception
            ) {
                Toast.makeText(
                    this,
                    "Não foi possível abrir o navegador",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        mostrarPopup(
            menu,
            ancora
        )
    }

    // ============================================================
    // COFRE
    // ============================================================

    private fun moverParaCofre(
        arquivo: File
    ) {

        if (!Premium.ativo(this)) {

            Toast.makeText(
                this,
                "O Cofre faz parte do Premium",
                Toast.LENGTH_SHORT
            ).show()

            startActivity(
                Intent(this, PremiumActivity::class.java)
            )

            return
        }

        if (!Cofre.temPin(this)) {

            Toast.makeText(
                this,
                "Abra o Cofre e crie um PIN primeiro",
                Toast.LENGTH_LONG
            ).show()

            startActivity(
                Intent(this, CofreActivity::class.java)
            )

            return
        }

        if (Cofre.guardar(this, arquivo)) {

            Toast.makeText(
                this,
                "Guardado no cofre 🔒",
                Toast.LENGTH_SHORT
            ).show()

            recarregarTelaAtual()

        } else {

            Toast.makeText(
                this,
                "Não foi possível guardar no cofre",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================================
    // BOTÕES E TEXTOS DA BARRA DE AÇÕES
    // ============================================================

    private fun criarBotaoAcao(
        texto: String,
        iconeRes: Int,
        cor: Int,
        acao: () -> Unit
    ): View {

        val botao =
            LinearLayout(this)

        botao.orientation =
            LinearLayout.HORIZONTAL

        botao.gravity =
            Gravity.CENTER

        val fundo =
            android.graphics.drawable.GradientDrawable()

        fundo.setColor(cor)

        fundo.cornerRadius =
            dp(23).toFloat()

        botao.background =
            fundo

        botao.isClickable =
            true

        val icone =
            ImageView(this)

        icone.setImageResource(
            iconeRes
        )

        icone.setColorFilter(
            Color.WHITE
        )

        botao.addView(
            icone,
            LinearLayout.LayoutParams(
                dp(20),
                dp(20)
            )
        )

        val nome =
            TextView(this)

        nome.text =
            texto

        nome.textSize =
            15f

        nome.setTextColor(
            Color.WHITE
        )

        nome.typeface =
            android.graphics.Typeface.DEFAULT_BOLD

        botao.addView(
            nome,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dp(8)
            }
        )

        botao.setOnClickListener {
            acao()
        }

        return botao
    }

    private fun criarTextoAjuda(
        texto: String
    ): TextView {

        val ajuda =
            TextView(this)

        ajuda.text =
            texto

        ajuda.textSize =
            14f

        ajuda.setTextColor(
            COR_TEXTO_SECUNDARIO
        )

        ajuda.setPadding(
            0,
            dp(6),
            0,
            0
        )

        return ajuda
    }

    private fun dp(
        valor: Int
    ): Int =
        (valor * resources.displayMetrics.density).toInt()

    // ============================================================
    // PESQUISA
    // ============================================================

    private fun pesquisarArquivos(
        texto: String
    ) {

        telaDePastasDeMidia = false
        pastaDeMidiaAtual = null

        homeScroll.visibility =
            View.GONE

        fileScreen.visibility =
            View.VISIBLE

        appTabs.visibility =
            View.GONE

        barraAcoes.visibility =
            View.GONE

        fileScreenTitle.text =
            "Pesquisa"

        currentPath.text =
            "Resultados para: $texto"

        mediaGrid.visibility =
            View.GONE

        fileList.visibility =
            View.VISIBLE

        thread {

            val resultado =
                ArrayList<File>()

            fun procurar(
                pasta: File
            ) {

                if (
                    resultado.size >= 300
                ) {
                    return
                }

                val lista =
                    try {
                        pasta.listFiles()
                    } catch (
                        _: Exception
                    ) {
                        null
                    }

                lista?.forEach { arquivo ->

                    if (
                        resultado.size >= 300
                    ) {
                        return@forEach
                    }

                    if (
                        arquivo.name.contains(
                            texto,
                            ignoreCase = true
                        )
                    ) {

                        resultado.add(
                            arquivo
                        )
                    }

                    if (
                        arquivo.isDirectory &&
                        arquivo.name != PASTA_INTERNA
                    ) {

                        procurar(
                            arquivo
                        )
                    }
                }
            }

            procurar(rootPath)

            runOnUiThread {

                fileList.adapter =
                    FileListAdapter(
                        resultado
                    )

                fileList.setOnItemClickListener {
                        _,
                        _,
                        position,
                        _ ->

                    if (
                        position < 0 ||
                        position >= resultado.size
                    ) {
                        return@setOnItemClickListener
                    }

                    val arquivo =
                        resultado[position]

                    if (
                        arquivo.isDirectory
                    ) {

                        abrirPasta(
                            arquivo,
                            arquivo.name
                        )

                    } else {

                        abrirArquivo(
                            arquivo
                        )
                    }
                }
            }
        }
    }

    // ============================================================
    // ABERTURA DE ARQUIVOS
    // ============================================================

    private fun abrirArquivo(
        arquivo: File
    ) {

        if (
            !arquivo.exists() ||
            !arquivo.isFile
        ) {

            Toast.makeText(
                this,
                "Arquivo não encontrado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val extensao =
            arquivo.extension
                .lowercase(
                    Locale.getDefault()
                )

        when {
                        TIPO_AUDIO.contains(extensao) -> {
                try {
                    val intent = Intent(
                        this,
                        AudioViewerActivity::class.java
                    )

                    intent.putExtra(
                        "filePath",
                        arquivo.absolutePath
                    )

                    startActivity(intent)

                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Erro ao reproduzir áudio: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            TIPO_IMAGEM.contains(extensao) -> {

                abrirVisualizadorImagem(
                    arquivo
                )
            }

            TIPO_VIDEO.contains(extensao) -> {

                abrirVisualizadorVideo(
                    arquivo
                )
            }

            extensao == "pdf" -> {

                abrirVisualizadorPdf(
                    arquivo
                )
            }

            extensao == "zip" -> {

                abrirVisualizadorZip(
                    arquivo
                )
            }

            else -> {

                abrirComAplicativo(
                    arquivo
                )
            }
        }
    }

    private fun abrirVisualizadorImagem(
        arquivo: File
    ) {

        try {

            val intent =
                Intent(
                    this,
                    ImageViewerActivity::class.java
                )

            intent.putExtra(
                "arquivo",
                arquivo.absolutePath
            )

            startActivity(intent)

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "Erro ao abrir imagem: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun abrirVisualizadorVideo(
        arquivo: File
    ) {

        try {

            val intent =
                Intent(
                    this,
                    VideoViewerActivity::class.java
                )

            intent.putExtra(
                "arquivo",
                arquivo.absolutePath
            )

            startActivity(intent)

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "Erro ao abrir vídeo: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun abrirVisualizadorPdf(
        arquivo: File
    ) {

        try {

            val intent =
                Intent(
                    this,
                    PdfViewerActivity::class.java
                )

            intent.putExtra(
                "arquivo",
                arquivo.absolutePath
            )

            startActivity(intent)

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "Erro ao abrir PDF: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun abrirVisualizadorZip(
        arquivo: File
    ) {

        try {

            val intent =
                Intent(
                    this,
                    ZipViewerActivity::class.java
                )

            intent.putExtra(
                "arquivo",
                arquivo.absolutePath
            )

            startActivity(intent)

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "Erro ao abrir ZIP: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun abrirComAplicativo(
        arquivo: File
    ) {

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
                obterMimeType(arquivo)
            )

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            startActivity(
                Intent.createChooser(
                    intent,
                    "Abrir com"
                )
            )

        } catch (
            _: ActivityNotFoundException
        ) {

            Toast.makeText(
                this,
                "Nenhum aplicativo pode abrir este arquivo",
                Toast.LENGTH_LONG
            ).show()

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "Não foi possível abrir: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun obterMimeType(
        arquivo: File
    ): String {

        return when (
            arquivo.extension.lowercase(
                Locale.getDefault()
            )
        ) {

            "txt" -> "text/plain"

            "html",
            "htm" -> "text/html"

            "jpg",
            "jpeg" -> "image/jpeg"

            "png" -> "image/png"

            "gif" -> "image/gif"

            "webp" -> "image/webp"

            "heic",
            "heif" -> "image/heif"

            "mp3" -> "audio/mpeg"

            "wav" -> "audio/wav"

            "ogg",
            "opus" -> "audio/ogg"

            "m4a" -> "audio/mp4"

            "aac" -> "audio/aac"

            "flac" -> "audio/flac"

            "amr" -> "audio/amr"

            "mp4",
            "m4v" -> "video/mp4"

            "mkv" -> "video/x-matroska"

            "avi" -> "video/x-msvideo"

            "mov" -> "video/quicktime"

            "3gp" -> "video/3gpp"

            "webm" -> "video/webm"

            "pdf" -> "application/pdf"

            "zip" -> "application/zip"

            "rar" -> "application/vnd.rar"

            "7z" -> "application/x-7z-compressed"

            "doc" ->
                "application/msword"

            "docx" ->
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

            "xls" ->
                "application/vnd.ms-excel"

            "xlsx" ->
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

            "ppt" ->
                "application/vnd.ms-powerpoint"

            "pptx" ->
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"

            "csv" ->
                "text/csv"

            "rtf" ->
                "application/rtf"

            "odt" ->
                "application/vnd.oasis.opendocument.text"

            "ods" ->
                "application/vnd.oasis.opendocument.spreadsheet"

            "odp" ->
                "application/vnd.oasis.opendocument.presentation"

            else -> "*/*"
        }
    }

    // ============================================================
    // ARMAZENAMENTO
    // ============================================================

    private fun atualizarArmazenamento() {

        thread {

            try {

                val stat =
                    android.os.StatFs(
                        rootPath.absolutePath
                    )

                val total =
                    stat.totalBytes

                val disponivel =
                    stat.availableBytes

                val usado =
                    total - disponivel

                val percentual =
                    if (total > 0) {

                        (
                            usado.toDouble() /
                                    total.toDouble() *
                                    100.0
                            ).toInt()

                    } else {

                        0
                    }

                runOnUiThread {

                    storageInfo.text =
                        "${formatarBytes(usado)} usados de " +
                                "${formatarBytes(total)}"

                    storageProgress.max =
                        100

                    storageProgress.progress =
                        percentual
                }

            } catch (
                _: Exception
            ) {

                runOnUiThread {

                    storageInfo.text =
                        "Não foi possível calcular o armazenamento"

                    storageProgress.progress =
                        0
                }
            }
        }
    }

    private fun formatarBytes(
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

        var valor =
            bytes.toDouble()

        var indice =
            0

        while (
            valor >= 1024 &&
            indice < unidades.size - 1
        ) {

            valor /= 1024
            indice++
        }

        return if (indice == 0) {

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
     * Antes de pedir o acesso aos arquivos, explica o motivo
     * (aviso exigido pela Play Store para permissões sensíveis).
     */
    private fun verificarPermissao() {

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.R
        ) {

            if (Environment.isExternalStorageManager()) {
                return
            }

            AlertDialog.Builder(this)
                .setTitle("Acesso aos arquivos")
                .setMessage(
                    "Para mostrar, organizar, mover e apagar seus arquivos, " +
                        "o Faxina precisa da permissão " +
                        "\"Acesso a todos os arquivos\".\n\n" +
                        "Seus arquivos ficam só no seu celular: o app não " +
                        "envia nada para a internet."
                )
                .setCancelable(false)
                .setNegativeButton("Agora não", null)
                .setPositiveButton("Permitir") { _, _ ->
                    abrirTelaDePermissao()
                }
                .show()

        } else if (
            checkSelfPermission(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                PEDIDO_PERMISSAO_ARQUIVOS
            )
        }
    }

    private fun abrirTelaDePermissao() {

        try {

            val intent =
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                )

            intent.data =
                Uri.parse(
                    "package:$packageName"
                )

            startActivity(intent)

        } catch (
            _: Exception
        ) {

            try {

                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    )
                )

            } catch (
                _: Exception
            ) {
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == PEDIDO_PERMISSAO_ARQUIVOS) {
            atualizarArmazenamento()
        }
    }

    // ============================================================
    // ANÁLISE
    // ============================================================

    /*
     * Percorre o armazenamento uma vez, soma o espaço de cada
     * tipo de arquivo e monta sugestões do que pode ser apagado.
     */
    private fun analisarArmazenamento() {

        homeScroll.visibility =
            View.GONE

        fileScreen.visibility =
            View.VISIBLE

        appTabs.visibility =
            View.GONE

        barraAcoes.visibility =
            View.GONE

        fileScreenTitle.text =
            "Análise do armazenamento"

        currentPath.text =
            "Analisando... isso pode levar alguns segundos"

        mediaGrid.visibility =
            View.GONE

        fileList.visibility =
            View.VISIBLE

        fileList.adapter =
            null

        fileList.setOnItemClickListener(null)

        fileList.setOnItemLongClickListener(null)

        thread {

            val analise =
                try {
                    fazerAnalise(rootPath)
                } catch (
                    _: Exception
                ) {
                    null
                }

            runOnUiThread {

                if (
                    fileScreenTitle.text.toString() !=
                    "Análise do armazenamento"
                ) {
                    return@runOnUiThread
                }

                if (analise == null) {

                    currentPath.text =
                        "Não foi possível analisar o armazenamento"

                    return@runOnUiThread
                }

                mostrarAnalise(analise)
            }
        }
    }

    private class Sugestao(
        val titulo: String,
        val descricao: String,
        val arquivos: List<File>,
        val tamanho: Long,
        val ehLixeira: Boolean = false
    )

    private class ResultadoAnalise(
        val porCategoria: LinkedHashMap<String, Long>,
        val sugestoes: List<Sugestao>
    )

    private fun categoriaDoArquivo(
        extensao: String
    ): String =
        when (extensao) {
            in TIPO_IMAGEM -> "Imagens"
            in TIPO_VIDEO -> "Vídeos"
            in TIPO_AUDIO -> "Áudios"
            in TIPO_DOCUMENTO -> "Documentos"
            "apk", "apks", "xapk" -> "Instaladores (APK)"
            else -> "Outros"
        }

    private fun novasCategorias(): LinkedHashMap<String, Long> =
        linkedMapOf(
            "Imagens" to 0L,
            "Vídeos" to 0L,
            "Áudios" to 0L,
            "Documentos" to 0L,
            "Instaladores (APK)" to 0L,
            "Outros" to 0L
        )

    // Usado também pelo cartão de memória
    private fun tamanhosPorCategoria(
        raiz: File
    ): LinkedHashMap<String, Long> {

        val categorias =
            novasCategorias()

        percorrerArquivos(raiz) { arquivo ->

            val categoria =
                categoriaDoArquivo(
                    arquivo.extension.lowercase(Locale.getDefault())
                )

            categorias[categoria] =
                (categorias[categoria] ?: 0L) + arquivo.length()
        }

        return categorias
    }

    // Visita todos os arquivos, sem entrar em pastas do sistema
    private fun percorrerArquivos(
        raiz: File,
        visitar: (File) -> Unit
    ) {

        val pilha =
            ArrayDeque<File>()

        pilha.add(raiz)

        while (pilha.isNotEmpty()) {

            val pasta =
                pilha.removeLast()

            val itens =
                try {
                    pasta.listFiles()
                } catch (
                    _: Exception
                ) {
                    null
                } ?: continue

            for (item in itens) {

                if (item.isDirectory) {

                    // Android/data e Android/obb são de outros apps
                    if (
                        item.name == "Android" &&
                        pasta.absolutePath == raiz.absolutePath
                    ) {
                        continue
                    }

                    // Lixeira e cofre ficam de fora
                    if (item.name == PASTA_INTERNA) {
                        continue
                    }

                    pilha.add(item)

                } else {

                    try {
                        visitar(item)
                    } catch (
                        _: Exception
                    ) {
                    }
                }
            }
        }
    }

    private fun fazerAnalise(
        raiz: File
    ): ResultadoAnalise {

        val categorias =
            novasCategorias()

        val grandes =
            ArrayList<File>()

        val apks =
            ArrayList<File>()

        val downloadsAntigos =
            ArrayList<File>()

        val miniaturas =
            ArrayList<File>()

        val vazios =
            ArrayList<File>()

        val downloads =
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ).absolutePath + File.separator

        val limiteAntigo =
            System.currentTimeMillis() -
                90L * 24 * 60 * 60 * 1000

        val limiteGrande =
            100L * 1024 * 1024

        percorrerArquivos(raiz) { arquivo ->

            val tamanho =
                arquivo.length()

            val extensao =
                arquivo.extension.lowercase(Locale.getDefault())

            val categoria =
                categoriaDoArquivo(extensao)

            categorias[categoria] =
                (categorias[categoria] ?: 0L) + tamanho

            val caminho =
                arquivo.absolutePath

            when {

                caminho.contains("/.thumbnails/") ->
                    miniaturas.add(arquivo)

                categoria == "Instaladores (APK)" ->
                    apks.add(arquivo)

                tamanho >= limiteGrande ->
                    grandes.add(arquivo)

                caminho.startsWith(downloads) &&
                    arquivo.lastModified() < limiteAntigo ->
                    downloadsAntigos.add(arquivo)

                tamanho == 0L ->
                    vazios.add(arquivo)
            }
        }

        val lixeira =
            Armazenamento.itensDaLixeira()

        fun soma(lista: List<File>): Long =
            lista.sumOf {
                if (it.isDirectory) calcularTamanhoPasta(it) else it.length()
            }

        val sugestoes =
            ArrayList<Sugestao>()

        if (lixeira.isNotEmpty()) {
            sugestoes.add(
                Sugestao(
                    "Esvaziar a lixeira",
                    "${lixeira.size} item(ns) que você já apagou",
                    lixeira,
                    soma(lixeira),
                    ehLixeira = true
                )
            )
        }

        if (apks.isNotEmpty()) {
            sugestoes.add(
                Sugestao(
                    "Instaladores (APK)",
                    "${apks.size} arquivo(s) de instalação que não são mais necessários",
                    apks.sortedByDescending { it.length() },
                    soma(apks)
                )
            )
        }

        if (grandes.isNotEmpty()) {
            sugestoes.add(
                Sugestao(
                    "Arquivos grandes",
                    "${grandes.size} arquivo(s) com mais de 100 MB",
                    grandes.sortedByDescending { it.length() },
                    soma(grandes)
                )
            )
        }

        if (downloadsAntigos.isNotEmpty()) {
            sugestoes.add(
                Sugestao(
                    "Downloads antigos",
                    "${downloadsAntigos.size} download(s) com mais de 3 meses",
                    downloadsAntigos.sortedByDescending { it.length() },
                    soma(downloadsAntigos)
                )
            )
        }

        if (miniaturas.isNotEmpty()) {
            sugestoes.add(
                Sugestao(
                    "Miniaturas em cache",
                    "${miniaturas.size} miniatura(s) que o celular recria quando precisar",
                    miniaturas.sortedByDescending { it.length() },
                    soma(miniaturas)
                )
            )
        }

        if (vazios.isNotEmpty()) {
            sugestoes.add(
                Sugestao(
                    "Arquivos vazios",
                    "${vazios.size} arquivo(s) sem conteúdo (0 bytes)",
                    vazios,
                    0L
                )
            )
        }

        return ResultadoAnalise(
            categorias,
            sugestoes.sortedByDescending { it.tamanho }
        )
    }

    private fun mostrarAnalise(
        analise: ResultadoAnalise
    ) {

        val espaco =
            Armazenamento.espaco(rootPath)

        currentPath.text =
            if (espaco != null) {
                "${formatarBytes(espaco.usado)} usados • " +
                    "${formatarBytes(espaco.livre)} livres"
            } else {
                "Análise concluída"
            }

        val itens =
            ArrayList<ItemAnalise>()

        itens.add(
            ItemAnalise.Cabecalho(
                "Espaço por categoria"
            )
        )

        val totalCategorias =
            analise.porCategoria.values.sum().coerceAtLeast(1L)

        analise.porCategoria
            .entries
            .sortedByDescending { it.value }
            .forEach {
                itens.add(
                    ItemAnalise.Categoria(
                        it.key,
                        it.value,
                        (it.value * 100 / totalCategorias).toInt()
                    )
                )
            }

        itens.add(
            ItemAnalise.Cabecalho(
                "Sugestões para liberar espaço"
            )
        )

        if (analise.sugestoes.isEmpty()) {

            itens.add(
                ItemAnalise.Cabecalho(
                    "Tudo certo! Nada para limpar agora."
                )
            )
        }

        analise.sugestoes.forEach {
            itens.add(
                ItemAnalise.ItemSugestao(it)
            )
        }

        fileList.adapter =
            AnaliseAdapter(itens)

        fileList.setOnItemClickListener {
                _,
                _,
                position,
                _ ->

            val item =
                itens.getOrNull(position)

            if (item is ItemAnalise.ItemSugestao) {

                if (item.sugestao.ehLixeira) {
                    abrirLixeira()
                } else {
                    abrirSugestao(item.sugestao)
                }
            }
        }

        fileList.setOnItemLongClickListener(null)
    }

    // Lista os arquivos da sugestão com um botão para limpar tudo
    private fun abrirSugestao(
        sugestao: Sugestao
    ) {

        fileScreenTitle.text =
            sugestao.titulo

        currentPath.text =
            "${sugestao.arquivos.size} arquivo(s) • " +
                formatarBytes(sugestao.tamanho)

        val arquivos =
            sugestao.arquivos.filter { it.exists() }

        fileList.adapter =
            FileListAdapter(arquivos)

        fileList.setOnItemClickListener {
                _,
                _,
                position,
                _ ->

            arquivos.getOrNull(position)?.let {
                abrirArquivo(it)
            }
        }

        fileList.setOnItemLongClickListener {
                _,
                view,
                position,
                _ ->

            arquivos.getOrNull(position)?.let {
                mostrarMenuMidia(it, view)
            }

            true
        }

        barraAcoes.removeAllViews()

        barraAcoes.visibility =
            View.VISIBLE

        barraAcoes.addView(
            criarTextoAjuda(
                sugestao.descricao +
                    ". Os arquivos vão para a lixeira e podem ser restaurados."
            )
        )

        barraAcoes.addView(
            criarBotaoAcao(
                "Mover tudo para a lixeira",
                R.drawable.ic_acao_lixeira,
                Color.rgb(229, 57, 53)
            ) {

                AlertDialog.Builder(this)
                    .setTitle("Limpar ${sugestao.titulo.lowercase()}?")
                    .setMessage(
                        "${arquivos.size} arquivo(s) vão para a lixeira."
                    )
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Limpar") { _, _ ->

                        thread {

                            var movidos = 0

                            arquivos.forEach { arquivo ->

                                val destino =
                                    Armazenamento.nomeLivre(
                                        Armazenamento.pastaLixeira,
                                        arquivo.name
                                    )

                                if (Armazenamento.mover(arquivo, destino)) {

                                    Armazenamento.registrarNaLixeira(
                                        this,
                                        destino,
                                        arquivo
                                    )

                                    movidos++
                                }
                            }

                            runOnUiThread {

                                Toast.makeText(
                                    this,
                                    "$movidos arquivo(s) movidos para a lixeira",
                                    Toast.LENGTH_LONG
                                ).show()

                                Anuncios.mostrarAposLimpeza(this)

                                analisarArmazenamento()
                            }
                        }
                    }
                    .show()
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
            ).apply {
                topMargin = dp(8)
            }
        )
    }

    private sealed class ItemAnalise {

        class Cabecalho(
            val texto: String
        ) : ItemAnalise()

        class Categoria(
            val nome: String,
            val tamanho: Long,
            val percentual: Int
        ) : ItemAnalise()

        class ItemSugestao(
            val sugestao: Sugestao
        ) : ItemAnalise()
    }

    private inner class AnaliseAdapter(
        private val itens: List<ItemAnalise>
    ) : BaseAdapter() {

        override fun getCount(): Int =
            itens.size

        override fun getItem(
            position: Int
        ): Any =
            itens[position]

        override fun getItemId(
            position: Int
        ): Long =
            position.toLong()

        override fun isEnabled(
            position: Int
        ): Boolean =
            itens[position] is ItemAnalise.ItemSugestao

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {

            val item =
                itens[position]

            val linha =
                LinearLayout(this@MainActivity)

            linha.orientation =
                LinearLayout.VERTICAL

            linha.setPadding(
                dp(16),
                dp(12),
                dp(16),
                dp(12)
            )

            when (item) {

                is ItemAnalise.Cabecalho -> {

                    linha.setBackgroundColor(
                        Color.rgb(245, 245, 245)
                    )

                    val texto =
                        TextView(this@MainActivity)

                    texto.text =
                        item.texto

                    texto.textSize =
                        15f

                    texto.typeface =
                        android.graphics.Typeface.DEFAULT_BOLD

                    texto.setTextColor(
                        Color.rgb(21, 101, 192)
                    )

                    linha.addView(texto)
                }

                is ItemAnalise.Categoria -> {

                    val topo =
                        LinearLayout(this@MainActivity)

                    topo.orientation =
                        LinearLayout.HORIZONTAL

                    val nome =
                        TextView(this@MainActivity)

                    nome.text =
                        item.nome

                    nome.textSize =
                        17f

                    nome.setTextColor(
                        COR_TEXTO_PRINCIPAL
                    )

                    topo.addView(
                        nome,
                        LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                    )

                    val valor =
                        TextView(this@MainActivity)

                    valor.text =
                        formatarBytes(item.tamanho)

                    valor.textSize =
                        16f

                    valor.typeface =
                        android.graphics.Typeface.DEFAULT_BOLD

                    valor.setTextColor(
                        COR_TEXTO_PRINCIPAL
                    )

                    topo.addView(valor)

                    linha.addView(topo)

                    val barra =
                        ProgressBar(
                            this@MainActivity,
                            null,
                            android.R.attr.progressBarStyleHorizontal
                        )

                    barra.max = 100

                    barra.progress =
                        item.percentual

                    linha.addView(
                        barra,
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dp(6)
                        ).apply {
                            topMargin = dp(6)
                        }
                    )
                }

                is ItemAnalise.ItemSugestao -> {

                    val topo =
                        LinearLayout(this@MainActivity)

                    topo.orientation =
                        LinearLayout.HORIZONTAL

                    topo.gravity =
                        Gravity.CENTER_VERTICAL

                    val icone =
                        ImageView(this@MainActivity)

                    icone.setImageResource(
                        R.drawable.ic_acao_lixeira
                    )

                    icone.setColorFilter(
                        Color.rgb(229, 57, 53)
                    )

                    topo.addView(
                        icone,
                        LinearLayout.LayoutParams(
                            dp(28),
                            dp(28)
                        )
                    )

                    val textos =
                        LinearLayout(this@MainActivity)

                    textos.orientation =
                        LinearLayout.VERTICAL

                    val titulo =
                        TextView(this@MainActivity)

                    titulo.text =
                        item.sugestao.titulo

                    titulo.textSize =
                        17f

                    titulo.typeface =
                        android.graphics.Typeface.DEFAULT_BOLD

                    titulo.setTextColor(
                        COR_TEXTO_PRINCIPAL
                    )

                    textos.addView(titulo)

                    val descricao =
                        TextView(this@MainActivity)

                    descricao.text =
                        item.sugestao.descricao

                    descricao.textSize =
                        14f

                    descricao.setTextColor(
                        COR_TEXTO_SECUNDARIO
                    )

                    textos.addView(descricao)

                    topo.addView(
                        textos,
                        LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f
                        ).apply {
                            marginStart = dp(14)
                            marginEnd = dp(8)
                        }
                    )

                    val valor =
                        TextView(this@MainActivity)

                    valor.text =
                        formatarBytes(item.sugestao.tamanho)

                    valor.textSize =
                        16f

                    valor.typeface =
                        android.graphics.Typeface.DEFAULT_BOLD

                    valor.setTextColor(
                        Color.rgb(229, 57, 53)
                    )

                    topo.addView(valor)

                    linha.addView(topo)
                }
            }

            return linha
        }
    }

    private fun tamanhoPorExtensoes(
        pasta: File,
        extensoes: Set<String>
    ): Long {

        var total =
            0L

        fun procurar(
            diretorio: File
        ) {

            val arquivos =
                try {
                    diretorio.listFiles()
                } catch (
                    _: Exception
                ) {
                    null
                }

            arquivos?.forEach { arquivo ->

                if (
                    arquivo.isDirectory
                ) {

                    procurar(
                        arquivo
                    )

                } else {

                    val extensao =
                        arquivo.extension
                            .lowercase(
                                Locale.getDefault()
                            )

                    if (
                        extensoes.contains(
                            extensao
                        )
                    ) {

                        total +=
                            try {
                                arquivo.length()
                            } catch (
                                _: Exception
                            ) {
                                0L
                            }
                    }
                }
            }
        }

        procurar(pasta)

        return total
    }

    // ============================================================
    // VOLTAR
    // ============================================================

    private fun voltar() {

        if (
            fileScreen.visibility !=
            View.VISIBLE
        ) {
            return
        }

        fileScreen.visibility =
            View.GONE

        appTabs.visibility =
            View.GONE

        barraAcoes.visibility =
            View.GONE

        homeScroll.visibility =
            View.VISIBLE

        mediaGrid.visibility =
            View.GONE

        fileList.visibility =
            View.VISIBLE

        currentDirectory =
            rootPath

        pastaDeMidiaAtual =
            null

        telaDePastasDeMidia =
            false

        tipoDeMidiaAtual =
            TIPO_IMAGEM

        categoriaMidiaAtual =
            ""

        searchEdit.setText("")

        atualizarArmazenamento()
    }

    // ============================================================
    // ÍCONES
    // ============================================================

    private fun obterIconeArquivo(
        arquivo: File
    ): Int {

        if (arquivo.isDirectory) {
            return R.drawable.ic_folder
        }

        val extensao =
            arquivo.extension.lowercase(
                Locale.getDefault()
            )

        return when (extensao) {

            in TIPO_IMAGEM ->
                R.drawable.imagens

            in TIPO_VIDEO ->
                R.drawable.videos

            in TIPO_AUDIO ->
                R.drawable.audios

            "apk", "apks", "xapk" ->
                R.drawable.aplicativos

            "pdf" ->
                R.drawable.ic_tipo_pdf

            "doc", "docx", "odt", "rtf" ->
                R.drawable.ic_tipo_word

            "xls", "xlsx", "ods", "csv" ->
                R.drawable.ic_tipo_excel

            "ppt", "pptx", "odp" ->
                R.drawable.ic_tipo_ppt

            "txt", "log", "md", "json", "xml" ->
                R.drawable.ic_tipo_texto

            "zip", "rar", "7z", "tar", "gz" ->
                R.drawable.ic_zip

            else ->
                R.drawable.ic_tipo_arquivo
        }
    }

    // ============================================================
    // NOVO ÍCONE DE ÁUDIO
    // ============================================================

    private fun criarIconeAudio():
            android.graphics.drawable.Drawable {

        return object :
            android.graphics.drawable.Drawable() {

            private val paint =
                Paint(
                    Paint.ANTI_ALIAS_FLAG
                )

            override fun draw(
                canvas: Canvas
            ) {

                val esquerda =
                    bounds.left.toFloat()

                val topo =
                    bounds.top.toFloat()

                val direita =
                    bounds.right.toFloat()

                val baixo =
                    bounds.bottom.toFloat()

                val largura =
                    direita - esquerda

                val altura =
                    baixo - topo

                val lado =
                    minOf(
                        largura,
                        altura
                    )

                val cx =
                    esquerda +
                            largura / 2f

                val cy =
                    topo +
                            altura / 2f

                val margem =
                    lado * 0.08f

                val rect =
                    RectF(
                        esquerda + margem,
                        topo + margem,
                        direita - margem,
                        baixo - margem
                    )

                // Fundo arredondado azul/roxo
                paint.style =
                    Paint.Style.FILL

                paint.shader =
                    LinearGradient(
                        rect.left,
                        rect.top,
                        rect.right,
                        rect.bottom,
                        Color.rgb(
                            35,
                            113,
                            255
                        ),
                        Color.rgb(
                            124,
                            67,
                            220
                        ),
                        Shader.TileMode.CLAMP
                    )

                canvas.drawRoundRect(
                    rect,
                    lado * 0.22f,
                    lado * 0.22f,
                    paint
                )

                paint.shader =
                    null

                // Arco principal do fone
                paint.style =
                    Paint.Style.STROKE

                paint.strokeWidth =
                    lado * 0.075f

                paint.strokeCap =
                    Paint.Cap.ROUND

                paint.strokeJoin =
                    Paint.Join.ROUND

                paint.color =
                    Color.WHITE

                val arco =
                    RectF(
                        cx - lado * 0.285f,
                        cy - lado * 0.285f,
                        cx + lado * 0.285f,
                        cy + lado * 0.285f
                    )

                canvas.drawArc(
                    arco,
                    205f,
                    130f,
                    false,
                    paint
                )

                // Almofada esquerda
                paint.style =
                    Paint.Style.FILL

                val esquerdaFone =
                    RectF(
                        cx - lado * 0.335f,
                        cy - lado * 0.015f,
                        cx - lado * 0.205f,
                        cy + lado * 0.285f
                    )

                canvas.drawRoundRect(
                    esquerdaFone,
                    lado * 0.055f,
                    lado * 0.055f,
                    paint
                )

                // Almofada direita
                val direitaFone =
                    RectF(
                        cx + lado * 0.205f,
                        cy - lado * 0.015f,
                        cx + lado * 0.335f,
                        cy + lado * 0.285f
                    )

                canvas.drawRoundRect(
                    direitaFone,
                    lado * 0.055f,
                    lado * 0.055f,
                    paint
                )

                // Pequenos detalhes inferiores
                paint.color =
                    Color.argb(
                        210,
                        255,
                        255,
                        255
                    )

                val detalheEsquerdo =
                    RectF(
                        cx - lado * 0.17f,
                        cy + lado * 0.23f,
                        cx - lado * 0.03f,
                        cy + lado * 0.30f
                    )

                canvas.drawRoundRect(
                    detalheEsquerdo,
                    lado * 0.03f,
                    lado * 0.03f,
                    paint
                )

                val detalheDireito =
                    RectF(
                        cx + lado * 0.03f,
                        cy + lado * 0.23f,
                        cx + lado * 0.17f,
                        cy + lado * 0.30f
                    )

                canvas.drawRoundRect(
                    detalheDireito,
                    lado * 0.03f,
                    lado * 0.03f,
                    paint
                )
            }

            override fun setAlpha(
                alpha: Int
            ) {
                paint.alpha =
                    alpha
            }

            override fun setColorFilter(
                colorFilter:
                    android.graphics.ColorFilter?
            ) {
                paint.colorFilter =
                    colorFilter
            }

            @Deprecated(
                "Deprecated in Android SDK"
            )
            override fun getOpacity(): Int {

                return android.graphics.PixelFormat.TRANSLUCENT
            }
        }
    }

    // ============================================================
    // MENU DE MÍDIA
    // ============================================================

    private fun mostrarMenuMidia(
        arquivo: File,
        ancora: View
    ) {

        if (!arquivo.exists()) {

            Toast.makeText(
                this,
                "Arquivo não encontrado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val menu =
            LinearLayout(this)

        MenuEscuro.prepararMenu(
            menu
        )

        adicionarOpcaoMenu(
            menu,
            "↗",
            "Compartilhar"
        ) {

            compartilharArquivo(
                arquivo
            )
        }

        adicionarOpcaoMenu(
            menu,
            "▣",
            "Copiar"
        ) {

            mostrarEscolhaDePastaParaCopiar(
                arquivo
            )
        }

        adicionarOpcaoMenu(
            menu,
            "→",
            "Mover para"
        ) {

            mostrarEscolhaDePasta(
                arquivo
            )
        }

        adicionarOpcoesCartao(
            menu,
            arquivo
        )

        adicionarOpcaoMenu(
            menu,
            "🔒",
            "Mover para o cofre"
        ) {

            moverParaCofre(
                arquivo
            )
        }

        adicionarOpcaoMenu(
            menu,
            "✎",
            "Renomear"
        ) {

            renomearArquivo(
                arquivo
            )
        }

        adicionarOpcaoMenu(
            menu,
            "ⓘ",
            "Informações"
        ) {

            mostrarInformacoes(
                arquivo
            )
        }

        adicionarOpcaoMenu(
            menu,
            "+",
            "Criar pasta"
        ) {

            criarPastaNoLocal(
                arquivo.parentFile
                    ?: rootPath
            )
        }

        adicionarOpcaoMenu(
            menu,
            "🗑",
            "Mover para lixeira"
        ) {

            moverParaLixeira(
                arquivo
            )
        }

        mostrarPopup(
            menu,
            ancora
        )
    }

    private fun adicionarOpcaoMenu(
        menu: LinearLayout,
        icone: String,
        texto: String,
        acao: () -> Unit
    ) {

        menu.addView(
            MenuEscuro.criarLinha(
                this,
                icone,
                texto
            ) {

                popupMenuAtual?.dismiss()

                popupMenuAtual =
                    null

                acao()
            }
        )
    }

    private fun mostrarPopup(
        menu: LinearLayout,
        ancora: View
    ) {

        val largura =
            (
                resources.displayMetrics.density *
                        280
                ).toInt()

        val popup =
            PopupWindow(
                menu,
                largura,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )

        popup.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(
                Color.TRANSPARENT
            )
        )

        popup.isOutsideTouchable =
            true

        popup.isFocusable =
            true

        popupMenuAtual?.dismiss()

        popupMenuAtual =
            popup

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.LOLLIPOP
        ) {

            popup.elevation =
                12f
        }

        menu.measure(
            View.MeasureSpec.makeMeasureSpec(
                largura,
                View.MeasureSpec.EXACTLY
            ),
            View.MeasureSpec.makeMeasureSpec(
                0,
                View.MeasureSpec.UNSPECIFIED
            )
        )

        val localizacao =
            IntArray(2)

        ancora.getLocationOnScreen(
            localizacao
        )

        val larguraTela =
            resources.displayMetrics.widthPixels

        var x =
            localizacao[0]

        if (
            x + largura >
            larguraTela
        ) {

            x =
                larguraTela -
                        largura -
                        8
        }

        x =
            x.coerceAtLeast(8)

        val alturaMenu =
            menu.measuredHeight

        val alturaTela =
            resources.displayMetrics.heightPixels

        val espacoAbaixo =
            alturaTela -
                    (
                        localizacao[1] +
                                ancora.height
                        )

        val y =
            if (
                espacoAbaixo >=
                alturaMenu + 12
            ) {

                localizacao[1] +
                        ancora.height

            } else {

                (
                    localizacao[1] -
                            alturaMenu
                    )
                    .coerceAtLeast(8)
            }

        popup.showAtLocation(
            ancora,
            Gravity.TOP or Gravity.START,
            x,
            y
        )
    }

    // ============================================================
    // MENU DE PASTA
    // ============================================================

    private fun mostrarMenuPasta(
        pasta: File,
        ancora: View
    ) {

        val menu =
            LinearLayout(this)

        MenuEscuro.prepararMenu(
            menu
        )

        adicionarOpcaoMenu(
            menu,
            "ⓘ",
            "Informações"
        ) {

            mostrarInformacoes(
                pasta
            )
        }

        adicionarOpcaoMenu(
            menu,
            "✎",
            "Renomear"
        ) {

            renomearArquivo(
                pasta
            )
        }

        adicionarOpcaoMenu(
            menu,
            "→",
            "Mover para"
        ) {

            mostrarEscolhaDePasta(
                pasta
            )
        }

        adicionarOpcoesCartao(
            menu,
            pasta
        )

        adicionarOpcaoMenu(
            menu,
            "+",
            "Criar pasta"
        ) {

            criarPastaNoLocal(
                pasta
            )
        }

        adicionarOpcaoMenu(
            menu,
            "🗑",
            "Mover para lixeira"
        ) {

            moverParaLixeira(
                pasta
            )
        }

        mostrarPopup(
            menu,
            ancora
        )
    }

    // ============================================================
    // INFORMAÇÕES
    // ============================================================

    private fun mostrarInformacoes(
        arquivo: File
    ) {

        val tamanho =
            if (arquivo.isDirectory) {

                calcularTamanhoPasta(
                    arquivo
                )

            } else {

                arquivo.length()
            }

        val tipo =
            if (arquivo.isDirectory) {

                "Pasta"

            } else {

                arquivo.extension
                    .uppercase(
                        Locale.getDefault()
                    )
                    .ifEmpty {
                        "Arquivo"
                    }
            }

        val ultimaAlteracao =
            try {

                java.text.SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    Locale.getDefault()
                ).format(
                    java.util.Date(
                        arquivo.lastModified()
                    )
                )

            } catch (
                _: Exception
            ) {

                "Desconhecida"
            }

        val mensagem =
            """
            Nome: ${arquivo.name}
            
            Tipo: $tipo
            
            Tamanho: ${formatarBytes(tamanho)}
            
            Local:
            ${arquivo.parent ?: arquivo.absolutePath}
            
            Modificado:
            $ultimaAlteracao
            """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Informações")
            .setMessage(mensagem)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun calcularTamanhoPasta(
        pasta: File
    ): Long {

        var total =
            0L

        try {

            val arquivos =
                pasta.listFiles()
                    ?: return 0L

            for (arquivo in arquivos) {

                if (arquivo.isDirectory) {

                    total +=
                        calcularTamanhoPasta(
                            arquivo
                        )

                } else {

                    total +=
                        try {
                            arquivo.length()
                        } catch (
                            _: Exception
                        ) {
                            0L
                        }
                }
            }

        } catch (
            _: Exception
        ) {
        }

        return total
    }

    // ============================================================
    // RENOMEAR
    // ============================================================

    private fun renomearArquivo(
        arquivo: File
    ) {

        val campo =
            EditText(this)

        campo.setText(
            arquivo.name
        )

        campo.selectAll()

        val dialog =
            AlertDialog.Builder(this)
                .setTitle("Renomear")
                .setView(campo)
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

                if (novoNome.isEmpty()) {

                    Toast.makeText(
                        this,
                        "Digite um nome válido",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                if (novoNome.contains("/")) {

                    Toast.makeText(
                        this,
                        "Nome inválido",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                val novoArquivo =
                    File(
                        arquivo.parentFile
                            ?: rootPath,
                        novoNome
                    )

                if (novoArquivo.exists()) {

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

                    Toast.makeText(
                        this,
                        "Renomeado com sucesso",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()

                    recarregarTelaAtual()

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
    // CRIAR PASTA
    // ============================================================

    private fun criarPastaNoLocal(
        local: File
    ) {

        val campo =
            EditText(this)

        campo.hint =
            "Nome da pasta"

        val dialog =
            AlertDialog.Builder(this)
                .setTitle("Criar pasta")
                .setView(campo)
                .setNegativeButton(
                    "Cancelar",
                    null
                )
                .setPositiveButton(
                    "Criar",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val nome =
                    campo.text
                        .toString()
                        .trim()

                if (nome.isEmpty()) {

                    Toast.makeText(
                        this,
                        "Digite um nome",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                if (nome.contains("/")) {

                    Toast.makeText(
                        this,
                        "Nome inválido",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                val pasta =
                    File(
                        local,
                        nome
                    )

                if (pasta.exists()) {

                    Toast.makeText(
                        this,
                        "Essa pasta já existe",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                val criada =
                    try {
                        pasta.mkdirs()
                    } catch (
                        _: Exception
                    ) {
                        false
                    }

                if (criada) {

                    Toast.makeText(
                        this,
                        "Pasta criada",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()

                    recarregarTelaAtual()

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
    // MOVER
    // ============================================================

    private fun mostrarEscolhaDePasta(
        arquivo: File
    ) {

        val inicial =
            arquivo.parentFile
                ?: rootPath

        mostrarSeletorDePasta(
            arquivo,
            inicial
        )
    }

    private fun mostrarSeletorDePasta(
        arquivo: File,
        pastaAtual: File
    ) {

        val pastas =
            try {

                pastaAtual.listFiles()
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

        val nomes =
            ArrayList<String>()

        if (pastaAtual != rootPath) {
            nomes.add("⬆  ..")
        }

        nomes.add("＋  Criar pasta aqui")

        pastas.forEach {
            nomes.add("📁  ${it.name}")
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
                .setTitle("Mover para")
                .setView(lista)
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

            if (
                position < 0 ||
                position >= nomes.size
            ) {
                return@setOnItemClickListener
            }

            val escolhido =
                nomes[position]

            when {

                escolhido.startsWith("⬆") -> {

                    val pai =
                        pastaAtual.parentFile

                    if (
                        pai != null &&
                        pai.absolutePath.startsWith(
                            rootPath.absolutePath
                        )
                    ) {

                        dialog.dismiss()

                        mostrarSeletorDePasta(
                            arquivo,
                            pai
                        )
                    }
                }

                escolhido.startsWith("＋") -> {

                    dialog.dismiss()

                    criarPastaNoLocal(
                        pastaAtual
                    )
                }

                else -> {

                    val indicePasta =
                        if (
                            pastaAtual != rootPath
                        ) {
                            position - 2
                        } else {
                            position - 1
                        }

                    if (
                        indicePasta >= 0 &&
                        indicePasta < pastas.size
                    ) {

                        val destino =
                            pastas[indicePasta]

                        dialog.dismiss()

                        confirmarMoverArquivo(
                            arquivo,
                            destino
                        )
                    }
                }
            }
        }

        dialog.show()
    }

    private fun confirmarMoverArquivo(
        arquivo: File,
        destino: File
    ) {

        AlertDialog.Builder(this)
            .setTitle("Mover arquivo")
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

                moverArquivoParaPasta(
                    arquivo,
                    destino
                )
            }
            .show()
    }

    private fun moverArquivoParaPasta(
        arquivo: File,
        destinoPasta: File
    ) {

        if (
            !destinoPasta.exists() ||
            !destinoPasta.isDirectory
        ) {

            Toast.makeText(
                this,
                "Pasta de destino inválida",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val destino =
            File(
                destinoPasta,
                arquivo.name
            )

        if (destino.exists()) {

            Toast.makeText(
                this,
                "Já existe um arquivo com esse nome no destino",
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

            Toast.makeText(
                this,
                "Arquivo movido com sucesso",
                Toast.LENGTH_SHORT
            ).show()

            recarregarTelaAtual()

        } else {

            Toast.makeText(
                this,
                "Não foi possível mover o arquivo",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ============================================================
    // COMPARTILHAR
    // ============================================================

    private fun compartilharArquivo(
        arquivo: File
    ) {

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
                    "Compartilhar arquivo"
                )
            )

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "Não foi possível compartilhar: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ============================================================
    // COPIAR
    // ============================================================

    private fun mostrarEscolhaDePastaParaCopiar(
        arquivo: File
    ) {

        val inicial =
            arquivo.parentFile
                ?: rootPath

        mostrarSeletorDePastaParaCopiar(
            arquivo,
            inicial
        )
    }

    private fun mostrarSeletorDePastaParaCopiar(
        arquivo: File,
        pastaAtual: File
    ) {

        val pastas =
            try {

                pastaAtual.listFiles()
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

        val nomes =
            ArrayList<String>()

        if (pastaAtual != rootPath) {
            nomes.add("⬆  ..")
        }

        nomes.add("📋  Copiar aqui")
        nomes.add("＋  Criar pasta aqui")

        pastas.forEach {
            nomes.add("📁  ${it.name}")
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
                .setTitle("Copiar para")
                .setView(lista)
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

            if (
                position < 0 ||
                position >= nomes.size
            ) {
                return@setOnItemClickListener
            }

            val escolhido =
                nomes[position]

            when {

                escolhido.startsWith("⬆") -> {

                    val pai =
                        pastaAtual.parentFile

                    if (
                        pai != null &&
                        pai.absolutePath.startsWith(
                            rootPath.absolutePath
                        )
                    ) {

                        dialog.dismiss()

                        mostrarSeletorDePastaParaCopiar(
                            arquivo,
                            pai
                        )
                    }
                }

                escolhido.startsWith("📋") -> {

                    dialog.dismiss()

                    copiarArquivoParaPasta(
                        arquivo,
                        pastaAtual
                    )
                }

                escolhido.startsWith("＋") -> {

                    dialog.dismiss()

                    criarPastaNoLocal(
                        pastaAtual
                    )
                }

                else -> {

                    val indicePasta =
                        if (
                            pastaAtual != rootPath
                        ) {
                            position - 3
                        } else {
                            position - 2
                        }

                    if (
                        indicePasta >= 0 &&
                        indicePasta < pastas.size
                    ) {

                        val destino =
                            pastas[indicePasta]

                        dialog.dismiss()

                        copiarArquivoParaPasta(
                            arquivo,
                            destino
                        )
                    }
                }
            }
        }

        dialog.show()
    }

    private fun copiarArquivoParaPasta(
        arquivo: File,
        destinoPasta: File
    ) {

        if (
            !arquivo.exists() ||
            !arquivo.isFile
        ) {

            Toast.makeText(
                this,
                "Arquivo não encontrado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (
            !destinoPasta.exists() ||
            !destinoPasta.isDirectory
        ) {

            Toast.makeText(
                this,
                "Pasta de destino inválida",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (
            arquivo.parentFile?.absolutePath ==
            destinoPasta.absolutePath
        ) {

            Toast.makeText(
                this,
                "O arquivo já está nessa pasta",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        var destino =
            File(
                destinoPasta,
                arquivo.name
            )

        if (destino.exists()) {

            val nomeBase =
                arquivo.nameWithoutExtension

            val extensao =
                arquivo.extension

            var contador =
                1

            while (destino.exists()) {

                val novoNome =
                    if (extensao.isEmpty()) {

                        "${nomeBase}_copia_$contador"

                    } else {

                        "${nomeBase}_copia_$contador.$extensao"
                    }

                destino =
                    File(
                        destinoPasta,
                        novoNome
                    )

                contador++
            }
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

                            var quantidade =
                                entrada.read(
                                    buffer
                                )

                            while (
                                quantidade != -1
                            ) {

                                saida.write(
                                    buffer,
                                    0,
                                    quantidade
                                )

                                quantidade =
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
                        if (destino.exists()) {
                            destino.delete()
                        }
                    } catch (
                        _: Exception
                    ) {
                    }

                    false
                }

            runOnUiThread {

                if (sucesso) {

                    Toast.makeText(
                        this,
                        "Arquivo copiado com sucesso",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this,
                        "Não foi possível copiar o arquivo",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ============================================================
    // LIXEIRA
    // ============================================================

    private fun moverParaLixeira(
        arquivo: File
    ) {

        val lixeira =
            File(
                rootPath,
                ".GerenciadorArquivos/.Lixeira"
            )

        try {

            if (!lixeira.exists()) {
                lixeira.mkdirs()
            }

            var destino =
                File(
                    lixeira,
                    arquivo.name
                )

            if (destino.exists()) {

                val nomeBase =
                    arquivo.nameWithoutExtension

                val extensao =
                    arquivo.extension

                var contador =
                    1

                while (destino.exists()) {

                    val novoNome =
                        if (extensao.isEmpty()) {

                            "${nomeBase}_$contador"

                        } else {

                            "${nomeBase}_$contador.$extensao"
                        }

                    destino =
                        File(
                            lixeira,
                            novoNome
                        )

                    contador++
                }
            }

            val sucesso =
                Armazenamento.mover(
                    arquivo,
                    destino
                )

            if (sucesso) {

                Armazenamento.registrarNaLixeira(
                    this,
                    destino,
                    arquivo
                )

                Toast.makeText(
                    this,
                    "Movido para a lixeira",
                    Toast.LENGTH_SHORT
                ).show()

                recarregarTelaAtual()

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
    // RECARREGAR
    // ============================================================

    private fun recarregarTelaAtual() {

        if (
            telaDePastasDeMidia &&
            pastaDeMidiaAtual == null
        ) {

            val pastas =
                if (
                    tipoDeMidiaAtual == TIPO_VIDEO
                ) {

                    listOf(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DCIM
                        ),
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_MOVIES
                        )
                    )

                } else {

                    listOf(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DCIM
                        ),
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES
                        )
                    )
                }

            abrirCategoriaDeMidia(
                categoriaMidiaAtual,
                tipoDeMidiaAtual,
                pastas
            )

            return
        }

        if (
            pastaDeMidiaAtual != null
        ) {

            buscarMidiasDaPasta(
                tipoDeMidiaAtual,
                pastaDeMidiaAtual!!
            )

            return
        }

        if (currentDirectory.exists()) {

            carregarArquivos(
                currentDirectory
            )
        }
    }

    // ============================================================
    // ADAPTER DA LISTA
    // ============================================================

    private inner class FileListAdapter(
        private val arquivos: List<File>
    ) : BaseAdapter() {

        override fun getCount(): Int =
            arquivos.size

        override fun getItem(
            position: Int
        ): File =
            arquivos[position]

        override fun getItemId(
            position: Int
        ): Long =
            position.toLong()

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {

            val arquivo =
                arquivos[position]

            val linha =
                LinearLayout(
                    this@MainActivity
                )

            linha.orientation =
                LinearLayout.HORIZONTAL

            linha.gravity =
                Gravity.CENTER_VERTICAL

            linha.setPadding(
                dp(14),
                dp(10),
                dp(14),
                dp(10)
            )

            val icone =
                ImageView(
                    this@MainActivity
                )

            val extensao =
                arquivo.extension
                    .lowercase(
                        Locale.getDefault()
                    )

            icone.setImageResource(
                obterIconeArquivo(
                    arquivo
                )
            )

            val tamanhoIcone =
                (40 * resources.displayMetrics.density)
                    .toInt()

            linha.addView(
                icone,
                LinearLayout.LayoutParams(
                    tamanhoIcone,
                    tamanhoIcone
                )
            )

            val textos =
                LinearLayout(
                    this@MainActivity
                )

            textos.orientation =
                LinearLayout.VERTICAL

            textos.setPadding(
                12,
                0,
                0,
                0
            )

            val nome =
                TextView(
                    this@MainActivity
                )

            nome.text =
                arquivo.name

            nome.textSize =
                17f

            nome.setTextColor(
                COR_TEXTO_PRINCIPAL
            )

            nome.maxLines =
                1

            nome.ellipsize =
                TextUtils.TruncateAt.END

            textos.addView(
                nome,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            val info =
                TextView(
                    this@MainActivity
                )

            info.text =
                if (
                    arquivo.isDirectory
                ) {

                    "Pasta"

                } else {

                    formatarBytes(
                        arquivo.length()
                    )
                }

            info.textSize =
                14f

            info.setTextColor(
                COR_TEXTO_SECUNDARIO
            )

            textos.addView(
                info,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            linha.addView(
                textos,
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            return linha
        }
    }

    // ============================================================
    // ADAPTER DAS PASTAS DE MÍDIA
    // CORRIGIDO PARA NÃO CORTAR A QUANTIDADE
    // ============================================================

    private inner class PastaMediaAdapter(
        private val pastas: List<PastaMedia>
    ) : BaseAdapter() {

        override fun getCount(): Int =
            pastas.size

        override fun getItem(
            position: Int
        ): PastaMedia =
            pastas[position]

        override fun getItemId(
            position: Int
        ): Long =
            position.toLong()

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {

            val larguraColuna =
                if (parent.width > 0) {
                    parent.width / 3
                } else {
                    resources.displayMetrics.widthPixels / 3
                }

            val espaco =
                8

            val tamanhoImagem =
                (
                    larguraColuna -
                            (espaco * 2) -
                            6
                    ).coerceAtLeast(1)

            val container =
                FrameLayout(
                    this@MainActivity
                )

            container.setPadding(
                3,
                4,
                3,
                8
            )

            val layout =
                LinearLayout(
                    this@MainActivity
                )

            layout.orientation =
                LinearLayout.VERTICAL

            layout.gravity =
                Gravity.CENTER_HORIZONTAL

            layout.setPadding(
                0,
                0,
                0,
                0
            )

            val imagem =
                ImageView(
                    this@MainActivity
                )

            imagem.scaleType =
                ImageView.ScaleType.CENTER_CROP

            imagem.setBackgroundColor(
                Color.rgb(
                    235,
                    235,
                    235
                )
            )

            val pasta =
                pastas[position]

            val exemplo =
                pasta.arquivos.firstOrNull()

            val chave =
                if (exemplo != null) {

                    exemplo.absolutePath +
                            "_" +
                            exemplo.lastModified()

                } else {

                    pasta.pasta.absolutePath
                }

            imagem.tag =
                chave

            if (exemplo != null) {

                val bitmap =
                    thumbnailCache.get(
                        chave
                    )

                if (bitmap != null) {

                    imagem.setImageBitmap(
                        bitmap
                    )

                } else {

                    if (
                        tipoDeMidiaAtual ==
                        TIPO_VIDEO
                    ) {

                        imagem.setImageResource(
                            android.R.drawable.ic_media_play
                        )

                    } else {

                        imagem.setImageResource(
                            android.R.drawable.ic_menu_gallery
                        )
                    }

                    thumbnailExecutor.execute {

                        val thumb =
                            carregarMiniatura(
                                exemplo,
                                tamanhoImagem
                            )

                        if (thumb != null) {

                            thumbnailCache.put(
                                chave,
                                thumb
                            )

                            runOnUiThread {

                                if (
                                    imagem.tag == chave
                                ) {

                                    imagem.setImageBitmap(
                                        thumb
                                    )
                                }
                            }
                        }
                    }
                }

            } else {

                imagem.setImageResource(
                    android.R.drawable.ic_menu_gallery
                )
            }

            layout.addView(
                imagem,
                LinearLayout.LayoutParams(
                    tamanhoImagem,
                    tamanhoImagem
                )
            )

            val nome =
                TextView(
                    this@MainActivity
                )

            nome.text =
                pasta.pasta.name

            nome.textSize =
                15f

            nome.setTextColor(
                COR_TEXTO_PRINCIPAL
            )

            nome.gravity =
                Gravity.CENTER

            nome.maxLines =
                2

            nome.ellipsize =
                TextUtils.TruncateAt.END

            nome.includeFontPadding =
                true

            layout.addView(
                nome,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    42
                )
            )

            val quantidade =
                TextView(
                    this@MainActivity
                )

            quantidade.text =
                "(${pasta.arquivos.size})"

            quantidade.textSize =
                13f

            quantidade.setTextColor(
                COR_TEXTO_SECUNDARIO
            )

            quantidade.gravity =
                Gravity.CENTER

            quantidade.includeFontPadding =
                true

            quantidade.maxLines =
                1

            quantidade.ellipsize =
                TextUtils.TruncateAt.END

            layout.addView(
                quantidade,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    26
                )
            )

            container.addView(
                layout,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            container.minimumHeight =
                tamanhoImagem +
                        42 +
                        26 +
                        12

            return container
        }
    }

    // ============================================================
    // ADAPTER DAS IMAGENS / VÍDEOS
    // ============================================================

    private inner class MediaAdapter(
        private val arquivos: List<File>
    ) : BaseAdapter() {

        override fun getCount(): Int =
            arquivos.size

        override fun getItem(
            position: Int
        ): File =
            arquivos[position]

        override fun getItemId(
            position: Int
        ): Long =
            position.toLong()

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {

            val container =
                FrameLayout(
                    this@MainActivity
                )

            container.setPadding(
                2,
                2,
                2,
                2
            )

            val imagem =
                ImageView(
                    this@MainActivity
                )

            imagem.scaleType =
                ImageView.ScaleType.CENTER_CROP

            imagem.setBackgroundColor(
                Color.rgb(
                    235,
                    235,
                    235
                )
            )

            val larguraTela =
                resources.displayMetrics.widthPixels

            val tamanho =
                larguraTela / 3

            container.addView(
                imagem,
                FrameLayout.LayoutParams(
                    tamanho,
                    tamanho
                )
            )

            val arquivo =
                arquivos[position]

            val extensao =
                arquivo.extension
                    .lowercase(
                        Locale.getDefault()
                    )

            val chaveCache =
                arquivo.absolutePath +
                        "_" +
                        arquivo.lastModified()

            imagem.tag =
                chaveCache

            if (
                TIPO_IMAGEM.contains(
                    extensao
                ) ||
                TIPO_VIDEO.contains(
                    extensao
                )
            ) {

                val cache =
                    thumbnailCache.get(
                        chaveCache
                    )

                if (cache != null) {

                    imagem.setImageBitmap(
                        cache
                    )

                } else {

                    imagem.setImageResource(
                        if (
                            TIPO_VIDEO.contains(
                                extensao
                            )
                        ) {

                            android.R.drawable.ic_media_play

                        } else {

                            android.R.drawable.ic_menu_gallery
                        }
                    )

                    thumbnailExecutor.execute {

                        val bitmap =
                            carregarMiniatura(
                                arquivo,
                                tamanho
                            )

                        if (bitmap != null) {

                            thumbnailCache.put(
                                chaveCache,
                                bitmap
                            )

                            runOnUiThread {

                                if (
                                    !isFinishing &&
                                    imagem.tag ==
                                    chaveCache
                                ) {

                                    imagem.setImageBitmap(
                                        bitmap
                                    )
                                }
                            }
                        }
                    }
                }

            } else {

                if (
                    TIPO_AUDIO.contains(
                        extensao
                    )
                ) {

                    imagem.setImageDrawable(
                        criarIconeAudio()
                    )

                } else {

                    imagem.setImageResource(
                        obterIconeArquivo(
                            arquivo
                        )
                    )
                }
            }

            return container
        }
    }

    // ============================================================
    // ADAPTER DOS APLICATIVOS
    // ============================================================

    private inner class AppListAdapter(
        private val aplicativos: List<AppInfoItem>
    ) : BaseAdapter() {

        override fun getCount(): Int =
            aplicativos.size

        override fun getItem(
            position: Int
        ): AppInfoItem =
            aplicativos[position]

        override fun getItemId(
            position: Int
        ): Long =
            position.toLong()

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {

            val aplicativo =
                aplicativos[position]

            val linha =
                LinearLayout(
                    this@MainActivity
                )

            linha.orientation =
                LinearLayout.HORIZONTAL

            linha.gravity =
                Gravity.CENTER_VERTICAL

            linha.setPadding(
                dp(14),
                dp(10),
                dp(14),
                dp(10)
            )

            val icone =
                ImageView(
                    this@MainActivity
                )

            try {

                icone.setImageDrawable(
                    aplicativo.info.loadIcon(
                        packageManager
                    )
                )

            } catch (
                _: Exception
            ) {

                icone.setImageResource(
                    android.R.drawable.sym_def_app_icon
                )
            }

            linha.addView(
                icone,
                LinearLayout.LayoutParams(
                    52,
                    52
                )
            )

            val textos =
                LinearLayout(
                    this@MainActivity
                )

            textos.orientation =
                LinearLayout.VERTICAL

            textos.gravity =
                Gravity.CENTER_VERTICAL

            textos.setPadding(
                14,
                0,
                0,
                0
            )

            val nome =
                TextView(
                    this@MainActivity
                )

            nome.text =
                aplicativo.nome

            nome.textSize =
                17f

            nome.setTextColor(
                COR_TEXTO_PRINCIPAL
            )

            nome.maxLines =
                1

            nome.ellipsize =
                TextUtils.TruncateAt.END

            textos.addView(
                nome,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            val pacote =
                TextView(
                    this@MainActivity
                )

            pacote.text =
                aplicativo.info.packageName

            pacote.textSize =
                13f

            pacote.setTextColor(
                COR_TEXTO_SECUNDARIO
            )

            pacote.maxLines =
                1

            pacote.ellipsize =
                TextUtils.TruncateAt.END

            textos.addView(
                pacote,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            linha.addView(
                textos,
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            return linha
        }
    }

    // ============================================================
    // MINIATURAS
    // ============================================================

    private fun carregarMiniatura(
        arquivo: File,
        tamanho: Int
    ): Bitmap? {

        return try {

            val extensao =
                arquivo.extension
                    .lowercase(
                        Locale.getDefault()
                    )

            if (
                TIPO_VIDEO.contains(
                    extensao
                )
            ) {

                val retriever =
                    MediaMetadataRetriever()

                try {

                    retriever.setDataSource(
                        arquivo.absolutePath
                    )

                    val frame =
                        retriever.getFrameAtTime(
                            0,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )

                    if (frame != null) {

                        val largura =
                            frame.width

                        val altura =
                            frame.height

                        val maior =
                            maxOf(
                                largura,
                                altura
                            )

                        if (
                            maior > tamanho
                        ) {

                            reduzirComQualidade(
                                frame,
                                tamanho
                            )

                        } else {

                            frame
                        }

                    } else {

                        null
                    }

                } finally {

                    retriever.release()
                }

            } else if (
                android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.P
            ) {

                // Mesmo decodificador da galeria: respeita a rotação
                // da foto (EXIF), as cores e abre HEIC
                miniaturaComImageDecoder(
                    arquivo,
                    tamanho
                ) ?: miniaturaComBitmapFactory(
                    arquivo,
                    tamanho
                )

            } else {

                miniaturaComBitmapFactory(
                    arquivo,
                    tamanho
                )
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

    @android.annotation.TargetApi(28)
    private fun miniaturaComImageDecoder(
        arquivo: File,
        tamanho: Int
    ): Bitmap? {

        return try {

            val bitmap =
                android.graphics.ImageDecoder.decodeBitmap(
                    android.graphics.ImageDecoder.createSource(arquivo)
                ) { decoder, info, _ ->

                    val menorLado =
                        minOf(
                            info.size.width,
                            info.size.height
                        )

                    var amostra = 1

                    while (menorLado / (amostra * 2) >= tamanho) {
                        amostra *= 2
                    }

                    decoder.setTargetSampleSize(amostra)

                    decoder.allocator =
                        android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                }

            reduzirComQualidade(
                bitmap,
                tamanho
            )

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

    private fun miniaturaComBitmapFactory(
        arquivo: File,
        tamanho: Int
    ): Bitmap? {

        return try {

            run {

                val informacoes =
                    BitmapFactory.Options()

                informacoes.inJustDecodeBounds =
                    true

                BitmapFactory.decodeFile(
                    arquivo.absolutePath,
                    informacoes
                )

                val larguraOriginal =
                    informacoes.outWidth

                val alturaOriginal =
                    informacoes.outHeight

                if (
                    larguraOriginal <= 0 ||
                    alturaOriginal <= 0
                ) {

                    return null
                }

                var escala =
                    1

                while (
                    larguraOriginal / escala >
                    tamanho * 2 &&
                    alturaOriginal / escala >
                    tamanho * 2
                ) {

                    escala *= 2
                }

                val options =
                    BitmapFactory.Options()

                options.inSampleSize =
                    escala

                options.inPreferredConfig =
                    Bitmap.Config.ARGB_8888

                BitmapFactory.decodeFile(
                    arquivo.absolutePath,
                    options
                )?.let {
                    reduzirComQualidade(
                        it,
                        tamanho
                    )
                }
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

    // Reduz a imagem em etapas de no máximo metade do tamanho,
    // até o menor lado ficar do tamanho da miniatura. Reduzir
    // tudo de uma vez deixa a miniatura serrilhada ("pixada").
    private fun reduzirComQualidade(
        original: Bitmap,
        tamanho: Int
    ): Bitmap {

        var atual =
            original

        while (true) {

            val menorLado =
                minOf(
                    atual.width,
                    atual.height
                )

            if (menorLado <= tamanho) {
                break
            }

            val proximoMenor =
                maxOf(
                    menorLado / 2,
                    tamanho
                )

            val escala =
                proximoMenor.toFloat() /
                        menorLado.toFloat()

            val reduzida =
                Bitmap.createScaledBitmap(
                    atual,
                    (atual.width * escala)
                        .toInt()
                        .coerceAtLeast(1),
                    (atual.height * escala)
                        .toInt()
                        .coerceAtLeast(1),
                    true
                )

            if (atual !== original) {
                atual.recycle()
            }

            atual =
                reduzida
        }

        if (atual !== original) {
            original.recycle()
        }

        return atual
    }

    // ============================================================
    // DADOS
    // ============================================================

    private data class PastaMedia(
        val pasta: File,
        val arquivos: ArrayList<File>
    )

    private data class AppInfoItem(
        val info: ApplicationInfo,
        val nome: String
    )

    // ============================================================
    // CICLO DE VIDA
    // ============================================================

    override fun onDestroy() {

        Premium.removerOuvinte(aoMudarPremium)

        try {

            thumbnailExecutor.shutdownNow()

            thumbnailExecutor.awaitTermination(
                300,
                TimeUnit.MILLISECONDS
            )

        } catch (
            _: Exception
        ) {
        }

        thumbnailCache.evictAll()

        super.onDestroy()
    }

    override fun onBackPressed() {

        if (
            fileScreen.visibility ==
            View.VISIBLE
        ) {

            voltar()

        } else {

            super.onBackPressed()
        }
    }
}
