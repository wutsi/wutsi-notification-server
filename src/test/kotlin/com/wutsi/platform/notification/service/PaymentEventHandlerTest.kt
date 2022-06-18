package com.wutsi.platform.notification.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.Account
import com.wutsi.platform.account.dto.GetAccountResponse
import com.wutsi.platform.account.dto.Phone
import com.wutsi.platform.core.tracing.TracingContext
import com.wutsi.platform.notification.event.PaymentEventHandler
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.dto.GetTransactionResponse
import com.wutsi.platform.payment.dto.Transaction
import com.wutsi.platform.payment.entity.TransactionType
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
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class PaymentEventHandlerTest {
    @MockBean
    private lateinit var accountApi: WutsiAccountApi

    @MockBean
    private lateinit var smsApi: WutsiSmsApi

    @MockBean
    private lateinit var paymentApi: WutsiPaymentApi

    @MockBean
    private lateinit var tenantApi: WutsiTenantApi

    @MockBean
    private lateinit var tracingContext: TracingContext

    @Autowired
    private lateinit var service: PaymentEventHandler

    val tenant = Tenant(
        id = 1,
        monetaryFormat = "#,###,##0 XAF",
        webappUrl = "https://www.wutsi.me"
    )

    @BeforeEach
    fun setUp() {
        doReturn("1").whenever(tracingContext).tenantId()

        doReturn(SendMessageResponse(id = "xxxx")).whenever(smsApi).sendMessage(any())
        doReturn(GetTenantResponse(tenant)).whenever(tenantApi).getTenant(any())
    }

    @Test
    fun onTransfer() {
        // GIVEN
        val tx = createTransaction(TransactionType.TRANSFER)
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        val sender = createAccount(tx.accountId, "Ray Sponsible")
        doReturn(GetAccountResponse(sender)).whenever(accountApi).getAccount(tx.accountId)

        val recipient = createAccount(tx.recipientId, "John Smith")
        doReturn(GetAccountResponse(recipient)).whenever(accountApi).getAccount(tx.recipientId!!)

        // WHEN
        service.onTransferSuccessful("111")

        // THEN
        val request = argumentCaptor<SendMessageRequest>()
        verify(smsApi).sendMessage(request.capture())

        assertEquals(recipient.phone?.number, request.firstValue.phoneNumber)
        assertEquals("Wutsi: You have received 5,000 XAF from Ray Sponsible", request.firstValue.message)
        assertTrue(request.firstValue.message.length < 160)
    }

    @Test
    fun onTransferFr() {
        // GIVEN
        val tx = createTransaction(TransactionType.TRANSFER)
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        val sender = createAccount(tx.accountId, "Ray Sponsible")
        doReturn(GetAccountResponse(sender)).whenever(accountApi).getAccount(tx.accountId)

        val recipient = createAccount(tx.recipientId, "John Smith", language = "fr")
        doReturn(GetAccountResponse(recipient)).whenever(accountApi).getAccount(tx.recipientId!!)

        // WHEN
        service.onTransferSuccessful("111")

        // THEN
        val request = argumentCaptor<SendMessageRequest>()
        verify(smsApi).sendMessage(request.capture())

        assertEquals(recipient.phone?.number, request.firstValue.phoneNumber)
        assertEquals("Wutsi: Vous avez recu 5,000 XAF de Ray Sponsible", request.firstValue.message)
    }

    @Test
    fun onCharge() {
        // GIVEN
        val tx = createTransaction(TransactionType.CHARGE, orderId = "403430-3094304930-fe001")
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        val sender = createAccount(tx.accountId, "Ray Sponsible")
        doReturn(GetAccountResponse(sender)).whenever(accountApi).getAccount(tx.accountId)

        val recipient = createAccount(tx.recipientId, "John Smith")
        doReturn(GetAccountResponse(recipient)).whenever(accountApi).getAccount(tx.recipientId!!)

        // WHEN
        service.onChargeSuccessful("111")

        // THEN
        val request = argumentCaptor<SendMessageRequest>()
        verify(smsApi).sendMessage(request.capture())

        assertEquals(recipient.phone?.number, request.firstValue.phoneNumber)
        assertEquals(
            "Wutsi: You have received a payment of 5,000 XAF from Ray Sponsible for the order #E001. https://bit.ly/3xZxlCo",
            request.firstValue.message
        )
        assertTrue(request.firstValue.message.length < 160)
    }

    @Test
    fun onCashout() {
        // GIVEN
        val tx = createTransaction(TransactionType.CASHOUT)
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        val sender = createAccount(tx.accountId, "John Smith")
        doReturn(GetAccountResponse(sender)).whenever(accountApi).getAccount(tx.accountId)

        // WHEN
        service.onCashoutSuccessful("111")

        // THEN
        val request = argumentCaptor<SendMessageRequest>()
        verify(smsApi).sendMessage(request.capture())

        assertEquals(sender.phone?.number, request.firstValue.phoneNumber)
        assertEquals(
            "Wutsi: Successfully withdraw 5,100 XAF from your Wallet",
            request.firstValue.message
        )
        assertTrue(request.firstValue.message.length < 160)
    }

    @Test
    fun onCashin() {
        // GIVEN
        val tx = createTransaction(TransactionType.CASHIN)
        doReturn(GetTransactionResponse(tx)).whenever(paymentApi).getTransaction(any())

        val sender = createAccount(tx.accountId, "John Smith")
        doReturn(GetAccountResponse(sender)).whenever(accountApi).getAccount(tx.accountId)

        // WHEN
        service.onCashinSuccessful("111")

        // THEN
        val request = argumentCaptor<SendMessageRequest>()
        verify(smsApi).sendMessage(request.capture())

        assertEquals(sender.phone?.number, request.firstValue.phoneNumber)
        assertEquals(
            "Wutsi: Successfully recharged 5,000 XAF into your Wallet",
            request.firstValue.message
        )
        assertTrue(request.firstValue.message.length < 160)
    }

    private fun createTransaction(type: TransactionType, orderId: String? = null) = Transaction(
        id = "320930293029302",
        currency = "XAF",
        amount = 5100.0,
        net = 5000.0,
        recipientId = 1L,
        accountId = 11L,
        type = type.name,
        orderId = orderId
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
