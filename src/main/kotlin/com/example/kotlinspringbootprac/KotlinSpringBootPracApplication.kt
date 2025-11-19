package com.example.kotlinspringbootprac

import io.github.cdimascio.dotenv.Dotenv
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class KotlinSpringBootPracApplication

fun main(args: Array<String>) {
    // Load .env file before Spring Boot starts
    try {
        val dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load()

        val logger = LoggerFactory.getLogger(KotlinSpringBootPracApplication::class.java)
        dotenv.entries().forEach { entry ->
            System.setProperty(entry.key, entry.value)
            logger.debug("Loaded environment variable: ${entry.key}")
        }
        logger.info("Loaded .env file successfully")
    } catch (e: Exception) {
        // Ignore if .env file doesn't exist
    }

    SpringApplication.run(KotlinSpringBootPracApplication::class.java, *args)
}
