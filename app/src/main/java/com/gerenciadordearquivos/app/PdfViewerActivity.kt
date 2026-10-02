package com.gerenciadordearquivos.app

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class PdfViewerActivity : AppCompatActivity() {

    private var renderer: PdfRenderer? = null
    private var descriptor: ParcelFileDescriptor? = null

    private lateinit var imageView: ImageView
    private lateinit var pageInfo: TextView

    private var paginaAtual = 0
    private var totalPaginas = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val caminho = intent.getStringExtra("arquivo")

        if (caminho.isNullOrEmpty()) {
            Toast.makeText(
                this,
                "Arquivo PDF inválido",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        val arquivo = File(caminho)

        if (!arquivo.exists()) {
            Toast.makeText(
                this,
                "PDF não encontrado",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        criarInterface()

        try {

            descriptor = ParcelFileDescriptor.open(
                arquivo,
                ParcelFileDescriptor.MODE_READ_ONLY
            )

            renderer = PdfRenderer(descriptor!!)

            totalPaginas = renderer!!.pageCount

            if (totalPaginas <= 0) {
                Toast.makeText(
                    this,
                    "PDF sem páginas",
                    Toast.LENGTH_LONG
                ).show()

                finish()
                return
            }

            mostrarPagina(0)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Erro ao abrir PDF: ${e.message}",
                Toast.LENGTH_LONG
            ).show()

            finish()
        }
    }

    private fun criarInterface() {

        val raiz = LinearLayout(this)

        raiz.orientation = LinearLayout.VERTICAL

        raiz.setBackgroundColor(
            Color.rgb(30, 30, 30)
        )

        // ==========================
        // BARRA SUPERIOR
        // ==========================

        val barra = LinearLayout(this)

        barra.orientation = LinearLayout.HORIZONTAL

        barra.gravity = Gravity.CENTER_VERTICAL

        barra.setPadding(
            12,
            8,
            12,
            8
        )

        barra.setBackgroundColor(
            Color.rgb(21, 101, 192)
        )

        // VOLTAR

        val voltar = Button(this)

        voltar.text = "‹"

        voltar.textSize = 28f

        voltar.setTextColor(Color.WHITE)

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

        // INFORMAÇÃO DA PÁGINA

        pageInfo = TextView(this)

        // CORRIGIDO
        pageInfo.setTextColor(Color.WHITE)

        pageInfo.textSize = 16f

        pageInfo.gravity = Gravity.CENTER

        barra.addView(
            pageInfo,
            LinearLayout.LayoutParams(
                0,
                55,
                1f
            )
        )

        raiz.addView(
            barra,
            LinearLayout.LayoutParams(
                -1,
                65
            )
        )

        // ==========================
        // PDF
        // ==========================

        imageView = ImageView(this)

        imageView.scaleType =
            ImageView.ScaleType.FIT_CENTER

        imageView.setBackgroundColor(
            Color.rgb(50, 50, 50)
        )

        val scroll = ScrollView(this)

        // CORRIGIDO:
        // ScrollView.LayoutParams é válido,
        // mas usamos FrameLayout.LayoutParams
        // de forma explícita para evitar ambiguidade.
        scroll.addView(
            imageView,
            android.widget.FrameLayout.LayoutParams(
                -1,
                -2
            )
        )

        raiz.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        // ==========================
        // CONTROLES
        // ==========================

        val controles = LinearLayout(this)

        controles.orientation =
            LinearLayout.HORIZONTAL

        controles.gravity =
            Gravity.CENTER

        controles.setPadding(
            8,
            8,
            8,
            8
        )

        controles.setBackgroundColor(
            Color.rgb(21, 101, 192)
        )

        // ANTERIOR

        val anterior = Button(this)

        anterior.text = "‹ Anterior"

        anterior.setOnClickListener {

            if (paginaAtual > 0) {

                mostrarPagina(
                    paginaAtual - 1
                )
            }
        }

        // PRÓXIMA

        val proxima = Button(this)

        proxima.text = "Próxima ›"

        proxima.setOnClickListener {

            if (
                paginaAtual <
                totalPaginas - 1
            ) {

                mostrarPagina(
                    paginaAtual + 1
                )
            }
        }

        controles.addView(
            anterior,
            LinearLayout.LayoutParams(
