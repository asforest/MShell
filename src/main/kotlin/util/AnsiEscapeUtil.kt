package com.github.asforest.mshell.util

object AnsiEscapeUtil
{
    val pattern = Regex("" +
            "[\u001B\u009B]" +
            "[\\[\\]()#;?]*" +
            "(" +
                "(" + "(.*(;.*)*)?" + "\u0007" + ")" +
                "|" +
                "(" + "(\\d{1,4}(;\\d{0,4})*)?[\\dA-PRZcf-ntqry=><~]" + ")" +
            ")"
    )

    fun toHumanReadableText(escapeSequence: String): String
    {
        return escapeSequence.replace("\u001b", "ESC")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\u0007", "BEL")
//                        .replace(" ", "<S>")
            .replace("\b", "\\b");
    }

}