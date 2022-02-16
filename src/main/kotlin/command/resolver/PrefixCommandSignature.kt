package com.github.asforest.mshell.command.resolver

/**
 * 带前缀的CommandSignature
 */
data class PrefixCommandSignature(
    val prefix: String,
    val signature: CommandSignature,
)