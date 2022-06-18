package com.wutsi.platform.notification.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.ecommerce.order.WutsiOrderApi
import com.wutsi.ecommerce.order.dto.GetOrderResponse
import com.wutsi.ecommerce.order.dto.Order
import com.wutsi.ecommerce.shipping.WutsiShippingApi
import com.wutsi.ecommerce.shipping.dto.GetShippingResponse
import com.wutsi.ecommerce.shipping.dto.Shipping
import com.wutsi.ecommerce.shipping.entity.ShippingType
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.Account
import com.wutsi.platform.account.dto.GetAccountResponse
import com.wutsi.platform.account.dto.Phone
import com.wutsi.platform.core.tracing.TracingContext
import com.wutsi.platform.notification.event.OrderEventHandler
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
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class OrderEventHandlerTest {
    @MockBean
    private lateinit var accountApi: WutsiAccountApi

    @MockBean
    private lateinit var shippingApi: WutsiShippingApi

    @MockBean
    private lateinit var smsApi: WutsiSmsApi

    @MockBean
    private lateinit var orderApi: WutsiOrderApi

    @MockBean
    private lateinit var tracingContext: TracingContext

    @Autowired
    private lateinit var service: OrderEventHandler

    private var order: Order = createOrder()
    private lateinit var merchant: Account
    private lateinit var customer: Account

    private val tenant = Tenant(
        id = 1,
        monetaryFormat = "#,###,##0 XAF",
        webappUrl = "https://www.wutsi.me"
    )

    @BeforeEach
    fun setUp() {
        doReturn("1").whenever(tracingContext).tenantId()

        val smsResponse = SendMessageResponse(id = "xxxx")
        doReturn(smsResponse).whenever(smsApi).sendMessage(any())

        doReturn(GetOrderResponse(order)).whenever(orderApi).getOrder(any())

        merchant = createAccount(order.merchantId, "Ray Sponsible")
        doReturn(GetAccountResponse(merchant)).whenever(accountApi).getAccount(order.merchantId)

        customer = createAccount(order.accountId, "Roger Milla")
        doReturn(GetAccountResponse(customer)).whenever(accountApi).getAccount(order.accountId)
    }

    @Test
    fun onOrderOpened() {
        // WHEN
        service.onOrderOpened(order.id, tenant)

        // THEN
        val request = argumentCaptor<SendMessageRequest>()
        verify(smsApi).sendMessage(request.capture())

        assertEquals(merchant.phone?.number, request.firstValue.phoneNumber)
        assertEquals(
            "Wutsi: You have received a new order of 5,100 XAF from Roger Milla. https://bit.ly/3N4OW05",
            request.firstValue.message
        )
        assertTrue(request.firstValue.message.length < 160)
    }

    @Test
    fun onOrderCancelled() {
        val order = createOrder()
        doReturn(GetOrderResponse(order)).whenever(orderApi).getOrder(any())

        val customer = createAccount(order.merchantId, "Ray Sponsible")
        doReturn(GetAccountResponse(customer)).whenever(accountApi).getAccount(order.accountId)

        // WHEN
        service.onOrderCancelled(order.id, tenant)

        // THEN
        val request = argumentCaptor<SendMessageRequest>()
        verify(smsApi).sendMessage(request.capture())

        assertEquals(customer.phone?.number, request.firstValue.phoneNumber)
        assertEquals("Wutsi: Your order #3094 has been cancelled. https://bit.ly/3N4OW05", request.firstValue.message)
        assertTrue(request.firstValue.message.length < 160)
    }

    @Test
    fun onOrderReadyToPickupInStore() {
        // GIVEN
        val order = createOrder(11)
        doReturn(GetOrderResponse(order)).whenever(orderApi).getOrder(any())

        val shipping = createShipping(11, ShippingType.IN_STORE_PICKUP)
        doReturn(GetShippingResponse(shipping)).whenever(shippingApi).getShipping(any())

        val customer = createAccount(order.accountId, "Roger Milla")
        doReturn(GetAccountResponse(customer)).whenever(accountApi).getAccount(any())

        // WHEN
        service.onOrderReadyForPickup("111", tenant)

        // THEN
        val request = argumentCaptor<SendMessageRequest>()
        verify(smsApi).sendMessage(request.capture())

        assertEquals(customer.phone?.number, request.firstValue.phoneNumber)
        assertEquals(
            "Wutsi: Your order #3094 is now available in store for pickup. https://bit.ly/3tLN5X7",
            request.firstValue.message
        )
    }

    private fun createOrder(shippingId: Long? = null) = com.wutsi.ecommerce.order.dto.Order(
        id = "39043094",
        accountId = 1L,
        merchantId = 11L,
        totalPrice = 5100.0,
        currency = "XAF",
        shippingId = shippingId
    )

    private fun createShipping(id: Long, type: ShippingType) = Shipping(
        id = id,
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
