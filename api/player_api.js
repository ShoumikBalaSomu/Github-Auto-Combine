module.exports = async (req, res) => {
    // Set CORS headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        res.status(200).end();
        return;
    }

    const { action, username, password } = req.query;

    // Simple auth check: Accept any username/password for convenience, 
    // but in a true private repo you could check process.env.XTREAM_USER etc.
    if (!username || !password) {
        return res.status(401).json({ error: "Missing authentication" });
    }

    // Determine the base URL for the M3U8
    // In Vercel, req.headers.host gives the domain
    const protocol = req.headers['x-forwarded-proto'] || 'https';
    const baseUrl = `${protocol}://${req.headers.host}`;
    
    let m3uUrl;
    // We fetch the m3u8 relative to the current deployment
    if (process.env.VERCEL_URL) {
         m3uUrl = `${baseUrl}/output/combine_live.m3u8`;
    } else {
         // Fallback for local testing or GitHub Pages
         m3uUrl = `${baseUrl}/output/combine_live.m3u8`;
    }

    try {
        const response = await fetch(m3uUrl);
        if (!response.ok) {
            throw new Error(`Failed to fetch playlist: ${response.status}`);
        }
        
        const m3uText = await response.text();
        const parsed = parseM3U(m3uText);

        switch (action) {
            case 'get_live_categories':
                return res.json(parsed.categories);
            case 'get_live_streams':
                return res.json(parsed.streams);
            case 'get_vod_categories':
                const mCats = await fetch(`${baseUrl}/output/movies.json`).then(r => r.json()).catch(() => []);
                const uniqueMCats = [...new Set(mCats.map(m => m.group))];
                return res.json(uniqueMCats.map((name, i) => ({ category_id: (i+1).toString(), category_name: name, parent_id: 0 })));
            case 'get_vod_streams':
                const categoryId = req.query.category_id;
                const mStreams = await fetch(`${baseUrl}/output/movies.json`).then(r => r.json()).catch(() => []);
                const mCatMap = new Map();
                [...new Set(mStreams.map(m => m.group))].forEach((name, i) => mCatMap.set(name, (i+1).toString()));
                let finalMovies = mStreams.map((m, i) => ({
                    num: i+1,
                    name: m.name,
                    stream_type: "movie",
                    stream_id: m.id || (i+1),
                    stream_icon: m.logo,
                    rating: m.rating || 5,
                    added: "1600000000",
                    category_id: mCatMap.get(m.group),
                    container_extension: "mp4",
                    custom_sid: "",
                    direct_source: ""
                }));
                if (categoryId) finalMovies = finalMovies.filter(m => m.category_id === categoryId);
                return res.json(finalMovies);
            case 'get_series_categories':
                const sCats = await fetch(`${baseUrl}/output/series.json`).then(r => r.json()).catch(() => []);
                const uniqueSCats = [...new Set(sCats.map(s => s.group))];
                return res.json(uniqueSCats.map((name, i) => ({ category_id: (i+1).toString(), category_name: name, parent_id: 0 })));
            case 'get_series':
                const sCategoryId = req.query.category_id;
                const sStreams = await fetch(`${baseUrl}/output/series.json`).then(r => r.json()).catch(() => []);
                const sCatMap = new Map();
                [...new Set(sStreams.map(s => s.group))].forEach((name, i) => sCatMap.set(name, (i+1).toString()));
                let finalSeries = sStreams.map((s, i) => ({
                    num: i+1,
                    name: s.name,
                    series_id: s.id || (i+1),
                    cover: s.logo,
                    plot: "",
                    cast: "",
                    director: "",
                    genre: s.group,
                    releaseDate: "",
                    last_modified: "1600000000",
                    rating: s.rating || 5,
                    category_id: sCatMap.get(s.group),
                    backdrop_path: [s.logo]
                }));
                if (sCategoryId) finalSeries = finalSeries.filter(s => s.category_id === sCategoryId);
                return res.json(finalSeries);
                // If no action is specified, return account info (auth check)
                return res.json({
                    user_info: {
                        username: username,
                        password: password,
                        message: "Authentication successful",
                        auth: 1,
                        status: "Active",
                        exp_date: "1999999999", // Never expires
                        is_trial: "0",
                        active_cons: 1,
                        created_at: "1600000000",
                        max_connections: 5,
                        allowed_output_formats: ["m3u8", "ts"]
                    },
                    server_info: {
                        url: req.headers.host,
                        port: "443",
                        https_port: "443",
                        server_protocol: "https",
                        rtmp_port: "80",
                        timezone: "Europe/London",
                        timestamp_now: Math.floor(Date.now() / 1000),
                        time_now: new Date().toISOString()
                    }
                });
        }
    } catch (error) {
        console.error("Xtream API Error:", error);
        return res.status(500).json({ error: "Internal Server Error", details: error.message });
    }
};

// Extremely lightweight M3U parser for Serverless environment
function parseM3U(m3u) {
    const lines = m3u.split('\n');
    
    const categoriesMap = new Map();
    const streams = [];
    
    let currentCategory = "Uncategorized";
    let currentChannel = {};
    let categoryIdCounter = 1;
    let streamIdCounter = 1;

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim();
        
        if (line.startsWith('#EXTINF:')) {
            currentChannel = {};
            
            // Extract group-title
            const groupMatch = line.match(/group-title="([^"]+)"/);
            if (groupMatch) {
                currentCategory = groupMatch[1].replace(/[\uD800-\uDBFF][\uDC00-\uDFFF]|\uD83C[\uDDE6-\uDDFF]\uD83C[\uDDE6-\uDDFF]/g, '').trim();
            }

            // Extract tvg-logo
            const logoMatch = line.match(/tvg-logo="([^"]+)"/);
            if (logoMatch) {
                currentChannel.stream_icon = logoMatch[1];
            }

            // Extract name (everything after the last comma)
            const nameParts = line.split(',');
            if (nameParts.length > 1) {
                currentChannel.name = nameParts.pop().trim();
            } else {
                currentChannel.name = "Unknown Channel";
            }

            // Assign category ID
            if (!categoriesMap.has(currentCategory)) {
                categoriesMap.set(currentCategory, categoryIdCounter++);
            }
            currentChannel.category_id = categoriesMap.get(currentCategory);
            
        } else if (line && !line.startsWith('#')) {
            // It's a URL
            if (currentChannel.name) {
                streams.push({
                    num: streamIdCounter,
                    name: currentChannel.name,
                    stream_type: "live",
                    stream_id: streamIdCounter++,
                    stream_icon: currentChannel.stream_icon || "",
                    epg_channel_id: null,
                    added: "1600000000",
                    category_id: currentChannel.category_id,
                    custom_sid: "",
                    tv_archive: 1,
                    direct_source: line,
                    tv_archive_duration: 7
                });
            }
        }
    }

    const categories = Array.from(categoriesMap.entries()).map(([name, id]) => ({
        category_id: id.toString(),
        category_name: name,
        parent_id: 0
    }));

    return { categories, streams };
}
