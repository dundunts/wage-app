package org.turter.wageapp.messaging.service

import org.springframework.stereotype.Component
import org.turter.wageapp.domain.notification.NotificationEvent
import org.turter.wageapp.domain.shift.Checkpoint

@Component
class NotificationEventTextMessageFactory {

    fun getTextMessage(e: NotificationEvent): String =
        when (e) {
            is NotificationEvent.SessionOpened -> """
                === Сессия открыта ===
                ID сессии: ${e.sessionId}. 
                Дата: ${e.date}.
                Время: ${e.startWorkTime}.
            """.trimIndent()

            is NotificationEvent.CheckpointSaved -> """
                === Чекпоинт сохранен ===
                ${e.checkpoint.getCheckpointDescription()}
            """.trimIndent()

            is NotificationEvent.CheckpointReplaced -> """
                === Чекпоинт замещен ===
                ID замещенного чекпоинта: ${e.replacedCheckpointId}
                
                === Новый чекпоинт. ===
                ${e.checkpoint.getCheckpointDescription()}
            """.trimIndent()

            is NotificationEvent.CheckpointDeleted -> """
                === Чекпоинт удален ===
                ID: ${e.deletedId}
            """.trimIndent()

            is NotificationEvent.ShiftResultCreated -> """
                === Результат смены создан ===
                ID: ${e.shiftResult.id}
                Дата: ${e.shiftResult.date}
                
                Выплаты (работник | % | чай | сумма):
                ${e.shiftResult.payments.map { payment -> 
                    "- ${payment.employee.lastName}: ${payment.percentFromRevenue} | ${payment.tips} | ${payment.tips + payment.percentFromRevenue}" 
                }}
                
            """.trimIndent()
        }

    private fun Checkpoint.getCheckpointDescription(): String = """
                ID: ${id}. 
                Время: ${dateTime}.
                Выручка: ${revenue}.
                Чаевые: ${tips}.
                Работники: 
                ${employees.map { emp -> "- ${emp.firstName} ${emp.lastName[0]}." }}
            """.trimIndent()

}