package com.gerenciadordearquivos.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var homeScroll: ScrollView
    private lateinit var fileScreen: LinearLayout
    private lateinit var fileScreenTitle: TextView
    private lateinit var currentPath: TextView
    private lateinit var fileList: ListView

    private lateinit var storageInfo: TextView
    private lateinit var storageProgress: ProgressBar
    private lateinit var searchEdit: EditText

    private var currentDirectory: File? = null

    private val rootPath: File
        get() = Environment.getExternalStorageDirectory()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        inicializarViews()
        configurarBotoes()
        atualizarArmazenamento()

        if (!temPermissao()) {
            pedirPermissao()
        }
    }


    private fun inicializarViews() {

        homeScroll = findViewById(R.id.homeScroll)
        fileScreen = findViewById(R.id.fileScreen)

        fileScreenTitle = findViewById(R.id.fileScreenTitle)
        currentPath = findViewById(R.id.currentPath)
        fileList = findViewById(R.id.fileList)

        storageInfo = findViewById(R.id.storageInfo)
        storageProgress = findViewById(R.id.storageProgress)

        searchEdit = findViewById(R.id.searchEdit)
    }


    private fun configurarBotoes() {

        // ARMAZENAMENTO PRINCIPAL
        findViewById<View>(R.id.categoryStorage).setOnClickListener {
            abrirPasta(rootPath, "Armazenamento principal")
        }


        // DOWNLOADS
        findViewById<View>(R.id.categoryDownloads).setOnClickListener {

            val pasta = Environment
                .getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )

            abrirPasta(pasta, "Downloads")
        }


        // IMAGENS
        findViewById<View>(R.id.categoryImages).setOnClickListener {

            mostrarCategoriasDePasta(
                "Imagens",
                listOf(
                    File(rootPath, "DCIM"),
                    File(rootPath, "Pictures")
                )
            )
        }


        // VÍDEOS
        findViewById<View>(R.id.categoryVideos).setOnClickListener {

            mostrarCategoriasDePasta(
                "Vídeos",
                listOf(
                    File(rootPath, "DCIM"),
                    File(rootPath, "Movies")
                )
            )
        }


        // ÁUDIO
        findViewById<View>(R.id.categoryAudio).setOnClickListener {

            val pasta = File(rootPath, "Music")

            abrirPasta(pasta, "Áudio")
        }


        // DOCUMENTOS
        findViewById<View>(R.id.categoryDocuments).setOnClickListener {

            val pasta = Environment
                .getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS
                )

            abrirPasta(pasta, "Documentos")
        }


        // APLICATIVOS
        findViewById<View>(R.id.categoryApps).setOnClickListener {
            abrirAplicativos()
        }


        // LIXEIRA
        findViewById<View>(R.id.categoryTrash).setOnClickListener {
            abrirLixeira()
        }


        // ANÁLISE
        findViewById<View>(R.id.categoryAnalysis).setOnClickListener {
            analisarArmazenamento()
        }


        // PESQUISA
        searchEdit.setOnEditorActionListener { _, _, _ ->

            val texto = searchEdit.text.toString().trim()

            if (texto.isNotEmpty()) {
                pesquisarArquivos(texto)
            }

            true
        }
    }


    // =========================================================
    // ARMAZENAMENTO
    // =========================================================

    private fun atualizarArmazenamento() {

        thread {

            val stat = android.os.StatFs(rootPath.path)

            val total =
                stat.totalBytes

            val disponivel =
                stat.availableBytes

            val usado =
                total - disponivel

            val percentual =
                ((usado.toDouble() / total.toDouble()) * 100)
                    .toInt()

            val totalGB =
                total / 1024.0 / 1024.0 / 1024.0

            val usadoGB =
                usado / 1024.0 / 1024.0 / 1024.0

            val disponivelGB =
                disponivel / 1024.0 / 1024.0 / 1024.0

            runOnUiThread {

                storageInfo.text =
                    String.format(
                        Locale.getDefault(),
                        "%.1f GB / %.1f GB",
                        usadoGB,
                        totalGB
                    )

                storageProgress.progress =
                    percentual

                storageInfo.append(
                    "  •  ${String.format(
                        Locale.getDefault(),
                        "%.1f GB",
                        disponivelGB
                    )} livres"
                )
            }
        }
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

        mostrarTelaArquivos(titulo)

        currentDirectory = pasta

        carregarArquivos(pasta)
    }


    private fun mostrarTelaArquivos(titulo: String) {

        homeScroll.visibility = View.GONE
        fileScreen.visibility = View.VISIBLE

        fileScreenTitle.text = titulo
    }


    // =========================================================
    // LISTAR ARQUIVOS
    // =========================================================

    private fun carregarArquivos(pasta: File) {

        currentDirectory = pasta

        currentPath.text = pasta.absolutePath

        val arquivos =
            pasta.listFiles()
                ?.sortedWith(
                    compareBy<File> {
                        !it.isDirectory
                    }.thenBy {
                        it.name.lowercase(Locale.getDefault())
                    }
                )
                ?: emptyList()

        val nomes = arquivos.map {

            if (it.isDirectory) {
                "📁  ${it.name}"
            } else {
                "📄  ${it.name}"
            }
        }

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                nomes
            )

        fileList.adapter = adapter


        fileList.setOnItemClickListener {
                _,
                _,
                position,
                _ ->

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


    // =========================================================
    // CATEGORIAS DE PASTAS
    // =========================================================

    private fun mostrarCategoriasDePasta(
        titulo: String,
        pastas: List<File>
    ) {

        mostrarTelaArquivos(titulo)

        currentPath.text =
            "Pastas disponíveis"

        val existentes =
            pastas.filter {
                it.exists()
            }

        val nomes =
            existentes.map {
                "📁  ${it.name}"
            }

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                nomes
            )

        fileList.adapter = adapter

        fileList.setOnItemClickListener {
                _,
                _,
                position,
                _ ->

            abrirPasta(
                existentes[position],
                existentes[position].name
            )
        }
    }


    // =========================================================
    // APLICATIVOS
    // =========================================================

    private fun abrirAplicativos() {

        val packageManager = packageManager

        val aplicativos =
            packageManager
                .getInstalledApplications(0)
                .filter {

                    packageManager
                        .getLaunchIntentForPackage(
                            it.packageName
                        ) != null
                }
                .sortedBy {

                    packageManager
                        .getApplicationLabel(it)
                        .toString()
                        .lowercase(Locale.getDefault())
                }

        mostrarTelaArquivos("Aplicativos")

        currentPath.text =
            "${aplicativos.size} aplicativos instalados"


        val adapter =
            object : BaseAdapter() {

                override fun getCount(): Int {
                    return aplicativos.size
                }

                override fun getItem(
                    position: Int
                ): Any {
                    return aplicativos[position]
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
                        LinearLayout(this@MainActivity)

                    layout.orientation =
                        LinearLayout.HORIZONTAL

                    layout.gravity =
                        android.view.Gravity.CENTER_VERTICAL

                    layout.setPadding(
                        20,
                        10,
                        20,
                        10
                    )

                    val icon =
                        ImageView(this@MainActivity)

                    icon.layoutParams =
                        LinearLayout.LayoutParams(
                            55,
                            55
                        )

                    val nome =
                        TextView(this@MainActivity)

                    nome.text =
                        packageManager
                            .getApplicationLabel(
                                aplicativos[position]
                            )

                    nome.textSize = 16f
                    nome.setTextColor(
                        android.graphics.Color.DKGRAY
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

                    icon.setImageDrawable(
                        aplicativos[position]
                            .loadIcon(packageManager)
                    )

                    layout.addView(icon)
                    layout.addView(nome)

                    return layout
                }
            }


        fileList.adapter = adapter


        fileList.setOnItemClickListener {
                _,
                _,
                position,
                _ ->

            val aplicativo =
                aplicativos[position]

            val intent =
                packageManager
                    .getLaunchIntentForPackage(
                        aplicativo.packageName
                    )

            if (intent != null) {
                startActivity(intent)
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

        currentPath.text =
            "Procurando por \"$termo\"..."

        fileList.adapter =
            ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                listOf("Pesquisando...")
            )


        thread {

            val encontrados =
                mutableListOf<File>()

            procurarRecursivamente(
                rootPath,
                termo.lowercase(Locale.getDefault()),
                encontrados
            )


            runOnUiThread {

                currentPath.text =
                    "${encontrados.size} resultado(s)"


                val nomes =
                    encontrados.map {

                        if (it.isDirectory) {
                            "📁  ${it.name}"
                        } else {
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

                    val arquivo =
                        encontrados[position]

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
            pasta.listFiles()
                ?: return


        for (arquivo in arquivos) {

            if (
                arquivo.name
                    .lowercase(Locale.getDefault())
                    .contains(termo)
            ) {

                resultado.add(arquivo)
            }


            if (
                arquivo.isDirectory &&
                !arquivo.name.startsWith(".")
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

        Toast.makeText(
            this,
            "Análise do armazenamento",
            Toast.LENGTH_SHORT
        ).show()

        atualizarArmazenamento()
    }


    // =========================================================
    // ABRIR ARQUIVO
    // =========================================================

    private fun abrirArquivo(
        arquivo: File
    ) {

        try {

            val intent =
                Intent(
                    Intent.ACTION_VIEW
                )

            val uri =
                Uri.fromFile(arquivo)

            intent.setDataAndType(
                uri,
                "*/*"
            )

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Não foi possível abrir este arquivo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // VOLTAR
    // =========================================================

    override fun onBackPressed() {

        if (fileScreen.visibility == View.VISIBLE) {

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

                startActivity(intent)

            } catch (e: Exception) {

                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    )
                )
            }
        }
    }
}
