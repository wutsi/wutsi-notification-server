package com.wutsi.platform.notification.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.ecommerce.order.event.OrderEventPayload
import com.wutsi.platform.core.stream.Event
import com.wutsi.platform.core.tracing.TracingContext
import com.wutsi.platform.payment.entity.TransactionType
import com.wutsi.platform.payment.event.EventURN
import com.wutsi.platform.payment.event.TransactionEventPayload
import com.wutsi.platform.tenant.WutsiTenantApi
import com.wutsi.platform.tenant.dto.GetTenantResponse
import com.wutsi.platform.tenant.dto.Tenant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class EventHandlerTest {
    @MockBean
    private lateinit var tenantApi: WutsiTenantApi

    @MockBean
    private lateinit var tracingContext: TracingContext

    @Autowired
    private lateinit var eventHandler: EventHandler

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var payment: PaymentEventHandler

    @MockBean
    private lateinit var order: OrderEventHandler

    val tenant = Tenant(
        id = 1,
        monetaryFormat = "#,###,##0 XAF"
    )

    @BeforeEach
    fun setUp() {
        doReturn("1").whenever(tracingContext).tenantId()

        doReturn(GetTenantResponse(tenant)).whenever(tenantApi).getTenant(any())
    }

    // Payment events
    @Test
    fun onTransfer() {
        // GIVEN
        val payload = createTransactionEventPayload(TransactionType.TRANSFER)

        // WHEN
        val event = Event(
            type = EventURN.TRANSACTION_SUCCESSFUL.urn,
            payload = objectMapper.writeValueAsString(payload)
        )
        eventHandler.onEvent(event)

        // THEN
        verify(payment).onTransferSuccessful(payload.transactionId)
    }

    @Test
    fun onCashout() {
        // GIVEN
        val payload = createTransactionEventPayload(TransactionType.CASHOUT)

        // WHEN
        val event = Event(
            type = EventURN.TRANSACTION_SUCCESSFUL.urn,
            payload = objectMapper.writeValueAsString(payload)
        )
        eventHandler.onEvent(event)

        // THEN
        verify(payment).onCashoutSuccessful(payload.transactionId)
    }

    @Test
    fun onCashin() {
        // GIVEN
        val payload = createTransactionEventPayload(TransactionType.CASHIN)

        // WHEN
        val event = Event(
            type = EventURN.TRANSACTION_SUCCESSFUL.urn,
            payload = objectMapper.writeValueAsString(payload)
        )
        eventHandler.onEvent(event)

        // THEN
        verify(payment).onCashinSuccessful(payload.transactionId)
    }

    @Test
    fun onCharge() {
        // GIVEN
        val payload = createTransactionEventPayload(TransactionType.CHARGE)

        // WHEN
        val event = Event(
            type = EventURN.TRANSACTION_SUCCESSFUL.urn,
            payload = objectMapper.writeValueAsString(payload)
        )
        eventHandler.onEvent(event)

        // THEN
        verify(payment).onChargeSuccessful(payload.transactionId)
    }

    // Order events
    @Test
    fun onOrderOpened() {
        val payload = createOrderEventPayload()

        // WHEN
        val event = Event(
            type = com.wutsi.ecommerce.order.event.EventURN.ORDER_OPENED.urn,
            payload = objectMapper.writeValueAsString(payload)
        )
        eventHandler.onEvent(event)

        // THEN
        verify(order).onOrderOpened(payload.orderId)
    }

    @Test
    fun onOrderCancelled() {
        val payload = createOrderEventPayload()

        // WHEN
        val event = Event(
            type = com.wutsi.ecommerce.order.event.EventURN.ORDER_CANCELLED.urn,
            payload = objectMapper.writeValueAsString(payload)
        )
        eventHandler.onEvent(event)

        // THEN
        verify(order).onOrderCancelled(payload.orderId)
    }

    @Test
    fun onOrderDone() {
        val payload = createOrderEventPayload()

        // WHEN
        val event = Event(
            type = com.wutsi.ecommerce.order.event.EventURN.ORDER_DONE.urn,
            payload = objectMapper.writeValueAsString(payload)
        )
        eventHandler.onEvent(event)

        // THEN
        noOp()
    }

    @Test
    fun onOrderReadyForPickup() {
        val payload = OrderEventPayload("111")

        // WHEN
        val event = Event(
            type = com.wutsi.ecommerce.order.event.EventURN.ORDER_READY_FOR_PICKUP.urn,
            payload = objectMapper.writeValueAsString(payload)
        )
        eventHandler.onEvent(event)

        // THEN
        verify(order).onOrderReadyForPickup(payload.orderId)
    }

    @Test
    fun onOrerDelivered() {
        val payload = OrderEventPayload("111")

        // WHEN
        val event = Event(
            type = com.wutsi.ecommerce.order.event.EventURN.ORDER_DELIVERED.urn,
            payload = objectMapper.writeValueAsString(payload)
        )
        eventHandler.onEvent(event)

        // THEN
        noOp()
    }

    @Test
    fun onOrderInTransit() {
        val payload = OrderEventPayload("111")

        // WHEN
        val event = Event(
            type = com.wutsi.ecommerce.order.event.EventURN.ORDER_IN_TRANSIT.urn,
            payload = objectMapper.writeValueAsString(payload)
        )
        eventHandler.onEvent(event)

        // THEN
        noOp()
    }

    private fun noOp() {
        verify(order, never()).onOrderOpened(any())
        verify(order, never()).onOrderCancelled(any())
        verify(payment, never()).onTransferSuccessful(any())
    }

    private fun createOrderEventPayload() = OrderEventPayload(
        orderId = "39043094"
    )

    private fun createTransactionEventPayload(type: TransactionType) = TransactionEventPayload(
        type = type.name,
        transactionId = "320930293029302"
    )
}
