package com.iptv.autocombine.ui.mobile

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.iptv.autocombine.R
import com.iptv.autocombine.databinding.ActivityMainBinding
import com.iptv.autocombine.model.Channel
import com.iptv.autocombine.viewmodel.MainViewModel

/**
 * Main activity for mobile and tablet devices.
 *
 * On phones: Shows a single-pane layout with bottom navigation switching between
 * channel list, favorites, and settings fragments.
 *
 * On tablets (sw600dp+): Shows a two-pane layout with the channel list on the left
 * and an embedded video player on the right.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    /** ExoPlayer instance for tablet two-pane mode */
    private var tabletPlayer: ExoPlayer? = null

    /** Whether we're in tablet two-pane mode */
    private val isTabletMode: Boolean
        get() = binding.root.findViewById<View>(R.id.tablet_player_container) != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()

        // Show channel list fragment by default
        if (savedInstanceState == null) {
            showFragment(ChannelListFragment())
        }
    }

    /**
     * Sets up bottom navigation with fragment switching.
     */
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_channels -> {
                    showFragment(ChannelListFragment())
                    true
                }
                R.id.nav_favorites -> {
                    showFragment(FavoritesFragment())
                    true
                }
                R.id.nav_settings -> {
                    showFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Replaces the fragment container with the given fragment.
     */
    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    /**
     * Plays a channel. On tablet, plays in the embedded player.
     * On phone, launches the full-screen PlayerActivity.
     *
     * @param channel The channel to play
     */
    fun playChannel(channel: Channel) {
        viewModel.setCurrentChannel(channel)

        if (isTabletMode) {
            playInTabletMode(channel)
        } else {
            // Launch full-screen player on phones
            val intent = Intent(this, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_CHANNEL_NAME, channel.displayName)
                putExtra(PlayerActivity.EXTRA_CHANNEL_URL, channel.url)
                putExtra(PlayerActivity.EXTRA_CHANNEL_GROUP, channel.displayGroup)
                putExtra(PlayerActivity.EXTRA_CHANNEL_LOGO, channel.effectiveLogoUrl)
            }
            startActivity(intent)
        }
    }

    /**
     * Plays a channel in the tablet embedded player.
     */
    private fun playInTabletMode(channel: Channel) {
        val playerContainer = binding.root.findViewById<View>(R.id.tablet_player_container)
        playerContainer?.visibility = View.VISIBLE

        val channelNameView = binding.root.findViewById<android.widget.TextView>(R.id.tablet_channel_name)
        val channelGroupView = binding.root.findViewById<android.widget.TextView>(R.id.tablet_channel_group)
        channelNameView?.text = channel.displayName
        channelGroupView?.text = channel.displayGroup

        val playerView = binding.root.findViewById<androidx.media3.ui.PlayerView>(R.id.tablet_player_view)

        // Release existing player
        tabletPlayer?.release()

        // Create new player
        tabletPlayer = ExoPlayer.Builder(this).build().also { player ->
            playerView?.player = player

            val mediaItem = MediaItem.fromUri(channel.url)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true

            player.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    // Show error in the player area
                    channelNameView?.text = getString(R.string.player_error)
                }
            })
        }
    }

    override fun onPause() {
        super.onPause()
        tabletPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        tabletPlayer?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        tabletPlayer?.release()
        tabletPlayer = null
    }
}
