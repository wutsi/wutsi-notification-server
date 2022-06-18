package com.wutsi.platform.notification.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.ecommerce.order.event.OrderEventPayload
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.stream.Event
import com.wutsi.platform.core.tracing.TracingContext
import com.wutsi.platform.payment.entity.TransactionType
import com.wutsi.platform.payment.event.EventURN
import com.wutsi.platform.payment.event.TransactionEventPayload
import com.wutsi.platform.tenant.WutsiTenantApi
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class EventHandler(
    private val objectMapper: ObjectMapper,
    private val tracingContext: TracingContext,
    private val tenantApi: WutsiTenantApi,
    private val payment: PaymentEventHandler,
    private val order: OrderEventHandler,
    private val logger: KVLogger
) {
    @EventListener
    fun onEvent(event: Event) {
        var messageId: String? = null

        if (EventURN.TRANSACTION_SUCCESSFUL.urn == event.type) {
            val payload = objectMapper.readValue(event.payload, TransactionEventPayload::class.java)
            logger.add("transaction_id", payload.transactionId)

            if (payload.type == TransactionType.TRANSFER.name) {
                messageId = payment.onTransferSuccessful(payload.transactionId)
            } else if (payload.type == TransactionType.CHARGE.name) {
                messageId = payment.onChargeSuccessful(payload.transactionId)
            } else if (payload.type == TransactionType.CASHIN.name) {
                messageId = payment.onCashinSuccessful(payload.transactionId)
            } else if (payload.type == TransactionType.CASHOUT.name) {
                messageId = payment.onCashoutSuccessful(payload.transactionId)
            }
        } else if (com.wutsi.ecommerce.order.event.EventURN.ORDER_OPENED.urn == event.type) {
            val payload = objectMapper.readValue(event.payload, OrderEventPayload::class.java)
            logger.add("order_id", payload.orderId)
            messageId = order.onOrderOpened(payload.orderId)
        } else if (com.wutsi.ecommerce.order.event.EventURN.ORDER_CANCELLED.urn == event.type) {
            val payload = objectMapper.readValue(event.payload, OrderEventPayload::class.java)
            logger.add("order_id", payload.orderId)
            messageId = order.onOrderCancelled(payload.orderId)
        } else if (com.wutsi.ecommerce.order.event.EventURN.ORDER_READY_FOR_PICKUP.urn == event.type) {
            val payload = objectMapper.readValue(event.payload, OrderEventPayload::class.java)
            logger.add("order_id", payload.orderId)
            messageId = order.onOrderReadyForPickup(payload.orderId)
        }

        logger.add("message_id", messageId)
    }
}
