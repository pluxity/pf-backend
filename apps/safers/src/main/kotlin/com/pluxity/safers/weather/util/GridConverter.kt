package com.pluxity.safers.weather.util

import com.pluxity.safers.weather.dto.GridCoordinate
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

object GridConverter {
    private const val RE = 6371.00877
    private const val GRID = 5.0
    private const val SLAT1 = 30.0
    private const val SLAT2 = 60.0
    private const val OLON = 126.0
    private const val OLAT = 38.0
    private const val XO = 210.0 / GRID
    private const val YO = 675.0 / GRID

    private const val DEGRAD = Math.PI / 180.0

    private val sn: Double
    private val sf: Double
    private val ro: Double

    init {
        val re = RE / GRID
        val slat1Rad = SLAT1 * DEGRAD
        val slat2Rad = SLAT2 * DEGRAD
        val olatRad = OLAT * DEGRAD

        sn = ln(cos(slat1Rad) / cos(slat2Rad)) /
            ln(tan(Math.PI * 0.25 + slat2Rad * 0.5) / tan(Math.PI * 0.25 + slat1Rad * 0.5))
        sf = tan(Math.PI * 0.25 + slat1Rad * 0.5).pow(sn) * cos(slat1Rad) / sn
        ro = re * sf / tan(Math.PI * 0.25 + olatRad * 0.5).pow(sn)
    }

    fun toGrid(
        lon: Double,
        lat: Double,
    ): GridCoordinate {
        val re = RE / GRID
        val ra = re * sf / tan(Math.PI * 0.25 + lat * DEGRAD * 0.5).pow(sn)
        var theta = lon * DEGRAD - OLON * DEGRAD
        if (theta > Math.PI) theta -= 2.0 * Math.PI
        if (theta < -Math.PI) theta += 2.0 * Math.PI
        theta *= sn

        val nx = (ra * sin(theta) + XO + 1.5).toInt()
        val ny = (ro - ra * cos(theta) + YO + 1.5).toInt()

        if (nx !in 1..149 || ny !in 1..253) {
            return GridCoordinate(nx = 0, ny = 0)
        }

        return GridCoordinate(nx = nx, ny = ny)
    }
}
