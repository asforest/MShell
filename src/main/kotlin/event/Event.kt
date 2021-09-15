package com.github.asforest.mshell.event

class Event<Context, Callback>(val context: Context)
    : Iterable<Event.Listener<Callback>> where Callback : Function<Unit>
{
    val listeners = mutableListOf<Listener<Callback>>()

    fun always(cb: Callback)
    {
        this += Listener(cb, ListenerType.ALWAYS)
    }

    fun once(cb: Callback)
    {
        this += Listener(cb, ListenerType.ONCE)
    }

    fun invokeSuspend(calling: (arg: Callback) -> Unit)
    {
        listeners.forEach { calling(it.callback) }
        listeners.removeIf { it.type == ListenerType.ONCE }
    }

    operator fun plusAssign(listener: Listener<Callback>)
    {
        listeners += listener
    }

    operator fun invoke(calling: (arg: Callback) -> Unit)
    {
        invokeSuspend(calling)
    }

    override fun iterator(): MutableIterator<Listener<Callback>>
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

