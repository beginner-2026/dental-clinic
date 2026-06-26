package com.dental.ui.odontogram

import com.dental.data.ToothRepository
import com.dental.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

data class OdontogramState(
    val teeth: List<Tooth> = emptyList(),
    val prostheticItems: List<ProstheticItem> = emptyList(),
    val currentQuadrant: Int = 1,
    val viewMode: OdontogramViewMode = OdontogramViewMode.FULL_JAW,
    val activeLayer: OdontogramLayer = OdontogramLayer.ORTHO,
    val selectedTooth: Int? = null,
    val selectedToothPart: ToothPart? = null,
    val showToothMenu: Boolean = false,
    val showToothPartMenu: Boolean = false,
    val bridgeMode: Boolean = false,
    val bridgeFirstTooth: Int? = null,
    val bridgeSecondTooth: Int? = null,
    val undoStack: List<ProstheticItem> = emptyList(),
    val readOnly: Boolean = false,
    val currentPatientId: Long? = null,
    val currentPatientName: String = "",
    val crownSelections: Map<Int, CrownOption> = emptyMap(),
    val rootSelections: Map<Int, RootOption> = emptyMap()
)

enum class OdontogramViewMode { FULL_JAW, QUADRANT }
enum class OdontogramLayer { TEETH, ORTHO, PERIO }

class OdontogramViewModel(
    private val toothRepository: ToothRepository? = null
) {
    private val _state = MutableStateFlow(OdontogramState())
    val state: StateFlow<OdontogramState> = _state.asStateFlow()

    fun loadPatientData(patientId: Long, patientName: String) {
        val repo = toothRepository ?: return
        val teeth = repo.getTeethByPatientId(patientId)
        val items = repo.getProstheticItemsByPatientId(patientId)
        val finalTeeth = if (teeth.isEmpty()) {
            repo.initDefaultTeeth(patientId)
            repo.getTeethByPatientId(patientId)
        } else {
            teeth
        }
        _state.value = OdontogramState(
            teeth = finalTeeth,
            prostheticItems = items,
            currentPatientId = patientId,
            currentPatientName = patientName
        )
    }

    fun clearPatientData() {
        _state.value = OdontogramState()
    }

    fun setTeeth(teeth: List<Tooth>) {
        _state.value = _state.value.copy(teeth = teeth)
    }

    fun setProstheticItems(items: List<ProstheticItem>) {
        _state.value = _state.value.copy(prostheticItems = items)
    }

    fun setViewMode(mode: OdontogramViewMode) {
        _state.value = _state.value.copy(viewMode = mode)
    }

    fun setActiveLayer(layer: OdontogramLayer) {
        _state.value = _state.value.copy(activeLayer = layer)
    }

    fun setQuadrant(q: Int) {
        _state.value = _state.value.copy(currentQuadrant = q)
    }

    fun toggleReadOnly() {
        _state.value = _state.value.copy(readOnly = !_state.value.readOnly)
    }

    fun selectTooth(number: Int, part: ToothPart? = null) {
        val s = _state.value
        if (s.readOnly) return

        if (s.bridgeMode) {
            if (s.bridgeFirstTooth == null) {
                _state.value = s.copy(bridgeFirstTooth = number)
            } else {
                val first = s.bridgeFirstTooth!!
                val quadFirst = first / 10
                val quadSecond = number / 10
                if (quadFirst == quadSecond) {
                    _state.value = s.copy(
                        bridgeSecondTooth = number,
                        selectedTooth = number,
                        showToothMenu = true
                    )
                }
            }
        } else if (part != null) {
            _state.value = s.copy(
                selectedTooth = number,
                selectedToothPart = part,
                showToothPartMenu = true
            )
        } else {
            _state.value = s.copy(
                selectedTooth = number,
                showToothMenu = true
            )
        }
    }

    fun dismissMenu() {
        _state.value = _state.value.copy(
            selectedTooth = null,
            selectedToothPart = null,
            showToothMenu = false,
            showToothPartMenu = false,
            bridgeMode = false,
            bridgeFirstTooth = null,
            bridgeSecondTooth = null
        )
    }

    fun setToothStatus(number: Int, status: ToothStatus) {
        val s = _state.value
        val updatedTeeth = s.teeth.map { tooth ->
            if (tooth.number == number) tooth.copy(status = status) else tooth
        }.ifEmpty {
            listOf(Tooth(id = 0, patientId = s.currentPatientId ?: 0, number = number, arch = getJawForQuadrant(number / 10), quadrant = number / 10, status = status))
        }
        _state.value = s.copy(teeth = updatedTeeth)

        val pid = s.currentPatientId
        if (pid != null && toothRepository != null) {
            toothRepository.updateToothStatus(pid, number, status)
        }
    }

    fun toggleBridgeMode() {
        val s = _state.value
        _state.value = s.copy(
            bridgeMode = !s.bridgeMode,
            bridgeFirstTooth = null,
            bridgeSecondTooth = null
        )
    }

    fun applyProsthetic(type: ProstheticType, material: ProstheticMaterial, stage: ProstheticStage) {
        val s = _state.value
        val toothNumber = s.selectedTooth ?: return
        val pid = s.currentPatientId ?: return

        val toothIds = if (type == ProstheticType.BRIDGE && s.bridgeFirstTooth != null && s.bridgeSecondTooth != null) {
            val start = minOf(s.bridgeFirstTooth!!, s.bridgeSecondTooth!!)
            val end = maxOf(s.bridgeFirstTooth!!, s.bridgeSecondTooth!!)
            (start..end).toList()
        } else {
            listOf(toothNumber)
        }

        val oldItems = s.prostheticItems

        if (type == ProstheticType.REMOVAL) {
            val toRemove = oldItems.filter { item ->
                item.toothIds.any { it in toothIds }
            }
            toRemove.forEach { item ->
                if (item.id != 0L) toothRepository?.deleteProstheticItem(item.id)
            }
            _state.value = s.copy(
                prostheticItems = oldItems.filterNot { item ->
                    item.toothIds.any { it in toothIds }
                },
                showToothMenu = false,
                selectedTooth = null,
                bridgeMode = false,
                bridgeFirstTooth = null,
                bridgeSecondTooth = null
            )
            return
        }

        val newItem = ProstheticItem(
            id = 0L,
            patientId = pid,
            toothIds = toothIds,
            type = type,
            material = material,
            stage = stage,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )

        val savedId = toothRepository?.saveProstheticItem(newItem) ?: 0L
        val savedItem = newItem.copy(id = savedId)

        _state.value = s.copy(
            prostheticItems = oldItems + savedItem,
            undoStack = s.undoStack + savedItem,
            showToothMenu = false,
            selectedTooth = null,
            bridgeMode = false,
            bridgeFirstTooth = null,
            bridgeSecondTooth = null
        )
    }

    fun applyCrownOption(number: Int, option: CrownOption) {
        val s = _state.value
        val updated = if (option == CrownOption.MISSING) {
            s.crownSelections + (number to option) to s.rootSelections
        } else {
            s.crownSelections + (number to option) to s.rootSelections
        }
        _state.value = s.copy(
            crownSelections = updated.first,
            showToothPartMenu = false,
            selectedTooth = null,
            selectedToothPart = null
        )
    }

    fun applyRootOption(number: Int, option: RootOption) {
        val s = _state.value
        _state.value = s.copy(
            rootSelections = s.rootSelections + (number to option),
            showToothPartMenu = false,
            selectedTooth = null,
            selectedToothPart = null
        )
    }

    fun dismissPartMenu() {
        _state.value = _state.value.copy(
            showToothPartMenu = false,
            selectedTooth = null,
            selectedToothPart = null
        )
    }

    fun undo() {
        val s = _state.value
        if (s.undoStack.isEmpty()) return
        val last = s.undoStack.last()
        if (last.id != 0L) toothRepository?.deleteProstheticItem(last.id)
        _state.value = s.copy(
            prostheticItems = s.prostheticItems.filterNot { it.id == last.id && it.toothIds == last.toothIds },
            undoStack = s.undoStack.dropLast(1)
        )
    }

    fun getToothProsthetics(number: Int): List<ProstheticItem> {
        return _state.value.prostheticItems.filter { item ->
            number in item.toothIds && item.stage != ProstheticStage.COMPLETED
        }
    }

    fun getLatestStage(number: Int): ProstheticStage {
        val items = getToothProsthetics(number)
        val tooth = _state.value.teeth.find { it.number == number }
        if (tooth?.status == ToothStatus.MISSING) return ProstheticStage.COMPLETED
        return items.maxByOrNull { it.updatedAt }?.stage ?: ProstheticStage.EXISTING
    }

    companion object {
        fun getQuadrantToothNumbers(quadrant: Int): List<Int> {
            return when (quadrant) {
                1 -> (18 downTo 11).toList()
                2 -> (21..28).toList()
                3 -> (31..38).toList()
                4 -> (41 downTo 48).toList()
                else -> emptyList()
            }
        }

        fun getQuadrantForTooth(number: Int): Int = number / 10
        fun getJawForQuadrant(q: Int): Arch = if (q <= 2) Arch.UPPER else Arch.LOWER
    }
}
