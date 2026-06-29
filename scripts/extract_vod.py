#!/usr/bin/env python3
import os
import sys
import json
import logging
import asyncio
import aiohttp
from pathlib import Path

# Setup paths and logging
OUTPUT_DIR = Path(__file__).parent.parent / "output"
PLAYLISTS_FILE = Path(__file__).parent.parent / "input" / "playlists.txt"

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger("extract_vod")

USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

def get_xtream_credentials():
    urls = []
    if PLAYLISTS_FILE.exists():
        with open(PLAYLISTS_FILE, 'r') as f:
            for line in f:
                if line.strip() and not line.startswith('#'):
                    urls.append(line.strip())
                    
    secret_urls = os.environ.get('SECRET_PLAYLISTS', '')
    if secret_urls:
        for line in secret_urls.split('\n'):
            if line.strip() and not line.startswith('#'):
                urls.append(line.strip())
                
    creds = []
    for url in urls:
        if 'get.php' in url and 'username=' in url and 'password=' in url:
            from urllib.parse import urlparse, parse_qs
            parsed = urlparse(url)
            base = f"{parsed.scheme}://{parsed.netloc}"
            qs = parse_qs(parsed.query)
            if 'username' in qs and 'password' in qs:
                creds.append({
                    "server": base,
                    "username": qs['username'][0],
                    "password": qs['password'][0]
                })
    return creds

async def extract_vods(session, cred):
    movies = []
    series = []
    server = cred['server']
    u = cred['username']
    p = cred['password']
    
    log.info(f"Extracting VODs from {server}")
    try:
        # Get Movies
        cats_res = await session.get(f"{server}/player_api.php?username={u}&password={p}&action=get_vod_categories")
        cats = await cats_res.json()
        if isinstance(cats, list):
            for cat in cats[:5]: # limit to first 5 categories to avoid massive payload during testing
                streams_res = await session.get(f"{server}/player_api.php?username={u}&password={p}&action=get_vod_streams&category_id={cat['category_id']}")
                streams = await streams_res.json()
                if isinstance(streams, list):
                    for stream in streams[:20]: # limit to 20 per category
                        movies.append({
                            "id": stream.get('stream_id'),
                            "name": stream.get('name'),
                            "logo": stream.get('stream_icon'),
                            "rating": stream.get('rating'),
                            "group": cat.get('category_name'),
                            "type": "movie",
                            "url": f"../api/xtream?server={server}&username={u}&password={p}&stream={stream.get('stream_id')}.{stream.get('container_extension', 'mp4')}"
                        })
    except Exception as e:
        log.warning(f"Failed to fetch movies from {server}: {e}")

    try:
        # Get Series
        cats_res = await session.get(f"{server}/player_api.php?username={u}&password={p}&action=get_series_categories")
        cats = await cats_res.json()
        if isinstance(cats, list):
            for cat in cats[:2]:
                streams_res = await session.get(f"{server}/player_api.php?username={u}&password={p}&action=get_series&category_id={cat['category_id']}")
                streams = await streams_res.json()
                if isinstance(streams, list):
                    for stream in streams[:10]:
                        series.append({
                            "id": stream.get('series_id'),
                            "name": stream.get('name'),
                            "logo": stream.get('cover'),
                            "rating": stream.get('rating'),
                            "group": cat.get('category_name'),
                            "type": "series",
                            "url": "" # series playback requires seasons/episodes logic, so we just list them for now
                        })
    except Exception as e:
        log.warning(f"Failed to fetch series from {server}: {e}")

    return movies, series

async def main():
    log.info("Starting V8 Titan VOD Scraper")
    creds = get_xtream_credentials()
    
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    
    if not creds:
        log.info("No Xtream Codes providers found. Creating empty VOD databases to prevent API crashes.")
        with open(OUTPUT_DIR / "movies.json", 'w', encoding='utf-8') as f: json.dump([], f)
        with open(OUTPUT_DIR / "series.json", 'w', encoding='utf-8') as f: json.dump([], f)
        return
    
    all_movies = []
    all_series = []
    
    async with aiohttp.ClientSession(headers={"User-Agent": USER_AGENT}) as session:
        tasks = [extract_vods(session, c) for c in creds]
        results = await asyncio.gather(*tasks)
        for m, s in results:
            all_movies.extend(m)
            all_series.extend(s)
            
    # Save to JSON
    with open(OUTPUT_DIR / 'movies.json', 'w') as f:
        json.dump(all_movies, f, indent=2)
    with open(OUTPUT_DIR / 'series.json', 'w') as f:
        json.dump(all_series, f, indent=2)
        
    log.info(f"VOD Extraction Complete: {len(all_movies)} movies, {len(all_series)} series.")

if __name__ == "__main__":
    asyncio.run(main())
