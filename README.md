<div align="center">
  <h1>🏆 IPTV Auto-Combine (V9 Apex)</h1>
  <p><em>The Most Advanced Free IPTV Engine on GitHub — 100% Automated, 100% Free</em></p>

  <p>
    <a href="https://github.com/shoumikbalasomu/Github-Auto-Combine/actions"><img src="https://github.com/shoumikbalasomu/Github-Auto-Combine/actions/workflows/merge-playlists.yml/badge.svg" alt="Pipeline Status"></a>
    <img src="https://img.shields.io/badge/Python-3.12+-blue.svg" alt="Python 3.12+">
    <img src="https://img.shields.io/badge/Kotlin-Android-green.svg" alt="Kotlin Android">
    <img src="https://img.shields.io/badge/Vercel-Serverless-black.svg" alt="Vercel Serverless">
    <img src="https://img.shields.io/badge/Channels-15000+-purple.svg" alt="15000+ Channels">
    <img src="https://img.shields.io/badge/Cost-$0.00-brightgreen.svg" alt="100% Free">
  </p>
</div>

<br>

Welcome to **IPTV Auto-Combine V9 Apex**, the most advanced automated IPTV engine on GitHub. This is not just a playlist merger — it is a full **enterprise-grade IPTV platform** that runs entirely on free infrastructure.

**What you get for $0.00:**
- 🌍 **15,000+ channels** from 250+ countries, auto-updated 4 times per day
- 🚀 **Serverless Xtream Codes API** — connect OTT Navigator, TiviMate, Smarters with a username/password login
- 🎬 **VOD Engine** — automatic Movies & TV Series extraction from Xtream providers
- ⏪ **Catch-Up DVR** — 7-day time-shift for supported channels
- 🍿 **Netflix-Style Web Player** — watch live TV in your browser + fetch external playlists dynamically
- 📱 **Android Link Checker App** — verify M3U, Xtream, and MAC sources on your phone/tablet/TV
- 🛡️ **Dead Link Healing** — automatically removes dead channels and replaces them with backups
- 📺 **EPG TV Guide** — XMLTV program guide with category-tagged color grids
- 🔒 **Domain Ignore List** — skip checking trusted domains, auto-mark them as live

---

## ✨ V9 Apex Features

| Feature | Description |
| :--- | :--- |
| 🌍 **15,000+ Free Channels** | Aggregates 150+ playlist sources (iptv-org, Free-TV, community curated) across 250+ countries. Deep BD 🇧🇩 & India 🇮🇳 coverage. |
| 🍿 **Web Player** | A Netflix-style browser player (`/docs`). Watch live TV + dynamically fetch external M3U playlists from the web! |
| 🎬 **VOD Engine** | Scrapes Movies & TV Shows from your Xtream Codes providers into an offline JSON database. |
| ⏪ **Catch-Up TV** | Serverless DVR API. Rewind live TV up to 7 days backward in supported players. |
| 🔄 **Auto-Healing Router** | Groups identical channels. If one stream dies, the API seamlessly redirects to the backup. |
| 🧠 **Smart Tagger** | Fuzzy-logic AI auto-corrects messy channel names and injects official logos and EPG IDs. |
| 🎬 **Deep Codec Probe** | FFprobe extracts stream resolution and tags channels with `[4K]` or `[1080p]`. |
| ⚡ **Parallel Pipeline** | GitHub Actions runs merge, link-check, and EPG generation as parallel jobs for 4x speed. |
| 📱 **Android Link Checker** | Standalone app to verify M3U playlists, Xtream accounts, and MAC portals on any Android device. |
| 🔐 **Stealth Mode** | Keep your premium providers 100% hidden using GitHub Secrets + private repos. |
| 🛡️ **Domain Ignore List** | Skip checking trusted domains — auto-mark them as LIVE in both the pipeline and the Android app. |
| 🌐 **External Playlist Fetcher** | Web player can load any M3U URL from the internet on-the-fly with one-click country buttons. |

---

## 📺 Default Feed Links (Public — Free)

| Asset | Link |
| :--- | :--- |
| **All Channels (.m3u8)** | `https://shoumikbalasomu.github.io/Github-Auto-Combine/output/combine.m3u8` |
| **Live Channels (.m3u8)** | `https://shoumikbalasomu.github.io/Github-Auto-Combine/output/combine_live.m3u8` |
| **By Country (.m3u8)** | `https://shoumikbalasomu.github.io/Github-Auto-Combine/output/combine_by_country.m3u8` |
| **TV Guide / EPG (.xml)** | `https://shoumikbalasomu.github.io/Github-Auto-Combine/output/epg.xml` |

Just paste any of these links into your favorite IPTV player (VLC, OTT Navigator, TiviMate, etc.) and enjoy!

---

## 🚀 How to Deploy (100% Free — Stealth Mode)

To get the absolute most out of V9 Apex, deploy it using a **Private Fork + Vercel Serverless Architecture**. This completely hides your premium IPTV credentials from the public while giving you access to the ultra-fast Xtream Codes API.

**Everything is free:**
- GitHub Private Repos → Free
- GitHub Actions (2,000 min/month) → Free
- Vercel Hobby Tier → Free (no credit card required)

👉 **[Read the Ultimate Deployment Guide](./docs/DEPLOYMENT.md)** — Clone, inject secrets, and deploy in under 5 minutes.

---

## 📱 Android Link Checker App

A standalone Android app that lets you verify the health of your IPTV sources before adding them to your player.

### Features
- ✅ **M3U Playlist Checker** — Paste a URL, the app downloads it and checks every channel
- ✅ **Xtream Codes Checker** — Enter server/username/password to verify all live streams
- ✅ **MAC Portal Checker** — Enter portal URL + MAC address to verify Stalker handshake
- ✅ **Multiple Sources** — Add as many sources as you want of each type
- ✅ **Export** — Save only the working channels as a clean M3U8 file
- ✅ **All Android Devices** — Phones, Tablets (two-pane), and Android TV (Leanback)
- ✅ **Domain Ignore List** — Settings screen to manage trusted domains that skip checking
- ✅ **Premium Dark Theme** — Material Design 3 with purple/indigo accents

### How to Install
1. Open the `android/link-checker/` folder in Android Studio
2. Click **Run** to build and install on your device
3. Or download the pre-built APK from the [Releases](../../releases) page

---

## ➕ Adding Your Own Premium Providers

### Method 1: GitHub Secrets (Recommended)
Keep your credentials invisible:
1. Go to **Settings** → **Secrets and variables** → **Actions**
2. Create secret: `SECRET_PLAYLISTS`
3. Paste your Xtream Codes M3U URLs (one per line)

### Method 2: Vercel Middleware Bridges

**Stalker to M3U Bridge:**
`https://your-vercel.app/api/stalker?portal=http://provider.com/c/&mac=00:1A:79:XX:YY:ZZ`

**Xtream Codes Proxy:**
`https://your-vercel.app/api/xtream?server=http://provider.com:80&username=USER&password=PASS`

---

## 📖 MAC Portal (Stalker Middleware)

For older MAG 250/254 boxes, see our [MAC Portal Guide](./docs/MAC_PORTAL_GUIDE.md).

---

## 🏗️ Repository Architecture

```
├── 📁 input/          # Raw M3U playlist URLs + domain ignore list
├── 📁 scripts/        # Python engine (merge, check, EPG, VOD extraction)
├── 📁 api/            # Vercel Serverless Functions (Xtream API, Catchup, Play)
├── 📁 docs/           # Netflix-style Web Player + External Fetcher + Deployment Guide
├── 📁 android/        # Android Link Checker App (Kotlin + Compose) with Domain Ignore
├── 📁 output/         # Generated M3U8, EPG XML, and JSON databases
└── 📄 vercel.json     # API routing and CORS configuration
```

---

## 🔄 How the Pipeline Works

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│  Download &  │────▶│  Check Dead  │────▶│ Generate EPG│
│  Merge M3Us  │     │  Links + Tag │     │ + Deploy    │
│  (Job 1)     │     │  (Job 2)     │     │ (Job 3)     │
└─────────────┘     └──────────────┘     └─────────────┘
       │                    │                    │
  Runs 4x/day       2000 concurrent        Auto-commits
  Deduplicates       HTTP probes           to GitHub Pages
  Groups by country  FFprobe resolution     Serves via CDN
```

---

<div align="center">
  <p>Built with ❤️ and 🐍 Python + ☕ Kotlin. Licensed under the <strong>MIT License</strong>.</p>
  <p><strong>100% Free. 100% Automated. Enterprise Quality.</strong></p>
</div>
