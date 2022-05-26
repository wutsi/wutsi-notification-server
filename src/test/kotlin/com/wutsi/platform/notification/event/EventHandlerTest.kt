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
import com.wutsi.platform.notification.service.OrderNotificationService
import com.wutsi.platform.notification.service.PaymentNotificationService
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
    private lateinit var payment: PaymentNotificationService

    @MockBean
    private lateinit var order: OrderNotificationService

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
        verify(payment).onTransferSuccessful(payload.transactionId, tenant)
    }

    @Test
    fun onCashout() = ignore(TransactionType.CASHOUT)

    @Test
    fun onCashin() = ignore(TransactionType.CASHIN)

    @Test
    fun onPayment() = ignore(TransactionType.PAYMENT)

    private fun ignore(type: TransactionType) {
        // GIVEN
        val payload = createTransactionEventPayload(type)

        // WHEN
        val event = Event(
            type = EventURN.TRANSACTION_SUCCESSFUL.urn,
            payload = objectMapper.writeValueAsString(payload)
        )
        eventHandler.onEvent(event)

        // THEN
        noOp()
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
        verify(order).onOrderOpened(payload.orderId, tenant)
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
        verify(order).onOrderCancelled(payload.orderId, tenant)
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

    private fun noOp() {
        verify(order, never()).onOrderOpened(any(), any())
        verify(payment, never()).onTransferSuccessful(any(), any())
    }

    private fun createOrderEventPayload() = OrderEventPayload(
        orderId = "39043094"
    )

    private fun createTransactionEventPayload(type: TransactionType) = TransactionEventPayload(
        type = type.name,
        transactionId = "320930293029302"
    )
}
