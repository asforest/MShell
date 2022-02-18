package com.github.asforest.mshell.stream

import com.github.asforest.mshell.event.Event
import com.github.asforest.mshell.exception.system.ListenerNotFoundException
import com.github.asforest.mshell.model.Preset
import com.github.asforest.mshell.util.AnsiEscapeUtil
import com.github.asforest.mshell.util.MShellUtils
import com.github.asforest.mshell.util.StringUtils.splitAndReplace
import kotlinx.coroutines.*
import java.io.Writer
import java.lang.RuntimeException
import java.util.concurrent.LinkedBlockingDeque
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * BatchingWriter 用来将时间相近的两条消息合并成一条发送
 */
class BatchingWriter(
    val preset: Preset,
    var onBatchedOutput: suspend (text: String)->Unit,
): Writer() {
    private val buffer = LinkedBlockingDeque<MessageObject>(1*1024*1024)
    private val coBatching: Job
    private var onNewDataArrive = Event()
    private var exitFlag = false

    init {
        coBatching = GlobalScope.launch {
            while(!exitFlag)
            {
                if(!buffer.needHandle)
                    waitUntilDataArrive()

                processBufferContents()
            }
        }
    }

    private suspend fun processBufferContents()
    {
        val truncation = preset.truncationThreshold
        val batchingTimeout = preset.batchingInteval.toLong()

        val sbuf = SendingBuffer()

        while (buffer.needHandle)
        {
            while (true)
            {
                if (!buffer.needHandle)
                {
                    // 合并时间较近的两条消息
                    val elapse = waitUntilDataArrive(batchingTimeout.toInt())
                    if (elapse == null) break else continue // break 会导致函数返回
                }

                val msgEntry = buffer.pop()

                // 强制打断合并
                if (msgEntry.isTruncation)
                    break

                val msg = msgEntry.content!!

                // 如果总长度超出truncation，则分多次发送
                if (sbuf.length + msg.length > truncation)
                {
                    if (msg.length <= truncation)
                    {
//                        println("overflow(small): ${sbuf.length + msg.length}(${msg.length}) > $truncation |${msg.trim().escaped}|${sbuf.toString().escaped}]")

                        // 将消息重新插回队列里，等待下个循环来处理
                        buffer.addFirst(msgEntry)
                        break
                    } else {
//                        println("overflow(big): ${sbuf.length + msg.length}(${msg.length}) > $truncation |${msg.trim().escaped}|${sbuf.toString().escaped}]")

                        // 发送并清空所有现有内容
                        callOnBatchedOutput(sbuf.take())

                        // 强行插入发送缓冲区
                        sbuf += msg
                        break
                    }
                }

//                println("normal handle: sbuf: ${sbuf.length} + msg:${msg.length}")
                // 正常处理
                sbuf += msg
            }

            callOnBatchedOutput(sbuf.take())
        }
    }

    /**
     * 等待合并协程结束
     */
    suspend fun wait()
    {
        coBatching.join()
    }

    override fun close()
    {
        exitFlag = true

        flush()
    }

    override fun flush()
    {
        buffer += MessageObject(null)

        onNewDataArrive()

//        println("truncate")
    }

    override fun write(cbuf: CharArray, off: Int, len: Int)
    {
        try {
            val str = String(cbuf, off, len).replace(AnsiEscapeUtil.pattern, "")

            if (str.isNotEmpty())
            {
                val mos = str.splitAndReplace(Regex("(\\r\\n|\\n|\\r)"), "\n").map { MessageObject(it) }

                buffer.addAll(mos)

                if (mos.any { it.doNewlineSymbolPresent })
                    onNewDataArrive()
            }
        } catch (e: IllegalStateException) {
            throw BatchingBwriterBufferOverflowException("The buffer of BatchingWriter overflows")
        }
    }

    fun shutdown()
    {
        if(coBatching.isActive)
            coBatching.cancel()
    }

    operator fun plusAssign(str: String)
    {
        this.append(str)
    }

    /**
     * 挂起协程，直到有数据到来，或者等待超过指定时间
     * @param timeoutMs 等待超时，为0则永不超时
     * @return 等待了多长时间。如果是因为超时，返回null，否则返回实际等待时间，单位毫秒
     */
    private suspend fun waitUntilDataArrive(timeoutMs: Int = 0): Long?
    {
        val startTime = System.currentTimeMillis()

        var continuation: Continuation<Long?>? = null

        var listener: Event.Listener? = null
        var job: Job? = null

        return suspendCoroutine<Long?> {
            // 更新Continuation对象
            val isReIn = continuation != null
            continuation = it

            if (isReIn)
                return@suspendCoroutine

            // 条件恢复协程
            listener = onNewDataArrive.once {
                job?.cancel()
                continuation!!.resume(System.currentTimeMillis() - startTime)
            }

            // 超时恢复协程
            if (timeoutMs != 0)
            {
                job = GlobalScope.launch {
                    delay(timeoutMs.toLong())
                    
                    if (listener in onNewDataArrive)
                    {
                        onNewDataArrive -= listener!!
                        continuation!!.resume(null)
                    }
                }
            }
        }
    }

    /**
     * 调用onBatchedOutput回调函数
     */
    private suspend fun callOnBatchedOutput(str: String)
    {
        if (str.isNotEmpty())
        {
            onBatchedOutput(str)
//            println("send(${str.length}): ${str.replace(Regex("(\\n|\\r|\\r\\n)"), "/n")}")
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private val String.escaped get() = this.replace(Regex("(\\r\\n|\\n|\\r)"), "/n")

    @Suppress("NOTHING_TO_INLINE")
    private val LinkedBlockingDeque<MessageObject>.needHandle get() = this.any { it.isTruncation || it.doNewlineSymbolPresent }

    private data class MessageObject(val content: String?)
    {
        val isTruncation = content == null

        val doNewlineSymbolPresent = !isTruncation && (content!!.contains("\n") || content!!.contains("\r"))

        override fun toString(): String = content.toString()
    }

    /**
     * 消息发送缓冲区，是一个StringBuffer类的封装
     */
    private class SendingBuffer
    {
        var sb = StringBuffer()

        val length get() = sb.length

        fun clear() { sb = StringBuffer() }

        fun take(): String = toString().also { clear() }

        operator fun plusAssign(str: String) { sb.append(str) }

        override fun toString(): String = sb.toString().trim()
    }

    class BatchingBwriterBufferOverflowException(message: String) : Exception(message)
}