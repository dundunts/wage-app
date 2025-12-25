package org.turter.wageapp.domain.shared

class EntityNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class NotUniqueValueException(message: String) : RuntimeException(message)