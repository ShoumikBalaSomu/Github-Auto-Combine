#!/usr/bin/env python3
"""
Dead Link Checker — Checks IPTV stream URLs for availability.

Uses concurrent HEAD/GET requests to check if streams are alive.
Generates a 'combined_live.m3u' file containing only working channels.
"""

import os
import sys
import json
import time
import logging
import argparse
import subprocess
from typing import List, Tuple, Optional
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

try:
    import requests
except ImportError:
    print("ERROR: 'requests' package not found. Install with: pip install requests")
    sys.exit(1)

from merge_playlists import M3UParser, OutputGenerator, Channel, OUTPUT_DIR
from country_mapper import get_country_with_flag

# ─── Configuration ────────────────────────────────────────────────
LINK_TIMEOUT = 10  # seconds per link check
PROBE_TIMEOUT = 5  # seconds for ffprobe analysis
MAX_WORKERS = 10  # concurrent checks
MAX_CHANNELS_TO_CHECK = 5000  # limit to avoid excessive checking
USER_AGENT = "Mozilla/5.0 (IPTV-Link-Checker/1.0)"

# ─── Logging ──────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger("check_links")


def probe_resolution(url: str) -> Optional[int]:
    """Uses ffprobe to quickly extract the vertical resolution of the stream."""
    cmd = [
        "ffprobe",
        "-v", "error",
        "-select_streams", "v:0",
        "-show_entries", "stream=height",
        "-of", "csv=s=x:p=0",
        "-analyzeduration", "1000000",
        "-probesize", "1000000",
        "-timeout", "5000000", # 5 seconds in microseconds (for tcp/udp)
        url
    ]
    try:
        # Run ffprobe with a strict timeout
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=PROBE_TIMEOUT)
        output = result.stdout.strip()
        if output and output.isdigit():
            return int(output)
    except Exception:
        pass
    return None


def check_single_link(channel: Channel, do_probe: bool = False) -> Tuple[Channel, bool, str]:
    """
    Check if a single stream URL is alive.
    Returns (channel, is_alive, reason).
    """
    url = channel.url.strip()
    
    if not url:
        return channel, False, "empty URL"
    
    try:
        session = requests.Session()
        session.headers.update({
            'User-Agent': USER_AGENT,
            'Accept': '*/*',
        })
        
        # First try HEAD request (faster)
        is_alive = False
        reason = ""
        try:
            response = session.head(url, timeout=LINK_TIMEOUT, allow_redirects=True)
            if response.status_code < 400:
                is_alive = True
                reason = f"HEAD {response.status_code}"
        except requests.exceptions.RequestException:
            pass
        
        # Fallback to GET with stream
        if not is_alive:
            try:
                response = session.get(url, timeout=LINK_TIMEOUT, stream=True, allow_redirects=True)
                if response.status_code < 400:
                    chunk = next(response.iter_content(1024), None)
                    response.close()
                    if chunk:
                        is_alive = True
                        reason = f"GET {response.status_code}"
                    else:
                        return channel, False, f"GET {response.status_code} (no data)"
                else:
                    response.close()
                    return channel, False, f"HTTP {response.status_code}"
            except StopIteration:
                return channel, False, "no data in stream"
        
        # If alive and probing is enabled, extract resolution
        if is_alive and do_probe:
            res = probe_resolution(url)
            if res:
                # Add resolution tag if not already present
                if f"{res}p" not in channel.name.lower() and f"{res}i" not in channel.name.lower():
                    if res >= 2160:
                        tag = "4K"
                    else:
                        tag = f"{res}p"
                    channel.name = f"{channel.name} [{tag}]"
                    channel.extra_attrs["tvg-resolution"] = str(res)
                    # We also add an artificial bump to duration or something so deduplicator prefers it
                    # But actually we'll handle this in the deduplicator explicitly using extra_attrs
        
        return channel, is_alive, reason
        
    
    except requests.exceptions.Timeout:
        return channel, False, "timeout"
    except requests.exceptions.ConnectionError:
        return channel, False, "connection error"
    except requests.exceptions.TooManyRedirects:
        return channel, False, "too many redirects"
    except Exception as e:
        return channel, False, f"error: {str(e)[:50]}"


def check_links(channels: List[Channel], max_workers: int = MAX_WORKERS, do_probe: bool = False) -> Tuple[List[Channel], List[Channel]]:
    """
    Check all channel links concurrently.
    Returns (live_channels, dead_channels).
    """
    total = len(channels)
    
    if total > MAX_CHANNELS_TO_CHECK:
        log.warning(f"Too many channels ({total}). Checking first {MAX_CHANNELS_TO_CHECK} only.")
        channels = channels[:MAX_CHANNELS_TO_CHECK]
        total = len(channels)
    
    log.info(f"Checking {total} channels with {max_workers} workers. Probing: {do_probe}...")
    
    live_channels = []
    dead_channels = []
    checked = 0
    start_time = time.time()
    
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        futures = {executor.submit(check_single_link, ch, do_probe): ch for ch in channels}
        
        for future in as_completed(futures):
            checked += 1
            channel, is_alive, reason = future.result()
            
            if is_alive:
                live_channels.append(channel)
            else:
                dead_channels.append(channel)
            
            # Progress update every 50 channels
            if checked % 50 == 0 or checked == total:
                elapsed = time.time() - start_time
                rate = checked / elapsed if elapsed > 0 else 0
                log.info(
                    f"  Progress: {checked}/{total} "
                    f"({checked * 100 // total}%) "
                    f"| Live: {len(live_channels)} "
                    f"| Dead: {len(dead_channels)} "
                    f"| Rate: {rate:.1f}/s"
                )
    
    elapsed = time.time() - start_time
    log.info(f"\n✓ Link check complete in {elapsed:.1f}s")
    log.info(f"  Live: {len(live_channels)} | Dead: {len(dead_channels)}")
    
    return live_channels, dead_channels


def run_link_checker():
    """Run the link checker on the combined playlist."""
    log.info("=" * 60)
    log.info("IPTV Dead Link Checker")
    log.info("=" * 60)
    
    # Load the combined playlist
    combined_file = OUTPUT_DIR / "combined_all.m3u"
    if not combined_file.exists():
        log.error(f"Combined playlist not found: {combined_file}")
        log.error("Run merge_playlists.py first!")
        return
    
    log.info(f"\n📂 Loading: {combined_file}")
    with open(combined_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    channels = M3UParser.parse(content, source_url="combined")
    log.info(f"  Loaded {len(channels)} channels")
    
    # Check links
    log.info(f"\n🔍 Checking links (Probe enabled: {DO_PROBE})...")
    live_channels, dead_channels = check_links(channels, max_workers=MAX_WORKERS, do_probe=DO_PROBE)
    
    # Write live channels to combined_live.m3u
    log.info("\n📦 Writing live channels...")
    sorted_live = sorted(live_channels, key=lambda c: (c.country or "ZZZ", c.display_name.lower()))
    count = OutputGenerator.write_m3u(
        sorted_live,
        OUTPUT_DIR / "combined_live.m3u",
        use_country_group=True,
    )
    log.info(f"  ✓ combined_live.m3u: {count} live channels")
    
    # Update stats
    stats_file = OUTPUT_DIR / "stats.json"
    if stats_file.exists():
        with open(stats_file, 'r', encoding='utf-8') as f:
            stats = json.load(f)
    else:
        stats = {}
    
    stats['live_channels'] = len(live_channels)
    stats['dead_channels'] = len(dead_channels)
    stats['link_check_time'] = __import__('datetime').datetime.utcnow().isoformat() + "Z"
    
    with open(stats_file, 'w', encoding='utf-8') as f:
        json.dump(stats, f, indent=2, ensure_ascii=False)
    
    # Write dead links report
    dead_report = OUTPUT_DIR / "dead_links.txt"
    with open(dead_report, 'w', encoding='utf-8') as f:
        f.write(f"# Dead Links Report\n")
        f.write(f"# Generated: {stats['link_check_time']}\n")
        f.write(f"# Total dead: {len(dead_channels)}\n\n")
        for ch in dead_channels:
            f.write(f"{ch.display_name} | {ch.url}\n")
    
    log.info(f"  ✓ Dead links report: {dead_report}")
    
    # Summary
    log.info("\n" + "=" * 60)
    log.info("📊 LINK CHECK SUMMARY")
    log.info("=" * 60)
    log.info(f"  Total checked: {len(live_channels) + len(dead_channels)}")
    log.info(f"  ✅ Live: {len(live_channels)}")
    log.info(f"  ❌ Dead: {len(dead_channels)}")
    log.info(f"  Live rate: {len(live_channels) * 100 / max(1, len(live_channels) + len(dead_channels)):.1f}%")
    log.info("=" * 60)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="IPTV Dead Link Checker")
    parser.add_argument("--timeout", type=int, default=LINK_TIMEOUT, help="Timeout per link (seconds)")
    parser.add_argument("--workers", type=int, default=MAX_WORKERS, help="Concurrent workers")
    parser.add_argument("--probe", action="store_true", help="Enable ffprobe stream resolution probing")
    parser.add_argument("--verbose", "-v", action="store_true", help="Verbose logging")
    args = parser.parse_args()
    
    LINK_TIMEOUT = args.timeout
    MAX_WORKERS = args.workers
    DO_PROBE = args.probe
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    run_link_checker()
