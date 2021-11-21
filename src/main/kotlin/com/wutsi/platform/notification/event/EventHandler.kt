package com.wutsi.platform.notification.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.Account
import com.wutsi.platform.core.stream.Event
import com.wutsi.platform.notification.service.TenantProvider
import com.wutsi.platform.payment.event.EventURN
import com.wutsi.platform.payment.event.TransactionEventPayload
import com.wutsi.platform.sms.WutsiSmsApi
import com.wutsi.platform.sms.dto.SendMessageRequest
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import java.util.Locale

@Service
class EventHandler(
    private val accountApi: WutsiAccountApi,
    private val smsApi: WutsiSmsApi,
    private val tenantProvider: TenantProvider,
    private val objectMapper: ObjectMapper,
    private val messages: MessageSource,
) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(EventHandler::class.java)
    }

    @EventListener
    fun onEvent(event: Event) {
        LOGGER.info("onEvent(${event.type},...)")
        if (EventURN.TRANSACTION_SUCCESSFULL.urn.equals(event.type)) {
            val payload = objectMapper.readValue(event.payload, TransactionEventPayload::class.java)
            if (payload.type == "TRANSFER") {
                val sender = accountApi.getAccount(payload.senderId).account
                sendSMS(payload, sender)
            }
        }
    }

    private fun sendSMS(payload: TransactionEventPayload, sender: Account) {
        val phoneNumber = sender.phone?.number
            ?: return

        val tenant = tenantProvider.get(payload.tenantId)
        val formatter = DecimalFormat(tenant.monetaryFormat)
        smsApi.sendMessage(
            SendMessageRequest(
                message = getText(
                    key = "sms.message",
                    args = arrayOf(formatter.format(payload.amount), sender.displayName ?: ""),
                    locale = Locale(sender.language)
                ),
                phoneNumber = phoneNumber
            )
        )
    }

    protected fun getText(key: String, args: Array<Any?> = emptyArray(), locale: Locale) =
        messages.getMessage(key, args, locale) ?: key
}
