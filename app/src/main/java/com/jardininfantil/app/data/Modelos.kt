package com.jardininfantil.app.data

import org.json.JSONObject

enum class Status(val label: String) {
    PRESENTE("Presente"), AUSENTE("Ausente"), ENTREGADO("Entregado")
}

data class Guardian(
    val name: String,
    val relation: String,
    val idMasked: String,
    val authorized: Boolean = true,
    val document: String = ""
) { val initials: String get() = name.initials() }

data class Child(
    val id: String,
    val name: String,
    val room: String,
    val age: Int,
    val birth: String,
    val document: String,
    val complement: String,
    val guardians: List<Guardian>,
    var status: Status = Status.AUSENTE,
    var attendanceId: String? = null
) {
    val initials: String get() = name.initials()
    val firstName: String get() = name.substringBefore(" ")
}

data class Supply(val name: String, var units: Int, val lowAt: Int = 5)

data class SessionUser(val id: String, val name: String, val email: String, val role: String) {
    val isAdministrator: Boolean get() = role == "administradora"
}

object Jardin {
    val rooms = mutableListOf<String>()
    val children = mutableListOf<Child>()
    val supplies = mutableListOf<Supply>()
    val complementDelivered = mutableSetOf<String>()
    var reportSummary: JSONObject = JSONObject()
    fun arrived() = children.count { it.status != Status.AUSENTE }
    fun pendingPickup() = children.filter { it.status == Status.PRESENTE }
    fun complementPending() = children.filter { it.id !in complementDelivered }
    fun lowSupplies() = supplies.filter { it.units <= it.lowAt }
}

private fun String.initials(): String = split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
