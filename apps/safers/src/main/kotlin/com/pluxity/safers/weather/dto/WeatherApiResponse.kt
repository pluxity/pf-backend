package com.pluxity.safers.weather.dto

data class WeatherApiResponse(
    val response: Response,
) {
    data class Response(
        val header: Header,
        val body: Body?,
    )

    data class Header(
        val resultCode: String,
        val resultMsg: String,
    )

    data class Body(
        val dataType: String?,
        val items: Items?,
    )

    data class Items(
        val item: List<Item>,
    )

    data class Item(
        val baseDate: String,
        val baseTime: String,
        val category: String,
        val fcstDate: String? = null,
        val fcstTime: String? = null,
        val fcstValue: String? = null,
        val nx: Int,
        val ny: Int,
        val obsrValue: String? = null,
    )
}
