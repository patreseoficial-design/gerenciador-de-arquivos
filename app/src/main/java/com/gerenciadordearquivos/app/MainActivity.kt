package com.gerenciadordearquivos.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var fileList: ListView
    private lateinit var storageInfo: TextView
    private lateinit var storageProgress: ProgressBar

    private var currentDirectory: File =
        Environment.getExternalStorageDirectory()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        fileList = findViewById(R.id.fileList)
        storageInfo = findViewById(R.id.storageInfo)
        storageProgress = findViewById(R.id.storageProgress)

        solicitarPermissao()

        atualizarArmazenamento()
        mostrarArquivos()

        configurarCategorias()
    }

    private fun solicitarPermissao() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {

            if (!Environment.isExternalStorageManager()) {

                try {

                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName")
                    )

                    startActivity(intent)

                } catch (e: Exception) {

                    e.printStackTrace()

                }
            }
        }
    }

    private fun atualizarArmazenamento() {

        val stat = android.os.StatFs(
            Environment.getExternalStorageDirectory().path
        )

        val total = stat.totalBytes
        val livre = stat.availableBytes
        val usado = total - livre

        val percentual =
            ((usado.toDouble() / total.toDouble()) * 100).toInt()

        storageProgress.progress = percentual

        storageInfo.text =
            "${formatarTamanho(usado)} usados de ${formatarTamanho(total)}"
    }

    private fun formatarTamanho(bytes: Long): String {

        if (bytes <= 0) return "0 B"

        val unidades = arrayOf(
            "B",
            "KB",
            "MB",
            "GB",
            "TB"
        )

        var valor = bytes.toDouble()
        var indice = 0

        while (valor >= 1024 && indice < unidades.size - 1) {

            valor /= 1024
            indice++

        }

        return String.format(
            java.util.Locale.getDefault(),
            "%.1f %s",
            valor,
            unidades[indice]
        )
    }

    private fun mostrarArquivos() {

        val arquivos = currentDirectory.listFiles()
            ?.sortedWith(
                compareBy<File> { !it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
            ?: emptyList()

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

        fileList.setOnItemClickListener { _, _, position, _ ->

            if (position >= arquivos.size) return@setOnItemClickListener

            val arquivo = arquivos[position]

            if (arquivo.isDirectory) {

                currentDirectory = arquivo
                mostrarArquivos()

            } else {

                abrirArquivo(arquivo)
            }
        }
    }

    private fun abrirArquivo(arquivo: File) {

        try {

            val uri = Uri.fromFile(arquivo)

            val intent = Intent(Intent.ACTION_VIEW)

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

    private fun configurarCategorias() {

        val imagens =
            findViewById<LinearLayout>(R.id.categoryImages)

        val videos =
            findViewById<LinearLayout>(R.id.categoryVideos)

        val audio =
            findViewById<LinearLayout>(R.id.categoryAudio)

        val documentos =
            findViewById<LinearLayout>(R.id.categoryDocuments)

        val downloads =
            findViewById<LinearLayout>(R.id.categoryDownloads)

        val aplicativos =
            findViewById<LinearLayout>(R.id.categoryApk)

        imagens.setOnClickListener {
            abrirCategoria("Imagens")
        }

        videos.setOnClickListener {
            abrirCategoria("Vídeos")
        }

        audio.setOnClickListener {
            abrirCategoria("Áudios")
        }

        documentos.setOnClickListener {
            abrirCategoria("Documentos")
        }

        downloads.setOnClickListener {

            val pasta = Environment
                .getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )

            abrirPasta(pasta)
        }

        aplicativos.setOnClickListener {
            Toast.makeText(
                this,
                "Categoria Aplicativos em desenvolvimento",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun abrirCategoria(nome: String) {

        Toast.makeText(
            this,
            "$nome em desenvolvimento",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun abrirPasta(pasta: File) {

        if (pasta.exists() && pasta.isDirectory) {

            currentDirectory = pasta
            mostrarArquivos()

        } else {

            Toast.makeText(
                this,
                "Pasta não encontrada",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onBackPressed() {

        val raiz =
            Environment.getExternalStorageDirectory()

        if (currentDirectory.absolutePath != raiz.absolutePath) {

            val pai = currentDirectory.parentFile

            if (pai != null) {

                currentDirectory = pai
                mostrarArquivos()

            } else {

                super.onBackPressed()
            }

        } else {

            super.onBackPressed()
        }
    }
}
