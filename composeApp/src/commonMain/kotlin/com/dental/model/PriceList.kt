package com.dental.model

data class PriceListItem(
    val id: Int,
    val name: String,
    val code: String,
    val category: String,
    val defaultPrice: Long
)

object PriceList {
    val items: List<PriceListItem> = listOf(
        PriceListItem(1, "Initial Consultation", "CONS_INIT", "Consultation", 5000_00),
        PriceListItem(2, "Follow-up Consultation", "CONS_FU", "Consultation", 3000_00),
        PriceListItem(3, "X-Ray Panoramic", "XRAY_PANO", "X-Ray", 2000_00),
        PriceListItem(4, "X-Ray Periapical", "XRAY_PA", "X-Ray", 800_00),
        PriceListItem(5, "X-Ray CBCT", "XRAY_CBCT", "X-Ray", 5000_00),
        PriceListItem(6, "Professional Cleaning", "CLEAN", "Hygiene", 3000_00),
        PriceListItem(7, "Scaling", "SCALE", "Hygiene", 2500_00),
        PriceListItem(8, "Whitening", "WHITE", "Cosmetic", 15000_00),
        PriceListItem(9, "Filling - Composite", "FILL_COMP", "Therapy", 4000_00),
        PriceListItem(10, "Filling - Light-cured", "FILL_LIGHT", "Therapy", 5000_00),
        PriceListItem(11, "Root Canal Treatment", "RCT", "Endodontics", 12000_00),
        PriceListItem(12, "Metal-Ceramic Crown", "CROWN_MC", "Orthopedics", 18000_00),
        PriceListItem(13, "Zirconium Crown", "CROWN_ZR", "Orthopedics", 25000_00),
        PriceListItem(14, "Metal Crown", "CROWN_M", "Orthopedics", 10000_00),
        PriceListItem(15, "Temporary Crown", "CROWN_TEMP", "Orthopedics", 3000_00),
        PriceListItem(16, "Bridge (per unit)", "BRIDGE", "Orthopedics", 20000_00),
        PriceListItem(17, "Dental Implant", "IMPLANT", "Surgery", 35000_00),
        PriceListItem(18, "Tooth Extraction", "EXTRACT", "Surgery", 3000_00),
        PriceListItem(19, "Wisdom Tooth Extraction", "EXTRACT_WIS", "Surgery", 7000_00),
        PriceListItem(20, "Denture - Complete", "DENT_FULL", "Orthopedics", 40000_00),
        PriceListItem(21, "Denture - Partial", "DENT_PART", "Orthopedics", 25000_00),
        PriceListItem(22, "Anesthesia (local)", "ANESTH", "Medication", 500_00),
        PriceListItem(23, "Sedation", "SEDATION", "Medication", 5000_00),
        PriceListItem(24, "Antibiotic Prescription", "ANTIBIO", "Medication", 300_00),
        PriceListItem(25, "Night Guard / Splint", "SPLINT", "Orthopedics", 8000_00),
        PriceListItem(26, "Gum Flap Surgery", "GUM_FLAP", "Surgery", 10000_00),
        PriceListItem(27, "Bone Graft", "BONE_GRAFT", "Surgery", 15000_00),
        PriceListItem(28, "Sinus Lift", "SINUS_LIFT", "Surgery", 20000_00),
        PriceListItem(29, "Teeth Whitening (home kit)", "WHITE_HOME", "Cosmetic", 8000_00),
        PriceListItem(30, "Veneer - Composite", "VEN_COMP", "Cosmetic", 8000_00),
        PriceListItem(31, "Veneer - Ceramic", "VEN_CERA", "Cosmetic", 20000_00),
        PriceListItem(32, "Orthodontic Retainer", "RETAIN", "Orthodontics", 6000_00)
    )

    fun search(query: String): List<PriceListItem> {
        if (query.isBlank()) return items
        return items.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.code.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true)
        }
    }

    fun getByCategory(): Map<String, List<PriceListItem>> {
        return items.groupBy { it.category }
    }
}
