package com.github.asforest.mshell.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.mamoe.mirai.data.UserProfile
import java.util.*

/**
 * 代表预设开启json-mode之后发往会话stdin的json消息
 */
data class JsonMessage(
    val bot: String,
    val group: String,
    val relation: String,
    val message: String,

    val nick: String,
    val id: String,
    val remark: String,

    // group related
    val join: Int,
    val speak: Int,
    val namecard: String,
    val title: String,

    // profile
    val email: String,
    val age: Int,
    val level: Int,
    val sex: UserProfile.Sex,
) {
    override fun toString(): String
    {
        return JsonObject(mapOf(
            "bot" to JsonPrimitive(bot),
            "group" to JsonPrimitive(group),
            "relation" to JsonPrimitive(relation),
            "message" to JsonPrimitive(message),
            "nick" to JsonPrimitive(nick),
            "id" to JsonPrimitive(id),
            "remark" to JsonPrimitive(remark),

            "join" to JsonPrimitive(join),
            "speak" to JsonPrimitive(speak),
            "namecard" to JsonPrimitive(namecard),
            "title" to JsonPrimitive(title),

            "email" to JsonPrimitive(email),
            "age" to JsonPrimitive(age),
            "level" to JsonPrimitive(level),
            "sex" to JsonPrimitive(sex.toString().lowercase(Locale.getDefault())),
        )).toString()
    }
}