package com.github.asforest.mshell.util

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription

object MiraiUtil
{
    private val MF: Map<String, String> = ManifestUtil.manifest

    val version: String get() = MF["Mirai-Plugin-Version"] ?: "0.0.0"

    val gitCommit: String get() = MF["Git-Commit"] ?: "<development>"

    val compileTime: String get() = MF["Compile-Time"] ?: "<no compile time>"

    val compileTimeMs: Long get() = MF["Compile-Time-Ms"]?.toLong() ?: 0L

    val pluginDescription: JvmPluginDescription get() = JvmPluginDescription("com.github.asforest.mshell", version, "MShell")
}