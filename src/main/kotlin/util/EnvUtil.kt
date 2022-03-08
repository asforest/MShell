package com.github.asforest.mshell.util

import com.github.asforest.mshell.exception.system.ManifestNotReadableException
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import java.net.URLDecoder
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

object EnvUtil
{
    private val MF: Map<String, String> = EnvUtil.manifest

    val version: String get() = MF["Mirai-Plugin-Version"] ?: "0.0.0"

    val gitCommit: String get() = MF["Git-Commit"] ?: "<development>"

    val compileTime: String get() = MF["Compile-Time"] ?: "<no compile time>"

    val compileTimeMs: Long get() = MF["Compile-Time-Ms"]?.toLong() ?: 0L

    val pluginDescription: JvmPluginDescription get() = JvmPluginDescription("com.github.asforest.mshell", version, "MShell")

    /**
     * 读取版本信息（程序打包成Jar后才有效）
     * @return Application版本号，如果为打包成Jar则返回null
     */
    val manifest: Map<String, String> get()
    {
        return try {
            originManifest.entries.associate { it.key.toString() to it.value.toString() }
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