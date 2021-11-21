package com.wutsi.platform.notification.service

import com.wutsi.platform.tenant.WutsiTenantApi
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.stereotype.Service

@Service
class TenantProvider(
    private val tenantApi: WutsiTenantApi
) {
    private val tenants: MutableMap<Long, Tenant> = mutableMapOf()

    fun get(id: Long): Tenant {
        var tenant = tenants[id]
        if (tenant == null) {
            tenant = tenantApi.getTenant(id).tenant
            tenants[id] = tenant
        }
        return tenant
    }
}
