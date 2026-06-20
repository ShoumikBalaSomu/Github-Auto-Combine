package com.iptv.autocombine.ui.mobile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.iptv.autocombine.R
import com.iptv.autocombine.model.PlaylistState
import com.iptv.autocombine.viewmodel.MainViewModel

/**
 * Fragment displaying the list of channels organized by country/group.
 *
 * Features:
 * - Search bar with real-time filtering
 * - Expandable country/group sections
 * - Pull-to-refresh
 * - Loading, error, and empty states
 * - Smooth animations
 */
class ChannelListFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: ChannelAdapter

    // Views
    private lateinit var searchEditText: EditText
    private lateinit var channelsHeaderText: TextView
    private lateinit var channelCountText: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingLayout: FrameLayout
    private lateinit var errorLayout: LinearLayout
    private lateinit var errorText: TextView
    private lateinit var retryButton: MaterialButton
    private lateinit var emptyLayout: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_channel_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupRecyclerView()
        setupSearch()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun bindViews(view: View) {
        searchEditText = view.findViewById(R.id.search_edit_text)
        channelsHeaderText = view.findViewById(R.id.channels_header)
        channelCountText = view.findViewById(R.id.channel_count_text)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        recyclerView = view.findViewById(R.id.channels_recycler_view)
        loadingLayout = view.findViewById(R.id.loading_layout)
        errorLayout = view.findViewById(R.id.error_layout)
        errorText = view.findViewById(R.id.error_text)
        retryButton = view.findViewById(R.id.retry_button)
        emptyLayout = view.findViewById(R.id.empty_layout)
    }

    private fun setupRecyclerView() {
        adapter = ChannelAdapter(
            onChannelClick = { channel ->
                (activity as? MainActivity)?.playChannel(channel)
            },
            onFavoriteClick = { channel ->
                viewModel.toggleFavorite(channel)
                adapter.notifyDataSetChanged()
            },
            onGroupClick = { groupIndex ->
                viewModel.toggleGroupExpansion(groupIndex)
            },
            isFavorite = { channel ->
                viewModel.favoritesManager.isFavorite(channel)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(false)

        // Add item animation
        recyclerView.itemAnimator?.apply {
            addDuration = 200
            removeDuration = 200
            changeDuration = 150
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.search(s?.toString() ?: "")
            }
        })
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(
            resources.getColor(R.color.md_theme_primary, null)
        )
        swipeRefresh.setProgressBackgroundColorSchemeColor(
            resources.getColor(R.color.surface_dark_2, null)
        )
        swipeRefresh.setOnRefreshListener {
            viewModel.loadPlaylist()
        }

        retryButton.setOnClickListener {
            viewModel.loadPlaylist()
        }
    }

    private fun observeViewModel() {
        // Observe playlist state for loading/error/success
        viewModel.playlistState.observe(viewLifecycleOwner) { state ->
            swipeRefresh.isRefreshing = false

            when (state) {
                is PlaylistState.Loading -> showLoading()
                is PlaylistState.Success -> {
                    showContent()
                    channelCountText.text = getString(R.string.channel_count, state.totalChannels)
                }
                is PlaylistState.Error -> showError(state.message)
                is PlaylistState.Idle -> { /* no-op */ }
            }
        }

        // Observe filtered groups (search results)
        viewModel.filteredGroups.observe(viewLifecycleOwner) { groups ->
            if (groups.isEmpty() && viewModel.playlistState.value is PlaylistState.Success) {
                showEmpty()
            } else if (groups.isNotEmpty()) {
                adapter.submitGroups(groups)
            }
        }

        // Observe favorite events for snackbar feedback
        viewModel.favoriteEvent.observe(viewLifecycleOwner) { event ->
            event?.let { (channel, isFavorite) ->
                val message = if (isFavorite) {
                    getString(R.string.added_to_favorites)
                } else {
                    getString(R.string.removed_from_favorites)
                }
                view?.let {
                    Snackbar.make(it, "${channel.displayName}: $message", Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(resources.getColor(R.color.surface_dark_3, null))
                        .setTextColor(resources.getColor(R.color.text_primary, null))
                        .show()
                }
                viewModel.clearFavoriteEvent()
            }
        }
    }

    private fun showLoading() {
        loadingLayout.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        errorLayout.visibility = View.GONE
        emptyLayout.visibility = View.GONE
    }

    private fun showContent() {
        loadingLayout.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        errorLayout.visibility = View.GONE
        emptyLayout.visibility = View.GONE
    }

    private fun showError(message: String) {
        loadingLayout.visibility = View.GONE
        recyclerView.visibility = View.GONE
        errorLayout.visibility = View.VISIBLE
        emptyLayout.visibility = View.GONE
        errorText.text = message
    }

    private fun showEmpty() {
        loadingLayout.visibility = View.GONE
        recyclerView.visibility = View.GONE
        errorLayout.visibility = View.GONE
        emptyLayout.visibility = View.VISIBLE
    }
}
