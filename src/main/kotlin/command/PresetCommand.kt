package com.github.asforest.mshell.command

import com.github.asforest.mshell.MShellPlugin
import com.github.asforest.mshell.command.MShellCommand.Admin
import com.github.asforest.mshell.command.MShellCommand.CallingContext
import com.github.asforest.mshell.command.resolver.TreeCommand
import com.github.asforest.mshell.configuration.PresetsConfig
import com.github.asforest.mshell.exception.business.MissingParamaterException
import com.github.asforest.mshell.exception.business.PresetAlreadyExistedYetException
import com.github.asforest.mshell.exception.business.PresetNotFoundException
import com.github.asforest.mshell.exception.business.UnsupportedCharsetException
import com.github.asforest.mshell.data.Preset
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import java.nio.charset.Charset

@ConsoleExperimentalApi
object PresetCommand : TreeCommand()
{
    val ep: PresetsConfig by lazy { PresetsConfig }

    @Command(desc = "创建一个环境预设", aliases = ["a"], permission = Admin)
    suspend fun CallingContext.add(preset: String, charset: String, vararg shell: String)
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

    @Command(desc = "删除一个环境预设", aliases = ["r"], permission = Admin)
    suspend fun CallingContext.remove(preset: String)
    {
        withCatch {
            getPresetWithThrow(preset)
            ep.presets.remove(preset)
            ep.write()
            sendMessage("已删除环境预设 $preset")
        }
    }

    @Command(desc = "列出所有环境预设配置", aliases = ["l"], permission = Admin)
    suspend fun CallingContext.list(preset: String? = null)
    {
        var output = ""
        ep.presets.filter { preset==null || preset in it.key }.forEach {
            output += "${it.key}: ${it.value}\n\n"
        }
        sendMessage(output.trim().ifEmpty { "还没有任何环境预设" })
    }

    @Command(desc = "设置默认的环境预设方案", aliases = ["d"], permission = Admin)
    suspend fun CallingContext.def(preset: String? =null)
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

    @Command(desc = "从配置文件重新加载环境预设方案", aliases = ["r"], permission = Admin)
    suspend fun CallingContext.reload() {
        withCatch {
            ep.read()
            sendMessage("环境预设配置文件重载完成")
        }
    }

    @Command(desc = "设置会话的启动命令行", aliases = ["shell"], permission = Admin)
    suspend fun CallingContext.cmd(preset: String, vararg shell: String)
    {
        withCatch {
            getPresetWithThrow(preset)

            if(shell.isEmpty())
            {
                ep.presets[preset]!!.command = ""
                sendMessage("已清空 $preset 环境预设的启动命令行选项")
            } else {
                val s = shell.joinToString(" ")
                ep.presets[preset]!!.command = s
                sendMessage("已更新 $preset 环境预设的启动命令行为 $s")
            }
            ep.write()
        }
    }

    @Command(desc = "设置会话的工作目录", permission = Admin)
    suspend fun CallingContext.cwd(preset: String, vararg dir: String)
    {
        withCatch {
            getPresetWithThrow(preset)

            if(dir.isEmpty())
            {
                ep.presets[preset]!!.workdir = ""
                sendMessage("已清空 $preset 环境预设的工作目录选项")
            } else {
                val d = dir.joinToString(" ")
                ep.presets[preset]!!.workdir = d
                sendMessage("已更新 $preset 环境预设的工作目录为 $d")
            }
            ep.write()
        }
    }

    @Command(desc = "设置会话的环境变量", permission = Admin)
    suspend fun CallingContext.env(preset: String, key: String = "", vararg value: String)
    {
        withCatch {
            val _preset = getPresetWithThrow(preset)
            if(key.isNotEmpty())
            {
                if(value.isEmpty())
                {
                    ep.presets[preset]!!.env.remove(key)
                    sendMessage("已移除 $preset 环境预设的环境变量 $key")
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

    @Command(desc = "设置会话的初始化输入", aliases = ["exec"], permission = Admin)
    suspend fun CallingContext.initial(preset: String, vararg exec: String)
    {
        withCatch {
            getPresetWithThrow(preset)

            if(exec.isEmpty())
            {
                ep.presets[preset]!!.initialInput = ""
                sendMessage("已清空 $preset 环境预设的初始化输入")
            } else {
                val e = exec.joinToString(" ")
                ep.presets[preset]!!.initialInput = e
                sendMessage("已更新 $preset 环境预设的初始化输入为 $e")
            }
            ep.write()
        }
    }

    @Command(desc = "设置会话标准输入输出使用的字符集", permission = Admin)
    suspend fun CallingContext.charset(preset: String, charset: String ="")
    {
        withCatch {
            getPresetWithThrow(preset)

            if(charset.isEmpty()) {
                ep.presets[preset]!!.charset = ""
                sendMessage("已清空 $preset 环境预设的编码选项")
            } else {
                if(!Charset.isSupported(charset))
                    throw UnsupportedCharsetException(charset)
                ep.presets[preset]!!.charset = charset
                sendMessage("已更新 $preset 环境预设的编码为 $charset")
            }
            ep.write()
        }
    }

    @Command(desc = "设置会话单实例约束", aliases = ["singleins"], permission = Admin)
    suspend fun CallingContext.single(preset: String, singleins: Boolean)
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

    @Command(desc = "设置会话是否使用Json模式运行", permission = Admin)
    suspend fun CallingContext.jsonmode(preset: String, jsonMode: Boolean)
    {
        withCatch {
            val _preset = getPresetWithThrow(preset)
            _preset.jsonMode = jsonMode

            if(jsonMode)
                sendMessage("已启用环境预设 $preset 的Json模式")
            else
                sendMessage("已禁用环境预设 $preset 的Json模式")

            ep.write()
        }
    }

    @Command(desc = "设置会话是否使用PTY模式运行", permission = Admin)
    suspend fun CallingContext.ptymode(preset: String, ptyMode: Boolean)
    {
        withCatch {
            val _preset = getPresetWithThrow(preset)
            _preset.ptyMode = ptyMode

            if(ptyMode)
                sendMessage("已启用环境预设 $preset 的PTY模式")
            else
                sendMessage("已禁用环境预设 $preset 的PTY模式")

            ep.write()
        }
    }

    @Command(desc = "设置是否屏蔽群聊内的会话连接和断开等状态消息", permission = Admin)
    suspend fun CallingContext.silent(preset: String, silentMode: Boolean)
    {
        withCatch {
            val _preset = getPresetWithThrow(preset)
            _preset.silentMode = silentMode

            if(silentMode)
                sendMessage("已启用环境预设 $preset 的Silent模式")
            else
                sendMessage("已禁用环境预设 $preset 的Silent模式")

            ep.write()
        }
    }

    @Command(desc = "设置会话的终端宽度", permission = Admin)
    suspend fun CallingContext.columns(preset: String, columns: Int)
    {
        withCatch {
            val _preset = getPresetWithThrow(preset)
            _preset.columns = columns
            sendMessage("环境预设 $preset 的终端宽度已更新为 $columns")
            ep.write()
        }
    }

    @Command(desc = "设置会话的终端高度", permission = Admin)
    suspend fun CallingContext.rows(preset: String, rows: Int)
    {
        withCatch {
            val _preset = getPresetWithThrow(preset)
            _preset.rows = rows
            sendMessage("环境预设 $preset 的终端高度已更新为 $rows")
            ep.write()
        }
    }

    @Command(desc = "设置会话的stdout合并间隔", permission = Admin)
    suspend fun CallingContext.batch(preset: String, intevalInMs: Int)
    {
        withCatch {
            val _preset = getPresetWithThrow(preset)
            _preset.batchingInteval = intevalInMs
            sendMessage("环境预设 $preset 的合并间隔已更新为 $intevalInMs ms")
            ep.write()
        }
    }

    @Command(desc = "设置会话的stdout合并字符数上限", permission = Admin)
    suspend fun CallingContext.truncation(preset: String, thresholdInChars: Int)
    {
        withCatch {
            val _preset = getPresetWithThrow(preset)
            _preset.truncationThreshold = thresholdInChars
            sendMessage("环境预设 $preset 的合并字符数上限已更新为 $thresholdInChars 字符")
            ep.write()
        }
    }


    @Command(desc = "设置会话的历史消息缓冲区大小", aliases = ["lastwill"], permission = Admin)
    suspend fun CallingContext.history(preset: String, capacityInChars: Int)
    {
        withCatch {
            val _preset = getPresetWithThrow(preset)
            _preset.historyCapacity = capacityInChars
            sendMessage("环境预设 $preset 的历史消息缓冲区大小已更新为 $capacityInChars 字符")
            ep.write()
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun getPresetWithThrow(presetName: String): Preset
    {
        return ep.presets[presetName] ?: throw PresetNotFoundException(presetName)
    }

    suspend inline fun CallingContext.withCatch(block: CallingContext.() -> Unit)
    {
        MShellPlugin.catchException(sender.user) { block() }
    }
}