package com.example.homeshieldai

data class Distribuidora(
    val nombre: String,
    val tarifaRef: Float? = null,   // S/ por kWh, sin IGV
    val cargoFijoRef: Float? = null // S/ por mes, sin IGV
)

const val REFERENCIA_NOTA = "Valor de referencia (Osinergmin / SEAL, jul 2026). Confírmalo con tu recibo."

val departamentos: Map<String, List<Distribuidora>> = mapOf(
    "Amazonas" to listOf(Distribuidora("Electro Oriente")),
    "Áncash" to listOf(Distribuidora("Hidrandina")),
    "Apurímac" to listOf(Distribuidora("Electro Sur Este")),
    "Arequipa" to listOf(Distribuidora("SEAL", 0.466f, 3.51f)),
    "Ayacucho" to listOf(Distribuidora("Electrocentro")),
    "Cajamarca" to listOf(Distribuidora("Hidrandina")),
    "Callao" to listOf(Distribuidora("Pluz Energía"), Distribuidora("Luz del Sur")),
    "Cusco" to listOf(Distribuidora("Electro Sur Este")),
    "Huancavelica" to listOf(Distribuidora("Electrocentro")),
    "Huánuco" to listOf(Distribuidora("Electrocentro")),
    "Ica" to listOf(Distribuidora("Electro Dunas")),
    "Junín" to listOf(Distribuidora("Electrocentro")),
    "La Libertad" to listOf(Distribuidora("Hidrandina")),
    "Lambayeque" to listOf(Distribuidora("Electronorte")),
    "Lima" to listOf(Distribuidora("Pluz Energía"), Distribuidora("Luz del Sur")),
    "Loreto" to listOf(Distribuidora("Electro Oriente")),
    "Madre de Dios" to listOf(Distribuidora("Electro Sur Este")),
    "Moquegua" to listOf(Distribuidora("Electrosur")),
    "Pasco" to listOf(Distribuidora("Electrocentro")),
    "Piura" to listOf(Distribuidora("Enosa")),
    "Puno" to listOf(Distribuidora("Electro Puno")),
    "San Martín" to listOf(Distribuidora("Electro Oriente")),
    "Tacna" to listOf(Distribuidora("Electrosur")),
    "Tumbes" to listOf(Distribuidora("Enosa")),
    "Ucayali" to listOf(Distribuidora("Electro Ucayali"))
)

const val OTRA_DISTRIBUIDORA = "Otra distribuidora"