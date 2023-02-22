package com.reteno.core.data.local.database.manager

internal interface RetenoDatabaseManagerWrappedLink {
    fun insertWrappedLink(url: String)
    fun getWrappedLinks(limit: Int? = null): List<String>
    fun getWrappedLinksCount(): Long
    fun deleteWrappedLinks(count: Int, oldest: Boolean = true)
    fun deleteWrappedLinksByTime(outdatedTime: String): Int
}