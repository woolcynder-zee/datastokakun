package com.stokakun.app.data

data class GameStat(
    val game: String,
    val total: Int,
    val available: Int,
    val reserved: Int,
    val sold: Int,
    val activeValue: Long
)
