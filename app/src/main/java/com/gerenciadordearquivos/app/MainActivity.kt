package com.gerenciadordearquivos.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
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

    private lateinit var storageInfo: TextView
    private lateinit var storageProgress: ProgressBar
    private lateinit var searchEdit: EditText

    private val rootPath: File =
        Environment.getExternalStorageDirectory()

    private var currentDirectory: File =
        rootPath

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

            abrirLixeira()
        }

        findViewById<View>(
            R.id.categoryAnalysis
        ).setOnClickListener {

            analisarArmazenamento()
        }

        findViewById<View>(
            R.id.backButton
        ).setOnClickListener {

            voltar()
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
                        pesquisarArquivos(texto)
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

        currentDirectory =
            pasta

        homeScroll.visibility =
            View.GONE

        fileScreen.visibility =
            View.VISIBLE

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

    private fun carregarArquivos(
        pasta: File
    ) {

        thread {

            val arquivos =
                try {

                    pasta.listFiles()
                        ?.sortedWith(
                            compareBy<File> {
                                !it.isDirectory
                            }.thenBy {
                                it.name.lowercase(
                                    Locale.getDefault()
                                )
                            }
                        )
                        ?: emptyList()

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
                        _,
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

    private fun abrirImagens() {

        homeScroll.visibility =
            View.GONE

        fileScreen.visibility =
            View.VISIBLE

        fileScreenTitle.text =
            "Imagens"

        currentPath.text =
            "DCIM / Pictures"

        fileList.visibility =
            View.GONE

        mediaGrid.visibility =
            View.VISIBLE

        buscarMidias(
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

    private fun abrirVideos() {

        homeScroll.visibility =
            View.GONE

        fileScreen.visibility =
            View.VISIBLE

        fileScreenTitle.text =
            "Vídeos"

        currentPath.text =
            "DCIM / Movies"

        fileList.visibility =
            View.GONE

        mediaGrid.visibility =
            View.VISIBLE

        buscarMidias(
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

    private fun buscarMidias(
        tipos: Set<String>,
        pastas: List<File>
    ) {

        mediaGrid.adapter =
            null

        Toast.makeText(
            this,
            "Carregando...",
            Toast.LENGTH_SHORT
        ).show()

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

            resultado.sortByDescending {
                it.lastModified()
            }

            runOnUiThread {

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

                        abrirArquivo(
                            resultado[position]
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

                        mostrarMenuImagem(
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

    private fun abrirAudio() {

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

    private fun abrirDocumentos() {

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
            }
        }
    }

    private fun abrirAplicativos() {

        homeScroll.visibility =
            View.GONE

        fileScreen.visibility =
            View.VISIBLE

        fileScreenTitle.text =
            "Aplicativos"

        currentPath.text =
            "Aplicativos instalados"

        mediaGrid.visibility =
            View.GONE

        fileList.visibility =
            View.VISIBLE

        thread {

            val pm =
                packageManager

            val aplicativos =
                pm.getInstalledApplications(
                    PackageManager.GET_META_DATA
                )
                    .filter {
                        pm.getLaunchIntentForPackage(
                            it.packageName
                        ) != null
                    }
                    .sortedBy {
                        pm.getApplicationLabel(it)
                            .toString()
                            .lowercase(
                                Locale.getDefault()
                            )
                    }

            runOnUiThread {

                val nomes =
                    aplicativos.map {
                        pm.getApplicationLabel(
                            it
                        ).toString()
                    }

                fileList.adapter =
                    ArrayAdapter(
                        this,
                        android.R.layout.simple_list_item_1,
                        nomes
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

                    val intent =
                        pm.getLaunchIntentForPackage(
                            aplicativos[position]
                                .packageName
                        )

                    if (intent != null) {
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun abrirLixeira() {

        val pasta =
            File(
                rootPath,
                ".GerenciadorArquivos/.Lixeira"
            )

        if (!pasta.exists()) {
            pasta.mkdirs()
        }

        abrirPasta(
            pasta,
            "Lixeira"
        )
    }

    private fun pesquisarArquivos(
        texto: String
    ) {

        homeScroll.visibility =
            View.GONE

        fileScreen.visibility =
            View.VISIBLE

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
                        arquivo.isDirectory
                    ) {

                        procurar(
                            arquivo
                        )
                    }
                }
            }

            procurar(
                rootPath
            )

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

            TIPO_IMAGEM.contains(
                extensao
            ) -> {

                abrirVisualizadorImagem(
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

            startActivity(
                intent
            )

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

            startActivity(
                intent
            )

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

            "txt" ->
                "text/plain"

            "html",
            "htm" ->
                "text/html"

            "jpg",
            "jpeg" ->
                "image/jpeg"

            "png" ->
                "image/png"

            "gif" ->
                "image/gif"

            "webp" ->
                "image/webp"

            "mp3" ->
                "audio/mpeg"

            "wav" ->
                "audio/wav"

            "m4a" ->
                "audio/mp4"

            "mp4" ->
                "video/mp4"

            "mkv" ->
                "video/x-matroska"

            "avi" ->
                "video/x-msvideo"

            "pdf" ->
                "application/pdf"

            "zip" ->
                "application/zip"

            "rar" ->
                "application/vnd.rar"

            "7z" ->
                "application/x-7z-compressed"

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

    private fun verificarPermissao() {

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.R
        ) {

            if (
                !Environment.isExternalStorageManager()
            ) {

                Toast.makeText(
                    this,
                    "Permita o acesso aos arquivos para usar o gerenciador",
                    Toast.LENGTH_LONG
                ).show()

                try {

                    val intent =
                        Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                        )

                    intent.data =
                        Uri.parse(
                            "package:$packageName"
                        )

                    startActivity(
                        intent
                    )

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
        }
    }

    private fun analisarArmazenamento() {

        homeScroll.visibility =
            View.GONE

        fileScreen.visibility =
            View.VISIBLE

        fileScreenTitle.text =
            "Análise do armazenamento"

        currentPath.text =
            "Calculando..."

        mediaGrid.visibility =
            View.GONE

        fileList.visibility =
            View.VISIBLE

        thread {

            val categorias =
                LinkedHashMap<String, Long>()

            categorias["Imagens"] =
                tamanhoPorExtensoes(
                    rootPath,
                    TIPO_IMAGEM
                )

            categorias["Vídeos"] =
                tamanhoPorExtensoes(
                    rootPath,
                    TIPO_VIDEO
                )

            categorias["Áudios"] =
                tamanhoPorExtensoes(
                    rootPath,
                    TIPO_AUDIO
                )

            categorias["Documentos"] =
                tamanhoPorExtensoes(
                    rootPath,
                    TIPO_DOCUMENTO
                )

            runOnUiThread {

                val linhas =
                    categorias.map {
                        "${it.key}: ${formatarBytes(it.value)}"
                    }

                currentPath.text =
                    "Espaço ocupado por categoria"

                fileList.adapter =
                    ArrayAdapter(
                        this,
                        android.R.layout.simple_list_item_1,
                        linhas
                    )

                fileList.setOnItemClickListener(
                    null
                )
            }
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

        procurar(
            pasta
        )

        return total
    }

    private fun voltar() {

        if (
            fileScreen.visibility !=
            View.VISIBLE
        ) {
            return
        }

        if (
            currentDirectory != rootPath &&
            currentDirectory.parentFile != null
        ) {

            val pai =
                currentDirectory.parentFile

            if (
                pai != null &&
                pai.absolutePath.startsWith(
                    rootPath.absolutePath
                )
            ) {

                abrirPasta(
                    pai,
                    if (
                        pai == rootPath
                    ) {
                        "Armazenamento"
                    } else {
                        pai.name
                    }
                )

                return
            }
        }

        fileScreen.visibility =
            View.GONE

        homeScroll.visibility =
            View.VISIBLE

        currentDirectory =
            rootPath

        searchEdit.setText(
            ""
        )
    }

    private fun obterIconeArquivo(
        arquivo: File
    ): Int {

        if (arquivo.isDirectory) {
            return android.R.drawable.ic_menu_agenda
        }

        return when (
            arquivo.extension.lowercase(
                Locale.getDefault()
            )
        ) {

            "jpg",
            "jpeg",
            "png",
            "webp",
            "gif",
            "bmp",
            "heic",
            "heif" ->
                android.R.drawable.ic_menu_gallery

            "mp4",
            "mkv",
            "avi",
            "mov",
            "3gp",
            "webm" ->
                android.R.drawable.ic_media_play

            "mp3",
            "wav",
            "ogg",
            "m4a",
            "aac",
            "flac",
            "opus" ->
                android.R.drawable.ic_media_ff

            "pdf" ->
                android.R.drawable.ic_menu_save

            "zip",
            "rar",
            "7z" ->
                android.R.drawable.ic_menu_sort_by_size

            else ->
                android.R.drawable.ic_menu_edit
        }
    }

    /*
     * ============================================================
     * MENU DE AÇÕES DAS IMAGENS/VÍDEOS
     * ============================================================
     */

    private fun mostrarMenuImagem(
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

        menu.orientation =
            LinearLayout.VERTICAL

        menu.setBackgroundColor(
            Color.WHITE
        )

        menu.setPadding(
            0,
            6,
            0,
            6
        )

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
            "✎",
            "Renomear"
        ) {

            renomearArquivo(
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

        val largura =
            (
                resources.displayMetrics.density *
                    250
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
                Color.WHITE
            )
        )

        popup.isOutsideTouchable =
            true

        popup.isFocusable =
            true

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
            x + largura > larguraTela
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

    private fun adicionarOpcaoMenu(
        menu: LinearLayout,
        icone: String,
        texto: String,
        acao: () -> Unit
    ) {

        val linha =
            LinearLayout(this)

        linha.orientation =
            LinearLayout.HORIZONTAL

        linha.gravity =
            Gravity.CENTER_VERTICAL

        linha.setPadding(
            18,
            0,
            18,
            0
        )

        linha.isClickable =
            true

        val simbolo =
            TextView(this)

        simbolo.text =
            icone

        simbolo.textSize =
            21f

        simbolo.gravity =
            Gravity.CENTER

        simbolo.setTextColor(
            Color.rgb(
                60,
                60,
                60
            )
        )

        linha.addView(
            simbolo,
            LinearLayout.LayoutParams(
                38,
                56
            )
        )

        val nome =
            TextView(this)

        nome.text =
            texto

        nome.textSize =
            15f

        nome.setTextColor(
            Color.rgb(
                40,
                40,
                40
            )
        )

        nome.gravity =
            Gravity.CENTER_VERTICAL

        linha.addView(
            nome,
            LinearLayout.LayoutParams(
                0,
                56,
                1f
            )
        )

        linha.setOnClickListener {
            acao()
        }

        menu.addView(
            linha,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                56
            )
        )
    }

    private fun mostrarInformacoes(
        arquivo: File
    ) {

        val tamanho =
            if (
                arquivo.isDirectory
            ) {

                calcularTamanhoPasta(
                    arquivo
                )

            } else {

                arquivo.length()
            }

        val tipo =
            if (
                arquivo.isDirectory
            ) {

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

        android.app.AlertDialog.Builder(
            this
        )
            .setTitle(
                "Informações"
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

    private fun calcularTamanhoPasta(
        pasta: File
    ): Long {

        var total =
            0L

        try {

            val arquivos =
                pasta.listFiles()
                    ?: return 0L

            for (
                arquivo in arquivos
            ) {

                if (
                    arquivo.isDirectory
                ) {

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
            android.app.AlertDialog.Builder(
                this
            )
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
                android.app.AlertDialog.BUTTON_POSITIVE
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

                val novoArquivo =
                    File(
                        arquivo.parentFile
                            ?: rootPath,
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

    private fun criarPastaNoLocal(
        local: File
    ) {

        val campo =
            EditText(this)

        campo.hint =
            "Nome da pasta"

        val dialog =
            android.app.AlertDialog.Builder(
                this
            )
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
                    "Criar",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                android.app.AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

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

                    return@setOnClickListener
                }

                if (
                    nome.contains("/")
                ) {

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

                if (
                    pasta.exists()
                ) {

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

        if (
            pastaAtual != rootPath
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

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                nomes
            )

        lista.adapter =
            adapter

        val dialog =
            android.app.AlertDialog.Builder(
                this
            )
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

            if (
                position < 0 ||
                position >= nomes.size
            ) {
                return@setOnItemClickListener
            }

            val escolhido =
                nomes[position]

            when {

                escolhido.startsWith(
                    "⬆"
                ) -> {

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

                escolhido.startsWith(
                    "＋"
                ) -> {

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

        android.app.AlertDialog.Builder(
            this
        )
            .setTitle(
                "Mover arquivo"
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

        if (
            destino.exists()
        ) {

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

    private fun moverParaLixeira(
        arquivo: File
    ) {

        val lixeira =
            File(
                rootPath,
                ".GerenciadorArquivos/.Lixeira"
            )

        try {

            if (
                !lixeira.exists()
            ) {
                lixeira.mkdirs()
            }

            val destino =
                File(
                    lixeira,
                    arquivo.name
                )

            if (
                destino.exists()
            ) {

                val nomeBase =
                    arquivo.nameWithoutExtension

                val extensao =
                    arquivo.extension

                var contador =
                    1

                var novoDestino =
                    destino

                while (
                    novoDestino.exists()
                ) {

                    val novoNome =
                        if (
                            extensao.isEmpty()
                        ) {

                            "${nomeBase}_$contador"

                        } else {

                            "${nomeBase}_$contador.$extensao"
                        }

                    novoDestino =
                        File(
                            lixeira,
                            novoNome
                        )

                    contador++
                }

                val sucesso =
                    arquivo.renameTo(
                        novoDestino
                    )

                if (sucesso) {

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

                return
            }

            val sucesso =
                arquivo.renameTo(
                    destino
                )

            if (sucesso) {

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

    private fun recarregarTelaAtual() {

        when (
            fileScreenTitle.text
                .toString()
        ) {

            "Imagens" -> {

                abrirImagens()
            }

            "Vídeos" -> {

                abrirVideos()
            }

            else -> {

                if (
                    currentDirectory.exists()
                ) {

                    carregarArquivos(
                        currentDirectory
                    )
                }
            }
        }
    }

    /*
     * ============================================================
     * LISTA DE ARQUIVOS
     * ============================================================
     */

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

            val linha =
                LinearLayout(
                    this@MainActivity
                )

            linha.orientation =
                LinearLayout.HORIZONTAL

            linha.gravity =
                Gravity.CENTER_VERTICAL

            linha.setPadding(
                14,
                10,
                14,
                10
            )

            val icone =
                ImageView(
                    this@MainActivity
                )

            icone.setImageResource(
                obterIconeArquivo(
                    arquivos[position]
                )
            )

            linha.addView(
                icone,
                LinearLayout.LayoutParams(
                    48,
                    48
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
                arquivos[position].name

            nome.textSize =
                15f

            nome.setTextColor(
                Color.DKGRAY
            )

            nome.maxLines =
                1

            nome.ellipsize =
                android.text.TextUtils.TruncateAt.END

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
                    arquivos[position].isDirectory
                ) {

                    "Pasta"

                } else {

                    formatarBytes(
                        arquivos[position].length()
                    )
                }

            info.textSize =
                12f

            info.setTextColor(
                Color.GRAY
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

    /*
     * ============================================================
     * GRID DE MÍDIA
     * ============================================================
     */

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

            if (
                TIPO_IMAGEM.contains(
                    extensao
                )
            ) {

                val chaveCache =
                    arquivo.absolutePath +
                            "_" +
                            arquivo.lastModified()

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
                        android.R.drawable.ic_menu_gallery
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
                                    imagem.parent != null
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

                imagem.setImageResource(
                    obterIconeArquivo(
                        arquivo
                    )
                )
            }

            return container
        }
    }

    private fun carregarMiniatura(
        arquivo: File,
        tamanho: Int
    ): Bitmap? {

        return try {

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
                larguraOriginal / escala > tamanho * 2 &&
                alturaOriginal / escala > tamanho * 2
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

    override fun onDestroy() {

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
