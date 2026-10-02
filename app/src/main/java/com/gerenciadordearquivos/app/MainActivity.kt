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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        inicializarViews()
        configurarBotoes()
        configurarPesquisa()
        atualizarArmazenamento()
        verificarPermissao()
    }

    private fun calcularTamanhoCache(): Int {
        val memoria =
            Runtime.getRuntime().maxMemory() / 1024

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

                    val texto =
                        s?.toString()?.trim() ?: ""

                    if (texto.length >= 2) {
                        pesquisarArquivos(texto)
                    }
                }

                override fun afterTextChanged(
                    s: Editable?
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

        currentDirectory = pasta

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

        carregarArquivos(pasta)
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
                        ) ?: emptyList()

                } catch (
                    _: Exception
                ) {
                    emptyList()
                }

            runOnUiThread {

                val adapter =
                    FileListAdapter(arquivos)

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

            val resultado =
                ArrayList<File>()

            fun procurar(
                pasta: File
            ) {

                if (resultado.size >= 500) {
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

                    if (resultado.size >= 500) {
                        return@forEach
                    }

                    if (arquivo.isDirectory) {

                        procurar(arquivo)

                    } else {

                        val extensao =
                            arquivo.extension.lowercase(
                                Locale.getDefault()
                            )

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

                if (resultado.size >= 500) {
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

                    if (resultado.size >= 500) {
                        return@forEach
                    }

                    if (arquivo.isDirectory) {

                        procurar(arquivo)

                    } else {

                        val extensao =
                            arquivo.extension.lowercase(
                                Locale.getDefault()
                            )

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
                it.name.lowercase(
                    Locale.getDefault()
                )
            }

            runOnUiThread {

                currentPath.text =
                    "${resultado.size} arquivo(s)"

                fileList.adapter =
                    FileListAdapter(resultado)

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
                            aplicativos[position].packageName
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

                if (resultado.size >= 300) {
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

                    if (resultado.size >= 300) {
                        return@forEach
                    }

                    if (
                        arquivo.name.contains(
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

                fileList.adapter =
                    FileListAdapter(resultado)

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

    private fun abrirArquivo(
        arquivo: File
    ) {

        if (!arquivo.exists()) {

            Toast.makeText
