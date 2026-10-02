package com.gerenciadordearquivos.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
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

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var imagem: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val caminho = intent.getStringExtra("arquivo")

        if (caminho.isNullOrEmpty()) {
            Toast.makeText(
                this,
                "Imagem inválida",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        val arquivo = File(caminho)

        if (!arquivo.exists() || !arquivo.isFile) {
            Toast.makeText(
                this,
                "Imagem não encontrada",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        criarInterface(arquivo)
    }

    private fun criarInterface(arquivo: File) {

        val raiz = LinearLayout(this)

        raiz.orientation = LinearLayout.VERTICAL

        raiz.setBackgroundColor(
            Color.BLACK
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
            Color.rgb(21, 101, 192)
        )

        // =====================================================
        // BOTÃO VOLTAR
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
        // NOME DO ARQUIVO
        // =====================================================

        val nome = TextView(this)

        nome.text = arquivo.name

        nome.setTextColor(
            Color.WHITE
        )

        nome.textSize = 15f

        nome.gravity =
            Gravity.CENTER_VERTICAL

        nome.maxLines = 1

        nome.ellipsize =
            android.text.TextUtils.TruncateAt.END

        val nomeParams =
            LinearLayout.LayoutParams(
                0,
                55,
                1f
            )

        nomeParams.leftMargin = 8

        barra.addView(
            nome,
            nomeParams
        )

        raiz.addView(
            barra,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                65
            )
        )

        // =====================================================
        // ÁREA DA IMAGEM
        // =====================================================

        val scroll = ScrollView(this)

        scroll.setBackgroundColor(
            Color.BLACK
        )

        imagem = ImageView(this)

        imagem.scaleType =
            ImageView.ScaleType.FIT_CENTER

        imagem.adjustViewBounds =
            true

        imagem.setBackgroundColor(
            Color.BLACK
        )

        // =====================================================
        // CARREGAR IMAGEM
        // =====================================================

        val bitmap = carregarImagem(
            arquivo
        )

        if (bitmap == null) {

            Toast.makeText(
                this,
                "Não foi possível carregar a imagem",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        imagem.setImageBitmap(
            bitmap
        )

        // Não usamos ScrollView.LayoutParams aqui.
        // O próprio ScrollView cria os parâmetros corretos.

        scroll.addView(
            imagem
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
        // INFORMAÇÕES DA IMAGEM
        // =====================================================

        val informacoes = TextView(this)

        informacoes.setTextColor(
            Color.WHITE
        )

        informacoes.textSize = 12f

        informacoes.gravity =
            Gravity.CENTER

        informacoes.setPadding(
            8,
            5,
            8,
            5
        )

        informacoes.text =
            "${bitmap.width} × ${bitmap.height}"

        raiz.addView(
            informacoes,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                40
            )
        )

        // =====================================================
        // MOSTRAR INTERFACE
        // =====================================================

        setContentView(
            raiz
        )
    }

    // =========================================================
    // CARREGAMENTO SEGURO DA IMAGEM
    // =========================================================

    private fun carregarImagem(
        arquivo: File
    ): Bitmap? {

        return try {

            // Primeiro descobrimos as dimensões
            // sem carregar a imagem inteira.

            val limites =
                BitmapFactory.Options()

            limites.inJustDecodeBounds =
                true

            BitmapFactory.decodeFile(
                arquivo.absolutePath,
                limites
            )

            val largura =
                limites.outWidth

            val altura =
                limites.outHeight

            if (
                largura <= 0 ||
                altura <= 0
            ) {
                return null
            }

            // Limite máximo da imagem carregada.
            // Isso reduz o risco de OutOfMemoryError.

            val tamanhoMaximo = 4096

            var sample = 1

            while (
                largura / sample > tamanhoMaximo ||
                altura / sample > tamanhoMaximo
            ) {
                sample *= 2
            }

            val opcoes =
                BitmapFactory.Options()

            opcoes.inSampleSize =
                sample

            opcoes.inPreferredConfig =
                Bitmap.Config.ARGB_8888

            BitmapFactory.decodeFile(
                arquivo.absolutePath,
                opcoes
            )

        } catch (
            _: OutOfMemoryError
        ) {

            null

        } catch (
            _: Exception
        ) {

            null
        }
    }

    // =========================================================
    // LIMPEZA
    // =========================================================

    override fun onDestroy() {

        if (::imagem.isInitialized) {

            imagem.setImageDrawable(
                null
            )
        }

        super.onDestroy()
    }
}
