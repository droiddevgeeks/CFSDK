package com.github.droiddevgeeks.cfsdk.network.model

data class SDK(
    val currentVersion: Int,
    val url: String,
    val platform: String,
)

sealed class SDKPlatform {
    data object Android : SDKPlatform()
    data object IntelliJ : SDKPlatform()
    data object WebStorm : SDKPlatform()
    data object PhpStorm : SDKPlatform()
    data object GoLand : SDKPlatform()
    data object PyCharm : SDKPlatform()
}