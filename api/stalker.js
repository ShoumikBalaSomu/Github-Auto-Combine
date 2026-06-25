module.exports = async (req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');

    const { portal, mac, cmd } = req.query;

    if (!portal || !mac) {
        return res.status(400).send("Missing 'portal' or 'mac' parameters.");
    }

    const cleanPortal = portal.replace(/\/$/, "");
    const apiUrl = `${cleanPortal}/server/load.php`;
    
    // Base headers to emulate a MAG box
    const headers = {
        "User-Agent": "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) MAG200 stbapp ver: 2 rev: 250 Safari/533.3",
        "Cookie": `mac=${mac}; stb_lang=en; timezone=Europe/London;`,
        "X-User-Agent": "Model: MAG250; Link:"
    };

    try {
        // Step 1: Handshake to get Token
        const handshakeRes = await fetch(`${apiUrl}?type=stb&action=handshake&token=&device_mac=${mac}`, { headers });
        const handshakeData = await handshakeRes.json();
        const token = handshakeData.js?.token;

        if (!token) {
            return res.status(401).send("Failed to authenticate MAC address. Token not received.");
        }

        // Add token to headers
        headers["Authorization"] = `Bearer ${token}`;

        // Step 2: Handle Playback Redirect (if cmd is provided)
        if (cmd) {
            const linkRes = await fetch(`${apiUrl}?type=itv&action=create_link&cmd=${encodeURIComponent(cmd)}&mac=${mac}`, { headers });
            const linkData = await linkRes.json();
            const streamUrl = linkData.js?.cmd;
            
            if (streamUrl) {
                // Return HTTP 302 Redirect to the actual video stream
                res.writeHead(302, { Location: streamUrl.split(' ')[0] });
                return res.end();
            } else {
                return res.status(404).send("Stream not found or offline.");
            }
        }

        // Step 3: Generate M3U Playlist
        // First get profile
        await fetch(`${apiUrl}?type=stb&action=get_profile&mac=${mac}`, { headers });
        
        // Then get all channels
        const channelsRes = await fetch(`${apiUrl}?type=itv&action=get_all_channels&mac=${mac}`, { headers });
        const channelsData = await channelsRes.json();
        
        const channels = channelsData.js?.data || [];

        // Build the base URL for the M3U callbacks
        const protocol = req.headers['x-forwarded-proto'] || 'https';
        const host = req.headers.host;
        const callbackBase = `${protocol}://${host}/api/stalker?portal=${encodeURIComponent(portal)}&mac=${encodeURIComponent(mac)}&cmd=`;

        let m3u = "#EXTM3U\n";
        
        for (const ch of channels) {
            if (ch.name && ch.cmd) {
                const group = ch.tv_genre_id ? `Category ${ch.tv_genre_id}` : "Uncategorized";
                m3u += `#EXTINF:-1 tvg-id="${ch.xmltv_id || ''}" tvg-logo="${ch.logo || ''}" group-title="${group}",${ch.name}\n`;
                m3u += `${callbackBase}${encodeURIComponent(ch.cmd)}\n`;
            }
        }

        res.setHeader('Content-Type', 'audio/x-mpegurl');
        res.status(200).send(m3u);

    } catch (error) {
        console.error("Stalker API Error:", error);
        return res.status(500).send(`Internal Server Error: ${error.message}`);
    }
};
