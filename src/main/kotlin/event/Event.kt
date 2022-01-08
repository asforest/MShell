package com.github.asforest.mshell.event

import com.github.asforest.mshell.exception.EventLoopReachCountLimitException
import com.github.asforest.mshell.exception.ListenerAlreadyAddedException
import com.github.asforest.mshell.exception.ListenerNotFoundException
import kotlin.reflect.jvm.reflect

class Event<TContext, TCallback>(val context: TContext)
    : Iterable<Event.Listener<TCallback>> where TCallback : Function<Unit>
{
    val listeners = mutableListOf<Listener<TCallback>>()

    fun always(label: String? = null, cb: TCallback)
    {
        addListener(cb, label, ListenerType.ALWAYS)
    }

    fun once(label: String? = null, cb: TCallback)
    {
        addListener(cb, label, ListenerType.ONCE)
    }

    private fun addListener(callback: TCallback, label: String? = null, type: ListenerType)
    {
        val _label = label ?: callback.hashCode().toString()
        this += Listener(callback, _label, type)
    }

    operator fun plusAssign(listener: Listener<TCallback>)
    {
        if(listener in this)
            throw  ListenerAlreadyAddedException(listener.label)

        listeners += listener
    }

    operator fun minusAssign(listener: Listener<TCallback>)
    {
        if(listener !in this)
            throw ListenerNotFoundException(listener.label)

        listeners -= listener
    }

    operator fun minusAssign(label: String)
    {
        if(label !in this)
            throw ListenerNotFoundException(label)

        listeners.removeIf { it.label == label }
    }

    operator fun invoke(calling: (arg: TCallback) -> Unit)
    {
        var limit = 100000

        while (true)
        {
            val listenersToTrigger = listeners.filter { it.type != ListenerType.NEVER }

            for (listener in listenersToTrigger)
            {
                calling(listener.callback)

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

    operator fun contains(listener: Listener<TCallback>): Boolean
    {
        return listeners.any { it.label == listener.label }
    }

    operator fun contains(label: String): Boolean
    {
        return listeners.any { it.label == label }
    }

    operator fun get(label: String): Listener<TCallback>?
    {
        return listeners.firstOrNull { it.label == label }
    }

    override fun iterator(): MutableIterator<Listener<TCallback>>
    {
        return listeners.iterator()
    }

    data class Listener<Callback>(
        val callback: Callback,
        val label: String,
        var type: ListenerType,
    )

    enum class ListenerType
    {
        NEVER, ONCE, ALWAYS
    }
}

