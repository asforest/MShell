package com.github.asforest.mshell.command.resolver

import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.*

abstract class SmartCommand
{
    protected val smartFunctions: Map<String, CommandSignature> by lazy {
//        val actualGenaricType = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<*>

        this::class.functions
            .filter { it.isSubCommandFunction() }
            .onEach { it.checkExtensionReceiver() }
            .onEach { it.checkModifiers() }
            .associate { it.name to createCommandSignature(it) }
    }

    /**
     * 尝试解析Arguments
     * @param commandText: 要被解析的Arguments
     * @return 解析好的函数对象，如果未能解析，则返回null
     * @throws AbstractArgumentParsers.ArgumentParserException 当类型不正确时
     */
    protected fun resolveCommandText(commandText: String): CommandCallResolver.ArgumentedFunction?
    {
        if(commandText.isEmpty())
            return null

        val splited = commandText.split(" ")
        val subCommandName = splited[0]
        val subCommandArguments = splited.drop(1)

        val signature = smartFunctions[subCommandName] ?: return null
        return CommandCallResolver.resolve(signature, subCommandArguments)
    }

    private fun createCommandSignature(func: KFunction<*>): CommandSignature
    {
        return CommandSignature(func.name, func, func.valueParameters.map {
            CommandSignature.Parameter(
                it.name ?: "",
                it.isOptional,
                it.isVararg,
                it.type
            )
        }, func.findAnnotation<SmartCommandFunc>()?.description ?: "")
    }

    private fun KFunction<*>.isSubCommandFunction(): Boolean = hasAnnotation<SmartCommandFunc>()

    private fun KFunction<*>.checkExtensionReceiver() {
        if(this.extensionReceiverParameter != null)
            throw IllegalDeclarationException("SubCommand function cannot be a extension function")
    }

    private fun KFunction<*>.checkModifiers() {
        if (isInline) throw IllegalDeclarationException("SubCommand function cannot be inline")
        if (visibility == KVisibility.PRIVATE) throw IllegalDeclarationException("SubCommand function must be public.")
        if (this.hasAnnotation<JvmStatic>()) throw IllegalDeclarationException("SubCommand function must not be static.")
        if (isAbstract) throw IllegalDeclarationException("SubCommand function cannot be abstract")
    }

    class IllegalDeclarationException(message: String) : Exception(message)

    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    protected annotation class SmartCommandFunc(
        val description: String = "",
        val aliases: Array<String> = []
    )
}