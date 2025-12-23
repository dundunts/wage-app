package org.turter.wageapp.domain.shared

class EntityNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class NotUniqueValue(message: String) : RuntimeException(message)