<div align="center">
  <img src="https://raw.githubusercontent.com/shoumikbalasomu/Github-Auto-Combine/main/docs/output/logo.png" width="120" alt="IPTV Auto-Combine Logo" onerror="this.src='https://cdn-icons-png.flaticon.com/512/3159/3159302.png'">
  <h1>🌟 IPTV Auto-Combine (V3 Peak Premium)</h1>
  <p><em>A fully automated, ultra-fast IPTV playlist manager with Serverless Xtream Codes API.</em></p>

  <p>
    <a href="https://github.com/shoumikbalasomu/Github-Auto-Combine/actions"><img src="https://github.com/shoumikbalasomu/Github-Auto-Combine/actions/workflows/merge-playlists.yml/badge.svg" alt="Pipeline Status"></a>
    <img src="https://img.shields.io/badge/Python-3.11+-blue.svg" alt="Python 3.11+">
    <img src="https://img.shields.io/badge/Vercel-Serverless-black.svg" alt="Vercel Serverless">
    <img src="https://img.shields.io/badge/Asyncio-Powered-purple.svg" alt="Asyncio Powered">
  </p>
</div>

<br>

Welcome to **IPTV Auto-Combine V3**, the most advanced automated IPTV engine on GitHub. This repository goes beyond a standard M3U generator. It provides a full **Serverless Xtream Codes API**, **EPG XMLTV Generation**, and advanced deployment strategies to keep your fork **100% private** while serving public users.

---

## ✨ V6 Omniverse Features

| Feature | Description |
| :--- | :--- |
| 🍿 **Web Player** | A fully functional Netflix-style browser player (`/docs`). Watch live streams natively! |
| 🎬 **VOD Engine** | Automatically scrapes thousands of Movies & TV Shows from your Xtream Codes providers into an offline JSON DB! |
| ⏪ **Catch-Up TV** | A serverless Time-Shift DVR API. Rewind live TV up to 7 days backward natively in the player! |
| 🔐 **Anti-Piracy Security** | Lock your Vercel `api/play.js` with an `AUTH_TOKEN` environment variable so no one can steal your bandwidth! |
| 🔄 **Auto-Healing Router** | Groups identical channels. If one stream dies, the API seamlessly redirects you to the backup stream! |
| 🧠 **Smart Tagger** | Uses Fuzzy-Logic AI to auto-correct messy channel names and inject official Logos and EPG IDs! |
| 🎬 **Deep Codec Priority** | Auto-extracts stream resolution and tags channels with `[4K]` or `[1080p]`. |
| ⚡ **Vercel Edge CDN** | Lightning fast API responses across the globe using Edge caching. |

---

## 📺 Default Feed Links

If you are using the public GitHub Pages deployment:

| Asset | Link |
| :--- | :--- |
| **All Channels (.m3u8)** | `https://shoumikbalasomu.github.io/Github-Auto-Combine/output/combine.m3u8` |
| **Live Channels (.m3u8)** | `https://shoumikbalasomu.github.io/Github-Auto-Combine/output/combine_live.m3u8` |
| **TV Guide / EPG (.xml)** | `https://shoumikbalasomu.github.io/Github-Auto-Combine/output/epg.xml` |
| **Xtream API Server** | *(Vercel deployment required - see below)* |

---

## 🕵️‍♂️ "Stealth Mode" Setup (Private Fork + Public API)

If you fork this repository to build your own premium service, you probably want to **hide your sources** from the public. GitHub Pages cannot host private repos for free. Here is the ultimate workaround using Vercel.

1. **Fork** this repository and make it **Private**.
2. Go to [Vercel](https://vercel.com) and sign in with GitHub.
3. Click **Add New Project** -> **Import your private fork**.
4. Leave all build settings as default. Click **Deploy**.
5. *Boom!* Vercel will instantly host your `docs/` UI, your `output/combine_live.m3u8` files, AND your `api/player_api.js` serverless function.

Your source code remains completely hidden, but your users get a lightning-fast premium service!

---

## 🚀 How to Connect Players (Xtream Codes API)

If you deployed using the Vercel method above, your users don't need clunky M3U URLs. They can log in like a premium service:

**App:** IPTV Smarters / TiviMate / XCIPTV  
**Server URL:** `https://your-vercel-domain.vercel.app`  
**Username:** `admin` *(or anything)*  
**Password:** `admin` *(or anything)*  

The built-in Vercel serverless function (`api/player_api.js`) automatically intercepts these requests, parses your live M3U8, and serves the exact JSON categories and streams the apps expect.

---

## 📖 MAC Portal (Stalker Middleware)

If you want to provide your feeds to older MAG 250/254 boxes that strictly require MAC Portals (Stalker), please read our [MAC Portal Guide](./docs/MAC_PORTAL_GUIDE.md) for bridging instructions.

---

## ➕ Adding Your Premium Providers (Xtream & MAC)

If you have your own premium subscriptions, this repository provides advanced **Serverless Middleware Bridges** to securely use them without leaking your credentials!

### 1. The GitHub Secrets Method (Recommended for GitHub Actions)
If you just want the GitHub scraper to merge your premium Xtream Codes M3U along with the public channels:
1. Go to your GitHub repository **Settings** -> **Secrets and variables** -> **Actions**.
2. Create a new repository secret named `SECRET_PLAYLISTS`.
3. Paste your Xtream Codes M3U URLs (one per line). 
*The GitHub Action will now securely merge these premium channels without exposing your credentials!*

### 2. The Vercel Middleware Bridges (For Live Streaming)
If you deployed to Vercel, this repository acts as an **Active Middleware Proxy**!

**A) Stalker to M3U Bridge:**
Got a MAC portal but want to use it on an M3U player (like VLC or Apple TV)?
Simply construct this URL using your Vercel domain:
`https://your-vercel.app/api/stalker?portal=http://provider.com/c/&mac=00:1A:79:XX:YY:ZZ`
*The serverless function will dynamically perform the Stalker handshake and stream a fresh M3U playlist directly to your player!*

**B) Xtream Codes Proxy:**
Want to hide your IP or share a playlist without exposing the real server?
`https://your-vercel.app/api/xtream?server=http://provider.com:80&username=USER&password=PASS`
*This outputs a proxied M3U file where every video stream routes through your Vercel serverless function!*

---

## 🏗️ Repository Architecture

- 📁 **`input/`** - Define your raw M3U playlist sources here (`playlists.txt`).
- 📁 **`scripts/`** - Async Python engine and `generate_epg.py`.
- 📁 **`api/`** - Vercel Serverless Functions (`player_api.js` Xtream bridge).
- 📁 **`docs/`** - The frontend dashboard and Stalker guides.
- 📁 **`output/`** - The final M3U8 and XMLTV files.
- 📄 **`vercel.json`** - Configuration for API rewrites and CORS.

<div align="center">
  <p>Built with ❤️ and 🐍 Python. Licensed under the <strong>MIT License</strong>.</p>
</div>
