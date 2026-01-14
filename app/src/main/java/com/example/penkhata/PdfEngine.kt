package com.example.penkhata

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

fun saveInvoiceData(ctx: Context, invNo: String, bName: String, bGst: String, items: List<InvItem>) {
    val json = JSONObject(); json.put("date", SimpleDateFormat("yyyy-MM-dd").format(Date()))
    json.put("invNo", invNo); json.put("bName", bName); json.put("bGst", bGst)
    var total = 0.0; var tax = 0.0
    items.forEach { val t = it.qty * it.rate; total += t; tax += t - (t / (1 + it.taxRate/100)) }
    json.put("total", total); json.put("tax", tax)
    File(ctx.filesDir, "inv_${System.currentTimeMillis()}.json").writeText(json.toString())
}

fun loadGstData(ctx: Context): Map<String, String> {
    var total = 0.0; var tax = 0.0
    ctx.filesDir.listFiles()?.filter { it.name.startsWith("inv_") }?.forEach {
        val j = JSONObject(it.readText()); total += j.getDouble("total"); tax += j.getDouble("tax")
    }
    return mapOf("total" to String.format("%.2f", total), "tax" to String.format("%.2f", tax))
}

fun saveLedgerEntry(ctx: Context, e: LedgerEntry) {
    val j = JSONObject(); j.put("date", e.date); j.put("party", e.party); j.put("type", e.type); j.put("amt", e.amount); j.put("desc", e.desc)
    File(ctx.filesDir, "led_${System.currentTimeMillis()}.json").writeText(j.toString())
}

fun loadLedger(ctx: Context): List<LedgerEntry> {
    val l = mutableListOf<LedgerEntry>()
    ctx.filesDir.listFiles()?.filter { it.name.startsWith("led_") }?.forEach {
        val j = JSONObject(it.readText()); l.add(LedgerEntry(0, j.getString("date"), j.getString("party"), j.getString("type"), j.getDouble("amt"), j.getString("desc")))
    }
    return l
}

fun convertToWords(num: Long): String { return "$num Rupees Only" } // Simplified to save space

fun drawMultiLineText(c: Canvas, text: String, x: Float, y: Float, p: Paint, width: Float) {
    if (p.measureText(text) < width) { c.drawText(text, x, y, p); return }
    val words = text.split(" "); var line = ""; var cy = y
    for (w in words) {
        if (p.measureText(line + w) < width) line += "$w "
        else { c.drawText(line, x, cy, p); line = "$w "; cy += p.textSize + 2f }
    }
    if (line.isNotEmpty()) c.drawText(line, x, cy, p)
}

fun createPdf(ctx: Context, isQuote: Boolean, invNo: String, date: String, payMode: String, delNote: String,
              sName: String, sAddr: String, sGst: String, sBank: String, sIfsc: String, sJuris: String,
              bName: String, bAddr: String, bGst: String, bState: String, items: List<InvItem>) {
    val doc = PdfDocument(); val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val c = page.canvas; val p = Paint(); val bp = Paint().apply{style=Paint.Style.STROKE; strokeWidth=1f}
    val m=20f; val w=555f; val h=800f; val midX=m+w/2

    c.drawRect(m, m, m+w, m+h, bp) // Border
    p.isFakeBoldText=true; p.textAlign=Paint.Align.CENTER; p.textSize=14f
    c.drawText(if(isQuote) "QUOTATION" else "TAX INVOICE", midX, m+15, p)
    c.drawLine(m, m+20, m+w, m+20, bp)

    val r1=m+20; val rH=120f
    c.drawLine(midX, r1, midX, r1+rH, bp); c.drawLine(m, r1+rH/2, m+w, r1+rH/2, bp)

    p.textAlign=Paint.Align.LEFT; p.textSize=12f; p.isFakeBoldText=true
    c.drawText(sName, m+5, r1+15, p)
    p.isFakeBoldText=false; p.textSize=10f
    c.drawText(sAddr, m+5, r1+30, p); c.drawText("GSTIN: $sGst", m+5, r1+45, p)

    c.drawText("Buyer: $bName", m+5, r1+rH/2+15, p)
    drawMultiLineText(c, bAddr, m+5, r1+rH/2+30, p, w/2 - 10)
    c.drawText("GSTIN: $bGst", m+5, r1+rH/2+60, p)

    val qX = midX + w/4; val line1 = r1+rH/4; val line2 = r1 + 2*rH/4; val line3 = r1 + 3*rH/4
    c.drawLine(midX, line1, m+w, line1, bp); c.drawLine(midX, line2, m+w, line2, bp); c.drawLine(midX, line3, m+w, line3, bp); c.drawLine(qX, r1, qX, r1+rH, bp)
    fun cell(l:String, v:String, x:Float, y:Float) { c.drawText(l, x+2, y+12, p); p.isFakeBoldText=true; c.drawText(v, qX+2, y+12, p); p.isFakeBoldText=false }
    cell("No", invNo, midX, r1); cell("Date", date, midX, line1); cell("Note", delNote, midX, line2); cell("Terms", payMode, midX, line3)

    val tTop = r1+rH; val hH=20f
    c.drawLine(m, tTop, m+w, tTop, bp); c.drawLine(m, tTop+hH, m+w, tTop+hH, bp)
    val c1=m; val w1=30f; val c2=c1+w1; val w2=200f; val c3=c2+w2; val w3=50f
    val c4=c3+w3; val w4=60f; val c5=c4+w4; val w5=70f; val c6=c5+w5; val w6=50f; val c7=c6+w6; val w7=95f
    fun vLine(top:Float, bot:Float) { c.drawLine(c2,top,c2,bot,bp); c.drawLine(c3,top,c3,bot,bp); c.drawLine(c4,top,c4,bot,bp); c.drawLine(c5,top,c5,bot,bp); c.drawLine(c6,top,c6,bot,bp); c.drawLine(c7,top,c7,bot,bp) }
    vLine(tTop, tTop+hH)
    p.isFakeBoldText=true; p.textAlign=Paint.Align.CENTER
    c.drawText("SI", c1+w1/2, tTop+14, p); c.drawText("Description", c2+w2/2, tTop+14, p); c.drawText("HSN", c3+w3/2, tTop+14, p); c.drawText("Qty", c4+w4/2, tTop+14, p); c.drawText("Rate", c5+w5/2, tTop+14, p); c.drawText("Per", c6+w6/2, tTop+14, p); c.drawText("Amt", c7+w7/2, tTop+14, p)

    p.isFakeBoldText=false; var y=tTop+hH; var gTotal=0.0; var totalTaxable=0.0; var totalTax=0.0
    items.forEachIndexed { i, it ->
        val rowInc = it.qty * it.rate; val taxF = 1 + (it.taxRate/100); val rowBase = rowInc/taxF; val rowTax = rowInc - rowBase
        gTotal += rowInc; totalTaxable += rowBase; totalTax += rowTax
        c.drawText("${i+1}", c1+w1/2, y+14, p)
        p.textAlign=Paint.Align.LEFT; c.drawText(it.desc, c2+5, y+14, p)
        if(it.serial.isNotEmpty()) { val ps=Paint(p); ps.textSize=8f; c.drawText("Sr:${it.serial}", c2+5, y+24, ps) }
        p.textAlign=Paint.Align.CENTER
        c.drawText(it.hsn, c3+w3/2, y+14, p); c.drawText(it.qty.toString(), c4+w4/2, y+14, p)
        c.drawText(String.format("%.2f", rowBase), c5+w5/2, y+14, p); c.drawText(it.unit, c6+w6/2, y+14, p)
        p.textAlign=Paint.Align.RIGHT; c.drawText(String.format("%.2f", rowBase*it.qty), m+w-5, y+14, p)
        y+=20f
    }
    val fTop = m+h-200f; vLine(tTop+hH, fTop); c.drawLine(m, fTop, m+w, fTop, bp)
    y=fTop; val tX=c7; p.textAlign=Paint.Align.RIGHT
    fun row(l:String, v:String) { c.drawText(l, tX-10, y+14, p); c.drawText(v, m+w-5, y+14, p); c.drawLine(tX, y, tX, y+20, bp); c.drawLine(tX, y+20, m+w, y+20, bp); y+=20f }
    row("Total Value", String.format("%.2f", totalTaxable))
    row("SGST", String.format("%.2f", totalTax/2)); row("CGST", String.format("%.2f", totalTax/2))
    p.isFakeBoldText=true; c.drawText("Grand Total", tX-10, y+14, p); c.drawText("₹ ${String.format("%.0f", gTotal)}", m+w-5, y+14, p); c.drawLine(tX, y, tX, m+h, bp)

    val fY = fTop+20; p.textAlign=Paint.Align.LEFT; p.isFakeBoldText=false
    c.drawText("Amount: ${convertToWords(gTotal.toLong())}", m+5, fY, p)
    if(sBank.isNotEmpty()) {
        val bankY = fY+40; c.drawLine(m, bankY, tX, bankY, bp)
        c.drawText("Bank Details:", m+5, bankY+15, p); p.isFakeBoldText=true; c.drawText("$sBank | $sIfsc", m+5, bankY+30, p)
    }
    val decY = fY+80; c.drawLine(m, decY, tX, decY, bp)
    p.isFakeBoldText=false; p.textSize=8f
    c.drawText("Declaration: We declare this invoice shows the actual price of goods.", m+5, decY+12, p)
    c.drawText("Subject to $sJuris Jurisdiction", m+5, decY+22, p); p.isFakeBoldText=true
    c.drawText("GOODS ONCE SOLD CANNOT BE RETURNED", m+5, decY+35, p)
    val sigY = decY; c.drawLine(c5, sigY, tX, sigY, bp)
    p.textSize=10f; p.textAlign=Paint.Align.CENTER
    c.drawText("For, $sName", (c5+tX)/2+30, sigY+15, p); c.drawText("Auth. Signatory", (c5+tX)/2+30, m+h-10, p)
    p.isFakeBoldText=false; p.textSize=8f; c.drawText("Computer generated invoice.", midX, m+h+15, p)

    doc.finishPage(page)
    val name = if(isQuote) "Quote" else "Inv"
    val f = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "${name}_${System.currentTimeMillis()}.pdf")
    doc.writeTo(FileOutputStream(f)); doc.close()
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).apply { setDataAndType(FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", f), "application/pdf"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "View"))
}
