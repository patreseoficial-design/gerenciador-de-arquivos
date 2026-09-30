package com.gerenciadordearquivos.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    // HOME
    private lateinit var homeScroll: ScrollView

    // TELA DE ARQUIVOS
    private lateinit var fileScreen: LinearLayout
    private lateinit var fileScreenTitle: TextView
    private lateinit var currentPath: TextView
    private lateinit var fileList: ListView
    private lateinit var mediaGrid: GridView

    // ARMAZENAMENTO
    private lateinit var storageInfo: TextView
    private lateinit var storageProgress: ProgressBar

    // PESQUISA
    private lateinit var searchEdit: EditText

    // CAMINHO ATUAL
    private val rootPath: File =
        Environment.getExternalStorageDirectory()

    private var currentDirectory: File = rootPath


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        inicializarViews()
        configurarBotoes()
        configurarPesquisa()
        atualizarArmazenamento()

        verificarPermissao()
    }


    // =========================================================
    // INICIALIZAÇÃO
    // =========================================================

    private fun inicializarViews() {

        homeScroll = findViewById(R.id.homeScroll)

        fileScreen = findViewById(R.id.fileScreen)
        fileScreenTitle = findViewById(R.id.fileScreenTitle)
        currentPath = findViewById(R.id.currentPath)

        fileList = findViewById(R.id.fileList)
        mediaGrid = findViewById(R.id.mediaGrid)

        storageInfo = findViewById(R.id.storageInfo)
        storageProgress = findViewById(R.id.storageProgress)

        searchEdit = findViewById(R.id.searchEdit)
    }


    // =========================================================
    // BOTÕES DA HOME
    // =========================================================

    private fun configurarBotoes() {

        findViewById<View>(R.id.categoryStorage)
            .setOnClickListener {
                abrirPasta(rootPath, "Armazenamento")
            }

        findViewById<View>(R.id.categoryDownloads)
            .setOnClickListener {
                val pasta = Environment
                    .getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )

                abrirPasta(pasta, "Downloads")
            }

        findViewById<View>(R.id.categoryImages)
            .setOnClickListener {
                abrirImagens()
            }

        findViewById<View>(R.id.categoryVideos)
            .setOnClickListener {
                abrirVideos()
            }

        findViewById<View>(R.id.categoryAudio)
            .setOnClickListener {
                abrirAudio()
            }

        findViewById<View>(R.id.categoryDocuments)
            .setOnClickListener {
                abrirDocumentos()
            }

        findViewById<View>(R.id.categoryApps)
            .setOnClickListener {
                abrirAplicativos()
            }

        findViewById<View>(R.id.categoryTrash)
            .setOnClickListener {
                abrirLixeira()
            }

        findViewById<View>(R.id.categoryAnalysis)
            .setOnClickListener {
                analisarArmazenamento()
            }

        findViewById<View>(R.id.backButton)
            .setOnClickListener {
                voltar()
            }
    }


    // =========================================================
    // PESQUISA
    // =========================================================

    private fun configurarPesquisa() {

        searchEdit.addTextChangedListener(
            object : TextWatcher {

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
                    val texto = s
                        ?.toString()
                        ?.trim()
                        ?: ""

                    if (texto.length >= 2) {
                        pesquisarArquivos(texto)
                    }
                }

                override fun afterTextChanged(s: Editable?) {
                }
            }
        )
    }


    // =========================================================
    // ABRIR PASTA
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

        currentDirectory = pasta

        homeScroll.visibility = View.GONE
        fileScreen.visibility = View.VISIBLE

        fileScreenTitle.text = titulo
        currentPath.text = pasta.absolutePath

        mediaGrid.visibility = View.GONE
        fileList.visibility = View.VISIBLE

        carregarArquivos(pasta)
    }


    // =========================================================
    // ARQUIVOS NORMAIS
    // =========================================================

    private fun carregarArquivos(pasta: File) {

        thread {

            val arquivos = try {

                pasta.listFiles()
                    ?.sortedWith(
                        compareBy<File> {
                            !it.isDirectory
                        }.thenBy {
                            it.name.lowercase(Locale.getDefault())
                        }
                    )
                    ?: emptyList()

            } catch (e: Exception) {
                emptyList()
            }

            runOnUiThread {

                val nomes = arquivos.map { arquivo ->

                    if (arquivo.isDirectory) {
                        "📁  ${arquivo.name}"
                    } else {
                        "📄  ${arquivo.name}"
                    }
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    nomes
                )

                fileList.adapter = adapter

                fileList.setOnItemClickListener {
                        _, _, position, _ ->

                    val arquivo = arquivos[position]

                    if (arquivo.isDirectory) {

                        abrirPasta(
                            arquivo,
                            arquivo.name
                        )

                    } else {

                        abrirArquivo(arquivo)
                    }
                }
            }
        }
    }


    // =========================================================
    // IMAGENS
    // =========================================================

    private fun abrirImagens() {

        homeScroll.visibility = View.GONE
        fileScreen.visibility = View.VISIBLE

        fileScreenTitle.text = "Imagens"
        currentPath.text = "DCIM / Pictures"

        fileList.visibility = View.GONE
        mediaGrid.visibility = View.VISIBLE

        buscarMidias(
            tipos = TIPO_IMAGEM,
            pastas = listOf(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM
                ),
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                )
            )
        )
    }


    // =========================================================
    // VÍDEOS
    // =========================================================

    private fun abrirVideos() {

        homeScroll.visibility = View.GONE
        fileScreen.visibility = View.VISIBLE

        fileScreenTitle.text = "Vídeos"
        currentPath.text = "DCIM / Movies"

        fileList.visibility = View.GONE
        mediaGrid.visibility = View.VISIBLE

        buscarMidias(
            tipos = TIPO_VIDEO,
            pastas = listOf(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM
                ),
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MOVIES
                )
            )
        )
    }


    // =========================================================
    // BUSCA DE MÍDIAS
    // =========================================================

    private fun buscarMidias(
        tipos: Set<String>,
        pastas: List<File>
    ) {

        mediaGrid.adapter = null

        Toast.makeText(
            this,
            "Carregando...",
            Toast.LENGTH_SHORT
        ).show()

        thread {

            val resultado = ArrayList<File>()

            fun procurar(pasta: File) {

                if (resultado.size >= 500) {
                    return
                }

                val lista = try {
                    pasta.listFiles()
                } catch (e: Exception) {
                    null
                }

                lista?.forEach { arquivo ->

                    if (resultado.size >= 500) {
                        return@forEach
                    }

                    if (arquivo.isDirectory) {

                        procurar(arquivo)

                    } else {

                        val extensao =
                            arquivo.extension
                                .lowercase(Locale.getDefault())

                        if (tipos.contains(extensao)) {
                            resultado.add(arquivo)
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
                    MediaAdapter(resultado)

                mediaGrid.setOnItemClickListener {
                        _, _, position, _ ->

                    if (position >= 0 &&
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


    // =========================================================
    // ÁUDIO
    // =========================================================

    private fun abrirAudio() {

        val pastas = listOf(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC
            ),
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_RECORDINGS
            )
        )

        abrirListaPorExtensao(
            "Áudio",
            pastas,
            TIPO_AUDIO
        )
    }


    // =========================================================
    // DOCUMENTOS
    // =========================================================

    private fun abrirDocumentos() {

        val pastas = listOf(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS
            ),
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
        )

        abrirListaPorExtensao(
            "Documentos",
            pastas,
            TIPO_DOCUMENTO
        )
    }


    private fun abrirListaPorExtensao(
        titulo: String,
        pastas: List<File>,
        tipos: Set<String>
    ) {

        homeScroll.visibility = View.GONE
        fileScreen.visibility = View.VISIBLE

        fileScreenTitle.text = titulo
        currentPath.text = "Buscando arquivos..."

        mediaGrid.visibility = View.GONE
        fileList.visibility = View.VISIBLE

        thread {

            val resultado = ArrayList<File>()

            fun procurar(pasta: File) {

                if (resultado.size >= 500) {
                    return
                }

                val lista = try {
                    pasta.listFiles()
                } catch (e: Exception) {
                    null
                }

                lista?.forEach { arquivo ->

                    if (resultado.size >= 500) {
                        return@forEach
                    }

                    if (arquivo.isDirectory) {

                        procurar(arquivo)

                    } else {

                        val extensao =
                            arquivo.extension
                                .lowercase(Locale.getDefault())

                        if (tipos.contains(extensao)) {
                            resultado.add(arquivo)
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
                it.name.lowercase(Locale.getDefault())
            }

            runOnUiThread {

                currentPath.text =
                    "${resultado.size} arquivo(s)"

                val nomes = resultado.map {

                    "📄  ${it.name}"
                }

                fileList.adapter =
                    ArrayAdapter(
                        this,
                        android.R.layout.simple_list_item_1,
                        nomes
                    )

                fileList.setOnItemClickListener {
                        _, _, position, _ ->

                    abrirArquivo(
                        resultado[position]
                    )
                }
            }
        }
    }


    // =========================================================
    // ABRIR APLICATIVOS
    // =========================================================

    private fun abrirAplicativos() {

        homeScroll.visibility = View.GONE
        fileScreen.visibility = View.VISIBLE

        fileScreenTitle.text = "Aplicativos"
        currentPath.text = "Aplicativos instalados"

        mediaGrid.visibility = View.GONE
        fileList.visibility = View.VISIBLE

        thread {

            val pm = packageManager

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
                            .lowercase(Locale.getDefault())
                    }

            runOnUiThread {

                val nomes = aplicativos.map {

                    pm.getApplicationLabel(it)
                        .toString()
                }

                fileList.adapter =
                    ArrayAdapter(
                        this,
                        android.R.layout.simple_list_item_1,
                        nomes
                    )

                fileList.setOnItemClickListener {
                        _, _, position, _ ->

                    val app =
                        aplicativos[position]

                    val intent =
                        pm.getLaunchIntentForPackage(
                            app.packageName
                        )

                    if (intent != null) {
                        startActivity(intent)
                    }
                }
            }
        }
    }


    // =========================================================
    // LIXEIRA
    // =========================================================

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


    // =========================================================
    // PESQUISA
    // =========================================================

    private fun pesquisarArquivos(
        texto: String
    ) {

        homeScroll.visibility = View.GONE
        fileScreen.visibility = View.VISIBLE

        fileScreenTitle.text = "Pesquisa"
        currentPath.text =
            "Resultados para: $texto"

        mediaGrid.visibility = View.GONE
        fileList.visibility = View.VISIBLE

        thread {

            val resultado =
                ArrayList<File>()

            fun procurar(pasta: File) {

                if (resultado.size >= 300) {
                    return
                }

                val lista = try {
                    pasta.listFiles()
                } catch (e: Exception) {
                    null
                }

                lista?.forEach { arquivo ->

                    if (resultado.size >= 300) {
                        return@forEach
                    }

                    if (arquivo.name
                            .contains(
                                texto,
                                ignoreCase = true
                            )
                    ) {
                        resultado.add(arquivo)
                    }

                    if (arquivo.isDirectory) {
                        procurar(arquivo)
                    }
                }
            }

            procurar(rootPath)

            runOnUiThread {

                val nomes =
                    resultado.map { arquivo ->

                        if (arquivo.isDirectory) {
                            "📁  ${arquivo.name}"
                        } else {
                            "📄  ${arquivo.name}"
                        }
                    }

                fileList.adapter =
                    ArrayAdapter(
                        this,
                        android.R.layout.simple_list_item_1,
                        nomes
                    )

                fileList.setOnItemClickListener {
                        _, _, position, _ ->

                    val arquivo =
                        resultado[position]

                    if (arquivo.isDirectory) {
                        abrirPasta(
                            arquivo,
                            arquivo.name
                        )
                    } else {
                        abrirArquivo(arquivo)
                    }
                }
            }
        }
    }


    // =========================================================
    // ABRIR ARQUIVO
    // =========================================================

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

        try {

            val uri =
                FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    arquivo
                )

            val tipo =
                obterTipoMime(arquivo)

            val intent =
                Intent(Intent.ACTION_VIEW).apply {

                    setDataAndType(
                        uri,
                        tipo
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    clipData =
                        android.content.ClipData.newRawUri(
                            "arquivo",
                            uri
                        )
                }

            try {

                startActivity(intent)

            } catch (e: ActivityNotFoundException) {

                // Segunda tentativa usando MIME genérico
                val intentGenerico =
                    Intent(Intent.ACTION_VIEW).apply {

                        setDataAndType(
                            uri,
                            "*/*"
                        )

                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                        clipData =
                            android.content.ClipData.newRawUri(
                                "arquivo",
                                uri
                            )
                    }

                try {

                    startActivity(intentGenerico)

                } catch (e2: ActivityNotFoundException) {

                    Toast.makeText(
                        this,
                        "Não existe aplicativo para abrir este arquivo",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Erro ao abrir: ${e.message}",
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
            arquivo.extension
                .lowercase(Locale.getDefault())
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

            "rtf" ->
                "application/rtf"

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
    // MINIATURA
    // =========================================================

    private fun carregarMiniatura(
        arquivo: File
    ): Bitmap? {

        return try {

            if (!arquivo.exists()) {
                return null
            }

            val options =
                BitmapFactory.Options()

            options.inJustDecodeBounds = true

            BitmapFactory.decodeFile(
                arquivo.absolutePath,
                options
            )

            val largura =
                options.outWidth

            val altura =
                options.outHeight

            if (largura <= 0 ||
                altura <= 0
            ) {
                return null
            }

            var sample = 1

            val maior =
                maxOf(
                    largura,
                    altura
                )

            while (
                maior / sample > 300
            ) {
                sample *= 2
            }

            val optionsFinal =
                BitmapFactory.Options()

            optionsFinal.inSampleSize =
                sample

            optionsFinal.inPreferredConfig =
                Bitmap.Config.RGB_565

            BitmapFactory.decodeFile(
                arquivo.absolutePath,
                optionsFinal
            )

        } catch (e: OutOfMemoryError) {

            null

        } catch (e: Exception) {

            null
        }
    }


    // =========================================================
    // VOLTAR
    // =========================================================

    private fun voltar() {

        if (fileScreen.visibility != View.VISIBLE) {
            return
        }

        if (currentDirectory != rootPath &&
            currentDirectory.parentFile != null
        ) {

            val pai =
                currentDirectory.parentFile

            if (pai != null) {

                currentDirectory = pai

                abrirPasta(
                    pai,
                    pai.name.ifEmpty {
                        "Armazenamento"
                    }
                )

                return
            }
        }

        fileScreen.visibility = View.GONE
        homeScroll.visibility = View.VISIBLE

        mediaGrid.adapter = null
        fileList.adapter = null
    }


    // =========================================================
    // ARMAZENAMENTO
    // =========================================================

    private fun atualizarArmazenamento() {

        thread {

            try {

                val total =
                    rootPath.totalSpace

                val disponivel =
                    rootPath.freeSpace

                val usado =
                    total - disponivel

                val percentual =
                    (
                        usado.toDouble() /
                            total.toDouble()
                        ) * 100.0

                val totalGb =
                    total /
                        (1024.0 * 1024.0 * 1024.0)

                val disponivelGb =
                    disponivel /
                        (1024.0 * 1024.0 * 1024.0)

                runOnUiThread {

                    storageInfo.text =
                        String.format(
                            Locale.getDefault(),
                            "%.1f GB usados • %.1f GB livres",
                            totalGb - disponivelGb,
                            disponivelGb
                        )

                    storageProgress.progress =
                        percentual.toInt()
                }

            } catch (e: Exception) {
            }
        }
    }


    // =========================================================
    // ANÁLISE
    // =========================================================

    private fun analisarArmazenamento() {

        Toast.makeText(
            this,
            "Analisando armazenamento...",
            Toast.LENGTH_SHORT
        ).show()

        thread {

            var arquivos = 0
            var pastas = 0
            var tamanho = 0L

            fun analisar(pasta: File) {

                val lista = try {
                    pasta.listFiles()
                } catch (e: Exception) {
                    null
                }

                lista?.forEach { arquivo ->

                    if (arquivo.isDirectory) {

                        pastas++
                        analisar(arquivo)

                    } else {

                        arquivos++

                        try {
                            tamanho +=
                                arquivo.length()
                        } catch (e: Exception) {
                        }
                    }
                }
            }

            analisar(rootPath)

            val tamanhoMb =
                tamanho /
                    (1024.0 * 1024.0)

            runOnUiThread {

                Toast.makeText(
                    this,
                    "$arquivos arquivos • $pastas pastas • " +
                            String.format(
                                Locale.getDefault(),
                                "%.1f MB analisados",
                                tamanhoMb
                            ),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // =========================================================
    // PERMISSÃO
    // =========================================================

    private fun verificarPermissao() {

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.R
        ) {

            if (
                !Environment.isExternalStorageManager()
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

                    startActivity(intent)

                } catch (e: Exception) {

                    try {

                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                            )
                        )

                    } catch (e2: Exception) {
                    }
                }
            }
        }
    }


    // =========================================================
    // ADAPTER DE MINIATURAS
    // =========================================================

    private inner class MediaAdapter(
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

            val container: LinearLayout
            val thumbnail: ImageView
            val nome: TextView

            if (convertView == null) {

                container =
                    LinearLayout(this@MainActivity)

                container.orientation =
                    LinearLayout.VERTICAL

                container.gravity =
                    Gravity.CENTER

                container.setPadding(
                    3,
                    3,
                    3,
                    3
                )

                thumbnail =
                    ImageView(this@MainActivity)

                thumbnail.scaleType =
                    ImageView.ScaleType.CENTER_CROP

                val tamanho =
                    (parent.resources.displayMetrics.widthPixels / 3) - 12

                container.addView(
                    thumbnail,
                    LinearLayout.LayoutParams(
                        tamanho,
                        tamanho
                    )
                )

                nome =
                    TextView(this@MainActivity)

                nome.textSize =
                    11f

                nome.setTextColor(
                    android.graphics.Color.DKGRAY
                )

                nome.gravity =
                    Gravity.CENTER

                nome.maxLines = 2

                nome.ellipsize =
                    android.text.TextUtils.TruncateAt.END

                container.addView(
                    nome,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        45
                    )
                )

                container.tag =
                    ViewHolderMedia(
                        thumbnail,
                        nome
                    )

            } else {

                container =
                    convertView as LinearLayout

                val holder =
                    container.tag as ViewHolderMedia

                thumbnail =
                    holder.thumbnail

                nome =
                    holder.nome
            }

            val arquivo =
                arquivos[position]

            nome.text =
                arquivo.name

            thumbnail.tag =
                arquivo.absolutePath

            thumbnail.setImageResource(
                android.R.drawable.ic_menu_gallery
            )

            thread {

                val bitmap =
                    carregarMiniatura(
                        arquivo
                    )

                runOnUiThread {

                    if (
                        thumbnail.tag ==
                        arquivo.absolutePath
                    ) {

                        if (bitmap != null) {

                            thumbnail.setImageBitmap(
                                bitmap
                            )

                        } else {

                            thumbnail.setImageResource(
                                android.R.drawable.ic_menu_gallery
                            )
                        }
                    }
                }
            }

            return container
        }
    }


    private data class ViewHolderMedia(
        val thumbnail: ImageView,
        val nome: TextView
    )


    // =========================================================
    // TIPOS
    // =========================================================

    companion object {

        val TIPO_IMAGEM =
            setOf(
                "jpg",
                "jpeg",
                "png",
                "gif",
                "webp",
                "bmp",
                "heic",
                "heif"
            )

        val TIPO_VIDEO =
            setOf(
                "mp4",
                "m4v",
                "mkv",
                "avi",
                "mov",
                "3gp",
                "webm"
            )

        val TIPO_AUDIO =
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

        val TIPO_DOCUMENTO =
            setOf(
                "pdf",
                "txt",
                "csv",
                "html",
                "htm",
                "rtf",
                "doc",
                "docx",
                "xls",
                "xlsx",
                "ppt",
                "pptx"
            )
    }
}
