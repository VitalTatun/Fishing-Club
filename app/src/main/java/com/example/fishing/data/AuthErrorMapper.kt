package com.example.fishing.data

object AuthErrorMapper {

    fun toUserFriendlyMessage(e: Throwable): String {
        return ErrorMapper.toUserFriendlyMessage(e)
    }
}
