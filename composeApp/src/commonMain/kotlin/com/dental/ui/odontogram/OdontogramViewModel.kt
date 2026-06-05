package com.dental.ui.odontogram

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
    val showToothMenu: Boolean = false,
    val bridgeMode: Boolean = false,
    val bridgeFirstTooth: Int? = null,
    val bridgeSecondTooth: Int? = null,
    val undoStack: List<ProstheticItem> = emptyList(),
    val readOnly: Boolean = false
)

enum class OdontogramViewMode { FULL_JAW, QUADRANT }
enum class OdontogramLayer { TEETH, ORTHO, PERIO }

class OdontogramViewModel {
    private val _state = MutableStateFlow(OdontogramState())
    val state: StateFlow<OdontogramState> = _state.asStateFlow()

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

    fun selectTooth(number: Int) {
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
                    val start = minOf(first, number)
                    val end = maxOf(first, number)
                    val ids = (start..end).toList()
                    _state.value = s.copy(
                        bridgeSecondTooth = number,
                        selectedTooth = number,
                        showToothMenu = true
                    )
                }
            }
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
            showToothMenu = false,
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
            listOf(Tooth(id = 0, patientId = 0, number = number, arch = OdontogramViewModel.getJawForQuadrant(number / 10), quadrant = number / 10, status = status))
        }
        _state.value = s.copy(teeth = updatedTeeth)
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

        val toothIds = if (type == ProstheticType.BRIDGE && s.bridgeFirstTooth != null && s.bridgeSecondTooth != null) {
            val start = minOf(s.bridgeFirstTooth!!, s.bridgeSecondTooth!!)
            val end = maxOf(s.bridgeFirstTooth!!, s.bridgeSecondTooth!!)
            (start..end).toList()
        } else {
            listOf(toothNumber)
        }

        val oldItems = s.prostheticItems
        val newItem = ProstheticItem(
            id = Clock.System.now().toEpochMilliseconds(),
            patientId = 0L,
            toothIds = toothIds,
            type = if (type == ProstheticType.REMOVAL) ProstheticType.CROWN else type,
            material = material,
            stage = if (type == ProstheticType.REMOVAL) ProstheticStage.COMPLETED else stage,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )

        _state.value = s.copy(
            prostheticItems = if (type == ProstheticType.REMOVAL) {
                oldItems.filterNot { item ->
                    item.toothIds.any { it in toothIds }
                }
            } else {
                oldItems + newItem
            },
            undoStack = s.undoStack + newItem,
            showToothMenu = false,
            selectedTooth = null,
            bridgeMode = false,
            bridgeFirstTooth = null,
            bridgeSecondTooth = null
        )
    }

    fun undo() {
        val s = _state.value
        if (s.undoStack.isEmpty()) return
        val last = s.undoStack.last()
        _state.value = s.copy(
            prostheticItems = s.prostheticItems.filterNot { it == last },
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
