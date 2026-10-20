package com.wendigo.perde.ui

import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.wendigo.perde.R

@OptIn(UnstableApi::class)
class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var titleView: TextView

    private val listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Toast.makeText(
                this@PlayerActivity,
                getString(R.string.player_error, error.errorCodeName),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playerView = findViewById(R.id.playerView)
        titleView = findViewById(R.id.playerTitle)
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        val url = intent.getStringExtra(EXTRA_URL)
        val name = intent.getStringExtra(EXTRA_NAME) ?: getString(R.string.app_name)
        titleView.text = name

        if (url.isNullOrBlank()) {
            Toast.makeText(this, R.string.player_missing_url, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val exoPlayer = ExoPlayer.Builder(this).build()
        player = exoPlayer
        playerView.player = exoPlayer
        exoPlayer.addListener(listener)
        exoPlayer.setMediaItem(
            MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(name)
                        .build()
                )
                .build()
        )
        exoPlayer.playWhenReady = true
        exoPlayer.prepare()
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
        playerView.player = null
    }

    companion object {
        const val EXTRA_URL = "com.wendigo.perde.EXTRA_URL"
        const val EXTRA_NAME = "com.wendigo.perde.EXTRA_NAME"
    }
}
