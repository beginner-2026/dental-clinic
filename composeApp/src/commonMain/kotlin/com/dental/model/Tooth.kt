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

enum class ToothType {
    UPPER_MOLAR,
    LOWER_MOLAR,
    UPPER_PREMOLAR,
    LOWER_PREMOLAR,
    UPPER_ANTERIOR,
    LOWER_ANTERIOR,
    OTHER
}

fun getToothType(number: Int): ToothType {
    return when (number) {
        18, 17, 16, 26, 27, 28 -> ToothType.UPPER_MOLAR
        48, 47, 46, 36, 37, 38 -> ToothType.LOWER_MOLAR
        15, 14, 25, 24 -> ToothType.UPPER_PREMOLAR
        45, 44, 35, 34 -> ToothType.LOWER_PREMOLAR
        13, 12, 11, 21, 22, 23 -> ToothType.UPPER_ANTERIOR
        43, 42, 41, 31, 32, 33 -> ToothType.LOWER_ANTERIOR
        else -> ToothType.OTHER
    }
}

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

enum class ToothPart { CROWN, ROOT }

enum class CrownOption {
    METAL_CERAMIC,
    CAST_SOLID,
    ZIRCONIUM_OXIDE,
    FULL_CERAMIC,
    IMPLANT_CROWN,
    TEMPORARY,
    ARTIFICIAL_MC,
    ARTIFICIAL_CAST,
    ARTIFICIAL_REMOVABLE,
    PLOMBA,
    MISSING
}

enum class RootOption {
    POST_CORE,
    ANCHOR_PIN,
    ENDO_TREATED,
    ENDO_PROBLEM,
    IMPLANT,
    RETAINED,
    MISSING
}
