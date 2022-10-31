package com.github.asforest.mshell.session

import kotlin.math.abs

/**
 * 代表一个历史消息保留类
 */
class HistoryMessageReserver(var capacityInBytes: Int)
{
    val bytesUsed:Int get() {
        var bytesUsed = 0
        for (message in buffer)
            bytesUsed += message.message.length
        return bytesUsed
    }

    val buffer = ArrayList<HistoryMessage>(capacityInBytes)

    fun append(message: String)
    {
        if(capacityInBytes == 0)
            return

        val capacity = abs(capacityInBytes)

        // 单次消息超过大小限制了
        buffer += if(message.length > capacity) {
            val overflowAllowed = capacityInBytes < 0 // 要么溢出，要么截断
            buffer.clear()

            val msg = if(overflowAllowed) message else message.substring(message.length - capacity)
            HistoryMessage(System.currentTimeMillis(), msg)
        } else {
            while (bytesUsed + message.length > capacity)
                buffer.removeFirst()

            HistoryMessage(System.currentTimeMillis(), message)
        }
    }

    fun hasMessage(since: Long): Boolean
    {
        return buffer.any { it.time > since }
    }

    fun getAllMessage(since: Long): List<String>
    {
        return buffer.filter { it.time > since }.map { it.message }
    }

    fun getAllLines(since: Long): List<HistoryMessage>
    {
        return buffer.filter { it.time > since }
    }

    override fun toString(): String
    {
        val sb = StringBuffer()

        for (message in buffer)
            sb.append(message)

        return sb.toString()
    }

    data class HistoryMessage(
        val time: Long,
        val message: String
    )
}