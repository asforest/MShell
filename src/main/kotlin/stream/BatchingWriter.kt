package com.github.asforest.mshell.stream

import com.github.asforest.mshell.configuration.MainConfig
import com.github.asforest.mshell.event.Event
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.Writer
import java.lang.Exception
import java.util.concurrent.LinkedBlockingQueue
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BatchingWriter(
    var onBatchedOutput: suspend (text: String)->Unit,
//    val onClose: (()->Unit)?
): Writer() {
    private val buffer = LinkedBlockingQueue<MessageBuffered>(1*1024*1024)

    private val coBatching: Job
    private var onNewDataArrive = Event<Unit, () -> Unit>(Unit)

    init {
        coBatching = GlobalScope.launch {
            val truncation = MainConfig.stdoutputTruncationThreshold
            val batchingTimeout = MainConfig.stdoutputBatchingTimeoutInMs.toLong()

            while(true)
            {
                // 如果队里里没有消息，则将协程暂时挂起，进行等待
                if(buffer.isEmpty())
                    suspendCoroutine<Unit> { onNewDataArrive.once { it.resume(Unit) } }

                // 进行消息合批
                val buffered = StringBuffer()
                while (buffered.length < truncation && buffer.isNotEmpty())
                {
                    val msg = buffer.poll() ?: break

                    if(!msg.isTruncation)
                        buffered.append(msg.content)
                    else
                        break

                    // 处理队列中最后一条消息后，才开始等待
                    if(buffer.isEmpty())
                        delay(batchingTimeout)
                }

                // 发送合批后的消息
                val str = buffered.toString()
                if(str.isNotEmpty())
                    onBatchedOutput(str)
            }
        }
    }

    /**
     * 等待合批协程结束
     */
    suspend fun wait()
    {
        coBatching.join()
    }

    override fun close()
    {
        if(coBatching.isActive)
            coBatching.cancel()

//        if(onClose != null)
//            onClose!!()
    }

    override fun flush()
    {
        buffer += MessageBuffered()
        onNewDataArrive { it() }
    }

    override fun write(cbuf: CharArray, off: Int, len: Int)
    {
        try {
            buffer += MessageBuffered(String(cbuf, off, len))
        } catch (e: IllegalStateException) {
            throw BatchingBwriterBufferOverflowException("The buffer of BatchingWriter overflows")
        }
        onNewDataArrive { it() }
    }

    operator fun plusAssign(str: String)
    {
        this.append(str)
    }

    class MessageBuffered(message: CharSequence? =null)
    {
        val content: CharSequence
        val isTruncation: Boolean

        init {
            content = message ?: ""
            isTruncation = content.isEmpty()
        }
    }

    class BatchingBwriterBufferOverflowException(message: String) : Exception(message)
}