package com.gerenciadordearquivos.app

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class PdfViewerActivity : AppCompatActivity() {

    // Mesmo tamanho de letra do resto do app
    override fun attachBaseContext(
        novoContexto: android.content.Context
    ) {
        super.attachBaseContext(
            ModoSimples.contexto(novoContexto)
        )
    }

    private var renderer: PdfRenderer? = null
    private var descriptor: ParcelFileDescriptor? = null

    private lateinit var imageView: ImageView
    private lateinit var pageInfo: TextView

    private var paginaAtual = 0
    private var totalPaginas = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val caminho =
            intent.getStringExtra("arquivo")

        if (caminho.isNullOrEmpty()) {

            Toast.makeText(
                this,
                tr("Arquivo PDF inválido"),
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        val arquivo =
            File(caminho)

        if (!arquivo.exists() || !arquivo.isFile) {

            Toast.makeText(
                this,
                tr("PDF não encontrado"),
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        criarInterface()

        try {

            descriptor =
                ParcelFileDescriptor.open(
                    arquivo,
                    ParcelFileDescriptor.MODE_READ_ONLY
                )

            renderer =
                PdfRenderer(
                    descriptor!!
                )

            totalPaginas =
                renderer!!.pageCount

            if (totalPaginas <= 0) {

                Toast.makeText(
                    this,
                    tr("PDF sem páginas"),
                    Toast.LENGTH_LONG
                ).show()

                finish()
                return
            }

            mostrarPagina(0)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                tr("Erro ao abrir PDF: {0}", e.message),
                Toast.LENGTH_LONG
            ).show()

            finish()
        }
    }

    // =========================================================
    // INTERFACE
    // =========================================================

    private fun criarInterface() {

        val raiz =
            LinearLayout(this)

        raiz.orientation =
            LinearLayout.VERTICAL

        raiz.setBackgroundColor(
            Color.rgb(
                30,
                30,
                30
            )
        )

        // =====================================================
        // BARRA SUPERIOR
        // =====================================================

        val barra =
            LinearLayout(this)

        barra.orientation =
            LinearLayout.HORIZONTAL

        barra.gravity =
            Gravity.CENTER_VERTICAL

        barra.setPadding(
            12,
            8,
            12,
            8
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

        val voltar =
            Button(this)

        voltar.text =
            "‹"

        voltar.textSize =
            28f

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
        // INFORMAÇÃO DA PÁGINA
        // =====================================================

        pageInfo =
            TextView(this)

        pageInfo.setTextColor(
            Color.WHITE
        )

        pageInfo.textSize =
            16f

        pageInfo.gravity =
            Gravity.CENTER

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
                ViewGroup.LayoutParams.MATCH_PARENT,
                65
            )
        )

        // =====================================================
        // ÁREA DO PDF
        // =====================================================

        imageView =
            ImageView(this)

        imageView.scaleType =
            ImageView.ScaleType.FIT_CENTER

        imageView.setBackgroundColor(
            Color.rgb(
                50,
                50,
                50
            )
        )

        val scroll =
            ScrollView(this)

        scroll.setBackgroundColor(
            Color.rgb(
                50,
                50,
                50
            )
        )

        // Não usamos ScrollView.LayoutParams.
        // O ScrollView cria os parâmetros corretos.

        scroll.addView(
            imageView
        )

        raiz.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // =====================================================
        // CONTROLES
        // =====================================================

        val controles =
            LinearLayout(this)

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
            Color.rgb(
                21,
                101,
                192
            )
        )

        // =====================================================
        // ANTERIOR
        // =====================================================

        val anterior =
            Button(this)

        anterior.text =
            tr("‹ Anterior")

        anterior.setOnClickListener {

            if (paginaAtual > 0) {

                mostrarPagina(
                    paginaAtual - 1
                )
            }
        }

        // =====================================================
        // PRÓXIMA
        // =====================================================

        val proxima =
            Button(this)

        proxima.text =
            tr("Próxima ›")

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
                0,
                55,
                1f
            )
        )

        controles.addView(
            proxima,
            LinearLayout.LayoutParams(
                0,
                55,
                1f
            )
        )

        raiz.addView(
            controles,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                70
            )
        )

        setContentView(
            raiz
        )
    }

    // =========================================================
    // MOSTRAR PÁGINA
    // =========================================================

    private fun mostrarPagina(
        numero: Int
    ) {

        val pdf =
            renderer ?: return

        if (
            numero < 0 ||
            numero >= pdf.pageCount
        ) {
            return
        }

        try {

            val pagina =
                pdf.openPage(numero)

            val largura =
                pagina.width

            val altura =
                pagina.height

            // Renderiza em resolução maior para
            // deixar o PDF mais nítido.

            val bitmap =
                Bitmap.createBitmap(
                    largura * 2,
                    altura * 2,
                    Bitmap.Config.ARGB_8888
                )

            bitmap.eraseColor(
                Color.WHITE
            )

            pagina.render(
                bitmap,
                null,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            )

            pagina.close()

            imageView.setImageBitmap(
                bitmap
            )

            paginaAtual =
                numero

            pageInfo.text =
                tr("Página {0} de {1}", numero + 1, totalPaginas)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                tr("Erro ao renderizar página: {0}", e.message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // LIMPEZA
    // =========================================================

    override fun onDestroy() {

        imageViewIfInitialized()

        try {
            renderer?.close()
        } catch (_: Exception) {
        }

        try {
            descriptor?.close()
        } catch (_: Exception) {
        }

        renderer = null
        descriptor = null

        super.onDestroy()
    }

    private fun imageViewIfInitialized() {

        if (::imageView.isInitialized) {

            imageView.setImageDrawable(
                null
            )
        }
    }
}
