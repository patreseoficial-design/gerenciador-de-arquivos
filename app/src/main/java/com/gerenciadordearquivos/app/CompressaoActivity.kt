package com.gerenciadordearquivos.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.concurrent.thread

/*
 * =============================================================
 * COMPRIMIR FOTOS (Premium)
 * Regrava fotos JPEG grandes com qualidade alta (85%) e no
 * máximo 3000 px no lado maior. A foto continua bonita na tela
 * e costuma ficar de 2 a 4 vezes menor. Data, local e rotação
 * (EXIF) são mantidos. A original vai para a lixeira, para
 * poder voltar atrás.
 * =============================================================
 */

class CompressaoActivity : TelaBase() {

    private companion object {
        const val TAMANHO_MINIMO = 1_500_000L
        const val LADO_MAXIMO = 3000
        const val QUALIDADE = 85
    }

    private var itens: List<ItemArquivo> = emptyList()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        titulo.text = tr("Comprimir fotos")

        procurar()
    }

    override fun onResume() {

        super.onResume()

        if (itens.isNotEmpty()) atualizarRodape()
    }

    private fun procurar() {

        status.text = tr("Procurando fotos grandes...")
        acoes.removeAllViews()
        rodape.visibility = View.GONE
        lista.adapter = null

        thread {

            val fotos =
                listOf(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                )
                    .filter { it.isDirectory }
                    .flatMap { pasta ->
                        pasta.walkTopDown()
                            .onEnter { !it.name.startsWith(".") }
                            .filter { it.isFile }
                            .filter {
                                val ext = it.extension.lowercase(Locale.getDefault())
                                (ext == "jpg" || ext == "jpeg") && it.length() >= TAMANHO_MINIMO
                            }
                            .toList()
                    }
                    .sortedByDescending { it.length() }

            runOnUiThread {

                if (isFinishing) return@runOnUiThread

                itens = fotos.map { ItemArquivo(it, marcado = true) }

                mostrar()
            }
        }
    }

    private fun mostrar() {

        acoes.removeAllViews()

        if (itens.isEmpty()) {
            status.text = tr("Nenhuma foto grande para comprimir 🎉")
            rodape.visibility = View.GONE
            return
        }

        val total = itens.sumOf { it.arquivo!!.length() }

        status.text =
            tr("{0} foto(s) grandes • {1} • economia estimada: ~{2}", itens.size, formatarBytes(total), formatarBytes(total * 6 / 10))

        acoes.addView(
            criarTexto(
                tr("As fotos ficam com qualidade alta (boa para ver no celular e postar), mantendo data e local. As originais vão para a lixeira: esvazie a lixeira depois para liberar o espaço de vez.")
            )
        )

        lista.adapter = SelecaoAdapter(itens) { atualizarRodape() }

        lista.setOnItemClickListener { _, _, position, _ ->
            itens.getOrNull(position)?.arquivo?.let { abrirArquivo(it) }
        }

        atualizarRodape()
    }

    private fun atualizarRodape() {

        val marcados = itens.filter { it.marcado }

        val premium = Premium.ativo(this)

        rodape.removeAllViews()

        adicionarBotao(
            rodape,
            criarBotao(
                when {
                    marcados.isEmpty() -> tr("Marque as fotos")
                    premium -> tr("Comprimir {0} foto(s)", marcados.size)
                    else -> tr("⭐ Comprimir com o Premium")
                },
                null,
                if (premium) COR_AZUL else COR_DOURADO
            ) {
                if (!Premium.ativo(this)) {
                    startActivity(Intent(this, PremiumActivity::class.java))
                } else {
                    confirmar()
                }
            }
        )

        rodape.visibility = View.VISIBLE
    }

    private fun confirmar() {

        val marcados = itens.filter { it.marcado }.mapNotNull { it.arquivo }

        if (marcados.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle(tr("Comprimir {0} foto(s)?", marcados.size))
            .setMessage(
                tr("As fotos originais irão para a lixeira do Arquivos Pro (dá para restaurar se não gostar).")
            )
            .setNegativeButton(tr("Cancelar"), null)
            .setPositiveButton(tr("Comprimir")) { _, _ -> comprimir(marcados) }
            .show()
    }

    private fun comprimir(
        fotos: List<File>
    ) {

        rodape.visibility = View.GONE

        thread {

            var economia = 0L
            var feitas = 0

            fotos.forEachIndexed { i, foto ->

                runOnUiThread {
                    status.text = tr("Comprimindo {0} de {1}...", i + 1, fotos.size)
                }

                val ganho = comprimirFoto(foto)

                if (ganho > 0) {
                    economia += ganho
                    feitas++
                }
            }

            runOnUiThread {

                if (isFinishing) return@runOnUiThread

                Toast.makeText(
                    this,
                    tr("{0} foto(s) comprimidas • {1} a menos. Esvazie a lixeira para liberar o espaço.", feitas, formatarBytes(economia)),
                    Toast.LENGTH_LONG
                ).show()

                procurar()
            }
        }
    }

    // Devolve quantos bytes economizou (0 se não valeu a pena)
    private fun comprimirFoto(
        foto: File
    ): Long {

        val temporario = File(foto.parentFile, ".${foto.nameWithoutExtension}.faxina.tmp")

        return try {

            val limites = BitmapFactory.Options()
            limites.inJustDecodeBounds = true
            BitmapFactory.decodeFile(foto.absolutePath, limites)

            val maior = maxOf(limites.outWidth, limites.outHeight)
            if (maior <= 0) return 0

            var amostra = 1
            while (maior / (amostra * 2) >= LADO_MAXIMO) amostra *= 2

            val opcoes = BitmapFactory.Options()
            opcoes.inSampleSize = amostra

            var bitmap = BitmapFactory.decodeFile(foto.absolutePath, opcoes) ?: return 0

            val ladoAtual = maxOf(bitmap.width, bitmap.height)

            if (ladoAtual > LADO_MAXIMO) {
                val escala = LADO_MAXIMO.toFloat() / ladoAtual
                val reduzido =
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * escala).toInt(),
                        (bitmap.height * escala).toInt(),
                        true
                    )
                bitmap.recycle()
                bitmap = reduzido
            }

            FileOutputStream(temporario).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, QUALIDADE, it)
            }

            bitmap.recycle()

            val economia = foto.length() - temporario.length()

            // Só troca se ficou pelo menos 20% menor
            if (economia < foto.length() / 5) {
                temporario.delete()
                return 0
            }

            copiarExif(foto, temporario)

            temporario.setLastModified(foto.lastModified())

            // Original para a lixeira; comprimida no lugar dela
            val naLixeira = Armazenamento.nomeLivre(Armazenamento.pastaLixeira, foto.name)
            val caminhoOriginal = File(foto.absolutePath)

            if (!Armazenamento.mover(foto, naLixeira)) {
                temporario.delete()
                return 0
            }

            Armazenamento.registrarNaLixeira(this, naLixeira, caminhoOriginal)

            if (!temporario.renameTo(caminhoOriginal)) {
                // Não deu para colocar no lugar: devolve a original
                Armazenamento.mover(naLixeira, caminhoOriginal)
                temporario.delete()
                return 0
            }

            // Avisa a galeria que a foto mudou
            android.media.MediaScannerConnection.scanFile(
                this,
                arrayOf(caminhoOriginal.absolutePath),
                null,
                null
            )

            economia

        } catch (_: Throwable) {

            temporario.delete()

            0
        }
    }

    private fun copiarExif(
        origem: File,
        destino: File
    ) {

        try {

            val antigo = ExifInterface(origem.absolutePath)
            val novo = ExifInterface(destino.absolutePath)

            listOf(
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_DATETIME_DIGITIZED,
                ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_F_NUMBER,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_ISO_SPEED_RATINGS,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_FLASH,
                ExifInterface.TAG_WHITE_BALANCE
            ).forEach { tag ->
                antigo.getAttribute(tag)?.let { novo.setAttribute(tag, it) }
            }

            novo.saveAttributes()

        } catch (_: Exception) {
        }
    }
}
