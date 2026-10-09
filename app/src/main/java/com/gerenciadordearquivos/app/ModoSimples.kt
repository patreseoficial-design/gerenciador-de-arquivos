package com.gerenciadordearquivos.app

import android.content.Context
import android.content.res.Configuration

/*
 * =============================================================
 * LETRAS GRANDES
 * Aumenta todos os textos do app (bom para quem enxerga pouco).
 * Cada tela chama ModoSimples.contexto() em attachBaseContext.
 * =============================================================
 */

object ModoSimples {

    private const val PREFS = "preferencias"

    private const val CHAVE = "letras_grandes"

    private const val AUMENTO = 1.3f

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

        if (!ativo(base)) {
            return base
        }

        val config =
            Configuration(base.resources.configuration)

        config.fontScale =
            config.fontScale * AUMENTO

        return base.createConfigurationContext(config)
    }
}
