package com.github.asforest.mshell.configuration

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object MainConfig : AutoSavePluginConfig ("config")
{
    var stdoutTruncationInBytes: Int by value(2048)
    var stdoutBatchingIntevalInMs: Int by value(300)
    var lastwillCapacityInBytes: Int by value(2048)
    var sessionInputPrefix: String by value("")
}