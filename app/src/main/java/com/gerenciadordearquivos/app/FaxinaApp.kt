package com.gerenciadordearquivos.app

import android.app.Application

/*
 * Inicia o idioma antes de qualquer tela abrir (também vale para
 * o aviso de celular cheio e o widget, que rodam sem tela).
 */
class FaxinaApp : Application() {

    override fun onCreate() {

        super.onCreate()

        I18n.iniciar(this)
    }
}
