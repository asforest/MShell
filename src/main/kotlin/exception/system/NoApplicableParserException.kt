package com.github.asforest.mshell.exception.system

import com.github.asforest.mshell.command.resolver.CommandSignature
import com.github.asforest.mshell.exception.AbstractSystemException

class NoApplicableParserException(function: CommandSignature, index: Int, parameter: CommandSignature.Parameter)
    : AbstractSystemException(
    "No applicable parser for " +
            "function '${function.name}'(${function.description}), " +
            "paramter index: $index, " +
            "paramter type: ${parameter.type}, " +
            "generic type(if has): ${if (parameter.isVararg) parameter.genericType.toString() else "not a Vararg"}, " +
            "is paramter primitiveArrayType: ${parameter.isPrimitiveArrayType}"
)