package com.dental.model

data class Diagnosis(
    val id: Long = 0,
    val patientId: Long,
    val code: String? = null,
    val diagnosisText: String,
    val toothNumber: Long? = null,
    val createdAt: Long = 0
)

data class PredefinedDiagnosis(
    val code: String?,
    val text: String
)

val PREDEFINED_DIAGNOSES = listOf(
    PredefinedDiagnosis("К08.1", "Потеря зубов вследствие несчастного случая, удаления или локальной периодонтальной болезни"),
    PredefinedDiagnosis(null, "Состояние после эндодонтического лечения"),
    PredefinedDiagnosis("K03.1", "Клиновидный дефект"),
    PredefinedDiagnosis("K02.0", "Кариес эмали"),
    PredefinedDiagnosis("К02.1", "Кариес дентина"),
    PredefinedDiagnosis("К02.2", "Кариес корня"),
    PredefinedDiagnosis("S02.52", "Перелом коронки зуба с повреждением пульпы"),
    PredefinedDiagnosis("S02.53", "Фрактура (перелом) корня зуба"),
    PredefinedDiagnosis("K04.0", "Пульпит"),
    PredefinedDiagnosis("К04.4", "Острый апикальный периодонтит пульпарного происхождения"),
    PredefinedDiagnosis("К04.5", "Хронический апикальный периодонтит"),
    PredefinedDiagnosis("К04.8", "Корневая киста"),
    PredefinedDiagnosis("К05.3", "Хронический пародонтит"),
    PredefinedDiagnosis("К05.22", "Острый перикоронит"),
    PredefinedDiagnosis("К01.0", "Ретенированные зубы")
)
