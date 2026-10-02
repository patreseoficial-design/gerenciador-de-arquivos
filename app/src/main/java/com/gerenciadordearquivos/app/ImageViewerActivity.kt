package com.gerenciadordearquivos.app

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class ImageViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val caminho =
            intent.getStringExtra("arquivo")

        if (caminho.isNullOrEmpty()) {
            finish()
            return
        }

        val arquivo =
            File(caminho)

        if (!arquivo.exists()) {
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

    private fun criarInterface(
        arquivo: File
    ) {

        val raiz =
            LinearLayout(this)

        raiz.orientation =
            LinearLayout.VERTICAL

        raiz.setBackgroundColor(
            Color.BLACK
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

        val nome =
            TextView(this)

        nome.text =
            arquivo.name

        nome.textColor =
            Color.WHITE

        nome.textSize = 15f

        nome.gravity =
            Gravity.CENTER_VERTICAL

        nome.maxLines = 1

        nome.ellipsize =
            android.text.TextUtils.TruncateAt.END

        barra.addView(
            nome,
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

        // IMAGEM

        val imagem =
            ImageView(this)

        imagem.scaleType =
            ImageView.ScaleType.FIT_CENTER

        val bitmap =
            BitmapFactory.decodeFile(
                arquivo.absolutePath
            )

        if (bitmap != null) {

            imagem.setImageBitmap(
                bitmap
            )

        } else {

            Toast.makeText(
                this,
                "Não foi possível carregar a imagem",
                Toast.LENGTH_LONG
            ).show()
        }

        raiz.addView(
            imagem,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(raiz)
    }
}
