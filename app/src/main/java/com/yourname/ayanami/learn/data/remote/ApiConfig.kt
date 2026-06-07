package com.yourname.ayanami.learn.data.remote

import com.yourname.ayanami.learn.BuildConfig

object ApiConfig {
    val BASE_URL: String = BuildConfig.API_BASE_URL.trimEnd('/')
    val WS_BASE_URL: String = BuildConfig.WS_BASE_URL.trimEnd('/')
}
