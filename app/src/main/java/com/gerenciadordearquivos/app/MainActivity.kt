package com.gerenciadordearquivos.app

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    private lateinit var homeScroll: View
    private lateinit var fileScreen: View
    private lateinit var fileScreenTitle: TextView
    private lateinit var currentPath: TextView
    private lateinit var fileList: LinearLayout
    private lateinit var mediaGrid: androidx.recyclerview.widget.RecyclerView
    private lateinit var storageInfo: TextView
    private lateinit var storageProgress: ProgressBar
    private lateinit var searchEdit: EditText

    private val rootPath =
        Environment.getExternalStorageDirectory()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        homeScroll = findViewById(R.id.homeScroll)
        fileScreen = findViewById(R.id.fileScreen)
        fileScreenTitle = findViewById(R.id.fileScreenTitle)
        currentPath = findViewById(R.id.currentPath)
        fileList = findViewById(R.id.fileList)
        mediaGrid = findViewById(R.id.mediaGrid)
        storageInfo = findViewById(R.id.storageInfo)
        storageProgress = findViewById(R.id.storageProgress)
        searchEdit = findViewById(R.id.searchEdit)

        atualizarArmazenamento()

        configurarBotoes()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {

            if (!Environment.isExternalStorageManager()) {

                try {

                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName")
                    )

                    startActivity(intent)

                } catch (e: Exception) {

                    val intent = Intent(
                        Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    )

                    startActivity(intent)
                }
            }
        }
    }


    private fun configurarBotoes() {

        findViewById<View>(R.id.btnArmazenamento)
            .setOnClickListener {

                carregarArquivos(
                    rootPath,
                    "Armazenamento"
                )
            }


        findViewById<View>(R.id.btnDownloads)
            .setOnClickListener {

                val pasta =
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )

                carregarArquivos(
                    pasta,
                    "Downloads"
                )
            }


        findViewById<View>(R.id.btnImagens)
            .setOnClickListener {

                buscarMidias(
                    listOf(
                        File(rootPath, "DCIM"),
                        File(rootPath, "Pictures")
                    ),
                    true
                )
            }


        findViewById<View>(R.id.btnVideos)
            .setOnClickListener {

                buscarMidias(
                    listOf(
                        File(rootPath, "DCIM"),
                        File(rootPath, "Movies")
                    ),
                    false
                )
            }


        findViewById<View>(R.id.btnAudio)
            .setOnClickListener {

                buscarAudio()
            }


        findViewById<View>(R.id.btnDocumentos)
            .setOnClickListener {

                buscarDocumentos()
            }


        findViewById<View>(R.id.btnAplicativos)
            .setOnClickListener {

                abrirAplicativos()
            }


        findViewById<View>(R.id.btnLixeira)
            .setOnClickListener {

                abrirLixeira()
            }


        findViewById<View>(R.id.btnAnalise)
            .setOnClickListener {

                analisarArmazenamento()
            }


        findViewById<View>(R.id.btnBuscar)
            .setOnClickListener {

                realizarBusca()
            }


        findViewById<View>(R.id.btnVoltar)
            .setOnClickListener {

                voltarInicio()
            }


        searchEdit.setOnEditorActionListener { _, _, _ ->

            realizarBusca()

            true
        }
    }


    private fun voltarInicio() {

        fileScreen.visibility = View.GONE
        homeScroll.visibility = View.VISIBLE
    }


    private fun atualizarArmazenamento() {

        try {

            val stat =
                android.os.StatFs(rootPath.absolutePath)

            val total =
                stat.totalBytes

            val disponivel =
                stat.availableBytes

            val usado =
                total - disponivel

            val percentual =
                (
                    usado.toDouble() /
                        total.toDouble()
                    * 100
                ).toInt()

            storageInfo.text =
                "${formatarBytes(usado)} usados de ${formatarBytes(total)}"

            storageProgress.progress =
                percentual

        } catch (e: Exception) {

            storageInfo.text =
                "Não foi possível calcular o armazenamento"
        }
    }


    private fun formatarBytes(bytes: Long): String {

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

        return String.format(
            Locale.getDefault(),
            "%.1f %s",
            valor,
            unidades[indice]
        )
    }


    private fun carregarArquivos(
        pasta: File,
        titulo: String
    ) {

        homeScroll.visibility = View.GONE
        fileScreen.visibility = View.VISIBLE

        fileScreenTitle.text =
            titulo

        currentPath.text =
            pasta.absolutePath

        fileList.visibility = View.VISIBLE
        mediaGrid.visibility = View.GONE

        fileList.removeAllViews()

        if (!pasta.exists() || !pasta.isDirectory) {

            val vazio =
                TextView(this)

            vazio.text =
                "Pasta não encontrada"

            vazio.setPadding(
                30,
                30,
                30,
                30
            )

            fileList.addView(vazio)

            return
        }

        thread {

            val arquivos =
                try {

                    pasta.listFiles()
                        ?.sortedWith(
                            compareBy<File> {
                                !it.isDirectory
                            }.thenBy {
                                it.name.lowercase()
                            }
                        )
                        ?: emptyList()

                } catch (e: Exception) {

                    emptyList()
                }

            runOnUiThread {

                fileList.removeAllViews()

                if (arquivos.isEmpty()) {

                    val vazio =
                        TextView(this)

                    vazio.text =
                        "Pasta vazia"

                    vazio.setPadding(
                        30,
                        30,
                        30,
                        30
                    )

                    fileList.addView(vazio)

                    return@runOnUiThread
                }

                for (arquivo in arquivos) {

                    val item =
                        TextView(this)

                    item.text =
                        if (arquivo.isDirectory) {
                            "📁  ${arquivo.name}"
                        } else {
                            "📄  ${arquivo.name}"
                        }

                    item.textSize = 16f

                    item.setPadding(
                        25,
                        25,
                        25,
                        25
                    )

                    item.setOnClickListener {

                        if (arquivo.isDirectory) {

                            carregarArquivos(
                                arquivo,
                                arquivo.name
                            )

                        } else {

                            abrirArquivo(arquivo)
                        }
                    }

                    fileList.addView(item)
                }
            }
        }
    }


    private fun buscarMidias(
        pastas: List<File>,
        imagens: Boolean
    ) {

        homeScroll.visibility = View.GONE
        fileScreen.visibility = View.VISIBLE

        fileScreenTitle.text =
            if (imagens) {
                "Imagens"
            } else {
                "Vídeos"
            }

        currentPath.text =
            "Carregando..."

        fileList.visibility = View.GONE
        mediaGrid.visibility = View.VISIBLE

        mediaGrid.layoutManager =
            androidx.recyclerview.widget.GridLayoutManager(
                this,
                3
            )

        thread {

            val resultado =
                mutableListOf<File>()

            for (pasta in pastas) {

                buscarRecursivo(
                    pasta,
                    resultado,
                    imagens,
                    500
                )

                if (resultado.size >= 500) {
                    break
                }
            }

            runOnUiThread {

                currentPath.text =
                    "${resultado.size} arquivo(s)"

                mediaGrid.adapter =
                    MediaAdapter(
                        resultado
                    ) { arquivo ->

                        abrirArquivo(arquivo)
                    }
            }
        }
    }


    private fun buscarRecursivo(
        pasta: File,
        resultado: MutableList<File>,
        imagens: Boolean,
        limite: Int
    ) {

        if (
            resultado.size >= limite ||
            !pasta.exists() ||
            !pasta.isDirectory
        ) {
            return
        }

        val arquivos =
            try {

                pasta.listFiles()

            } catch (e: Exception) {

                null
            } ?: return

        for (arquivo in arquivos) {

            if (resultado.size >= limite) {
                break
            }

            if (arquivo.isDirectory) {

                buscarRecursivo(
                    arquivo,
                    resultado,
                    imagens,
                    limite
                )

            } else {

                val extensao =
                    arquivo.extension.lowercase(
                        Locale.getDefault()
                    )

                if (imagens) {

                    if (
                        extensao in setOf(
                            "jpg",
                            "jpeg",
                            "png",
                            "gif",
                            "webp",
                            "bmp",
                            "heic",
                            "heif"
                        )
                    ) {

                        resultado.add(arquivo)
                    }

                } else {

                    if (
                        extensao in setOf(
                            "mp4",
                            "m4v",
                            "mkv",
                            "avi",
                            "mov",
                            "3gp",
                            "webm"
                        )
                    ) {

                        resultado.add(arquivo)
                    }
                }
            }
        }
    }


    private fun buscarAudio() {

        homeScroll.visibility = View.GONE
        fileScreen.visibility = View.VISIBLE

        fileScreenTitle.text =
            "Áudios"

        currentPath.text =
            "Carregando..."

        fileList.visibility = View.VISIBLE
        mediaGrid.visibility = View.GONE

        val pastas =
            listOf(
                File(rootPath, "Music"),
                File(rootPath, "Recordings"),
                File(rootPath, "DCIM")
            )

        thread {

            val resultado =
                mutableListOf<File>()

            for (pasta in pastas) {

                buscarAudioRecursivo(
                    pasta,
                    resultado,
                    500
                )

                if (resultado.size >= 500) {
                    break
                }
            }

            runOnUiThread {

                fileList.removeAllViews()

                currentPath.text =
                    "${resultado.size} áudio(s)"

                for (arquivo in resultado) {

                    val item =
                        TextView(this)

                    item.text =
                        "🎵  ${arquivo.name}"

                    item.textSize = 16f

                    item.setPadding(
                        25,
                        25,
                        25,
                        25
                    )

                    item.setOnClickListener {

                        abrirArquivo(arquivo)
                    }

                    fileList.addView(item)
                }
            }
        }
    }


    private fun buscarAudioRecursivo(
        pasta: File,
        resultado: MutableList<File>,
        limite: Int
    ) {

        if (
            resultado.size >= limite ||
            !pasta.exists() ||
            !pasta.isDirectory
        ) {
            return
        }

        val arquivos =
            try {
                pasta.listFiles()
            } catch (e: Exception) {
                null
            } ?: return

        for (arquivo in arquivos) {

            if (resultado.size >= limite) {
                break
            }

            if (arquivo.isDirectory) {

                buscarAudioRecursivo(
                    arquivo,
                    resultado,
                    limite
                )

            } else {

                val ext =
                    arquivo.extension.lowercase(
                        Locale.getDefault()
                    )

                if (
                    ext in setOf(
                        "mp3",
                        "wav",
                        "ogg",
                        "m4a",
                        "aac",
                        "flac",
                        "opus",
                        "amr"
                    )
                ) {

                    resultado.add(arquivo)
                }
            }
        }
    }


    private fun buscarDocumentos() {

        homeScroll.visibility = View.GONE
        fileScreen.visibility = View.VISIBLE

        fileScreenTitle.text =
            "Documentos"

        currentPath.text =
            "Documentos"

        fileList.visibility = View.VISIBLE
        mediaGrid.visibility = View.GONE

        val pastas =
            listOf(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS
                ),
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
            )

        thread {

            val resultado =
                mutableListOf<File>()

            for (pasta in pastas) {

                buscarDocumentosRecursivo(
                    pasta,
                    resultado,
                    500
                )
            }

            runOnUiThread {

                fileList.removeAllViews()

                currentPath.text =
                    "${resultado.size} documento(s)"

                for (arquivo in resultado) {

                    val item =
                        TextView(this)

                    item.text =
                        "📄  ${arquivo.name}"

                    item.textSize = 16f

                    item.setPadding(
                        25,
                        25,
                        25,
                        25
                    )

                    item.setOnClickListener {

                        abrirArquivo(arquivo)
                    }

                    fileList.addView(item)
                }
            }
        }
    }


    private fun buscarDocumentosRecursivo(
        pasta: File,
        resultado: MutableList<File>,
        limite: Int
    ) {

        if (
            resultado.size >= limite ||
            !pasta.exists() ||
            !pasta.isDirectory
        ) {
            return
        }

        val arquivos =
            try {
                pasta.listFiles()
            } catch (e: Exception) {
                null
            } ?: return

        for (arquivo in arquivos) {

            if (resultado.size >= limite) {
                break
            }

            if (arquivo.isDirectory) {

                buscarDocumentosRecursivo(
                    arquivo,
                    resultado,
                    limite
                )

            } else {

                val ext =
                    arquivo.extension.lowercase(
                        Locale.getDefault()
                    )

                if (
                    ext in setOf(
                        "pdf",
                        "txt",
                        "csv",
                        "html",
                        "htm",
                        "doc",
                        "docx",
                        "xls",
                        "xlsx",
                        "ppt",
                        "pptx",
                        "rtf"
                    )
                ) {

                    resultado.add(arquivo)
                }
            }
        }
    }


    private fun realizarBusca() {

        val termo =
            searchEdit.text.toString().trim()

        if (termo.isEmpty()) {

            Toast.makeText(
                this,
                "Digite algo para pesquisar",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        homeScroll.visibility = View.GONE
        fileScreen.visibility = View.VISIBLE

        fileScreenTitle.text =
            "Busca"

        currentPath.text =
            "Pesquisando..."

        fileList.visibility = View.VISIBLE
        mediaGrid.visibility = View.GONE

        thread {

            val resultado =
                mutableListOf<File>()

            buscarPorNome(
                rootPath,
                termo.lowercase(
                    Locale.getDefault()
                ),
                resultado,
                300
            )

            runOnUiThread {

                fileList.removeAllViews()

                currentPath.text =
                    "${resultado.size} resultado(s)"

                for (arquivo in resultado) {

                    val item =
                        TextView(this)

                    item.text =
                        if (arquivo.isDirectory) {
                            "📁  ${arquivo.name}"
                        } else {
                            "📄  ${arquivo.name}"
                        }

                    item.textSize = 16f

                    item.setPadding(
                        25,
                        25,
                        25,
                        25
                    )

                    item.setOnClickListener {

                        if (arquivo.isDirectory) {

                            carregarArquivos(
                                arquivo,
                                arquivo.name
                            )

                        } else {

                            abrirArquivo(arquivo)
                        }
                    }

                    fileList.addView(item)
                }
            }
        }
    }


    private fun buscarPorNome(
        pasta: File,
        termo: String,
        resultado: MutableList<File>,
        limite: Int
    ) {

        if (
            resultado.size >= limite ||
            !pasta.exists() ||
            !pasta.isDirectory
        ) {
            return
        }

        val arquivos =
            try {
                pasta.listFiles()
            } catch (e: Exception) {
                null
            } ?: return

        for (arquivo in arquivos) {

            if (resultado.size >= limite) {
                break
            }

            if (
                arquivo.name.lowercase(
                    Locale.getDefault()
                ).contains(termo)
            ) {

                resultado.add(arquivo)
            }

            if (arquivo.isDirectory) {

                buscarPorNome(
                    arquivo,
                    termo,
                    resultado,
                    limite
                )
            }
        }
    }


    private fun abrirAplicativos() {

        homeScroll.visibility = View.GONE
        fileScreen.visibility = View.VISIBLE

        fileScreenTitle.text =
            "Aplicativos"

        currentPath.text =
            "Aplicativos instalados"

        fileList.visibility = View.VISIBLE
        mediaGrid.visibility = View.GONE

        fileList.removeAllViews()

        val apps =
            packageManager
                .getInstalledApplications(0)
                .sortedBy {

                    packageManager
                        .getApplicationLabel(it)
                        .toString()
                        .lowercase(
                            Locale.getDefault()
                        )
                }

        for (app in apps) {

            val nome =
                packageManager
                    .getApplicationLabel(app)
                    .toString()

            val item =
                TextView(this)

            item.text =
                "📱  $nome"

            item.textSize = 16f

            item.setPadding(
                25,
                25,
                25,
                25
            )

            item.setOnClickListener {

                val intent =
                    packageManager
                        .getLaunchIntentForPackage(
                            app.packageName
                        )

                if (intent != null) {

                    startActivity(intent)

                } else {

                    Toast.makeText(
                        this,
                        "Não foi possível abrir o aplicativo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            fileList.addView(item)
        }
    }


    private fun abrirLixeira() {

        val lixeira =
            File(
                rootPath,
                ".GerenciadorArquivos/.Lixeira"
            )

        carregarArquivos(
            lixeira,
            "Lixeira"
        )
    }


    private fun analisarArmazenamento() {

        Toast.makeText(
            this,
            "Análise do armazenamento em desenvolvimento",
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

            if (!arquivo.exists()) {

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
                        ClipData.newRawUri(
                            "arquivo",
                            uri
                        )
                }

            try {

                startActivity(intent)

            } catch (
                e: ActivityNotFoundException
            ) {

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
                            ClipData.newRawUri(
                                "arquivo",
                                uri
                            )
                    }

                try {

                    startActivity(
                        intentGenerico
                    )

                } catch (
                    e2: ActivityNotFoundException
                ) {

                    Toast.makeText(
                        this,
                        "Nenhum aplicativo instalado consegue abrir este arquivo",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Erro ao abrir arquivo: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // =========================================================
    // MIME TYPE
    // =========================================================

    private fun obterTipoMime(
        arquivo: File
    ): String {

        return when (
            arquivo.extension.lowercase(
                Locale.getDefault()
            )
        ) {

            // IMAGENS

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


            // VÍDEOS

            "mp4" ->
                "video/mp4"

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


            // ÁUDIO

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


            // DOCUMENTOS

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


            // COMPACTADOS

            "zip" ->
                "application/zip"

            "rar" ->
                "application/vnd.rar"

            "7z" ->
                "application/x-7z-compressed"


            // APK

            "apk" ->
                "application/vnd.android.package-archive"


            // DESCONHECIDO

            else ->
                "*/*"
        }
    }


    // =========================================================
    // MINIATURAS
    // =========================================================

    private fun carregarMiniatura(
        arquivo: File
    ): Bitmap? {

        try {

            if (!arquivo.exists()) {
                return null
            }

            val options =
                BitmapFactory.Options()

            options.inJustDecodeBounds =
                true

            BitmapFactory.decodeFile(
                arquivo.absolutePath,
                options
            )

            val largura =
                options.outWidth

            val altura =
                options.outHeight

            if (
                largura <= 0 ||
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

            return BitmapFactory.decodeFile(
                arquivo.absolutePath,
                optionsFinal
            )

        } catch (
            e: OutOfMemoryError
        ) {

            return null

        } catch (
            e: Exception
        ) {

            return null
        }
    }


    // =========================================================
    // ADAPTER DAS MINIATURAS
    // =========================================================

    private inner class MediaAdapter(
        private val arquivos: List<File>,
        private val aoClicar: (File) -> Unit
    ) :
        androidx.recyclerview.widget.RecyclerView.Adapter<MediaAdapter.ViewHolder>() {


        inner class ViewHolder(
            view: View
        ) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(
                view
            ) {

            val thumbnail =
                view.findViewById<ImageView>(
                    R.id.mediaThumbnail
                )

            val nome =
                view.findViewById<TextView>(
                    R.id.mediaName
                )
        }


        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {

            val view =
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.item_media,
                        parent,
                        false
                    )

            return ViewHolder(view)
        }


        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int
        ) {

            val arquivo =
                arquivos[position]

            holder.nome.text =
                arquivo.name

            holder.thumbnail.tag =
                arquivo.absolutePath

            holder.thumbnail.setImageResource(
                android.R.drawable.ic_menu_gallery
            )

            holder.itemView.setOnClickListener {

                aoClicar(arquivo)
            }

            thread {

                val bitmap =
                    carregarMiniatura(
                        arquivo
                    )

                holder.thumbnail.post {

                    if (
                        holder.thumbnail.tag ==
                        arquivo.absolutePath
                    ) {

                        if (bitmap != null) {

                            holder.thumbnail.setImageBitmap(
                                bitmap
                            )
                        }
                    }
                }
            }
        }


        override fun getItemCount(): Int {

            return arquivos.size
        }
    }
}
