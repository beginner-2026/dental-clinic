package com.dental.model

data class Position(
    val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0
)

data class PositionSelection(
    val position: Position,
    val toothNumbers: String = ""
)

data class Visit(
    val id: Long = 0,
    val patientId: Long,
    val visitDate: Long,
    val createdAt: Long = 0,
    val positions: List<PositionSelection> = emptyList()
)
