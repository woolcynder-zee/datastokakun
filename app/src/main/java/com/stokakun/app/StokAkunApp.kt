package com.stokakun.app

import android.app.Application
import com.stokakun.app.data.AppDatabase
import com.stokakun.app.repository.AccountRepository

class StokAkunApp : Application() {

    lateinit var repository: AccountRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        repository = AccountRepository(
            context = this,
            accountDao = db.accountDao(),
            screenshotDao = db.screenshotDao(),
            database = db
        )
    }
}
