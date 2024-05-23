package net.intergamma.stock.store.dto

data class SetReservationDto(
    val userId: String,
    val amountToReserve: Long
)