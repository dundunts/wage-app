package org.turter.wageapp.domain.shared

class EntityNotFoundException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class NotUniqueValueException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

open class ConflictDataException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

open class NotConsistenceDataException(message: String) : RuntimeException(message)
