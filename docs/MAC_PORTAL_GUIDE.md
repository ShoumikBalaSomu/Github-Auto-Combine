# 📺 MAC Portal / Stalker Middleware Guide

While this repository generates static `.m3u8` playlists and a serverless Xtream Codes API, many set-top boxes (like MAG 250/254/322) strictly use the **Stalker Middleware (MAC Portal)** system.

Because Stalker Middleware requires a heavy SQL database and PHP backend (like Infomir's Ministra), it cannot be hosted directly on GitHub Pages or Vercel Serverless. However, if you are building a premium service, you can easily bridge this repo's output into a MAC Portal.

## Option 1: Use a Third-Party Portal Emulator
Many Smart TV apps (like Smart STB or STB Emu) allow you to bypass traditional MAC portals and just load an M3U file directly, while simulating the MAG UI.
- Simply feed your `https://your-domain.com/output/combine_live.m3u8` into STB Emu's playlist settings.

## Option 2: Host Your Own Lightweight Portal Backend
If you want to offer your users a real `http://portal.yourdomain.com/c/` address:
1. **Rent a cheap VPS** (e.g., DigitalOcean, Hetzner, Linode).
2. Install [Ministra TV Platform](https://www.infomir.eu/eng/solutions/ministra-tv-platform/) (the official Stalker Middleware).
3. Write a simple CRON script on your VPS that automatically pulls the `combine_live.m3u8` from your GitHub/Vercel link every hour and imports it into the Ministra MySQL database using their API.
4. You can then assign MAG boxes to your users via MAC Address, and the content will be perfectly in sync with your GitHub Action scraper!

## Option 3: Xtream Codes to Stalker Bridge
If you deploy the Vercel Serverless Xtream API included in this repo (`api/player_api.js`), you can use IPTV billing panels (like Xtream UI) to automatically convert your Xtream connection into a MAC connection for older devices.
