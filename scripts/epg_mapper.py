#!/usr/bin/env python3
"""
EPG Mapper — Intelligently maps channel names to official EPG tvg-ids.

Fetches the iptv-org channels database and provides a fast fuzzy-matching
lookup to assign the correct tvg-id to raw IPTV channels, enabling
rich TV Guide support in apps like OTT Navigator.
"""

import re
import json
import logging
from typing import Dict, Optional, List

try:
    import requests
except ImportError:
    pass

log = logging.getLogger("epg_mapper")

EPG_API_URL = "https://iptv-org.github.io/api/channels.json"
# We will build a mapping of normalized name -> tvg-id
_epg_map: Dict[str, str] = {}
_is_loaded = False


def normalize_name(name: str) -> str:
    """Normalize channel name for reliable matching."""
    if not name:
        return ""
    name = name.lower()
    # Remove common suffixes that confuse matching
    name = re.sub(r'\s*(hd|sd|fhd|uhd|4k|hevc|h\.?265|h\.?264|\(.*?\)|\[.*?\])\s*', ' ', name)
    # Remove all non-alphanumeric characters
    name = re.sub(r'[^a-z0-9]', '', name)
    return name


def load_epg_database() -> bool:
    """Download and build the EPG mapping index."""
    global _epg_map, _is_loaded
    if _is_loaded:
        return True
        
    try:
        log.info(f"Downloading EPG channel database from {EPG_API_URL}...")
        response = requests.get(EPG_API_URL, timeout=15)
        response.raise_for_status()
        channels_data = response.json()
        
        # Build the index
        for ch in channels_data:
            tvg_id = ch.get("id")
            name = ch.get("name")
            if tvg_id and name:
                norm_name = normalize_name(name)
                if norm_name:
                    # If multiple channels have same normalized name, keep the first one
                    if norm_name not in _epg_map:
                        _epg_map[norm_name] = tvg_id
                
                # Also index by alt_names if available
                alt_names = ch.get("alt_names", [])
                for alt in alt_names:
                    norm_alt = normalize_name(alt)
                    if norm_alt and norm_alt not in _epg_map:
                        _epg_map[norm_alt] = tvg_id
                        
        _is_loaded = True
        log.info(f"✓ EPG database loaded with {len(_epg_map)} channel mappings.")
        return True
    except Exception as e:
        log.error(f"Failed to load EPG database: {e}")
        return False


def get_tvg_id(channel_name: str) -> Optional[str]:
    """Find the official EPG tvg-id for a given channel name."""
    if not _is_loaded:
        load_epg_database()
        
    if not channel_name or not _is_loaded:
        return None
        
    norm_name = normalize_name(channel_name)
    return _epg_map.get(norm_name)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    load_epg_database()
    
    test_channels = [
        "Star Plus HD",
        "BBC One",
        "CNN International",
        "Al Jazeera English (1080p)",
        "Random Unknown Channel 123"
    ]
    
    for ch in test_channels:
        tvg_id = get_tvg_id(ch)
        print(f"{ch:<30} -> {tvg_id}")
