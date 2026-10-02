package com.gerenciadordearquivos.app

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class ZipViewerActivity : AppCompatActivity() {

    private lateinit var lista: ListView

    private var arquivoZip: File? = null

    private val entradas =
        ArrayList<ZipEntry>()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        val caminho =
            intent.getStringExtra("arquivo")

        if (caminho.isNullOrEmpty()) {
            finish()
            return
        }

        arquivoZip =
            File(caminho)

        if (
            arquivoZip == null ||
            !arquivoZip!!.exists()
        ) {

            Toast.makeText(
                this,
                "ZIP não encontrado",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        criarInterface()

        carregarZip()
    }

    private fun criarInterface() {

        val raiz =
            LinearLayout(this)

        raiz.orientation =
            LinearLayout.VERTICAL

        raiz.setBackgroundColor(
            Color.WHITE
        )

        // BARRA

        val barra =
            LinearLayout(this)

        barra.orientation =
            LinearLayout.HORIZONTAL

        barra.gravity =
            Gravity.CENTER_VERTICAL

        barra.setPadding(
            8,
            5,
            8,
            5
        )

        barra.setBackgroundColor(
            Color.rgb(21, 101, 192)
        )

        val voltar =
            Button(this)

        voltar.text = "‹"

        voltar.textSize = 28f

        voltar.setOnClickListener {
            finish()
        }

        barra.addView(
            voltar,
            LinearLayout.LayoutParams(
                55,
                55
            )
        )

        val titulo =
            TextView(this)

        titulo.text =
            arquivoZip?.name ?: "ZIP"

        titulo.textColor =
            Color.WHITE

        titulo.textSize = 16f

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        titulo.maxLines = 1

        titulo.ellipsize =
            android.text.TextUtils.TruncateAt.END

        barra.addView(
            titulo,
            LinearLayout.LayoutParams(
                0,
                55,
                1f
            )
        )

        val extrair =
            Button(this)

        extrair.text =
            "Extrair"

        extrair.setOnClickListener {
            extrairZip()
        }

        barra.addView(
            extrair,
            LinearLayout.LayoutParams(
                100,
                55
            )
        )

        raiz.addView(
            barra,
            LinearLayout.LayoutParams(
                -1,
                65
            )
        )

        // LISTA

        lista =
            ListView(this)

        raiz.addView(
            lista,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(raiz)
    }

    private fun carregarZip() {

        Thread {

            try {

                val zip =
                    ZipFile(
                        arquivoZip!!
                    )

                val enumeracao =
                    zip.entries()

                while (
                    enumeracao.hasMoreElements()
                ) {

                    val entrada =
                        enumeracao.nextElement()

                    entradas.add(
                        entrada
                    )
                }

                zip.close()

                runOnUiThread {

                    val nomes =
                        entradas.map { entrada ->

                            if (entrada.isDirectory) {
                                "📁  ${entrada.name}"
                            } else {
                                "📄  ${entrada.name}"
                            }
                        }

                    lista.adapter =
                        ArrayAdapter(
                            this,
                            android.R.layout.simple_list_item_1,
                            nomes
                        )

                    if (entradas.isEmpty()) {

                        Toast.makeText(
                            this,
                            "ZIP vazio",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "Erro ao ler ZIP: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.start()
    }

    private fun extrairZip() {

        val zipFile =
            arquivoZip

        if (
            zipFile == null ||
            !zipFile.exists()
        ) {
            return
        }

        val destino =
            File(
                zipFile.parentFile,
                zipFile.nameWithoutExtension
            )

        if (!destino.exists()) {
            destino.mkdirs()
        }

        Toast.makeText(
            this,
            "Extraindo...",
            Toast.LENGTH_SHORT
        ).show()

        Thread {

            try {

                val zip =
                    ZipFile(zipFile)

                val buffer =
                    ByteArray(8192)

                val enumeracao =
                    zip.entries()

                while (
                    enumeracao.hasMoreElements()
                ) {

                    val entrada =
                        enumeracao.nextElement()

                    val arquivoDestino =
                        File(
                            destino,
                            entrada.name
                        )

                    // Proteção contra ZIP Slip
                    val caminhoDestino =
                        arquivoDestino
                            .canonicalPath

                    val caminhoBase =
                        destino
                            .canonicalPath +
                                File.separator

                    if (
                        !caminhoDestino
                            .startsWith(
                                caminhoBase
                            )
                    ) {
                        continue
                    }

                    if (
                        entrada.isDirectory
                    ) {

                        arquivoDestino.mkdirs()

                    } else {

                        arquivoDestino.parentFile
                            ?.mkdirs()

                        zip.getInputStream(
                            entrada
                        ).use { entradaStream ->

                            FileOutputStream(
                                arquivoDestino
                            ).use { saida ->

                                var quantidade: Int

                                while (
                                    entradaStream
                                        .read(buffer)
                                        .also {
                                            quantidade = it
                                        } != -1
                                ) {

                                    saida.write(
                                        buffer,
                                        0,
                                        quantidade
                                    )
                                }
                            }
                        }
                    }
                }

                zip.close()

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "Extraído em:\n${destino.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                runOnUiThread {

                    Toast.makeText(
                        this,
                        "Erro ao extrair: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.start()
    }
}
