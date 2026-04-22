package com.tuneflow.core.network

fun interface NavidromeClientProvider {
    fun create(session: SessionData): NavidromeClient
}

object DefaultNavidromeClientProvider : NavidromeClientProvider {
    override fun create(session: SessionData): NavidromeClient = NavidromeClient(session)
}
