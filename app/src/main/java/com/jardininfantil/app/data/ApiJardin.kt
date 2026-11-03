package com.jardininfantil.app.data

import com.jardininfantil.app.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.Period

/** Cliente HTTP de la API Node/MongoDB configurada para este entorno. */
object ApiJardin {
    val baseUrl: String = BuildConfig.API_BASE_URL.trimEnd('/')
    @Volatile var bearerToken: String? = null

    fun request(method: String, path: String, body: JSONObject? = null): Any {
        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            bearerToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching {
                    val errorJson = JSONObject(response)
                    errorJson.optString("message").ifBlank { errorJson.optString("error") }
                }.getOrNull()
                throw ApiException(status, message?.takeIf { it.isNotBlank() } ?: "Error HTTP $status")
            }
            return when {
                response.trimStart().startsWith("[") -> JSONArray(response)
                response.trimStart().startsWith("{") -> JSONObject(response)
                else -> JSONObject()
            }
        } finally {
            connection.disconnect()
        }
    }
}

class ApiException(val statusCode: Int, override val message: String) : IOException(message)

data class HistoryData(val attendance: List<JSONObject>, val deliveries: List<JSONObject>)

/** Convierte y conserva las respuestas de la API que utiliza la interfaz. */
object JardinRepository {
    fun login(email: String, password: String): JSONObject = ApiJardin.request(
        "POST", "/auth/login", JSONObject().put("email", email).put("password", password)
    ) as JSONObject

    fun me(): JSONObject = ApiJardin.request("GET", "/auth/me") as JSONObject

    fun reload(date: String = LocalDate.now().toString(), reportMonth: String? = null) {
        val reportPeriod = reportMonth?.let { "month=$it" } ?: "date=$date"
        val childrenJson = ApiJardin.request("GET", "/children") as JSONArray
        val attendanceJson = ApiJardin.request("GET", "/attendance?date=$date") as JSONArray
        val inventoryJson = ApiJardin.request("GET", "/inventory") as JSONArray
        val deliveriesJson = ApiJardin.request("GET", "/aid-deliveries?date=$date") as JSONArray
        val report = try { ApiJardin.request("GET", "/reports/summary?$reportPeriod") as JSONObject }
            catch (error: ApiException) { if (error.statusCode == 403) JSONObject() else throw error }

        val attendanceByChild = (0 until attendanceJson.length()).map { attendanceJson.getJSONObject(it) }
            .associateBy { relationId(it.opt("child")) }
        val children = (0 until childrenJson.length()).map { index ->
            val json = childrenJson.getJSONObject(index)
            val childId = json.optString("_id")
            val attendance = attendanceByChild[childId]
            val guardiansJson = json.optJSONArray("authorizedGuardians") ?: JSONArray()
            val guardians = (0 until guardiansJson.length()).map { guardianIndex ->
                val guardian = guardiansJson.getJSONObject(guardianIndex)
                val document = guardian.optString("document")
                Guardian(
                    name = guardian.optString("name", "Acudiente"),
                    relation = guardian.optString("relation", "Acudiente autorizado"),
                    idMasked = maskDocument(document),
                    authorized = true,
                    document = document
                )
            }
            val status = when (attendance?.optString("status")) {
                "presente" -> Status.PRESENTE
                "entregado" -> Status.ENTREGADO
                else -> Status.AUSENTE
            }
            val birthDate = json.optString("birthDate")
            val age = runCatching { Period.between(LocalDate.parse(birthDate), LocalDate.now()).years }.getOrDefault(0)
            Child(
                id = childId,
                name = json.optString("name", "Sin nombre"),
                room = json.optString("classroom", "Salón sin asignar"),
                age = age,
                birth = birthDate.takeIf { it.isNotBlank() && it != "null" } ?: "No reportada",
                document = json.optString("document").ifBlank { "No reportado" },
                complement = json.optString("complement", "Según inventario"),
                guardians = guardians,
                status = status,
                attendanceId = attendance?.optString("_id")?.takeIf { it.isNotBlank() }
            )
        }

        Jardin.children.clear()
        Jardin.children.addAll(children)
        Jardin.rooms.clear()
        Jardin.rooms.addAll(children.map { it.room }.distinct())
        Jardin.supplies.clear()
        (0 until inventoryJson.length()).forEach { index ->
            val item = inventoryJson.getJSONObject(index)
            Jardin.supplies += Supply(item.optString("kitType"), item.optInt("stock"))
        }
        Jardin.complementDelivered.clear()
        (0 until deliveriesJson.length()).forEach { index -> Jardin.complementDelivered += relationId(deliveriesJson.getJSONObject(index).opt("child")) }
        Jardin.reportSummary = report
    }

    fun inventoryMovements(month: String): JSONArray = ApiJardin.request("GET", "/inventory/movements?month=$month") as JSONArray

    fun child(child: Child): Child {
        val json = ApiJardin.request("GET", "/children/${child.id}") as JSONObject
        val guardiansJson = json.optJSONArray("authorizedGuardians") ?: JSONArray()
        val guardians = (0 until guardiansJson.length()).map { index ->
            val guardian = guardiansJson.getJSONObject(index)
            val document = guardian.optString("document")
            Guardian(
                name = guardian.optString("name", "Acudiente"),
                relation = guardian.optString("relation", "Acudiente"),
                idMasked = maskDocument(document),
                document = document
            )
        }
        val birthDate = json.optString("birthDate")
        val age = runCatching { Period.between(LocalDate.parse(birthDate), LocalDate.now()).years }.getOrDefault(0)
        return child.copy(
            name = json.optString("name", child.name),
            room = json.optString("classroom", child.room),
            age = age,
            birth = birthDate.takeIf { it.isNotBlank() && it != "null" } ?: "No reportada",
            document = json.optString("document").ifBlank { "No reportado" },
            complement = json.optString("complement", "Según inventario"),
            guardians = guardians
        )
    }

    fun history(date: String): HistoryData {
        val attendance = ApiJardin.request("GET", "/attendance?date=$date") as JSONArray
        val deliveries = ApiJardin.request("GET", "/aid-deliveries?date=$date") as JSONArray
        return HistoryData(attendance.toObjects(), deliveries.toObjects())
    }

    fun createChild(name: String, classroom: String, guardians: List<Guardian>, birthDate: String = "", document: String = "", complement: String = "Según inventario"): JSONObject =
        ApiJardin.request("POST", "/children", childBody(name, classroom, guardians, birthDate, document, complement)) as JSONObject

    fun updateChild(id: String, name: String, classroom: String, guardians: List<Guardian>, birthDate: String = "", document: String = "", complement: String = "Según inventario"): JSONObject =
        ApiJardin.request("PUT", "/children/$id", childBody(name, classroom, guardians, birthDate, document, complement)) as JSONObject

    fun retireChild(id: String): JSONObject = ApiJardin.request("DELETE", "/children/$id") as JSONObject

    fun checkIn(childId: String): JSONObject = ApiJardin.request(
        "POST", "/attendance", JSONObject().put("childId", childId).put("date", LocalDate.now().toString())
    ) as JSONObject

    fun checkOut(attendanceId: String, guardian: Guardian): JSONObject = ApiJardin.request(
        "PATCH", "/attendance/$attendanceId/checkout",
        JSONObject().put("guardianName", guardian.name).put("guardianDocument", guardian.document)
    ) as JSONObject

    fun deliverAid(childId: String, kitType: String, quantity: Int): JSONObject = ApiJardin.request(
        "POST", "/aid-deliveries",
        JSONObject().put("childId", childId).put("kitType", kitType).put("quantity", quantity).put("date", LocalDate.now().toString())
    ) as JSONObject

    fun restock(kitType: String, quantity: Int, note: String = ""): JSONObject = ApiJardin.request(
        "POST", "/inventory/restock", JSONObject().put("kitType", kitType).put("quantity", quantity).put("note", note).put("date", LocalDate.now().toString())
    ) as JSONObject

    private fun childBody(name: String, classroom: String, guardians: List<Guardian>, birthDate: String, document: String, complement: String) = JSONObject()
        .put("name", name.trim())
        .put("classroom", classroom.trim())
        .put("document", document.trim())
        .put("complement", complement.trim())
        .put("authorizedGuardians", JSONArray().apply {
            guardians.forEach { guardian -> put(JSONObject().put("name", guardian.name.trim()).put("document", guardian.document.trim()).put("relation", guardian.relation)) }
        }).apply { if (birthDate.isNotBlank()) put("birthDate", birthDate.trim()) }

    private fun JSONArray.toObjects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun relationId(value: Any?): String = when (value) {
        is JSONObject -> value.optString("_id")
        null -> ""
        else -> value.toString()
    }
    private fun maskDocument(document: String): String = if (document.length <= 2) "•••• $document" else "•••• ${document.takeLast(2)}"
}
