
package com.gerenciadordearquivos.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val texto = TextView(this)
        texto.text = "Gerenciador de Arquivos"
        texto.textSize = 24f
        texto.setPadding(32, 32, 32, 32)

        setContentView(texto)
    }
}
