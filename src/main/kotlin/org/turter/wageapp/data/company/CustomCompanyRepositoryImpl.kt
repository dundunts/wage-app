package org.turter.wageapp.data.company

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import org.turter.wageapp.domain.company.Company
import reactor.core.publisher.Flux
import java.util.UUID

@Repository
class CustomCompanyRepositoryImpl(private val dbClient: DatabaseClient) : CustomCompanyRepository {

    override fun findAllForUserId(userId: UUID): Flux<Company> {
        val sql = """
            with company_ids as (select uc.company_id from wage_app.user_companies uc where uc.user_id = :userId)
            select *
            from wage_app.companies c
            where c.id in (select * from company_ids)
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