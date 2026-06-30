// ═══════════════════════════════════════════════════════════════
//  IPTV Omniverse — V9 Apex Web Player Engine
//  Features: Live TV, VOD, External M3U Fetcher, Country Filters
// ═══════════════════════════════════════════════════════════════

let allChannels = [];
let liveChannels = [];
let hls = null;
let activeCountryFilter = null;

document.addEventListener('DOMContentLoaded', async () => {
    const token = localStorage.getItem('omniverse_token');
    if (token) document.getElementById('authToken').value = token;

    await loadStats();
    await loadLiveChannels();
    await loadVODs();
});

// ─── Stats ─────────────────────────────────────────────────────
async function loadStats() {
    try {
        const res = await fetch('./output/stats.json');
        if (res.ok) {
            const stats = await res.json();
            document.getElementById('statLive').textContent = (stats.live_channels || 0).toLocaleString();
            document.getElementById('statCountries').textContent = stats.countries || '250+';
            if (stats.link_check_time) {
                const d = new Date(stats.link_check_time);
                const ago = timeSince(d);
                document.getElementById('statUpdated').textContent = ago;
            }
        }
    } catch (e) { console.log('Stats unavailable'); }
}

function timeSince(date) {
    const seconds = Math.floor((new Date() - date) / 1000);
    if (seconds < 60) return 'just now';
    if (seconds < 3600) return Math.floor(seconds / 60) + 'm ago';
    if (seconds < 86400) return Math.floor(seconds / 3600) + 'h ago';
    return Math.floor(seconds / 86400) + 'd ago';
}

// ─── VOD Loading ───────────────────────────────────────────────
async function loadVODs() {
    try {
        const mRes = await fetch('./output/movies.json');
        if (mRes.ok) {
            const movies = await mRes.json();
            allChannels = allChannels.concat(movies);
            renderGrid('movies', movies);
        }
        const sRes = await fetch('./output/series.json');
        if (sRes.ok) {
            const series = await sRes.json();
            allChannels = allChannels.concat(series);
            renderGrid('series', series);
        }
    } catch (e) { console.log('No VODs found.'); }
}

// ─── Tab Switching ─────────────────────────────────────────────
function switchTab(tabId) {
    document.querySelectorAll('.nav-links li').forEach(li => li.classList.remove('active'));
    event.target.classList.add('active');

    document.querySelectorAll('.grid-container').forEach(grid => {
        grid.classList.remove('active-tab');
        grid.classList.add('hidden-tab');
    });

    document.getElementById(tabId).classList.add('active-tab');
    document.getElementById('searchInput').value = '';
    activeCountryFilter = null;
    document.querySelectorAll('.pill').forEach(p => p.classList.remove('active'));

    if (tabId === 'live') {
        renderGrid('live', liveChannels);
    }
}

function saveToken() {
    const token = document.getElementById('authToken').value;
    localStorage.setItem('omniverse_token', token);
}

// ─── Live Channel Loading ──────────────────────────────────────
async function loadLiveChannels() {
    updateLoadingStatus('Loading combined playlist...');

    try {
        let res = await fetch('./output/combine_vercel.m3u8');
        if (!res.ok) res = await fetch('./output/combine_live.m3u8');
        if (!res.ok) res = await fetch('./output/combine.m3u8');

        const m3uText = await res.text();
        const channels = parseM3U(m3uText);
        liveChannels = channels;
        allChannels = allChannels.concat(channels);

        document.getElementById('loading').style.display = 'none';
        renderGrid('live', channels);
        buildCountryFilters(channels);

        document.getElementById('statLive').textContent = channels.length.toLocaleString();

    } catch (e) {
        document.getElementById('loading').innerHTML = '<p style="color: var(--danger);">Failed to load Omniverse Database.</p>';
        console.error(e);
    }
}

function updateLoadingStatus(text) {
    const el = document.getElementById('loadingStatus');
    if (el) el.textContent = text;
}

// ─── M3U Parser ────────────────────────────────────────────────
function parseM3U(m3u) {
    const lines = m3u.split('\n');
    const channels = [];
    let currentChannel = {};

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim();
        if (line.startsWith('#EXTINF:')) {
            const logoMatch = line.match(/tvg-logo="(.*?)"/);
            const groupMatch = line.match(/group-title="(.*?)"/);
            const nameSplit = line.split(',');

            currentChannel = {
                name: nameSplit[nameSplit.length - 1] || 'Unknown Channel',
                logo: logoMatch ? logoMatch[1] : '',
                group: groupMatch ? groupMatch[1] : 'Uncategorized',
                type: 'live'
            };
        } else if (line && !line.startsWith('#')) {
            currentChannel.url = line;
            if (currentChannel.name) {
                channels.push(currentChannel);
            }
            currentChannel = {};
        }
    }

    return channels;
}

// ─── Country Filter Pills ──────────────────────────────────────
function buildCountryFilters(channels) {
    const groups = {};
    channels.forEach(ch => {
        const g = ch.group || 'Other';
        groups[g] = (groups[g] || 0) + 1;
    });

    // Sort by count descending, take top 20
    const sorted = Object.entries(groups)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 20);

    const container = document.getElementById('filterPills');
    container.innerHTML = '';

    // "All" pill
    const allPill = document.createElement('span');
    allPill.className = 'pill active';
    allPill.textContent = `All (${channels.length})`;
    allPill.onclick = () => {
        activeCountryFilter = null;
        document.querySelectorAll('.pill').forEach(p => p.classList.remove('active'));
        allPill.classList.add('active');
        renderGrid('live', liveChannels);
    };
    container.appendChild(allPill);

    sorted.forEach(([group, count]) => {
        const pill = document.createElement('span');
        pill.className = 'pill';
        pill.textContent = `${group} (${count})`;
        pill.onclick = () => {
            activeCountryFilter = group;
            document.querySelectorAll('.pill').forEach(p => p.classList.remove('active'));
            pill.classList.add('active');
            const filtered = liveChannels.filter(ch => ch.group === group);
            renderGrid('live', filtered);
        };
        container.appendChild(pill);
    });
}

// ─── Render Grid ───────────────────────────────────────────────
function renderGrid(tabId, items) {
    const grid = document.getElementById(tabId);
    if (!grid) return;

    // Use DocumentFragment for performance with large lists
    const fragment = document.createDocumentFragment();
    const defaultIcon = 'https://cdn-icons-png.flaticon.com/512/3159/3159302.png';

    // Virtual windowing: render only first 500, load more on scroll
    const maxInitial = 500;
    const toRender = items.slice(0, maxInitial);

    toRender.forEach(item => {
        const card = document.createElement('div');
        card.className = `card ${item.type === 'movie' ? 'movie-card' : ''}`;
        card.innerHTML = `
            <img src="${item.logo || defaultIcon}" alt="${item.name}" loading="lazy" onerror="this.src='${defaultIcon}'">
            <h3>${item.name}</h3>
            ${item.group ? `<span class="group-tag">${item.group}</span>` : ''}
        `;
        card.onclick = () => playStream(item.name, item.url);
        fragment.appendChild(card);
    });

    grid.innerHTML = '';
    grid.appendChild(fragment);

    // If more items remain, add a "Load More" button
    if (items.length > maxInitial) {
        const loadMore = document.createElement('div');
        loadMore.style.cssText = 'grid-column: 1/-1; text-align: center; padding: 2rem;';
        loadMore.innerHTML = `<button onclick="loadMoreChannels()" style="padding: 12px 32px; border-radius: 12px; border: 1px solid var(--glass-border); background: var(--card-bg); color: var(--text-primary); font-weight: 700; cursor: pointer; font-size: 0.9rem;">Load ${items.length - maxInitial} more channels</button>`;
        grid.appendChild(loadMore);

        // Store remainder for lazy load
        window._remainingItems = items.slice(maxInitial);
        window._targetGrid = tabId;
    }
}

function loadMoreChannels() {
    if (!window._remainingItems) return;
    const grid = document.getElementById(window._targetGrid);
    const fragment = document.createDocumentFragment();
    const defaultIcon = 'https://cdn-icons-png.flaticon.com/512/3159/3159302.png';

    window._remainingItems.forEach(item => {
        const card = document.createElement('div');
        card.className = `card ${item.type === 'movie' ? 'movie-card' : ''}`;
        card.innerHTML = `
            <img src="${item.logo || defaultIcon}" alt="${item.name}" loading="lazy" onerror="this.src='${defaultIcon}'">
            <h3>${item.name}</h3>
            ${item.group ? `<span class="group-tag">${item.group}</span>` : ''}
        `;
        card.onclick = () => playStream(item.name, item.url);
        fragment.appendChild(card);
    });

    // Remove the "Load More" button
    const loadMoreDiv = grid.querySelector('[style*="grid-column"]');
    if (loadMoreDiv) loadMoreDiv.remove();

    grid.appendChild(fragment);
    window._remainingItems = null;
}

// ─── Search / Filter ───────────────────────────────────────────
function filterContent() {
    const term = document.getElementById('searchInput').value.toLowerCase();
    let source = liveChannels;

    if (activeCountryFilter) {
        source = liveChannels.filter(ch => ch.group === activeCountryFilter);
    }

    if (term) {
        source = source.filter(ch =>
            ch.name.toLowerCase().includes(term) ||
            (ch.group && ch.group.toLowerCase().includes(term))
        );
    }

    renderGrid('live', source);
}

// ─── External Playlist Loader ──────────────────────────────────
async function loadExternalPlaylist() {
    const url = document.getElementById('externalUrl').value.trim();
    if (!url) return;
    await fetchAndMergePlaylist(url);
}

function quickLoad(url) {
    document.getElementById('externalUrl').value = url;
    fetchAndMergePlaylist(url);
}

async function fetchAndMergePlaylist(url) {
    const statusEl = document.getElementById('externalStatus');
    statusEl.textContent = '⏳ Fetching playlist...';
    statusEl.style.color = 'var(--warning)';

    try {
        // Use cors-anywhere or direct fetch
        let res;
        try {
            res = await fetch(url);
        } catch (corsError) {
            // Try CORS proxy as fallback
            res = await fetch(`https://api.allorigins.win/raw?url=${encodeURIComponent(url)}`);
        }

        if (!res.ok) throw new Error(`HTTP ${res.status}`);

        const text = await res.text();
        const channels = parseM3U(text);

        if (channels.length === 0) {
            statusEl.textContent = '❌ No channels found in playlist.';
            statusEl.style.color = 'var(--danger)';
            return;
        }

        // Merge with live channels (deduplicate by URL)
        const existingUrls = new Set(liveChannels.map(ch => ch.url));
        let added = 0;
        channels.forEach(ch => {
            if (!existingUrls.has(ch.url)) {
                liveChannels.push(ch);
                allChannels.push(ch);
                existingUrls.add(ch.url);
                added++;
            }
        });

        statusEl.textContent = `✅ Loaded ${channels.length} channels (${added} new, ${channels.length - added} duplicates skipped)`;
        statusEl.style.color = 'var(--success)';

        // Switch to Live TV tab and re-render
        document.getElementById('statLive').textContent = liveChannels.length.toLocaleString();
        buildCountryFilters(liveChannels);

    } catch (e) {
        statusEl.textContent = `❌ Failed: ${e.message}`;
        statusEl.style.color = 'var(--danger)';
    }
}

// ─── Video Player ──────────────────────────────────────────────
function playStream(title, url) {
    const token = localStorage.getItem('omniverse_token');

    if (url.includes('/api/') && token) {
        url += (url.includes('?') ? '&' : '?') + `token=${token}`;
    }

    document.getElementById('playerTitle').innerText = title;
    document.getElementById('playerModal').classList.remove('hidden');
    document.getElementById('playerError').classList.add('hidden');

    const video = document.getElementById('videoPlayer');

    if (Hls.isSupported()) {
        if (hls) hls.destroy();
        hls = new Hls({
            maxBufferLength: 30,
            maxMaxBufferLength: 60,
            enableWorker: true,
            lowLatencyMode: true,
        });
        hls.loadSource(url);
        hls.attachMedia(video);
        hls.on(Hls.Events.MANIFEST_PARSED, () => video.play());
        hls.on(Hls.Events.ERROR, (event, data) => {
            if (data.fatal) {
                // Try direct play as last resort
                if (data.type === Hls.ErrorTypes.NETWORK_ERROR) {
                    hls.destroy();
                    video.src = url;
                    video.play().catch(() => {
                        document.getElementById('playerError').classList.remove('hidden');
                    });
                } else {
                    document.getElementById('playerError').classList.remove('hidden');
                }
            }
        });
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        video.src = url;
        video.play();
    } else {
        // Fallback: try direct
        video.src = url;
        video.play().catch(() => {
            document.getElementById('playerError').classList.remove('hidden');
        });
    }
}

function closePlayer() {
    document.getElementById('playerModal').classList.add('hidden');
    if (hls) {
        hls.destroy();
        hls = null;
    }
    const video = document.getElementById('videoPlayer');
    video.pause();
    video.src = '';
}

// ─── Keyboard Shortcuts ────────────────────────────────────────
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closePlayer();
    if (e.key === '/' && document.activeElement.tagName !== 'INPUT') {
        e.preventDefault();
        document.getElementById('searchInput').focus();
    }
});
