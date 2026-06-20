package com.iptv.autocombine.ui.mobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.iptv.autocombine.R
import com.iptv.autocombine.model.Channel
import com.iptv.autocombine.model.ChannelGroup

/**
 * RecyclerView adapter for displaying channels organized in expandable groups.
 *
 * Supports two item types:
 * - GROUP_HEADER: Expandable group header with country flag and channel count
 * - CHANNEL_ITEM: Individual channel card with logo, name, group, and favorite button
 *
 * @param onChannelClick Called when a channel card is clicked (to play)
 * @param onFavoriteClick Called when the favorite button is toggled
 * @param onGroupClick Called when a group header is clicked (to expand/collapse)
 * @param isFavorite Function to check if a channel is a favorite
 */
class ChannelAdapter(
    private val onChannelClick: (Channel) -> Unit,
    private val onFavoriteClick: (Channel) -> Unit,
    private val onGroupClick: (Int) -> Unit,
    private val isFavorite: (Channel) -> Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_GROUP_HEADER = 0
        private const val TYPE_CHANNEL_ITEM = 1
    }

    /** Sealed class representing items in the flat list */
    private sealed class ListItem {
        data class GroupHeader(val group: ChannelGroup, val groupIndex: Int) : ListItem()
        data class ChannelEntry(val channel: Channel) : ListItem()
    }

    private var items: List<ListItem> = emptyList()
    private var groups: List<ChannelGroup> = emptyList()

    /**
     * Submits a new list of channel groups. Flattens the groups into a mixed list
     * of headers and channel items based on expansion state.
     *
     * Uses DiffUtil for efficient updates.
     */
    fun submitGroups(newGroups: List<ChannelGroup>) {
        val oldItems = items
        groups = newGroups
        items = flattenGroups(newGroups)

        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = oldItems.size
            override fun getNewListSize() = items.size

            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                val old = oldItems[oldPos]
                val new = items[newPos]
                return when {
                    old is ListItem.GroupHeader && new is ListItem.GroupHeader ->
                        old.group.name == new.group.name
                    old is ListItem.ChannelEntry && new is ListItem.ChannelEntry ->
                        old.channel.id == new.channel.id
                    else -> false
                }
            }

            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                val old = oldItems[oldPos]
                val new = items[newPos]
                return when {
                    old is ListItem.GroupHeader && new is ListItem.GroupHeader ->
                        old.group.name == new.group.name &&
                            old.group.isExpanded == new.group.isExpanded &&
                            old.group.channels.size == new.group.channels.size
                    old is ListItem.ChannelEntry && new is ListItem.ChannelEntry ->
                        old.channel == new.channel
                    else -> false
                }
            }
        })

        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * Submits a flat list of channels (no groups). Used for favorites and search results.
     */
    fun submitChannels(channels: List<Channel>) {
        val oldItems = items
        items = channels.map { ListItem.ChannelEntry(it) }

        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = oldItems.size
            override fun getNewListSize() = items.size

            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                val old = oldItems[oldPos]
                val new = items[newPos]
                return old is ListItem.ChannelEntry && new is ListItem.ChannelEntry &&
                    old.channel.id == new.channel.id
            }

            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                return oldItems[oldPos] == items[newPos]
            }
        })

        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * Flattens the groups into a list with headers and visible channels.
     */
    private fun flattenGroups(groups: List<ChannelGroup>): List<ListItem> {
        val result = mutableListOf<ListItem>()
        groups.forEachIndexed { index, group ->
            result.add(ListItem.GroupHeader(group, index))
            if (group.isExpanded) {
                group.channels.forEach { channel ->
                    result.add(ListItem.ChannelEntry(channel))
                }
            }
        }
        return result
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.GroupHeader -> TYPE_GROUP_HEADER
            is ListItem.ChannelEntry -> TYPE_CHANNEL_ITEM
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_GROUP_HEADER -> {
                val view = inflater.inflate(R.layout.item_group_header, parent, false)
                GroupHeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_channel, parent, false)
                ChannelViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.GroupHeader -> (holder as GroupHeaderViewHolder).bind(item)
            is ListItem.ChannelEntry -> (holder as ChannelViewHolder).bind(item.channel)
        }
    }

    /**
     * ViewHolder for group header items.
     */
    inner class GroupHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val groupName: TextView = itemView.findViewById(R.id.group_name)
        private val channelCount: TextView = itemView.findViewById(R.id.group_channel_count)
        private val expandArrow: ImageView = itemView.findViewById(R.id.expand_arrow)
        private val headerLayout: LinearLayout = itemView.findViewById(R.id.group_header_layout)

        fun bind(item: ListItem.GroupHeader) {
            groupName.text = item.group.name
            channelCount.text = "${item.group.channels.size} ch"

            // Rotate arrow based on expansion state
            expandArrow.rotation = if (item.group.isExpanded) 0f else -90f

            headerLayout.setOnClickListener {
                onGroupClick(item.groupIndex)
            }
        }
    }

    /**
     * ViewHolder for channel card items.
     */
    inner class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val channelLogo: ImageView = itemView.findViewById(R.id.channel_logo)
        private val channelName: TextView = itemView.findViewById(R.id.channel_name)
        private val channelGroup: TextView = itemView.findViewById(R.id.channel_group)
        private val favoriteButton: ImageButton = itemView.findViewById(R.id.favorite_button)
        private val cardView: com.google.android.material.card.MaterialCardView =
            itemView.findViewById(R.id.channel_card)

        fun bind(channel: Channel) {
            channelName.text = channel.displayName
            channelGroup.text = channel.displayGroup

            // Load logo with Coil
            val logoUrl = channel.effectiveLogoUrl
            if (logoUrl.isNotBlank()) {
                channelLogo.load(logoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.channel_placeholder)
                    error(R.drawable.channel_placeholder)
                    transformations(RoundedCornersTransformation(8f))
                }
            } else {
                channelLogo.setImageResource(R.drawable.channel_placeholder)
            }

            // Favorite state
            val isFav = isFavorite(channel)
            favoriteButton.setImageResource(
                if (isFav) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )

            // Click listeners
            cardView.setOnClickListener {
                onChannelClick(channel)
            }

            favoriteButton.setOnClickListener {
                onFavoriteClick(channel)
            }
        }
    }
}
