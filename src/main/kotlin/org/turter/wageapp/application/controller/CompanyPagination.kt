package org.turter.wageapp.application.controller

internal object CompanyPagination {
    const val DEFAULT_PAGE = 0
    const val DEFAULT_PAGE_SIZE = 100
    const val MAX_PAGE_SIZE = 2000

    fun normalizePage(value: String?): Int = value?.toIntOrNull()?.takeIf { it >= 0 } ?: DEFAULT_PAGE

    fun normalizeSize(value: String?): Int =
        value?.toIntOrNull()?.takeIf { it > 0 }?.coerceAtMost(MAX_PAGE_SIZE) ?: DEFAULT_PAGE_SIZE
}
