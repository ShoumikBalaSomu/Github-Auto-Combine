# IPTV Auto-Combine

A fully automated IPTV playlist manager that collects, merges, deduplicates, and organizes IPTV channels by country. Powered by GitHub Actions and served via GitHub Pages.

## Features

- **Automated Collection**: Pulls IPTV M3U/M3U8 links from multiple sources
- **Intelligent Organization**: Removes default groups and categorizes channels by their actual country of origin
- **Deduplication**: Automatically removes duplicate channels, keeping the one with the best metadata
- **Dead Link Filtering**: Verifies streams and provides a `combined_live.m3u` of only working channels
- **Zero Cost**: Runs entirely on GitHub Actions within the free tier

## Setup Guide

### How to add your own playlists
1. Edit the `input/playlists.txt` file in this repository
2. Add your IPTV M3U/M3U8 URLs (one per line)
3. Commit and push your changes
4. GitHub Actions will automatically process the playlists and update the output!

### How to use with OTT Navigator
1. Go to your GitHub Pages dashboard (Link is available in the repository settings)
2. Copy the link for "All Channels (By Country)" or "Live Channels Only"
3. Open **OTT Navigator** on your device
4. Go to **Settings** → **Providers** → **Add Provider**
5. Select "M3U Playlist" and paste the link
6. Save and the channels will load, beautifully organized by country!

## Repository Structure

- `input/`: Contains `playlists.txt` where you define your sources
- `scripts/`: Python processing engine
- `docs/`: GitHub Pages dashboard (HTML/CSS)
- `output/`: Generated M3U files (auto-committed)
- `android/`: Native Android IPTV player (TV/Mobile/Tablet)

## Automated Updates
The repository uses GitHub Actions to automatically run the scripts:
- Once daily at midnight UTC
- Manually via the "Actions" tab
- Whenever `input/playlists.txt` is updated

## License
MIT License
