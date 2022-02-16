package com.github.asforest.mshell.command.mshell

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.command.mshell.MShellCommand.Admin
import com.github.asforest.mshell.command.resolver.AbstractSmartCommand
import com.github.asforest.mshell.configuration.PresetsConfig
import com.github.asforest.mshell.exception.business.MissingParamaterException
import com.github.asforest.mshell.exception.business.PresetAlreadyExistedYetException
import com.github.asforest.mshell.exception.business.PresetNotFoundException
import com.github.asforest.mshell.exception.business.UnsupportedCharsetException
import com.github.asforest.mshell.model.Preset
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import java.nio.charset.Charset

@ConsoleExperimentalApi
object PresetCommand : AbstractSmartCommand()
{
    val ep: PresetsConfig by lazy { PresetsConfig }

    @CommandFunc(desc = "创建一个环境预设", permission = Admin)
    suspend fun CommandSender.add(preset: String, charset: String, vararg shell: String)
    {
        withCatch {
            if (preset in ep.presets.keys)
                throw PresetAlreadyExistedYetException("环境预设 $preset 已经存在了，不能重复创建")
            if(!Charset.isSupported(charset))
                throw UnsupportedCharsetException(charset)
            if(shell.isEmpty())
                throw MissingParamaterException("shell")

            ep.presets[preset] = Preset(
                name = preset,
                command = shell.joinToString(" "),
                charset = charset,
            )

            // 如果这是创建的第一个预设，就设置为默认预设
            if(ep.defaultPreset == "")
                ep.defaultPreset = preset

            ep.write()
            list(preset)
        }
    }

    @CommandFunc(desc = "删除一个环境预设", permission = Admin)
    suspend fun CommandSender.remove(preset: String)
    {
        withCatch {
            getPresetWithThrow(preset)
            ep.presets.remove(preset)
            ep.write()
            sendMessage("已删除环境预设 $preset")
        }
    }

    @CommandFunc(desc = "列出所有环境预设配置", permission = Admin)
    suspend fun CommandSender.list(preset: String? = null)
    {
        var output = ""
        ep.presets.filter { preset==null || preset in it.key }.forEach {
            output += "${it.key}: ${it.value}\n"
        }
        sendMessage(output.ifEmpty { "还没有任何环境预设" })
    }

    @CommandFunc(desc = "设置会话的启动程序", permission = Admin)
    suspend fun CommandSender.shell(preset: String, vararg shell: String)
    {
        withCatch {
            getPresetWithThrow(preset)

            if(shell.isEmpty())
            {
                ep.presets[preset]!!.command = ""
                sendMessage("已清空 $preset 环境预设的 shell 选项")
            } else {
                val s = shell.joinToString(" ")
                ep.presets[preset]!!.command = s
                sendMessage("已更新 $preset 环境预设的 shell 为 $s")
            }
            ep.write()
        }
    }

    @CommandFunc(desc = "设置会话的工作目录", permission = Admin)
    suspend fun CommandSender.cwd(preset: String, vararg dir: String)
    {
        withCatch {
            getPresetWithThrow(preset)

            if(dir.isEmpty())
            {
                ep.presets[preset]!!.workdir = ""
                sendMessage("已清空 $preset 环境预设的 cwd 选项")
            } else {
                val d = dir.joinToString(" ")
                ep.presets[preset]!!.workdir = d
                sendMessage("已更新 $preset 环境预设的 cwd 为 $d")
            }
            ep.write()
        }
    }

    @CommandFunc(desc = "设置会话的环境变量", permission = Admin)
    suspend fun CommandSender.env(preset: String, key: String = "", vararg value: String)
    {
        withCatch {
            val _preset = getPresetWithThrow(preset)
            if(key.isNotEmpty())
            {
                if(value.isEmpty())
                {
                    ep.presets[preset]!!.env.remove(key)
                    sendMessage("已移除 $preset 环境预设的环境变量键 $key")
                } else {
                    val v = value.joinToString(" ")
                    ep.presets[preset]!!.env[key] = v
                    sendMessage("已更新 $preset 环境预设的环境变量 $key=$v")
                }
                ep.write()
            }
            sendMessage(_preset.env.toString())
        }
    }

    @CommandFunc(desc = "设置会话的初始化命令", permission = Admin)
    suspend fun CommandSender.exec(preset: String, vararg exec: String)
    {
        withCatch {
            getPresetWithThrow(preset)

            if(exec.isEmpty())
            {
                ep.presets[preset]!!.input = ""
                sendMessage("已清空 $preset 环境预设的 exec 选项")
            } else {
                val e = exec.joinToString(" ")
                ep.presets[preset]!!.input = e
                sendMessage("已更新 $preset 环境预设的 exec 为 $e")
            }
            ep.write()
        }
    }

    @CommandFunc(desc = "设置会话标准输入输出使用的字符集", permission = Admin)
    suspend fun CommandSender.charset(preset: String, charset: String ="")
    {
        withCatch {
            getPresetWithThrow(preset)

            if(charset.isEmpty()) {
                ep.presets[preset]!!.charset = ""
                sendMessage("已清空 $preset 环境预设的 charset 选项")
            } else {
                if(!Charset.isSupported(charset))
                    throw UnsupportedCharsetException(charset)
                ep.presets[preset]!!.charset = charset
                sendMessage("已更新 $preset 环境预设的 charset 为 $charset")
            }
            ep.write()
        }
    }

    @CommandFunc(desc = "设置会话单实例约束", permission = Admin)
    suspend fun CommandSender.singleins(preset: String, singleins: Boolean)
    {
        withCatch {
            val _preset = getPresetWithThrow(preset)
            _preset.singleInstance = singleins

            if(singleins)
                sendMessage("已启用环境预设 $preset 的单实例约束")
             else
                sendMessage("已禁用环境预设 $preset 的单实例约束")

            ep.write()
        }
    }

    @CommandFunc(desc = "设置会话的终端宽度", permission = Admin)
    suspend fun CommandSender.columns(preset: String, columns: Int)
    {
        withCatch {
            val _preset = getPresetWithThrow(preset)
            _preset.columns = columns
            sendMessage("环境预设 $preset 的终端宽度已更新为 $columns")
            ep.write()
        }
    }

    @CommandFunc(desc = "设置会话的终端高度", permission = Admin)
    suspend fun CommandSender.rows(preset: String, rows: Int)
    {
        withCatch {
            val _preset = getPresetWithThrow(preset)
            _preset.rows = rows
            sendMessage("环境预设 $preset 的终端高度已更新为 $rows")
            ep.write()
        }
    }

    @CommandFunc(desc = "设置会话的stdout合并字符数上限", permission = Admin)
    suspend fun CommandSender.truncation(preset: String, thresholdInChars: Int)
    {
        withCatch {
            val _preset = getPresetWithThrow(preset)
            _preset.truncationThreshold = thresholdInChars
            sendMessage("环境预设 $preset 的合并字符数上限已更新为 $thresholdInChars 字符")
            ep.write()
        }
    }

    @CommandFunc(desc = "设置会话的stdout合并间隔", permission = Admin)
    suspend fun CommandSender.batch(preset: String, intevalInMs: Int)
    {
        withCatch {
            val _preset = getPresetWithThrow(preset)
            _preset.batchingInteval = intevalInMs
            sendMessage("环境预设 $preset 的合并间隔已更新为 $intevalInMs ms")
            ep.write()
        }
    }

    @CommandFunc(desc = "设置会话的遗愿消息缓冲区大小", permission = Admin)
    suspend fun CommandSender.lastwill(preset: String, capacityInChars: Int)
    {
        withCatch {
            val _preset = getPresetWithThrow(preset)
            _preset.lastwillCapacity = capacityInChars
            sendMessage("环境预设 $preset 的遗愿消息缓冲区大小已更新为 $capacityInChars 字符")
            ep.write()
        }
    }

    @CommandFunc(desc = "设置默认的环境预设方案", permission = Admin)
    suspend fun CommandSender.def(preset: String? =null)
    {
        withCatch {
            if(preset != null)
            {
                getPresetWithThrow(preset)

                ep.defaultPreset = preset
                ep.write()
                sendMessage("已设置默环境认预设为 $preset")
            } else {
                sendMessage("当前默认环境预设是 ${ep.defaultPreset}")
            }
        }
    }

    @CommandFunc(desc = "从配置文件重新加载环境预设方案", permission = Admin)
    suspend fun CommandSender.reload() {
        withCatch {
            ep.read()
            sendMessage("环境预设配置文件重载完成")
        }
    }


    @Suppress("NOTHING_TO_INLINE")
    inline fun getPresetWithThrow(presetName: String): Preset
    {
        return ep.presets[presetName] ?: throw PresetNotFoundException(presetName)
    }

    suspend inline fun CommandSender.withCatch(block: CommandSender.() -> Unit)
    {
        MShellPlugin.catchException(user) { block() }
    }
}