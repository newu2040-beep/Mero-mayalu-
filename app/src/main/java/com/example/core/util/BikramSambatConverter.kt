package com.example.core.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class BikramSambatDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val monthNameEn: String,
    val monthNameNe: String
) {
    fun format(isNepali: Boolean): String = if (isNepali) {
        "$year $monthNameNe $day वि.सं."
    } else {
        "$year $monthNameEn $day BS"
    }

    fun formatNumeric(): String =
        "%04d-%02d-%02d".format(year, month, day)
}

data class NepaliFestival(
    val id: String,
    val nameEn: String,
    val nameNe: String,
    val bsPeriodEn: String,
    val bsPeriodNe: String,
    val approxBsMonth: Int,
    val approxBsDay: Int,
    val descriptionEn: String,
    val descriptionNe: String,
    val regionContextEn: String,
    val regionContextNe: String
)

data class FestivalCountdown(
    val festival: NepaliFestival,
    val daysRemaining: Long,
    val targetBsDate: BikramSambatDate
)

data class HeritageSpot(
    val id: String,
    val titleEn: String,
    val titleNe: String,
    val city: String,
    val summaryEn: String,
    val summaryNe: String
)

object BikramSambatConverter {
    val monthNamesEn = listOf(
        "Baisakh", "Jestha", "Ashadh", "Shrawan", "Bhadra", "Ashwin",
        "Kartik", "Mangsir", "Poush", "Magh", "Falgun", "Chaitra"
    )
    val monthNamesNe = listOf(
        "वैशाख", "जेठ", "असार", "साउन", "भदौ", "असोज",
        "कात्तिक", "मंसिर", "पुस", "माघ", "फागुन", "चैत"
    )

    // Verified Bikram Sambat month lengths for 2000 BS .. 2090 BS (anchor 2000-01-01 BS == 1943-04-14 AD)
    private val bsYearMonthDays: Map<Int, IntArray> = mapOf(
        2000 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2001 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2002 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2003 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2004 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2005 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2006 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2007 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2008 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
        2009 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2010 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2011 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2012 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2013 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2014 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2015 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2016 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2017 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2018 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2019 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2020 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2021 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2022 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2023 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2024 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2025 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2026 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2027 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2028 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2029 to intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        2030 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2031 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2032 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2033 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2034 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2035 to intArrayOf(30, 32, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
        2036 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2037 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2038 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2039 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2040 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2041 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2042 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2043 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2044 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2045 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2046 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2047 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2048 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2049 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2050 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2051 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2052 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2053 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2054 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2055 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2056 to intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        2057 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2058 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2059 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2060 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2061 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2062 to intArrayOf(30, 32, 31, 32, 31, 31, 29, 30, 29, 30, 29, 31),
        2063 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2064 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2065 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2066 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
        2067 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2068 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2069 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2070 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2071 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2072 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2073 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2074 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2075 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2076 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2077 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2078 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2079 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2080 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2081 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2082 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2083 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2084 to intArrayOf(31, 32, 31, 32, 30, 31, 30, 29, 30, 29, 30, 30),
        2085 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2086 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2087 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2088 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2089 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2090 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31)
    )

    private val anchorAd = LocalDate.of(1943, 4, 14) // 2000 Baisakh 1 BS

    fun daysInMonth(bsYear: Int, bsMonth: Int): Int {
        val months = bsYearMonthDays[bsYear] ?: return 30
        return months.getOrNull(bsMonth - 1) ?: 30
    }

    fun fromLocalDate(date: LocalDate): BikramSambatDate? {
        var remainingDays = ChronoUnit.DAYS.between(anchorAd, date).toInt()
        if (remainingDays < 0) return null

        for (year in 2000..2090) {
            val months = bsYearMonthDays[year] ?: return null
            for (m in months.indices) {
                val daysInMonth = months[m]
                if (remainingDays < daysInMonth) {
                    return BikramSambatDate(
                        year = year,
                        month = m + 1,
                        day = remainingDays + 1,
                        monthNameEn = monthNamesEn[m],
                        monthNameNe = monthNamesNe[m]
                    )
                }
                remainingDays -= daysInMonth
            }
        }
        return null
    }

    fun toLocalDate(bsYear: Int, bsMonth: Int, bsDay: Int): LocalDate? {
        if (bsYear !in 2000..2090 || bsMonth !in 1..12) return null
        val maxDay = daysInMonth(bsYear, bsMonth)
        if (bsDay !in 1..maxDay) return null
        var days = 0L
        for (year in 2000 until bsYear) {
            val months = bsYearMonthDays[year] ?: return null
            days += months.sum()
        }
        val targetMonths = bsYearMonthDays[bsYear] ?: return null
        for (m in 0 until (bsMonth - 1)) {
            days += targetMonths[m]
        }
        days += (bsDay - 1)
        return anchorAd.plusDays(days)
    }

    fun parseBsToLocalDate(bsIsoString: String): LocalDate? {
        val parts = bsIsoString.trim().split("-", "/")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        return toLocalDate(y, m, d)
    }

    fun upcomingFestivalsWithCountdown(today: LocalDate = LocalDate.now()): List<FestivalCountdown> {
        val todayBs = fromLocalDate(today) ?: return emptyList()
        return NepaliCultureCatalog.festivals.mapNotNull { fest ->
            var targetYear = todayBs.year
            var targetAd = toLocalDate(targetYear, fest.approxBsMonth, fest.approxBsDay)
            if (targetAd == null || targetAd.isBefore(today)) {
                targetYear += 1
                targetAd = toLocalDate(targetYear, fest.approxBsMonth, fest.approxBsDay)
            }
            val resolvedAd = targetAd ?: return@mapNotNull null
            val targetBs = fromLocalDate(resolvedAd) ?: return@mapNotNull null
            val days = ChronoUnit.DAYS.between(today, resolvedAd)
            FestivalCountdown(festival = fest, daysRemaining = days, targetBsDate = targetBs)
        }.sortedBy { it.daysRemaining }
    }
}

object NepaliCultureCatalog {
    val interests = listOf(
        "Music" to "संगीत",
        "Hiking" to "पदमार्ग",
        "Food" to "परिकार",
        "Books" to "पुस्तक",
        "Films" to "चलचित्र",
        "Travel" to "यात्रा",
        "Photography" to "फोटोग्राफी",
        "Sports" to "खेलकुद",
        "Art" to "कला",
        "Festivals" to "चाडपर्व",
        "Cooking" to "पाककला",
        "Volunteering" to "स्वयंसेवा"
    )

    val culturalBadges = listOf(
        "" to "कुनै ब्याज छैन / None",
        "Newari heritage lover" to "नेवारी सम्पदा प्रेमी",
        "Festival enthusiast" to "चाडपर्व प्रेमी",
        "Local food explorer" to "स्थानीय परिकार अन्वेषक",
        "Hiking enthusiast" to "हिमाली पदमार्ग प्रेमी"
    )

    val prompts = listOf(
        "Favourite momo spot?" to "मनपर्ने मःमः खाने ठाउँ?",
        "A place in Nepal you want to revisit?" to "फेरि घुम्न मन लागेको नेपालको ठाउँ?",
        "A tradition you enjoy?" to "तपाईंलाई मनपर्ने परम्परा?",
        "Tea or chiya?" to "चिया कि कफी? मनपर्ने गफ?"
    )

    val thoughtTags = listOf(
        "Chiya & Guff" to "चिया र गफ",
        "Heritage Walk" to "सम्पदा यात्रा",
        "Festival Mood" to "चाडपर्व उमङ्ग",
        "Poem / Shayari" to "कविता / शायरी",
        "Himalayan View" to "हिमाली दृश्य",
        "Local Food" to "स्थानीय परिकार"
    )

    val icebreakers = listOf(
        "Jhol momo or steamed C-momo on a rainy Kathmandu evening?" to "काठमाडौंको झरीमा झोल मःमः कि सी-मःमः मनपर्छ?",
        "Sunrise at Nagarkot or evening boating on Phewa Lake?" to "नगरकोटको सूर्योदय कि फेवातालको साँझको डुङ्गा सयर?",
        "If we visited Patan Durbar Square this weekend, what would we try first?" to "यो सप्ताहन्त पाटन दरबार घुम्न जाँदा सबैभन्दा पहिले के खाने वा हेर्ने?",
        "Which Nepali song or lok dohori always lifts your mood?" to "कुन नेपाली गीत वा लोक दोहोरी सुन्दा तपाईंको मन फुरुङ्ग हुन्छ?",
        "Dudh chiya with ilaychi or lemon-ginger tea during winter mornings?" to "जाडो बिहानीमा अलैंची दूध चिया कि कागती-अदुवा चिया?"
    )

    val provincesAndDistricts: Map<String, List<String>> = mapOf(
        "Bagmati" to listOf("Kathmandu", "Lalitpur", "Bhaktapur", "Kavrepalanchok", "Chitwan", "Makwanpur", "Nuwakot", "Dhading", "Sindhupalchok"),
        "Gandaki" to listOf("Kaski", "Tanahun", "Syangja", "Gorkha", "Lamjung", "Mustang", "Baglung"),
        "Koshi" to listOf("Morang", "Sunsari", "Jhapa", "Ilam", "Dhankuta", "Solukhumbu"),
        "Lumbini" to listOf("Rupandehi", "Banke", "Dang", "Palpa", "Kapilvastu", "Bardiya"),
        "Madhesh" to listOf("Dhanusha", "Parsa", "Bara", "Saptari", "Siraha"),
        "Karnali" to listOf("Surkhet", "Jumla", "Dailekh", "Mugu"),
        "Sudurpashchim" to listOf("Kailali", "Kanchanpur", "Doti", "Dadeldhura")
    )

    val festivals = listOf(
        NepaliFestival(
            id = "dashain",
            nameEn = "Dashain",
            nameNe = "बडादशैं",
            bsPeriodEn = "Ashwin Shukla Paksha",
            bsPeriodNe = "असोज शुक्ल पक्ष",
            approxBsMonth = 6,
            approxBsDay = 24,
            descriptionEn = "Nepal's longest harvest and family reunion festival with tika, jamara, and bamboo swings.",
            descriptionNe = "टीका, जमरा र लिङ्गे पिङसँगै परिवार मिलन र सद्भावको महान् चाड।",
            regionContextEn = "Celebrated nationwide with regional traditions.",
            regionContextNe = "देशभर विविध क्षेत्रीय परम्परासहित मनाइन्छ।"
        ),
        NepaliFestival(
            id = "tihar",
            nameEn = "Tihar (Swanti)",
            nameNe = "तिहार (स्वन्ती)",
            bsPeriodEn = "Kartik Krishna/Shukla Paksha",
            bsPeriodNe = "कात्तिक कृष्ण/शुक्ल पक्ष",
            approxBsMonth = 7,
            approxBsDay = 18,
            descriptionEn = "Five days of lights, marigold garlands, Mha Puja in Newar households, and Bhai Tika.",
            descriptionNe = "दियो, सयपत्री माला, नेवार समुदायमा म्हः पूजा र भाइटीकाको उज्यालो पर्व।",
            regionContextEn = "Kathmandu Valley celebrates Swanti & Nepal Sambat New Year.",
            regionContextNe = "काठमाडौं उपत्यकामा स्वन्ती र नेपाल संवत् नयाँ वर्ष विशेष हुन्छ।"
        ),
        NepaliFestival(
            id = "indra_jatra",
            nameEn = "Indra Jatra (Yenya)",
            nameNe = "इन्द्रजात्रा (येँयाः)",
            bsPeriodEn = "Bhadra Shukla",
            bsPeriodNe = "भदौ शुक्ल",
            approxBsMonth = 5,
            approxBsDay = 29,
            descriptionEn = "Vibrant Kathmandu Valley heritage festival featuring Kumari chariot processions and Lakhey dances.",
            descriptionNe = "कुमारी रथयात्रा, लाखे नाच र परम्परागत बाजासहित काठमाडौं उपत्यकाको जीवन्त जात्रा।",
            regionContextEn = "Kathmandu Durbar Square & historic Newari settlements.",
            regionContextNe = "बसन्तपुर दरबार क्षेत्र र नेवारी बस्तीहरू।"
        ),
        NepaliFestival(
            id = "bisket_jatra",
            nameEn = "Nepali New Year / Bisket",
            nameNe = "नयाँ वर्ष / बिस्केट जात्रा",
            bsPeriodEn = "Baisakh 1 (BS New Year)",
            bsPeriodNe = "वैशाख १ (नयाँ वर्ष)",
            approxBsMonth = 1,
            approxBsDay = 1,
            descriptionEn = "Historic Bhaktapur and Thimi spring festival marking the Bikram Sambat New Year.",
            descriptionNe = "भक्तपुर र थिमीमा विक्रम संवत् नयाँ वर्ष स्वागत गर्ने ऐतिहासिक रथ र सिन्दूर जात्रा।",
            regionContextEn = "Bhaktapur, Madhyapur Thimi & Tokha.",
            regionContextNe = "भक्तपुर, मध्यपुर थिमी र टोखा।"
        ),
        NepaliFestival(
            id = "lhosar",
            nameEn = "Lhosar",
            nameNe = "ल्होसार",
            bsPeriodEn = "Magh Shukla Pratipada",
            bsPeriodNe = "माघ शुक्ल प्रतिपदा",
            approxBsMonth = 10,
            approxBsDay = 15,
            descriptionEn = "Tamu, Sonam, and Gyalpo New Year celebrations with khapse, prayer flags, and folk dances.",
            descriptionNe = "तमु, सोनाम र ग्याल्पो नयाँ वर्ष — खाप्से, लुङदार र सांस्कृतिक नाचगानसहित।",
            regionContextEn = "Himalayan, hill, and urban communities across Nepal.",
            regionContextNe = "हिमाली, पहाडी र शहरी समुदायहरूमा मनाइन्छ।"
        ),
        NepaliFestival(
            id = "holi",
            nameEn = "Fagu Purnima (Holi)",
            nameNe = "फागु पूर्णिमा (होली)",
            bsPeriodEn = "Falgun Purnima",
            bsPeriodNe = "फागुन पूर्णिमा",
            approxBsMonth = 11,
            approxBsDay = 25,
            descriptionEn = "Joyful spring festival of colors celebrated across the hills and Terai over two consecutive days.",
            descriptionNe = "पहाड र तराईमा दुई दिनसम्म मनाइने रङ, संगीत र वसन्तको पर्व।",
            regionContextEn = "Hill districts on Purnima; Terai districts the next day.",
            regionContextNe = "पूर्णिमाको दिन पहाडमा र भोलिपल्ट तराईमा मनाइन्छ।"
        )
    )

    val heritageSpots = listOf(
        HeritageSpot(
            id = "patan",
            titleEn = "Patan Durbar Square & Courtyards",
            titleNe = "पाटन दरबार क्षेत्र र बहालहरू",
            city = "Lalitpur",
            summaryEn = "Masterworks of Newari bronze, carved wood windows (Aankhijhyal), and tranquil stone water spouts.",
            summaryNe = "नेवारी काष्ठकला, आँखीझ्याल, धातु शिल्प र शान्त ढुंगेधाराहरूको अद्वितीय संगम।"
        ),
        HeritageSpot(
            id = "bhaktapur",
            titleEn = "Bhaktapur Pottery & Nyatapola",
            titleNe = "भक्तपुर पोताला र न्यातपोल",
            city = "Bhaktapur",
            summaryEn = "Five-tiered pagoda craftsmanship, clay pottery squares, and famous Juju Dhau heritage.",
            summaryNe = "पाँचतले न्यातपोल मन्दिर, कुमाले टोलको माटो कला र जुजु धौ परम्परा।"
        )
    )
}
