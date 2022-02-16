package com.github.asforest.mshell.command.resolver

import net.mamoe.mirai.console.util.safeCast
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaField

abstract class AbstractSmartCommand
{
    /**
     * 所有指令函数列表，包括子对象里的。通常用来生成指令列表供用户查看
     * Map<指令前缀, 函数签名>
     */
    val allCommandFunctions: Map<String, CommandSignature> by lazy {
        val functions = mutableListOf<Pair<String, CommandSignature>>()

        for (commandFunction in commandFunctions)
        {
            functions += commandFunction.key to commandFunction.value
        }

        fun findAllNestedCommands(parentObject: AbstractSmartCommand)
        {
            for (nestedCommandFunction in parentObject.nestedCommandFunctions)
                for (nestFun in nestedCommandFunction.value.commandFunctions.values)
                    functions += nestedCommandFunction.key + " " + nestFun.name to nestFun
        }

        findAllNestedCommands(this)

        functions.toMap()
    }

    /**
     * 所有定义在本类里的指令函数列表
     * Map<函数名, 函数签名>
     */
    protected val commandFunctions: Map<String, CommandSignature> by lazy {
//        val actualGenaricType = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<*>

        this::class.functions
            .filter { it.isSubCommandFunction() }
            .onEach { it.checkModifiers() }
            .associate { it.name to createCommandSignature(it) }
    }

    /**
     * 所有定义在SmartCommand类型的子对象里的指令函数列表
     * Map<对象名, 成员变量对象>
     */
    protected val nestedCommandFunctions: Map<String, AbstractSmartCommand> by lazy {
        this::class.declaredMemberProperties
            .filter { it.returnType.classifier.safeCast<KClass<*>>()?.isSubclassOf(AbstractSmartCommand::class) == true }
            .filter { it.javaField!!.isAnnotationPresent(NestedCommand::class.java) }
            .associate {
                val jf = it.javaField!!
                val name = jf.getAnnotation(NestedCommand::class.java).name.ifEmpty { jf.name}
                val isStatic = Modifier.isStatic(jf.modifiers)
                val isPrivate = Modifier.isPrivate(jf.modifiers)
                if(isPrivate)
                    jf.isAccessible = true
                name to it.javaField!!.get(if(isStatic) null else this) as AbstractSmartCommand
            }
    }

    /**
     * 尝试解析Arguments
     * @param commandText 要被解析的Arguments
     * @param callerPermission 调用者拥有的权限，用来判断有无权限执行指令
     * @return 解析好的函数对象，如果未能解析，则返回null
     * @throws AbstractArgumentParsers.ArgumentParserException 解析成功，但参数类型不正确（不匹配）时
     * @throws PermissionDeniedException 解析成功，但权限不够时
     */
    fun resolveCommandText(commandText: String, callerPermission: Int): CommandCallResolver.ArgumentedFunction?
    {
        if(commandText.isEmpty())
            return null

        val splited = commandText.split(" ")
        val label = splited[0]
        val arguments = splited.drop(1)

        val resolved = if(commandFunctions[label] != null) {
            CommandCallResolver.resolve(commandFunctions[label]!!, arguments, this)
        } else {
            nestedCommandFunctions[label]?.resolveCommandText(arguments.joinToString(" "), callerPermission)
        }

        if(resolved != null && resolved.signature.permissionMask and callerPermission == 0)
            throw PermissionDeniedException(resolved)

        return resolved
    }

    private fun createCommandSignature(func: KFunction<*>): CommandSignature
    {
        val annotation = func.findAnnotation<CommandFunc>()!!

        return CommandSignature(func.name, func, func.valueParameters.map {
            CommandSignature.Parameter(
                it.name ?: "",
                it.isOptional,
                it.isVararg,
                it.type
            )
        }, func.extensionReceiverParameter, annotation.permission, annotation.desc)
    }

    private fun KFunction<*>.isSubCommandFunction(): Boolean = hasAnnotation<CommandFunc>()

    private fun KFunction<*>.checkModifiers() {
        if (isInline) throw IllegalDeclarationException("SubCommand function cannot be inline")
        if (visibility == KVisibility.PRIVATE) throw IllegalDeclarationException("SubCommand function must be public.")
        if (this.hasAnnotation<JvmStatic>()) throw IllegalDeclarationException("SubCommand function must not be static.")
        if (isAbstract) throw IllegalDeclarationException("SubCommand function cannot be abstract")
    }

    class IllegalDeclarationException(message: String) : Exception(message)

    class PermissionDeniedException(val argumentedFunction: CommandCallResolver.ArgumentedFunction) : Exception()

    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    protected annotation class CommandFunc(
        val desc: String,
        val permission: Int = 0,
    )

    @Target(AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.RUNTIME)
    protected annotation class NestedCommand(
        val name: String = ""
    )
}