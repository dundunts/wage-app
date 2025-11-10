package org.turter.wageapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WageAppApplication

fun main(args: Array<String>) {
    runApplication<WageAppApplication>(*args)
}
