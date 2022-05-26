package com.wutsi.platform.notification.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.ecommerce.order.WutsiOrderApi
import com.wutsi.ecommerce.order.dto.GetOrderResponse
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.Account
import com.wutsi.platform.account.dto.GetAccountResponse
import com.wutsi.platform.account.dto.Phone
import com.wutsi.platform.core.tracing.TracingContext
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
internal class OrderNotificationServiceTest {
    @MockBean
    private lateinit var accountApi: WutsiAccountApi

    @MockBean
    private lateinit var smsApi: WutsiSmsApi

    @MockBean
    private lateinit var orderApi: WutsiOrderApi

    @MockBean
    private lateinit var tracingContext: TracingContext

    @Autowired
    private lateinit var service: OrderNotificationService

    private val tenant = Tenant(
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
    fun onOrderOpened() {
        val order = createOrder()
        doReturn(GetOrderResponse(order)).whenever(orderApi).getOrder(any())

        val merchant = createAccount(order.merchantId, "Ray Sponsible")
        doReturn(GetAccountResponse(merchant)).whenever(accountApi).getAccount(order.merchantId)

        // WHEN
        service.onOrderOpened(order.id, tenant)

        // THEN
        val request = argumentCaptor<SendMessageRequest>()
        verify(smsApi).sendMessage(request.capture())

        assertEquals(merchant.phone?.number, request.firstValue.phoneNumber)
        assertEquals("Wutsi: You have received order #3094 - 5,100 XAF", request.firstValue.message)
    }

    @Test
    fun onOrderCancelled() {
        val order = createOrder()
        doReturn(GetOrderResponse(order)).whenever(orderApi).getOrder(any())

        val merchant = createAccount(order.merchantId, "Ray Sponsible")
        doReturn(GetAccountResponse(merchant)).whenever(accountApi).getAccount(order.merchantId)

        // WHEN
        service.onOrderCancelled(order.id, tenant)

        // THEN
        val request = argumentCaptor<SendMessageRequest>()
        verify(smsApi).sendMessage(request.capture())

        assertEquals(merchant.phone?.number, request.firstValue.phoneNumber)
        assertEquals("Wutsi: Your order #3094 has been cancelled", request.firstValue.message)
    }

    private fun createOrder() = com.wutsi.ecommerce.order.dto.Order(
        id = "39043094",
        accountId = 1L,
        merchantId = 11L,
        totalPrice = 5100.0,
        currency = "XAF"
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
