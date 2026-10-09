package com.gerenciadordearquivos.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom

/*
 * =============================================================
 * COFRE (Premium)
 * Pasta escondida, protegida por PIN dentro do app. Os arquivos
 * somem da galeria e das outras pastas. Não é criptografia: é
 * uma área privada do Faxina.
 * =============================================================
 */

object Cofre {

    val pasta: File =
        File(Armazenamento.raizCelular, ".GerenciadorArquivos/.Cofre")

    private const val PREFS = "cofre"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun temPin(context: Context): Boolean =
        prefs(context).contains("pin_hash")

    private fun hash(pin: String, sal: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("$sal:$pin".toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun definirPin(context: Context, pin: String) {

        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        val sal = bytes.joinToString("") { "%02x".format(it) }

        prefs(context).edit()
            .putString("pin_sal", sal)
            .putString("pin_hash", hash(pin, sal))
            .apply()
    }

    fun pinCorreto(context: Context, pin: String): Boolean {

        val sal = prefs(context).getString("pin_sal", null) ?: return false

        return prefs(context).getString("pin_hash", null) == hash(pin, sal)
    }

    private fun prepararPasta() {

        pasta.mkdirs()

        // Impede a galeria de mostrar o que está no cofre
        val nomedia = File(pasta, ".nomedia")
        if (!nomedia.exists()) {
            try { nomedia.createNewFile() } catch (_: Exception) { }
        }
    }

    fun itens(): List<File> =
        pasta.listFiles()
            ?.filter { it.name != ".nomedia" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    // Guarda o arquivo no cofre (e de onde ele veio)
    fun guardar(context: Context, arquivo: File): Boolean {

        prepararPasta()

        val destino = Armazenamento.nomeLivre(pasta, arquivo.name)

        if (!Armazenamento.mover(arquivo, destino)) return false

        prefs(context).edit()
            .putString("origem:${destino.name}", arquivo.absolutePath)
            .apply()

        return true
    }

    // Devolve para a pasta de origem (ou para "Restaurados")
    fun retirar(context: Context, arquivo: File): File? {

        val origem =
            prefs(context).getString("origem:${arquivo.name}", null)?.let { File(it) }

        val pastaDestino =
            origem?.parentFile ?: File(Armazenamento.raizCelular, "Restaurados")

        pastaDestino.mkdirs()

        val destino =
            Armazenamento.nomeLivre(pastaDestino, origem?.name ?: arquivo.name)

        if (!Armazenamento.mover(arquivo, destino)) return null

        prefs(context).edit().remove("origem:${arquivo.name}").apply()

        return destino
    }
}

class CofreActivity : TelaBase() {

    private var desbloqueado = false

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        titulo.text = "Cofre"

        status.text = "🔒 Cofre trancado"

        // Sem Premium só dá para abrir se já houver algo guardado
        if (!Premium.ativo(this) && Cofre.itens().isEmpty()) {

            Toast.makeText(this, "O Cofre faz parte do Premium", Toast.LENGTH_LONG).show()

            startActivity(Intent(this, PremiumActivity::class.java))

            finish()

            return
        }

        if (Cofre.temPin(this)) pedirPin() else criarPin()
    }

    private fun campoPin(dica: String): EditText {

        val campo = EditText(this)
        campo.hint = dica
        campo.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        campo.textSize = 22f
        return campo
    }

    private fun caixa(vararg campos: View): LinearLayout {

        val caixa = LinearLayout(this)
        caixa.orientation = LinearLayout.VERTICAL
        caixa.setPadding(dp(24), dp(8), dp(24), 0)
        campos.forEach { caixa.addView(it) }
        return caixa
    }

    private fun criarPin() {

        val pin1 = campoPin("Novo PIN (4 a 8 números)")
        val pin2 = campoPin("Repita o PIN")

        AlertDialog.Builder(this)
            .setTitle("Crie o PIN do Cofre")
            .setMessage(
                "Guarde bem esse PIN: sem ele não dá para abrir o Cofre."
            )
            .setView(caixa(pin1, pin2))
            .setCancelable(false)
            .setNegativeButton("Cancelar") { _, _ -> finish() }
            .setPositiveButton("Criar") { _, _ ->

                val a = pin1.text.toString()
                val b = pin2.text.toString()

                when {
                    a.length !in 4..8 -> {
                        Toast.makeText(this, "O PIN precisa ter de 4 a 8 números", Toast.LENGTH_LONG).show()
                        criarPin()
                    }
                    a != b -> {
                        Toast.makeText(this, "Os PINs não são iguais", Toast.LENGTH_LONG).show()
                        criarPin()
                    }
                    else -> {
                        Cofre.definirPin(this, a)
                        desbloqueado = true
                        mostrarItens()
                    }
                }
            }
            .show()
    }

    private fun pedirPin() {

        val pin = campoPin("PIN")

        AlertDialog.Builder(this)
            .setTitle("Digite o PIN do Cofre")
            .setView(caixa(pin))
            .setCancelable(false)
            .setNegativeButton("Cancelar") { _, _ -> finish() }
            .setPositiveButton("Abrir") { _, _ ->

                if (Cofre.pinCorreto(this, pin.text.toString())) {
                    desbloqueado = true
                    mostrarItens()
                } else {
                    Toast.makeText(this, "PIN incorreto", Toast.LENGTH_SHORT).show()
                    pedirPin()
                }
            }
            .show()
    }

    // Tranca de novo ao sair do app
    override fun onStop() {

        super.onStop()

        if (desbloqueado && !isChangingConfigurations) {
            finish()
        }
    }

    private fun mostrarItens() {

        val arquivos = Cofre.itens()

        status.text =
            if (arquivos.isEmpty()) "🔓 Cofre vazio"
            else "🔓 ${arquivos.size} item(ns) no cofre"

        acoes.removeAllViews()
        acoes.addView(
            criarTexto(
                "Para guardar algo aqui, segure um arquivo na tela de arquivos " +
                    "e escolha \"Mover para o cofre\". Os arquivos do cofre não " +
                    "aparecem na galeria. Toque para abrir; segure para tirar do cofre."
            )
        )

        val itens = arquivos.map { ItemArquivo(it) }

        lista.adapter = SelecaoAdapter(itens) { }

        lista.setOnItemClickListener { _, _, position, _ ->
            itens.getOrNull(position)?.arquivo?.let { abrirArquivo(it) }
        }

        lista.setOnItemLongClickListener { _, _, position, _ ->

            val arquivo = itens.getOrNull(position)?.arquivo ?: return@setOnItemLongClickListener false

            AlertDialog.Builder(this)
                .setTitle(arquivo.name)
                .setItems(arrayOf("Tirar do cofre", "Mover para a lixeira")) { _, qual ->
                    when (qual) {
                        0 -> {
                            val destino = Cofre.retirar(this, arquivo)
                            Toast.makeText(
                                this,
                                if (destino != null) "Devolvido para ${destino.parentFile?.name}"
                                else "Não foi possível tirar do cofre",
                                Toast.LENGTH_SHORT
                            ).show()
                            mostrarItens()
                        }
                        1 -> moverParaLixeira(listOf(arquivo)) { _, _ -> mostrarItens() }
                    }
                }
                .show()

            true
        }

        // Seleção em massa
        rodape.removeAllViews()

        if (itens.isNotEmpty()) {

            adicionarBotao(
                rodape,
                criarBotao("Tirar marcados do cofre", null, COR_AZUL) {

                    val marcados = itens.filter { it.marcado }.mapNotNull { it.arquivo }

                    if (marcados.isEmpty()) {
                        Toast.makeText(this, "Marque os itens primeiro", Toast.LENGTH_SHORT).show()
                    } else {
                        val ok = marcados.count { Cofre.retirar(this, it) != null }
                        Toast.makeText(this, "$ok item(ns) devolvidos", Toast.LENGTH_SHORT).show()
                        mostrarItens()
                    }
                }
            )

            rodape.visibility = View.VISIBLE

        } else {

            rodape.visibility = View.GONE
        }
    }
}
