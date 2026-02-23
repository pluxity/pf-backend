package com.pluxity.safers

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.pluxity"])
class SafersApplication

fun main(args: Array<String>) {
    runApplication<SafersApplication>(*args)
}
