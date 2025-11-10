package org.turter.wageapp

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/test")
class TestController {

    @GetMapping("/private")
    fun testPrivate(): String = "Hello World for Authenticated only"

    @GetMapping("/public")
    fun testFree(): String = "Hello World for all"

}