package com.gerenciadordearquivos.app

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.util.LruCache

class MainActivity : AppCompatActivity() {

    companion object {

        private const val TIPO_IMAGEM = 1
        private const val TIPO_VIDEO = 2

        private val EXTENSOES_IMAGEM = setOf(
            "jpg",
            "jpeg",
            "png",
            "gif",
            "webp",
            "bmp",
            "heic",
            "heif"
        )

        private val EXTENSOES_VIDEO = setOf(
            "mp4",
            "mkv",
            "avi",
            "mov",
            "wmv",
            "webm",
            "3gp",
            "m4v"
        )

        private val EXTENSOES_AUDIO = setOf(
            "mp3",
            "wav",
            "ogg",
            "m4a",
            "aac",
            "flac",
            "opus",
            "amr"
        )

        private val EXTENSOES_DOCUMENTOS = setOf(
            "pdf",
            "doc",
            "docx",
            "xls",
            "xlsx",
            "ppt",
            "pptx",
            "txt",
            "csv",
            "rtf",
            "odt",
            "ods",
            "odp"
        )
    }

    private lateinit var homeScroll: View
    private lateinit var fileScreen: View
    private lateinit var fileScreenTitle: TextView
    private lateinit var currentPath: TextView
    private lateinit var fileList: ListView
    private lateinit var mediaGrid: GridView
    private lateinit var storageInfo: TextView
    private lateinit var storageProgress: ProgressBar
    private lateinit var searchEdit: EditText

    private var pastaAtual: File =
        Environment.getExternalStorageDirectory()

    private var tipoDeMidiaAtual = 0

    private val thumbnailCache =
        object : LruCache<String, Bitmap>(20 * 1024) {

            override fun sizeOf(
                key: String,
                bitmap: Bitmap
            ): Int {
                return bitmap.byteCount / 1024
            }
        }

    private val thumbnailExecutor: ExecutorService =
        Executors.newFixedThreadPool(3)

    private var fileListData =
        mutableListOf<File>()

    private data class PastaMedia(
        val pasta: File,
        val arquivos: List<File>
    )

    private data class AppInfoItem(
        val info: ApplicationInfo,
        val nome: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            resources.getIdentifier(
                "activity_main",
                "layout",
                packageName
            )
        )

        inicializarViews()
        configurarCategorias()
        configurarBusca()

        verificarPermissao()
        atualizarArmazenamento()
    }

    private fun inicializarViews() {

        homeScroll =
            findViewById(
                resources.getIdentifier(
                    "homeScroll",
                    "id",
                    packageName
                )
            )

        fileScreen =
            findViewById(
                resources.getIdentifier(
                    "fileScreen",
                    "id",
                    packageName
                )
            )

        fileScreenTitle =
            findViewById(
                resources.getIdentifier(
                    "fileScreenTitle",
                    "id",
                    packageName
                )
            )

        currentPath =
            findViewById(
                resources.getIdentifier(
                    "currentPath",
                    "id",
                    packageName
                )
            )

        fileList =
            findViewById(
                resources.getIdentifier(
                    "fileList",
                    "id",
                    packageName
                )
            )

        mediaGrid =
            findViewById(
                resources.getIdentifier(
                    "mediaGrid",
                    "id",
                    packageName
                )
            )

        storageInfo =
            findViewById(
                resources.getIdentifier(
                    "storageInfo",
                    "id",
                    packageName
                )
            )

        storageProgress =
            findViewById(
                resources.getIdentifier(
                    "storageProgress",
                    "id",
                    packageName
                )
            )

        searchEdit =
            findViewById(
                resources.getIdentifier(
                    "searchEdit",
                    "id",
                    packageName
                )
            )
    }

    private fun configurarCategorias() {

        findViewById<View>(
            resources.getIdentifier(
                "categoryStorage",
                "id",
                packageName
            )
        )?.setOnClickListener {
            abrirArmazenamento()
        }

        findViewById<View>(
            resources.getIdentifier(
                "categoryDownloads",
                "id",
                packageName
            )
        )?.setOnClickListener {
            abrirDownloads()
        }

        findViewById<View>(
            resources.getIdentifier(
                "categoryImages",
                "id",
                packageName
            )
        )?.setOnClickListener {
            abrirImagens()
        }

        findViewById<View>(
            resources.getIdentifier(
                "categoryVideos",
                "id",
                packageName
            )
        )?.setOnClickListener {
            abrirVideos()
        }

        findViewById<View>(
            resources.getIdentifier(
                "categoryAudio",
                "id",
                packageName
            )
        )?.setOnClickListener {
            abrirAudio()
        }

        findViewById<View>(
            resources.getIdentifier(
                "categoryDocuments",
                "id",
                packageName
            )
        )?.setOnClickListener {
            abrirDocumentos()
        }

        findViewById<View>(
            resources.getIdentifier(
                "categoryApps",
                "id",
                packageName
            )
        )?.setOnClickListener {
            abrirAplicativos()
        }

        findViewById<View>(
            resources.getIdentifier(
                "categoryTrash",
                "id",
                packageName
            )
        )?.setOnClickListener {
            abrirLixeira()
        }

        findViewById<View>(
            resources.getIdentifier(
                "categoryAnalysis",
                "id",
                packageName
            )
        )?.setOnClickListener {
            analisarArmazenamento()
        }

        findViewById<View>(
            resources.getIdentifier(
                "backButton",
                "id",
                packageName
            )
        )?.setOnClickListener {
            voltar()
        }
    }

    private fun configurarBusca() {

        searchEdit.setOnEditorActionListener { _, _, _ ->

            executarBusca(
                searchEdit.text.toString()
            )

            true
        }

        searchEdit.setOnFocusChangeListener { _, hasFocus ->

            if (!hasFocus) {

                val texto =
                    searchEdit.text.toString()

                if (texto.length >= 2) {
                    executarBusca(texto)
                }
            }
        }
    }

    // ============================================================
    // ARMAZENAMENTO
    // ============================================================

    private fun atualizarArmazenamento() {

        try {

            val stat =
                android.os.StatFs(
                    Environment.getExternalStorageDirectory().path
                )

            val total =
                stat.totalBytes

            val livre =
                stat.availableBytes

            val usado =
                total - livre

            val porcentagem =
                if (total > 0) {
                    ((usado * 100.0) / total)
                        .toInt()
                        .coerceIn(0, 100)
                } else {
                    0
                }

            storageProgress.max = 100
            storageProgress.progress = porcentagem

            storageInfo.text =
                "${formatarTamanho(usado)} usados de ${formatarTamanho(total)}"

        } catch (_: Exception) {

            storageInfo.text =
                "Armazenamento indisponível"
        }
    }

    private fun formatarTamanho(bytes: Long): String {

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

        var indice = 0

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

    private fun verificarPermissao() {

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.R
        ) {

            if (
                !Environment.isExternalStorageManager()
            ) {
                // Não abre automaticamente a tela de configurações.
            }
        }
    }

    // ============================================================
    // ARMAZENAMENTO
    // ============================================================

    private fun abrirArmazenamento() {

        pastaAtual =
            Environment.getExternalStorageDirectory()

        tipoDeMidiaAtual = 0

        mostrarTelaArquivos(
            "Armazenamento principal"
        )

        carregarArquivos(
            pastaAtual
        )
    }

    private fun abrirDownloads() {

        val pasta =
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )

        pastaAtual = pasta
        tipoDeMidiaAtual = 0

        mostrarTelaArquivos("Download")

        carregarArquivos(pasta)
    }

    private fun abrirDocumentos() {

        val pasta =
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS
            )

        pastaAtual = pasta
        tipoDeMidiaAtual = 0

        mostrarTelaArquivos("Documentos")

        carregarArquivos(pasta)
    }

    private fun abrirAudio() {

        pastaAtual =
            Environment.getExternalStorageDirectory()

        tipoDeMidiaAtual = 0

        mostrarTelaArquivos("Áudio")

        val resultado =
            buscarArquivosPorExtensao(
                pastaAtual,
                EXTENSOES_AUDIO
            )

        fileListData =
            resultado.toMutableList()

        fileList.adapter =
            FileListAdapter(fileListData)

        currentPath.text =
            "${resultado.size} arquivo(s) de áudio"
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
        tipo: Int,
        pastas: List<File>
    ) {

        tipoDeMidiaAtual = tipo

        homeScroll.visibility =
            View.GONE

        fileScreen.visibility =
            View.VISIBLE

        fileScreenTitle.text =
            titulo

        fileList.visibility =
            View.GONE

        mediaGrid.visibility =
            View.VISIBLE

        buscarPastasDeMidia(
            pastas
        )
    }

    private fun buscarPastasDeMidia(
        pastasRaiz: List<File>
    ) {

        thumbnailExecutor.execute {

            val grupos =
                LinkedHashMap<String, MutableList<File>>()

            for (raiz in pastasRaiz) {

                if (
                    raiz.exists() &&
                    raiz.isDirectory
                ) {

                    buscarMidiasRecursivamente(
                        raiz,
                        grupos
                    )
                }
            }

            val lista =
                grupos.map {

                    PastaMedia(
                        File(it.key),
                        it.value
                    )

                }.sortedBy {

                    it.pasta.name.lowercase(
                        Locale.getDefault()
                    )
                }

            runOnUiThread {

                mediaGrid.adapter =
                    PastaMediaAdapter(lista)

                currentPath.text =
                    "${lista.size} pasta(s)"
            }
        }
    }

    private fun buscarMidiasRecursivamente(
        pasta: File,
        grupos: MutableMap<String, MutableList<File>>
    ) {

        val arquivos =
            try {
                pasta.listFiles()
            } catch (_: Exception) {
                null
            }
                ?: return

        for (arquivo in arquivos) {

            if (arquivo.isDirectory) {

                if (
                    !arquivo.name.startsWith(".")
                ) {

                    buscarMidiasRecursivamente(
                        arquivo,
                        grupos
                    )
                }

            } else if (arquivo.isFile) {

                val extensao =
                    arquivo.extension.lowercase(
                        Locale.getDefault()
                    )

                val corresponde =
                    if (
                        tipoDeMidiaAtual ==
                        TIPO_IMAGEM
                    ) {
                        extensao in EXTENSOES_IMAGEM
                    } else {
                        extensao in EXTENSOES_VIDEO
                    }

                if (corresponde) {

                    val chave =
                        arquivo.parentFile
                            ?.absolutePath
                            ?: continue

                    if (
                        !grupos.containsKey(chave)
                    ) {
                        grupos[chave] =
                            mutableListOf()
                    }

                    grupos[chave]!!.add(
                        arquivo
                    )
                }
            }
        }
    }

    private fun abrirPastaDeMidia(
        pasta: File
    ) {

        pastaAtual = pasta

        mediaGrid.visibility =
            View.VISIBLE

        fileList.visibility =
            View.GONE

        thumbnailExecutor.execute {

            val resultado =
                buscarMidiasDaPasta(
                    pasta
                )

            runOnUiThread {

                mediaGrid.adapter =
                    MediaAdapter(
                        resultado
                    )

                currentPath.text =
                    if (
                        tipoDeMidiaAtual ==
                        TIPO_VIDEO
                    ) {
                        "${resultado.size} vídeo(s)"
                    } else {
                        "${resultado.size} imagem(ns)"
                    }
            }
        }
    }

    private fun buscarMidiasDaPasta(
        pasta: File
    ): List<File> {

        val arquivos =
            try {
                pasta.listFiles()
            } catch (_: Exception) {
                null
            }
                ?: return emptyList()

        return arquivos
            .filter {
                it.isFile &&
                        (
                                if (
                                    tipoDeMidiaAtual ==
                                    TIPO_VIDEO
                                ) {
                                    it.extension.lowercase(
                                        Locale.getDefault()
                                    ) in EXTENSOES_VIDEO
                                } else {
                                    it.extension.lowercase(
                                        Locale.getDefault()
                                    ) in EXTENSOES_IMAGEM
                                }
                                )
            }
            .sortedBy {
                it.name.lowercase(
                    Locale.getDefault()
                )
            }
    }

    // ============================================================
    // APLICATIVOS INSTALADOS
    // ============================================================

    private fun abrirAplicativos() {

        homeScroll.visibility =
            View.GONE

        fileScreen.visibility =
            View.VISIBLE

        fileScreenTitle.text =
            "Aplicativos"

        currentPath.text =
            "Carregando aplicativos..."

        mediaGrid.visibility =
            View.GONE

        fileList.visibility =
            View.VISIBLE

        thumbnailExecutor.execute {

            val aplicativos =
                carregarAplicativosInstalados()

            runOnUiThread {

                fileList.adapter =
                    AppListAdapter(
                        aplicativos
                    )

                currentPath.text =
                    "${aplicativos.size} aplicativos instalados"

                if (aplicativos.isEmpty()) {

                    Toast.makeText(
                        this,
                        "Nenhum aplicativo encontrado",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun carregarAplicativosInstalados():
            List<AppInfoItem> {

        val pm =
            packageManager

        val resultado =
            mutableListOf<AppInfoItem>()

        try {

            /*
             * Usamos ACTION_MAIN + CATEGORY_LAUNCHER
             * para encontrar os aplicativos que possuem
             * um ícone e podem ser iniciados pelo usuário.
             *
             * Isso é mais confiável para um gerenciador
             * de arquivos do que simplesmente mostrar
             * todos os pacotes internos do Android.
             */

            val intent =
                Intent(
                    Intent.ACTION_MAIN,
                    null
                ).apply {
                    addCategory(
                        Intent.CATEGORY_LAUNCHER
                    )
                }

            val atividades =
                if (
                    android.os.Build.VERSION.SDK_INT >=
                    android.os.Build.VERSION_CODES.TIRAMISU
                ) {

                    pm.queryIntentActivities(
                        intent,
                        PackageManager.ResolveInfoFlags.of(
                            PackageManager.MATCH_ALL.toLong()
                        )
                    )

                } else {

                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(
                        intent,
                        PackageManager.MATCH_ALL
                    )
                }

            val pacotesAdicionados =
                HashSet<String>()

            for (resolveInfo in atividades) {

                try {

                    val info =
                        resolveInfo.activityInfo
                            ?.applicationInfo
                            ?: continue

                    val pacote =
                        info.packageName

                    if (
                        pacotesAdicionados.contains(
                            pacote
                        )
                    ) {
                        continue
                    }

                    pacotesAdicionados.add(
                        pacote
                    )

                    val nome =
                        info.loadLabel(pm)
                            .toString()
                            .trim()

                    if (
                        nome.isNotEmpty()
                    ) {

                        resultado.add(
                            AppInfoItem(
                                info,
                                nome
                            )
                        )
                    }

                } catch (_: Exception) {
                }
            }

        } catch (_: Exception) {

            /*
             * Fallback para aparelhos onde a consulta
             * acima apresentar alguma limitação.
             */

            try {

                val aplicativos =
                    if (
                        android.os.Build.VERSION.SDK_INT >=
                        android.os.Build.VERSION_CODES.TIRAMISU
                    ) {

                        pm.getInstalledApplications(
                            PackageManager.ApplicationInfoFlags.of(
                                PackageManager.GET_META_DATA.toLong()
                            )
                        )

                    } else {

                        @Suppress("DEPRECATION")
                        pm.getInstalledApplications(
                            PackageManager.GET_META_DATA
                        )
                    }

                for (app in aplicativos) {

                    try {

                        val launchIntent =
                            pm.getLaunchIntentForPackage(
                                app.packageName
                            )

                        if (
                            launchIntent != null
                        ) {

                            val nome =
                                app.loadLabel(pm)
                                    .toString()
                                    .trim()

                            if (
                                nome.isNotEmpty()
                            ) {

                                resultado.add(
                                    AppInfoItem(
                                        app,
                                        nome
                                    )
                                )
                            }
                        }

                    } catch (_: Exception) {
                    }
                }

            } catch (_: Exception) {
            }
        }

        return resultado
            .distinctBy {
                it.info.packageName
            }
            .sortedBy {
                it.nome.lowercase(
                    Locale.getDefault()
                )
            }
    }

    private fun abrirAplicativo(
        app: AppInfoItem
    ) {

        try {

            val intent =
                packageManager.getLaunchIntentForPackage(
                    app.info.packageName
                )

            if (intent != null) {

                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                startActivity(intent)

            } else {

                Toast.makeText(
                    this,
                    "Não foi possível abrir o aplicativo",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Erro ao abrir o aplicativo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================================
    // LIXEIRA
    // ============================================================

    private fun obterPastaLixeira(): File {

        return File(
            Environment.getExternalStorageDirectory(),
            ".GerenciadorArquivos/.Lixeira"
        )
    }

    private fun abrirLixeira() {

        val lixeira =
            obterPastaLixeira()

        if (!lixeira.exists()) {
            lixeira.mkdirs()
        }

        pastaAtual = lixeira
        tipoDeMidiaAtual = 0

        mostrarTelaArquivos("Lixeira")

        carregarArquivos(lixeira)
    }

    // ============================================================
    // TELA DE ARQUIVOS
    // ============================================================

    private fun mostrarTelaArquivos(
        titulo: String
    ) {

        homeScroll.visibility =
            View.GONE

        fileScreen.visibility =
            View.VISIBLE

        fileScreenTitle.text =
            titulo

        mediaGrid.visibility =
            View.GONE

        fileList.visibility =
            View.VISIBLE
    }

    private fun carregarArquivos(
        pasta: File
    ) {

        val arquivos =
            try {
                pasta.listFiles()
            } catch (_: Exception) {
                null
            }

        fileListData =
            arquivos
                ?.filter {
                    !it.name.startsWith(".")
                }
                ?.sortedWith(
                    compareBy<File> {
                        !it.isDirectory
                    }.thenBy {
                        it.name.lowercase(
                            Locale.getDefault()
                        )
                    }
                )
                ?.toMutableList()
                ?: mutableListOf()

        fileList.adapter =
            FileListAdapter(
                fileListData
            )

        currentPath.text =
            pasta.absolutePath
    }

    // ============================================================
    // BUSCA
    // ============================================================

    private fun executarBusca(
        textoOriginal: String
    ) {

        val texto =
            textoOriginal.trim()

        if (texto.length < 2) {
            return
        }

        homeScroll.visibility =
            View.GONE

        fileScreen.visibility =
            View.VISIBLE

        fileScreenTitle.text =
            "Resultados da busca"

        mediaGrid.visibility =
            View.GONE

        fileList.visibility =
            View.VISIBLE

        currentPath.text =
            "Procurando..."

        thumbnailExecutor.execute {

            val encontrados =
                mutableListOf<File>()

            buscarRecursivamente(
                Environment.getExternalStorageDirectory(),
                texto.lowercase(
                    Locale.getDefault()
                ),
                encontrados
            )

            runOnUiThread {

                fileListData =
                    encontrados.toMutableList()

                fileList.adapter =
                    FileListAdapter(
                        fileListData
                    )

                currentPath.text =
                    "${encontrados.size} resultado(s)"
            }
        }
    }

    private fun buscarRecursivamente(
        pasta: File,
        termo: String,
        resultado: MutableList<File>
    ) {

        if (resultado.size >= 300) {
            return
        }

        val arquivos =
            try {
                pasta.listFiles()
            } catch (_: Exception) {
                null
            }
                ?: return

        for (arquivo in arquivos) {

            if (resultado.size >= 300) {
                return
            }

            if (
                arquivo.name
                    .lowercase(
                        Locale.getDefault()
                    )
                    .contains(termo)
            ) {

                resultado.add(
                    arquivo
                )
            }

            if (
                arquivo.isDirectory &&
                !arquivo.name.startsWith(".")
            ) {

                buscarRecursivamente(
                    arquivo,
                    termo,
                    resultado
                )
            }
        }
    }

    // ============================================================
    // ABERTURA DE ARQUIVOS
    // ============================================================

    private fun abrirArquivo(
        arquivo: File
    ) {

        if (!arquivo.exists()) {

            Toast.makeText(
                this,
                "Arquivo não encontrado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (arquivo.isDirectory) {

            pastaAtual = arquivo
            tipoDeMidiaAtual = 0

            mostrarTelaArquivos(
                arquivo.name
            )

            carregarArquivos(
                arquivo
            )

            return
        }

        val extensao =
            arquivo.extension.lowercase(
                Locale.getDefault()
            )

        try {

            when {

                extensao in EXTENSOES_IMAGEM -> {

                    val intent =
                        Intent(
                            this,
                            ImageViewerActivity::class.java
                        )

                    intent.putExtra(
                        "filePath",
                        arquivo.absolutePath
                    )

                    startActivity(intent)
                }

                extensao in EXTENSOES_VIDEO -> {

                    val intent =
                        Intent(
                            this,
                            VideoViewerActivity::class.java
                        )

                    intent.putExtra(
                        "filePath",
                        arquivo.absolutePath
                    )

                    startActivity(intent)
                }

                extensao == "pdf" -> {

                    val intent =
                        Intent(
                            this,
                            PdfViewerActivity::class.java
                        )

                    intent.putExtra(
                        "filePath",
                        arquivo.absolutePath
                    )

                    startActivity(intent)
                }

                extensao == "zip" ||
                        extensao == "rar" ||
                        extensao == "7z" -> {

                    val intent =
                        Intent(
                            this,
                            ZipViewerActivity::class.java
                        )

                    intent.putExtra(
                        "filePath",
                        arquivo.absolutePath
                    )

                    startActivity(intent)
                }

                else -> {

                    abrirComAplicativoExterno(
                        arquivo
                    )
                }
            }

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Não foi possível abrir o arquivo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun obterTipoMime(
        arquivo: File
    ): String {

        return when (
            arquivo.extension.lowercase(
                Locale.getDefault()
            )
        ) {

            "jpg",
            "jpeg" ->
                "image/jpeg"

            "png" ->
                "image/png"

            "gif" ->
                "image/gif"

            "webp" ->
                "image/webp"

            "mp4" ->
                "video/mp4"

            "mkv" ->
                "video/x-matroska"

            "avi" ->
                "video/x-msvideo"

            "mov" ->
                "video/quicktime"

            "mp3" ->
                "audio/mpeg"

            "wav" ->
                "audio/wav"

            "pdf" ->
                "application/pdf"

            "zip" ->
                "application/zip"

            "txt" ->
                "text/plain"

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

            else ->
                "*/*"
        }
    }

    private fun abrirComAplicativoExterno(
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
                obterTipoMime(arquivo)
            )

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            startActivity(intent)

        } catch (_: ActivityNotFoundException) {

            Toast.makeText(
                this,
                "Nenhum aplicativo consegue abrir este arquivo",
                Toast.LENGTH_SHORT
            ).show()

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Erro ao abrir o arquivo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================================
    // BUSCA POR EXTENSÃO
    // ============================================================

    private fun buscarArquivosPorExtensao(
        pasta: File,
        extensoes: Set<String>
    ): List<File> {

        val resultado =
            mutableListOf<File>()

        buscarExtensoesRecursivamente(
            pasta,
            extensoes,
            resultado
        )

        return resultado
    }

    private fun buscarExtensoesRecursivamente(
        pasta: File,
        extensoes: Set<String>,
        resultado: MutableList<File>
    ) {

        if (resultado.size >= 500) {
            return
        }

        val arquivos =
            try {
                pasta.listFiles()
            } catch (_: Exception) {
                null
            }
                ?: return

        for (arquivo in arquivos) {

            if (resultado.size >= 500) {
                return
            }

            if (arquivo.isDirectory) {

                if (
                    !arquivo.name.startsWith(".")
                ) {

                    buscarExtensoesRecursivamente(
                        arquivo,
                        extensoes,
                        resultado
                    )
                }

            } else {

                if (
                    arquivo.extension.lowercase(
                        Locale.getDefault()
                    ) in extensoes
                ) {

                    resultado.add(
                        arquivo
                    )
                }
            }
        }
    }

    // ============================================================
    // ANÁLISE DO ARMAZENAMENTO
    // ============================================================

    private fun analisarArmazenamento() {

        val categorias =
            linkedMapOf(
                "Imagens" to EXTENSOES_IMAGEM,
                "Vídeos" to EXTENSOES_VIDEO,
                "Áudio" to EXTENSOES_AUDIO,
                "Documentos" to EXTENSOES_DOCUMENTOS
            )

        thumbnailExecutor.execute {

            val contagem =
                linkedMapOf<String, Long>()

            for ((nome, extensoes) in categorias) {

                var tamanho = 0L

                val arquivos =
                    buscarArquivosPorExtensao(
                        Environment.getExternalStorageDirectory(),
                        extensoes
                    )

                for (arquivo in arquivos) {
                    tamanho += arquivo.length()
                }

                contagem[nome] = tamanho
            }

            runOnUiThread {

                val mensagem =
                    buildString {

                        append(
                            "Uso por categoria:\n\n"
                        )

                        for ((nome, tamanho) in contagem) {

                            append(
                                "$nome: ${formatarTamanho(tamanho)}\n"
                            )
                        }
                    }

                AlertDialog.Builder(this)
                    .setTitle(
                        "Análise do armazenamento"
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
                arquivo.extension.lowercase(
                    Locale.getDefault()
                )

            if (
                extensao in EXTENSOES_VIDEO
            ) {

                val retriever =
                    MediaMetadataRetriever()

                retriever.setDataSource(
                    arquivo.absolutePath
                )

                val bitmap =
                    if (
                        android.os.Build.VERSION.SDK_INT >=
                        android.os.Build.VERSION_CODES.O_MR1
                    ) {

                        retriever.getScaledFrameAtTime(
                            0,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            tamanho,
                            tamanho
                        )

                    } else {

                        retriever.getFrameAtTime(
                            0,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )
                    }

                retriever.release()

                bitmap

            } else {

                val options =
                    BitmapFactory.Options()

                options.inSampleSize =
                    calcularSampleSize(
                        arquivo,
                        tamanho
                    )

                BitmapFactory.decodeFile(
                    arquivo.absolutePath,
                    options
                )
            }

        } catch (_: Exception) {

            null
        }
    }

    private fun calcularSampleSize(
        arquivo: File,
        tamanho: Int
    ): Int {

        return try {

            val options =
                BitmapFactory.Options()

            options.inJustDecodeBounds =
                true

            BitmapFactory.decodeFile(
                arquivo.absolutePath,
                options
            )

            var sample =
                1

            while (
                options.outWidth / sample > tamanho * 2 ||
                options.outHeight / sample > tamanho * 2
            ) {

                sample *= 2
            }

            sample

        } catch (_: Exception) {

            2
        }
    }

    // ============================================================
    // VOLTAR — DIRETO PARA A TELA INICIAL
    // ============================================================

    private fun voltar() {

        /*
         * Não importa em qual pasta/subpasta o usuário esteja:
         *
         * Imagens
         *   -> DCIM
         *      -> WhatsApp
         *
         * ao apertar voltar, retorna diretamente para
         * a tela inicial do Gerenciador de Arquivos+.
         */

        fileScreen.visibility =
            View.GONE

        homeScroll.visibility =
            View.VISIBLE

        mediaGrid.visibility =
            View.GONE

        fileList.visibility =
            View.VISIBLE

        pastaAtual =
            Environment.getExternalStorageDirectory()

        tipoDeMidiaAtual = 0

        searchEdit.clearFocus()

        atualizarArmazenamento()
    }

    // ============================================================
    // ADAPTER DE ARQUIVOS
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
                dp(12),
                dp(8),
                dp(12),
                dp(8)
            )

            val icone =
                ImageView(
                    this@MainActivity
                )

            icone.layoutParams =
                LinearLayout.LayoutParams(
                    dp(48),
                    dp(48)
                )

            icone.setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(5)
            )

            if (arquivo.isDirectory) {

                icone.setImageResource(
                    android.R.drawable.ic_menu_agenda
                )

            } else {

                icone.setImageResource(
                    obterIconeArquivo(
                        arquivo
                    )
                )
            }

            linha.addView(
                icone
            )

            val textos =
                LinearLayout(
                    this@MainActivity
                )

            textos.orientation =
                LinearLayout.VERTICAL

            textos.gravity =
                Gravity.CENTER_VERTICAL

            textos.layoutParams =
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )

            val nome =
                TextView(
                    this@MainActivity
                )

            nome.text =
                arquivo.name

            nome.textSize =
                15f

            nome.setTextColor(
                Color.DKGRAY
            )

            nome.maxLines =
                1

            nome.ellipsize =
                TextUtils.TruncateAt.END

            textos.addView(
                nome
            )

            val detalhe =
                TextView(
                    this@MainActivity
                )

            detalhe.text =
                if (arquivo.isDirectory) {
                    "Pasta"
                } else {
                    formatarTamanho(
                        arquivo.length()
                    )
                }

            detalhe.textSize =
                12f

            detalhe.setTextColor(
                Color.GRAY
            )

            textos.addView(
                detalhe
            )

            linha.addView(
                textos
            )

            linha.setOnClickListener {

                abrirArquivo(
                    arquivo
                )
            }

            linha.setOnLongClickListener {

                mostrarMenuArquivo(
                    arquivo
                )

                true
            }

            return linha
        }
    }

    // ============================================================
    // ADAPTER DE APLICATIVOS
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

            val app =
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
                dp(12),
                dp(8),
                dp(12),
                dp(8)
            )

            val icone =
                ImageView(
                    this@MainActivity
                )

            icone.layoutParams =
                LinearLayout.LayoutParams(
                    dp(52),
                    dp(52)
                )

            try {

                icone.setImageDrawable(
                    app.info.loadIcon(
                        packageManager
                    )
                )

            } catch (_: Exception) {

                icone.setImageResource(
                    android.R.drawable.sym_def_app_icon
                )
            }

            linha.addView(
                icone
            )

            val textos =
                LinearLayout(
                    this@MainActivity
                )

            textos.orientation =
                LinearLayout.VERTICAL

            textos.gravity =
                Gravity.CENTER_VERTICAL

            textos.layoutParams =
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )

            val nome =
                TextView(
                    this@MainActivity
                )

            nome.text =
                app.nome

            nome.textSize =
                16f

            nome.setTextColor(
                Color.DKGRAY
            )

            nome.maxLines =
                1

            nome.ellipsize =
                TextUtils.TruncateAt.END

            textos.addView(
                nome
            )

            val pacote =
                TextView(
                    this@MainActivity
                )

            pacote.text =
                app.info.packageName

            pacote.textSize =
                11f

            pacote.setTextColor(
                Color.GRAY
            )

            pacote.maxLines =
                1

            pacote.ellipsize =
                TextUtils.TruncateAt.END

            textos.addView(
                pacote
            )

            linha.addView(
                textos
            )

            linha.setOnClickListener {

                abrirAplicativo(
                    app
                )
            }

            linha.setOnLongClickListener {

                mostrarMenuAplicativo(
                    app
                )

                true
            }

            return linha
        }
    }

    // ============================================================
    // ADAPTER DAS PASTAS DE IMAGENS/VÍDEOS
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

            val container =
                FrameLayout(
                    this@MainActivity
                )

            container.setPadding(
                dp(4),
                dp(5),
                dp(4),
                dp(8)
            )

            val larguraTela =
                resources.displayMetrics.widthPixels

            val tamanhoColuna =
                larguraTela / 3

            val larguraImagem =
                tamanhoColuna - dp(16)

            val layout =
                LinearLayout(
                    this@MainActivity
                )

            layout.orientation =
                LinearLayout.VERTICAL

            layout.gravity =
                Gravity.CENTER_HORIZONTAL

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
                                larguraImagem
                            )

                        if (thumb != null) {

                            thumbnailCache.put(
                                chave,
                                thumb
                            )

                            runOnUiThread {

                                if (
                                    imagem.tag ==
                                    chave
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
                    larguraImagem,
                    dp(105)
                )
            )

            val nome =
                TextView(
                    this@MainActivity
                )

            nome.text =
                pasta.pasta.name

            nome.textSize =
                13f

            nome.setTextColor(
                Color.DKGRAY
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
                    dp(42)
                )
            )

            val quantidade =
                TextView(
                    this@MainActivity
                )

            quantidade.text =
                "(${pasta.arquivos.size})"

            quantidade.textSize =
                12f

            quantidade.setTextColor(
                Color.GRAY
            )

            quantidade.gravity =
                Gravity.CENTER

            quantidade.includeFontPadding =
                true

            layout.addView(
                quantidade,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(24)
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
                dp(185)

            container.setOnClickListener {

                abrirPastaDeMidia(
                    pasta.pasta
                )
            }

            container.setOnLongClickListener {

                mostrarMenuPastaMedia(
                    pasta
                )

                true
            }

            return container
        }
    }

    // ============================================================
    // ADAPTER DAS FOTOS/VÍDEOS
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

            val larguraTela =
                resources.displayMetrics.widthPixels

            val tamanho =
                (larguraTela / 3) - dp(8)

            val imagem =
                ImageView(
                    this@MainActivity
                )

            imagem.scaleType =
                ImageView.ScaleType.CENTER_CROP

            val arquivo =
                arquivos[position]

            val chave =
                arquivo.absolutePath +
                        "_" +
                        arquivo.lastModified()

            imagem.tag =
                chave

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
                            arquivo,
                            tamanho
                        )

                    if (thumb != null) {

                        thumbnailCache.put(
                            chave,
                            thumb
                        )

                        runOnUiThread {

                            if (
                                imagem.tag ==
                                chave
                            ) {

                                imagem.setImageBitmap(
                                    thumb
                                )
                            }
                        }
                    }
                }
            }

            container.addView(
                imagem,
                FrameLayout.LayoutParams(
                    tamanho,
                    tamanho
                )
            )

            container.setPadding(
                dp(2),
                dp(2),
                dp(2),
                dp(2)
            )

            container.setOnClickListener {

                abrirArquivo(
                    arquivo
                )
            }

            container.setOnLongClickListener {

                mostrarMenuArquivo(
                    arquivo
                )

                true
            }

            return container
        }
    }

    // ============================================================
    // MENUS
    // ============================================================

    private fun mostrarMenuArquivo(
        arquivo: File
    ) {

        val opcoes =
            arrayOf(
                "Abrir",
                "Compartilhar",
                "Copiar caminho",
                "Renomear",
                "Mover para lixeira",
                "Informações"
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
                        abrirArquivo(
                            arquivo
                        )

                    1 ->
                        compartilharArquivo(
                            arquivo
                        )

                    2 ->
                        copiarCaminho(
                            arquivo
                        )

                    3 ->
                        renomearArquivo(
                            arquivo
                        )

                    4 ->
                        moverParaLixeira(
                            arquivo
                        )

                    5 ->
                        mostrarInformacoes(
                            arquivo
                        )
                }
            }
            .show()
    }

    private fun mostrarMenuPastaMedia(
        pasta: PastaMedia
    ) {

        val opcoes =
            arrayOf(
                "Abrir pasta",
                "Compartilhar pasta",
                "Copiar caminho"
            )

        AlertDialog.Builder(this)
            .setTitle(
                pasta.pasta.name
            )
            .setItems(
                opcoes
            ) { _, escolha ->

                when (escolha) {

                    0 ->
                        abrirPastaDeMidia(
                            pasta.pasta
                        )

                    1 ->
                        compartilharArquivo(
                            pasta.pasta
                        )

                    2 ->
                        copiarCaminho(
                            pasta.pasta
                        )
                }
            }
            .show()
    }

    private fun mostrarMenuAplicativo(
        app: AppInfoItem
    ) {

        val opcoes =
            arrayOf(
                "Abrir",
                "Informações do aplicativo"
            )

        AlertDialog.Builder(this)
            .setTitle(
                app.nome
            )
            .setItems(
                opcoes
            ) { _, escolha ->

                when (escolha) {

                    0 ->
                        abrirAplicativo(
                            app
                        )

                    1 -> {

                        try {

                            val intent =
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                )

                            intent.data =
                                Uri.parse(
                                    "package:${app.info.packageName}"
                                )

                            startActivity(intent)

                        } catch (_: Exception) {
                        }
                    }
                }
            }
            .show()
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
                if (arquivo.isDirectory) {
                    "*/*"
                } else {
                    obterTipoMime(
                        arquivo
                    )
                }

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
                    "Compartilhar"
                )
            )

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Não foi possível compartilhar",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================================
    // COPIAR CAMINHO
    // ============================================================

    private fun copiarCaminho(
        arquivo: File
    ) {

        val clipboard =
            getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "Caminho",
                arquivo.absolutePath
            )
        )

        Toast.makeText(
            this,
            "Caminho copiado",
            Toast.LENGTH_SHORT
        ).show()
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
                .setTitle(
                    "Renomear"
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
                    campo.text.toString().trim()

                if (
                    novoNome.isEmpty()
                ) {
                    return@setOnClickListener
                }

                val novoArquivo =
                    File(
                        arquivo.parentFile,
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

                if (
                    arquivo.renameTo(
                        novoArquivo
                    )
                ) {

                    dialog.dismiss()

                    carregarArquivos(
                        pastaAtual
                    )

                    Toast.makeText(
                        this,
                        "Renomeado com sucesso",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this,
                        "Não foi possível renomear",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        dialog.show()
    }

    // ============================================================
    // LIXEIRA
    // ============================================================

    private fun moverParaLixeira(
        arquivo: File
    ) {

        try {

            val lixeira =
                obterPastaLixeira()

            if (!lixeira.exists()) {
                lixeira.mkdirs()
            }

            var destino =
                File(
                    lixeira,
                    arquivo.name
                )

            var contador = 1

            while (destino.exists()) {

                destino =
                    File(
                        lixeira,
                        "${arquivo.name}($contador)"
                    )

                contador++
            }

            if (
                arquivo.renameTo(
                    destino
                )
            ) {

                Toast.makeText(
                    this,
                    "Movido para a lixeira",
                    Toast.LENGTH_SHORT
                ).show()

                carregarArquivos(
                    pastaAtual
                )

            } else {

                Toast.makeText(
                    this,
                    "Não foi possível mover para a lixeira",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "Erro ao mover para a lixeira",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================================
    // INFORMAÇÕES
    // ============================================================

    private fun mostrarInformacoes(
        arquivo: File
    ) {

        val texto =
            buildString {

                append(
                    "Nome: ${arquivo.name}\n\n"
                )

                append(
                    "Caminho:\n${arquivo.absolutePath}\n\n"
                )

                append(
                    "Tipo: ${
                        if (
                            arquivo.isDirectory
                        ) {
                            "Pasta"
                        } else {
                            arquivo.extension
                        }
                    }\n\n"
                )

                if (arquivo.isFile) {

                    append(
                        "Tamanho: ${
                            formatarTamanho(
                                arquivo.length()
                            )
                        }\n\n"
                    )
                }

                append(
                    "Modificado:\n${java.text.SimpleDateFormat(
                        "dd/MM/yyyy HH:mm",
                        Locale.getDefault()
                    ).format(
                        java.util.Date(
                            arquivo.lastModified()
                        )
                    )}"
                )
            }

        AlertDialog.Builder(this)
            .setTitle(
                "Informações"
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

    // ============================================================
    // ÍCONES
    // ============================================================

    private fun obterIconeArquivo(
        arquivo: File
    ): Int {

        val extensao =
            arquivo.extension.lowercase(
                Locale.getDefault()
            )

        return when {

            extensao in EXTENSOES_IMAGEM ->
                android.R.drawable.ic_menu_gallery

            extensao in EXTENSOES_VIDEO ->
                android.R.drawable.ic_media_play

            extensao in EXTENSOES_AUDIO ->
                android.R.drawable.ic_media_play

            extensao == "pdf" ->
                android.R.drawable.ic_menu_view

            extensao in EXTENSOES_DOCUMENTOS ->
                android.R.drawable.ic_menu_edit

            extensao == "zip" ||
                    extensao == "rar" ||
                    extensao == "7z" ->
                android.R.drawable.ic_menu_save

            else ->
                android.R.drawable.ic_menu_help
        }
    }

    // ============================================================
    // DP
    // ============================================================

    private fun dp(
        valor: Int
    ): Int {

        return (
                valor *
                        resources.displayMetrics.density
                ).toInt()
    }

    // ============================================================
    // COPIAR ARQUIVO / PASTA
    // ============================================================

    private fun copiarArquivo(
        origem: File,
        destino: File
    ) {

        try {

            if (origem.isDirectory) {

                if (!destino.exists()) {
                    destino.mkdirs()
                }

                origem.listFiles()
                    ?.forEach { filho ->

                        copiarArquivo(
                            filho,
                            File(
                                destino,
                                filho.name
                            )
                        )
                    }

            } else {

                FileInputStream(
                    origem
                ).use { input ->

                    FileOutputStream(
                        destino
                    ).use { output ->

                        val buffer =
                            ByteArray(
                                8192
                            )

                        var quantidade: Int

                        while (
                            input.read(
                                buffer
                            ).also {
                                quantidade = it
                            } > 0
                        ) {

                            output.write(
                                buffer,
                                0,
                                quantidade
                            )
                        }
                    }
                }
            }

        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {

        super.onDestroy()

        thumbnailExecutor.shutdownNow()

        thumbnailCache.evictAll()
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
