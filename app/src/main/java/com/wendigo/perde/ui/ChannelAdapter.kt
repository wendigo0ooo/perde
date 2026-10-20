package com.wendigo.perde.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wendigo.perde.R
import com.wendigo.perde.data.Channel
import com.wendigo.perde.data.Favorites

class ChannelAdapter(
    private val favorites: Favorites,
    private val onPlay: (Channel) -> Unit,
    private val onFavoriteChanged: () -> Unit
) : RecyclerView.Adapter<ChannelAdapter.Holder>() {

    private val items = ArrayList<Channel>()

    fun submit(list: List<Channel>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.rowName)
        private val meta: TextView = view.findViewById(R.id.rowMeta)
        private val star: ImageButton = view.findViewById(R.id.rowFavorite)

        fun bind(channel: Channel) {
            name.text = channel.name

            val parts = ArrayList<String>(2)
            channel.group?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
            parts.add(channel.source)
            meta.text = parts.joinToString(" · ")

            val favorite = favorites.isFavorite(channel.url)
            star.setImageResource(if (favorite) R.drawable.ic_star else R.drawable.ic_star_outline)
            star.contentDescription = itemView.context.getString(
                if (favorite) R.string.unfavorite else R.string.favorite
            )

            itemView.setOnClickListener { onPlay(channel) }
            star.setOnClickListener {
                favorites.toggle(channel.url)
                onFavoriteChanged()
            }
        }
    }
}
