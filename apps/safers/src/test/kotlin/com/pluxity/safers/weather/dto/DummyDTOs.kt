package com.pluxity.safers.weather.dto

fun dummyForecastApiResponse(): WeatherApiResponse =
    WeatherApiResponse(
        response =
            WeatherApiResponse.Response(
                header = WeatherApiResponse.Header(resultCode = "00", resultMsg = "NORMAL_SERVICE"),
                body =
                    WeatherApiResponse.Body(
                        dataType = "JSON",
                        items =
                            WeatherApiResponse.Items(
                                item =
                                    listOf(
                                        WeatherApiResponse.Item(
                                            baseDate = "20260306",
                                            baseTime = "1200",
                                            category = "T1H",
                                            fcstDate = "20260306",
                                            fcstTime = "1300",
                                            fcstValue = "15.0",
                                            nx = 55,
                                            ny = 127,
                                        ),
                                    ),
                            ),
                    ),
            ),
    )

fun dummyObservationApiResponse(): WeatherApiResponse =
    WeatherApiResponse(
        response =
            WeatherApiResponse.Response(
                header = WeatherApiResponse.Header(resultCode = "00", resultMsg = "NORMAL_SERVICE"),
                body =
                    WeatherApiResponse.Body(
                        dataType = "JSON",
                        items =
                            WeatherApiResponse.Items(
                                item =
                                    listOf(
                                        WeatherApiResponse.Item(
                                            baseDate = "20260306",
                                            baseTime = "1200",
                                            category = "T1H",
                                            nx = 55,
                                            ny = 127,
                                            obsrValue = "14.5",
                                        ),
                                    ),
                            ),
                    ),
            ),
    )

fun dummyErrorApiResponse(): WeatherApiResponse =
    WeatherApiResponse(
        response =
            WeatherApiResponse.Response(
                header = WeatherApiResponse.Header(resultCode = "03", resultMsg = "NO_DATA"),
                body = null,
            ),
    )
