package com.example.fishing.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** AUD-03: the password update request must use GoTrue's field names. */
class PasswordUpdateBodyTest {

    @Test
    fun `body sends current password as current_password`() {
        val body = passwordUpdateBody("new-password", "old-password")
        val fields = Json.parseToJsonElement(body).jsonObject

        assertEquals("new-password", fields["password"]?.jsonPrimitive?.content)
        assertEquals("old-password", fields["current_password"]?.jsonPrimitive?.content)
        assertNull(fields["currentPassword"])
    }
}
