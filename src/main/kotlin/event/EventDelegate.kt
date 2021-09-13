package com.github.asforest.mshell.event

class EventDelegate<Context, Callback>: Iterable<Callback> where Callback : Function<Unit>
{
    val listeners = mutableListOf<Callback>()

    fun on(cb: Callback)
    {
        this += cb
    }

    fun invoke(context: Context, calling: (arg: Callback) -> Unit)
    {
        listeners.forEach { calling(it) }
    }

    suspend fun invokeSuspend(context: Context, calling: suspend (arg: Callback) -> Unit)
    {
        listeners.forEach { calling(it) }
    }

    operator fun plusAssign(cb: Callback)
    {
        listeners += cb
    }

    suspend operator fun invoke(context: Context, calling: suspend (arg: Callback) -> Unit)
    {
        invokeSuspend(context, calling)
    }

    override fun iterator(): MutableIterator<Callback>
    {
        return listeners.iterator()
    }
}

