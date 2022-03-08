package com.github.asforest.mshell.command.resolver

import kotlin.reflect.full.callSuspend

/**
 * @param signature: 函数签名
 * @param arguments: 调用函数时要用到的实参
 * @param thisRef: Function的this引用
 */
data class ArgumentedFunction(
    val signature: CommandSignature,
    val arguments: List<Any?>,
    val thisRef: Any
) {
    private val isExtensionFunction: Boolean = signature.extensionReceiverParameter != null

    @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
    private inline fun buildArgs(extensionReceiver: Any?): Array<Any?>
    {
        return if(extensionReceiver != null && isExtensionFunction)
            arrayOf(extensionReceiver, *arguments.toTypedArray())
        else
            arguments.toTypedArray()
    }

    @JvmOverloads
    suspend fun callSuspend(extensionReceiver: Any? = null)
    {
        val args = buildArgs(extensionReceiver)
//        println("call: ${args.map { it.toString() }} | value args:  $arguments")
        signature.callable.callSuspend(thisRef, *args)
    }

    @JvmOverloads
    suspend fun call(extensionReceiver: Any? = null)
    {
        val args = buildArgs(extensionReceiver)
        signature.callable.callSuspend(thisRef, *args)
    }
}