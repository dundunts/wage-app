package org.turter.wageapp.domain.shift

import org.turter.wageapp.domain.shared.ConflictDataException
import org.turter.wageapp.domain.shared.NotConsistenceDataException

class WrongCompanyIdException(message: String) : ConflictDataException(message)

class ShiftSessionNotDraftException(message: String) : ConflictDataException(message)

class ShiftResultConflictException(message: String, cause: Throwable? = null) : ConflictDataException(message, cause)

class ShiftSessionClosedException(message: String) : ConflictDataException(message)

class ShiftSessionNotClosedException(message: String) : ConflictDataException(message)

class ShiftSessionNotAvailableForModifyException(message: String) : ConflictDataException(message)

class SeveralSessionsOpenedException(message: String) : NotConsistenceDataException(message)
