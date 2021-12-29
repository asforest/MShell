package com.github.asforest.mshell.event

class Event<TContext, TCallback>(val context: TContext)
    : Iterable<Event.Listener<TCallback>> where TCallback : Function<Unit>
{
    val listeners = mutableListOf<Listener<TCallback>>()

    fun always(cb: TCallback)
    {
        this += Listener(cb, ListenerType.ALWAYS)
    }

    fun once(cb: TCallback)
    {
        this += Listener(cb, ListenerType.ONCE)
    }

    fun invokeSuspend(calling: (arg: TCallback) -> Unit)
    {
        listeners.forEach { calling(it.callback) }
        listeners.removeIf { it.type == ListenerType.ONCE }
    }

    operator fun plusAssign(listener: Listener<TCallback>)
    {
        listeners += listener
    }

    operator fun minusAssign(listener: Listener<TCallback>)
    {
        listeners -= listener
    }

    operator fun invoke(calling: (arg: TCallback) -> Unit)
    {
        invokeSuspend(calling)
    }

    override fun iterator(): MutableIterator<Listener<TCallback>>
    {
        return listeners.iterator()
    }

    data class Listener<Callback>(
        val callback: Callback,
        val type: ListenerType
    )

    enum class ListenerType
    {
        NEVER, ONCE, ALWAYS
    }
}

