package com.github.asforest.mshell.command

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.configuration.PresetsConfig
import com.github.asforest.mshell.exception.external.*
import com.github.asforest.mshell.model.EnvironmentalPreset
import com.github.asforest.mshell.permission.MShellPermissions
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import java.nio.charset.Charset

@ConsoleExperimentalApi
object PresetCommand : CompositeCommand(
    MShellPlugin,
    primaryName = "mshellp",
    description = "MShell插件环境预设管理指令",
    secondaryNames = arrayOf("msp", "mp", "mse", "me"),
    parentPermission = MShellPermissions.all
) {
    val ep: PresetsConfig get() = PresetsConfig

    @SubCommand @Description("创建一个环境预设")
    suspend fun CommandSender.add(
        @Name("preset") presetName: String,
        @Name("charset") charset: String,
        @Name("shell") vararg shell: String
    ) {
        withCatch {
            if (presetName in ep.presets.keys)
                throw PresetAlreadyExistedYetException("环境预设'$presetName'已经存在了")
            if(!Charset.isSupported(charset))
                throw UnsupportedCharsetException(charset)
            if(shell.isEmpty())
                throw MissingParamaterException("shell")

            ep.presets[presetName] = EnvironmentalPreset(
                name = name,
                shell = shell.joinToString(" "),
                charset = charset,
            )

            // 如果这是创建的第一个预设，就设置为默认预设
            if(ep.defaultPreset == "")
                ep.defaultPreset = presetName

            ep.write()
            list(presetName)
        }
    }

    @SubCommand @Description("删除一个环境预设")
    suspend fun CommandSender.remove(
        @Name("preset") presetName: String
    ) {
        withCatch {
            getPresetWithThrow(presetName)
            ep.presets.remove(presetName)
            ep.write()
            sendMessage("已删除环境预设'$presetName'")
        }
    }

    @SubCommand @Description("列出所有环境预设配置")
    suspend fun CommandSender.list(
        @Name("preset") presetName: String? = null
    ) {
        var output = ""
        ep.presets.filter { presetName==null || presetName in it.key }.forEach {
            output += "${it.key}: ${it.value}\n"
        }
        sendMessage(output.ifEmpty { "还没有任何环境预设" })
    }

    @SubCommand @Description("设置会话的启动程序")
    suspend fun CommandSender.shell(
        @Name("preset")presetName: String,
        @Name("shell") vararg shell: String
    ) {
        withCatch {
            val preset = getPresetWithThrow(presetName)
            if(shell.isEmpty())
            {
                ep.presets[presetName]!!.shell = ""
                sendMessage("已清空${presetName}预设的shell选项")
            } else {
                val s = shell.joinToString(" ")
                ep.presets[presetName]!!.shell = s
                sendMessage("已更新${presetName}预设的shell: '$s'")
            }
            ep.write()
        }
    }

    @SubCommand @Description("设置会话的工作目录")
    suspend fun CommandSender.cwd(
        @Name("preset")presetName: String,
        @Name("dir") vararg dir: String
    ) {
        withCatch {
            val preset = getPresetWithThrow(presetName)
            if(dir.isEmpty())
            {
                ep.presets[presetName]!!.workdir = ""
                sendMessage("已清空${presetName}预设的cwd选项")
            } else {
                val d = dir.joinToString(" ")
                ep.presets[presetName]!!.workdir = d
                sendMessage("已更新${presetName}预设的cwd: '$d'")
            }
            ep.write()
        }
    }

    @SubCommand @Description("设置会话的环境变量")
    suspend fun CommandSender.env(
        @Name("preset") presetName: String,
        @Name("key") key: String = "",
        @Name("value") vararg value: String
    ) {
        withCatch {
            val preset = getPresetWithThrow(presetName)
            if(key.isNotEmpty())
            {
                if(value.isEmpty())
                {
                    ep.presets[presetName]!!.env.remove(key)
                    sendMessage("已移除${presetName}预设的环境变量: '$key'")
                } else {
                    val v = value.joinToString(" ")
                    ep.presets[presetName]!!.env[key] = v
                    sendMessage("已更新${presetName}预设的环境变量'$key': '$v'")
                }
                ep.write()
            }
            sendMessage(preset.env.toString())
        }
    }

    @SubCommand @Description("设置会话的初始化命令")
    suspend fun CommandSender.exec(
        @Name("preset") presetName: String,
        @Name("exec") vararg exec: String
    ) {
        withCatch {
            val preset = getPresetWithThrow(presetName)
            if(exec.isEmpty())
            {
                ep.presets[presetName]!!.exec = ""
                sendMessage("已清空${presetName}预设的exec选项")
            } else {
                val e = exec.joinToString(" ")
                ep.presets[presetName]!!.exec = e
                sendMessage("已更新${presetName}预设的exec: '$e'")
            }
            ep.write()
        }
    }

    @SubCommand @Description("设置会话标准输入输出使用的字符集")
    suspend fun CommandSender.charset(
        @Name("preset") presetName: String,
        @Name("charset") charset: String =""
    ) {
        withCatch {
            val preset = getPresetWithThrow(presetName)
            if(charset.isEmpty()) {
                ep.presets[presetName]!!.charset = ""
                sendMessage("已清空${presetName}预设的charset选项")
            } else {
                if(!Charset.isSupported(charset))
                    throw UnsupportedCharsetException(charset)
                ep.presets[presetName]!!.charset = charset
                sendMessage("已更新${presetName}预设的charset: '$charset'")
            }
            ep.write()
        }
    }

    @SubCommand @Description("设置会话单实例约束")
    suspend fun CommandSender.singleins(
        @Name("preset") preset: String,
        @Name("singleinstance") singleins: Boolean
    ) {
        withCatch {
            val _preset = getPresetWithThrow(preset)
            _preset.singleInstance = singleins

            if(singleins)
                sendMessage("已启用${preset}预设的单实例约束")
             else
                sendMessage("已禁用${preset}预设的单实例约束")

            ep.write()
        }
    }

    @SubCommand @Description("设置默认的环境预设方案")
    suspend fun CommandSender.def(
        @Name("preset") presetName: String? =null
    ) {
        withCatch {
            if(presetName != null)
            {
                val preset = getPresetWithThrow(presetName)
                ep.defaultPreset = presetName
                ep.write()
                sendMessage("已更新默认环境预设: '$presetName'")
            } else {
                sendMessage("当前默认环境预设是: ${ep.defaultPreset}")
            }
        }
    }

    @SubCommand @Description("从配置文件重新加载环境预设方案")
    suspend fun CommandSender.reload() {
        withCatch {
            ep.read()
            sendMessage("环境预设配置文件重载完成")
        }
    }

    fun getPresetWithThrow(presetName: String): EnvironmentalPreset
    {
        return ep.presets[presetName] ?: throw PresetNotFoundException(presetName)
    }

    suspend inline fun CommandSender.withCatch(block: CommandSender.() -> Unit)
    {
        try { block() } catch (e: BaseExternalException) { sendMessage(e.message ?: e.stackTraceToString()) }
    }
}