package org.turter.wageapp.data.employee

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.RowsFetchSpec
import org.springframework.stereotype.Repository
import org.turter.wageapp.data.employee.entity.EmployeeWithCompanyRow
import reactor.core.publisher.Flux
import java.util.*

@Repository
class EmployeeCustomRepositoryImpl(
    private val client: DatabaseClient
) : EmployeeCustomRepository {

    override fun findEmployeesByCompanyIds(companyIds: List<UUID>): Flux<EmployeeWithCompanyRow> =
        client.sql(
            """
            select e.*, ec.company_id
            from wage_app.employees e
            join wage_app.employees_companies ec on ec.employee_id = e.id
            where ec.company_id = any(:companyIds)
            """
        )
            .bind("companyIds", companyIds.toTypedArray())
            .toEmployeeWithCompanyRow()
            .all()

    override fun findCoworkersByUserId(userId: String): Flux<EmployeeWithCompanyRow> =
        client.sql(
            """
                with emp_id as (select e.id from wage_app.employees e where e.user_id = :userId),
                     emp_binds as (select *
                                   from wage_app.employees_companies ec
                                            join emp_id on ec.employee_id = emp_id.id),
                     company_binds as (select *
                                       from wage_app.employees_companies
                                       where company_id in (select eb.company_id from emp_binds eb))
                select
                    emps.id,
                    emps.first_name,
                    emps.last_name,
                    emps.patronymic,
                    emps.simple_name,
                    emps.user_id,
                    company_binds.company_id
                from wage_app.employees emps
                         join company_binds on emps.id = company_binds.employee_id;
            """.trimIndent()
        )
            .bind("userId", userId)
            .toEmployeeWithCompanyRow()
            .all()

    private fun DatabaseClient.GenericExecuteSpec.toEmployeeWithCompanyRow(): RowsFetchSpec<EmployeeWithCompanyRow> =
        map { row, _ ->
            EmployeeWithCompanyRow(
                employeeId = row.get("id", UUID::class.java)!!,
                companyId = row.get("company_id", UUID::class.java)!!,
                userId = row.get("user_id", String::class.java),
                firstName = row.get("first_name", String::class.java)!!,
                lastName = row.get("last_name", String::class.java)!!,
                patronymic = row.get("patronymic", String::class.java)!!,
                simpleName = row.get("simple_name", String::class.java)
            )
        }
}

