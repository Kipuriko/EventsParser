package ru.purebytestudio.eventparser.data.platform

import android.content.Context
import ru.purebytestudio.eventparser.platform.ResourceProvider

class AndroidResourceProvider(private val context: Context) : ResourceProvider {
    override fun getString(id: Int): String {
        return context.getString(id)
    }

    override fun getString(
        id: Int,
        vararg args: Any
    ): String {
        return context.getString(
            id,
            *args
        )
    }
}