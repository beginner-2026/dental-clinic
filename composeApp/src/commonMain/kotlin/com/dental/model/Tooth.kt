package com.dental.model

data class Tooth(
    val id: Long = 0,
    val patientId: Long,
    val number: Int,
    val arch: Arch,
    val quadrant: Int,
    val status: ToothStatus = ToothStatus.PRESENT
)

enum class Arch { UPPER, LOWER }

enum class ToothStatus { PRESENT, MISSING, IMPLANT }

data class ProstheticItem(
    val id: Long = 0,
    val patientId: Long,
    val toothIds: List<Int>,
    val type: ProstheticType,
    val material: ProstheticMaterial,
    val stage: ProstheticStage,
    val treatmentId: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

enum class ProstheticType { CROWN, BRIDGE, IMPLANT, TEMPORARY, REMOVAL, POST_CORE, PONTIC }

enum class ProstheticMaterial { METAL_CERAMIC, ZIRCONIUM, METAL, COMPOSITE, CERAMIC }

enum class ProstheticStage { EXISTING, PLANNED, IN_PROGRESS, COMPLETED }
