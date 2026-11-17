package com.scorecard.weekly.data

/**
 * The checklist from "Score Your Week in 5 Areas".
 * Each area has 9 items worth 1 point each, plus 1 "gut-feel" point = max 10.
 * To change the wording of an item, edit the text here.
 */
data class Area(
    val title: String,
    val shortName: String,
    val emoji: String,
    val colorArgb: Long,
    val items: List<String>,
)

const val GUT_FEEL_LABEL = "Overall, I felt good about this area"
const val MAX_AREA_SCORE = 10

val AREAS: List<Area> = listOf(
    Area(
        title = "Health & Energy",
        shortName = "Health",
        emoji = "💪",
        colorArgb = 0xFF2E9E6B,
        items = listOf(
            "I got 7–8 hours of sleep most nights.",
            "I exercised or moved my body regularly.",
            "I ate balanced meals.",
            "I drank enough water.",
            "I maintained a consistent sleep schedule.",
            "I spent time outdoors.",
            "I avoided unnecessary late nights.",
            "I took care of my physical health.",
            "I felt energetic most days.",
        ),
    ),
    Area(
        title = "Work & Productivity",
        shortName = "Work",
        emoji = "💼",
        colorArgb = 0xFF3B7DD8,
        items = listOf(
            "I completed my most important tasks.",
            "I followed my daily priorities.",
            "I made progress on my biggest goal.",
            "I avoided unnecessary procrastination.",
            "I managed my time effectively.",
            "I stayed focused while working.",
            "I finished tasks instead of constantly switching.",
            "I planned my days in advance.",
            "I used my working hours wisely.",
        ),
    ),
    Area(
        title = "Personal Growth",
        shortName = "Growth",
        emoji = "🌱",
        colorArgb = 0xFF9C5BD6,
        items = listOf(
            "I read something valuable.",
            "I learned something new.",
            "I practised a useful skill.",
            "I challenged myself to do something difficult.",
            "I stepped outside my comfort zone.",
            "I reflected on my decisions.",
            "I spent time developing a new idea.",
            "I made progress on a personal goal.",
            "I did something that made me better.",
        ),
    ),
    Area(
        title = "Relationships",
        shortName = "People",
        emoji = "🤝",
        colorArgb = 0xFFE0703A,
        items = listOf(
            "I spent quality time with family.",
            "I checked in on someone I care about.",
            "I had meaningful conversations.",
            "I gave people my full attention.",
            "I made time for my friends.",
            "I expressed appreciation to someone.",
            "I listened without constantly checking my phone.",
            "I made an effort to stay connected.",
            "I created a good memory with someone.",
        ),
    ),
    Area(
        title = "Mental Well-being",
        shortName = "Mind",
        emoji = "🧘",
        colorArgb = 0xFFD94C7C,
        items = listOf(
            "I made time for myself.",
            "I managed my stress effectively.",
            "I limited unnecessary scrolling.",
            "I took breaks when I needed them.",
            "I spent time doing things I enjoy.",
            "I avoided overthinking things I couldn't control.",
            "I maintained a positive environment.",
            "I gave myself enough time to recharge.",
            "I felt mentally present throughout the week.",
        ),
    ),
)

val MAX_TOTAL_SCORE: Int = AREAS.size * MAX_AREA_SCORE
