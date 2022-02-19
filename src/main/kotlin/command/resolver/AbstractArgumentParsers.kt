package com.github.asforest.mshell.command.resolver

import java.lang.reflect.ParameterizedType

abstract class AbstractArgumentParsers<TTarget>
{
    protected abstract fun onParse(raw: String): TTarget

    var argumentIndex = 0
    var raw = ""
    lateinit var signature: CommandSignature

    /**
     * 尝试解析一个实参
     * @throws ArgumentParserException 如果参数类型对不上
     */
    fun parse(raw: String, argumentIndex: Int, signature: CommandSignature): TTarget
    {
        this.raw = raw
        this.argumentIndex = argumentIndex
        this.signature = signature

        return onParse(raw)
    }

    @Suppress("NOTHING_TO_INLINE")
    protected inline fun fail(): Nothing
    {
        val actualGenaricType = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<*>
        throw ArgumentParserException(argumentIndex, raw, actualGenaricType, signature)
    }

    class ArgumentParserException(
        val argIndex: Int,
        val raw: String,
        val typeExcepted: Class<*>,
        val signature: CommandSignature,
        cause: Throwable? = null
    ) : IllegalArgumentException("第 $argIndex 个参数 $raw 无法解析为 ${typeExcepted.simpleName}", cause)
}
