package org.turter.wageapp.application.data.company

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import org.turter.wageapp.domain.company.Company
import reactor.core.publisher.Flux
import java.util.*

@Repository
class CustomCompanyRepositoryImpl(private val dbClient: DatabaseClient) : CustomCompanyRepository {

    override fun findAllForUserId(userId: String): Flux<Company> {
        val sql = """
            with emp_id as (select e.id from wage_app.employees e where e.user_id = :userId),
                 emp_binds as (select *
                               from wage_app.employees_companies ec
                                        join emp_id on ec.employee_id = emp_id.id)
            select c.id, c.title, c.k_from_revenue, c.default_shift_start_time
            from wage_app.companies c
                     join emp_binds on c.id = emp_binds.company_id
        """.trimIndent()

        return dbClient.sql(sql)
            .bind("userId", userId)
            .map { row, _ ->
                Company(
                    row.get("id", UUID::class.java)!!,
                    row.get("title", String::class.java)!!,
                    row.get("k_from_revenue", Int::class.java)!!,
                    row.get("default_shift_start_time", String::class.java)!!,
                )
            }
            .all()
    }
}