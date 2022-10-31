package com.github.asforest.mshell.data

/**
 * @param name 预设的名字，不能重复
 * @param command 子进程的启动命令行，用来启动子进程
 * @param charset 子进程stin/stdout的字符集/编码，通常为gb2312或者utf-8（stderr被合并到了stdout）
 * @param workdir 子进程的工作目录
 * @param env 子进程的环境变量（此处可以添加环境变量或者覆盖父进程的环境变量）
 * @param initialInput 子进程启动后立即发往stdin的内容，可用作执行初始化程序脚本或者其它功能
 * @param singleInstance 单实例模式
 * @param columns PTY的宽度（单位：行）
 * @param rows PTY的高度（单位：行）
 * @param truncationThreshold 子进程stdout合并字符数上限（单位：字符）
 * @param batchingInteval 子进程stdout合并间隔（单位：字符）
 * @param lastwillCapacity 遗愿消息缓冲区大小（单位：字符）
 * @param jsonMode 是否启用JsonMode
 * @param ptyMode 是否使用PTY运行会话
 * @param silentMode 是否屏蔽群聊会话内的连接和断开等状态消息，其它消息不受影响
 */
data class Preset(
    var name: String,
    var command: String = "",
    var charset: String = "",
    var workdir: String = "",
    var env: MutableMap<String, String> = mutableMapOf(),
    var initialInput: String = "",
    var singleInstance: Boolean = false,
    var columns: Int = 80,
    var rows: Int = 24,
    var truncationThreshold: Int = 2048,
    var batchingInteval: Int = 300,
    var lastwillCapacity: Int = 2048,
    var jsonMode: Boolean = false,
    var ptyMode: Boolean = true,
    var silentMode: Boolean = false,
) {
    override fun toString(): String
    {
        val str2 = buildList {
            add("command=$command")
            add("charset=$charset")

            if (workdir.isNotEmpty())
                add("workdir=$workdir")

            if (env.isNotEmpty())
                add("env=$env")

            if (initialInput.isNotEmpty())
                add("initial-input=$initialInput")

            add("pty=${if (ptyMode) "${columns}x$rows" else "disabled"}")

            if (truncationThreshold != 2048)
                add("truncation= $truncationThreshold")

            if (batchingInteval != 300)
                add("batch= $batchingInteval")

            if (lastwillCapacity != 2048)
                add("lastwill= $lastwillCapacity")

            if (singleInstance)
                add("single-instance")

            if (jsonMode)
                add("json-mode")

            if (silentMode)
                add("group-silent-mode")
        }

        val str = str2.joinToString(", ")
        return "[$str]"
    }
}