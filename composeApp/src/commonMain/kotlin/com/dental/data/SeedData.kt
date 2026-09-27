package com.dental.data

import com.dental.data.db.DentalDatabase
import com.dental.model.*
import kotlinx.datetime.*

object SeedData {

    val POSITION_NAMES = listOf(
        "Консультация",
        "КЛКТ",
        "ОПТГ",
        "Прицельный снимок",
        "Изготовление КВ",
        "Фиксация КВ",
        "Извлечение КВ",
        "Препарирование зуба",
        "Ретракция",
        "Оттиск силиконовый",
        "Оттиск альгинатный",
        "Оттиск функциональный",
        "Примерка каркаса",
        "Примерка с массой",
        "Временная фиксация",
        "Постоянная фиксация СИЦ",
        "Постоянная фиксация ЦФЦ",
        "Снятие коронки",
        "Изготовление временной коронки",
        "Определение ЦСЧ",
        "Постановка",
        "Сдача протеза",
        "Перебазировка протеза",
        "Коррекция протеза",
        "Профессиональная чистка зубов",
        "Нанесение герметика",
        "Коррекция десневого края",
        "Лечение перикоронита",
        "Наложение гемостатической губки",
        "Удаление зуба",
        "Оплата"
    )

    fun seedIfEmpty(database: DentalDatabase) {
        val patientQueries = database.patientQueries
        val appointmentQueries = database.appointmentQueries
        val positionQueries = database.visitPositionQueries

        if (patientQueries.getAll().executeAsList().isNotEmpty()) return

        POSITION_NAMES.forEachIndexed { index, name ->
            positionQueries.insertPosition(name = name, sortOrder = index.toLong())
        }

        val priceListQueries = database.priceListQueries
        val PRICE_LIST_ITEMS = listOf(
            Triple("Консультация и диагностика", "Прием (осмотр, консультация) врача стоматолога ортопеда первичный", 1500_00),
            Triple("Консультация и диагностика", "Прицельная внутриротовая контактная рентгенография", 500_00),
            Triple("Консультация и диагностика", "Панорамный снимок (ОПТГ)", 2000_00),
            Triple("Консультация и диагностика", "КЛКТ (3D-исследование) одной челюсти", 3000_00),
            Triple("Консультация и диагностика", "КЛКТ (3D-исследование) двух челюстей", 5000_00),
            Triple("Консультация и диагностика", "КЛКТ (3D-исследование) челюстно-лицевой области", 2000_00),
            Triple("Анестезия", "Аппликационная анестезия", 180_00),
            Triple("Анестезия", "Инфильтрационная анестезия", 700_00),
            Triple("Анестезия", "Проводниковая анестезия", 1200_00),
            Triple("Анестезия", "Интралигаментарная анестезия", 1000_00),
            Triple("Ортопедия. Несъёмное протезирование", "Культевая вкладка цельнолитая (изготовление и фиксация)", 7500_00),
            Triple("Ортопедия. Несъёмное протезирование", "Восстановление зуба металлокерамической коронкой и культевой вкладкой", 25000_00),
            Triple("Ортопедия. Несъёмное протезирование", "Восстановление зуба металлокерамической коронкой Ni", 17000_00),
            Triple("Ортопедия. Несъёмное протезирование", "Восстановление зуба металлокерамической коронкой Co", 22000_00),
            Triple("Ортопедия. Несъёмное протезирование", "Восстановление зуба цельнолитой металлической коронкой", 9000_00),
            Triple("Ортопедия. Несъёмное протезирование", "Восстановление зуба коронкой на основе диоксида циркония", 30000_00),
            Triple("Ортопедия. Несъёмное протезирование", "Восстановление зуба цельнокерамической коронкой", 30000_00),
            Triple("Ортопедия. Несъёмное протезирование", "Временная коронка из пластмассы", 3600_00),
            Triple("Ортопедия. Несъёмное протезирование", "Коронка металлическая штампованная", 7000_00),
            Triple("Ортопедия. Съёмное протезирование", "Частичный съёмный акриловый пластиночный протез", 32000_00),
            Triple("Ортопедия. Съёмное протезирование", "Полный съёмный акриловый пластиночный протез", 35000_00),
            Triple("Ортопедия. Съёмное протезирование", "Частичный съёмный бюгельный протез", 55000_00),
            Triple("Ортопедия. Съёмное протезирование", "Частичный съёмный термопластический протез", 50000_00),
            Triple("Ортопедия. Съёмное протезирование", "Иммедиат термопластический протез", 20000_00),
            Triple("Ортопедия. Съёмное протезирование", "Иммедиат акриловый протез", 15000_00),
            Triple("Терапия", "Реставрация композитная", 3000_00),
            Triple("Терапия", "Профессиональная чистка зубов (за челюсть)", 3000_00),
            Triple("Терапия", "Пародонтальная повязка", 600_00),
            Triple("Терапия", "Аппликация десенсибилизирующего геля", 400_00),
            Triple("Хирургия", "Удаление зуба", 4000_00),
            Triple("Хирургия", "Коррекция десны", 1200_00),
            Triple("Хирургия", "Разрез", 1000_00),
            Triple("Пользовательские услуги", "Снятие оттиска альгинатного с одной челюсти", 800_00),
            Triple("Пользовательские услуги", "Снятие оттиска силиконового с одной челюсти", 1500_00),
            Triple("Пользовательские услуги", "Определение прикуса (центральной окклюзии)", 900_00),
            Triple("Пользовательские услуги", "Культевая вкладка, изготовление", 6000_00),
            Triple("Пользовательские услуги", "Культевая вкладка, фиксация", 800_00),
            Triple("Пользовательские услуги", "Коронка цельнокерамическая или циркониевая", 25000_00),
            Triple("Пользовательские услуги", "Коронка металлокерамическая Ni", 12000_00),
            Triple("Пользовательские услуги", "Коронка металлокерамическая Co", 17000_00),
            Triple("Пользовательские услуги", "Коронка цельнолитая", 5200_00),
            Triple("Пользовательские услуги", "Коронка штампованная", 4500_00),
            Triple("Пользовательские услуги", "Коронка временная пластмассовая", 3000_00),
            Triple("Пользовательские услуги", "Фиксация коронки на постоянный цемент", 800_00),
            Triple("Пользовательские услуги", "Снятие коронки", 800_00),
            Triple("Пользовательские услуги", "Ретракция десны", 500_00),
            Triple("Пользовательские услуги", "Фиксация коронки на временный цемент", 500_00),
            Triple("Пользовательские услуги", "Перебазировка протеза", 6000_00),
            Triple("Пользовательские услуги", "Починка протеза", 2000_00),
        )
        PRICE_LIST_ITEMS.forEachIndexed { index, (category, name, price) ->
            priceListQueries.insert(
                category = category,
                name = name,
                defaultPrice = price.toLong(),
                sortOrder = index.toLong()
            )
        }

        val now = Clock.System.now().toEpochMilliseconds()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val patients = listOf(
            Triple("Иванов", "Иван", "Иванович"),
            Triple("Петрова", "Мария", "Сергеевна"),
            Triple("Сидоров", "Алексей", "Владимирович"),
            Triple("Кузнецова", "Ольга", "Дмитриевна"),
            Triple("Смирнов", "Дмитрий", "Анатольевич")
        )

        val patientIds = patients.map { (last, first, middle) ->
            patientQueries.insert(
                lastName = last,
                firstName = first,
                middleName = middle,
                birthDate = now - (30L * 365 * 24 * 60 * 60 * 1000),
                phone = "+7 (999) 123-45-67",
                email = null,
                sex = if (last.endsWith("а") || last.endsWith("я")) Sex.FEMALE.name else Sex.MALE.name,
                notes = null,
                createdAt = now,
                updatedAt = now
            )
            patientQueries.getLastInsertId().executeAsOne()
        }

        fun dateToEpochMillis(date: LocalDate, hour: Int, minute: Int): Long {
            val time = LocalTime(hour, minute.coerceIn(0, 59))
            val dateTime = LocalDateTime(date, time)
            return dateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        }

        data class AppointmentSeed(val patientIndex: Int, val hour: Int, val minute: Int, val type: AppointmentType)

        val appointments = listOf(
            AppointmentSeed(0, 9, 0, AppointmentType.INITIAL),
            AppointmentSeed(1, 10, 30, AppointmentType.FOLLOW_UP),
            AppointmentSeed(2, 14, 0, AppointmentType.EMERGENCY),
            AppointmentSeed(3, 11, 0, AppointmentType.FOLLOW_UP),
            AppointmentSeed(4, 15, 30, AppointmentType.INITIAL),
            AppointmentSeed(0, 16, 0, AppointmentType.FOLLOW_UP),
            AppointmentSeed(2, 8, 30, AppointmentType.INITIAL)
        )

        appointments.forEach { seed ->
            val start = dateToEpochMillis(today, seed.hour, seed.minute)
            val endMinute = seed.minute + 30
            val endHour = seed.hour + endMinute / 60
            val end = dateToEpochMillis(today, endHour, endMinute % 60)
            appointmentQueries.insert(
                patientId = patientIds[seed.patientIndex],
                startTime = start,
                endTime = end,
                durationMinutes = 30L,
                doctorId = null,
                chairId = null,
                type = seed.type.name,
                status = AppointmentStatus.PLANNED.name,
                note = null
            )
        }
    }
}
