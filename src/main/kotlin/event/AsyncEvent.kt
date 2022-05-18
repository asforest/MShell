package com.github.asforest.mshell.event

import com.github.asforest.mshell.exception.system.EventLoopReachCountLimitException
import com.github.asforest.mshell.exception.system.ListenerAlreadyAddedException
import com.github.asforest.mshell.exception.system.ListenerNotFoundException

class AsyncEvent : Iterable<AsyncEvent.Listener>
{
    val listeners = mutableListOf<Listener>()

    fun always(cb: suspend () -> Unit)
    {
        addListener(cb, ListenerType.ALWAYS)
    }

    fun once(cb: suspend () -> Unit)
    {
        addListener(cb, ListenerType.ONCE)
    }

    private fun addListener(callback: suspend () -> Unit, type: ListenerType)
    {
        this += Listener(callback, type)
    }

    operator fun plusAssign(listener: Listener)
    {
        if(listener in this)
            throw  ListenerAlreadyAddedException()

        listeners += listener
    }

    operator fun minusAssign(listener: Listener)
    {
        if(listener !in this)
            throw ListenerNotFoundException()

        listeners -= listener
    }

    suspend fun invoke()
    {
        val vaildListeners = listeners.filter { it.type != ListenerType.NEVER }

        for (listener in vaildListeners)
        {
            listener.callback()

            if(listener.type == ListenerType.ONCE)
                listener.type = ListenerType.NEVER
        }

        listeners.removeIf { it.type == ListenerType.NEVER }
    }

    operator fun contains(listener: Listener): Boolean
    {
        return listener in listeners
    }

    override fun iterator(): MutableIterator<Listener>
    {
        return listeners.iterator()
    }

    class Listener(
        var callback: suspend () -> Unit,
        var type: ListenerType,
    )

    enum class ListenerType
    {
        NEVER, ONCE, ALWAYS
    }
}

