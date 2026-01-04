package org.turter.wageapp.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.turter.wageapp.domain.salary.PayrollAggregator

@Configuration
class DomainBeans {

    @Bean
    fun PayrollAggregator(): PayrollAggregator = PayrollAggregator()

}