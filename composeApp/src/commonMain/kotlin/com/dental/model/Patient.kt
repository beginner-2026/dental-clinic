package com.dental.model

data class Patient(
    val id: Long = 0,
    val lastName: String,
    val firstName: String,
    val middleName: String? = null,
    val birthDate: Long? = null,
    val phone: String? = null,
    val email: String? = null,
    val sex: Sex = Sex.MALE,
    val notes: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

enum class Sex { MALE, FEMALE }
