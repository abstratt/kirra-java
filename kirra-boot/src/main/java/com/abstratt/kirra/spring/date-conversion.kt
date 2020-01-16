package com.abstratt.kirra.spring

import java.sql.Date
import java.sql.Time
import java.time.LocalDate
import java.time.LocalTime
import javax.persistence.AttributeConverter
import javax.persistence.Converter


@Converter(autoApply = true)
class LocalDateConverter : AttributeConverter<LocalDate, Date> {
    override fun convertToDatabaseColumn(entityValue: LocalDate?): Date? =
            if (entityValue == null) null else Date.valueOf(entityValue)

    override fun convertToEntityAttribute(databaseValue: Date?): LocalDate? =
            databaseValue?.toLocalDate()
}

@Converter(autoApply = true)
class LocalTimeConverter : AttributeConverter<LocalTime, Time> {
    override fun convertToDatabaseColumn(entityValue: LocalTime?) = entityValue?.let { Time.valueOf(it) }
    override fun convertToEntityAttribute(databaseValue: java.sql.Time?) = databaseValue?.toLocalTime()
}