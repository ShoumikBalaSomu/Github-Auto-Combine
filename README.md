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

## ✨ V3 Premium Features

| Feature | Description |
| :--- | :--- |
| ⚡ **Async Engine** | Verifies thousands of channels concurrently using `asyncio` & `aiohttp`. |
| 🛡️ **Stealth Deployment** | Host this on a private repository while serving public users via Vercel! |
| 📡 **Xtream Codes API** | Emulates `player_api.php` so your users can log in via IPTV Smarters or TiviMate natively. |
| 📅 **EPG Generator** | Automatically generates a lightning-fast `epg.xml` for your live channels. |
| 📺 **MAC Portal Ready** | Complete guides included to bridge your feeds into Stalker Middleware portals. |
| 💎 **Glassmorphism UI** | A premium interactive web dashboard for checking stream stats. |

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

If you already have a subscription to a premium IPTV provider and want this GitHub scraper to automatically pull, verify, and merge its channels, you can add them to `input/playlists.txt`:

### For Xtream Codes Providers
Xtream Codes natively supports M3U output. You can construct your URL like this and place it in `input/playlists.txt`:
```text
http://PROVIDER_DOMAIN:PORT/get.php?username=YOUR_USER&password=YOUR_PASS&type=m3u_plus&output=ts
```

### For MAC Portal (Stalker) Providers
MAC Portals authenticate via a device MAC address and do not directly provide an `.m3u` link. To use a MAC portal with this GitHub scraper, you must first convert it:
1. Ask your provider if they offer an **"M3U Alternative"** or an Xtream Codes login for your account (most do!). If they provide it, use the Xtream format above.
2. If they do not, you will need to use a Stalker-to-M3U extraction tool to dump your channels into an `.m3u` file, host that file somewhere (like a private GitHub Gist), and add the raw Gist URL to your `input/playlists.txt`.

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
