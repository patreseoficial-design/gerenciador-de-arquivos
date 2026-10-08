package com.gerenciadordearquivos.app

import android.app.Activity
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File

class AudioViewerActivity : Activity() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var playPauseButton: Button
    private lateinit var titleText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val caminho = intent.getStringExtra("filePath")

        if (caminho.isNullOrEmpty()) {
            finish()
            return
        }

        val arquivo = File(caminho)

        if (!arquivo.exists()) {
            finish()
            return
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }

        titleText = TextView(this).apply {
            text = arquivo.name
            textSize = 20f
            gravity = Gravity.CENTER
        }

        val audioIcon = TextView(this).apply {
            text = "🎧"
            textSize = 70f
            gravity = Gravity.CENTER
        }

        playPauseButton = Button(this).apply {
            text = "⏸ Pausar"
            textSize = 18f
        }

        val voltarButton = Button(this).apply {
            text = "← Voltar"
        }

        layout.addView(
            titleText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        layout.addView(
            audioIcon,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                180
            )
        )

        layout.addView(
            playPauseButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        layout.addView(
            voltarButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(layout)

        playPauseButton.setOnClickListener {
            val player = mediaPlayer ?: return@setOnClickListener

            if (player.isPlaying) {
                player.pause()
                playPauseButton.text = "▶ Reproduzir"
            } else {
                player.start()
                playPauseButton.text = "⏸ Pausar"
            }
        }

        voltarButton.setOnClickListener {
            finish()
        }

        iniciarAudio(arquivo)
    }

    private fun iniciarAudio(arquivo: File) {
        try {
            mediaPlayer = MediaPlayer.create(
                this,
                Uri.fromFile(arquivo)
            )

            mediaPlayer?.setOnCompletionListener {
                playPauseButton.text = "▶ Reproduzir"
            }

            mediaPlayer?.start()

            playPauseButton.text = "⏸ Pausar"

        } catch (e: Exception) {
            mediaPlayer?.release()
            mediaPlayer = null
            finish()
        }
    }

    override fun onPause() {
        super.onPause()

        mediaPlayer?.pause()

        if (::playPauseButton.isInitialized) {
            playPauseButton.text = "▶ Reproduzir"
        }
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        super.onDestroy()
    }
}
