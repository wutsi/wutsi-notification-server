package com.wutsi.platform.notification.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.Account
import com.wutsi.platform.account.dto.GetAccountResponse
import com.wutsi.platform.account.dto.Phone
import com.wutsi.platform.core.stream.Event
import com.wutsi.platform.payment.event.EventURN
import com.wutsi.platform.payment.event.TransactionEventPayload
import com.wutsi.platform.sms.WutsiSmsApi
import com.wutsi.platform.sms.dto.SendMessageRequest
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

    @Autowired
    private lateinit var eventHandler: EventHandler

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        val tenant = Tenant(
            id = 1,
            monetaryFormat = "#,###,##0 XAF"
        )
        doReturn(GetTenantResponse(tenant)).whenever(tenantApi).getTenant(any())
    }

    @Test
    fun onTransfer() {
        // GIVEN
        val payload = createTransactionEventPayload("TRANSFER")

        val sender = createAccount(payload.senderId)
        doReturn(GetAccountResponse(sender)).whenever(accountApi).getAccount(payload.senderId)

        // WHEN
        val event = Event(
            type = EventURN.TRANSACTION_SUCCESSFULL.urn,
            payload = objectMapper.writeValueAsString(payload)
        )
        eventHandler.onEvent(event)

        // THEN
        val request = argumentCaptor<SendMessageRequest>()
        verify(smsApi).sendMessage(request.capture())

        assertEquals(sender.phone?.number, request.firstValue.phoneNumber)
        assertEquals("You have received 5,000 XAF from Ray Sponsible", request.firstValue.message)
    }

    @Test
    fun onCashin() {
        // GIVEN
        val payload = createTransactionEventPayload("CASHIN")

        val sender = createAccount(payload.senderId)
        doReturn(GetAccountResponse(sender)).whenever(accountApi).getAccount(payload.senderId)

        // WHEN
        val event = Event(
            type = EventURN.TRANSACTION_SUCCESSFULL.urn,
            payload = objectMapper.writeValueAsString(payload)
        )
        eventHandler.onEvent(event)

        // THEN
        verify(smsApi, never()).sendMessage(any())
    }

    @Test
    fun onCashout() {
        // GIVEN
        val payload = createTransactionEventPayload("CASHOUT")

        val sender = createAccount(payload.senderId)
        doReturn(GetAccountResponse(sender)).whenever(accountApi).getAccount(payload.senderId)

        // WHEN
        val event = Event(
            type = EventURN.TRANSACTION_SUCCESSFULL.urn,
            payload = objectMapper.writeValueAsString(payload)
        )
        eventHandler.onEvent(event)

        // THEN
        verify(smsApi, never()).sendMessage(any())
    }

    private fun createTransactionEventPayload(type: String) = TransactionEventPayload(
        tenantId = 1,
        type = type,
        currency = "XAF",
        amount = 5000.0,
        recipientId = 1L,
        senderId = 11L,
        transactionId = "320930293029302"
    )

    private fun createAccount(id: Long) = Account(
        id = id,
        displayName = "Ray Sponsible",
        language = "en",
        phone = Phone(
            number = "+237695096577"
        )
    )
}
