package com.gerenciadordearquivos.app

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale
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

    private var currentDirectory: File? = null

    private var currentMediaFiles =
        mutableListOf<File>()

    private val rootPath: File
        get() = Environment.getExternalStorageDirectory()


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )

        inicializarViews()
        configurarBotoes()
        atualizarArmazenamento()

        if (!temPermissao()) {
            pedirPermissao()
        }
    }


    // =========================================================
    // VIEWS
    // =========================================================

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


    // =========================================================
    // BOTÕES DA HOME
    // =========================================================

    private fun configurarBotoes() {

        findViewById<View>(
            R.id.categoryStorage
        ).setOnClickListener {

            abrirPasta(
                rootPath,
                "Armazenamento principal"
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


        searchEdit.setOnEditorActionListener {
                _,
                _,
                _ ->

            val texto =
                searchEdit.text
                    .toString()
                    .trim()

            if (texto.isNotEmpty()) {

                pesquisarArquivos(
                    texto
                )
            }

            true
        }
    }


    // =========================================================
    // ARMAZENAMENTO
    // =========================================================

    private fun atualizarArmazenamento() {

        thread {

            try {

                val stat =
                    android.os.StatFs(
                        rootPath.path
                    )

                val total =
                    stat.totalBytes

                val disponivel =
                    stat.availableBytes

                val usado =
                    total - disponivel

                val percentual =
                    (
                        usado.toDouble() /
                        total.toDouble() *
                        100
                    ).toInt()

                val totalGB =
                    total /
                        1024.0 /
                        1024.0 /
                        1024.0

                val usadoGB =
                    usado /
                        1024.0 /
                        1024.0 /
                        1024.0

                val disponivelGB =
                    disponivel /
                        1024.0 /
                        1024.0 /
                        1024.0


                runOnUiThread {

                    storageInfo.text =
                        String.format(
                            Locale.getDefault(),
                            "%.1f GB usados de %.1f GB\n%.1f GB livres",
                            usadoGB,
                            totalGB,
                            disponivelGB
                        )

                    storageProgress.progress =
                        percentual
                }

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }


    // =========================================================
    // IMAGENS
    // =========================================================

    private fun abrirImagens() {

        buscarMidias(
            titulo = "Imagens",
            pastas = listOf(
                File(rootPath, "DCIM"),
                File(rootPath, "Pictures")
            ),
            tipo = "imagem"
        )
    }


    // =========================================================
    // VÍDEOS
    // =========================================================

    private fun abrirVideos() {

        buscarMidias(
            titulo = "Vídeos",
            pastas = listOf(
                File(rootPath, "DCIM"),
                File(rootPath, "Movies")
            ),
            tipo = "video"
        )
    }


    // =========================================================
    // BUSCAR MÍDIAS
    // =========================================================

    private fun buscarMidias(
        titulo: String,
        pastas: List<File>,
        tipo: String
    ) {

        mostrarTelaArquivos(
            titulo
        )

        mostrarGrade()

        currentPath.text =
            "Procurando..."

        mediaGrid.adapter =
            null


        thread {

            val arquivos =
                mutableListOf<File>()


            for (pasta in pastas) {

                if (pasta.exists()) {

                    procurarMidias(
                        pasta,
                        arquivos,
                        tipo
                    )
                }
            }


            arquivos.sortBy {

                it.name.lowercase(
                    Locale.getDefault()
                )
            }


            runOnUiThread {

                currentMediaFiles =
                    arquivos


                currentPath.text =
                    "${arquivos.size} arquivo(s)"


                mediaGrid.adapter =
                    MediaAdapter(
                        arquivos
                    )


                mediaGrid.setOnItemClickListener {
                        _,
                        _,
                        position,
                        _ ->

                    if (
                        position <
                        currentMediaFiles.size
                    ) {

                        abrirArquivo(
                            currentMediaFiles[
                                position
                            ]
                        )
                    }
                }
            }
        }
    }


    private fun procurarMidias(
        pasta: File,
        resultado: MutableList<File>,
        tipo: String
    ) {

        if (resultado.size >= 500) {
            return
        }


        val arquivos =
            try {
                pasta.listFiles()
            } catch (e: Exception) {
                null
            }
                ?: return


        for (arquivo in arquivos) {

            if (arquivo.isDirectory) {

                if (
                    !arquivo.name
                        .startsWith(".")
                ) {

                    procurarMidias(
                        arquivo,
                        resultado,
                        tipo
                    )
                }

            } else {

                val correto =
                    if (tipo == "imagem") {

                        ehImagem(
                            arquivo
                        )

                    } else {

                        ehVideo(
                            arquivo
                        )
                    }


                if (correto) {

                    resultado.add(
                        arquivo
                    )
                }
            }


            if (resultado.size >= 500) {
                return
            }
        }
    }


    // =========================================================
    // ÁUDIO
    // =========================================================

    private fun abrirAudio() {

        buscarArquivosPorTipo(
            titulo = "Áudio",
            pastas = listOf(
                File(rootPath, "Music"),
                File(rootPath, "Recordings"),
                File(rootPath, "DCIM")
            ),
            tipo = "audio"
        )
    }


    // =========================================================
    // DOCUMENTOS
    // =========================================================

    private fun abrirDocumentos() {

        buscarArquivosPorTipo(
            titulo = "Documentos",
            pastas = listOf(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS
                ),
                File(rootPath, "Download")
            ),
            tipo = "documento"
        )
    }


    private fun buscarArquivosPorTipo(
        titulo: String,
        pastas: List<File>,
        tipo: String
    ) {

        mostrarTelaArquivos(
            titulo
        )

        mostrarLista()

        currentPath.text =
            "Procurando..."


        thread {

            val encontrados =
                mutableListOf<File>()


            for (pasta in pastas) {

                if (pasta.exists()) {

                    procurarTipo(
                        pasta,
                        encontrados,
                        tipo
                    )
                }
            }


            encontrados.sortBy {

                it.name.lowercase(
                    Locale.getDefault()
                )
            }


            runOnUiThread {

                currentPath.text =
                    "${encontrados.size} arquivo(s)"


                val nomes =
                    encontrados.map {

                        when (tipo) {

                            "audio" ->
                                "🎵  ${it.name}"

                            "documento" ->
                                "📄  ${it.name}"

                            else ->
                                "📄  ${it.name}"
                        }
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
                        position <
                        encontrados.size
                    ) {

                        abrirArquivo(
                            encontrados[
                                position
                            ]
                        )
                    }
                }
            }
        }
    }


    private fun procurarTipo(
        pasta: File,
        resultado: MutableList<File>,
        tipo: String
    ) {

        if (resultado.size >= 500) {
            return
        }


        val arquivos =
            try {
                pasta.listFiles()
            } catch (e: Exception) {
                null
            }
                ?: return


        for (arquivo in arquivos) {

            if (arquivo.isDirectory) {

                if (
                    !arquivo.name
                        .startsWith(".")
                ) {

                    procurarTipo(
                        arquivo,
                        resultado,
                        tipo
                    )
                }

            } else {

                val correto =
                    when (tipo) {

                        "audio" ->
                            ehAudio(
                                arquivo
                            )

                        "documento" ->
                            ehDocumento(
                                arquivo
                            )

                        else ->
                            false
                    }


                if (correto) {

                    resultado.add(
                        arquivo
                    )
                }
            }


            if (resultado.size >= 500) {
                return
            }
        }
    }


    // =========================================================
    // ARMAZENAMENTO PRINCIPAL
    // =========================================================

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


        mostrarTelaArquivos(
            titulo
        )

        mostrarLista()

        currentDirectory =
            pasta

        carregarArquivos(
            pasta
        )
    }


    private fun mostrarTelaArquivos(
        titulo: String
    ) {

        homeScroll.visibility =
            View.GONE

        fileScreen.visibility =
            View.VISIBLE

        fileScreenTitle.text =
            titulo
    }


    private fun mostrarLista() {

        fileList.visibility =
            View.VISIBLE

        mediaGrid.visibility =
            View.GONE
    }


    private fun mostrarGrade() {

        fileList.visibility =
            View.GONE

        mediaGrid.visibility =
            View.VISIBLE
    }


    // =========================================================
    // LISTAR PASTA
    // =========================================================

    private fun carregarArquivos(
        pasta: File
    ) {

        currentDirectory =
            pasta

        currentPath.text =
            pasta.absolutePath


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

            } catch (e: Exception) {

                null
            }
                ?: emptyList()


        val nomes =
            arquivos.map {

                if (it.isDirectory) {

                    "📁  ${it.name}"

                } else {

                    when {

                        ehImagem(it) ->
                            "🖼️  ${it.name}"

                        ehVideo(it) ->
                            "🎬  ${it.name}"

                        ehAudio(it) ->
                            "🎵  ${it.name}"

                        ehDocumento(it) ->
                            "📄  ${it.name}"

                        else ->
                            "📄  ${it.name}"
                    }
                }
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
                position >= arquivos.size
            ) {
                return@setOnItemClickListener
            }


            val arquivo =
                arquivos[position]


            if (arquivo.isDirectory) {

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


    // =========================================================
    // APLICATIVOS
    // =========================================================

    private fun abrirAplicativos() {

        mostrarTelaArquivos(
            "Aplicativos"
        )

        mostrarLista()


        val pm =
            packageManager


        val aplicativos =
            pm.getInstalledApplications(0)
                .filter {

                    pm.getLaunchIntentForPackage(
                        it.packageName
                    ) != null

                }
                .sortedBy {

                    pm.getApplicationLabel(
                        it
                    ).toString().lowercase(
                        Locale.getDefault()
                    )
                }


        currentPath.text =
            "${aplicativos.size} aplicativos instalados"


        val adapter =
            object : BaseAdapter() {

                override fun getCount():
                    Int {

                    return aplicativos.size
                }


                override fun getItem(
                    position: Int
                ): Any {

                    return aplicativos[
                        position
                    ]
                }


                override fun getItemId(
                    position: Int
                ): Long {

                    return position.toLong()
                }


                override fun getView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {

                    val layout =
                        LinearLayout(
                            this@MainActivity
                        )

                    layout.orientation =
                        LinearLayout.HORIZONTAL

                    layout.gravity =
                        android.view.Gravity
                            .CENTER_VERTICAL

                    layout.setPadding(
                        20,
                        10,
                        20,
                        10
                    )


                    val icon =
                        ImageView(
                            this@MainActivity
                        )


                    icon.layoutParams =
                        LinearLayout.LayoutParams(
                            55,
                            55
                        )


                    icon.setImageDrawable(
                        aplicativos[
                            position
                        ].loadIcon(pm)
                    )


                    val nome =
                        TextView(
                            this@MainActivity
                        )


                    nome.text =
                        pm.getApplicationLabel(
                            aplicativos[
                                position
                            ]
                        )


                    nome.textSize =
                        16f

                    nome.setTextColor(
                        android.graphics.Color
                            .DKGRAY
                    )

                    nome.setPadding(
                        20,
                        0,
                        0,
                        0
                    )


                    nome.layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            -1,
                            1f
                        )


                    layout.addView(
                        icon
                    )

                    layout.addView(
                        nome
                    )


                    return layout
                }
            }


        fileList.adapter =
            adapter


        fileList.setOnItemClickListener {
                _,
                _,
                position,
                _ ->

            val aplicativo =
                aplicativos[
                    position
                ]


            val intent =
                pm.getLaunchIntentForPackage(
                    aplicativo.packageName
                )


            if (intent != null) {

                startActivity(
                    intent
                )
            }
        }
    }


    // =========================================================
    // LIXEIRA
    // =========================================================

    private fun abrirLixeira() {

        val lixeira =
            File(
                rootPath,
                ".GerenciadorArquivos/.Lixeira"
            )


        if (!lixeira.exists()) {
            lixeira.mkdirs()
        }


        abrirPasta(
            lixeira,
            "Lixeira"
        )
    }


    // =========================================================
    // PESQUISA
    // =========================================================

    private fun pesquisarArquivos(
        termo: String
    ) {

        mostrarTelaArquivos(
            "Pesquisa"
        )

        mostrarLista()

        currentDirectory =
            null

        currentPath.text =
            "Procurando por \"$termo\"..."


        fileList.adapter =
            ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                listOf(
                    "Pesquisando..."
                )
            )


        thread {

            val encontrados =
                mutableListOf<File>()


            procurarRecursivamente(
                rootPath,
                termo.lowercase(
                    Locale.getDefault()
                ),
                encontrados
            )


            runOnUiThread {

                currentPath.text =
                    "${encontrados.size} resultado(s)"


                val nomes =
                    encontrados.map {

                        when {

                            it.isDirectory ->
                                "📁  ${it.name}"

                            ehImagem(it) ->
                                "🖼️  ${it.name}"

                            ehVideo(it) ->
                                "🎬  ${it.name}"

                            ehAudio(it) ->
                                "🎵  ${it.name}"

                            else ->
                                "📄  ${it.name}"
                        }
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
                        position >= encontrados.size
                    ) {
                        return@setOnItemClickListener
                    }


                    val arquivo =
                        encontrados[
                            position
                        ]


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


    private fun procurarRecursivamente(
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
            } catch (e: Exception) {
                null
            }
                ?: return


        for (arquivo in arquivos) {

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
                !arquivo.name
                    .startsWith(".")
            ) {

                procurarRecursivamente(
                    arquivo,
                    termo,
                    resultado
                )
            }


            if (resultado.size >= 300) {
                return
            }
        }
    }


    // =========================================================
    // ANÁLISE
    // =========================================================

    private fun analisarArmazenamento() {

        atualizarArmazenamento()

        Toast.makeText(
            this,
            "Armazenamento atualizado",
            Toast.LENGTH_SHORT
        ).show()
    }


    // =========================================================
    // ABRIR ARQUIVO
    // =========================================================

    private fun abrirArquivo(
        arquivo: File
    ) {

        try {

            if (!arquivo.exists() || !arquivo.isFile) {

                Toast.makeText(
                    this,
                    "Arquivo não encontrado",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }


            val uri =
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    arquivo
                )


            val tipo =
                obterTipoMime(
                    arquivo
                )


            val intent =
                Intent(
                    Intent.ACTION_VIEW
                ).apply {

                    setDataAndType(
                        uri,
                        tipo
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    clipData =
                        ClipData.newRawUri(
                            "arquivo",
                            uri
                        )
                }


            // Abre diretamente.
            // NÃO usa "Abrir com".
            startActivity(
                intent
            )


        } catch (e: ActivityNotFoundException) {

            Toast.makeText(
                this,
                "Não há aplicativo instalado para abrir este tipo de arquivo",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Erro ao abrir arquivo: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // =========================================================
    // MIME
    // =========================================================

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

            "bmp" ->
                "image/bmp"

            "heic" ->
                "image/heic"

            "heif" ->
                "image/heif"


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


            "mp3" ->
                "audio/mpeg"

            "wav" ->
                "audio/wav"

            "ogg" ->
                "audio/ogg"

            "m4a" ->
                "audio/mp4"

            "aac" ->
                "audio/aac"

            "flac" ->
                "audio/flac"

            "opus" ->
                "audio/opus"

            "amr" ->
                "audio/amr"


            "pdf" ->
                "application/pdf"

            "txt" ->
                "text/plain"

            "csv" ->
                "text/csv"

            "html",
            "htm" ->
                "text/html"


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


            "zip" ->
                "application/zip"

            "rar" ->
                "application/vnd.rar"

            "7z" ->
                "application/x-7z-compressed"


            "apk" ->
                "application/vnd.android.package-archive"


            else ->
                "*/*"
        }
    }


    // =========================================================
    // TIPOS DE ARQUIVO
    // =========================================================

    private fun ehImagem(
        arquivo: File
    ): Boolean {

        return when (
            arquivo.extension.lowercase(
                Locale.getDefault()
            )
        ) {

            "jpg",
            "jpeg",
            "png",
            "gif",
            "webp",
            "bmp",
            "heic",
            "heif" -> true

            else -> false
        }
    }


    private fun ehVideo(
        arquivo: File
    ): Boolean {

        return when (
            arquivo.extension.lowercase(
                Locale.getDefault()
            )
        ) {

            "mp4",
            "mkv",
            "avi",
            "mov",
            "3gp",
            "webm",
            "m4v" -> true

            else -> false
        }
    }


    private fun ehAudio(
        arquivo: File
    ): Boolean {

        return when (
            arquivo.extension.lowercase(
                Locale.getDefault()
            )
        ) {

            "mp3",
            "wav",
            "ogg",
            "m4a",
            "aac",
            "flac",
            "opus",
            "amr" -> true

            else -> false
        }
    }


    private fun ehDocumento(
        arquivo: File
    ): Boolean {

        return when (
            arquivo.extension.lowercase(
                Locale.getDefault()
            )
        ) {

            "pdf",
            "txt",
            "doc",
            "docx",
            "xls",
            "xlsx",
            "ppt",
            "pptx",
            "csv",
            "rtf" -> true

            else -> false
        }
    }


    // =========================================================
    // VOLTAR
    // =========================================================

    override fun onBackPressed() {

        if (
            fileScreen.visibility ==
            View.VISIBLE
        ) {

            val pastaAtual =
                currentDirectory


            if (
                pastaAtual != null &&
                pastaAtual.absolutePath !=
                rootPath.absolutePath
            ) {

                val pai =
                    pastaAtual.parentFile


                if (
                    pai != null &&
                    pai.exists()
                ) {

                    abrirPasta(
                        pai,
                        pai.name
                    )

                    return
                }
            }


            fileScreen.visibility =
                View.GONE

            homeScroll.visibility =
                View.VISIBLE

            return
        }


        super.onBackPressed()
    }


    // =========================================================
    // PERMISSÃO
    // =========================================================

    private fun temPermissao(): Boolean {

        return if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.R
        ) {

            Environment.isExternalStorageManager()

        } else {

            true
        }
    }


    private fun pedirPermissao() {

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.R
        ) {

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

            } catch (e: Exception) {

                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    )
                )
            }
        }
    }


    // =========================================================
    // ADAPTER DAS MINIATURAS
    // =========================================================

    inner class MediaAdapter(
        private val arquivos: List<File>
    ) : BaseAdapter() {

        override fun getCount(): Int {
            return arquivos.size
        }


        override fun getItem(
            position: Int
        ): Any {

            return arquivos[position]
        }


        override fun getItemId(
            position: Int
        ): Long {

            return position.toLong()
        }


        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {

            val view =
                convertView
                    ?: LayoutInflater
                        .from(this@MainActivity)
                        .inflate(
                            R.layout.item_media,
                            parent,
                            false
                        )


            val image =
                view.findViewById<ImageView>(
                    R.id.mediaThumbnail
                )


            val name =
                view.findViewById<TextView>(
                    R.id.mediaName
                )


            val arquivo =
                arquivos[position]


            name.text =
                arquivo.name


            // Marca a ImageView com o arquivo atual.
            image.tag =
                arquivo.absolutePath


            image.setImageResource(
                android.R.drawable.ic_menu_gallery
            )


            thread {

                val bitmap =
                    carregarMiniatura(
                        arquivo
                    )


                runOnUiThread {

                    // Verifica se a ImageView ainda
                    // pertence ao mesmo arquivo.
                    if (
                        image.tag ==
                        arquivo.absolutePath
                    ) {

                        if (bitmap != null) {

                            image.setImageBitmap(
                                bitmap
                            )
                        }
                    }
                }
            }


            return view
        }
    }


    // =========================================================
    // MINIATURAS
    // =========================================================

    private fun carregarMiniatura(
        arquivo: File
    ): Bitmap? {

        return try {

            if (ehImagem(arquivo)) {

                // Primeiro descobre o tamanho
                // sem carregar a imagem.
                val opcoes =
                    BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }


                BitmapFactory.decodeFile(
                    arquivo.absolutePath,
                    opcoes
                )


                val larguraOriginal =
                    opcoes.outWidth

                val alturaOriginal =
                    opcoes.outHeight


                if (
                    larguraOriginal <= 0 ||
                    alturaOriginal <= 0
                ) {
                    return null
                }


                // Tamanho máximo da miniatura.
                val tamanhoMaximo =
                    300


                var escala =
                    1


                while (
                    larguraOriginal / escala >
                        tamanhoMaximo ||
                    alturaOriginal / escala >
                        tamanhoMaximo
                ) {

                    escala *= 2
                }


                val opcoesFinais =
                    BitmapFactory.Options().apply {

                        inSampleSize =
                            escala

                        // RGB_565 usa menos memória
                        // para miniaturas.
                        inPreferredConfig =
                            Bitmap.Config.RGB_565

                        inDither =
                            true
                    }


                BitmapFactory.decodeFile(
                    arquivo.absolutePath,
                    opcoesFinais
                )


            } else if (ehVideo(arquivo)) {

                ThumbnailUtils.createVideoThumbnail(
                    arquivo.absolutePath,
                    MediaStore.Video.Thumbnails.MINI_KIND
                )

            } else {

                null
            }

        } catch (e: OutOfMemoryError) {

            System.gc()

            null

        } catch (e: Exception) {

            e.printStackTrace()

            null
        }
    }
}
