package com.gerenciadordearquivos.app

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class ZipViewerActivity : AppCompatActivity() {

    private lateinit var lista: ListView

    private var arquivoZip: File? = null

    private val entradas = ArrayList<ZipEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val caminho = intent.getStringExtra("arquivo")

        if (caminho.isNullOrEmpty()) {
            Toast.makeText(
                this,
                "ZIP inválido",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        arquivoZip = File(caminho)

        if (!arquivoZip!!.exists() || !arquivoZip!!.isFile) {
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

    // =========================================================
    // INTERFACE
    // =========================================================

    private fun criarInterface() {

        val raiz = LinearLayout(this)

        raiz.orientation =
            LinearLayout.VERTICAL

        raiz.setBackgroundColor(
            Color.WHITE
        )

        // =====================================================
        // BARRA SUPERIOR
        // =====================================================

        val barra = LinearLayout(this)

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
            Color.rgb(
                21,
                101,
                192
            )
        )

        // =====================================================
        // VOLTAR
        // =====================================================

        val voltar = Button(this)

        voltar.text = "‹"

        voltar.textSize = 28f

        voltar.setTextColor(
            Color.WHITE
        )

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

        // =====================================================
        // TÍTULO
        // =====================================================

        val titulo = TextView(this)

        titulo.text =
            arquivoZip?.name ?: "ZIP"

        titulo.setTextColor(
            Color.WHITE
        )

        titulo.textSize = 16f

        titulo.gravity =
            Gravity.CENTER_VERTICAL

        titulo.maxLines = 1

        titulo.ellipsize =
            android.text.TextUtils.TruncateAt.END

        val tituloParams =
            LinearLayout.LayoutParams(
                0,
                55,
                1f
            )

        tituloParams.leftMargin = 8

        barra.addView(
            titulo,
            tituloParams
        )

        // =====================================================
        // BOTÃO EXTRAIR
        // =====================================================

        val extrair = Button(this)

        extrair.text = "Extrair"

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

        // =====================================================
        // LISTA DO ZIP
        // =====================================================

        lista = ListView(this)

        raiz.addView(
            lista,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(
            raiz
        )
    }

    // =========================================================
    // LER ZIP
    // =========================================================

    private fun carregarZip() {

        val zipAtual = arquivoZip

        if (zipAtual == null || !zipAtual.exists()) {
            return
        }

        Thread {

            try {

                entradas.clear()

                ZipFile(
                    zipAtual
                ).use { zip ->

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
                }

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

    // =========================================================
    // EXTRAIR ZIP
    // =========================================================

    private fun extrairZip() {

        val zipFile =
            arquivoZip

        if (
            zipFile == null ||
            !zipFile.exists() ||
            !zipFile.isFile
        ) {

            Toast.makeText(
                this,
                "ZIP não encontrado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val pastaPai =
            zipFile.parentFile

        if (pastaPai == null) {

            Toast.makeText(
                this,
                "Não foi possível determinar a pasta de destino",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val destino =
            File(
                pastaPai,
                zipFile.nameWithoutExtension
            )

        if (
            !destino.exists() &&
            !destino.mkdirs()
        ) {

            Toast.makeText(
                this,
                "Não foi possível criar a pasta de destino",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        Toast.makeText(
            this,
            "Extraindo...",
            Toast.LENGTH_SHORT
        ).show()

        Thread {

            try {

                ZipFile(
                    zipFile
                ).use { zip ->

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

                        // =================================================
                        // PROTEÇÃO CONTRA ZIP SLIP
                        // =================================================

                        val caminhoDestino =
                            arquivoDestino.canonicalPath

                        val caminhoBase =
                            destino.canonicalPath +
                                    File.separator

                        if (
                            !caminhoDestino.startsWith(
                                caminhoBase
                            )
                        ) {

                            continue
                        }

                        // =================================================
                        // PASTA
                        // =================================================

                        if (entrada.isDirectory) {

                            arquivoDestino.mkdirs()

                        } else {

                            arquivoDestino.parentFile
                                ?.mkdirs()

                            // =================================================
                            // ARQUIVO
                            // =================================================

                            zip.getInputStream(
                                entrada
                            ).use { entradaStream ->

                                FileOutputStream(
                                    arquivoDestino
                                ).use { saida ->

                                    while (true) {

                                        val quantidade =
                                            entradaStream.read(
                                                buffer
                                            )

                                        if (
                                            quantidade == -1
                                        ) {
                                            break
                                        }

                                        if (
                                            quantidade > 0
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
                    }
                }

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
                        "Erro ao extrair ZIP: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        }.start()
    }
}
