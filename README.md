<div align="center">
  <img src="https://raw.githubusercontent.com/shoumikbalasomu/Github-Auto-Combine/main/docs/output/logo.png" width="120" alt="IPTV Auto-Combine Logo" onerror="this.src='https://cdn-icons-png.flaticon.com/512/3159/3159302.png'">
  <h1>🌟 IPTV Auto-Combine</h1>
  <p><em>A fully automated, incredibly fast, and smart IPTV playlist manager.</em></p>
  
  <p>
    <a href="https://github.com/shoumikbalasomu/Github-Auto-Combine/actions"><img src="https://github.com/shoumikbalasomu/Github-Auto-Combine/actions/workflows/merge-playlists.yml/badge.svg" alt="Pipeline Status"></a>
    <img src="https://img.shields.io/badge/Python-3.11+-blue.svg" alt="Python 3.11+">
    <img src="https://img.shields.io/badge/Asyncio-Powered-purple.svg" alt="Asyncio Powered">
    <img src="https://img.shields.io/github/license/shoumikbalasomu/Github-Auto-Combine" alt="License">
  </p>
</div>

<br>

Welcome to **IPTV Auto-Combine**, an automated engine running exclusively on GitHub Actions to collect, merge, deduplicate, and organize IPTV channels from across the globe into beautiful, clean playlists.

---

## ✨ Premium Features

| Feature | Description |
| :--- | :--- |
| ⚡ **Async I/O Processing** | Downloads and verifies thousands of channels in seconds using highly optimized `asyncio` and `aiohttp` pipelines. |
| 🌍 **Smart Geolocation** | Automatically identifies the channel's country of origin and organizes them neatly with country flags. |
| 🔍 **Dead-Link Removal** | Probes stream URLs and video resolutions using asynchronous `ffprobe` to remove dead links. |
| 🛡️ **Auto-Deduplication** | Identifies identical streams across different source playlists and keeps only the highest quality link. |
| 💎 **Beautiful Dashboard** | Comes with a sleek, interactive GitHub Pages dashboard for one-click playlist copying. |

---

## 📺 Your Playlist Links

Simply copy these links and paste them directly into your favorite IPTV player (e.g., *OTT Navigator*, *TiViMate*, *VLC*, *Kodi*).

| Playlist Type | Features | Direct URL |
| :--- | :--- | :--- |
| **All Channels** | Includes all merged channels. *(Fastest update, untested links)* | [`combine.m3u8`](https://shoumikbalasomu.github.io/Github-Auto-Combine/output/combine.m3u8) |
| **Live Channels Only** | Guaranteed working streams. *(Dead links removed)* | [`combine_live.m3u8`](https://shoumikbalasomu.github.io/Github-Auto-Combine/output/combine_live.m3u8) |

> **Note:** The `combine_live.m3u8` playlist is updated daily or whenever a manual workflow is triggered via the GitHub Actions tab.

---

## 🚀 Quick Setup Guide

### 1️⃣ Add Your Playlists
Want to use your own sources? 
1. Open the `input/playlists.txt` file in this repository.
2. Add your M3U/M3U8 URLs (one URL per line).
3. Commit the changes. 
*GitHub Actions will instantly spin up to process and merge your sources!*

### 2️⃣ Use in OTT Navigator
1. Navigate to **Settings** → **Providers** → **Add Provider**.
2. Select **"M3U Playlist"**.
3. Paste either the *All Channels* or *Live Channels* link from above.
4. Hit **Save**. The channels will load automatically grouped by country!

---

## 🏗️ Repository Architecture

- 📁 **`input/`** - Define your raw M3U playlist sources here (`playlists.txt`).
- 📁 **`scripts/`** - The high-performance Python engine (`asyncio` / `aiohttp`).
- 📁 **`docs/`** - The source code for the premium Glassmorphism GitHub Pages dashboard.
- 📁 **`output/`** - The final, auto-generated M3U8 files served to your players.
- 📁 **`android/`** - Native Android IPTV player (TV/Mobile/Tablet) tailored for these playlists.

---

<div align="center">
  <p>Built with ❤️ and 🐍 Python. Licensed under the <strong>MIT License</strong>.</p>
</div>
