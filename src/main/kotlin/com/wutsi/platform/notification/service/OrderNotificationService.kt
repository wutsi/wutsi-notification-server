package com.wutsi.platform.notification.service

import com.wutsi.ecommerce.order.WutsiOrderApi
import com.wutsi.ecommerce.order.dto.Order
import com.wutsi.ecommerce.shipping.WutsiShippingApi
import com.wutsi.ecommerce.shipping.entity.ShippingType
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.sms.WutsiSmsApi
import com.wutsi.platform.sms.dto.SendMessageRequest
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import java.util.Locale

@Service
class OrderNotificationService(
    private val accountApi: WutsiAccountApi,
    private val smsApi: WutsiSmsApi,
    private val messages: MessageSource,
    private val orderApi: WutsiOrderApi,
    private val shippingApi: WutsiShippingApi
) {
    fun onOrderOpened(orderId: String, tenant: Tenant): String {
        val order = orderApi.getOrder(orderId).order
        val merchant = accountApi.getAccount(order.merchantId).account
        val formatter = DecimalFormat(tenant.monetaryFormat)

        return smsApi.sendMessage(
            SendMessageRequest(
                message = getText(
                    key = "sms.order-opened",
                    args = arrayOf(shortOrderId(order), formatter.format(order.totalPrice)),
                    locale = Locale(merchant.language)
                ),
                phoneNumber = merchant.phone!!.number
            )
        ).id
    }

    fun onOrderCancelled(orderId: String, tenant: Tenant): String {
        val order = orderApi.getOrder(orderId).order
        val merchant = accountApi.getAccount(order.merchantId).account

        return smsApi.sendMessage(
            SendMessageRequest(
                message = getText(
                    key = "sms.order-cancelled",
                    args = arrayOf(shortOrderId(order)),
                    locale = Locale(merchant.language)
                ),
                phoneNumber = merchant.phone!!.number
            )
        ).id
    }

    /**
     * Send notification for in-store pickup
     */
    fun onOrderReadyForPickup(orderId: String, tenant: Tenant): String? {
        val order = orderApi.getOrder(orderId).order
        val shipping = order.shippingId?.let { shippingApi.getShipping(it).shipping }

        if (shipping?.type == ShippingType.IN_STORE_PICKUP.name) {
            val customer = accountApi.getAccount(order.accountId).account
            return smsApi.sendMessage(
                SendMessageRequest(
                    message = getText(
                        key = "sms.order-ready-for-pickup-in-store",
                        args = arrayOf(shortOrderId(order)),
                        locale = Locale(customer.language)
                    ),
                    phoneNumber = customer.phone!!.number
                )
            ).id
        }
        return null
    }

    private fun shortOrderId(order: Order): String =
        order.id.uppercase().takeLast(4)

    private fun getText(key: String, args: Array<Any?> = emptyArray(), locale: Locale) =
        messages.getMessage(key, args, locale) ?: key
}
