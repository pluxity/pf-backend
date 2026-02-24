package com.pluxity.safers.weather.entity

enum class WeatherCategory(
    val description: String,
) {
    T1H("기온"),
    RN1("1시간 강수량"),
    SKY("하늘상태"),
    UUU("동서바람성분"),
    VVV("남북바람성분"),
    REH("습도"),
    PTY("강수형태"),
    LGT("낙뢰"),
    VEC("풍향"),
    WSD("풍속"),
    ;

    companion object {
        private val NAME_MAP = entries.associateBy { it.name }

        fun fromName(name: String): WeatherCategory? = NAME_MAP[name]
    }
}
