package com.iptv.autocombine.util

/**
 * Maps country names and country codes to their corresponding flag emojis.
 *
 * Unicode flag emojis are composed of regional indicator symbols.
 * For example, "US" becomes 🇺🇸 (U+1F1FA U+1F1F8).
 */
object CountryFlags {

    /**
     * Common country name to ISO 3166-1 alpha-2 code mapping.
     * Used when the M3U group-title contains a country name rather than a code.
     */
    private val COUNTRY_NAME_TO_CODE = mapOf(
        "afghanistan" to "AF", "albania" to "AL", "algeria" to "DZ",
        "argentina" to "AR", "armenia" to "AM", "australia" to "AU",
        "austria" to "AT", "azerbaijan" to "AZ", "bahrain" to "BH",
        "bangladesh" to "BD", "belarus" to "BY", "belgium" to "BE",
        "bolivia" to "BO", "bosnia" to "BA", "bosnia and herzegovina" to "BA",
        "brazil" to "BR", "bulgaria" to "BG", "cambodia" to "KH",
        "cameroon" to "CM", "canada" to "CA", "chile" to "CL",
        "china" to "CN", "colombia" to "CO", "costa rica" to "CR",
        "croatia" to "HR", "cuba" to "CU", "cyprus" to "CY",
        "czech republic" to "CZ", "czechia" to "CZ",
        "denmark" to "DK", "dominican republic" to "DO",
        "ecuador" to "EC", "egypt" to "EG", "el salvador" to "SV",
        "estonia" to "EE", "ethiopia" to "ET",
        "finland" to "FI", "france" to "FR",
        "georgia" to "GE", "germany" to "DE", "ghana" to "GH",
        "greece" to "GR", "guatemala" to "GT",
        "honduras" to "HN", "hong kong" to "HK", "hungary" to "HU",
        "iceland" to "IS", "india" to "IN", "indonesia" to "ID",
        "iran" to "IR", "iraq" to "IQ", "ireland" to "IE",
        "israel" to "IL", "italy" to "IT",
        "jamaica" to "JM", "japan" to "JP", "jordan" to "JO",
        "kazakhstan" to "KZ", "kenya" to "KE", "korea" to "KR",
        "south korea" to "KR", "north korea" to "KP",
        "kuwait" to "KW", "kyrgyzstan" to "KG",
        "laos" to "LA", "latvia" to "LV", "lebanon" to "LB",
        "libya" to "LY", "lithuania" to "LT", "luxembourg" to "LU",
        "malaysia" to "MY", "mexico" to "MX", "moldova" to "MD",
        "mongolia" to "MN", "montenegro" to "ME", "morocco" to "MA",
        "mozambique" to "MZ", "myanmar" to "MM",
        "nepal" to "NP", "netherlands" to "NL", "new zealand" to "NZ",
        "nicaragua" to "NI", "nigeria" to "NG", "norway" to "NO",
        "oman" to "OM",
        "pakistan" to "PK", "panama" to "PA", "paraguay" to "PY",
        "peru" to "PE", "philippines" to "PH", "poland" to "PL",
        "portugal" to "PT", "puerto rico" to "PR",
        "qatar" to "QA",
        "romania" to "RO", "russia" to "RU", "rwanda" to "RW",
        "saudi arabia" to "SA", "serbia" to "RS", "singapore" to "SG",
        "slovakia" to "SK", "slovenia" to "SI", "somalia" to "SO",
        "south africa" to "ZA", "spain" to "ES", "sri lanka" to "LK",
        "sudan" to "SD", "sweden" to "SE", "switzerland" to "CH",
        "syria" to "SY",
        "taiwan" to "TW", "tajikistan" to "TJ", "tanzania" to "TZ",
        "thailand" to "TH", "trinidad and tobago" to "TT",
        "tunisia" to "TN", "turkey" to "TR", "turkmenistan" to "TM",
        "uganda" to "UG", "ukraine" to "UA",
        "united arab emirates" to "AE", "uae" to "AE",
        "united kingdom" to "GB", "uk" to "GB",
        "united states" to "US", "usa" to "US", "us" to "US",
        "uruguay" to "UY", "uzbekistan" to "UZ",
        "venezuela" to "VE", "vietnam" to "VN",
        "yemen" to "YE",
        "zambia" to "ZM", "zimbabwe" to "ZW",
        // Common aliases
        "international" to "UN", "world" to "UN",
        "latino" to "MX", "latin america" to "MX",
        "arabic" to "SA", "arab" to "SA",
        "persian" to "IR", "kurdish" to "IQ",
        "turkish" to "TR", "russian" to "RU",
        "portuguese" to "PT", "spanish" to "ES",
        "french" to "FR", "german" to "DE",
        "italian" to "IT", "dutch" to "NL",
        "greek" to "GR", "polish" to "PL",
        "romanian" to "RO", "hungarian" to "HU",
        "chinese" to "CN", "japanese" to "JP",
        "korean" to "KR", "thai" to "TH",
        "hindi" to "IN", "urdu" to "PK"
    )

    /**
     * Returns the flag emoji for a given country name or code.
     *
     * @param countryOrGroup Country name or ISO 3166-1 alpha-2 code
     * @return Flag emoji string, or globe emoji 🌍 if not recognized
     */
    fun getFlagEmoji(countryOrGroup: String): String {
        val input = countryOrGroup.trim().lowercase()

        // Try direct ISO code match (exactly 2 characters)
        if (input.length == 2) {
            return codeToFlag(input.uppercase())
        }

        // Try name lookup
        val code = COUNTRY_NAME_TO_CODE[input]
        if (code != null) {
            return codeToFlag(code)
        }

        // Try partial match (e.g., "United States of America" contains "united states")
        for ((name, code2) in COUNTRY_NAME_TO_CODE) {
            if (input.contains(name) || name.contains(input)) {
                return codeToFlag(code2)
            }
        }

        // Default globe emoji for unrecognized
        return "🌍"
    }

    /**
     * Converts an ISO 3166-1 alpha-2 country code to a flag emoji.
     *
     * Each letter is offset to the Regional Indicator Symbol range (U+1F1E6 - U+1F1FF).
     *
     * @param code Two-letter country code (e.g., "US")
     * @return Flag emoji string (e.g., "🇺🇸")
     */
    private fun codeToFlag(code: String): String {
        if (code.length != 2) return "🌍"

        val firstChar = Character.toChars(0x1F1E6 + (code[0].uppercaseChar() - 'A'))
        val secondChar = Character.toChars(0x1F1E6 + (code[1].uppercaseChar() - 'A'))

        return String(firstChar) + String(secondChar)
    }
}
