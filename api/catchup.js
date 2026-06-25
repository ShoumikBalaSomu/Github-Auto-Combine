module.exports = async (req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');

    const { server, username, password, stream, start, duration } = req.query;

    if (!server || !username || !password || !stream || !start) {
        return res.status(400).send("Missing parameters for Catch-Up TV.");
    }

    const cleanServer = server.replace(/\/$/, "");

    try {
        // Construct the Xtream Codes time-shift DVR URL
        // Example: http://server.com/streaming/timeshift.php?username=X&password=Y&stream=123&start=1620000000&duration=60
        let catchupUrl = `${cleanServer}/streaming/timeshift.php?username=${username}&password=${password}&stream=${stream}&start=${start}`;
        
        if (duration) {
            catchupUrl += `&duration=${duration}`;
        }

        // Return HTTP 302 Redirect to the actual DVR video stream
        res.writeHead(302, { Location: catchupUrl });
        return res.end();

    } catch (error) {
        console.error("Catchup Error:", error);
        return res.status(500).send(`Internal Server Error: ${error.message}`);
    }
};
