package com.wutsi.platform.notification.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.ecommerce.order.WutsiOrderApi
import com.wutsi.ecommerce.order.event.OrderEventPayload
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.stream.Event
import com.wutsi.platform.core.tracing.TracingContext
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.entity.TransactionType
import com.wutsi.platform.payment.event.EventURN
import com.wutsi.platform.payment.event.TransactionEventPayload
import com.wutsi.platform.sms.WutsiSmsApi
import com.wutsi.platform.sms.dto.SendMessageRequest
import com.wutsi.platform.tenant.WutsiTenantApi
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.context.MessageSource
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import java.util.Locale

@Service
class EventHandler(
    private val accountApi: WutsiAccountApi,
    private val smsApi: WutsiSmsApi,
    private val tenantApi: WutsiTenantApi,
    private val objectMapper: ObjectMapper,
    private val messages: MessageSource,
    private val logger: KVLogger,
    private val paymentApi: WutsiPaymentApi,
    private val orderApi: WutsiOrderApi,
    private val tracingContext: TracingContext,
) {
    @EventListener
    fun onEvent(event: Event) {
        if (EventURN.TRANSACTION_SUCCESSFUL.urn == event.type) {
            val payload = objectMapper.readValue(event.payload, TransactionEventPayload::class.java)
            logger.add("transaction_id", payload.transactionId)
            logger.add("transaction_type", payload.type)
            logger.add("order_id", payload.orderId)

            if (payload.type == TransactionType.TRANSFER.name) {
                onTransferSuccessful(payload)
            }
        } else if (com.wutsi.ecommerce.order.event.EventURN.ORDER_READY.urn == event.type) {
            val payload = objectMapper.readValue(event.payload, OrderEventPayload::class.java)
            logger.add("order_id", payload.orderId)

            onOrderReady(payload)
        }
    }

    private fun onTransferSuccessful(payload: TransactionEventPayload) {
        val tx = paymentApi.getTransaction(payload.transactionId).transaction
        if (tx.recipientId == null)
            return

        val tenant = getTenant()
        val sender = accountApi.getAccount(tx.accountId).account
        val recipient = accountApi.getAccount(tx.recipientId!!).account
        val formatter = DecimalFormat(tenant.monetaryFormat)

        val messageId = smsApi.sendMessage(
            SendMessageRequest(
                message = getText(
                    key = "sms.transfer-successful",
                    args = arrayOf(formatter.format(tx.net), sender.displayName ?: ""),
                    locale = Locale(recipient.language)
                ),
                phoneNumber = recipient.phone!!.number

            )
        ).id

        logger.add("message_id", messageId)
    }

    private fun onOrderReady(payload: OrderEventPayload) {
        val tenant = getTenant()
        val order = orderApi.getOrder(payload.orderId).order
        val merchant = accountApi.getAccount(order.merchantId).account
        val formatter = DecimalFormat(tenant.monetaryFormat)

        val messageId = smsApi.sendMessage(
            SendMessageRequest(
                message = getText(
                    key = "sms.order-ready",
                    args = arrayOf(formatter.format(order.totalPrice)),
                    locale = Locale(merchant.language)
                ),
                phoneNumber = merchant.phone!!.number
            )
        ).id

        logger.add("message_id", messageId)
    }

    private fun getTenant(): Tenant {
        val tenantId = tracingContext.tenantId()!!.toLong()
        return tenantApi.getTenant(tenantId).tenant
    }

    protected fun getText(key: String, args: Array<Any?> = emptyArray(), locale: Locale) =
        messages.getMessage(key, args, locale) ?: key
}
