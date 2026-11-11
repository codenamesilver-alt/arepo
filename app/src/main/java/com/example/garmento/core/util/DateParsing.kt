package com.example.garmento.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

object DateParsing {
    fun parseLocalDateTime(value: String?): LocalDateTime {
        if (value.isNullOrBlank()) return LocalDateTime.now()
        return try {
            OffsetDateTime.parse(value).toLocalDateTime()
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(value)
            } catch (_: Exception) {
                try {
                    Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime()
                } catch (_: Exception) {
                    LocalDateTime.now()
                }
            }
        }
    }

    fun parseLocalDate(value: String?): LocalDate {
        if (value.isNullOrBlank()) return LocalDate.now()
        return try {
            OffsetDateTime.parse(value).toLocalDate()
        } catch (_: Exception) {
            try {
                LocalDate.parse(value)
            } catch (_: Exception) {
                try {
                    LocalDateTime.parse(value).toLocalDate()
                } catch (_: Exception) {
                    LocalDate.now()
                }
            }
        }
    }
}