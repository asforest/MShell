package com.github.asforest.mshell.model

/**
 * @param name 预设的名字，不能重复
 * @param command 子进程的启动命令行，用来启动子进程
 * @param charset 子进程stin/stdout的字符集/编码，通常为gb2312或者utf-8（stderr被合并到了stdout）
 * @param workdir 子进程的工作目录
 * @param env 子进程的环境变量（此处可以添加环境变量或者覆盖父进程的环境变量）
 * @param input 子进程启动后立即发往stdin的内容，可用作执行初始化程序脚本或者其它功能
 * @param singleInstance 单实例模式
 * @param columns PTY的宽度（单位：行）
 * @param rows PTY的高度（单位：行）
 * @param truncationThreshold 子进程stdout最大合并字符数的上限（单位：字符）
 * @param batchingInteval 子进程stdout最大合并间隔（单位：字符）
 * @param lastwillCapacity 遗愿消息缓冲区大小（单位：字符）
 */
data class Preset(
    var name: String,
    var command: String = "",
    var charset: String = "",
    var workdir: String = "",
    var env: HashMap<String, String> = HashMap(),
    var input: String = "",
    var singleInstance: Boolean = false,
    var columns: Int = 80,
    var rows: Int = 24,
    var truncationThreshold: Int = 2048,
    var batchingInteval: Int = 300,
    var lastwillCapacity: Int = 2048,
) {
    override fun toString(): String
    {
        val str = listOf(
            "shell=$command",
            "charset=$charset",
            "workdir=$workdir",
            "env=$env",
            "exec=$input",
            "singleInstance=$singleInstance",
            "ptySize=$columns x $rows",
        ).joinToString(", ")

        return "[$str]"
    }
}