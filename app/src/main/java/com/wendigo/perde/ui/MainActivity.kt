package com.wendigo.perde.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wendigo.perde.R
import com.wendigo.perde.data.Channel
import com.wendigo.perde.data.ChannelStore
import com.wendigo.perde.data.Favorites
import com.wendigo.perde.data.M3uParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var store: ChannelStore
    private lateinit var favorites: Favorites
    private lateinit var adapter: ChannelAdapter

    private lateinit var etSearch: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var tvChannelsEmpty: TextView
    private lateinit var tvChannelCount: TextView
    private lateinit var tvSourceInfo: TextView
    private lateinit var tvVersion: TextView
    private lateinit var tvAboutVersion: TextView
    private lateinit var btnFavorites: Button
    private lateinit var btnAddUrl: Button
    private lateinit var btnAddFile: Button
    private lateinit var btnClear: Button

    private lateinit var pages: List<View>
    private lateinit var navButtons: List<Button>

    private var channels: List<Channel> = emptyList()
    private var favoritesOnly = false

    private val openDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) importFromUri(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = ChannelStore(this)
        favorites = Favorites(this)

        etSearch = findViewById(R.id.etSearch)
        recycler = findViewById(R.id.recyclerChannels)
        tvChannelsEmpty = findViewById(R.id.tvChannelsEmpty)
        tvChannelCount = findViewById(R.id.tvChannelCount)
        tvSourceInfo = findViewById(R.id.tvSourceInfo)
        tvVersion = findViewById(R.id.tvVersion)
        tvAboutVersion = findViewById(R.id.tvAboutVersion)
        btnFavorites = findViewById(R.id.btnFavorites)
        btnAddUrl = findViewById(R.id.btnAddUrl)
        btnAddFile = findViewById(R.id.btnAddFile)
        btnClear = findViewById(R.id.btnClear)

        pages = listOf(
            findViewById(R.id.pageChannels),
            findViewById(R.id.pageSources),
            findViewById(R.id.pageGuide),
            findViewById(R.id.pageSettings)
        )
        navButtons = listOf(
            findViewById(R.id.navChannels),
            findViewById(R.id.navSources),
            findViewById(R.id.navGuide),
            findViewById(R.id.navSettings)
        )

        adapter = ChannelAdapter(
            favorites = favorites,
            onPlay = { play(it) },
            onFavoriteChanged = { refreshChannels() }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        navButtons.forEachIndexed { index, button ->
            button.setOnClickListener { selectPage(index) }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                refreshChannels()
            }
        })

        btnFavorites.setOnClickListener {
            favoritesOnly = !favoritesOnly
            btnFavorites.text = getString(
                if (favoritesOnly) R.string.all_channels else R.string.favorites
            )
            refreshChannels()
        }

        btnAddUrl.setOnClickListener { showUrlDialog() }
        btnAddFile.setOnClickListener { openDocument.launch(arrayOf("*/*")) }
        btnClear.setOnClickListener { confirmClear() }

        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (_: Throwable) {
            ""
        }
        val versionText = getString(R.string.settings_version, version)
        tvVersion.text = versionText
        tvAboutVersion.text = versionText

        selectPage(0)
        refreshChannels()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (pages[0].visibility != View.VISIBLE) {
            selectPage(0)
            return
        }
        super.onBackPressed()
    }

    private fun selectPage(index: Int) {
        pages.forEachIndexed { i, page ->
            page.visibility = if (i == index) View.VISIBLE else View.GONE
        }
        navButtons.forEachIndexed { i, button ->
            button.setTextColor(
                getColor(if (i == index) R.color.perde_gold else R.color.perde_text_dim)
            )
            button.setBackgroundResource(
                if (i == index) R.drawable.nav_selected else R.drawable.item_bg
            )
        }
        if (index == 1) refreshSourceInfo()
        if (index == 0) {
            recycler.post {
                recycler.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
            }
        }
    }

    private fun refreshChannels() {
        channels = store.load()
        val query = etSearch.text.toString().trim().lowercase()

        val filtered = channels.asSequence()
            .filter { channel ->
                query.isEmpty() ||
                    channel.name.lowercase().contains(query) ||
                    (channel.group ?: "").lowercase().contains(query) ||
                    channel.source.lowercase().contains(query)
            }
            .filter { channel -> !favoritesOnly || favorites.isFavorite(channel.url) }
            .sortedWith(
                compareByDescending<Channel> { favorites.isFavorite(it.url) }
                    .thenBy { it.name.lowercase() }
            )
            .toList()

        adapter.submit(filtered)
        tvChannelsEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        tvChannelsEmpty.text = getString(
            if (channels.isEmpty()) R.string.channels_empty else R.string.no_results
        )

        val sourceCount = channels.map { it.source }.distinct().size
        tvChannelCount.text = getString(R.string.channel_count, channels.size, sourceCount)
        refreshSourceInfo()
    }

    private fun refreshSourceInfo() {
        if (channels.isEmpty()) {
            tvSourceInfo.text = getString(R.string.source_none)
            return
        }
        val sources = channels.map { it.source }.distinct()
        val summary = getString(R.string.source_summary, channels.size, sources.size)
        tvSourceInfo.text = summary + "\n\n" + sources.joinToString("\n• ", prefix = "• ")
    }

    private fun play(channel: Channel) {
        val intent = Intent(this, PlayerActivity::class.java)
            .putExtra(PlayerActivity.EXTRA_URL, channel.url)
            .putExtra(PlayerActivity.EXTRA_NAME, channel.name)
        startActivity(intent)
    }

    private fun confirmClear() {
        AlertDialog.Builder(this)
            .setMessage(R.string.clear_confirm)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.clear_all) { _, _ ->
                store.clear()
                refreshChannels()
                Toast.makeText(this, R.string.source_none, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showUrlDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_source, null)
        val nameInput = view.findViewById<EditText>(R.id.etSourceName)
        val urlInput = view.findViewById<EditText>(R.id.etSourceUrl)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_add, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val url = urlInput.text.toString().trim()
                if (url.isEmpty()) {
                    urlInput.error = getString(R.string.source_name_required)
                    return@setOnClickListener
                }
                if (!url.startsWith("https://", ignoreCase = true)) {
                    urlInput.error = getString(R.string.source_https_only)
                    return@setOnClickListener
                }
                val name = nameInput.text.toString().trim().ifEmpty { hostOf(url) }
                dialog.dismiss()
                importFromUrl(name, url)
            }
        }
        dialog.show()
    }

    private fun importFromUrl(name: String, url: String) {
        setBusy(true)
        thread {
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Perde/0.1")
                }
                val result = try {
                    val code = connection.responseCode
                    if (code !in 200..299) throw IOException("HTTP $code")
                    connection.inputStream.use { input ->
                        M3uParser.parse(ByteArrayInputStream(readLimited(input, MAX_BYTES)))
                    }
                } finally {
                    connection.disconnect()
                }
                finishImport(name, result)
            } catch (t: Throwable) {
                failImport(t)
            }
        }
    }

    private fun importFromUri(uri: Uri) {
        setBusy(true)
        thread {
            try {
                val result = contentResolver.openInputStream(uri)?.use { input ->
                    M3uParser.parse(input)
                } ?: throw IOException("Dosya açılamadı")
                val name = uri.lastPathSegment
                    ?.substringAfterLast(':')
                    ?.substringAfterLast('/')
                    ?.takeIf { it.isNotBlank() }
                    ?: "Dosya"
                finishImport(name, result)
            } catch (t: Throwable) {
                failImport(t)
            }
        }
    }

    private fun finishImport(name: String, result: M3uParser.Result) {
        val imported = store.addSource(name, result.entries, result.skipped)
        runOnUiThread {
            setBusy(false)
            refreshChannels()
            val message = if (imported.added == 0) {
                getString(R.string.import_empty)
            } else {
                getString(R.string.import_done, imported.added, imported.skipped)
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            if (imported.added > 0) selectPage(0)
        }
    }

    private fun failImport(t: Throwable) {
        runOnUiThread {
            setBusy(false)
            val reason = t.message ?: t.javaClass.simpleName
            Toast.makeText(
                this,
                getString(R.string.import_failed, reason),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setBusy(busy: Boolean) {
        btnAddUrl.isEnabled = !busy
        btnAddFile.isEnabled = !busy
        btnClear.isEnabled = !busy
        if (busy) Toast.makeText(this, R.string.importing, Toast.LENGTH_SHORT).show()
    }

    private fun hostOf(url: String): String {
        return try {
            URL(url).host?.takeIf { it.isNotBlank() } ?: "Kaynak"
        } catch (_: Throwable) {
            "Kaynak"
        }
    }

    private fun readLimited(input: InputStream, limit: Long): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) throw IOException("Liste 64 MB sınırını aşıyor")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private companion object {
        const val MAX_BYTES = 64L * 1024 * 1024
    }
}
