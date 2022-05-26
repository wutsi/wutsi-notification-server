package com.wutsi.platform.notification.service

import com.wutsi.platform.account.WutsiAccountApi
import com.wutsi.platform.payment.WutsiPaymentApi
import com.wutsi.platform.sms.WutsiSmsApi
import com.wutsi.platform.sms.dto.SendMessageRequest
import com.wutsi.platform.tenant.dto.Tenant
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import java.util.Locale

@Service
class PaymentNotificationService(
    private val accountApi: WutsiAccountApi,
    private val smsApi: WutsiSmsApi,
    private val messages: MessageSource,
    private val paymentApi: WutsiPaymentApi,
) {
    fun onTransferSuccessful(transactionId: String, tenant: Tenant): String? {
        val tx = paymentApi.getTransaction(transactionId).transaction
        if (tx.recipientId == null)
            return null

        val sender = accountApi.getAccount(tx.accountId).account
        val recipient = accountApi.getAccount(tx.recipientId!!).account
        val formatter = DecimalFormat(tenant.monetaryFormat)

        return smsApi.sendMessage(
            SendMessageRequest(
                message = getText(
                    key = "sms.transfer-successful",
                    args = arrayOf(formatter.format(tx.net), sender.displayName ?: ""),
                    locale = Locale(recipient.language)
                ),
                phoneNumber = recipient.phone!!.number

            )
        ).id
    }

    private fun getText(key: String, args: Array<Any?> = emptyArray(), locale: Locale) =
        messages.getMessage(key, args, locale) ?: key
}
