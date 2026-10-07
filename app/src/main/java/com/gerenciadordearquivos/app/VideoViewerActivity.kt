package com.gerenciadordearquivos.app

import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class VideoViewerActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        val caminho =
            intent.getStringExtra("arquivo")

        if (caminho.isNullOrEmpty()) {

            Toast.makeText(
                this,
                "Vídeo não encontrado",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        val arquivo =
            File(caminho)

        if (
            !arquivo.exists() ||
            !arquivo.isFile
        ) {

            Toast.makeText(
                this,
                "Vídeo não encontrado",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        val layout =
            FrameLayout(this)

        layout.setBackgroundColor(
            Color.BLACK
        )

        videoView =
            VideoView(this)

        val parametros =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

        parametros.gravity =
            Gravity.CENTER

        layout.addView(
            videoView,
            parametros
        )

        setContentView(layout)

        val controlador =
            MediaController(this)

        controlador.setAnchorView(
            videoView
        )

        videoView.setMediaController(
            controlador
        )

        try {

            val uri =
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    arquivo
                )

            videoView.setVideoURI(uri)

            videoView.setOnPreparedListener {
                videoView.start()
            }

            videoView.setOnCompletionListener {
                controlador.show()
            }

            videoView.setOnErrorListener {
                    _,
                    _,
                    _ ->

                Toast.makeText(
                    this,
                    "Este formato ou codec de vídeo não é compatível",
                    Toast.LENGTH_LONG
                ).show()

                false
            }

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this,
                "Não foi possível reproduzir o vídeo",
                Toast.LENGTH_LONG
            ).show()

            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        if (
            ::videoView.isInitialized
        ) {

            videoView.start()
        }
    }

    override fun onPause() {
        super.onPause()

        if (
            ::videoView.isInitialized
        ) {

            videoView.pause()
        }
    }
}
