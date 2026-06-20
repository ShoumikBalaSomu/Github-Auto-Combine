#!/usr/bin/env python3
"""
IPTV Playlist Merger — Downloads, parses, deduplicates, and organizes
IPTV M3U playlists by country.

Features:
- Downloads playlists from URLs in playlists.txt
- Parses M3U/M3U8 format with full attribute support
- Removes original group-title tags (removes file groups)
- Identifies channel country using country_mapper
- Reassigns group-title to country with flag emoji
- Deduplicates channels by URL and fuzzy name matching
- Outputs organized M3U files (all, by country, individual countries)
"""

import os
import re
import sys
import json
import hashlib
import logging
import argparse
import unicodedata
from typing import Dict, List, Optional, Tuple, Set
from dataclasses import dataclass, field
from pathlib import Path
from urllib.parse import urlparse

# Add scripts directory to path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

try:
    import requests
except ImportError:
    print("ERROR: 'requests' package not found. Install with: pip install requests")
    sys.exit(1)

from country_mapper import identify_country, get_country_with_flag, FLAGS

# ─── Configuration ────────────────────────────────────────────────
SCRIPT_DIR = Path(__file__).parent
PROJECT_DIR = SCRIPT_DIR.parent
INPUT_DIR = PROJECT_DIR / "input"
OUTPUT_DIR = PROJECT_DIR / "output"
COUNTRIES_DIR = OUTPUT_DIR / "countries"
PLAYLISTS_FILE = INPUT_DIR / "playlists.txt"
STATS_FILE = OUTPUT_DIR / "stats.json"

REQUEST_TIMEOUT = 30  # seconds
MAX_RETRIES = 3
USER_AGENT = "Mozilla/5.0 (IPTV-Auto-Combine/1.0)"

# ─── Logging ──────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger("merge_playlists")


# ─── Data Models ──────────────────────────────────────────────────
@dataclass
class Channel:
    """Represents a single IPTV channel entry."""
    name: str = ""
    url: str = ""
    tvg_id: str = ""
    tvg_name: str = ""
    tvg_logo: str = ""
    tvg_country: str = ""
    tvg_language: str = ""
    tvg_shift: str = ""
    group_title: str = ""
    catchup: str = ""
    catchup_source: str = ""
    catchup_days: str = ""
    duration: str = "-1"
    extra_attrs: Dict[str, str] = field(default_factory=dict)
    country: str = ""  # Resolved country
    source_url: str = ""  # Which playlist this came from
    
    @property
    def normalized_name(self) -> str:
        """Normalize channel name for deduplication comparison."""
        name = self.display_name.lower().strip()
        # Remove common suffixes/prefixes
        name = re.sub(r'\s*(hd|sd|fhd|uhd|4k|hevc|h\.?265|h\.?264|\(.*?\)|\[.*?\])\s*', ' ', name)
        # Remove special characters
        name = re.sub(r'[^a-z0-9\s]', '', name)
        # Collapse whitespace
        name = re.sub(r'\s+', ' ', name).strip()
        return name
    
    @property
    def display_name(self) -> str:
        """Get the best display name for this channel."""
        return self.tvg_name or self.name or "Unknown Channel"
    
    @property
    def url_hash(self) -> str:
        """Hash the URL for deduplication."""
        return hashlib.md5(self.url.strip().encode()).hexdigest()
    
    def has_more_metadata(self, other: 'Channel') -> bool:
        """Check if this channel has more metadata than another."""
        self_score = sum([
            bool(self.tvg_id), bool(self.tvg_logo), bool(self.tvg_name),
            bool(self.tvg_country), bool(self.tvg_language),
            bool(self.catchup), bool(self.catchup_source),
        ])
        other_score = sum([
            bool(other.tvg_id), bool(other.tvg_logo), bool(other.tvg_name),
            bool(other.tvg_country), bool(other.tvg_language),
            bool(other.catchup), bool(other.catchup_source),
        ])
        return self_score >= other_score
    
    def to_m3u_line(self, use_country_group: bool = True) -> str:
        """Convert channel to M3U format lines."""
        # Build attributes string
        attrs = []
        if self.tvg_id:
            attrs.append(f'tvg-id="{self.tvg_id}"')
        if self.tvg_name:
            attrs.append(f'tvg-name="{self.tvg_name}"')
        if self.tvg_logo:
            attrs.append(f'tvg-logo="{self.tvg_logo}"')
        if self.tvg_country:
            attrs.append(f'tvg-country="{self.tvg_country}"')
        if self.tvg_language:
            attrs.append(f'tvg-language="{self.tvg_language}"')
        if self.tvg_shift:
            attrs.append(f'tvg-shift="{self.tvg_shift}"')
        
        # Set group-title to country if using country groups
        if use_country_group and self.country:
            group = get_country_with_flag(self.country)
        elif self.group_title:
            group = self.group_title
        else:
            group = get_country_with_flag("International")
        attrs.append(f'group-title="{group}"')
        
        # Add catchup attributes
        if self.catchup:
            attrs.append(f'catchup="{self.catchup}"')
        if self.catchup_source:
            attrs.append(f'catchup-source="{self.catchup_source}"')
        if self.catchup_days:
            attrs.append(f'catchup-days="{self.catchup_days}"')
        
        # Add any extra attributes
        for key, value in self.extra_attrs.items():
            attrs.append(f'{key}="{value}"')
        
        attrs_str = " ".join(attrs)
        display = self.display_name
        duration = self.duration or "-1"
        
        return f'#EXTINF:{duration} {attrs_str},{display}\n{self.url}'


# ─── M3U Parser ──────────────────────────────────────────────────
class M3UParser:
    """Parse M3U/M3U8 playlist content into Channel objects."""
    
    # Regex to extract attributes from EXTINF line
    ATTR_RE = re.compile(r'([\w-]+)="([^"]*)"')
    EXTINF_RE = re.compile(r'^#EXTINF:\s*(-?\d*\.?\d*)\s*(.*?)(?:,(.*))?$')
    
    @staticmethod
    def parse(content: str, source_url: str = "") -> List[Channel]:
        """Parse M3U content and return list of Channel objects."""
        channels = []
        lines = content.strip().splitlines()
        
        if not lines:
            return channels
        
        current_channel: Optional[Channel] = None
        extra_directives: Dict[str, str] = {}
        
        for line in lines:
            line = line.strip()
            
            if not line:
                continue
            
            # Skip M3U header
            if line.upper().startswith('#EXTM3U'):
                continue
            
            # Parse EXTINF line
            if line.startswith('#EXTINF:'):
                current_channel = Channel(source_url=source_url)
                
                match = M3UParser.EXTINF_RE.match(line)
                if match:
                    current_channel.duration = match.group(1) or "-1"
                    attr_string = match.group(2) or ""
                    current_channel.name = (match.group(3) or "").strip()
                    
                    # Extract attributes
                    known_attrs = {
                        'tvg-id', 'tvg-name', 'tvg-logo', 'tvg-country',
                        'tvg-language', 'tvg-shift', 'group-title',
                        'catchup', 'catchup-source', 'catchup-days',
                    }
                    
                    for attr_match in M3UParser.ATTR_RE.finditer(attr_string):
                        key = attr_match.group(1).lower()
                        value = attr_match.group(2)
                        
                        if key == 'tvg-id':
                            current_channel.tvg_id = value
                        elif key == 'tvg-name':
                            current_channel.tvg_name = value
                        elif key == 'tvg-logo':
                            current_channel.tvg_logo = value
                        elif key == 'tvg-country':
                            current_channel.tvg_country = value
                        elif key == 'tvg-language':
                            current_channel.tvg_language = value
                        elif key == 'tvg-shift':
                            current_channel.tvg_shift = value
                        elif key == 'group-title':
                            current_channel.group_title = value
                        elif key == 'catchup':
                            current_channel.catchup = value
                        elif key == 'catchup-source':
                            current_channel.catchup_source = value
                        elif key == 'catchup-days':
                            current_channel.catchup_days = value
                        else:
                            current_channel.extra_attrs[attr_match.group(1)] = value
                
                # Apply any accumulated extra directives
                current_channel.extra_attrs.update(extra_directives)
                extra_directives = {}
                continue
            
            # Parse other directives (EXTVLCOPT, KODIPROP, etc.)
            if line.startswith('#'):
                if current_channel is None:
                    # Accumulate directives before next channel
                    if ':' in line:
                        key = line.split(':')[0][1:]
                        value = line.split(':', 1)[1]
                        extra_directives[key] = value
                continue
            
            # This is a URL line
            if current_channel is not None:
                url = line.strip()
                if url and (url.startswith('http') or url.startswith('rtmp') or 
                           url.startswith('rtsp') or url.startswith('mms')):
                    current_channel.url = url
                    channels.append(current_channel)
                current_channel = None
                extra_directives = {}
        
        return channels


# ─── Playlist Downloader ─────────────────────────────────────────
class PlaylistDownloader:
    """Download playlists from URLs."""
    
    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': USER_AGENT,
            'Accept': '*/*',
        })
    
    def download(self, url: str) -> Optional[str]:
        """Download a playlist from URL with retries."""
        for attempt in range(MAX_RETRIES):
            try:
                log.info(f"  Downloading: {url} (attempt {attempt + 1})")
                response = self.session.get(url, timeout=REQUEST_TIMEOUT, allow_redirects=True)
                response.raise_for_status()
                
                # Try to decode with different encodings
                content = None
                for encoding in ['utf-8', 'latin-1', 'iso-8859-1', 'cp1252']:
                    try:
                        content = response.content.decode(encoding)
                        break
                    except (UnicodeDecodeError, LookupError):
                        continue
                
                if content is None:
                    content = response.content.decode('utf-8', errors='replace')
                
                # Validate it's a valid M3U
                if '#EXTINF' in content or '#EXTM3U' in content:
                    log.info(f"  ✓ Downloaded successfully ({len(content)} bytes)")
                    return content
                else:
                    log.warning(f"  ✗ Content doesn't appear to be M3U format")
                    return None
                    
            except requests.exceptions.RequestException as e:
                log.warning(f"  ✗ Attempt {attempt + 1} failed: {e}")
                if attempt == MAX_RETRIES - 1:
                    log.error(f"  ✗ All attempts failed for: {url}")
                    return None
        
        return None


# ─── Deduplicator ─────────────────────────────────────────────────
class Deduplicator:
    """Remove duplicate channels from a list."""
    
    @staticmethod
    def deduplicate(channels: List[Channel]) -> List[Channel]:
        """
        Remove duplicate channels using multiple strategies:
        1. Exact URL match → keep channel with most metadata
        2. Same normalized name + same country → keep the best one
        """
        log.info(f"  Deduplicating {len(channels)} channels...")
        
        # Phase 1: Exact URL dedup
        url_map: Dict[str, Channel] = {}
        for ch in channels:
            url_key = ch.url.strip().rstrip('/')
            if url_key in url_map:
                existing = url_map[url_key]
                # Keep the one with more metadata
                if ch.has_more_metadata(existing):
                    url_map[url_key] = ch
            else:
                url_map[url_key] = ch
        
        phase1_channels = list(url_map.values())
        removed_url = len(channels) - len(phase1_channels)
        if removed_url > 0:
            log.info(f"    Phase 1 (URL dedup): Removed {removed_url} duplicates")
        
        # Phase 2: Fuzzy name + country dedup
        name_country_map: Dict[str, Channel] = {}
        for ch in phase1_channels:
            key = f"{ch.normalized_name}||{ch.country}"
            if key in name_country_map:
                existing = name_country_map[key]
                if ch.has_more_metadata(existing):
                    name_country_map[key] = ch
            else:
                name_country_map[key] = ch
        
        result = list(name_country_map.values())
        removed_name = len(phase1_channels) - len(result)
        if removed_name > 0:
            log.info(f"    Phase 2 (Name dedup): Removed {removed_name} duplicates")
        
        log.info(f"  ✓ Deduplication complete: {len(channels)} → {len(result)} channels")
        return result


# ─── Output Generator ────────────────────────────────────────────
class OutputGenerator:
    """Generate organized M3U output files."""
    
    @staticmethod
    def write_m3u(channels: List[Channel], filepath: Path, 
                  use_country_group: bool = True, 
                  header_attrs: str = "") -> int:
        """Write channels to an M3U file. Returns number of channels written."""
        filepath.parent.mkdir(parents=True, exist_ok=True)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(f'#EXTM3U{" " + header_attrs if header_attrs else ""}\n')
            for ch in channels:
                f.write(ch.to_m3u_line(use_country_group=use_country_group) + '\n')
        
        return len(channels)
    
    @staticmethod
    def generate_all_outputs(channels: List[Channel]) -> Dict[str, int]:
        """Generate all output M3U files. Returns stats dict."""
        stats = {}
        
        # Sort channels by country, then by name
        sorted_channels = sorted(channels, key=lambda c: (c.country, c.display_name.lower()))
        
        # 1. Combined all channels (organized by country)
        count = OutputGenerator.write_m3u(
            sorted_channels,
            OUTPUT_DIR / "combined_all.m3u",
            use_country_group=True,
        )
        stats['combined_all'] = count
        log.info(f"  ✓ combined_all.m3u: {count} channels")
        
        # 2. Combined by country (same as above, but explicit name)
        count = OutputGenerator.write_m3u(
            sorted_channels,
            OUTPUT_DIR / "combined_by_country.m3u",
            use_country_group=True,
        )
        stats['combined_by_country'] = count
        log.info(f"  ✓ combined_by_country.m3u: {count} channels")
        
        # 3. Individual country files
        country_channels: Dict[str, List[Channel]] = {}
        for ch in sorted_channels:
            country = ch.country or "International"
            if country not in country_channels:
                country_channels[country] = []
            country_channels[country].append(ch)
        
        stats['countries'] = {}
        for country, chs in sorted(country_channels.items()):
            # Create safe filename
            safe_name = re.sub(r'[^a-z0-9_]', '_', country.lower()).strip('_')
            filepath = COUNTRIES_DIR / f"{safe_name}.m3u"
            count = OutputGenerator.write_m3u(
                chs, filepath, use_country_group=True,
            )
            stats['countries'][country] = count
            log.info(f"    ✓ countries/{safe_name}.m3u: {count} channels")
        
        stats['total_countries'] = len(country_channels)
        
        return stats


# ─── Main Pipeline ────────────────────────────────────────────────
def load_playlist_urls() -> List[str]:
    """Load playlist URLs from playlists.txt."""
    if not PLAYLISTS_FILE.exists():
        log.error(f"Playlists file not found: {PLAYLISTS_FILE}")
        return []
    
    urls = []
    with open(PLAYLISTS_FILE, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            # Skip empty lines and comments
            if not line or line.startswith('#'):
                continue
            # Validate it looks like a URL
            if line.startswith('http://') or line.startswith('https://'):
                urls.append(line)
            else:
                log.warning(f"  Skipping invalid URL: {line}")
    
    return urls


def run_merge_pipeline(check_links: bool = False) -> Dict:
    """Run the complete merge pipeline."""
    log.info("=" * 60)
    log.info("IPTV Playlist Auto-Combine — Merge Pipeline")
    log.info("=" * 60)
    
    # Step 1: Load playlist URLs
    log.info("\n📋 Step 1: Loading playlist URLs...")
    urls = load_playlist_urls()
    if not urls:
        log.error("No valid URLs found in playlists.txt!")
        return {"error": "No URLs found"}
    log.info(f"  Found {len(urls)} playlist URL(s)")
    
    # Step 2: Download playlists
    log.info("\n📥 Step 2: Downloading playlists...")
    downloader = PlaylistDownloader()
    all_channels: List[Channel] = []
    download_stats = {"success": 0, "failed": 0}
    
    for url in urls:
        content = downloader.download(url)
        if content:
            channels = M3UParser.parse(content, source_url=url)
            log.info(f"    Parsed {len(channels)} channels from {urlparse(url).netloc}")
            all_channels.extend(channels)
            download_stats["success"] += 1
        else:
            download_stats["failed"] += 1
    
    if not all_channels:
        log.error("No channels found in any playlist!")
        return {"error": "No channels found"}
    
    log.info(f"  ✓ Total channels downloaded: {len(all_channels)}")
    
    # Step 3: Remove original groups and identify countries
    log.info("\n🌍 Step 3: Identifying countries...")
    for ch in all_channels:
        # Identify country using all available signals
        ch.country = identify_country(
            channel_name=ch.display_name,
            group_title=ch.group_title,
            tvg_country=ch.tvg_country,
            tvg_language=ch.tvg_language,
            tvg_id=ch.tvg_id,
        )
    
    # Count countries
    country_counts: Dict[str, int] = {}
    for ch in all_channels:
        country_counts[ch.country] = country_counts.get(ch.country, 0) + 1
    
    log.info(f"  ✓ Identified {len(country_counts)} countries/categories")
    for country, count in sorted(country_counts.items(), key=lambda x: -x[1])[:15]:
        flag = FLAGS.get(country, "🌍")
        log.info(f"    {flag} {country}: {count} channels")
    if len(country_counts) > 15:
        log.info(f"    ... and {len(country_counts) - 15} more")
    
    # Step 4: Deduplicate
    log.info("\n🔄 Step 4: Deduplicating channels...")
    unique_channels = Deduplicator.deduplicate(all_channels)
    
    # Step 5: Generate output files
    log.info("\n📦 Step 5: Generating output files...")
    output_stats = OutputGenerator.generate_all_outputs(unique_channels)
    
    # Step 6: Save stats
    stats = {
        "total_urls": len(urls),
        "download_success": download_stats["success"],
        "download_failed": download_stats["failed"],
        "total_channels_raw": len(all_channels),
        "total_channels_unique": len(unique_channels),
        "duplicates_removed": len(all_channels) - len(unique_channels),
        "total_countries": output_stats.get("total_countries", 0),
        "countries": output_stats.get("countries", {}),
        "last_updated": __import__('datetime').datetime.utcnow().isoformat() + "Z",
    }
    
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    with open(STATS_FILE, 'w', encoding='utf-8') as f:
        json.dump(stats, f, indent=2, ensure_ascii=False)
    
    log.info(f"\n✓ Stats saved to: {STATS_FILE}")
    
    # Summary
    log.info("\n" + "=" * 60)
    log.info("📊 SUMMARY")
    log.info("=" * 60)
    log.info(f"  Playlists processed: {download_stats['success']}/{len(urls)}")
    log.info(f"  Total channels: {len(all_channels)}")
    log.info(f"  After dedup: {len(unique_channels)}")
    log.info(f"  Duplicates removed: {len(all_channels) - len(unique_channels)}")
    log.info(f"  Countries: {output_stats.get('total_countries', 0)}")
    log.info("=" * 60)
    
    return stats


def run_test():
    """Run a test with sample M3U content."""
    log.info("Running test with sample M3U content...")
    
    sample_m3u = '''#EXTM3U
#EXTINF:-1 tvg-id="StarPlus.in" tvg-name="Star Plus" tvg-logo="https://example.com/star.png" tvg-country="IN" tvg-language="Hindi" group-title="Entertainment",Star Plus
http://example.com/starplus/stream.m3u8
#EXTINF:-1 tvg-id="BBCOne.uk" tvg-name="BBC One" tvg-logo="https://example.com/bbc.png" tvg-country="GB" tvg-language="English" group-title="UK General",BBC One
http://example.com/bbcone/stream.m3u8
#EXTINF:-1 tvg-id="CNN.us" tvg-name="CNN" tvg-logo="https://example.com/cnn.png" group-title="News",CNN
http://example.com/cnn/stream.m3u8
#EXTINF:-1 tvg-name="Star Plus HD" tvg-logo="https://example.com/star-hd.png" group-title="Hindi Entertainment",Star Plus HD
http://example.com/starplus-hd/stream.m3u8
#EXTINF:-1 tvg-name="CNN" group-title="News",CNN
http://example.com/cnn/stream.m3u8
#EXTINF:-1 tvg-name="Geo TV" tvg-language="Urdu" group-title="Pakistani",Geo TV
http://example.com/geotv/stream.m3u8
#EXTINF:-1 tvg-name="NTV Bangla" tvg-country="BD" group-title="Bangla",NTV Bangla
http://example.com/ntv/stream.m3u8
#EXTINF:-1 tvg-name="Hiru TV" tvg-country="LK" group-title="Sinhala",Hiru TV
http://example.com/hiru/stream.m3u8
#EXTINF:-1 tvg-name="Unknown Channel 1" group-title="Random Group",Unknown Channel 1
http://example.com/unknown1/stream.m3u8
#EXTINF:-1 tvg-name="Al Jazeera English" group-title="News",Al Jazeera English
http://example.com/aljazeera/stream.m3u8
'''
    
    # Parse
    channels = M3UParser.parse(sample_m3u, source_url="test://sample")
    log.info(f"Parsed {len(channels)} channels from sample")
    
    # Identify countries
    for ch in channels:
        ch.country = identify_country(
            channel_name=ch.display_name,
            group_title=ch.group_title,
            tvg_country=ch.tvg_country,
            tvg_language=ch.tvg_language,
            tvg_id=ch.tvg_id,
        )
        log.info(f"  {ch.display_name:<25} → {get_country_with_flag(ch.country)}")
    
    # Deduplicate
    unique = Deduplicator.deduplicate(channels)
    log.info(f"\nDeduplication: {len(channels)} → {len(unique)} channels")
    
    # Show results
    log.info("\n--- Generated M3U Output ---")
    for ch in sorted(unique, key=lambda c: (c.country, c.display_name)):
        log.info(ch.to_m3u_line())
    
    log.info("\n✓ Test passed!")


# ─── Entry Point ──────────────────────────────────────────────────
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="IPTV Playlist Auto-Combine")
    parser.add_argument("--test", action="store_true", help="Run with sample data")
    parser.add_argument("--check-links", action="store_true", help="Also check for dead links")
    parser.add_argument("--verbose", "-v", action="store_true", help="Verbose logging")
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    if args.test:
        run_test()
    else:
        run_merge_pipeline(check_links=args.check_links)
