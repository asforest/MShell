package com.github.asforest.mshell.stream

import com.github.asforest.mshell.event.Event
import com.github.asforest.mshell.model.Preset
import com.github.asforest.mshell.util.AnsiEscapeUtil
import kotlinx.coroutines.*
import java.io.Writer
import java.util.concurrent.LinkedBlockingDeque
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@DelicateCoroutinesApi
class BatchingWriter(
    val preset: Preset,
    var onBatchedOutput: suspend (text: String)->Unit,
): Writer() {
    private val buffer = LinkedBlockingDeque<String>(1*1024*1024)
    private val coBatching: Job
    private var onNewDataArrive = Event()
    private var exitFlag = false

    init {
        coBatching = GlobalScope.launch {
            val truncation = preset.truncationThreshold
            val batchingTimeout = preset.batchingInteval.toLong()

            while(!exitFlag)
            {
                if(buffer.isEmpty())
                    waitUntilDataArrive()

                val sbuf = SendingBuffer()

                while (buffer.isNotEmpty())
                {
                    while (true)
                    {
                        val msg = buffer.poll()

                        if (msg == null)
                        {
                            // 合并时间较近的两条消息
                            val elapse = waitUntilDataArrive(batchingTimeout.toInt())

//                            println("$batchingTimeout : $elapse")

                            if (elapse < batchingTimeout)
                                continue
                            else
                                break
                        }

                        // 空字符串表示显式隔断
                        if (msg.isEmpty())
                        {
//                            println("truncate !")
                            break
                        }

                        // 如果总长度超出truncation，则分多次发送
                        if (sbuf.length + msg.length > truncation)
                        {
//                            val oo = msg.trim().replace(Regex("(\\n|\\r|\\r\\n)"), "/n")
//                            val bb = sbuf.toString().trim().replace(Regex("(\\n|\\r|\\r\\n)"), "/n")

                            if (msg.length <= truncation)
                            {
//                                println("overflow: ${sbuf.length + msg.length}(${msg.length}) > $truncation |$oo|$bb]")

                                // 将消息重新插回队列里，等待下个来循环处理
                                buffer.addFirst(msg)
                                break
                            } else {
//                                println("overflow!!!!: ${sbuf.length + msg.length}(${msg.length}) > $truncation |$oo|$bb]")

                                // 清空所有现有内容
                                callOnBatchedOutput(sbuf.take())

                                // 强行插入发送缓冲区
                                sbuf += msg
                                break
                            }
                        }

//                        println("normal handle: sbuf: ${sbuf.length} + msg:${msg.length}")

                        // 正常处理
                        sbuf += msg
                    }

                    callOnBatchedOutput(sbuf.take())
                }
            }
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

        onNewDataArrive()
    }

    override fun flush()
    {
        buffer += "" // 空字符串表示显式隔断

        onNewDataArrive()
    }

    override fun write(cbuf: CharArray, off: Int, len: Int)
    {
        try {
            val str = String(cbuf, off, len).trim().replace(AnsiEscapeUtil.pattern, "")
            if (str.isNotEmpty())
                buffer += str
        } catch (e: IllegalStateException) {
            throw BatchingBwriterBufferOverflowException("The buffer of BatchingWriter overflows")
        }

        onNewDataArrive()
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
     * 挂起协程，直到有数据到来
     * @return 等待了多长时间，单位毫秒
     */
    private suspend fun waitUntilDataArrive(timeoutMs: Int = 0): Long
    {
        val startTime = System.currentTimeMillis()

        var continuation: Continuation<Unit>? = null

        var listener: Event.Listener? = null
        var job: Job? = null

        suspendCoroutine<Unit> {
            // 更新Continuation对象
            val isReIn = continuation != null
            continuation = it

            if (isReIn)
                return@suspendCoroutine

            // 条件恢复协程
            listener = onNewDataArrive.once {
                job?.cancel()
                continuation!!.resume(Unit)
            }

            // 超时恢复协程
            if (timeoutMs != 0)
            {
                job = GlobalScope.launch {
                    delay(timeoutMs.toLong())
                    if (listener != null)
                        onNewDataArrive -= listener!!
                    continuation!!.resume(Unit)
                }
            }
        }

        return System.currentTimeMillis() - startTime
    }

    private suspend fun callOnBatchedOutput(str: String)
    {
        if (str.isNotEmpty())
        {
            onBatchedOutput(str)
//            println("send(${str.length}): ${str.replace(Regex("(\\n|\\r|\\r\\n)"), "/n")}")
        }
    }

    private class SendingBuffer
    {
        var sb = StringBuffer()

        val length get() = sb.length

        fun clear() { sb = StringBuffer() }

        fun take(): String = toString().also { clear() }

        operator fun plusAssign(str: String) { sb.append(str) }

        override fun toString(): String = sb.toString()
    }

    class BatchingBwriterBufferOverflowException(message: String) : Exception(message)
}