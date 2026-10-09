package com.gerenciadordearquivos.app

import android.content.Context
import android.content.res.Configuration

/*
 * =============================================================
 * TAMANHO DAS LETRAS
 * O app inteiro usa letras 15% maiores que o padrão do Android.
 * Com "Letras grandes" (menu ⋮) fica 50% maior.
 * Cada tela chama ModoSimples.contexto() em attachBaseContext.
 * =============================================================
 */

object ModoSimples {

    private const val PREFS = "preferencias"

    private const val CHAVE = "letras_grandes"

    // Aumento normal do app e aumento do modo "Letras grandes"
    private const val AUMENTO_PADRAO = 1.15f

    private const val AUMENTO_GRANDE = 1.5f

    fun ativo(
        context: Context
    ): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(CHAVE, false)

    fun definir(
        context: Context,
        ativo: Boolean
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(CHAVE, ativo)
            .apply()
    }

    fun contexto(
        base: Context
    ): Context {

        val config =
            Configuration(base.resources.configuration)

        // Idioma escolhido no app (datas, teclado numérico etc.)
        config.setLocale(I18n.locale())

        config.fontScale =
            config.fontScale *
                if (ativo(base)) AUMENTO_GRANDE else AUMENTO_PADRAO

        return base.createConfigurationContext(config)
    }
}
