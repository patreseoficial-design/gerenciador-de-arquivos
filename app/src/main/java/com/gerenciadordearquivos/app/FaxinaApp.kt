package com.gerenciadordearquivos.app

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.annotation.RequiresApi

/*
 * Inicia o idioma antes de qualquer tela abrir (também vale para
 * o aviso de celular cheio e o widget, que rodam sem tela).
 */
class FaxinaApp : Application() {

    override fun onCreate() {

        super.onCreate()

        I18n.iniciar(this)

        if (Build.VERSION.SDK_INT >= 36) {
            manterTelasLongeDasBarras()
        }
    }

    // No Android 16+ a tela sempre ocupa também o espaço da barra de
    // status e da barra de navegação. Afasta o conteúdo delas, como
    // nas versões anteriores (o fundo azul do tema aparece atrás das
    // barras). Os visualizadores de foto e vídeo cuidam disso sozinhos.
    @RequiresApi(36)
    private fun manterTelasLongeDasBarras() {

        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {

                override fun onActivityPostCreated(
                    activity: Activity,
                    savedInstanceState: Bundle?
                ) {

                    if (
                        activity is ImageViewerActivity ||
                        activity is VideoViewerActivity
                    ) return

                    val conteudo =
                        activity.findViewById<View>(android.R.id.content)
                            ?: return

                    conteudo.setOnApplyWindowInsetsListener { _, insets ->
                        val barras =
                            insets.getInsets(
                                WindowInsets.Type.systemBars() or
                                    WindowInsets.Type.displayCutout()
                            )
                        // O teclado também empurra a tela para cima
                        val teclado =
                            insets.getInsets(WindowInsets.Type.ime()).bottom
                        conteudo.setPadding(
                            barras.left,
                            barras.top,
                            barras.right,
                            maxOf(barras.bottom, teclado)
                        )
                        insets
                    }

                    conteudo.requestApplyInsets()
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityResumed(activity: Activity) {}
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            }
        )
    }
}
