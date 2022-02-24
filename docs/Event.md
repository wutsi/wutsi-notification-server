# Event Handling

# Event Consumed

| Event                                            | Source        | Description                                                                                      |
|--------------------------------------------------|---------------|--------------------------------------------------------------------------------------------------|
| `urn:wutsi:event:payment:transaction-successful` | wutsi-payment | This event will trigger a SMS notification to the recipient, for transactions of type `TRANSFER` |
| `urn:wutsi:event:order:order-ready`              | wutsi-order   | This event will trigger a SMS notification to the merchant                                       |
