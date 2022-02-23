package com.wutsi.platform.notification.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.ecommerce.order.WutsiOrderApi
import com.wutsi.ecommerce.order.dto.GetOrderResponse
import com.wutsi.ecommerce.order.event.OrderEventPayload
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.Account
import com.wutsi.platform.account.dto.GetAccountResponse
import com.wutsi.platform.account.dto.Phone
import com.wutsi.platform.core.stream.Event
import com.wutsi.platform.core.tracing.TracingContext
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.dto.GetTransactionResponse
import com.wutsi.platform.payment.dto.Transaction
import com.wutsi.platform.payment.entity.TransactionType
import com.wutsi.platform.payment.event.EventURN
import com.wutsi.platform.payment.event.TransactionEventPayload
import com.wutsi.platform.sms.WutsiSmsApi
import com.wutsi.platform.sms.dto.SendMessageRequest
import com.wutsi.platform.sms.dto.SendMessageResponse
import com.wutsi.platform.tenant.WutsiTenantApi
import com.wutsi.platform.tenant.dto.GetTenantResponse
import com.wutsi.platform.tenant.dto.Tenant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class EventHandlerTest {
    @MockBean
    private lateinit var accountApi: WutsiAccountApi

    @MockBean
    private lateinit var smsApi: WutsiSmsApi

    @MockBean
    private lateinit var tenantApi: WutsiTenantApi

    @MockBean
    private lateinit var paymentApi: WutsiPaymentApi

    @MockBean
    private lateinit var orderApi: WutsiOrderApi

    @MockBean
    private lateinit var tracingContext: TracingContext

    @Autowired
    private lateinit var eventHandler: EventHandler

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        doReturn("1").whenever(tracingContext).tenantId()

        val tenant = Tenant(
            id = 1,
            monetaryFormat = "#,###,##0 XAF"
        )
        doReturn(GetTenantResponse(tenant)).whenever(tenantApi).getTenant(any())

        val smsResponse = SendMessageResponse(id = "xxxx")
        doReturn(smsResponse).whenever(smsApi).sendMessage(any())
    }

    @Test
    fun onTransfer() {
        // GIVEN
        val payload = createTransactionEventPayload(TransactionType.TRANSFER)
        val tx = createTransaction(TransactionType.TRANSFER)
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        val sender = createAccount(tx.accountId, "Ray Sponsible")
        doReturn(GetAccountResponse(sender)).whenever(accountApi).getAccount(tx.accountId)

        val recipient = createAccount(tx.recipientId, "John Smith")
        doReturn(GetAccountResponse(recipient)).whenever(accountApi).getAccount(tx.recipientId!!)

        // WHEN
        val event = Event(
            type = EventURN.TRANSACTION_SUCCESSFUL.urn,
            payload = objectMapper.writeValueAsString(payload)
        )
        eventHandler.onEvent(event)

        // THEN
        val request = argumentCaptor<SendMessageRequest>()
        verify(smsApi).sendMessage(request.capture())

        assertEquals(recipient.phone?.number, request.firstValue.phoneNumber)
        assertEquals("Wutsi: Ray Sponsible sent you 5,000 XAF", request.firstValue.message)
    }

    @Test
    fun onCashout() = noOp(TransactionType.CASHOUT)

    @Test
    fun onCashin() = noOp(TransactionType.CASHIN)

    @Test
    fun onPayment() = noOp(TransactionType.PAYMENT)

    @Test
    fun onOrderReady() {
        val payload = createOrderEventPayload()
        val order = createOrder()
        doReturn(GetOrderResponse(order)).whenever(orderApi).getOrder(any())

        val merchant = createAccount(order.merchantId, "Ray Sponsible")
        doReturn(GetAccountResponse(merchant)).whenever(accountApi).getAccount(order.merchantId)

        // WHEN
        val event = Event(
            type = com.wutsi.ecommerce.order.event.EventURN.ORDER_READY.urn,
            payload = objectMapper.writeValueAsString(payload)
        )
        eventHandler.onEvent(event)

        // THEN
        val request = argumentCaptor<SendMessageRequest>()
        verify(smsApi).sendMessage(request.capture())

        assertEquals(merchant.phone?.number, request.firstValue.phoneNumber)
        assertEquals("Wutsi: You have received a new order of 5,100 XAF", request.firstValue.message)
    }

    private fun noOp(type: TransactionType) {
        // GIVEN
        val payload = createTransactionEventPayload(type)

        // WHEN
        val event = Event(
            type = EventURN.TRANSACTION_SUCCESSFUL.urn,
            payload = objectMapper.writeValueAsString(payload)
        )
        eventHandler.onEvent(event)

        // THEN
        verify(smsApi, never()).sendMessage(any())
    }

    private fun createOrderEventPayload() = OrderEventPayload(
        orderId = "39043094"
    )

    private fun createOrder() = com.wutsi.ecommerce.order.dto.Order(
        id = "39043094",
        accountId = 1L,
        merchantId = 11L,
        totalPrice = 5100.0,
        currency = "XAF"
    )

    private fun createTransactionEventPayload(type: TransactionType) = TransactionEventPayload(
        type = type.name,
        transactionId = "320930293029302"
    )

    private fun createTransaction(type: TransactionType) = Transaction(
        id = "320930293029302",
        currency = "XAF",
        amount = 5100.0,
        net = 5000.0,
        recipientId = 1L,
        accountId = 11L,
        type = type.name
    )

    private fun createAccount(id: Long?, displayName: String, language: String = "en") = Account(
        id = id ?: -1,
        displayName = displayName,
        language = language,
        phone = Phone(
            number = "+237695096577"
        )
    )
}
