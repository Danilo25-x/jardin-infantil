package com.jardininfantil.app

import android.content.Intent
import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jardininfantil.app.data.Child
import com.jardininfantil.app.data.Guardian
import com.jardininfantil.app.data.Jardin
import com.jardininfantil.app.data.Status
import com.jardininfantil.app.data.Supply
import com.jardininfantil.app.data.JardinRepository
import com.jardininfantil.app.data.ApiJardin
import com.jardininfantil.app.data.SessionUser
import com.jardininfantil.app.data.HistoryData
import java.util.concurrent.Executors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Navegación de la app. Cada pantalla es XML y [Jardin] conserva la última respuesta de la API.
 * La sesión se valida con la API y las acciones visibles dependen del rol asignado.
 */
class MainActivity : AppCompatActivity() {

    private var selected: Child? = null
    private var currentUser: SessionUser? = null
    private var reportMonth: java.time.YearMonth = java.time.YearMonth.now()
    private var roomFilter: String? = null
    private var statusFilter: Status? = null
    private val inflater by lazy { LayoutInflater.from(this) }
    private val locale = Locale("es", "CO")
    private val io = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiJardin.bearerToken = null
        currentUser = null
        showLogin()
    }

    // ---------- Utilidades ----------

    private fun <T : View> v(id: Int): T = findViewById(id)

    private fun text(id: Int, value: CharSequence) {
        v<TextView>(id).text = value
    }

    private fun onClick(id: Int, action: () -> Unit) {
        v<View>(id).setOnClickListener { action() }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    /** La red corre fuera del hilo de pantalla; el resultado vuelve al hilo principal. */
    private fun <T> apiCall(
        action: () -> T,
        onSuccess: (T) -> Unit,
        onError: (String) -> Unit = { toast(it) }
    ) {
        io.execute {
            try {
                val result = action()
                ui.post { onSuccess(result) }
            } catch (error: Exception) {
                ui.post { onError(error.message ?: "No fue posible conectar con la API") }
            }
        }
    }

    private fun reloadApi(onDone: () -> Unit) = apiCall(
        { JardinRepository.reload() },
        { onDone() },
        { message -> toast(message); onDone() }
    )

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun pill(view: TextView, label: String, bg: Int, fg: Int) {
        view.text = label
        view.setBackgroundResource(bg)
        view.setTextColor(getColor(fg))
    }

    private fun childPill(view: TextView, status: Status) {
        when (status) {
            Status.PRESENTE -> pill(view, status.label, R.drawable.fondo_estado_verde, R.color.verde_oscuro)
            Status.AUSENTE -> pill(view, status.label, R.drawable.fondo_estado_neutro, R.color.texto_suave)
            Status.ENTREGADO -> pill(view, status.label, R.drawable.fondo_estado_azul, R.color.azul_texto)
        }
    }

    private fun supplyPill(view: TextView, supply: Supply) {
        when {
            supply.units == 0 -> pill(view, "Agotado", R.drawable.fondo_estado_coral, R.color.coral_texto)
            supply.units <= supply.lowAt -> pill(view, "Bajo", R.drawable.fondo_estado_amarillo, R.color.amarillo_texto)
            else -> pill(view, "Normal", R.drawable.fondo_estado_verde, R.color.verde_oscuro)
        }
    }

    private fun avatarBackground(room: String) =
        if (room.contains("Explorador")) R.drawable.fondo_circulo_verde else R.drawable.fondo_circulo_amarillo

    /** Agrega una fila a un contenedor con un separador entre filas. */
    private fun addRow(container: LinearLayout, row: View) {
        if (container.childCount > 0) {
            val divider = View(this).apply { setBackgroundColor(getColor(R.color.borde)) }
            container.addView(divider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)))
        }
        container.addView(row)
    }

    private fun emptyText(message: String) = TextView(this).apply {
        setText(message)
        setTextColor(getColor(R.color.texto_suave))
        textSize = 13f
        setPadding(dp(14), dp(16), dp(14), dp(16))
    }

    private fun spinnerAdapter(items: List<String>) =
        ArrayAdapter(this, R.layout.elemento_selector, items).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

    private fun greetingWord(): String = when (LocalTime.now().hour) {
        in 5..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    private fun todayLabel(): String =
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", locale)).uppercase()

    private fun todayShort(): String =
        "Hoy, " + LocalDate.now().format(DateTimeFormatter.ofPattern("d 'de' MMMM", locale))

    /** Barra inferior compartida: marca la pestaña activa y enruta cada botón. */
    private fun bottomNav(selectedKey: String, onHome: () -> Unit) {
        fun tint(key: String, iconId: Int, labelId: Int) {
            val color = getColor(if (key == selectedKey) R.color.verde_oscuro else R.color.texto_suave)
            v<ImageView>(iconId).imageTintList = ColorStateList.valueOf(color)
            v<TextView>(labelId).setTextColor(color)
        }
        tint("home", R.id.navHomeIcon, R.id.navHomeText)
        tint("children", R.id.navChildrenIcon, R.id.navChildrenText)
        tint("reports", R.id.navReportsIcon, R.id.navReportsText)
        onClick(R.id.navHome, onHome)
        onClick(R.id.navChildren) { showChildren() }
        v<View>(R.id.navReports).visibility = if (currentUser?.isAdministrator == true) View.VISIBLE else View.GONE
        onClick(R.id.navReports) { if (currentUser?.isAdministrator == true) showReports() else toast("Solo administración puede consultar reportes") }
    }

    // ---------- Filas reutilizables ----------

    private fun childRow(child: Child, container: LinearLayout, onTap: () -> Unit) {
        val row = inflater.inflate(R.layout.fila_nino, container, false)
        row.findViewById<TextView>(R.id.avatarText).apply {
            text = child.initials
            setBackgroundResource(avatarBackground(child.room))
        }
        row.findViewById<TextView>(R.id.childName).text = child.name
        row.findViewById<TextView>(R.id.childRoom).text = child.room
        childPill(row.findViewById<TextView>(R.id.statusPill), child.status)
        row.setOnClickListener { onTap() }
        addRow(container, row)
    }

    private fun supplyRow(supply: Supply, container: LinearLayout) {
        val row = inflater.inflate(R.layout.fila_insumo, container, false)
        row.findViewById<TextView>(R.id.supplyName).text = supply.name
        row.findViewById<TextView>(R.id.supplyUnits).text = "${supply.units} unidades"
        supplyPill(row.findViewById<TextView>(R.id.statusPill), supply)
        addRow(container, row)
    }

    private fun supplyDetailRow(supply: Supply, container: LinearLayout) {
        val row = inflater.inflate(R.layout.detalle_insumo, container, false)
        row.findViewById<TextView>(R.id.supplyName).text = supply.name
        row.findViewById<TextView>(R.id.supplyUnits).text = supply.units.toString()
        row.findViewById<ProgressBar>(R.id.supplyBar).progress = supply.units
        supplyPill(row.findViewById<TextView>(R.id.statusPill), supply)
        container.addView(row)
    }

    // ---------- Pantallas ----------

    private fun showLogin() {
        setContentView(R.layout.pantalla_acceso)
        var passwordVisible = false
        val password = v<EditText>(R.id.passwordInput)

        onClick(R.id.togglePassword) {
            passwordVisible = !passwordVisible
            v<ImageView>(R.id.togglePassword).contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
            password.inputType = InputType.TYPE_CLASS_TEXT or
                (if (passwordVisible) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else InputType.TYPE_TEXT_VARIATION_PASSWORD)
            password.setSelection(password.text.length)
        }

        onClick(R.id.loginButton) {
            val email = v<EditText>(R.id.emailInput).text.toString().trim()
            val pass = password.text.toString()
            val error = when {
                email.isBlank() || pass.isBlank() -> "Ingresa tu correo y contraseña."
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Revisa el formato del correo."
                else -> ""
            }
            text(R.id.loginError, error)
            if (error.isEmpty()) {
                v<Button>(R.id.loginButton).isEnabled = false
                apiCall({ JardinRepository.login(email, pass) }, { response ->
                    val user = response.getJSONObject("user")
                    val token = response.getString("token")
                    currentUser = SessionUser(user.optString("id", user.optString("_id")), user.optString("name"), user.optString("email"), user.optString("role", "cuidadora"))
                    ApiJardin.bearerToken = token
                    showHome()
                }, { message -> v<Button>(R.id.loginButton).isEnabled = true; text(R.id.loginError, message) })
            }
        }
    }

    private fun showHome(refresh: Boolean = true) {
        if (refresh) {
            setContentView(R.layout.pantalla_inicio)
            reloadApi { showHome(false) }
            return
        }
        setContentView(R.layout.pantalla_inicio)
        text(R.id.dateLabel, todayLabel())
        text(R.id.greeting, "${greetingWord()}, equipo")
        text(R.id.totalCount, Jardin.children.size.toString())
        text(R.id.arrivedCount, Jardin.arrived().toString())

        val pending = Jardin.pendingPickup()
        text(R.id.pendingCount, pending.size.toString())
        val list = v<LinearLayout>(R.id.pendingList)
        if (pending.isEmpty()) addRow(list, emptyText("No hay niños pendientes de salida."))
        pending.forEach { child ->
            childRow(child, list) { selected = child; showProfile() }
        }

        onClick(R.id.avatarButton) { showAccountMenu() }
        onClick(R.id.actionAttendance) { showChildren() }
        onClick(R.id.actionDelivery) { showChildren() }
        onClick(R.id.actionComplement) { showComplement() }
        onClick(R.id.seeAll) { showChildren() }
        bottomNav("home") { showHome() }
    }

    private fun showChildren(refresh: Boolean = true) {
        if (refresh) {
            setContentView(R.layout.pantalla_ninos)
            reloadApi { showChildren(false) }
            return
        }
        setContentView(R.layout.pantalla_ninos)
        val container = v<LinearLayout>(R.id.childContainer)
        val search = v<EditText>(R.id.searchInput)
        val roomButton = v<TextView>(R.id.roomFilter)
        val statusButton = v<TextView>(R.id.statusFilter)

        fun render() {
            val query = search.text.toString()
            val matches = Jardin.children.filter { child ->
                child.name.contains(query, ignoreCase = true) &&
                    (roomFilter == null || child.room == roomFilter) &&
                    (statusFilter == null || child.status == statusFilter)
            }
            roomButton.text = roomFilter ?: "Todos los salones"
            statusButton.text = statusFilter?.label ?: "Estado: todos"
            text(R.id.childrenCount, "${matches.size} niños")
            container.removeAllViews()
            if (matches.isEmpty()) addRow(container, emptyText("No encontramos niños con ese filtro."))
            matches.forEach { child ->
                childRow(child, container) { selected = child; showProfile() }
            }
        }

        roomButton.setOnClickListener {
            val options: List<String?> = listOf(null) + Jardin.rooms
            roomFilter = options[(options.indexOf(roomFilter) + 1) % options.size]
            render()
        }
        statusButton.setOnClickListener {
            val options: List<Status?> = listOf(null) + Status.values().toList()
            statusFilter = options[(options.indexOf(statusFilter) + 1) % options.size]
            render()
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = render()
            override fun afterTextChanged(s: Editable?) = Unit
        })

        render()
        onClick(R.id.addChildButton) { if (currentUser?.isAdministrator == true) editChild(null) else toast("Solo administración puede registrar niños") }
        v<Button>(R.id.addChildButton).visibility = if (currentUser?.isAdministrator == true) View.VISIBLE else View.GONE
        bottomNav("children") { showHome() }
    }

    private fun showProfile(refresh: Boolean = true) {
        val child = selected ?: return showChildren()
        if (refresh) {
            apiCall({ JardinRepository.child(child) }, { selected = it; showProfile(false) }, { message -> toast(message); showProfile(false) })
            return
        }
        setContentView(R.layout.pantalla_perfil_nino)
        text(R.id.headerTitle, "Información del niño")
        v<ImageView>(R.id.headerAction).visibility = View.VISIBLE
        onClick(R.id.backButton) { showChildren() }
        onClick(R.id.headerAction) { addGuardian(child) }
        onClick(R.id.editChildButton) { editChild(child) }
        onClick(R.id.retireChildButton) { retireChild(child) }
        val adminOnly = if (currentUser?.isAdministrator == true) View.VISIBLE else View.GONE
        v<View>(R.id.editChildButton).visibility = adminOnly
        v<View>(R.id.retireChildButton).visibility = adminOnly
        v<View>(R.id.headerAction).visibility = adminOnly

        v<TextView>(R.id.childInitials).text = child.initials
        text(R.id.childName, child.name)
        childPill(v<TextView>(R.id.childStatus), child.status)
        text(R.id.profileAge, if (child.age > 0) "${child.age} años" else "No reportada")
        text(R.id.profileBirth, child.birth)
        text(R.id.profileRoom, child.room)
        text(R.id.profileDocument, child.document)
        text(R.id.complementName, child.complement)

        val guardians = v<LinearLayout>(R.id.guardianContainer)
        child.guardians.filter { it.authorized }.forEach { g: Guardian ->
            val row = inflater.inflate(R.layout.fila_acudiente, guardians, false)
            row.findViewById<TextView>(R.id.avatarText).text = g.initials
            row.findViewById<TextView>(R.id.guardianName).text = g.name
            row.findViewById<TextView>(R.id.guardianDetail).text = "${g.relation} · ${g.idMasked}"
            addRow(guardians, row)
        }

        val action = v<Button>(R.id.primaryAction)
        when (child.status) {
            Status.AUSENTE -> {
                action.text = "REGISTRAR LLEGADA"
                action.setOnClickListener { showCheckin() }
            }
            Status.PRESENTE -> {
                action.text = "ENTREGAR NIÑO"
                action.setOnClickListener { showDelivery() }
            }
            Status.ENTREGADO -> {
                action.text = "ENTREGADO HOY"
                action.isEnabled = false
                action.setBackgroundResource(R.drawable.fondo_estado_neutro)
                action.setTextColor(getColor(R.color.texto_suave))
            }
        }
    }

    private fun showCheckin() {
        val child = selected ?: return showChildren()
        setContentView(R.layout.pantalla_llegada)
        text(R.id.headerTitle, "Registrar llegada")
        onClick(R.id.backButton) { showProfile() }

        v<TextView>(R.id.childInitials).text = child.initials
        text(R.id.childName, child.name)
        text(R.id.childRoom, child.room)
        text(R.id.currentTime, LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a", locale)))
        text(R.id.currentDate, LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", locale)))

        val register = v<Button>(R.id.registerArrival)
        if (child.status != Status.AUSENTE) {
            register.text = "LLEGADA YA REGISTRADA"
            register.isEnabled = false
            register.setBackgroundResource(R.drawable.fondo_estado_neutro)
            register.setTextColor(getColor(R.color.texto_suave))
        }
        register.setOnClickListener {
            register.isEnabled = false
            apiCall({ JardinRepository.checkIn(child.id); JardinRepository.reload() }, {
                selected = Jardin.children.firstOrNull { it.id == child.id }
                toast("Llegada de ${child.firstName} registrada")
                showProfile()
            }, { message -> register.isEnabled = true; toast(message) })
        }
    }

    private fun showDelivery() {
        val child = selected ?: return showChildren()
        setContentView(R.layout.pantalla_entrega)
        text(R.id.headerTitle, "Entregar niño")
        onClick(R.id.backButton) { showProfile() }

        v<TextView>(R.id.childInitials).text = child.initials
        text(R.id.childName, child.name)
        text(R.id.childRoom, child.room)
        text(R.id.pickupTitle, "¿Quién recoge a ${child.firstName}?")

        val container = v<LinearLayout>(R.id.guardianContainer)
        val radios = mutableListOf<RadioButton>()
        var chosen: Guardian? = null
        child.guardians.filter { it.authorized }.forEach { g ->
            val row = inflater.inflate(R.layout.opcion_acudiente, container, false)
            row.findViewById<TextView>(R.id.avatarText).text = g.initials
            row.findViewById<TextView>(R.id.guardianName).text = g.name
            row.findViewById<TextView>(R.id.guardianDetail).text = g.relation
            val radio = row.findViewById<RadioButton>(R.id.radio)
            radios += radio
            row.setOnClickListener {
                radios.forEach { r -> r.isChecked = r === radio }
                chosen = g
            }
            addRow(container, row)
        }

        onClick(R.id.confirmDelivery) {
            val who = chosen
            if (who == null) {
                toast("Selecciona una persona autorizada")
            } else {
                val attendanceId = child.attendanceId
                if (attendanceId.isNullOrBlank()) {
                    toast("No hay una llegada registrada para este niño")
                } else {
                    v<Button>(R.id.confirmDelivery).isEnabled = false
                    apiCall({ JardinRepository.checkOut(attendanceId, who); JardinRepository.reload() }, {
                        toast("${child.firstName} fue entregado a ${who.name}")
                        showHome(false)
                    }, { message -> v<Button>(R.id.confirmDelivery).isEnabled = true; toast(message) })
                }
            }
        }
    }

    private fun showComplement(refresh: Boolean = true) {
        if (refresh) {
            setContentView(R.layout.pantalla_alimentacion)
            reloadApi { showComplement(false) }
            return
        }
        setContentView(R.layout.pantalla_alimentacion)
        text(R.id.headerTitle, "Complementación")
        onClick(R.id.backButton) { showHome() }
        text(R.id.dateLabel, todayShort())
        text(R.id.deliveredCount, Jardin.complementDelivered.size.toString())
        text(R.id.pendingCount, Jardin.complementPending().size.toString())

        val container = v<LinearLayout>(R.id.supplyContainer)
        Jardin.supplies.forEach { supplyRow(it, container) }

        onClick(R.id.seeInventory) { showInventory() }
        onClick(R.id.newDeliveryButton) { showNewDelivery() }
        onClick(R.id.historyButton) { showHistory(LocalDate.now()) }
    }

    private fun showNewDelivery(refresh: Boolean = true) {
        if (refresh) {
            setContentView(R.layout.pantalla_nueva_entrega)
            reloadApi { showNewDelivery(false) }
            return
        }
        setContentView(R.layout.pantalla_nueva_entrega)
        text(R.id.headerTitle, "Nueva entrega")
        onClick(R.id.backButton) { showComplement() }

        val childSpinner = v<Spinner>(R.id.childSpinner)
        val supplySpinner = v<Spinner>(R.id.supplySpinner)
        childSpinner.adapter = spinnerAdapter(Jardin.children.map { it.name })
        supplySpinner.adapter = spinnerAdapter(Jardin.supplies.map { it.name })
        selected?.let { current ->
            val index = Jardin.children.indexOfFirst { it.id == current.id }
            if (index >= 0) childSpinner.setSelection(index)
        }

        var quantity = 1
        val quantityText = v<TextView>(R.id.quantity)
        val available = v<TextView>(R.id.available)

        fun currentSupply(): Supply? = Jardin.supplies.getOrNull(supplySpinner.selectedItemPosition)

        fun refresh() {
            val supply = currentSupply() ?: run {
                quantityText.text = "0"
                available.text = "No hay kits configurados"
                return
            }
            quantity = quantity.coerceIn(1, maxOf(1, supply.units))
            quantityText.text = quantity.toString()
            available.text = "${supply.units} unidades"
        }

        supplySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                quantity = 1
                refresh()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        onClick(R.id.minusButton) { quantity = maxOf(1, quantity - 1); refresh() }
        onClick(R.id.plusButton) { quantity++; refresh() }

        onClick(R.id.submitDelivery) {
            val child = Jardin.children.getOrNull(childSpinner.selectedItemPosition)
            val supply = currentSupply()
            if (child == null || supply == null) {
                toast("No hay niños o kits disponibles")
            } else {
                v<Button>(R.id.submitDelivery).isEnabled = false
                apiCall({ JardinRepository.deliverAid(child.id, supply.name, quantity); JardinRepository.reload() }, {
                    toast("Entrega registrada para ${child.firstName}")
                    showComplement(false)
                }, { message -> v<Button>(R.id.submitDelivery).isEnabled = true; toast(message) })
            }
        }

        refresh()
    }

    private fun showInventory(refresh: Boolean = true) {
        if (refresh) {
            setContentView(R.layout.pantalla_inventario)
            reloadApi { showInventory(false) }
            return
        }
        setContentView(R.layout.pantalla_inventario)
        text(R.id.headerTitle, "Inventario")
        onClick(R.id.backButton) { showComplement() }

        text(R.id.totalUnits, "${Jardin.supplies.sumOf { it.units }} unidades")
        val low = Jardin.lowSupplies()
        text(
            R.id.alertBadge,
            if (low.isEmpty()) "Sin alertas" else "${low.size} alerta${if (low.size > 1) "s" else ""}"
        )
        text(
            R.id.recommendation,
            if (low.isEmpty()) "Inventario en niveles normales."
            else "Se recomienda reponer ${low.joinToString(" y ") { it.name }} antes de la próxima entrega."
        )

        val container = v<LinearLayout>(R.id.supplyContainer)
        Jardin.supplies.forEach { supplyDetailRow(it, container) }
        val restock = v<Button>(R.id.restockButton)
        restock.visibility = if (currentUser?.isAdministrator == true) View.VISIBLE else View.GONE
        restock.setOnClickListener { restockInventory() }
        val movements = v<Button>(R.id.inventoryHistoryButton)
        movements.visibility = if (currentUser?.isAdministrator == true) View.VISIBLE else View.GONE
        movements.setOnClickListener { showInventoryMovements() }
    }

    private fun showAdmin(refresh: Boolean = true) {
        if (currentUser?.isAdministrator != true) return showHome()
        if (refresh) {
            setContentView(R.layout.pantalla_administracion)
            reloadApi { showAdmin(false) }
            return
        }
        setContentView(R.layout.pantalla_administracion)
        onClick(R.id.avatarButton) { showHome() }
        text(R.id.updatedAt, "Actualizado ${LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a", locale))}")

        text(R.id.metricChildren, Jardin.children.size.toString())
        text(R.id.metricArrived, "${Jardin.arrived()} / ${Jardin.children.size}")
        text(R.id.metricComplements, Jardin.complementDelivered.size.toString())
        val low = Jardin.lowSupplies()
        text(R.id.metricAlerts, low.size.toString())
        text(
            R.id.alertText,
            if (low.isEmpty()) "Sin alertas de inventario."
            else low.joinToString(" · ") { "${it.name}: ${it.units} unidades" }
        )

        onClick(R.id.manageChildren) { showChildren() }
        onClick(R.id.manageAttendance) { showChildren() }
        onClick(R.id.manageComplement) { showComplement() }
        onClick(R.id.manageInventory) { showInventory() }
        onClick(R.id.manageReports) { if (currentUser?.isAdministrator == true) showReports() else toast("Acción exclusiva de administración") }
        onClick(R.id.logoutButton) { clearSession(); showLogin() }
        onClick(R.id.reviewInventory) { showInventory() }
        bottomNav("home") { showAdmin() }
    }

    private fun showReports(refresh: Boolean = true) {
        if (currentUser?.isAdministrator != true) return toast("Solo administración puede consultar reportes")
        if (refresh) {
            setContentView(R.layout.pantalla_reportes)
            apiCall({ JardinRepository.reload(reportMonth = reportMonth.toString()) }, { showReports(false) })
            return
        }
        setContentView(R.layout.pantalla_reportes)
        text(R.id.headerTitle, "Reportes")
        onClick(R.id.backButton) { showHome() }

        val registered = Jardin.children.size
        val report = Jardin.reportSummary
        val totals = report.optJSONObject("totals") ?: org.json.JSONObject()
        val attended = totals.optInt("childrenAttended", Jardin.arrived())
        val aidDeliveries = totals.optInt("aidDeliveries", Jardin.complementDelivered.size)
        text(R.id.reportRegistered, registered.toString())
        text(R.id.reportAttended, attended.toString())
        text(R.id.reportAbsent, (registered - attended).toString())
        text(R.id.reportDelivered, aidDeliveries.toString())
        text(R.id.reportPending, Jardin.complementPending().size.toString())

        text(R.id.period, "Periodo: " + reportMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)).replaceFirstChar { it.uppercase(locale) })
        val container = v<LinearLayout>(R.id.reportInventory)
        Jardin.supplies.forEach { supplyRow(it, container) }
        val details = v<LinearLayout>(R.id.reportDetail)
        details.removeAllViews()
        val attendance = report.optJSONArray("attendanceByChild") ?: org.json.JSONArray()
        val feeding = report.optJSONArray("aidByKit") ?: org.json.JSONArray()
        if (attendance.length() == 0 && feeding.length() == 0) addRow(details, emptyText("Sin actividad registrada en este periodo."))
        for (i in 0 until attendance.length()) {
            val row = attendance.getJSONObject(i)
            addRow(details, emptyText("${row.optString("name")}: ${row.optInt("arrivals")} llegadas · ${row.optInt("delivered")} entregas"))
        }
        for (i in 0 until feeding.length()) {
            val row = feeding.getJSONObject(i)
            addRow(details, emptyText("${row.optString("kitType")}: ${row.optInt("deliveries")} entregas · ${row.optInt("units")} unidades"))
        }
        details.visibility = View.GONE
        onClick(R.id.period) { DatePickerDialog(this, { _, year, month, _ -> reportMonth = java.time.YearMonth.of(year, month + 1); showReports() }, reportMonth.year, reportMonth.monthValue - 1, 1).show() }
        onClick(R.id.viewDetail) { details.visibility = if (details.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
        onClick(R.id.exportReport) { shareReport() }
        bottomNav("reports") { showHome() }
    }

    private fun showAccountMenu() {
        val options = if (currentUser?.isAdministrator == true) arrayOf("Administración", "Cerrar sesión") else arrayOf("Cerrar sesión")
        AlertDialog.Builder(this).setTitle(currentUser?.name ?: "Cuenta del jardín").setItems(options) { _, which ->
            if (currentUser?.isAdministrator == true && which == 0) showAdmin()
            else { clearSession(); showLogin() }
        }.setNegativeButton("Volver", null).show()
    }

    private fun clearSession() {
        ApiJardin.bearerToken = null
        currentUser = null
    }

    private fun editChild(child: Child?) {
        if (currentUser?.isAdministrator != true) return toast("Acción exclusiva de administración")
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(8), dp(24), 0) }
        fun field(label: String, value: String = "", type: Int = InputType.TYPE_CLASS_TEXT): EditText = EditText(this).also {
            it.hint = label; it.setText(value); it.inputType = type; form.addView(it)
        }
        val name = field("Nombre completo del niño", child?.name.orEmpty())
        val classroom = field("Salón", child?.room.orEmpty())
        val birth = field("Fecha nacimiento AAAA-MM-DD", child?.birth?.takeIf { it != "No reportada" }.orEmpty())
        val document = field("Documento del niño", child?.document?.takeIf { it != "No reportado" }.orEmpty())
        val complement = field("Complementación asignada", child?.complement.orEmpty())
        val guardian = child?.guardians?.firstOrNull()
        val guardianName = field("Nombre del acudiente", guardian?.name.orEmpty())
        val guardianDocument = field("Documento del acudiente", guardian?.document.orEmpty())
        val relation = field("Parentesco", guardian?.relation.orEmpty())
        AlertDialog.Builder(this).setTitle(if (child == null) "Registrar niño" else "Editar información")
            .setView(form).setNegativeButton("Cancelar", null).setPositiveButton("Guardar", null).create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (name.text.isBlank() || classroom.text.isBlank() || guardianName.text.isBlank() || guardianDocument.text.isBlank()) return@setOnClickListener toast("Completa nombre, salón y acudiente con documento")
                        val guardians = listOf(Guardian(guardianName.text.toString(), relation.text.toString().ifBlank { "Acudiente" }, guardianDocument.text.toString(), document = guardianDocument.text.toString())) + child?.guardians.orEmpty().drop(1)
                        val save = {
                            if (child == null) JardinRepository.createChild(name.text.toString(), classroom.text.toString(), guardians, birth.text.toString(), document.text.toString(), complement.text.toString())
                            else JardinRepository.updateChild(child.id, name.text.toString(), classroom.text.toString(), guardians, birth.text.toString(), document.text.toString(), complement.text.toString())
                        }
                        apiCall({ save(); JardinRepository.reload() }, { dialog.dismiss(); toast("Información guardada"); showChildren(false) })
                    }
                }
                dialog.show()
            }
    }

    private fun addGuardian(child: Child) {
        if (currentUser?.isAdministrator != true) return toast("Acción exclusiva de administración")
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(8), dp(24), 0) }
        fun input(hintText: String): EditText = EditText(this).apply { hint = hintText; form.addView(this) }
        val name = input("Nombre del acudiente")
        val document = input("Documento")
        val relation = input("Parentesco")
        AlertDialog.Builder(this).setTitle("Agregar acudiente autorizado").setView(form)
            .setNegativeButton("Cancelar", null).setPositiveButton("Guardar", null).create().also { dialog ->
                dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    if (name.text.isBlank() || document.text.isBlank()) return@setOnClickListener toast("Completa nombre y documento")
                    val guardian = Guardian(name.text.toString(), relation.text.toString().ifBlank { "Acudiente" }, document.text.toString(), document = document.text.toString())
                    val guardians = child.guardians + guardian
                    apiCall({ JardinRepository.updateChild(child.id, child.name, child.room, guardians, child.birth.takeIf { it != "No reportada" }.orEmpty(), child.document.takeIf { it != "No reportado" }.orEmpty(), child.complement); JardinRepository.reload() }, {
                        selected = Jardin.children.firstOrNull { it.id == child.id }; dialog.dismiss(); showProfile(); toast("Acudiente agregado")
                    })
                } }
                dialog.show()
            }
    }

    private fun retireChild(child: Child) {
        if (currentUser?.isAdministrator != true) return toast("Acción exclusiva de administración")
        AlertDialog.Builder(this).setTitle("Retirar del jardín")
            .setMessage("Se conservará el historial de ${child.name}.")
            .setNegativeButton("Cancelar", null).setPositiveButton("Retirar") { _, _ ->
                apiCall({ JardinRepository.retireChild(child.id); JardinRepository.reload() }, { selected = null; toast("Niño retirado"); showChildren(false) })
            }.show()
    }

    private fun showHistory(date: LocalDate) {
        setContentView(R.layout.pantalla_historial)
        text(R.id.headerTitle, "Historial")
        onClick(R.id.backButton) { showComplement() }
        val dateButton = v<Button>(R.id.historyDate)
        dateButton.text = date.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", locale))
        dateButton.setOnClickListener { DatePickerDialog(this, { _, y, m, d -> showHistory(LocalDate.of(y, m + 1, d)) }, date.year, date.monthValue - 1, date.dayOfMonth).show() }
        apiCall({ JardinRepository.history(date.toString()) }, { history ->
            val attendance = v<LinearLayout>(R.id.historyAttendance)
            val feeding = v<LinearLayout>(R.id.historyFeeding)
            attendance.removeAllViews(); feeding.removeAllViews()
            if (history.attendance.isEmpty()) addRow(attendance, emptyText("Sin registros de asistencia para este día."))
            history.attendance.forEach { item ->
                val childJson = item.opt("child") as? org.json.JSONObject
                val childName = childJson?.optString("name") ?: "Niño"
                val status = if (item.optString("status") == "entregado") "Entregado a ${item.optString("pickedUpBy", "acudiente")} · ${item.optString("checkoutTime")}" else "Presente desde ${item.optString("arrivalTime")}"
                addRow(attendance, emptyText("$childName · $status"))
            }
            if (history.deliveries.isEmpty()) addRow(feeding, emptyText("Sin entregas de alimentación para este día."))
            history.deliveries.forEach { item ->
                val childJson = item.opt("child") as? org.json.JSONObject
                addRow(feeding, emptyText("${childJson?.optString("name") ?: "Niño"} · ${item.optString("kitType")} · ${item.optInt("quantity")} unidades"))
            }
        })
        bottomNav("home") { showHome() }
    }

    private fun restockInventory() {
        if (currentUser?.isAdministrator != true) return toast("Acción exclusiva de administración")
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(8), dp(24), 0) }
        val kit = Spinner(this).apply { adapter = spinnerAdapter(Jardin.supplies.map { it.name }) }
        val quantity = EditText(this).apply { hint = "Unidades recibidas"; inputType = InputType.TYPE_CLASS_NUMBER }
        val note = EditText(this).apply { hint = "Nota opcional" }
        form.addView(kit); form.addView(quantity); form.addView(note)
        AlertDialog.Builder(this).setTitle("Reponer inventario").setView(form).setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null).create().also { dialog ->
                dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val count = quantity.text.toString().toIntOrNull()
                    val supply = Jardin.supplies.getOrNull(kit.selectedItemPosition)
                    if (count == null || count < 1 || supply == null) return@setOnClickListener toast("Indica un número de unidades válido")
                    apiCall({ JardinRepository.restock(supply.name, count, note.text.toString()); JardinRepository.reload() }, { dialog.dismiss(); toast("Inventario actualizado"); showInventory(false) })
                } }
                dialog.show()
            }
    }

    private fun showInventoryMovements() {
        if (currentUser?.isAdministrator != true) return toast("Acción exclusiva de administración")
        apiCall({ JardinRepository.inventoryMovements(reportMonth.toString()) }, { records ->
            val lines = (0 until records.length()).map { i ->
                val row = records.getJSONObject(i)
                "${row.optString("date")} · ${row.optString("kitType")} · +${row.optInt("quantity")} unidades${row.optString("note").takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}"
            }
            AlertDialog.Builder(this).setTitle("Reposiciones de ${reportMonth}")
                .setMessage(lines.ifEmpty { listOf("No hay reposiciones registradas en este mes.") }.joinToString("\n"))
                .setPositiveButton("Cerrar", null).show()
        })
    }

    private fun shareReport() {
        val report = Jardin.reportSummary
        val totals = report.optJSONObject("totals") ?: org.json.JSONObject()
        val rows = report.optJSONArray("attendanceByChild") ?: org.json.JSONArray()
        val aids = report.optJSONArray("aidByKit") ?: org.json.JSONArray()
        val content = buildString {
            appendLine("Reporte del jardín, ${reportMonth}")
            appendLine("Niños registrados: ${Jardin.children.size}")
            appendLine("Asistieron: ${totals.optInt("childrenAttended")}")
            appendLine("Pendientes de salida: ${totals.optInt("pendingPickup")}")
            appendLine("Entregas alimentarias: ${totals.optInt("aidDeliveries")}")
            appendLine(); appendLine("Asistencia por niño")
            for (i in 0 until rows.length()) { val row = rows.getJSONObject(i); appendLine("${row.optString("name")},${row.optInt("arrivals")},${row.optInt("delivered")}") }
            appendLine(); appendLine("Alimentación por kit")
            for (i in 0 until aids.length()) { val row = aids.getJSONObject(i); appendLine("${row.optString("kitType")},${row.optInt("deliveries")},${row.optInt("units")}") }
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, "Reporte del jardín $reportMonth").putExtra(Intent.EXTRA_TEXT, content), "Compartir reporte"))
    }
    override fun onDestroy() {
        io.shutdown()
        super.onDestroy()
    }
}

