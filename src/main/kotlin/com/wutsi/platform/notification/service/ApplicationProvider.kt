package com.wutsi.platform.notification.service

import com.wutsi.platform.security.WutsiSecurityApi
import com.wutsi.platform.security.dto.Application
import org.slf4j.LoggerFactory

class ApplicationProvider(
    private val securityApi: WutsiSecurityApi,
    private val apiKey: String
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(ApplicationProvider::class.java)
    }

    private var app: Application? = null

    fun get(): Application {
        if (app == null)
            fetch()

        return app!!
    }

    private fun fetch() {
        LOGGER.info("Loading application...")
        app = securityApi.application(apiKey).application
    }
}
