// Copyright 2023 Alliander N.V.

package org.gxf.soapbridge.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfiguration {
    @Bean
    @Throws(Exception::class)
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.authorizeHttpRequests {
            it
                .anyRequest().authenticated()
        }
        http.x509 {
            it
                .subjectPrincipalRegex("CN=(.*?)(?:,|$)")
                .userDetailsService(userDetailsService())
        }
        http.csrf { it.disable() }
        return http.build()
    }

    /**
     * Uses the CN of the client certificate as the username for Springs Principal object
     */
    @Bean
    fun userDetailsService(): UserDetailsService {
        return UserDetailsService { username ->
            return@UserDetailsService User(
                username, "", emptyList()
            )
        }
    }
}
