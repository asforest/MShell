package com.github.asforest.mshell.session

import kotlin.math.abs

class SessionLastwillMessage(
    var capacityInBytes: Int
) {
    val bytesUsed:Int get() {
        var bytesUsed = 0
        for (message in lastwillBuffer)
            bytesUsed += message.message.length
        return bytesUsed
    }

    val lastwillBuffer = ArrayList<LastwillMessage>(capacityInBytes)

    fun append(message: String)
    {
        if(capacityInBytes == 0)
            return

        val capacity = abs(capacityInBytes)

        // 单次消息超过大小限制了
        lastwillBuffer += if(message.length > capacity) {
            val overflowAllowed = capacityInBytes < 0 // 要么溢出，要么截断
            lastwillBuffer.clear()

            val msg = if(overflowAllowed) message else message.substring(message.length - capacity)
            LastwillMessage(System.currentTimeMillis(), msg)
        } else {
            while (bytesUsed + message.length > capacity)
                lastwillBuffer.removeFirst()

            LastwillMessage(System.currentTimeMillis(), message)
        }
    }

    fun hasMessage(since: Long): Boolean
    {
        return lastwillBuffer.any { it.time > since }
    }

    fun getAllMessage(since: Long): List<String>
    {
        return lastwillBuffer.filter { it.time > since }.map { it.message }
    }

    fun getAllLines(since: Long): List<LastwillMessage>
    {
        return lastwillBuffer.filter { it.time > since }
    }

    override fun toString(): String
    {
        val sb = StringBuffer()

        for (message in lastwillBuffer)
            sb.append(message)

        return sb.toString()
    }

    data class LastwillMessage(
        val time: Long,
        val message: String
    )
}