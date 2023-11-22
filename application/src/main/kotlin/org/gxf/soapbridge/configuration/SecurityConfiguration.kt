// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

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
    fun filterChain(http: HttpSecurity): SecurityFilterChain =
        http.authorizeHttpRequests {
            it
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
        }.x509 {
            it
                .subjectPrincipalRegex("CN=(.*?)(?:,|$)")
                .userDetailsService(userDetailsService())
        }.csrf { it.disable() }
            .build()


    /**
     * Uses the CN of the client certificate as the username for Springs Principal object
     */
    @Bean
    fun userDetailsService(): UserDetailsService =
        UserDetailsService { username ->
            return@UserDetailsService User(
                username, "", emptyList()
            )
        }
}
