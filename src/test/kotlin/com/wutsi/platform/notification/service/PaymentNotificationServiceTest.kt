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
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.dto.GetTransactionResponse
import com.wutsi.platform.payment.dto.Transaction
import com.wutsi.platform.payment.entity.TransactionType
import com.wutsi.platform.sms.WutsiSmsApi
import com.wutsi.platform.sms.dto.SendMessageRequest
import com.wutsi.platform.sms.dto.SendMessageResponse
import com.wutsi.platform.tenant.dto.Tenant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class PaymentNotificationServiceTest {
    @MockBean
    private lateinit var accountApi: WutsiAccountApi

    @MockBean
    private lateinit var smsApi: WutsiSmsApi

    @MockBean
    private lateinit var paymentApi: WutsiPaymentApi

    @MockBean
    private lateinit var tracingContext: TracingContext

    @Autowired
    private lateinit var service: PaymentNotificationService

    val tenant = Tenant(
        id = 1,
        monetaryFormat = "#,###,##0 XAF"
    )

    @BeforeEach
    fun setUp() {
        doReturn("1").whenever(tracingContext).tenantId()

        val smsResponse = SendMessageResponse(id = "xxxx")
        doReturn(smsResponse).whenever(smsApi).sendMessage(any())
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
        service.onTransferSuccessful("111", tenant)

        // THEN
        val request = argumentCaptor<SendMessageRequest>()
        verify(smsApi).sendMessage(request.capture())

        assertEquals(recipient.phone?.number, request.firstValue.phoneNumber)
        assertEquals("Wutsi: You have received 5,000 XAF from Ray Sponsible", request.firstValue.message)
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
        service.onTransferSuccessful("111", tenant)

        // THEN
        val request = argumentCaptor<SendMessageRequest>()
        verify(smsApi).sendMessage(request.capture())

        assertEquals(recipient.phone?.number, request.firstValue.phoneNumber)
        assertEquals("Wutsi: Vous avez recu 5,000 XAF de Ray Sponsible", request.firstValue.message)
    }

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
