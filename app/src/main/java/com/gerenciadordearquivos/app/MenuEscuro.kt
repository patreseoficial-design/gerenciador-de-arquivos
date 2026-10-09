package com.gerenciadordearquivos.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/*
 * =============================================================
 * MENU ESCURO
 * Visual único dos menus de ações (compartilhar, mover,
 * renomear, lixeira...) usado nas telas de arquivos,
 * imagens e vídeos.
 * =============================================================
 */

object MenuEscuro {

    val COR_FUNDO: Int =
        Color.rgb(24, 24, 26)

    val COR_TEXTO: Int =
        Color.WHITE

    val COR_PERIGO: Int =
        Color.rgb(255, 105, 97)

    // Ícone de cada ação, escolhido pelo texto da opção
    fun iconeDaAcao(
        texto: String
    ): Int? {

        val t =
            texto.lowercase()

        return when {

            t.contains("lixeira") ||
                t.contains("excluir") ->
                R.drawable.ic_acao_lixeira

            t.contains("desinstalar") ->
                R.drawable.ic_acao_desinstalar

            t.contains("restaurar") ->
                R.drawable.ic_acao_restaurar

            t.contains("compartilhar") ->
                R.drawable.ic_acao_compartilhar

            t.contains("abrir com") ->
                R.drawable.ic_acao_abrir_com

            t == "abrir" ->
                R.drawable.ic_acao_abrir

            t.contains("criar pasta") ->
                R.drawable.ic_acao_criar_pasta

            t.contains("cópia") ||
                t.contains("copiar") ->
                R.drawable.ic_acao_copiar

            t.contains("mover") ->
                R.drawable.ic_acao_mover

            t.contains("renomear") ->
                R.drawable.ic_acao_renomear

            t.contains("informaç") ->
                R.drawable.ic_acao_informacoes

            t == "mais" ->
                R.drawable.ic_acao_mais

            else -> null
        }
    }

    fun ehAcaoPerigosa(
        texto: String
    ): Boolean {

        val t =
            texto.lowercase()

        return t.contains("lixeira") ||
            t.contains("excluir") ||
            t.contains("desinstalar")
    }

    fun fundo(
        context: Context
    ): GradientDrawable {

        val fundo =
            GradientDrawable()

        fundo.setColor(
            COR_FUNDO
        )

        fundo.cornerRadius =
            dp(context, 16).toFloat()

        return fundo
    }

    fun prepararMenu(
        menu: LinearLayout
    ) {

        menu.orientation =
            LinearLayout.VERTICAL

        menu.background =
            fundo(menu.context)

        menu.setPadding(
            0,
            dp(menu.context, 8),
            0,
            dp(menu.context, 8)
        )
    }

    // Linha do menu: ícone + texto, branco sobre fundo escuro
    fun criarLinha(
        context: Context,
        iconeReserva: String,
        texto: String,
        acao: () -> Unit
    ): View {

        val cor =
            if (ehAcaoPerigosa(texto)) COR_PERIGO
            else COR_TEXTO

        val linha =
            LinearLayout(context)

        linha.orientation =
            LinearLayout.HORIZONTAL

        linha.gravity =
            Gravity.CENTER_VERTICAL

        linha.setPadding(
            dp(context, 20),
            0,
            dp(context, 20),
            0
        )

        linha.isClickable =
            true

        linha.isFocusable =
            true

        val fundoToque =
            android.util.TypedValue()

        if (
            context.theme.resolveAttribute(
                android.R.attr.selectableItemBackground,
                fundoToque,
                true
            )
        ) {
            linha.setBackgroundResource(
                fundoToque.resourceId
            )
        }

        val iconeRes =
            iconeDaAcao(texto)

        val icone: View =
            if (iconeRes != null) {

                ImageView(context).apply {
                    setImageResource(iconeRes)
                    setColorFilter(cor)
                    scaleType =
                        ImageView.ScaleType.FIT_CENTER
                }

            } else {

                TextView(context).apply {
                    text = iconeReserva
                    textSize = 20f
                    gravity = Gravity.CENTER
                    setTextColor(cor)
                }
            }

        linha.addView(
            icone,
            LinearLayout.LayoutParams(
                dp(context, 24),
                dp(context, 24)
            )
        )

        val nome =
            TextView(context)

        nome.text =
            texto

        nome.textSize =
            17f

        nome.typeface =
            Typeface.create(
                "sans-serif-medium",
                Typeface.NORMAL
            )

        nome.setTextColor(
            cor
        )

        nome.gravity =
            Gravity.CENTER_VERTICAL

        val nomeParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )

        nomeParams.marginStart =
            dp(context, 18)

        linha.addView(
            nome,
            nomeParams
        )

        linha.setOnClickListener {
            acao()
        }

        linha.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 54)
            )

        return linha
    }

    fun dp(
        context: Context,
        valor: Int
    ): Int =
        (valor * context.resources.displayMetrics.density).toInt()
}
