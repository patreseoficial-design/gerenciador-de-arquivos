package com.gerenciadordearquivos.app

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import java.io.File
import java.util.Locale

class AudioViewerActivity : Activity() {

    // Mesmo tamanho de letra do resto do app
    override fun attachBaseContext(
        novoContexto: android.content.Context
    ) {
        super.attachBaseContext(
            ModoSimples.contexto(novoContexto)
        )
    }

    private var mediaPlayer: MediaPlayer? = null

    private lateinit var playPauseButton: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var durationText: TextView

    private val handler = Handler(Looper.getMainLooper())

    private val atualizarProgresso = object : Runnable {
        override fun run() {

            val player = mediaPlayer

            if (player != null) {
                try {
                    if (player.isPlaying) {

                        seekBar.progress = player.currentPosition

                        currentTimeText.text =
                            formatarTempo(player.currentPosition)

                    }
                } catch (_: Exception) {
                }
            }

            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(18, 18, 18)
        window.navigationBarColor = Color.rgb(18, 18, 18)

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

        criarTela(arquivo)

        iniciarAudio(arquivo)
    }

    private fun criarTela(arquivo: File) {

        val root = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            gravity = Gravity.CENTER_HORIZONTAL

            setPadding(
                dp(24),
                dp(18),
                dp(24),
                dp(24)
            )

            background = GradientDrawable().apply {
                setColor(Color.rgb(18, 18, 18))
            }
        }

        // ============================================================
        // TOPO
        // ============================================================

        val topBar = LinearLayout(this).apply {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        }

        val voltarButton = ImageButton(this).apply {

            setImageResource(
                android.R.drawable.ic_menu_revert
            )

            setColorFilter(Color.WHITE)

            background = criarFundoBotao(
                Color.rgb(40, 40, 40),
                18
            )

            contentDescription = tr("Voltar")

            setPadding(
                dp(14),
                dp(14),
                dp(14),
                dp(14)
            )

            setOnClickListener {
                finish()
            }
        }

        topBar.addView(
            voltarButton,
            LinearLayout.LayoutParams(
                dp(50),
                dp(50)
            )
        )

        val tituloTopo = TextView(this).apply {

            text = tr("Reproduzindo")

            textSize = 19f

            setTextColor(Color.WHITE)

            gravity = Gravity.CENTER

            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        topBar.addView(
            tituloTopo,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        val espacoTopo = View(this)

        topBar.addView(
            espacoTopo,
            LinearLayout.LayoutParams(
                dp(50),
                dp(50)
            )
        )

        root.addView(topBar)

        // ============================================================
        // ESPAÇO
        // ============================================================

        val espaco1 = SpaceView(this)

        root.addView(
            espaco1,
            LinearLayout.LayoutParams(
                1,
                dp(35)
            )
        )

        // ============================================================
        // ÍCONE DO ÁUDIO
        // ============================================================

        val iconeContainer = LinearLayout(this).apply {

            gravity = Gravity.CENTER

            background = GradientDrawable().apply {

                setColor(Color.rgb(35, 35, 35))

                cornerRadius = dp(28).toFloat()
            }
        }

        val iconeAudio = ImageView(this).apply {

            setImageResource(
                R.drawable.audios
            )

            scaleType = ImageView.ScaleType.CENTER_INSIDE

            setPadding(
                dp(25),
                dp(25),
                dp(25),
                dp(25)
            )
        }

        iconeContainer.addView(
            iconeAudio,
            LinearLayout.LayoutParams(
                dp(210),
                dp(210)
            )
        )

        root.addView(
            iconeContainer,
            LinearLayout.LayoutParams(
                dp(210),
                dp(210)
            )
        )

        // ============================================================
        // ESPAÇO
        // ============================================================

        root.addView(
            SpaceView(this),
            LinearLayout.LayoutParams(
                1,
                dp(32)
            )
        )

        // ============================================================
        // NOME DA MÚSICA
        // ============================================================

        val nomeMusica = TextView(this).apply {

            text = arquivo.nameWithoutExtension

            textSize = 21f

            setTextColor(Color.WHITE)

            gravity = Gravity.CENTER

            maxLines = 2

            ellipsize =
                android.text.TextUtils.TruncateAt.END

            typeface =
                android.graphics.Typeface.DEFAULT_BOLD
        }

        root.addView(
            nomeMusica,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // ============================================================
        // TIPO
        // ============================================================

        val tipoAudio = TextView(this).apply {

            text = tr("Áudio")

            textSize = 14f

            setTextColor(
                Color.rgb(160, 160, 160)
            )

            gravity = Gravity.CENTER

            setPadding(
                0,
                dp(7),
                0,
                0
            )
        }

        root.addView(
            tipoAudio,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // ============================================================
        // ESPAÇO
        // ============================================================

        root.addView(
            SpaceView(this),
            LinearLayout.LayoutParams(
                1,
                dp(28)
            )
        )

        // ============================================================
        // BARRA DE PROGRESSO
        // ============================================================

        seekBar = SeekBar(this).apply {

            max = 1000

            progress = 0

            setPadding(
                0,
                0,
                0,
                0
            )
        }

        root.addView(
            seekBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(40)
            )
        )

        // ============================================================
        // TEMPOS
        // ============================================================

        val tempos = LinearLayout(this).apply {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(25)
            )
        }

        currentTimeText = TextView(this).apply {

            text = "0:00"

            textSize = 13f

            setTextColor(
                Color.rgb(170, 170, 170)
            )
        }

        durationText = TextView(this).apply {

            text = "0:00"

            textSize = 13f

            setTextColor(
                Color.rgb(170, 170, 170)
            )

            gravity = Gravity.END
        }

        tempos.addView(
            currentTimeText,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        tempos.addView(
            durationText,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        root.addView(tempos)

        // ============================================================
        // ESPAÇO
        // ============================================================

        root.addView(
            SpaceView(this),
            LinearLayout.LayoutParams(
                1,
                dp(25)
            )
        )

        // ============================================================
        // BOTÃO PLAY / PAUSE
        // ============================================================

        playPauseButton = ImageButton(this).apply {

            setImageResource(
                android.R.drawable.ic_media_pause
            )

            setColorFilter(Color.WHITE)

            background = criarFundoBotao(
                Color.rgb(255, 255, 255),
                100
            )

            setPadding(
                dp(23),
                dp(23),
                dp(23),
                dp(23)
            )

            contentDescription = tr("Pausar")

            setOnClickListener {

                alternarReproducao()
            }
        }

        root.addView(
            playPauseButton,
            LinearLayout.LayoutParams(
                dp(78),
                dp(78)
            )
        )

        setContentView(root)

        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {

                    if (fromUser) {

                        mediaPlayer?.seekTo(progress)

                        currentTimeText.text =
                            formatarTempo(progress)
                    }
                }

                override fun onStartTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }

                override fun onStopTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }
            }
        )
    }

    private fun iniciarAudio(arquivo: File) {

        try {

            mediaPlayer = MediaPlayer.create(
                this,
                Uri.fromFile(arquivo)
            )

            val player = mediaPlayer

            if (player == null) {
                finish()
                return
            }

            val duracao = player.duration

            seekBar.max = duracao

            durationText.text =
                formatarTempo(duracao)

            player.setOnCompletionListener {

                seekBar.progress = 0

                currentTimeText.text = "0:00"

                playPauseButton.setImageResource(
                    android.R.drawable.ic_media_play
                )

                playPauseButton.setColorFilter(
                    Color.BLACK
                )
            }

            player.start()

            playPauseButton.setImageResource(
                android.R.drawable.ic_media_pause
            )

            playPauseButton.setColorFilter(
                Color.BLACK
            )

            handler.post(atualizarProgresso)

        } catch (e: Exception) {

            mediaPlayer?.release()

            mediaPlayer = null

            finish()
        }
    }

    private fun alternarReproducao() {

        val player = mediaPlayer ?: return

        try {

            if (player.isPlaying) {

                player.pause()

                playPauseButton.setImageResource(
                    android.R.drawable.ic_media_play
                )

            } else {

                player.start()

                playPauseButton.setImageResource(
                    android.R.drawable.ic_media_pause
                )
            }

            playPauseButton.setColorFilter(
                Color.BLACK
            )

        } catch (_: Exception) {
        }
    }

    private fun criarFundoBotao(
        cor: Int,
        raio: Int
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(cor)

            cornerRadius =
                dp(raio).toFloat()
        }
    }

    private fun formatarTempo(
        milissegundos: Int
    ): String {

        val segundos =
            milissegundos / 1000

        val minutos =
            segundos / 60

        val segundosRestantes =
            segundos % 60

        return String.format(
            Locale.getDefault(),
            "%d:%02d",
            minutos,
            segundosRestantes
        )
    }

    private fun dp(valor: Int): Int {

        return (
            valor *
                resources.displayMetrics.density
        ).toInt()
    }

    override fun onPause() {

        super.onPause()

        mediaPlayer?.pause()

        if (::playPauseButton.isInitialized) {

            playPauseButton.setImageResource(
                android.R.drawable.ic_media_play
            )

            playPauseButton.setColorFilter(
                Color.BLACK
            )
        }
    }

    override fun onDestroy() {

        handler.removeCallbacks(
            atualizarProgresso
        )

        try {

            mediaPlayer?.stop()

        } catch (_: Exception) {
        }

        mediaPlayer?.release()

        mediaPlayer = null

        super.onDestroy()
    }

    private class SpaceView(
        context: android.content.Context
    ) : View(context)
}
