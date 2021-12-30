package com.github.asforest.mshell.configuration

object MShellConfig : YamlConfig("config.yml")
{
    /**
     * 子进程stdout最大合并字符数的上限，超过这个上限会被拆成两条消息
     */
    var truncationThreshold: Int = 2048

    /**
     * 子进程stdout最大合并间隔，超过这个间隔会被会被拆成两条消息
     */
    var batchingInteval: Int = 300

    /**
     * 遗愿消息缓冲区大小，越大的值能记录的子进程最后输出的内容就越多，单位是字符
     */
    var lastwillCapacity: Int = 2048

    /**
     * 会话输入前缀，如果不为空则只有带有此前缀的QQ消息才会被识别成发往子进程的消息，否则会被忽略
     *
     * 如果此选项为空，则任何消息都会被视为是发往子进程的消息
     */
    var sessionInputPrefix: String = ""

    override fun onLoad(deserialized: HashMap<String, Any>)
    {
        truncationThreshold = deserialized["truncation-threshold"] as Int
        batchingInteval = deserialized["batching-inteval"] as Int
        lastwillCapacity = deserialized["lastwill-message-capacity"] as Int
        sessionInputPrefix = deserialized["session-input-prefix"] as String
    }

    override fun onSave(serialized: HashMap<String, Any>)
    {
        serialized["truncation-threshold"] = truncationThreshold
        serialized["batching-inteval"] = batchingInteval
        serialized["lastwill-message-capacity"] = lastwillCapacity
        serialized["session-input-prefix"] = sessionInputPrefix
    }
}