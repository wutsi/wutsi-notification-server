package com.wutsi.platform.notification.service

import com.wutsi.ecommerce.shipping.WutsiShippingApi
import com.wutsi.ecommerce.shipping.entity.ShippingType
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.sms.WutsiSmsApi
import com.wutsi.platform.sms.dto.SendMessageRequest
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class ShippingNotificationService(
    private val accountApi: WutsiAccountApi,
    private val smsApi: WutsiSmsApi,
    private val messages: MessageSource,
    private val shippingApi: WutsiShippingApi
) {
    /**
     * Send notification for in-store pickup
     */
    fun onShippingReadyForPickup(shippingOrderId: Long, tenant: Tenant): String? {
        val shippingOrder = shippingApi.getShippingOrder(shippingOrderId).shippingOrder
        if (!shippingOrder.shipping.type.equals(ShippingType.IN_STORE_PICKUP.name, true))
            return null

        val customer = accountApi.getAccount(shippingOrder.customerId).account
        return smsApi.sendMessage(
            SendMessageRequest(
                message = getText(
                    key = "sms.shipping-ready-for-pickup-in-store",
                    args = arrayOf(shippingOrder.orderId.uppercase().takeLast(4)),
                    locale = Locale(customer.language)
                ),
                phoneNumber = customer.phone!!.number
            )
        ).id
    }

    private fun getText(key: String, args: Array<Any?> = emptyArray(), locale: Locale) =
        messages.getMessage(key, args, locale) ?: key
}
