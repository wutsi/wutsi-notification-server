## Event Handling

## Event Consumed

| Event                                            | Source        | Description                                                                                                   |
|--------------------------------------------------|---------------|---------------------------------------------------------------------------------------------------------------|
| `urn:wutsi:event:payment:transaction-successful` | wutsi-payment | This event will trigger a SMS notification to the recipient, for transactions of type `TRANSFER`              |
| `urn:wutsi:event:order:order-opened`             | wutsi-order   | This event will trigger a SMS notification to the merchant to inform him has has received a new order         |
| `urn:wutsi:event:order:order-ready-for-pickup`   | wutsi-order   | This event will trigger a SMS notification to the customer to inform him that its order is ready for shipping |
| `urn:wutsi:event:order:order-cancelled`          | wutsi-order   | This event will trigger a SMS notification to the customer to inform him that its order has been cancelled    |
