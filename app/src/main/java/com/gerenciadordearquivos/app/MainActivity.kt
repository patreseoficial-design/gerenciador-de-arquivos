package com.gerenciadordearquivos.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tela = TextView(this)

        tela.text = "TESTE DEFINITIVO\n\nMAIN ACTIVITY NOVA"
        tela.textSize = 26f
        tela.setTextColor(Color.WHITE)
        tela.setBackgroundColor(Color.BLACK)
        tela.gravity = Gravity.CENTER

        setContentView(tela)
    }
}
