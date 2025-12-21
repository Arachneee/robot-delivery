package com.robotdelivery.config

import org.h2.server.web.JakartaWebServlet
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class H2ConsoleConfig {
    @Bean
    fun h2ServletRegistration(): ServletRegistrationBean<JakartaWebServlet> {
        val registrationBean = ServletRegistrationBean(JakartaWebServlet())
        registrationBean.addUrlMappings("/h2-console/*")
        return registrationBean
    }
}
