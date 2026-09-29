package com.gerenciadordearquivos.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val texto = TextView(this)
        texto.text = "TESTE - Gerenciador de Arquivos"
        texto.textSize = 24f

        setContentView(texto)
    }
}
