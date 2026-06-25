let allChannels = [];
let hls = null;

document.addEventListener('DOMContentLoaded', async () => {
    // Check if token exists
    const token = localStorage.getItem('omniverse_token');
    if (token) document.getElementById('authToken').value = token;

    await loadLiveChannels();
    await loadVODs();
});

async function loadVODs() {
    try {
        const mRes = await fetch('../output/movies.json');
        if (mRes.ok) {
            const movies = await mRes.json();
            allChannels = allChannels.concat(movies);
            renderGrid('movies', movies);
        }
        
        const sRes = await fetch('../output/series.json');
        if (sRes.ok) {
            const series = await sRes.json();
            allChannels = allChannels.concat(series);
            renderGrid('series', series);
        }
    } catch (e) {
        console.log("No VODs found.");
    }
}

function switchTab(tabId) {
    document.querySelectorAll('.nav-links li').forEach(li => li.classList.remove('active'));
    event.target.classList.add('active');

    document.querySelectorAll('.grid-container').forEach(grid => {
        grid.classList.remove('active-tab');
        grid.classList.add('hidden-tab');
    });

    document.getElementById(tabId).classList.add('active-tab');
    document.getElementById('searchInput').value = '';
    filterContent();
}

function saveToken() {
    const token = document.getElementById('authToken').value;
    localStorage.setItem('omniverse_token', token);
}

async function loadLiveChannels() {
    try {
        // Fetch the V5 Singularity Auto-Healing Router M3U8
        // or fallback to the standard one if not deployed on Vercel
        let res = await fetch('../output/combine_vercel.m3u8');
        if (!res.ok) {
            res = await fetch('../output/combine_live.m3u8');
        }
        
        const m3uText = await res.text();
        parseM3U(m3uText);
    } catch (e) {
        document.getElementById('loading').innerText = "Failed to load Omniverse Database.";
        console.error(e);
    }
}

function parseM3U(m3u) {
    const lines = m3u.split('\n');
    let currentChannel = {};

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim();
        if (line.startsWith('#EXTINF:')) {
            const logoMatch = line.match(/tvg-logo="(.*?)"/);
            const groupMatch = line.match(/group-title="(.*?)"/);
            const nameSplit = line.split(',');
            
            currentChannel = {
                name: nameSplit[1] || 'Unknown Channel',
                logo: logoMatch ? logoMatch[1] : 'https://cdn-icons-png.flaticon.com/512/3159/3159302.png',
                group: groupMatch ? groupMatch[1] : 'Uncategorized',
                type: 'live'
            };
        } else if (line && !line.startsWith('#')) {
            currentChannel.url = line;
            allChannels.push(currentChannel);
            currentChannel = {};
        }
    }

    document.getElementById('loading').style.display = 'none';
    renderGrid('live', allChannels);
}

function renderGrid(tabId, items) {
    const grid = document.getElementById(tabId);
    grid.innerHTML = '';

    items.forEach(item => {
        const card = document.createElement('div');
        card.className = `card ${item.type === 'movie' ? 'movie-card' : ''}`;
        card.innerHTML = `
            <img src="${item.logo}" alt="${item.name}" onerror="this.src='https://cdn-icons-png.flaticon.com/512/3159/3159302.png'">
            <h3>${item.name}</h3>
            ${item.group ? `<p style="font-size: 0.8rem; color: #94a3b8; margin-top: 0.5rem;">${item.group}</p>` : ''}
        `;
        card.onclick = () => playStream(item.name, item.url);
        grid.appendChild(card);
    });
}

function filterContent() {
    const term = document.getElementById('searchInput').value.toLowerCase();
    const filtered = allChannels.filter(ch => ch.name.toLowerCase().includes(term) || (ch.group && ch.group.toLowerCase().includes(term)));
    renderGrid('live', filtered);
}

function playStream(title, url) {
    const token = localStorage.getItem('omniverse_token');
    
    // Add token if it's a relative/vercel API URL
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
            enableWorker: true
        });
        hls.loadSource(url);
        hls.attachMedia(video);
        hls.on(Hls.Events.MANIFEST_PARSED, function() {
            video.play();
        });
        hls.on(Hls.Events.ERROR, function(event, data) {
            if (data.fatal) {
                document.getElementById('playerError').classList.remove('hidden');
            }
        });
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        // For Safari
        video.src = url;
        video.play();
    }
}

function closePlayer() {
    document.getElementById('playerModal').classList.add('hidden');
    if (hls) {
        hls.destroy();
        hls = null;
    }
    document.getElementById('videoPlayer').pause();
    document.getElementById('videoPlayer').src = '';
}
