package com.github.asforest.mshell.command.resolver

/**
 * 带前缀版本的CommandSignature
 */
data class PrefixedCommandSignature(
    val prefix: String,
    val signature: CommandSignature,
)