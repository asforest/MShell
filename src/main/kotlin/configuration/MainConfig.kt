package com.github.asforest.mshell.configuration

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object MainConfig : AutoSavePluginConfig ("config")
{
    var admins: MutableList<Long> by value(mutableListOf(0L))
    var stdoutputTruncationThreshold: Int by value(512)
    var stdoutputBatchingTimeoutInMs: Int by value(300)
}