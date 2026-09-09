package com.example.fishing.data

object EmailValidator {

    private val emailPattern = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValid(email: String): Boolean {
        return emailPattern.matches(email.trim())
    }
}