module.exports = async (req, res) => {
    // Set CORS headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    // 🚀 V7 Enterprise Polish: Edge Caching for massive scale
    res.setHeader('Cache-Control', 's-maxage=60, stale-while-revalidate=120');

    if (req.method === 'OPTIONS') {
        res.status(200).end();
        return;
    }

    const { action, username, password } = req.query;

    if (!username || !password) {
        return res.status(401).json({ error: "Missing authentication" });
    }

    const protocol = req.headers['x-forwarded-proto'] || 'https';
    const baseUrl = `${protocol}://${req.headers.host}`;

    try {
        switch (action) {
            case 'get_live_categories':
                const liveCats = await fetch(`${baseUrl}/output/xtream_categories.json`).then(r => r.json()).catch(() => []);
                return res.json(liveCats);
                
            case 'get_live_streams':
                const liveStreams = await fetch(`${baseUrl}/output/xtream_live.json`).then(r => r.json()).catch(() => []);
                return res.json(liveStreams);
                
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
                
            default:
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
