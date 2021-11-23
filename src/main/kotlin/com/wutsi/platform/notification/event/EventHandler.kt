package com.wutsi.platform.notification.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.account.dto.Account
import com.wutsi.platform.core.logging.RequestKVLogger
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
            if (payload.type == "TRANSFER" && payload.recipientId != null) {
                val recipient = accountApi.getAccount(payload.recipientId!!).account
                val sender = accountApi.getAccount(payload.userId).account
                sendSMS(payload, sender, recipient)
            }
        }
    }

    private fun sendSMS(payload: TransactionEventPayload, sender: Account, recipient: Account) {
        val phoneNumber = recipient.phone?.number
        val logger = RequestKVLogger()
        logger.add("tenant_id", payload.tenantId)
        logger.add("amount", payload.amount)
        logger.add("currency", payload.currency)
        logger.add("transaction_id", payload.transactionId)
        logger.add("sender_id", sender.id)
        logger.add("sender_display_name", sender.displayName)
        logger.add("recipient_id", recipient.id)
        logger.add("recipient_phone_number", phoneNumber)

        try {
            if (phoneNumber == null)
                return

            val tenant = tenantProvider.get(payload.tenantId)
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
        } catch (ex: Exception) {
            logger.setException(ex)
        } finally {
            logger.log()
        }
    }

    protected fun getText(key: String, args: Array<Any?> = emptyArray(), locale: Locale) =
        messages.getMessage(key, args, locale) ?: key
}
