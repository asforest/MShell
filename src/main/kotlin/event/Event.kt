package com.github.asforest.mshell.event

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
        val validListeners = listeners.filter { it.type != ListenerType.NEVER }

        for (listener in validListeners)
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
        var callback: () -> Unit,
        var type: ListenerType,
    )

    enum class ListenerType
    {
        NEVER, ONCE, ALWAYS
    }
}

