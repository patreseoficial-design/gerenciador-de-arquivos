package com.gerenciadordearquivos.app

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.Locale
import kotlin.concurrent.thread

/*
 * =============================================================
 * FOTOS E VÍDEOS DUPLICADOS
 * Acha arquivos com conteúdo idêntico (mesmo tamanho e mesma
 * "impressão digital"). Em cada grupo mantém o mais antigo e
 * marca as cópias. Ver é grátis; remover é do Premium.
 * =============================================================
 */

class DuplicadasActivity : TelaBase() {

    private var itens: List<ItemArquivo> = emptyList()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        titulo.text = tr("Fotos duplicadas")

        procurar()
    }

    override fun onResume() {

        super.onResume()

        // Voltou da tela do Premium: atualiza o botão
        if (itens.isNotEmpty()) {
            atualizarRodape()
        }
    }

    private fun pastasParaProcurar(): List<File> {

        val raiz = Armazenamento.raizCelular

        return listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            File(raiz, "Android/media/com.whatsapp/WhatsApp/Media"),
            File(raiz, "WhatsApp/Media")
        ).filter { it.isDirectory }
    }

    private fun procurar() {

        status.text = tr("Procurando fotos e vídeos...")
        acoes.removeAllViews()
        rodape.visibility = View.GONE
        lista.adapter = null

        thread {

            // 1) Junta fotos e vídeos
            val midias =
                pastasParaProcurar()
                    .flatMap { pasta ->
                        pasta.walkTopDown()
                            .onEnter { !it.name.startsWith(".") || it.name == ".Statuses" }
                            .filter { it.isFile }
                            .filter {
                                val ext = it.extension.lowercase(Locale.getDefault())
                                ext in TIPO_IMAGEM || ext in TIPO_VIDEO
                            }
                            .toList()
                    }
                    .distinctBy { it.absolutePath }

            // 2) Só compara arquivos com o mesmo tamanho
            val candidatos =
                midias
                    .groupBy { it.length() }
                    .filter { it.key > 0 && it.value.size > 1 }
                    .values
                    .flatten()

            // 3) Compara a "impressão digital" do conteúdo
            val grupos = HashMap<String, MutableList<File>>()

            candidatos.forEachIndexed { indice, arquivo ->

                if (indice % 20 == 0) {
                    runOnUiThread {
                        status.text = tr("Comparando {0} de {1}...", indice + 1, candidatos.size)
                    }
                }

                val digital = impressaoDigital(arquivo) ?: return@forEachIndexed

                grupos.getOrPut("${arquivo.length()}:$digital") { mutableListOf() }
                    .add(arquivo)
            }

            val duplicados =
                grupos.values
                    .filter { it.size > 1 }
                    .sortedByDescending { grupo -> grupo.first().length() * (grupo.size - 1) }

            val novaLista = mutableListOf<ItemArquivo>()

            duplicados.forEachIndexed { numero, grupo ->

                val ordenado = grupo.sortedBy { it.lastModified() }

                novaLista.add(
                    ItemArquivo(
                        null,
                        cabecalho = tr("Grupo {0} • {1} cópias iguais", numero + 1, grupo.size)
                    )
                )

                ordenado.forEachIndexed { i, arquivo ->
                    novaLista.add(
                        ItemArquivo(
                            arquivo,
                            marcado = i > 0,
                            rotulo = if (i == 0) tr("Manter (original)") else null
                        )
                    )
                }
            }

            runOnUiThread {

                if (isFinishing) return@runOnUiThread

                itens = novaLista

                mostrarResultado(duplicados.size)
            }
        }
    }

    // MD5 do arquivo inteiro (até 64 MB) ou do começo + fim +
    // tamanho para arquivos maiores (vídeos grandes)
    private fun impressaoDigital(
        arquivo: File
    ): String? {

        return try {

            val md5 = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(64 * 1024)
            val limite = 64L * 1024 * 1024

            if (arquivo.length() <= limite) {

                FileInputStream(arquivo).use { entrada ->
                    while (true) {
                        val lidos = entrada.read(buffer)
                        if (lidos <= 0) break
                        md5.update(buffer, 0, lidos)
                    }
                }

            } else {

                RandomAccessFile(arquivo, "r").use { entrada ->
                    val pedaco = 1024 * 1024
                    val bloco = ByteArray(pedaco)

                    entrada.readFully(bloco)
                    md5.update(bloco)

                    entrada.seek(arquivo.length() / 2)
                    entrada.readFully(bloco)
                    md5.update(bloco)

                    entrada.seek(arquivo.length() - pedaco)
                    entrada.readFully(bloco)
                    md5.update(bloco)
                }
            }

            md5.digest().joinToString("") { "%02x".format(it) }

        } catch (_: Exception) {
            null
        }
    }

    private fun mostrarResultado(
        quantidadeGrupos: Int
    ) {

        acoes.removeAllViews()

        if (quantidadeGrupos == 0) {
            status.text = tr("Nenhuma foto ou vídeo duplicado encontrado 🎉")
            rodape.visibility = View.GONE
            return
        }

        val copias = itens.count { it.arquivo != null && it.rotulo == null }
        val espaco = itens.filter { it.arquivo != null && it.rotulo == null }.sumOf { it.arquivo!!.length() }

        status.text = tr("{0} cópia(s) ocupando {1}", copias, formatarBytes(espaco))

        acoes.addView(
            criarTexto(
                tr("O mais antigo de cada grupo fica guardado. As cópias já vêm marcadas; toque em uma foto para conferir.")
            )
        )

        lista.adapter = SelecaoAdapter(itens) { atualizarRodape() }

        lista.setOnItemClickListener { _, _, position, _ ->
            itens.getOrNull(position)?.arquivo?.let { abrirArquivo(it) }
        }

        atualizarRodape()
    }

    private fun atualizarRodape() {

        val marcados = itens.filter { it.marcado && it.arquivo != null }

        val premium = Premium.ativo(this)

        val texto =
            when {
                marcados.isEmpty() -> tr("Marque as cópias que quer apagar")
                premium -> tr("Mover {0} cópia(s) para a lixeira ({1})", marcados.size, formatarBytes(marcados.sumOf { it.arquivo!!.length() }))
                else -> tr("⭐ Liberar {0} com o Premium", formatarBytes(marcados.sumOf { it.arquivo!!.length() }))
            }

        rodape.removeAllViews()

        adicionarBotao(
            rodape,
            criarBotao(
                texto,
                if (premium) R.drawable.ic_acao_lixeira else null,
                if (premium) COR_VERMELHO else COR_DOURADO
            ) {
                if (Premium.ativo(this)) {
                    limpar()
                } else {
                    startActivity(Intent(this, PremiumActivity::class.java))
                }
            }
        )

        rodape.visibility = View.VISIBLE
    }

    private fun limpar() {

        val marcados = itens.filter { it.marcado }.mapNotNull { it.arquivo }

        if (marcados.isEmpty()) {
            Toast.makeText(this, tr("Marque as cópias que quer apagar"), Toast.LENGTH_SHORT).show()
            return
        }

        // Segurança: nunca apagar todas as cópias de um grupo
        val grupoSemOriginal =
            itens
                .fold(mutableListOf<MutableList<ItemArquivo>>()) { grupos, item ->
                    if (item.arquivo == null) grupos.add(mutableListOf()) else grupos.lastOrNull()?.add(item)
                    grupos
                }
                .any { grupo -> grupo.isNotEmpty() && grupo.all { it.marcado } }

        if (grupoSemOriginal) {
            Toast.makeText(
                this,
                tr("Deixe pelo menos uma foto de cada grupo desmarcada"),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(tr("Apagar {0} cópia(s)?", marcados.size))
            .setMessage(tr("Elas vão para a lixeira do Arquivos Pro e podem ser restauradas."))
            .setNegativeButton(tr("Cancelar"), null)
            .setPositiveButton(tr("Apagar")) { _, _ ->

                status.text = tr("Limpando...")

                moverParaLixeira(marcados) { movidos, bytes ->

                    Toast.makeText(
                        this,
                        tr("Pronto! {0} liberados ({1} cópia(s))", formatarBytes(bytes), movidos),
                        Toast.LENGTH_LONG
                    ).show()

                    procurar()
                }
            }
            .show()
    }
}
