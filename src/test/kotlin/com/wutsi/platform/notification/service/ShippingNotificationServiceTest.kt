package com.wutsi.platform.notification.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.wutsi.ecommerce.shipping.WutsiShippingApi
import com.wutsi.ecommerce.shipping.dto.GetShippingOrderResponse
import com.wutsi.ecommerce.shipping.dto.Shipping
import com.wutsi.ecommerce.shipping.dto.ShippingOrder
import com.wutsi.ecommerce.shipping.entity.ShippingOrderStatus
import com.wutsi.ecommerce.shipping.entity.ShippingType
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
internal class ShippingNotificationServiceTest {
    @MockBean
    private lateinit var accountApi: WutsiAccountApi

    @MockBean
    private lateinit var smsApi: WutsiSmsApi

    @MockBean
    private lateinit var shippingApi: WutsiShippingApi

    @MockBean
    private lateinit var tracingContext: TracingContext

    @Autowired
    private lateinit var service: ShippingNotificationService

    val orderId = "4304930-ef-43095fe99"
    val tenant = Tenant(
        id = 1,
        monetaryFormat = "#,###,##0 XAF"
    )

    val customer = Account(
        id = 1111,
        displayName = "Ray Sponsible",
        language = "en",
        phone = Phone(
            number = "+237695096577"
        )
    )

    @BeforeEach
    fun setUp() {
        doReturn("1").whenever(tracingContext).tenantId()

        doReturn(SendMessageResponse(id = "xxxx")).whenever(smsApi).sendMessage(any())
        doReturn(GetAccountResponse(customer)).whenever(accountApi).getAccount(any())
    }

    @Test
    fun onShippingReadyForPickup() {
        // GIVEN
        val shippingOrder = createShippingOrder(ShippingType.IN_STORE_PICKUP, ShippingOrderStatus.READY_FOR_PICKUP)
        doReturn(GetShippingOrderResponse(shippingOrder)).whenever(shippingApi).getShippingOrder(any())

        // WHEN
        service.onShippingReadyForPickup(111, tenant)

        // THEN
        val request = argumentCaptor<SendMessageRequest>()
        verify(smsApi).sendMessage(request.capture())

        assertEquals(customer.phone?.number, request.firstValue.phoneNumber)
        assertEquals(
            "Wutsi: Your order #FE99 is now ready. Please go to our store for pickup.",
            request.firstValue.message
        )
    }

    private fun createShippingOrder(shippingType: ShippingType, status: ShippingOrderStatus) = ShippingOrder(
        customerId = customer.id,
        merchantId = 1111,
        orderId = orderId,
        status = status.name,
        shipping = Shipping(
            id = 3333,
            type = shippingType.name
        )
    )
}
