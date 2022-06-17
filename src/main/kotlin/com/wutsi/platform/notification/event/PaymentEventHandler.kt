package com.wutsi.platform.notification.event

import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.core.logging.KVLogger
import com.wutsi.platform.core.url.UrlShortener
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.payment.dto.Transaction
import com.wutsi.platform.sms.WutsiSmsApi
import com.wutsi.platform.sms.dto.SendMessageRequest
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import java.util.Locale

@Service
class PaymentEventHandler(
    private val accountApi: WutsiAccountApi,
    private val smsApi: WutsiSmsApi,
    private val messages: MessageSource,
    private val paymentApi: WutsiPaymentApi,
    private val logger: KVLogger,
    private val urlShortener: UrlShortener,
) {
    /**
     * Send SMS notification to the recipient
     */
    fun onTransferSuccessful(transactionId: String, tenant: Tenant): String? {
        val tx = paymentApi.getTransaction(transactionId).transaction
        log(tx)
        if (tx.recipientId == null)
            return null

        val sender = accountApi.getAccount(tx.accountId).account
        val recipient = accountApi.getAccount(tx.recipientId!!).account
        val formatter = DecimalFormat(tenant.monetaryFormat)

        return smsApi.sendMessage(
            SendMessageRequest(
                message = getText(
                    key = "sms.payment-transfer-successful",
                    args = arrayOf(formatter.format(tx.net), sender.displayName ?: ""),
                    locale = Locale(recipient.language)
                ),
                phoneNumber = recipient.phone!!.number

            )
        ).id
    }

    /**
     * Send SMS notification to the recipient
     */
    fun onChargeSuccessful(transactionId: String, tenant: Tenant): String? {
        val tx = paymentApi.getTransaction(transactionId).transaction
        log(tx)
        if (tx.recipientId == null || tx.orderId == null)
            return null

        val orderId = tx.orderId!!.uppercase().takeLast(4)
        val sender = accountApi.getAccount(tx.accountId).account
        val recipient = accountApi.getAccount(tx.recipientId!!).account
        val formatter = DecimalFormat(tenant.monetaryFormat)
        val url = urlShortener.shorten("${tenant.webappUrl}/order?id=$orderId")

        return smsApi.sendMessage(
            SendMessageRequest(
                message = getText(
                    key = "sms.payment-charge-successful",
                    args = arrayOf(formatter.format(tx.net), sender.displayName ?: "", orderId, url),
                    locale = Locale(sender.language)
                ),
                phoneNumber = recipient.phone!!.number
            )
        ).id
    }

    /**
     * Send SMS notification to account owner
     */
    fun onCashinSuccessful(transactionId: String, tenant: Tenant): String? {
        val tx = paymentApi.getTransaction(transactionId).transaction
        log(tx)

        val account = accountApi.getAccount(tx.accountId).account
        val formatter = DecimalFormat(tenant.monetaryFormat)

        return smsApi.sendMessage(
            SendMessageRequest(
                message = getText(
                    key = "sms.payment-cashin-successful",
                    args = arrayOf(formatter.format(tx.net)),
                    locale = Locale(account.language)
                ),
                phoneNumber = account.phone!!.number
            )
        ).id
    }

    /**
     * Send SMS notification to account owner
     */
    fun onCashoutSuccessful(transactionId: String, tenant: Tenant): String? {
        val tx = paymentApi.getTransaction(transactionId).transaction
        log(tx)

        val account = accountApi.getAccount(tx.accountId).account
        val formatter = DecimalFormat(tenant.monetaryFormat)

        return smsApi.sendMessage(
            SendMessageRequest(
                message = getText(
                    key = "sms.payment-cashout-successful",
                    args = arrayOf(formatter.format(tx.amount)),
                    locale = Locale(account.language)
                ),
                phoneNumber = account.phone!!.number
            )
        ).id
    }

    private fun getText(key: String, args: Array<Any?> = emptyArray(), locale: Locale) =
        messages.getMessage(key, args, locale) ?: key

    private fun log(tx: Transaction) {
        logger.add("sender_id", tx.accountId)
        logger.add("recipient_id", tx.recipientId)
        logger.add("type", tx.type)
        logger.add("order_id", tx.orderId)
        logger.add("amount", tx.amount)
        logger.add("net", tx.net)
    }
}
