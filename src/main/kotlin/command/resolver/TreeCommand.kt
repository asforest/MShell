package com.github.asforest.mshell.command.resolver

import net.mamoe.mirai.console.util.safeCast
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaField

abstract class TreeCommand
{
    /**
     * 所有指令函数列表，包括子对象里的。通常用来生成指令列表/帮助信息
     */
    val allCommands: List<PrefixedCommandSignature> by lazy {
        val functions = mutableListOf<PrefixedCommandSignature>()

        for (command in this.commands)
        {
            functions += PrefixedCommandSignature(command.prefix, command.signature)
        }

        fun collectFromMemberProperty(parent: TreeCommand)
        {
            for (subCommand in parent.subCommandObjects.filter { !it.isAlias })
                for (command in subCommand.subtree.commands)
                    functions += PrefixedCommandSignature(subCommand.field + " " + command.prefix, command.signature)
        }

        collectFromMemberProperty(this)

        functions
    }

    /**
     * 所有定义在本类/子类里的指令函数列表
     */
    protected val commands: List<PrefixedCommandSignature> by lazy {
        this::class.functions
            .filter { it.isSubCommandFunction() }
            .onEach { it.checkModifiers() }
            .map { func ->
                val annotation = func.findAnnotation<Command>()!!

                val signature = CommandSignature(func.name, func, func.valueParameters.map {
                    CommandSignature.Parameter(it.name ?: "", it.isOptional, it.isVararg, it.type)
                }, func.extensionReceiverParameter, annotation.permission, annotation.desc)

                PrefixedCommandSignature(func.name, signature)
            }
    }

    /**
     * 所有子指令成员属性
     */
    protected val subCommandObjects: List<SubTreeCommand> by lazy {
        this::class.declaredMemberProperties
            .filter { it.returnType.classifier.safeCast<KClass<*>>()?.isSubclassOf(TreeCommand::class) == true }
            .filter { it.javaField!!.isAnnotationPresent(Tree::class.java) }
            .map {
                val field = it.javaField!!
                val annotation = field.getAnnotation(Tree::class.java)
                val name = annotation.name.ifEmpty { field.name }
                val isStatic = Modifier.isStatic(field.modifiers)
                val isPrivate = Modifier.isPrivate(field.modifiers)
                if(isPrivate)
                    field.isAccessible = true
                val obj = field.get(if(isStatic) null else this) as TreeCommand

                annotation.aliases.map { alias -> SubTreeCommand(alias, obj, true) } + SubTreeCommand(name, obj, false)
            }.flatten()
    }

    /**
     * 尝试解析Arguments
     * @param commands 要被解析的Arguments字符串列表
     * @param callerPermission 调用者拥有的权限，用来判断有无权限执行指令
     * @param prefix 指令前缀。用来知道当前函数是第几层子指令
     * @return 解析结果
     * @throws NoFunctionMatchedException 没有对应的函数（解析失败）
     * @throws TooFewArgumentsException 实参太少
     * @throws TooManyArgumentsException 实参太多
     * @throws AbstractArgumentParsers.ArgumentParserException 解析成功，但参数类型不正确（不匹配）时
     * @throws PermissionDeniedException 解析成功，但权限不够时
     */
    fun resolveCommandText(commands: List<String>, callerPermission: Int, prefix: String): ArgumentedFunction
    {
        if(commands.isEmpty())
            throw NoFunctionMatchedException()

        val label = commands[0]
        val arguments = commands.drop(1)

        val resolveResult = resolveWithinSelf(label, arguments)

        if (resolveResult == null)
        {
            val subCommand = subCommandObjects.firstOrNull { it.field == label } ?: throw NoFunctionMatchedException()
            val pfx = prefix + (if(prefix.isNotEmpty()) " " else "") + subCommand.field
            return subCommand.subtree.resolveCommandText(arguments, callerPermission, pfx)
        }

        when(resolveResult)
        {
            is CommandCallResolver.ResolveResult.TooFewArguments -> throw TooFewArgumentsException(prefix, resolveResult.signature)
            is CommandCallResolver.ResolveResult.TooManyArguments -> throw TooManyArgumentsException(prefix, resolveResult.signature)
            else -> {
                val result = resolveResult as CommandCallResolver.ResolveResult.ResolveCorrect
                if(result.afunction.signature.permissionMask and callerPermission == 0)
                    throw PermissionDeniedException(prefix, result.afunction)
                return result.afunction
            }
        }
    }

    private fun resolveWithinSelf(label: String, arguments: List<String>): CommandCallResolver.ResolveResult?
    {
        var tempResult: CommandCallResolver.ResolveResult? = null

        for (function in this.commands.filter { it.prefix == label })
        {
            tempResult = CommandCallResolver.resolve(function.signature, arguments, this)

            if (tempResult is CommandCallResolver.ResolveResult.ResolveCorrect)
                return tempResult
        }

        return tempResult
    }

    private fun KFunction<*>.isSubCommandFunction(): Boolean = hasAnnotation<Command>()

    private fun KFunction<*>.checkModifiers() {
        if (isInline) throw IllegalDeclarationException("SubCommand function cannot be inline")
        if (visibility == KVisibility.PRIVATE) throw IllegalDeclarationException("SubCommand function must be public.")
        if (this.hasAnnotation<JvmStatic>()) throw IllegalDeclarationException("SubCommand function must not be static.")
        if (isAbstract) throw IllegalDeclarationException("SubCommand function cannot be abstract")
    }

    class IllegalDeclarationException(message: String) : Exception(message)

    class NoFunctionMatchedException : Exception()

    class TooFewArgumentsException(val prefix: String, val signature: CommandSignature) : Exception()

    class TooManyArgumentsException(val prefix: String, val signature: CommandSignature) : Exception()

    class PermissionDeniedException(val prefix: String, val function: ArgumentedFunction) : Exception()

    /**
     * 代表一个定义在子对象里的指令函数列表
     */
    protected data class SubTreeCommand(
        val field: String,
        val subtree: TreeCommand,
        val isAlias: Boolean
    )

    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    protected annotation class Command(
        val desc: String,
        val permission: Int = 0,
    )

    @Target(AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.RUNTIME)
    protected annotation class Tree(
        val name: String = "",
        val aliases: Array<String> = []
    )
}