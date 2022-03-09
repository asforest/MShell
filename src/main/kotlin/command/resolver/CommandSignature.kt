package com.github.asforest.mshell.command.resolver

import kotlin.reflect.*
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

/**
 * @param name: callable 标识符名字
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
    /**
     * 字符串化后的parameters
     */
    val params = parameters.joinToString(" ") { p -> p.identity }

    companion object {
        @JvmStatic
        fun CreateFromKF(kf: KFunction<*>, prefix: String, permissionMask: Int, description: String): CommandSignature
        {
            val parameters = kf.valueParameters.map { Parameter(it.name ?: "", it.isOptional, it.isVararg, it.type) }

            return CommandSignature(prefix, kf, parameters, kf.extensionReceiverParameter, permissionMask, description)
        }
    }

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

        val genericType: KClass<*> by lazy {
            check(isVararg) { "can not access 'genericType' because paramter '$name' is not a Vararg" }

            if (isPrimitiveArrayType)
                type.jvmErasure.java.componentType.kotlin
            else
                type.arguments.first().type!!.classifier as KClass<*>
        }

//        val actualGenericType = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<*>

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