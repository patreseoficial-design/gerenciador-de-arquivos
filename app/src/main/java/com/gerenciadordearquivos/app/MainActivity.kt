package com.gerenciadordearquivos.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_main)
        } catch (e: Exception) {
            val erro = TextView(this)
            erro.text = "ERRO AO ABRIR O APLICATIVO:\n\n${e.javaClass.name}\n\n${e.message}"
            erro.textSize = 16f
            erro.setPadding(30, 30, 30, 30)
            setContentView(erro)
        }
    }
}
