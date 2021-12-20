package com.wutsi.platform.notification.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.platform.core.security.TokenProvider
import com.wutsi.platform.core.security.feign.FeignApiKeyRequestInterceptor
import com.wutsi.platform.core.security.feign.FeignAuthorizationRequestInterceptor
import com.wutsi.platform.core.stream.EventStream
import com.wutsi.platform.core.tracing.feign.FeignTracingRequestInterceptor
import com.wutsi.platform.payment.Environment.PRODUCTION
import com.wutsi.platform.payment.Environment.SANDBOX
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.WutsiPaymentApiBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles

@Configuration
public class PaymentApiConfiguration(
    private val tokenProvider: TokenProvider,
    private val tracingRequestInterceptor: FeignTracingRequestInterceptor,
    private val apiKeyRequestInterceptor: FeignApiKeyRequestInterceptor,
    private val mapper: ObjectMapper,
    private val env: Environment,
    private val eventStream: EventStream
) {
    @Bean
    fun paymentApi(): WutsiPaymentApi =
        WutsiPaymentApiBuilder().build(
            env = environment(),
            mapper = mapper,
            interceptors = listOf(
                apiKeyRequestInterceptor,
                tracingRequestInterceptor,
                FeignAuthorizationRequestInterceptor(tokenProvider)
            )
        )

    private fun environment(): com.wutsi.platform.payment.Environment =
        if (env.acceptsProfiles(Profiles.of("prod")))
            PRODUCTION
        else
            SANDBOX
}
