package com.wutsi.platform.notification.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.ecommerce.order.event.OrderEventPayload
import com.wutsi.ecommerce.shipping.event.ShippingOrderEventPayload
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.stream.Event
import com.wutsi.platform.core.tracing.TracingContext
import com.wutsi.platform.notification.service.OrderNotificationService
import com.wutsi.platform.notification.service.PaymentNotificationService
import com.wutsi.platform.notification.service.ShippingNotificationService
import com.wutsi.platform.payment.entity.TransactionType
import com.wutsi.platform.payment.event.EventURN
import com.wutsi.platform.payment.event.TransactionEventPayload
import com.wutsi.platform.tenant.WutsiTenantApi
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class EventHandler(
    private val objectMapper: ObjectMapper,
    private val tracingContext: TracingContext,
    private val tenantApi: WutsiTenantApi,
    private val payment: PaymentNotificationService,
    private val order: OrderNotificationService,
    private val shipping: ShippingNotificationService,
    private val logger: KVLogger
) {
    @EventListener
    fun onEvent(event: Event) {
        var messageId: String? = null

        if (EventURN.TRANSACTION_SUCCESSFUL.urn == event.type) {
            val payload = objectMapper.readValue(event.payload, TransactionEventPayload::class.java)
            logger.add("transaction_id", payload.transactionId)
            logger.add("transaction_type", payload.type)
            logger.add("order_id", payload.orderId)
            if (payload.type == TransactionType.TRANSFER.name) {
                messageId = payment.onTransferSuccessful(payload.transactionId, getTenant())
            }
        } else if (com.wutsi.ecommerce.order.event.EventURN.ORDER_OPENED.urn == event.type) {
            val payload = objectMapper.readValue(event.payload, OrderEventPayload::class.java)
            logger.add("order_id", payload.orderId)
            messageId = order.onOrderOpened(payload.orderId, getTenant())
        } else if (com.wutsi.ecommerce.order.event.EventURN.ORDER_CANCELLED.urn == event.type) {
            val payload = objectMapper.readValue(event.payload, OrderEventPayload::class.java)
            logger.add("order_id", payload.orderId)
            messageId = order.onOrderCancelled(payload.orderId, getTenant())
        } else if (com.wutsi.ecommerce.shipping.event.EventURN.SHIPPING_READY_FOR_PICKUP.urn == event.type) {
            val payload = objectMapper.readValue(event.payload, ShippingOrderEventPayload::class.java)
            logger.add("shipping_order_id", payload.shippingOrderId)
            messageId = shipping.onShippingReadyForPickup(payload.shippingOrderId, getTenant())
        }

        logger.add("message_id", messageId)
    }

    private fun getTenant(): Tenant {
        val tenantId = tracingContext.tenantId()!!.toLong()
        return tenantApi.getTenant(tenantId).tenant
    }
}
