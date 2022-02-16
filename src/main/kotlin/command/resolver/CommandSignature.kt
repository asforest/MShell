package com.github.asforest.mshell.command.resolver

import kotlin.reflect.*
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.jvmErasure
import kotlin.streams.toList

/**
 * @param name: 指令名字
 * @param callable: 指令KFunction对象
 * @param parameters: value parameter
 * @param extensionReceiverParameter: extension receiver parameter
 * @param permissionMask: 执行指令所需要的权限
 * @param description: 函数的描述
 */
data class CommandSignature(
    val name: String,
    val callable: KFunction<*>,
    val parameters: List<Parameter>,
    val extensionReceiverParameter: KParameter?,
    val permissionMask: Int,
    val description: String
) {
    data class Parameter(
        val name: String,
        val isOptional: Boolean,
        val isVararg: Boolean,
        val type: KType
    ) {
        val isPrimitiveArrayType: Boolean = type.classifier in primitiveArrayTypes

        val identity: String get() {
            val reg = Regex("[A-Z]")
            val _name = buildString {
                val chars = name.toCharArray()
                for ((index, char) in chars.withIndex())
                {
                    val str = char.toString()
                    if (reg.matches(str))
                    {
                        if (index != 0)
                            append("-")
                        append(str.lowercase())
                    } else {
                        append(str)
                    }
                }
            }
            return if(isOptional)
                "[${_name}]"
            else if (isVararg)
                "[${_name}...]"
            else
                "<${_name}>"
        }

        init {
            if(isVararg)
            {
                check(type.isSubtypeOf(typeOf<Array<out Any?>>()) || isPrimitiveArrayType) {
                    "type must be subtype of Array if vararg. Given $type."
                }
            }
        }

        val genaricType: KClass<*> by lazy {
            check(isVararg) { "can not access 'varargGenaricType' because paramter '$name' is not a Vararg" }

            if (isPrimitiveArrayType)
                type.jvmErasure.java.componentType.kotlin
            else
                type.arguments.first()::class
        }

        override fun toString(): String
        {
//            if (type.jvmErasure == Boolean::class)
//                return "true/false"

            val kc = type.jvmErasure

            val typeText = if (kc.java.isArray)
                "Array<${kc.java.componentType.simpleName}>"
            else
                kc.java.simpleName

            return "$name :$typeText"
        }

        companion object {
            @JvmStatic
            private val primitiveArrayTypes = listOf<KClass<*>>(
                ByteArray::class,
                CharArray::class,
                ShortArray::class,
                IntArray::class,
                LongArray::class,
                FloatArray::class,
                DoubleArray::class,
                BooleanArray::class,
            )
        }
    }
}