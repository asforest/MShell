package com.github.asforest.mshell.command.resolver

import kotlin.reflect.KClass

object BuiltinArgumentParsers
{
    val parsers = mutableMapOf<KClass<*>, AbstractArgumentParsers<*>>(
        Int::class to IntArgumentParser(),
        String::class to StringArgumentParser(),
        Boolean::class to BooleanArgumentParser(),
        Byte::class to ByteArgumentParser(),
        Short::class to ShortArgumentParser(),
        Long::class to LongArgumentParser(),
        Float::class to FloatArgumentParser(),
        Double::class to DoubleArgumentParser()
    )

    class BooleanArgumentParser : AbstractArgumentParsers<Boolean>()
    {
        override fun onParse(raw: String): Boolean = raw.toBooleanStrictOrNull() ?: fail()
    }

    class ByteArgumentParser : AbstractArgumentParsers<Byte>()
    {
        override fun onParse(raw: String): Byte = raw.toByteOrNull() ?: fail()
    }

    class ShortArgumentParser : AbstractArgumentParsers<Short>()
    {
        override fun onParse(raw: String): Short = raw.toShortOrNull() ?: fail()
    }

    class IntArgumentParser : AbstractArgumentParsers<Int>()
    {
        override fun onParse(raw: String): Int = raw.toIntOrNull() ?: fail()
    }

    class LongArgumentParser : AbstractArgumentParsers<Long>()
    {
        override fun onParse(raw: String): Long = raw.toLongOrNull() ?: fail()
    }

    class FloatArgumentParser : AbstractArgumentParsers<Float>()
    {
        override fun onParse(raw: String): Float = raw.toFloatOrNull() ?: fail()
    }

    class DoubleArgumentParser : AbstractArgumentParsers<Double>()
    {
        override fun onParse(raw: String): Double = raw.toDoubleOrNull() ?: fail()
    }

    class StringArgumentParser : AbstractArgumentParsers<String>()
    {
        override fun onParse(raw: String): String = raw
    }
}