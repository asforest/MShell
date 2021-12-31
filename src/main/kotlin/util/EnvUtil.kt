package com.github.asforest.mshell.util

import com.github.asforest.mshell.exception.ManifestNotReadableException
import java.net.URLDecoder
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

object EnvUtil
{
    /**
     * 读取版本信息（程序打包成Jar后才有效）
     * @return Application版本号，如果为打包成Jar则返回null
     */
    val manifest: Map<String, String> get()
    {
        return try {
            (originManifest as Map<String, String>).filterValues { it.isNotEmpty() }
        } catch (e: ManifestNotReadableException) {
            mapOf()
        }
    }

    val originManifest: Attributes
        get()
    {
        if(!isPackaged)
            throw ManifestNotReadableException()

        JarFile(jarFile.path).use { jar ->
            jar.getInputStream(jar.getJarEntry("META-INF/MANIFEST.MF")).use {
                return Manifest(it).mainAttributes
            }
        }
    }

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