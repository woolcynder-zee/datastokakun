package com.stokakun.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(status: AccountStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): AccountStatus = AccountStatus.valueOf(value)
}
