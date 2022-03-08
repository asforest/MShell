package com.github.asforest.mshell.command.resolver

import com.github.asforest.mshell.exception.system.NoApplicableParserException
import java.lang.reflect.Array
import kotlin.reflect.jvm.jvmErasure

object CommandCallResolver
{
    /**
     * 解析具体函数并匹配参数
     * @return 返回解析好的函数。如果失败，返回null
     * @throws AbstractArgumentParsers 如果参数类型对不上
     */
    fun resolve(signature: CommandSignature, arguments: List<String>, thisRef: Any): ResolveResult
    {
        val parameters = signature.parameters
//        println(arguments)

        val zipped = parameters.zip(arguments).toMutableList()
        val remainingParameters = parameters.drop(zipped.size).toMutableList()

        // 缺少参数
        if (remainingParameters.any { !it.isOptional && !it.isVararg })
            return ResolveResult.TooFewArguments(signature)

        // 最终实参
        val argumentsToCall = mutableListOf<Any?>()

        // 合并可变参数
        if(zipped.isNotEmpty() && arguments.size > parameters.size && zipped.last().first.isVararg)
        {
            val (vParameters, _) = zipped.removeLast()
            zipped.add(vParameters to arguments.drop(zipped.size).joinToString(" "))
        } else {
            // 不能有多余参数
            val remainingArguments = arguments.drop(zipped.size)
            if (remainingArguments.isNotEmpty())
                return ResolveResult.TooManyArguments(signature)
        }

//        println("common args: ${zipped.size}, varargs: ${remainingParameters.size}")

        var argIndex = 0

        // 解析普通参数
        for ((parameter, argument) in zipped)
        {
//            println(">> ${parameter.name} => $parameter    ${parameter.isOptional}     ${parameter.isVararg}, arg: $argument")

            if (parameter.isVararg)
            {
                val parser = BuiltinArgumentParsers.parsers[parameter.genaricType]
                if (parser != null)
                {
                    val varargs = argument.split(" ").map { parser.parse(it, argIndex, signature) }
                    val array = Array.newInstance(parameter.type.jvmErasure.java.componentType, varargs.size)

                    for (i in varargs.indices)
                        Array.set(array, i, varargs[i])

                    argumentsToCall.add(array)
                } else {
                    throw NoApplicableParserException(signature, argIndex, parameter)
                }
            } else {
                val parser = BuiltinArgumentParsers.parsers[parameter.type.jvmErasure]
                if (parser != null)
                    argumentsToCall += parser.parse(argument, argIndex, signature)
                else
                    throw NoApplicableParserException(signature, argIndex, parameter)
            }

            argIndex += 1
        }

        // 解析可选参数
        for (remainingParameter in remainingParameters)
        {
            // 使用空数组
            if (remainingParameter.isVararg)
                argumentsToCall += Array.newInstance(remainingParameter.type.jvmErasure.java.componentType, 0)

            // 使用null
            else if (remainingParameter.isOptional)
                argumentsToCall += null

            // 不会再有第三种情况出现
            else
                throw RuntimeException("unknown error")

        }

//        println("arg built: "+argumentsToCall.toString2())

        return ResolveResult.ResolveCorrect(ArgumentedFunction(signature, argumentsToCall, thisRef))
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

    sealed class ResolveResult()
    {
        class TooFewArguments(val signature: CommandSignature) : ResolveResult()

        class TooManyArguments(val signature: CommandSignature) : ResolveResult()

        class ResolveCorrect(val afunction: ArgumentedFunction) : ResolveResult()
    }
}