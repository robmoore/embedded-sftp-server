package org.sdf.rkm

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.integration.annotation.IntegrationComponentScan

@SpringBootApplication
@IntegrationComponentScan
class Application

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}

