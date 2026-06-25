#!/usr/bin/env python3
"""
EPG Generator — Generates an XMLTV EPG for the live channels.

This generator produces a lightweight XMLTV file containing 
24-hour programming blocks for all verified channels. This ensures 
that Premium IPTV players (Smarters, TiviMate) don't crash and 
can properly display the channel list in their EPG grids.
"""

import os
import sys
import logging
from datetime import datetime, timedelta, timezone

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from merge_playlists import M3UParser, OUTPUT_DIR

OUTPUT_EPG = OUTPUT_DIR / "epg.xml"

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger("generate_epg")

def get_live_channels() -> list:
    """Get a list of all channels present in the live playlist."""
    live_file = OUTPUT_DIR / "combine_live.m3u8"
    if not live_file.exists():
        log.error(f"Live playlist not found: {live_file}")
        return []

    with open(live_file, 'r', encoding='utf-8') as f:
        content = f.read()

    return M3UParser.parse(content, source_url="combined")

def generate_epg(channels: list):
    """Generate a placeholder EPG for all channels."""
    if not channels:
        log.warning("No channels found. Skipping EPG generation.")
        return

    log.info(f"Generating EPG for {len(channels)} channels...")
    
    now = datetime.now(timezone.utc)
    # Generate EPG for yesterday, today, and tomorrow
    start_time = (now - timedelta(days=1)).strftime("%Y%md%H%M%S +0000").replace('d', '')
    end_time = (now + timedelta(days=2)).strftime("%Y%md%H%M%S +0000").replace('d', '')

    try:
        with open(OUTPUT_EPG, 'w', encoding='utf-8') as f:
            f.write('<?xml version="1.0" encoding="UTF-8"?>\n')
            f.write('<!DOCTYPE tv SYSTEM "xmltv.dtd">\n')
            f.write('<tv generator-info-name="IPTV Auto-Combine V6 Omniverse" generator-info-url="https://github.com/shoumikbalasomu/Github-Auto-Combine">\n')
            
            # Write Channels
            for ch in channels:
                ch_id = ch.tvg_id or ch.name.replace(" ", "_")
                f.write(f'  <channel id="{ch_id}">\n')
                f.write(f'    <display-name>{ch.name}</display-name>\n')
                if ch.tvg_logo:
                    f.write(f'    <icon src="{ch.tvg_logo}" />\n')
                f.write(f'  </channel>\n')
                
            # Write Programming Placeholders
            for ch in channels:
                ch_id = ch.tvg_id or ch.name.replace(" ", "_")
                # Create a 72-hour continuous block
                start_str = (now - timedelta(days=1)).strftime("%Y%m%d000000 +0000")
                end_str = (now + timedelta(days=2)).strftime("%Y%m%d000000 +0000")
                
                f.write(f'  <programme start="{start_str}" stop="{end_str}" channel="{ch_id}">\n')
                f.write(f'    <title lang="en">{ch.name} Live Stream</title>\n')
                f.write(f'    <desc lang="en">Live 24/7 broadcast from {ch.name}. Managed by IPTV Auto-Combine V6 Omniverse.</desc>\n')
                if ch.group:
                    f.write(f'    <category lang="en">{ch.group}</category>\n')
                f.write(f'  </programme>\n')

            f.write('</tv>\n')
            
        log.info(f"✓ EPG Generation Complete!")
        log.info(f"  Saved to: {OUTPUT_EPG}")

    except Exception as e:
        log.error(f"Failed to generate EPG: {e}")

if __name__ == "__main__":
    log.info("=" * 60)
    log.info("📺 IPTV Electronic Program Guide (EPG) Generator")
    log.info("=" * 60)
    
    channels = get_live_channels()
    generate_epg(channels)
