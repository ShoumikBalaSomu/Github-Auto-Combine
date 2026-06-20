#!/usr/bin/env python3
"""
Country Mapper — Maps IPTV channel names to their country of origin.

Uses a comprehensive database of channel name patterns, keywords, and
language hints to identify which country a channel belongs to.
"""

import re
from typing import Optional, Dict, List, Tuple

# Country flag emojis
FLAGS: Dict[str, str] = {
    "Afghanistan": "🇦🇫", "Albania": "🇦🇱", "Algeria": "🇩🇿", "Argentina": "🇦🇷",
    "Armenia": "🇦🇲", "Australia": "🇦🇺", "Austria": "🇦🇹", "Azerbaijan": "🇦🇿",
    "Bahrain": "🇧🇭", "Bangladesh": "🇧🇩", "Belarus": "🇧🇾", "Belgium": "🇧🇪",
    "Bolivia": "🇧🇴", "Bosnia": "🇧🇦", "Brazil": "🇧🇷", "Bulgaria": "🇧🇬",
    "Cambodia": "🇰🇭", "Cameroon": "🇨🇲", "Canada": "🇨🇦", "Chile": "🇨🇱",
    "China": "🇨🇳", "Colombia": "🇨🇴", "Costa Rica": "🇨🇷", "Croatia": "🇭🇷",
    "Cuba": "🇨🇺", "Cyprus": "🇨🇾", "Czech Republic": "🇨🇿", "Denmark": "🇩🇰",
    "Dominican Republic": "🇩🇴", "Ecuador": "🇪🇨", "Egypt": "🇪🇬", "El Salvador": "🇸🇻",
    "Estonia": "🇪🇪", "Ethiopia": "🇪🇹", "Finland": "🇫🇮", "France": "🇫🇷",
    "Georgia": "🇬🇪", "Germany": "🇩🇪", "Ghana": "🇬🇭", "Greece": "🇬🇷",
    "Guatemala": "🇬🇹", "Honduras": "🇭🇳", "Hong Kong": "🇭🇰", "Hungary": "🇭🇺",
    "Iceland": "🇮🇸", "India": "🇮🇳", "Indonesia": "🇮🇩", "Iran": "🇮🇷",
    "Iraq": "🇮🇶", "Ireland": "🇮🇪", "Israel": "🇮🇱", "Italy": "🇮🇹",
    "Jamaica": "🇯🇲", "Japan": "🇯🇵", "Jordan": "🇯🇴", "Kazakhstan": "🇰🇿",
    "Kenya": "🇰🇪", "Kosovo": "🇽🇰", "Kuwait": "🇰🇼", "Kyrgyzstan": "🇰🇬",
    "Laos": "🇱🇦", "Latvia": "🇱🇻", "Lebanon": "🇱🇧", "Libya": "🇱🇾",
    "Lithuania": "🇱🇹", "Luxembourg": "🇱🇺", "Macedonia": "🇲🇰", "Malaysia": "🇲🇾",
    "Maldives": "🇲🇻", "Malta": "🇲🇹", "Mexico": "🇲🇽", "Moldova": "🇲🇩",
    "Mongolia": "🇲🇳", "Montenegro": "🇲🇪", "Morocco": "🇲🇦", "Mozambique": "🇲🇿",
    "Myanmar": "🇲🇲", "Nepal": "🇳🇵", "Netherlands": "🇳🇱", "New Zealand": "🇳🇿",
    "Nicaragua": "🇳🇮", "Nigeria": "🇳🇬", "North Korea": "🇰🇵", "Norway": "🇳🇴",
    "Oman": "🇴🇲", "Pakistan": "🇵🇰", "Palestine": "🇵🇸", "Panama": "🇵🇦",
    "Paraguay": "🇵🇾", "Peru": "🇵🇪", "Philippines": "🇵🇭", "Poland": "🇵🇱",
    "Portugal": "🇵🇹", "Qatar": "🇶🇦", "Romania": "🇷🇴", "Russia": "🇷🇺",
    "Saudi Arabia": "🇸🇦", "Senegal": "🇸🇳", "Serbia": "🇷🇸", "Singapore": "🇸🇬",
    "Slovakia": "🇸🇰", "Slovenia": "🇸🇮", "Somalia": "🇸🇴", "South Africa": "🇿🇦",
    "South Korea": "🇰🇷", "Spain": "🇪🇸", "Sri Lanka": "🇱🇰", "Sudan": "🇸🇩",
    "Sweden": "🇸🇪", "Switzerland": "🇨🇭", "Syria": "🇸🇾", "Taiwan": "🇹🇼",
    "Tajikistan": "🇹🇯", "Tanzania": "🇹🇿", "Thailand": "🇹🇭", "Tunisia": "🇹🇳",
    "Turkey": "🇹🇷", "Turkmenistan": "🇹🇲", "UAE": "🇦🇪", "Uganda": "🇺🇬",
    "Ukraine": "🇺🇦", "United Kingdom": "🇬🇧", "USA": "🇺🇸", "Uruguay": "🇺🇾",
    "Uzbekistan": "🇺🇿", "Venezuela": "🇻🇪", "Vietnam": "🇻🇳", "Yemen": "🇾🇪",
    "Zambia": "🇿🇲", "Zimbabwe": "🇿🇼", "International": "🌍",
    "Sports": "⚽", "Music": "🎵", "Kids": "👶", "Movies": "🎬",
    "News": "📰", "Documentary": "📚", "Religious": "🕌",
    "Entertainment": "🎭", "Adult": "🔞",
}

# Comprehensive channel name patterns → country mapping
# Each entry: (compiled_regex_pattern, country_name)
CHANNEL_PATTERNS: List[Tuple[re.Pattern, str]] = []

# Raw pattern definitions: channel_keyword → country
_PATTERN_MAP: Dict[str, List[str]] = {
    # ─── India ────────────────────────────────────────────────────
    "India": [
        r"\bstar\s*(plus|bharat|gold|pravah|maa|vijay|suvarna|kiranmala)\b",
        r"\b(zee\s*(tv|cinema|marathi|kannada|bangla|tamil|telugu|punjabi|news|anmol|action|cafe|bollywood|classic|zest|keralam|sarthak|biskope|thirai|cinemalu|picchar|yuva))\b",
        r"\b(sony\s*(tv|sab|pal|max|pix|aath|marathi|yay|wah|six|ten))\b",
        r"\b(colors\s*(tv|bangla|marathi|kannada|tamil|gujarati|super|cineplex|infinity|rishtey)?)\b",
        r"\b(ndtv|aaj\s*tak|abp\s*news|republic\s*(tv|bharat)|india\s*tv|news\s*18|wion|dd\s*(national|news|india|bharati|sports|kisan|urdu|punjabi|rajasthan|sahyadri|podhigai|chandana|saptagiri|yadagiri|girnar|kashir))\b",
        r"\b(sun\s*(tv|news|music|life|neo)|kalaignar|jaya\s*(tv|plus)|polimer|puthuyugam|captain\s*tv|adithya\s*tv|vendhar|vasanth|news\s*j|thanthi|peppers)\b",
        r"\b(gemini\s*(tv|movies|comedy|life|music)|etv\s*(telugu|andhra)?|maa\s*(tv|movies|gold|music)|zee\s*telugu|t\s*news|ntv\s*telugu|tv5|tv9\s*telugu|6tv|hmtv|10tv|sakshi|v6\s*news|aha)\b",
        r"\b(asianet|surya\s*tv|flowers|mazhavil|kairali|amrita|media\s*one|janam\s*tv|mathrubhumi|manorama|safari|kiran|zee\s*keralam|shemaroo)\b",
        r"\b(star\s*jalsha|zee\s*bangla|colors\s*bangla|sony\s*aath|jalsha\s*movies|ruposhi\s*bangla|24\s*ghanta|kolkata\s*tv|sangeet\s*bangla|aakash\s*aath)\b",
        r"\b(star\s*pravah|zee\s*marathi|colors\s*marathi|sony\s*marathi|fakt\s*marathi|shemaroo\s*marathibana)\b",
        r"\b(ptc\s*punjabi|zee\s*punjabi|chardikla|9x\s*tashan|pitaara|balle\s*balle)\b",
        r"\b(news\s*nation|sahara|sanskar|aastha|peace\s*of\s*mind|travelxp|epic|zing|mtv\s*(india|beats)|vh1\s*india|9xm|9x\s*jalwa|b4u|set\s*max|uhd\s*india)\b",
        r"\b(hungama|nick\s*(india|jr|hd\+)?|sonic|pogo|cartoon\s*network\s*india|disney\s*(india|channel\s*india))\b",
        r"\bdishplay\b", r"\btataplay\b", r"\bhotstar\b", r"\bjiocinema\b",
    ],
    # ─── USA ──────────────────────────────────────────────────────
    "USA": [
        r"\b(cnn|fox\s*news|msnbc|cnbc|bloomberg|abc\s*news|cbs\s*news|nbc\s*news|pbs|c-?span|newsmax|oan|newsy)\b",
        r"\b(espn|fox\s*sports|nba\s*tv|nfl\s*network|mlb\s*network|nhl\s*network|golf\s*channel|tennis\s*channel|stadium|acc\s*network|big\s*ten|sec\s*network|pac-?12|bein\s*sports?\s*(usa|xtra)?)\b",
        r"\b(hbo|showtime|starz|cinemax|epix|tmc|fx|fxx|amc|bravo|e!|lifetime|oxygen|syfy|usa\s*network|tnt|tbs|truTV|comedy\s*central|paramount\s*network)\b",
        r"\b(abc|cbs|nbc|fox(?!\s*(sports\s*(asia|africa|uk))|life)|the\s*cw|mynetworktv|pbs|ion|bounce|grit|laff|start\s*tv|antenna\s*tv|cozi|me\s*tv|movies!)\b",
        r"\b(discovery|history|national\s*geographic|nat\s*geo|animal\s*planet|tlc|hgtv|food\s*network|travel\s*channel|investigation\s*discovery|science\s*channel|smithsonian|diy\s*network|magnolia|cooking\s*channel)\b",
        r"\b(disney\s*(channel|xd|junior)|cartoon\s*network|nick(?:elodeon)?|boomerang|universal\s*kids|baby\s*first|sprout)\b",
        r"\b(mtv(?!\s*india)|vh1(?!\s*india)|bet|cmt|tv\s*land|logo|pop)\b",
        r"\b(hallmark|ion|we\s*tv|own|lmn|tcm|fandango|reelz|fxm)\b",
        r"\b(a&e|vice|destination|newsmax|oan|weather\s*channel|qvc|hsn)\b",
        r"\bpluto\s*tv\b", r"\btubi\b",
    ],
    # ─── United Kingdom ───────────────────────────────────────────
    "United Kingdom": [
        r"\b(bbc\s*(one|two|three|four|alba|scotland|wales|news|parliament|iplayer|earth|brit)|cbbc|cbeebies)\b",
        r"\b(itv\s*(1|2|3|4|be)?|channel\s*(4|5)|film4|more4|e4|4seven|5\s*(usa|star|action|select)|dave|yesterday|drama|really|quest|dmax)\b",
        r"\b(sky\s*(one|two|atlantic|arts|comedy|crime|cinema|documentaries|history|max|nature|news|sports|witness|sci-?fi|premiere|showcase|greats|action|family)|sky(?!\s*(sports\s*(cricket|asia))))\b",
        r"\b(bt\s*sport|premier\s*sports|eurosport\s*uk|racing\s*tv|at\s*the\s*races|box\s*nation)\b",
        r"\b(gold|w|alibi|eden|blaze|challenge|food\s*network\s*uk|pick|sony\s*channel\s*uk|pbs\s*america|together|true\s*(crime|entertainment|movies))\b",
        r"\b(gb\s*news|talk\s*tv|lbc|times\s*radio)\b",
        r"\b(stv|s4c|ulster|grampian|anglia|meridian|carlton|central|border|tyne\s*tees|west\s*country|london\s*live)\b",
    ],
    # ─── Canada ───────────────────────────────────────────────────
    "Canada": [
        r"\b(cbc|ctv|global\s*tv|citytv|tvo|ici\s*tele|ici\s*rdi|tva|v\s*tele|noovo|aptn|omni|cpac)\b",
        r"\b(tsn|sportsnet|rds|tva\s*sports)\b",
        r"\b(crave|super\s*channel|hollywood\s*suite|stingray|much\s*music|w\s*network|oln|hgtv\s*canada|food\s*network\s*canada|slice|showcase|space|ytv|treehouse|family\s*channel|family\s*jr)\b",
        r"\b(cp24|ctv\s*news|cbc\s*news\s*network|bnn\s*bloomberg)\b",
    ],
    # ─── Germany ──────────────────────────────────────────────────
    "Germany": [
        r"\b(ard|zdf|das\s*erste|3sat|arte|phoenix|one|tagesschau24|kika|zdfneo|zdfinfo|ard-?alpha)\b",
        r"\b(rtl|rtl2|rtl\s*plus|vox|ntv|n-?tv|super\s*rtl|toggo\s*plus|nitro)\b",
        r"\b(sat\.?1|pro\s*sieben|pro7|kabel\s*eins|sixx|sat\.?1\s*gold|pro7\s*maxx|kabel\s*eins\s*doku)\b",
        r"\b(welt|n24|tele\s*5|dmax\s*de|sport1|eurosport\s*de|sky\s*sport\s*de|sky\s*de)\b",
        r"\b(br|hr|mdr|ndr|rbb|sr|swr|wdr|bayern|hessen)\b",
    ],
    # ─── France ───────────────────────────────────────────────────
    "France": [
        r"\b(tf1|france\s*(2|3|4|5|24|info|o)|arte\s*fr|m6|w9|tmc|tfx|nrj|cstar|gulli|canal\s*\+|c8|cnews|bfm|lci|rmc)\b",
        r"\b(france\s*tv|france\s*inter|rfi|tv5\s*monde)\b",
    ],
    # ─── Spain ────────────────────────────────────────────────────
    "Spain": [
        r"\b(la\s*(1|2|sexta)|antena\s*3|cuatro|telecinco|tve|clan|tdp|neox|nova|mega|fdf|energy|divinity|be\s*mad|dkiss|trece|rtve|atresplayer)\b",
        r"\b(movistar|dazn\s*spain|gol|bein\s*la\s*liga)\b",
        r"\b(tv3|canal\s*sur|telemadrid|etb|tvg|tv\s*canaria|aragon\s*tv|castilla)\b",
    ],
    # ─── Italy ────────────────────────────────────────────────────
    "Italy": [
        r"\b(rai\s*(1|2|3|4|5|gulp|yoyo|movie|premium|news|storia|scuola|sport)|mediaset|canale\s*5|italia\s*(1|2)|rete\s*4|la5|iris|cine34|twentyseven|focus|top\s*crime|giallo)\b",
        r"\b(sky\s*(tg24|sport\s*it|italia|cinema\s*it|uno\s*it)|dazn\s*it(aly)?)\b",
        r"\b(la7|tv8|nove|cielo|tv2000|boing|frisbee|k2|super!|laeffe|deejay|real\s*time\s*it)\b",
    ],
    # ─── Turkey ───────────────────────────────────────────────────
    "Turkey": [
        r"\b(trt\s*(1|haber|spor|belgesel|muzik|cocuk|world|avaz|kurdi)|show\s*tv|star\s*tv\s*tr|kanal\s*d|atv|fox\s*tv\s*tr|tv8\s*tr|tv360|haber\s*turk|cnn\s*turk|ntv\s*turk|a\s*haber|bloomberg\s*ht|tgrt)\b",
        r"\b(beyaz\s*tv|360|teve2|tlc\s*tr|dmax\s*tr|nat\s*geo\s*tr|discovery\s*tr|cartoon\s*network\s*tr)\b",
    ],
    # ─── Arab / Middle East ───────────────────────────────────────
    "Saudi Arabia": [
        r"\b(mbc\s*(1|2|3|4|5|action|drama|max|persia|bollywood|iraq)?|sbc|rotana\s*(cinema|classic|clip|comedy|drama|khalijia|kids|music)|al\s*arabiya|al\s*ekhbariya|ssc|shahid)\b",
    ],
    "UAE": [
        r"\b(abu\s*dhabi|dubai\s*(tv|sports|one|racing)|al\s*emarat|sharjah|ajman|sama\s*dubai)\b",
    ],
    "Qatar": [
        r"\b(al\s*jazeera|bein\s*sports?\s*\d*(?!\s*(usa|uk|fr|asia)))\b",
        r"\b(qatar\s*tv|al\s*rayyan)\b",
    ],
    "Egypt": [
        r"\b(cbc|on\s*tv|dmc|al\s*nahar|mehwar|sada\s*el\s*balad|nile\s*(tv|drama|cinema|comedy|family|life|sport|culture)|al\s*hayat|ten\s*tv|extra\s*news)\b",
    ],
    "Lebanon": [
        r"\b(lbc|mtv\s*lebanon|otv\s*lebanon|al\s*manar|al\s*jadeed|nbn|future\s*tv|tele\s*liban)\b",
    ],
    "Iraq": [
        r"\b(al\s*iraqiya|al\s*sharqiya|al\s*sumaria|dijlah|hona\s*baghdad|al\s*baghdadia|kurdsat|nrt|rudaw)\b",
    ],
    "Kuwait": [
        r"\b(kuwait\s*tv|al\s*rai\s*tv|scope|funoon)\b",
    ],
    "Jordan": [
        r"\b(jordan\s*tv|roya|al\s*mamlaka)\b",
    ],
    "Morocco": [
        r"\b(al\s*aoula|2m|medi\s*1|snrt|arryadia|tamazight|laayoune)\b",
    ],
    "Tunisia": [
        r"\b(watania|nessma|hannibal|tunisna|el\s*hiwar)\b",
    ],
    "Algeria": [
        r"\b(entv|canal\s*algerie|echorouk|ennahar|el\s*bilad|dzair|samira)\b",
    ],
    # ─── Pakistan ─────────────────────────────────────────────────
    "Pakistan": [
        r"\b(geo\s*(tv|news|entertainment|kahani)|ary\s*(digital|news|zindagi|qtv|musik)|hum\s*(tv|news|sitaray|masala)|express\s*(news|entertainment)|bol\s*(tv|news|entertainment)|dunya\s*news|samaa\s*tv|dawn\s*news|92\s*news|aaj\s*news|neo\s*news|abb\s*tak|24\s*news|capital\s*tv|such\s*tv|public\s*news|aplus|ptv\s*(home|news|sports|global)|urdu\s*1|tv\s*one\s*pk|a-?plus)\b",
    ],
    # ─── Bangladesh ───────────────────────────────────────────────
    "Bangladesh": [
        r"\b(bangla\s*vision|channel\s*i|ntv\s*(bangla|bd)|rtv|atv|ekushey|gazi\s*tv|independent|desh\s*tv|boishakhi|my\s*tv|jamuna\s*tv|maasranga|ekattor|news24\s*bd|somoy|deepto|channel\s*9|71\s*tv|btv|sa\s*tv|bijoy\s*tv)\b",
    ],
    # ─── Sri Lanka ────────────────────────────────────────────────
    "Sri Lanka": [
        r"\b(rupavahini|itn|swarnavahini|sirasa|hiru\s*tv|derana|siyatha|tv\s*derana|supreme\s*tv\s*sl|shakthi|vasantham\s*tv)\b",
    ],
    # ─── Nepal ────────────────────────────────────────────────────
    "Nepal": [
        r"\b(nepal\s*tv|kantipur|avenues|image|mountain|sagarmatha|himalaya|news24\s*np|abc\s*nepal|ap1)\b",
    ],
    # ─── Russia ───────────────────────────────────────────────────
    "Russia": [
        r"\b(perviy|rossiya|ntv\s*ru|ren\s*tv|sts|tnt|pyatnica|domashny|tv3\s*ru|friday|che|yu|muztv|russian|ru\s*tv|rtri?|match\s*tv|mir|spas|zvezda|muz-?tv)\b",
    ],
    # ─── Japan ────────────────────────────────────────────────────
    "Japan": [
        r"\b(nhk|fuji\s*tv|tbs\s*japan|tv\s*asahi|tv\s*tokyo|nippon|wowow|animax\s*jp|bs\s*(nittele|fuji|tbs|asahi|japan)|abema)\b",
    ],
    # ─── South Korea ──────────────────────────────────────────────
    "South Korea": [
        r"\b(kbs|mbc\s*korea|sbs\s*korea|tvn|jtbc|ocn|mbn|ytn|channel\s*a|tv\s*chosun|arirang|ebs)\b",
    ],
    # ─── China ────────────────────────────────────────────────────
    "China": [
        r"\b(cctv|cgtn|phoenix\s*(chinese|infonews|hong\s*kong)|dragon|hunan|zhejiang|jiangsu|beijing|shanghai|guangdong|shenzhen|sichuan|anhui)\b",
    ],
    # ─── Brazil ───────────────────────────────────────────────────
    "Brazil": [
        r"\b(globo|sbt|record|band|rede\s*tv|tv\s*cultura|globo\s*news|sportv|premiere|multishow|telecine|canal\s*brasil|gnt|viva|bis|off|megapix|discovery\s*br|nat\s*geo\s*br)\b",
    ],
    # ─── Mexico ───────────────────────────────────────────────────
    "Mexico": [
        r"\b(televisa|tv\s*azteca|canal\s*(5|de\s*las\s*estrellas)|azteca\s*(uno|7|noticias)|las\s*estrellas|nu9ve|foro\s*tv|milenio|imagen|once|canal\s*22|adn\s*40)\b",
    ],
    # ─── Portugal ─────────────────────────────────────────────────
    "Portugal": [
        r"\b(rtp\s*(1|2|3|internacional|memoria|africa)|sic|sic\s*(noticias|mulher|radical|k|caras)|tvi|tvi\s*(24|reality|ficção|player)|cmtv|porto\s*canal|sport\s*tv\s*pt)\b",
    ],
    # ─── Netherlands ──────────────────────────────────────────────
    "Netherlands": [
        r"\b(npo\s*(1|2|3)|rtl\s*(4|5|7|8|z|crime)|sbs\s*(6|9)|veronica|net\s*5|bvn|nos|ziggo|ons|24kitchen|fox\s*sports\s*nl|espn\s*nl)\b",
    ],
    # ─── Poland ───────────────────────────────────────────────────
    "Poland": [
        r"\b(tvp\s*(1|2|3|info|sport|historia|kultura|seriale|rozrywka)|polsat|tvn|tvn24|tv4\s*pl|tv\s*puls|super\s*polsat|canal\s*\+\s*sport\s*pl)\b",
    ],
    # ─── Romania ──────────────────────────────────────────────────
    "Romania": [
        r"\b(tvr\s*(1|2|3)|pro\s*tv|antena\s*(1|3|stars)|kanal\s*d\s*ro|prima\s*tv|b1\s*tv|realitatea|romania\s*tv|digi\s*(24|sport|world|life|film)|happy|kiss\s*tv)\b",
    ],
    # ─── Greece ───────────────────────────────────────────────────
    "Greece": [
        r"\b(ert\s*(1|2|3)|mega\s*gr|ant1|alpha\s*gr|skai|star\s*gr|open\s*tv|kontra|epsilon)\b",
    ],
    # ─── Sweden ───────────────────────────────────────────────────
    "Sweden": [
        r"\b(svt\s*(1|2)|tv3\s*se|tv4\s*se|tv6\s*se|kanal\s*(5|9|11)\s*se|tv12|sjuan|c\s*more)\b",
    ],
    # ─── Norway ───────────────────────────────────────────────────
    "Norway": [
        r"\b(nrk\s*(1|2|3)|tv2\s*no|tv\s*norge|tvnorge|viasat\s*no|max\s*no)\b",
    ],
    # ─── Denmark ──────────────────────────────────────────────────
    "Denmark": [
        r"\b(dr\s*(1|2)|tv2\s*dk|tv3\s*dk|tv2\s*charlie|tv2\s*news\s*dk|tv2\s*zulu|kanal\s*(4|5)\s*dk)\b",
    ],
    # ─── Finland ──────────────────────────────────────────────────
    "Finland": [
        r"\b(yle\s*(tv1|tv2|teema|fem)|mtv3|nelonen|sub|liv|jim|hero|frii|star\s*fi)\b",
    ],
    # ─── Thailand ─────────────────────────────────────────────────
    "Thailand": [
        r"\b(thai\s*(pbs|rath)|ch\s*(3|5|7|8)|one\s*31|gmm\s*25|workpoint|amarin|thairath|nation|mono\s*29|pptv|bright|mcot)\b",
    ],
    # ─── Vietnam ──────────────────────────────────────────────────
    "Vietnam": [
        r"\b(vtv\s*(1|2|3|4|5|6)|htv\s*(7|9)|thvl|vinh\s*long|vtc\s*(1|3|7|9|14)|vnpt|k\+|fpt|sctv)\b",
    ],
    # ─── Indonesia ────────────────────────────────────────────────
    "Indonesia": [
        r"\b(rcti|sctv\s*id|indosiar|antv|trans\s*(7|tv)|gtv|mnc|metro\s*tv\s*id|tvone\s*id|kompas\s*tv|net\s*tv|tvri)\b",
    ],
    # ─── Malaysia ─────────────────────────────────────────────────
    "Malaysia": [
        r"\b(rtm|tv\s*(1|2|3)\s*my|ntv7|8tv|tv9\s*my|astro|bernama|al\s*hijrah|warna|tv\s*okey)\b",
    ],
    # ─── Philippines ──────────────────────────────────────────────
    "Philippines": [
        r"\b(abs-?cbn|gma|tv5\s*ph|ptvph|net25|anc|cnn\s*ph|one\s*news|s\+a|knowledge\s*channel)\b",
    ],
    # ─── Ukraine ──────────────────────────────────────────────────
    "Ukraine": [
        r"\b(1\+1|inter\s*ua|ictv|stb|novy|ukraina|2\+2|plusplus|mega\s*ua|nlo|tonis|ua:?pershiy|espreso|pryamiy|5\s*kanal)\b",
    ],
    # ─── Iran ─────────────────────────────────────────────────────
    "Iran": [
        r"\b(irib|irinn|press\s*tv|al\s*alam|sahar|iran\s*(international|tv)|manoto|gem\s*(tv|movie|series|classic|kids|junior)|bbc\s*persian)\b",
    ],
    # ─── Afghanistan ──────────────────────────────────────────────
    "Afghanistan": [
        r"\b(tolo|1tv\s*af|ariana|shamshad|lemar|zan\s*tv|khabar|ghazni|khurshid)\b",
    ],
    # ─── Israel ───────────────────────────────────────────────────
    "Israel": [
        r"\b(kan\s*(11|23)|keshet\s*12|reshet\s*13|i24|channel\s*(9|20)\s*il|sport\s*(1|2|3|4|5)\s*il|hot|yes)\b",
    ],
    # ─── Colombia ─────────────────────────────────────────────────
    "Colombia": [
        r"\b(caracol|rcn|canal\s*(1|uno)|city\s*tv\s*co|telepacifico|telecaribe|teleantioquia|canal\s*capital|noticias\s*rcn|senal\s*colombia)\b",
    ],
    # ─── Argentina ────────────────────────────────────────────────
    "Argentina": [
        r"\b(el\s*trece|telefe|america\s*tv\s*ar|tv\s*publica|canal\s*(9|26)\s*ar|todo\s*noticias|tn|c5n|cronica|a24|infobae)\b",
    ],
    # ─── Chile ────────────────────────────────────────────────────
    "Chile": [
        r"\b(tvn\s*cl|canal\s*13\s*cl|mega\s*cl|chilevision|la\s*red|tv\+|telecanal|24\s*horas)\b",
    ],
    # ─── Peru ─────────────────────────────────────────────────────
    "Peru": [
        r"\b(america\s*tv\s*pe|latina\s*tv|atv\s*pe|panamericana|tv\s*peru|willax|sol\s*tv|canal\s*n)\b",
    ],
    # ─── Venezuela ────────────────────────────────────────────────
    "Venezuela": [
        r"\b(venevision|televen|globovision|vtv|tves|meridiano|vale\s*tv)\b",
    ],
    # ─── Hungary ──────────────────────────────────────────────────
    "Hungary": [
        r"\b(m1|m2|m4\s*sport|duna|m5|rtl\s*klub|tv2\s*hu|atv\s*hu|hir\s*tv|echo\s*tv|sport\s*(1|2)\s*hu)\b",
    ],
    # ─── Czech Republic ──────────────────────────────────────────
    "Czech Republic": [
        r"\b(ct\s*(1|2|24|sport|art|d)|nova\s*cz|prima\s*cz|barrandov|ocko|sport\s*(1|2)\s*cz)\b",
    ],
    # ─── Serbia ───────────────────────────────────────────────────
    "Serbia": [
        r"\b(rts\s*(1|2|3)|pink|b92|happy\s*tv|prva|n1\s*rs|o2\s*tv\s*rs|superstar)\b",
    ],
    # ─── Croatia ──────────────────────────────────────────────────
    "Croatia": [
        r"\b(hrt\s*(1|2|3|4)|nova\s*tv\s*hr|rtl\s*hr|doma|n1\s*hr|sport\s*klub\s*hr)\b",
    ],
    # ─── Bulgaria ─────────────────────────────────────────────────
    "Bulgaria": [
        r"\b(bnt\s*(1|2|3|4)|btv|nova\s*bg|diema|ring|kanal\s*3\s*bg|bgtv|bloomberg\s*bg)\b",
    ],
    # ─── South Africa ─────────────────────────────────────────────
    "South Africa": [
        r"\b(sabc\s*(1|2|3)|etv\s*sa|dstv|m-?net|supersport|kyknet|moja|1magic|e!\s*africa|trace\s*africa|africa\s*magic)\b",
    ],
    # ─── Nigeria ──────────────────────────────────────────────────
    "Nigeria": [
        r"\b(nta|channels\s*tv|ait|silverbird|tvc\s*news|arise|wazobia)\b",
    ],
    # ─── Kenya ────────────────────────────────────────────────────
    "Kenya": [
        r"\b(kbc|citizen\s*tv|ntv\s*kenya|ktv|ktn|k24)\b",
    ],
    # ─── Australia ────────────────────────────────────────────────
    "Australia": [
        r"\b(abc\s*(australia|au)|sbs|nine|seven\s*network|ten|foxtel|fox\s*sports\s*au|sky\s*news\s*au|sky\s*racing\s*au|kayo|binge|stan)\b",
    ],
    # ─── New Zealand ──────────────────────────────────────────────
    "New Zealand": [
        r"\b(tvnz|three\s*nz|maori|duke|bravo\s*nz|sky\s*sport\s*nz)\b",
    ],
    # ─── Singapore ────────────────────────────────────────────────
    "Singapore": [
        r"\b(mediacorp|channel\s*(5|8|u|newsasia)|suria|vasantham\s*sg|toggle|mewatch)\b",
    ],
    # ─── Sports (Generic) ────────────────────────────────────────
    "Sports": [
        r"\b(eurosport|dazn|bt\s*sport|sky\s*sports|supersport|sport\s*tv|arena\s*sport|fight|ufc|wwe|nba|nfl|f1|formula)\b",
    ],
    # ─── Music (Generic) ─────────────────────────────────────────
    "Music": [
        r"\b(mtv\s*(live|classic|hits|rocks|dance|base)?|vh1|trace\s*(urban|mziki|naija)|club\s*mtv|nick\s*music)\b",
    ],
    # ─── Kids (Generic) ──────────────────────────────────────────
    "Kids": [
        r"\b(cartoon\s*network|nick(?:elodeon)?|disney\s*(channel|junior|xd)|boomerang|baby\s*tv|duck\s*tv|jimjam|lollipop)\b",
    ],
}

# Compile all patterns
for country, patterns in _PATTERN_MAP.items():
    for pattern in patterns:
        CHANNEL_PATTERNS.append((re.compile(pattern, re.IGNORECASE), country))

# Country code (from group-title or tvg-country) → country name
COUNTRY_CODES: Dict[str, str] = {
    "AF": "Afghanistan", "AL": "Albania", "DZ": "Algeria", "AR": "Argentina",
    "AM": "Armenia", "AU": "Australia", "AT": "Austria", "AZ": "Azerbaijan",
    "BH": "Bahrain", "BD": "Bangladesh", "BY": "Belarus", "BE": "Belgium",
    "BO": "Bolivia", "BA": "Bosnia", "BR": "Brazil", "BG": "Bulgaria",
    "KH": "Cambodia", "CM": "Cameroon", "CA": "Canada", "CL": "Chile",
    "CN": "China", "CO": "Colombia", "CR": "Costa Rica", "HR": "Croatia",
    "CU": "Cuba", "CY": "Cyprus", "CZ": "Czech Republic", "DK": "Denmark",
    "DO": "Dominican Republic", "EC": "Ecuador", "EG": "Egypt", "SV": "El Salvador",
    "EE": "Estonia", "ET": "Ethiopia", "FI": "Finland", "FR": "France",
    "GE": "Georgia", "DE": "Germany", "GH": "Ghana", "GR": "Greece",
    "GT": "Guatemala", "HN": "Honduras", "HK": "Hong Kong", "HU": "Hungary",
    "IS": "Iceland", "IN": "India", "ID": "Indonesia", "IR": "Iran",
    "IQ": "Iraq", "IE": "Ireland", "IL": "Israel", "IT": "Italy",
    "JM": "Jamaica", "JP": "Japan", "JO": "Jordan", "KZ": "Kazakhstan",
    "KE": "Kenya", "XK": "Kosovo", "KW": "Kuwait", "KG": "Kyrgyzstan",
    "LA": "Laos", "LV": "Latvia", "LB": "Lebanon", "LY": "Libya",
    "LT": "Lithuania", "LU": "Luxembourg", "MK": "Macedonia", "MY": "Malaysia",
    "MV": "Maldives", "MT": "Malta", "MX": "Mexico", "MD": "Moldova",
    "MN": "Mongolia", "ME": "Montenegro", "MA": "Morocco", "MZ": "Mozambique",
    "MM": "Myanmar", "NP": "Nepal", "NL": "Netherlands", "NZ": "New Zealand",
    "NI": "Nicaragua", "NG": "Nigeria", "KP": "North Korea", "NO": "Norway",
    "OM": "Oman", "PK": "Pakistan", "PS": "Palestine", "PA": "Panama",
    "PY": "Paraguay", "PE": "Peru", "PH": "Philippines", "PL": "Poland",
    "PT": "Portugal", "QA": "Qatar", "RO": "Romania", "RU": "Russia",
    "SA": "Saudi Arabia", "SN": "Senegal", "RS": "Serbia", "SG": "Singapore",
    "SK": "Slovakia", "SI": "Slovenia", "SO": "Somalia", "ZA": "South Africa",
    "KR": "South Korea", "ES": "Spain", "LK": "Sri Lanka", "SD": "Sudan",
    "SE": "Sweden", "CH": "Switzerland", "SY": "Syria", "TW": "Taiwan",
    "TJ": "Tajikistan", "TZ": "Tanzania", "TH": "Thailand", "TN": "Tunisia",
    "TR": "Turkey", "TM": "Turkmenistan", "AE": "UAE", "UG": "Uganda",
    "UA": "Ukraine", "GB": "United Kingdom", "UK": "United Kingdom",
    "US": "USA", "UY": "Uruguay", "UZ": "Uzbekistan", "VE": "Venezuela",
    "VN": "Vietnam", "YE": "Yemen", "ZM": "Zambia", "ZW": "Zimbabwe",
}

# Group-title keywords that map to countries
GROUP_KEYWORDS: Dict[str, str] = {
    "india": "India", "indian": "India", "hindi": "India", "tamil": "India",
    "telugu": "India", "malayalam": "India", "kannada": "India", "bangla": "India",
    "bengali": "India", "marathi": "India", "gujarati": "India", "punjabi": "India",
    "odia": "India", "assamese": "India", "bhojpuri": "India",
    "usa": "USA", "american": "USA", "united states": "USA",
    "uk": "United Kingdom", "british": "United Kingdom", "english": "United Kingdom",
    "canada": "Canada", "canadian": "Canada",
    "germany": "Germany", "german": "Germany", "deutsch": "Germany",
    "france": "France", "french": "France", "français": "France",
    "spain": "Spain", "spanish": "Spain", "español": "Spain",
    "italy": "Italy", "italian": "Italy", "italiano": "Italy",
    "turkey": "Turkey", "turkish": "Turkey", "türk": "Turkey",
    "arab": "Saudi Arabia", "arabic": "Saudi Arabia",
    "pakistan": "Pakistan", "pakistani": "Pakistan", "urdu": "Pakistan",
    "bangladesh": "Bangladesh", "bangladeshi": "Bangladesh",
    "sri lanka": "Sri Lanka", "sinhala": "Sri Lanka", "sinhalese": "Sri Lanka",
    "nepal": "Nepal", "nepali": "Nepal",
    "russia": "Russia", "russian": "Russia",
    "japan": "Japan", "japanese": "Japan",
    "korea": "South Korea", "korean": "South Korea",
    "china": "China", "chinese": "China",
    "brazil": "Brazil", "brazilian": "Brazil", "portuguese": "Portugal",
    "mexico": "Mexico", "mexican": "Mexico", "latino": "Mexico",
    "iran": "Iran", "persian": "Iran", "farsi": "Iran",
    "afghanistan": "Afghanistan", "afghan": "Afghanistan", "pashto": "Afghanistan",
    "indonesia": "Indonesia", "indonesian": "Indonesia",
    "malaysia": "Malaysia", "malay": "Malaysia",
    "philippines": "Philippines", "filipino": "Philippines", "tagalog": "Philippines",
    "thailand": "Thailand", "thai": "Thailand",
    "vietnam": "Vietnam", "vietnamese": "Vietnam",
    "ukraine": "Ukraine", "ukrainian": "Ukraine",
    "poland": "Poland", "polish": "Poland",
    "romania": "Romania", "romanian": "Romania",
    "greece": "Greece", "greek": "Greece",
    "netherlands": "Netherlands", "dutch": "Netherlands",
    "sweden": "Sweden", "swedish": "Sweden",
    "norway": "Norway", "norwegian": "Norway",
    "denmark": "Denmark", "danish": "Denmark",
    "finland": "Finland", "finnish": "Finland",
    "portugal": "Portugal",
    "hungary": "Hungary", "hungarian": "Hungary",
    "czech": "Czech Republic",
    "serbia": "Serbia", "serbian": "Serbia",
    "croatia": "Croatia", "croatian": "Croatia",
    "bulgaria": "Bulgaria", "bulgarian": "Bulgaria",
    "south africa": "South Africa",
    "nigeria": "Nigeria",
    "kenya": "Kenya",
    "australia": "Australia", "australian": "Australia",
    "new zealand": "New Zealand",
    "singapore": "Singapore",
    "sports": "Sports", "sport": "Sports", "football": "Sports", "cricket": "Sports",
    "music": "Music", "songs": "Music",
    "kids": "Kids", "children": "Kids", "cartoon": "Kids",
    "movies": "Movies", "cinema": "Movies", "film": "Movies",
    "news": "News",
    "documentary": "Documentary", "education": "Documentary",
    "religious": "Religious", "islamic": "Religious", "christian": "Religious",
    "entertainment": "Entertainment",
    "adult": "Adult", "xxx": "Adult", "18+": "Adult",
}


def get_country_from_group(group_title: str) -> Optional[str]:
    """Try to identify country from the group-title attribute."""
    if not group_title:
        return None
    group_lower = group_title.lower().strip()
    
    # Check for country code prefix like "US:", "IN:", "UK:" 
    code_match = re.match(r'^([A-Z]{2})[\s:|\-]', group_title.strip())
    if code_match:
        code = code_match.group(1)
        if code in COUNTRY_CODES:
            return COUNTRY_CODES[code]
    
    # Check group keywords
    for keyword, country in GROUP_KEYWORDS.items():
        if keyword in group_lower:
            return country
    
    return None


def get_country_from_name(channel_name: str) -> Optional[str]:
    """Try to identify country from the channel name using pattern matching."""
    if not channel_name:
        return None
    
    for pattern, country in CHANNEL_PATTERNS:
        if pattern.search(channel_name):
            return country
    
    return None


def get_country_from_attributes(tvg_country: str = "", tvg_language: str = "") -> Optional[str]:
    """Try to identify country from tvg-country or tvg-language attributes."""
    if tvg_country:
        country_upper = tvg_country.strip().upper()
        if country_upper in COUNTRY_CODES:
            return COUNTRY_CODES[country_upper]
        # Try as full name
        for code, name in COUNTRY_CODES.items():
            if name.lower() == tvg_country.strip().lower():
                return name
    
    if tvg_language:
        lang_lower = tvg_language.strip().lower()
        lang_map = {
            "hindi": "India", "tamil": "India", "telugu": "India",
            "malayalam": "India", "kannada": "India", "bengali": "India",
            "marathi": "India", "gujarati": "India", "punjabi": "India",
            "urdu": "Pakistan", "arabic": "Saudi Arabia", "persian": "Iran",
            "farsi": "Iran", "turkish": "Turkey", "russian": "Russia",
            "japanese": "Japan", "korean": "South Korea", "chinese": "China",
            "mandarin": "China", "cantonese": "Hong Kong", "thai": "Thailand",
            "vietnamese": "Vietnam", "indonesian": "Indonesia", "malay": "Malaysia",
            "tagalog": "Philippines", "french": "France", "german": "Germany",
            "spanish": "Spain", "italian": "Italy", "portuguese": "Portugal",
            "dutch": "Netherlands", "swedish": "Sweden", "norwegian": "Norway",
            "danish": "Denmark", "finnish": "Finland", "polish": "Poland",
            "romanian": "Romania", "greek": "Greece", "hungarian": "Hungary",
            "czech": "Czech Republic", "serbian": "Serbia", "croatian": "Croatia",
            "bulgarian": "Bulgaria", "ukrainian": "Ukraine", "sinhala": "Sri Lanka",
            "nepali": "Nepal", "pashto": "Afghanistan", "bangla": "Bangladesh",
        }
        if lang_lower in lang_map:
            return lang_map[lang_lower]
    
    return None


def identify_country(
    channel_name: str = "",
    group_title: str = "",
    tvg_country: str = "",
    tvg_language: str = "",
    tvg_id: str = "",
) -> str:
    """
    Identify the country of a channel using multiple signals.
    Returns the country name or 'International' if unknown.
    
    Priority:
    1. tvg-country attribute
    2. Channel name pattern matching
    3. Group-title keyword matching
    4. Language-based detection
    5. Fallback to 'International'
    """
    # 1. Try tvg-country / tvg-language attributes
    result = get_country_from_attributes(tvg_country, tvg_language)
    if result:
        return result
    
    # 2. Try channel name pattern matching (most reliable)
    result = get_country_from_name(channel_name)
    if result:
        return result
    
    # 3. Try tvg-id patterns (sometimes contains country info)
    if tvg_id:
        result = get_country_from_name(tvg_id)
        if result:
            return result
    
    # 4. Try group-title keyword matching
    result = get_country_from_group(group_title)
    if result:
        return result
    
    # 5. Fallback
    return "International"


def get_country_with_flag(country: str) -> str:
    """Return country name with its flag emoji for group-title."""
    flag = FLAGS.get(country, "🌍")
    return f"{flag} {country}"


if __name__ == "__main__":
    # Test the mapper
    test_cases = [
        ("Star Plus", "Entertainment", "IN", "Hindi"),
        ("BBC One", "UK Channels", "GB", "English"),
        ("CNN", "News", "US", "English"),
        ("Geo TV", "", "", "Urdu"),
        ("NTV Bangla", "", "BD", "Bangla"),
        ("Hiru TV", "", "LK", "Sinhala"),
        ("Unknown Channel", "", "", ""),
        ("Zee Tamil", "Tamil", "IN", "Tamil"),
        ("Al Jazeera", "News", "QA", "Arabic"),
        ("TRT 1", "", "TR", "Turkish"),
    ]
    
    print("Country Mapper Test Results:")
    print("-" * 70)
    for name, group, country_code, lang in test_cases:
        result = identify_country(name, group, country_code, lang)
        flag_result = get_country_with_flag(result)
        print(f"  {name:<25} → {flag_result}")
    print("-" * 70)
    print(f"Total patterns loaded: {len(CHANNEL_PATTERNS)}")
