package com.wutsi.platform.notification.config

import com.wutsi.platform.core.security.TokenProvider
import com.wutsi.platform.core.security.spring.AbstractTokenConfiguration
import com.wutsi.platform.notification.service.ApplicationProvider
import com.wutsi.platform.notification.service.ApplicationTokenProvider
import com.wutsi.platform.security.WutsiSecurityApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationTokenConfiguration(
    private val securityApi: WutsiSecurityApi,
    @Value("\${wutsi.platform.security.api-key}") private val apiKey: String
) : AbstractTokenConfiguration() {
    @Bean
    override fun tokenProvider(): TokenProvider =
        ApplicationTokenProvider(securityApi, applicationProvider())

    @Bean
    fun applicationProvider(): ApplicationProvider =
        ApplicationProvider(securityApi, apiKey)
}
