package com.wutsi.platform.notification.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.stream.Event
import com.wutsi.platform.payment.event.EventURN
import com.wutsi.platform.payment.event.TransactionEventPayload
import com.wutsi.platform.sms.WutsiSmsApi
import com.wutsi.platform.sms.dto.SendMessageRequest
import com.wutsi.platform.tenant.WutsiTenantApi
import org.springframework.context.MessageSource
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import java.util.Locale

@Service
class EventHandler(
    private val accountApi: WutsiAccountApi,
    private val smsApi: WutsiSmsApi,
    private val tenantApi: WutsiTenantApi,
    private val objectMapper: ObjectMapper,
    private val messages: MessageSource,
    private val logger: KVLogger
) {
    @EventListener
    fun onEvent(event: Event) {
        if (EventURN.TRANSACTION_SUCCESSFULL.urn == event.type) {
            val payload = objectMapper.readValue(event.payload, TransactionEventPayload::class.java)
            if (payload.type == "TRANSFER") {
                sendSMS(payload)
            }
        }
    }

    private fun sendSMS(payload: TransactionEventPayload) {
        logger.add("tenant_id", payload.tenantId)
        logger.add("amount", payload.amount)
        logger.add("currency", payload.currency)
        logger.add("transaction_id", payload.transactionId)
        logger.add("account_id", payload.accountId)
        logger.add("recipient_id", payload.recipientId)

        val recipient = accountApi.getAccount(payload.recipientId!!).account
        val phoneNumber = recipient.phone!!.number
        val sender = accountApi.getAccount(payload.accountId).account
        val tenant = tenantApi.getTenant(payload.tenantId).tenant
        val formatter = DecimalFormat(tenant.monetaryFormat)
        val messageId = smsApi.sendMessage(
            SendMessageRequest(
                message = getText(
                    key = "sms.message",
                    args = arrayOf(formatter.format(payload.amount), sender.displayName ?: ""),
                    locale = Locale(recipient.language)
                ),
                phoneNumber = phoneNumber
            )
        ).id
        logger.add("message_id", messageId)
    }

    protected fun getText(key: String, args: Array<Any?> = emptyArray(), locale: Locale) =
        messages.getMessage(key, args, locale) ?: key
}
