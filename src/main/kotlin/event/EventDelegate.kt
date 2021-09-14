package com.github.asforest.mshell.event

class EventDelegate<Context, Callback>(val context: Context)
    : Iterable<Callback> where Callback : Function<Unit>
{
    val listeners = mutableListOf<Callback>()

    fun on(cb: Callback)
    {
        this += cb
    }

    fun invoke( calling: (arg: Callback) -> Unit)
    {
        listeners.forEach { calling(it) }
    }

    suspend fun invokeSuspend(calling: suspend (arg: Callback) -> Unit)
    {
        listeners.forEach { calling(it) }
    }

    operator fun plusAssign(cb: Callback)
    {
        listeners += cb
    }

    suspend operator fun invoke(calling: suspend (arg: Callback) -> Unit)
    {
        invokeSuspend(calling)
    }

    override fun iterator(): MutableIterator<Callback>
    {
        return listeners.iterator()
    }
}

