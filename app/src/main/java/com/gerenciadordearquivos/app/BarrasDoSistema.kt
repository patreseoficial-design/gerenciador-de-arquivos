package com.gerenciadordearquivos.app

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View

/*
 * =============================================================
 * BARRAS DO SISTEMA
 * Nos visualizadores a barra de status fica escondida, mas a
 * barra de navegação do celular (botões ou gestos) continua
 * visível. As barras do app são afastadas dela, para os botões
 * do celular não ficarem por cima dos nossos.
 * =============================================================
 */

object BarrasDoSistema {

    @Suppress("DEPRECATION")
    fun configurarTelaCheia(
        activity: Activity
    ) {

        activity.window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        activity.window.navigationBarColor =
            Color.BLACK

        activity.window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    // Chama "aplicar" com o espaço ocupado pelo sistema em cada
    // lado da tela (esquerda, topo, direita, baixo), em pixels
    @Suppress("DEPRECATION")
    fun aoMudarEspacos(
        raiz: View,
        aplicar: (Int, Int, Int, Int) -> Unit
    ) {

        raiz.setOnApplyWindowInsetsListener {
                _,
                insets ->

            val topo =
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.P
                ) {
                    insets.displayCutout
                        ?.safeInsetTop
                        ?: 0
                } else {
                    0
                }

            aplicar(
                insets.systemWindowInsetLeft,
                topo,
                insets.systemWindowInsetRight,
                insets.systemWindowInsetBottom
            )

            insets
        }

        raiz.requestApplyInsets()
    }
}
