package com.gerenciadordearquivos.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PointF
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var imagem: ZoomImageView

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

        // =====================================================
        // RAIZ
        // =====================================================

        val raiz = LinearLayout(this)

        raiz.orientation =
            LinearLayout.VERTICAL

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

        voltar.setBackgroundColor(
            Color.TRANSPARENT
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

        nome.text =
            arquivo.name

        nome.setTextColor(
            Color.WHITE
        )

        nome.textSize =
            15f

        nome.gravity =
            Gravity.CENTER_VERTICAL

        nome.maxLines =
            1

        nome.ellipsize =
            android.text.TextUtils.TruncateAt.END

        val nomeParams =
            LinearLayout.LayoutParams(
                0,
                55,
                1f
            )

        nomeParams.leftMargin =
            8

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
        // IMAGEM COM ZOOM
        // =====================================================

        imagem = ZoomImageView(this)

        imagem.setBackgroundColor(
            Color.BLACK
        )

        // =====================================================
        // CARREGAR IMAGEM
        // =====================================================

        val bitmap =
            carregarImagem(arquivo)

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

        // =====================================================
        // ÁREA DA IMAGEM
        // =====================================================

        raiz.addView(
            imagem,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // =====================================================
        // INFORMAÇÕES
        // =====================================================

        val informacoes = TextView(this)

        informacoes.setTextColor(
            Color.WHITE
        )

        informacoes.textSize =
            12f

        informacoes.gravity =
            Gravity.CENTER

        informacoes.setPadding(
            8,
            5,
            8,
            5
        )

        informacoes.text =
            "${bitmap.width} × ${bitmap.height}   •   Use dois dedos para ampliar"

        raiz.addView(
            informacoes,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                40
            )
        )

        // =====================================================
        // MOSTRAR
        // =====================================================

        setContentView(
            raiz
        )
    }

    // =========================================================
    // CARREGAMENTO SEGURO
    // =========================================================

    private fun carregarImagem(
        arquivo: File
    ): Bitmap? {

        return try {

            // -------------------------------------------------
            // PRIMEIRO: descobrir dimensões
            // -------------------------------------------------

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

            // -------------------------------------------------
            // TAMANHO MÁXIMO
            // -------------------------------------------------

            val tamanhoMaximo =
                4096

            var sample =
                1

            while (
                largura / sample > tamanhoMaximo ||
                altura / sample > tamanhoMaximo
            ) {

                sample *= 2
            }

            // -------------------------------------------------
            // CARREGAR BITMAP
            // -------------------------------------------------

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

            imagem.setImageBitmap(
                null
            )
        }

        super.onDestroy()
    }
}


// =============================================================
// VISUALIZADOR DE IMAGEM COM ZOOM
// =============================================================

class ZoomImageView(
    context: android.content.Context
) : ImageView(context) {

    private val matriz =
        Matrix()

    private val matrizInicial =
        Matrix()

    private var escalaAtual =
        1f

    private var escalaMinima =
        1f

    private var escalaMaxima =
        5f

    private var inicializado =
        false

    private val pontoAnterior =
        PointF()

    private var arrastando =
        false

    private var ultimoNumeroDedos =
        0

    private val detectorZoom =
        ScaleGestureDetector(
            context,
            object :
                ScaleGestureDetector.SimpleOnScaleGestureListener() {

                override fun onScaleBegin(
                    detector: ScaleGestureDetector
                ): Boolean {

                    return true
                }

                override fun onScale(
                    detector: ScaleGestureDetector
                ): Boolean {

                    var novaEscala =
                        escalaAtual *
                            detector.scaleFactor

                    if (
                        novaEscala <
                        escalaMinima
                    ) {
                        novaEscala =
                            escalaMinima
                    }

                    if (
                        novaEscala >
                        escalaMaxima
                    ) {
                        novaEscala =
                            escalaMaxima
                    }

                    val fator =
                        novaEscala /
                            escalaAtual

                    matriz.postScale(
                        fator,
                        fator,
                        detector.focusX,
                        detector.focusY
                    )

                    escalaAtual =
                        novaEscala

                    limitarMovimento()

                    imageMatrix =
                        matriz

                    invalidate()

                    return true
                }
            }
        )

    init {

        scaleType =
            ScaleType.MATRIX

        setBackgroundColor(
            Color.BLACK
        )

        isClickable =
            true
    }

    // =========================================================
    // AJUSTAR IMAGEM PARA CABER INTEIRA
    // =========================================================

    override fun onSizeChanged(
        largura: Int,
        altura: Int,
        larguraAntiga: Int,
        alturaAntiga: Int
    ) {

        super.onSizeChanged(
            largura,
            altura,
            larguraAntiga,
            alturaAntiga
        )

        ajustarImagemInteira()
    }

    override fun setImageBitmap(
        bm: Bitmap?
    ) {

        super.setImageBitmap(
            bm
        )

        inicializado =
            false

        post {

            ajustarImagemInteira()
        }
    }

    private fun ajustarImagemInteira() {

        val bitmap =
            drawable ?: return

        val larguraView =
            width.toFloat()

        val alturaView =
            height.toFloat()

        if (
            larguraView <= 0 ||
            alturaView <= 0
        ) {
            return
        }

        val larguraImagem =
            bitmap.intrinsicWidth.toFloat()

        val alturaImagem =
            bitmap.intrinsicHeight.toFloat()

        if (
            larguraImagem <= 0 ||
            alturaImagem <= 0
        ) {
            return
        }

        // -----------------------------------------------------
        // ESCALA PARA A IMAGEM INTEIRA CABER NA TELA
        // -----------------------------------------------------

        val escalaX =
            larguraView /
                larguraImagem

        val escalaY =
            alturaView /
                alturaImagem

        escalaMinima =
            minOf(
                escalaX,
                escalaY
            )

        escalaAtual =
            escalaMinima

        escalaMaxima =
            escalaMinima * 5f

        // -----------------------------------------------------
        // CENTRALIZAR
        // -----------------------------------------------------

        val larguraFinal =
            larguraImagem *
                escalaMinima

        val alturaFinal =
            alturaImagem *
                escalaMinima

        val deslocamentoX =
            (larguraView -
                larguraFinal) / 2f

        val deslocamentoY =
            (alturaView -
                alturaFinal) / 2f

        matriz.reset()

        matriz.postScale(
            escalaMinima,
            escalaMinima
        )

        matriz.postTranslate(
            deslocamentoX,
            deslocamentoY
        )

        matrizInicial.set(
            matriz
        )

        imageMatrix =
            matriz

        inicializado =
            true

        invalidate()
    }

    // =========================================================
    // TOQUE / ZOOM / ARRASTAR
    // =========================================================

    override fun onTouchEvent(
        evento: MotionEvent
    ): Boolean {

        detectorZoom.onTouchEvent(
            evento
        )

        when (evento.actionMasked) {

            MotionEvent.ACTION_DOWN -> {

                pontoAnterior.set(
                    evento.x,
                    evento.y
                )

                arrastando =
                    true

                ultimoNumeroDedos =
                    1

                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {

                ultimoNumeroDedos =
                    evento.pointerCount

                arrastando =
                    false

                return true
            }

            MotionEvent.ACTION_MOVE -> {

                // ------------------------------------------------
                // Só arrasta quando não está fazendo pinch zoom
                // ------------------------------------------------

                if (
                    evento.pointerCount == 1 &&
                    arrastando &&
                    escalaAtual > escalaMinima
                ) {

                    val deslocamentoX =
                        evento.x -
                            pontoAnterior.x

                    val deslocamentoY =
                        evento.y -
                            pontoAnterior.y

                    matriz.postTranslate(
                        deslocamentoX,
                        deslocamentoY
                    )

                    limitarMovimento()

                    imageMatrix =
                        matriz

                    pontoAnterior.set(
                        evento.x,
                        evento.y
                    )

                    invalidate()
                }

                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {

                arrastando =
                    false

                ultimoNumeroDedos =
                    evento.pointerCount - 1

                return true
            }

            MotionEvent.ACTION_UP -> {

                arrastando =
                    false

                ultimoNumeroDedos =
                    0

                return true
            }

            MotionEvent.ACTION_CANCEL -> {

                arrastando =
                    false

                ultimoNumeroDedos =
                    0

                return true
            }
        }

        return true
    }

    // =========================================================
    // LIMITAR IMAGEM PARA NÃO SUMIR DA TELA
    // =========================================================

    private fun limitarMovimento() {

        val drawableAtual =
            drawable ?: return

        val valores =
            FloatArray(9)

        matriz.getValues(
            valores
        )

        val escalaX =
            valores[Matrix.MSCALE_X]

        val escalaY =
            valores[Matrix.MSCALE_Y]

        var esquerda =
            valores[Matrix.MTRANS_X]

        var topo =
            valores[Matrix.MTRANS_Y]

        val largura =
            drawableAtual.intrinsicWidth *
                escalaX

        val altura =
            drawableAtual.intrinsicHeight *
                escalaY

        val larguraView =
            width.toFloat()

        val alturaView =
            height.toFloat()

        // -----------------------------------------------------
        // HORIZONTAL
        // -----------------------------------------------------

        if (largura <= larguraView) {

            esquerda =
                (larguraView -
                    largura) / 2f

        } else {

            val limiteDireito =
                0f

            val limiteEsquerdo =
                larguraView -
                    largura

            if (
                esquerda >
                limiteDireito
            ) {
                esquerda =
                    limiteDireito
            }

            if (
                esquerda <
                limiteEsquerdo
            ) {
                esquerda =
                    limiteEsquerdo
            }
        }

        // -----------------------------------------------------
        // VERTICAL
        // -----------------------------------------------------

        if (altura <= alturaView) {

            topo =
                (alturaView -
                    altura) / 2f

        } else {

            val limiteBaixo =
                0f

            val limiteTopo =
                alturaView -
                    altura

            if (
                topo >
                limiteBaixo
            ) {
                topo =
                    limiteBaixo
            }

            if (
                topo <
                limiteTopo
            ) {
                topo =
                    limiteTopo
            }
        }

        valores[Matrix.MTRANS_X] =
            esquerda

        valores[Matrix.MTRANS_Y] =
            topo

        matriz.setValues(
            valores
        )
    }

    // =========================================================
    // DUPLO TOQUE PARA ZOOM RÁPIDO
    // =========================================================

    private var ultimoToque =
        0L

    override fun performClick(): Boolean {

        super.performClick()

        return true
    }
}
