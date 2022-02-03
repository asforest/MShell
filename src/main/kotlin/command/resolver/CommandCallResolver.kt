package com.github.asforest.mshell.command.resolver

import java.lang.reflect.Array
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.jvmErasure

object CommandCallResolver
{
    fun resolve(signature: CommandSignature, arguments: List<String>): ArgumentedFunction?
    {
        val parameters = signature.parameters
//        println(arguments)

        val zipped = parameters.zip(arguments).toMutableList()
        val remainingParameters = parameters.drop(zipped.size).toMutableList()

        // 缺少参数
        if (remainingParameters.any { !it.isOptional && !it.isVararg })
            return null

        // 最终实参
        val argumentsToCall = mutableListOf<Any?>()

        // 合并可变参数
        if(arguments.size > parameters.size && zipped.last().first.isVararg)
        {
            val (vParameters, _) = zipped.removeLast()
            zipped.add(vParameters to arguments.drop(zipped.size).joinToString(" "))
        }

        var argIndex = 0

        // 解析普通参数
        for ((parameter, argument) in zipped)
        {
//            println(">> ${parameter.name} => $parameter    ${parameter.isOptional}     ${parameter.isVararg}")

            if (parameter.isVararg)
            {
                val parser = BuiltinArgumentParsers.parsers[parameter.genaricType]
                if (parser != null)
                {
                    if (parameter.isBasicArrayType)
                    {
                        val varargs = argument.split(" ").map { parser.parse(it, argIndex, signature) }
                        val array = Array.newInstance(parameter.type.jvmErasure.java.componentType, varargs.size)

                        for (i in varargs.indices)
                            Array.set(array, i, varargs[i])

                        argumentsToCall.add(array)
                    } else {
                        for (el in argument.split(" "))
                            argumentsToCall += parser.parse(el, argIndex, signature)
                    }
                }
            } else {
                val parser = BuiltinArgumentParsers.parsers[parameter.type.jvmErasure]
                if (parser != null)
                    argumentsToCall += parser.parse(argument, argIndex, signature)
            }

            argIndex += 1
        }

        // 解析可选参数
        for (remainingParameter in remainingParameters)
        {
            // 使用空数组
            if (remainingParameter.isVararg)
                argumentsToCall += Array.newInstance(remainingParameter.type.jvmErasure.java.componentType, 0)
        }

        println(argumentsToCall.toString2())

        return ArgumentedFunction(signature.callable, argumentsToCall)
    }

    private fun MutableList<Any?>.toString2(): String
    {
        val list = this.map {
            if(it == null)
                "null"
            else if(it::class.java.isArray)
                (0 until Array.getLength(it))
                    .map { idx -> Array.get(it, idx).toString() }
            else
                it.toString()
        }.joinToString(", ")

        return "[$list]"
    }

    data class ArgumentedFunction(
        val function: KFunction<*>,
        val arguments: List<Any?>
    ) {
        suspend fun callSuspend(instance: Any) = function.callSuspend(instance, *arguments.toTypedArray())

        fun call(instance: Any) = function.call(instance, *arguments.toTypedArray())
    }
}