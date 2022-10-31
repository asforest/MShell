package com.github.asforest.mshell.configuration

object MShellConfig : YamlConfig("config.yml")
{
    /**
     * 会话输入前缀，如果不为空则只有带有此前缀的QQ消息才会被识别成发往子进程的消息，否则会被忽略
     *
     * 如果此选项为空，则任何消息都会被视为是发往子进程的消息
     */
    var sessionInputPrefix: String = ""

    override fun onLoad(deserialized: HashMap<String, Any>)
    {
        sessionInputPrefix = deserialized["session-input-prefix"] as String? ?: ""
    }

    override fun onSave(serialized: HashMap<String, Any>)
    {
        serialized["session-input-prefix"] = sessionInputPrefix
    }
}