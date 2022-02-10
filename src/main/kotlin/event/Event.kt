package com.github.asforest.mshell.event

import com.github.asforest.mshell.exception.system.EventLoopReachCountLimitException
import com.github.asforest.mshell.exception.system.ListenerAlreadyAddedException
import com.github.asforest.mshell.exception.system.ListenerNotFoundException

class Event : Iterable<Event.Listener>
{
    val listeners = mutableListOf<Listener>()

    fun always(cb: () -> Unit): Listener
    {
        return addListener(cb, ListenerType.ALWAYS)
    }

    fun once(cb: () -> Unit): Listener
    {
        return addListener(cb, ListenerType.ONCE)
    }

    private fun addListener(callback: () -> Unit, type: ListenerType): Listener
    {
        val listener = Listener(callback, type)
        this += listener
        return listener
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

    operator fun invoke()
    {
        var limit = 100000

        while (true)
        {
            val listenersToTrigger = listeners.filter { it.type != ListenerType.NEVER }

            for (listener in listenersToTrigger)
            {
                listener.callback()

                if(listener.type == ListenerType.ONCE)
                    listener.type = ListenerType.NEVER
            }

            if(listenersToTrigger.isEmpty())
                break

            if(limit-- <= 0)
                throw EventLoopReachCountLimitException(100000)
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

    data class Listener(
        var callback: () -> Unit,
        var type: ListenerType,
    )

    enum class ListenerType
    {
        NEVER, ONCE, ALWAYS
    }
}

