package com.dental.model

data class TreatmentPlanItem(
    val id: Long = 0,
    val patientId: Long,
    val toothNumbers: String,
    val procedure: String,
    val createdAt: Long = 0
)

data class PredefinedProcedure(
    val text: String
)

val PREDEFINED_PROCEDURES = listOf(
    PredefinedProcedure("Изготовление МК коронки"),
    PredefinedProcedure("Изготовление ЦЛ коронки"),
    PredefinedProcedure("Изготовление циркониевой коронки"),
    PredefinedProcedure("Изготовление керамической коронки"),
    PredefinedProcedure("Изготовление временной коронки"),
    PredefinedProcedure("Снятие коронки"),
    PredefinedProcedure("Изготовление культевой вкладки"),
    PredefinedProcedure("Извлечение культевой вкладки"),
    PredefinedProcedure("Изготовление бюгельного протеза на ВЧ"),
    PredefinedProcedure("Изготовление бюгельного протеза на НЧ"),
    PredefinedProcedure("Изготовление ЧСПП на ВЧ"),
    PredefinedProcedure("Изготовление ЧСПП на НЧ"),
    PredefinedProcedure("Изготовление ПСПП на ВЧ"),
    PredefinedProcedure("Изготовление ПСПП на НЧ"),
    PredefinedProcedure("Повторное эндодонтическое лечение"),
    PredefinedProcedure("Депульпирование зуба"),
    PredefinedProcedure("Удаление зуба")
)
