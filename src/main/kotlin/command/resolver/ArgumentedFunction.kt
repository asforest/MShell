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

    private fun buildArgs(extensionReceiver: Any?): kotlin.Array<Any?>
    {
        return (if(extensionReceiver != null && isExtensionFunction)
            listOf(extensionReceiver, *arguments.toTypedArray())
        else
            listOf(*arguments.toTypedArray())).toTypedArray()
    }

    @JvmOverloads
    suspend fun callSuspend(extensionReceiver: Any? = null)
    {
        val args = buildArgs(extensionReceiver)
        signature.callable.callSuspend(thisRef, *args)
    }

    @JvmOverloads
    suspend fun call(extensionReceiver: Any? = null)
    {
        val args = buildArgs(extensionReceiver)
        signature.callable.callSuspend(thisRef, *args)
    }
}