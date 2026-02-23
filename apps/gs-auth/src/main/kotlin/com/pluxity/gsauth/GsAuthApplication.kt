package com.pluxity.gsauth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.pluxity"])
class GsAuthApplication

fun main(args: Array<String>) {
    runApplication<GsAuthApplication>(*args)
}
