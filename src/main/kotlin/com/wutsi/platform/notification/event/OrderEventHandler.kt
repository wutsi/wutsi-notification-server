package com.wutsi.platform.notification.event

import com.wutsi.ecommerce.order.WutsiOrderApi
import com.wutsi.ecommerce.order.dto.Order
import com.wutsi.ecommerce.shipping.WutsiShippingApi
import com.wutsi.ecommerce.shipping.entity.ShippingType
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.url.UrlShortener
import com.wutsi.platform.sms.WutsiSmsApi
import com.wutsi.platform.sms.dto.SendMessageRequest
import com.wutsi.platform.tenant.WutsiTenantApi
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import java.util.Locale

@Service
class OrderEventHandler(
    private val accountApi: WutsiAccountApi,
    private val smsApi: WutsiSmsApi,
    private val tenantApi: WutsiTenantApi,
    private val orderApi: WutsiOrderApi,
    private val shippingApi: WutsiShippingApi,
    private val messages: MessageSource,
    private val logger: KVLogger,
    private val urlShortener: UrlShortener
) {
    /**
     * Send notification to merchant
     */
    fun onOrderOpened(orderId: String): String {
        val order = orderApi.getOrder(orderId).order
        val merchant = accountApi.getAccount(order.merchantId).account
        val customer = accountApi.getAccount(order.accountId).account
        val tenant = tenantApi.getTenant(order.tenantId).tenant
        logger.add("merchant_id", merchant.id)

        return smsApi.sendMessage(
            SendMessageRequest(
                message = getText(
                    key = "sms.order-opened",
                    args = arrayOf(
                        DecimalFormat(tenant.monetaryFormat).format(order.totalPrice),
                        customer.displayName ?: "",
                        orderUrl(orderId, tenant),
                    ),
                    locale = Locale(merchant.language)
                ),
                phoneNumber = merchant.phone!!.number
            )
        ).id
    }

    fun onOrderCancelled(orderId: String): String {
        val order = orderApi.getOrder(orderId).order
        val tenant = tenantApi.getTenant(order.tenantId).tenant
        val customer = accountApi.getAccount(order.accountId).account
        logger.add("customer_id", customer.id)

        return smsApi.sendMessage(
            SendMessageRequest(
                message = getText(
                    key = "sms.order-cancelled",
                    args = arrayOf(shortOrderId(order), orderUrl(orderId, tenant)),
                    locale = Locale(customer.language)
                ),
                phoneNumber = customer.phone!!.number
            )
        ).id
    }

    /**
     * Send notification for in-store pickup
     */
    fun onOrderReadyForPickup(orderId: String): String? {
        val order = orderApi.getOrder(orderId).order
        val tenant = tenantApi.getTenant(order.tenantId).tenant
        val shipping = order.shippingId?.let { shippingApi.getShipping(it).shipping }
        logger.add("shipping_id", shipping?.id)
        logger.add("shipping_type", shipping?.type)

        if (shipping?.type == ShippingType.IN_STORE_PICKUP.name) {
            val customer = accountApi.getAccount(order.accountId).account
            logger.add("customer_id", customer.id)

            return smsApi.sendMessage(
                SendMessageRequest(
                    message = getText(
                        key = "sms.order-ready-for-pickup-in-store",
                        args = arrayOf(shortOrderId(order), orderUrl(orderId, tenant)),
                        locale = Locale(customer.language)
                    ),
                    phoneNumber = customer.phone!!.number
                )
            ).id
        }
        return null
    }

    private fun orderUrl(orderId: String, tenant: Tenant): String =
        urlShortener.shorten("${tenant.webappUrl}/order?id=$orderId")

    private fun shortOrderId(order: Order): String =
        order.id.uppercase().takeLast(4)

    private fun getText(key: String, args: Array<Any?> = emptyArray(), locale: Locale) =
        messages.getMessage(key, args, locale) ?: key
}
