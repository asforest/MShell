package com.github.asforest.mshell.util

import com.github.asforest.mshell.exception.ManifestNotReadableException
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

object ManifestUtil
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


    val originManifest: Attributes get()
    {
        if(!EnvUtil.isPackaged)
            throw ManifestNotReadableException()

        JarFile(EnvUtil.jarFile.path).use { jar ->
            jar.getInputStream(jar.getJarEntry("META-INF/MANIFEST.MF")).use {
                return Manifest(it).mainAttributes
            }
        }
    }
}