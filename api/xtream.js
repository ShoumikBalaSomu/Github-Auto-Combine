module.exports = async (req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');

    const { server, username, password, stream } = req.query;

    if (!server || !username || !password) {
        return res.status(400).send("Missing 'server', 'username', or 'password' parameters.");
    }

    const cleanServer = server.replace(/\/$/, "");

    try {
        // Handle playback redirect
        if (stream) {
            // stream should be the stream_id + extension, e.g. "12345.m3u8"
            const streamUrl = `${cleanServer}/live/${username}/${password}/${stream}`;
            res.writeHead(302, { Location: streamUrl });
            return res.end();
        }

        // Generate M3U Playlist
        const authUrl = `${cleanServer}/get.php?username=${username}&password=${password}&type=m3u_plus&output=ts`;
        
        const response = await fetch(authUrl);
        if (!response.ok) {
            return res.status(response.status).send(`Failed to fetch from Xtream server: ${response.statusText}`);
        }

        let m3uText = await response.text();

        // Build the base URL for the M3U callbacks
        const protocol = req.headers['x-forwarded-proto'] || 'https';
        const host = req.headers.host;
        const callbackBase = `${protocol}://${host}/api/xtream?server=${encodeURIComponent(server)}&username=${encodeURIComponent(username)}&password=${encodeURIComponent(password)}&stream=`;

        // Rewrite all stream URLs to route through our redirector
        // A typical Xtream URL is: http://server.com:80/live/user/pass/12345.ts
        const regex = new RegExp(`^.*?/live/${username}/${password}/(.*?)$`, "gm");
        m3uText = m3uText.replace(regex, `${callbackBase}$1`);

        res.setHeader('Content-Type', 'audio/x-mpegurl');
        res.status(200).send(m3uText);

    } catch (error) {
        console.error("Xtream Proxy Error:", error);
        return res.status(500).send(`Internal Server Error: ${error.message}`);
    }
};
