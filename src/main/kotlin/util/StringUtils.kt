package com.github.asforest.mshell.util

object StringUtils
{
    /**
     * 拆分字符串，分隔符会被替换成fill
     */
    fun String.splitAndReplace(regex: Regex, fill: String): List<String>
    {
//        val reg = Regex("(\\r\\n|\\n|\\r)")

        val occurrences = regex.findAll(this).toList()
        val ranges = occurrences.map { IntRange(it.range.first, it.range.last + 1) }

//        val ptab = this.replace("\n", "/n").replace("\r", "/r")
//        println("Raw: <$ptab> Len: ${this.length} Ranges: $ranges")

        val result = mutableListOf<String>()

        // 首个字符串段落
        result += this.substring(0, if (ranges.isNotEmpty()) ranges.first().first else this.length)

        for ((index, range) in ranges.withIndex())
        {
            result += fill

            val reachEnd = range.last == this.length
            if (!reachEnd)
            {
                val next = if (index != ranges.size - 1) ranges[index + 1] else null
//                val r1 = range.last
//                val r2 = next?.first ?: this.length
                val q = this.substring(range.last, next?.first ?: this.length)
                result += q
//                println("<$q> $r1, $r2")
            }
        }

//        println("> "+result.toString().replace(reg, "/n"))

        return result
    }
}