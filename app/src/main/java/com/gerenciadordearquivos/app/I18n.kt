package com.gerenciadordearquivos.app

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import org.json.JSONObject
import java.util.Locale

/*
 * =============================================================
 * IDIOMAS
 *
 * Os textos do app são escritos em português no código e passam
 * por tr("texto"). Para os outros idiomas, a tradução vem de
 * assets/i18n/<idioma>.json ({"texto em português": "tradução"}).
 *
 * Partes que mudam usam {0}, {1}...:
 *   tr("{0} arquivo(s)", 3)  ->  "3 files" em inglês
 *
 * Para adicionar um idioma: crie assets/i18n/<código>.json e
 * coloque o código e o nome na lista IDIOMAS.
 * =============================================================
 */

object I18n {

    // Código -> nome no próprio idioma (aparece no seletor)
    val IDIOMAS =
        linkedMapOf(
            "pt" to "Português",
            "en" to "English",
            "es" to "Español",
            "fr" to "Français",
            "de" to "Deutsch",
            "it" to "Italiano",
            "id" to "Bahasa Indonesia"
        )

    const val AUTOMATICO = "auto"

    private const val PREFS = "preferencias"

    private const val CHAVE = "idioma"

    @Volatile
    var idioma: String = "pt"
        private set

    private var traducoes: Map<String, String> = emptyMap()

    // tradução -> texto original em português (para achar ícones)
    private var originais: Map<String, String> = emptyMap()

    private var appContext: Context? = null

    // Chamado pelo FaxinaApp ao abrir o app
    fun iniciar(context: Context) {

        appContext = context.applicationContext

        carregar(idiomaEscolhido(context))
    }

    // Escolha salva ("auto" = idioma do celular)
    fun escolha(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CHAVE, AUTOMATICO) ?: AUTOMATICO

    fun definir(context: Context, codigo: String) {

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CHAVE, codigo)
            .apply()

        carregar(idiomaEscolhido(context))
    }

    private fun idiomaEscolhido(context: Context): String {

        val escolhido = escolha(context)

        if (escolhido != AUTOMATICO && IDIOMAS.containsKey(escolhido)) {
            return escolhido
        }

        // Automático: idioma do celular; se não tivermos, inglês
        val doCelular = Locale.getDefault().language

        return if (IDIOMAS.containsKey(doCelular)) doCelular else "en"
    }

    private fun carregar(codigo: String) {

        idioma = codigo

        if (codigo == "pt") {
            traducoes = emptyMap()
            originais = emptyMap()
            return
        }

        val context = appContext ?: return

        val mapa = HashMap<String, String>()

        try {

            val json =
                context.assets.open("i18n/$codigo.json")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }

            val objeto = JSONObject(json)

            for (chave in objeto.keys()) {
                mapa[chave] = objeto.getString(chave)
            }

        } catch (_: Exception) {
        }

        traducoes = mapa

        originais = mapa.entries.associate { (pt, traduzido) -> traduzido to pt }
    }

    fun traduzir(texto: String, args: Array<out Any?>): String {

        var resultado = traducoes[texto] ?: texto

        args.forEachIndexed { i, valor ->
            resultado = resultado.replace("{$i}", valor.toString())
        }

        return resultado
    }

    // Texto original em português de uma tradução
    fun original(texto: String): String =
        originais[texto] ?: texto

    // Traduz os textos de uma tela montada pelo XML
    fun traduzirTela(view: View) {

        if (idioma == "pt") return

        when (view) {

            is ViewGroup ->
                for (i in 0 until view.childCount) {
                    traduzirTela(view.getChildAt(i))
                }

            is EditText ->
                view.hint?.let { view.hint = tr(it.toString()) }

            is TextView ->
                view.text?.let { view.text = tr(it.toString()) }
        }

        view.contentDescription?.let {
            view.contentDescription = tr(it.toString())
        }
    }

    // Locale usado nas datas e números
    fun locale(): Locale =
        Locale.forLanguageTag(idioma)
}

// Atalho usado em todo o app
fun tr(texto: String, vararg args: Any?): String =
    I18n.traduzir(texto, args)
