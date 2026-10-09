package com.gerenciadordearquivos.app

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File

/*
 * =============================================================
 * ARMAZENAMENTO
 * Cartão de memória, mover entre celular e cartão e lixeira
 * com restauração.
 * =============================================================
 */

object Armazenamento {

    val raizCelular: File =
        Environment.getExternalStorageDirectory()

    val pastaLixeira: File =
        File(
            raizCelular,
            ".GerenciadorArquivos/.Lixeira"
        )

    private const val PREFS_LIXEIRA =
        "lixeira_origens"

    // ------------------------------------------------------------
    // CARTÃO DE MEMÓRIA
    // ------------------------------------------------------------

    // Raiz do cartão de memória, ou null se não houver cartão
    fun raizCartao(
        context: Context
    ): File? {

        val pastas =
            try {
                context.getExternalFilesDirs(null)
            } catch (
                _: Exception
            ) {
                emptyArray()
            }

        for (pasta in pastas) {

            if (pasta == null) {
                continue
            }

            val removivel =
                try {
                    Environment.isExternalStorageRemovable(pasta)
                } catch (
                    _: Exception
                ) {
                    false
                }

            val montado =
                try {
                    Environment.getExternalStorageState(pasta) ==
                        Environment.MEDIA_MOUNTED
                } catch (
                    _: Exception
                ) {
                    false
                }

            if (!removivel || !montado) {
                continue
            }

            val caminho =
                pasta.absolutePath

            val indice =
                caminho.indexOf("/Android/")

            if (indice > 0) {
                return File(
                    caminho.substring(0, indice)
                )
            }
        }

        return null
    }

    fun estaNoCartao(
        context: Context,
        arquivo: File
    ): Boolean {

        val cartao =
            raizCartao(context) ?: return false

        val caminho =
            arquivo.absolutePath

        return caminho == cartao.absolutePath ||
            caminho.startsWith(
                cartao.absolutePath + File.separator
            )
    }

    data class Espaco(
        val total: Long,
        val livre: Long
    ) {
        val usado: Long
            get() = total - livre
    }

    fun espaco(
        pasta: File
    ): Espaco? {

        return try {

            val stat =
                StatFs(pasta.absolutePath)

            Espaco(
                stat.blockCountLong * stat.blockSizeLong,
                stat.availableBlocksLong * stat.blockSizeLong
            )

        } catch (
            _: Exception
        ) {
            null
        }
    }

    // Mesmo caminho relativo no outro armazenamento.
    // Ex.: celular/DCIM/foto.jpg -> cartão/DCIM/
    fun pastaEquivalente(
        arquivo: File,
        origemRaiz: File,
        destinoRaiz: File
    ): File {

        val pai =
            arquivo.parentFile ?: return destinoRaiz

        val relativo =
            pai.absolutePath
                .removePrefix(origemRaiz.absolutePath)
                .trimStart(File.separatorChar)

        return if (relativo.isEmpty()) {
            destinoRaiz
        } else {
            File(destinoRaiz, relativo)
        }
    }

    // ------------------------------------------------------------
    // MOVER / COPIAR (funciona entre celular e cartão)
    // ------------------------------------------------------------

    fun nomeLivre(
        pasta: File,
        nome: String
    ): File {

        var destino =
            File(pasta, nome)

        if (!destino.exists()) {
            return destino
        }

        val ponto =
            nome.lastIndexOf('.')

        val base =
            if (ponto > 0) nome.substring(0, ponto) else nome

        val extensao =
            if (ponto > 0) nome.substring(ponto) else ""

        var contador = 1

        while (destino.exists()) {

            destino =
                File(pasta, "${base}_$contador$extensao")

            contador++
        }

        return destino
    }

    // Move arquivo ou pasta. Tenta renomear (rápido); se forem
    // armazenamentos diferentes, copia e depois apaga o original.
    fun mover(
        origem: File,
        destino: File
    ): Boolean {

        destino.parentFile?.mkdirs()

        if (
            try {
                origem.renameTo(destino)
            } catch (
                _: Exception
            ) {
                false
            }
        ) {
            return true
        }

        val copiou =
            try {
                origem.copyRecursively(
                    destino,
                    overwrite = false
                )
            } catch (
                _: Exception
            ) {
                false
            }

        if (!copiou) {

            destino.deleteRecursively()

            return false
        }

        return origem.deleteRecursively()
    }

    // ------------------------------------------------------------
    // LIXEIRA
    // ------------------------------------------------------------

    private fun prefs(
        context: Context
    ) =
        context.getSharedPreferences(
            PREFS_LIXEIRA,
            Context.MODE_PRIVATE
        )

    // Guarda de onde o arquivo veio, para poder restaurar
    fun registrarNaLixeira(
        context: Context,
        naLixeira: File,
        origem: File
    ) {

        prefs(context)
            .edit()
            .putString(
                naLixeira.name,
                origem.absolutePath
            )
            .apply()
    }

    fun estaNaLixeira(
        arquivo: File
    ): Boolean {

        return arquivo.parentFile?.absolutePath ==
            pastaLixeira.absolutePath
    }

    fun itensDaLixeira(): List<File> {

        return pastaLixeira
            .listFiles()
            ?.toList()
            ?: emptyList()
    }

    // Devolve o arquivo para a pasta de onde veio. Se não se
    // sabe a origem, vai para a pasta "Restaurados".
    fun restaurar(
        context: Context,
        arquivo: File
    ): File? {

        val origemSalva =
            prefs(context)
                .getString(arquivo.name, null)
                ?.let { File(it) }

        val pastaDestino =
            origemSalva?.parentFile
                ?: File(raizCelular, "Restaurados")

        pastaDestino.mkdirs()

        val destino =
            nomeLivre(
                pastaDestino,
                origemSalva?.name ?: arquivo.name
            )

        if (!mover(arquivo, destino)) {
            return null
        }

        prefs(context)
            .edit()
            .remove(arquivo.name)
            .apply()

        return destino
    }

    fun excluirDefinitivamente(
        context: Context,
        arquivo: File
    ): Boolean {

        val ok =
            arquivo.deleteRecursively()

        if (ok) {
            prefs(context)
                .edit()
                .remove(arquivo.name)
                .apply()
        }

        return ok
    }

    // Apaga tudo; devolve quantos itens foram apagados
    fun esvaziarLixeira(
        context: Context
    ): Int {

        var apagados = 0

        for (item in itensDaLixeira()) {

            if (excluirDefinitivamente(context, item)) {
                apagados++
            }
        }

        return apagados
    }
}
