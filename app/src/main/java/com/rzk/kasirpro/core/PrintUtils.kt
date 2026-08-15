package com.rzk.kasirpro.core

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Printing and sharing.
 *
 * Rather than shipping a driver for one brand of thermal printer, receipts go through the
 * Android print framework: any printer the device already knows about works, including
 * Bluetooth thermal printers with a vendor print service, and "Save as PDF" is free.
 */
object PrintUtils {

    /**
     * The WebView must stay reachable until the framework has pulled the document out of
     * it — a local reference would be collected mid-print and the job would silently die.
     */
    private var printingWebView: WebView? = null

    fun printHtml(context: Context, html: String, jobName: String) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val adapter = view.createPrintDocumentAdapter(jobName)
                printManager.print(
                    jobName,
                    adapter,
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A6)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                )
                printingWebView = null
            }
        }
        printingWebView = webView
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    }

    fun shareText(context: Context, text: String, subject: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, subject))
    }
}
