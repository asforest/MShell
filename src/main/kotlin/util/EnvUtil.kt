package com.github.asforest.mshell.util

import java.net.URLDecoder

object EnvUtil
{
    /**
     * 程序是否被打包
     */
    @JvmStatic
    val isPackaged: Boolean get() = javaClass.getResource("").protocol != "file"

    /**
     * 获取当前Jar文件路径（仅打包后有效）
     */
    @JvmStatic
    val jarFile: FileObj get() = FileObj(URLDecoder.decode(EnvUtil.javaClass.protectionDomain.codeSource.location.file, "UTF-8"))
}