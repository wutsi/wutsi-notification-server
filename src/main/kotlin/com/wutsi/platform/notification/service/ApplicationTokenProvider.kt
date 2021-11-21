package com.wutsi.platform.notification.service

import com.wutsi.platform.core.security.TokenProvider
import com.wutsi.platform.security.WutsiSecurityApi
import com.wutsi.platform.security.dto.AuthenticationRequest
import org.slf4j.LoggerFactory

class ApplicationTokenProvider(
    private val securityApi: WutsiSecurityApi,
    private val applicationProvider: ApplicationProvider
) : TokenProvider {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(ApplicationTokenProvider::class.java)
    }

    private var accessToken: String? = null

    override fun getToken(): String? {
        if (accessToken == null)
            login()
        return accessToken
    }

    private fun login() {
        LOGGER.info("Authenticating....")
        accessToken = securityApi.authenticate(
            AuthenticationRequest(
                type = "application",
                apiKey = applicationProvider.get().apiKey
            )
        ).accessToken
    }
}
