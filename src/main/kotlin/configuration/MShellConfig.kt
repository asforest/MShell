package com.github.asforest.mshell.configuration

object MShellConfig : YamlConfig("config.yml")
{
    /**
     * 会话输入前缀，如果不为空则只有带有此前缀的QQ消息才会被识别成发往子进程的消息，否则会被忽略
     *
     * 如果此选项为空，则任何消息都会被视为是发往子进程的消息
     */
    var sessionInputPrefix: String = ""

    /**
     * 群聊会话中不显示连接和断开连接的状态消息，其它消息不受影响
     */
    var silentInGroup: Boolean = false

    override fun onLoad(deserialized: HashMap<String, Any>)
    {
        sessionInputPrefix = deserialized["session-input-prefix"] as String? ?: ""
        silentInGroup = deserialized["silent-in-group"] as Boolean? ?: false
    }

    override fun onSave(serialized: HashMap<String, Any>)
    {
        serialized["session-input-prefix"] = sessionInputPrefix
        serialized["silent-in-group"] = silentInGroup
    }
}