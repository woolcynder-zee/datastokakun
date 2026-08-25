package com.stokakun.app.ui.navigation

object Routes {
    const val HOME = "home"
    const val LIST = "list"
    const val ADD = "add"
    const val EDIT = "edit/{accountId}"
    const val DETAIL = "detail/{accountId}"
    const val FULLSCREEN = "fullscreen/{accountId}/{index}"

    fun edit(accountId: Long) = "edit/$accountId"
    fun detail(accountId: Long) = "detail/$accountId"
    fun fullscreen(accountId: Long, index: Int) = "fullscreen/$accountId/$index"
}
