package com.rzk.kasirpro.core

import com.rzk.kasirpro.data.local.entity.SettingsEntity
import com.rzk.kasirpro.data.model.SaleWithDetails

/**
 * Renders a sale as a receipt.
 *
 * Two outputs from one source of truth: monospaced plain text for sharing to WhatsApp or a
 * thermal printer, and HTML sized for 58 mm roll paper for the Android print framework
 * (which also gives "save as PDF" for free).
 */
object ReceiptFormatter {

    private const val WIDTH = 32

    fun buildText(details: SaleWithDetails, settings: SettingsEntity): String {
        val sale = details.sale
        val symbol = settings.currencySymbol
        val sb = StringBuilder()

        fun line(char: String = "-") = sb.appendLine(char.repeat(WIDTH))
        fun centered(text: String) {
            val pad = ((WIDTH - text.length) / 2).coerceAtLeast(0)
            sb.appendLine(" ".repeat(pad) + text)
        }

        fun row(left: String, right: String) {
            val space = (WIDTH - left.length - right.length).coerceAtLeast(1)
            sb.appendLine(left + " ".repeat(space) + right)
        }

        centered(settings.storeName)
        if (settings.storeAddress.isNotBlank()) centered(settings.storeAddress)
        if (settings.storePhone.isNotBlank()) centered(settings.storePhone)
        if (settings.receiptHeader.isNotBlank()) centered(settings.receiptHeader)
        line("=")

        row(sale.invoiceNo, Formatters.time(sale.createdAt))
        row(Formatters.date(sale.createdAt), sale.cashierName)
        if (sale.customerName.isNotBlank()) sb.appendLine(sale.customerName)
        line()

        details.items.forEach { item ->
            sb.appendLine(item.productName.take(WIDTH))
            row(
                "  ${item.qty} x ${Formatters.number(item.unitPrice)}",
                Formatters.number(item.qty * item.unitPrice)
            )
            if (item.promoDiscount > 0) {
                row("  ${item.promoName.take(18)}", "-${Formatters.number(item.promoDiscount)}")
            }
            if (item.lineDiscount > 0) {
                row("  disc", "-${Formatters.number(item.lineDiscount)}")
            }
        }
        line()

        row("Subtotal", Formatters.number(sale.subtotal))
        if (sale.orderDiscount > 0) row("Discount", "-${Formatters.number(sale.orderDiscount)}")
        if (sale.serviceCharge > 0) row("Service", Formatters.number(sale.serviceCharge))
        if (sale.taxAmount > 0) row("Tax", Formatters.number(sale.taxAmount))
        if (sale.roundingAdjustment != 0L) {
            row("Rounding", Formatters.number(sale.roundingAdjustment))
        }
        line("=")
        row("TOTAL", "$symbol ${Formatters.number(sale.total)}")

        details.payments.forEach { payment ->
            row(payment.method.name, Formatters.number(payment.amount))
        }
        if (sale.changeAmount > 0) row("Change", Formatters.number(sale.changeAmount))

        line()
        if (settings.receiptFooter.isNotBlank()) centered(settings.receiptFooter)

        return sb.toString()
    }

    fun buildHtml(details: SaleWithDetails, settings: SettingsEntity): String {
        val sale = details.sale
        val symbol = settings.currencySymbol

        val itemRows = details.items.joinToString("") { item ->
            val discountRow = buildString {
                if (item.promoDiscount > 0) {
                    append(
                        """<tr><td class="ind">${escape(item.promoName)}</td>
                           <td class="r neg">-${Formatters.number(item.promoDiscount)}</td></tr>"""
                    )
                }
                if (item.lineDiscount > 0) {
                    append(
                        """<tr><td class="ind">Discount</td>
                           <td class="r neg">-${Formatters.number(item.lineDiscount)}</td></tr>"""
                    )
                }
            }
            """
            <tr><td colspan="2" class="name">${escape(item.productName)}</td></tr>
            <tr><td class="ind">${item.qty} × ${Formatters.number(item.unitPrice)}</td>
                <td class="r">${Formatters.number(item.qty * item.unitPrice)}</td></tr>
            $discountRow
            """.trimIndent()
        }

        val totalRows = buildString {
            append(totalRow("Subtotal", Formatters.number(sale.subtotal)))
            if (sale.promoDiscount > 0) {
                append(totalRow("Discount", "-${Formatters.number(sale.promoDiscount)}"))
            }
            if (sale.orderDiscount > 0) {
                append(totalRow("Order discount", "-${Formatters.number(sale.orderDiscount)}"))
            }
            if (sale.serviceCharge > 0) {
                append(totalRow("Service", Formatters.number(sale.serviceCharge)))
            }
            if (sale.taxAmount > 0) append(totalRow("Tax", Formatters.number(sale.taxAmount)))
            if (sale.roundingAdjustment != 0L) {
                append(totalRow("Rounding", Formatters.number(sale.roundingAdjustment)))
            }
        }

        val paymentRows = details.payments.joinToString("") {
            totalRow(it.method.name, Formatters.number(it.amount))
        } + if (sale.changeAmount > 0) {
            totalRow("Change", Formatters.number(sale.changeAmount))
        } else ""

        return """
        <html><head><meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          @page { margin: 4mm; }
          body { font-family: monospace; font-size: 11px; width: 100%; color: #000; }
          .c { text-align: center; }
          .r { text-align: right; }
          .b { font-weight: bold; }
          .name { padding-top: 3px; }
          .ind { padding-left: 8px; color: #333; }
          .neg { color: #333; }
          table { width: 100%; border-collapse: collapse; }
          hr { border: none; border-top: 1px dashed #000; margin: 4px 0; }
          .total { font-size: 14px; font-weight: bold; }
        </style></head><body>
          <div class="c b" style="font-size:14px">${escape(settings.storeName)}</div>
          ${optionalCentered(settings.storeAddress)}
          ${optionalCentered(settings.storePhone)}
          ${optionalCentered(settings.receiptHeader)}
          <hr/>
          <table>
            <tr><td>${escape(sale.invoiceNo)}</td><td class="r">${Formatters.time(sale.createdAt)}</td></tr>
            <tr><td>${Formatters.date(sale.createdAt)}</td><td class="r">${escape(sale.cashierName)}</td></tr>
            ${if (sale.customerName.isNotBlank()) "<tr><td colspan=\"2\">${escape(sale.customerName)}</td></tr>" else ""}
          </table>
          <hr/>
          <table>$itemRows</table>
          <hr/>
          <table>$totalRows</table>
          <hr/>
          <table><tr><td class="total">TOTAL</td>
                     <td class="r total">$symbol ${Formatters.number(sale.total)}</td></tr></table>
          <table>$paymentRows</table>
          <hr/>
          ${optionalCentered(settings.receiptFooter)}
        </body></html>
        """.trimIndent()
    }

    private fun totalRow(label: String, value: String) =
        """<tr><td>${escape(label)}</td><td class="r">${escape(value)}</td></tr>"""

    private fun optionalCentered(text: String) =
        if (text.isBlank()) "" else """<div class="c">${escape(text)}</div>"""

    /** Store names and notes are user input and end up inside HTML — escape them. */
    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
