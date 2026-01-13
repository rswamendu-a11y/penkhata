package com.example.penkhata

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// --- DATA MODEL ---
data class InvItem(val desc: String, val hsn: String, val qty: Double, val rate: Double, val unit: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF212121))) {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    var currentScreen by remember { mutableStateOf("dashboard") }
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("penkhata_data", Context.MODE_PRIVATE) }

    // SETTINGS DATA
    var sName by remember { mutableStateOf(prefs.getString("sName", "") ?: "") }
    var sAddr by remember { mutableStateOf(prefs.getString("sAddr", "") ?: "") }
    var sGst by remember { mutableStateOf(prefs.getString("sGst", "") ?: "") }
    var sBank by remember { mutableStateOf(prefs.getString("sBank", "") ?: "") }
    var sIfsc by remember { mutableStateOf(prefs.getString("sIfsc", "") ?: "") }
    var sJuris by remember { mutableStateOf(prefs.getString("sJuris", "Jaipur") ?: "") }

    fun saveSettings(n: String, a: String, g: String, b: String, i: String, j: String) {
        prefs.edit().putString("sName",n).putString("sAddr",a).putString("sGst",g).putString("sBank",b).putString("sIfsc",i).putString("sJuris",j).apply()
        sName=n; sAddr=a; sGst=g; sBank=b; sIfsc=i; sJuris=j
        Toast.makeText(ctx, "Details Saved", Toast.LENGTH_SHORT).show()
        currentScreen = "dashboard"
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = currentScreen=="dashboard", onClick = { currentScreen="dashboard" }, icon = { Icon(Icons.Default.ReceiptLong, "Bill") }, label = { Text("Invoice") })
                NavigationBarItem(selected = currentScreen=="settings", onClick = { currentScreen="settings" }, icon = { Icon(Icons.Default.Settings, "Setup") }, label = { Text("Settings") })
            }
        }
    ) { p ->
        Box(Modifier.padding(p)) {
            if(currentScreen == "dashboard") DashboardScreen(sName, sAddr, sGst, sBank, sIfsc, sJuris)
            else SettingsScreen(sName, sAddr, sGst, sBank, sIfsc, sJuris, ::saveSettings)
        }
    }
}

@Composable
fun DashboardScreen(sName: String, sAddr: String, sGst: String, sBank: String, sIfsc: String, sJuris: String) {
    val ctx = LocalContext.current
    var invNo by remember { mutableStateOf("1") }
    var date by remember { mutableStateOf(SimpleDateFormat("dd-MMM-yyyy").format(Date())) }
    var payMode by remember { mutableStateOf("") }
    var delNote by remember { mutableStateOf("") }

    var bName by remember { mutableStateOf("") }
    var bAddr by remember { mutableStateOf("") }
    var bGst by remember { mutableStateOf("") }
    var bState by remember { mutableStateOf("") }

    var iDesc by remember { mutableStateOf("") }
    var iHsn by remember { mutableStateOf("") }
    var iQty by remember { mutableStateOf("") }
    var iRate by remember { mutableStateOf("") }
    var iUnit by remember { mutableStateOf("pcs") }
    var items by remember { mutableStateOf(listOf<InvItem>()) }

    var labour by remember { mutableStateOf("") }
    var cartage by remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("PenKhata Billing", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Card(Modifier.padding(vertical=5.dp)) {
            Column(Modifier.padding(10.dp)) {
                Text("Invoice Details", fontWeight = FontWeight.Bold)
                Row {
                    OutlinedTextField(invNo, {invNo=it}, label={Text("Inv No")}, modifier=Modifier.weight(1f))
                    Spacer(Modifier.width(5.dp))
                    OutlinedTextField(date, {date=it}, label={Text("Date")}, modifier=Modifier.weight(1f))
                }
                OutlinedTextField(payMode, {payMode=it}, label={Text("Pay Mode/Terms")}, modifier=Modifier.fillMaxWidth())
                OutlinedTextField(delNote, {delNote=it}, label={Text("Delivery Note")}, modifier=Modifier.fillMaxWidth())
            }
        }

        Card(Modifier.padding(vertical=5.dp)) {
            Column(Modifier.padding(10.dp)) {
                Text("Buyer Details", fontWeight = FontWeight.Bold)
                OutlinedTextField(bName, {bName=it}, label={Text("Name")}, modifier=Modifier.fillMaxWidth())
                OutlinedTextField(bAddr, {bAddr=it}, label={Text("Address")}, modifier=Modifier.fillMaxWidth())
                Row {
                    OutlinedTextField(bGst, {bGst=it}, label={Text("GSTIN")}, modifier=Modifier.weight(1f))
                    Spacer(Modifier.width(5.dp))
                    OutlinedTextField(bState, {bState=it}, label={Text("State Code")}, modifier=Modifier.weight(1f))
                }
            }
        }

        Card(Modifier.padding(vertical=5.dp)) {
            Column(Modifier.padding(10.dp)) {
                Text("Add Item", fontWeight = FontWeight.Bold)
                OutlinedTextField(iDesc, {iDesc=it}, label={Text("Description")}, modifier=Modifier.fillMaxWidth())
                Row {
                    OutlinedTextField(iHsn, {iHsn=it}, label={Text("HSN")}, modifier=Modifier.weight(1f))
                    Spacer(Modifier.width(5.dp))
                    OutlinedTextField(iUnit, {iUnit=it}, label={Text("Per (Unit)")}, modifier=Modifier.weight(1f))
                }
                Row {
                    OutlinedTextField(iQty, {iQty=it}, label={Text("Qty")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), modifier=Modifier.weight(1f))
                    Spacer(Modifier.width(5.dp))
                    OutlinedTextField(iRate, {iRate=it}, label={Text("Rate")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), modifier=Modifier.weight(1f))
                }
                Button(onClick={if(iDesc.isNotEmpty()){items=items+InvItem(iDesc,iHsn,iQty.toDoubleOrNull()?:0.0,iRate.toDoubleOrNull()?:0.0,iUnit); iDesc=""}}, modifier=Modifier.fillMaxWidth()) { Text("ADD ITEM") }
            }
        }
        items.forEachIndexed{i,it->Text("${i+1}. ${it.desc} - ${it.qty} ${it.unit}")}

        Card(Modifier.padding(vertical=5.dp)) {
            Column(Modifier.padding(10.dp)) {
                Row {
                    OutlinedTextField(labour, {labour=it}, label={Text("Labour Charge")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), modifier=Modifier.weight(1f))
                    Spacer(Modifier.width(5.dp))
                    OutlinedTextField(cartage, {cartage=it}, label={Text("Cartage")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), modifier=Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(onClick={
            if(sName.isEmpty()) Toast.makeText(ctx,"Setup Settings First!",Toast.LENGTH_LONG).show()
            else createGridPdf(ctx, invNo, date, payMode, delNote, sName, sAddr, sGst, sBank, sIfsc, sJuris, bName, bAddr, bGst, bState, items, labour.toDoubleOrNull()?:0.0, cartage.toDoubleOrNull()?:0.0)
        }, modifier=Modifier.fillMaxWidth(), colors=ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("GENERATE PDF") }
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
fun SettingsScreen(n: String, a: String, g: String, b: String, i: String, j: String, onSave: (String,String,String,String,String,String)->Unit) {
    var sName by remember { mutableStateOf(n) }; var sAddr by remember { mutableStateOf(a) }
    var sGst by remember { mutableStateOf(g) }; var sBank by remember { mutableStateOf(b) }; var sIfsc by remember { mutableStateOf(i) }; var sJuris by remember { mutableStateOf(j) }
    Column(Modifier.padding(16.dp)) {
        Text("Seller Settings", fontSize=24.sp, fontWeight=FontWeight.Bold)
        OutlinedTextField(sName, {sName=it}, label={Text("Firm Name")}, modifier=Modifier.fillMaxWidth())
        OutlinedTextField(sAddr, {sAddr=it}, label={Text("Address (City/State/Pin)")}, modifier=Modifier.fillMaxWidth())
        OutlinedTextField(sGst, {sGst=it}, label={Text("GSTIN")}, modifier=Modifier.fillMaxWidth())
        OutlinedTextField(sBank, {sBank=it}, label={Text("Bank Name & A/c No")}, modifier=Modifier.fillMaxWidth())
        OutlinedTextField(sIfsc, {sIfsc=it}, label={Text("IFSC Code")}, modifier=Modifier.fillMaxWidth())
        OutlinedTextField(sJuris, {sJuris=it}, label={Text("Jurisdiction City")}, modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        Button(onClick={onSave(sName,sAddr,sGst,sBank,sIfsc,sJuris)}, modifier=Modifier.fillMaxWidth()) { Text("SAVE") }
    }
}

fun createGridPdf(ctx: Context, invNo: String, date: String, payMode: String, delNote: String,
                  sName: String, sAddr: String, sGst: String, sBank: String, sIfsc: String, sJuris: String,
                  bName: String, bAddr: String, bGst: String, bState: String, items: List<InvItem>,
                  labour: Double, cartage: Double) {
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val c = page.canvas
    val p = Paint(); val bp = Paint().apply{style=Paint.Style.STROKE; strokeWidth=1f}
    val m=20f; val w=555f; val h=800f; val midX=m+w/2

    c.drawRect(m, m, m+w, m+h, bp) // Outer Border

    // Header
    p.isFakeBoldText=true; p.textAlign=Paint.Align.CENTER; p.textSize=14f
    c.drawText("GST INVOICE", midX, m+15, p)
    c.drawLine(m, m+20, m+w, m+20, bp)

    // Grid Layout (Top)
    val r1=m+20; val rH=120f
    c.drawLine(midX, r1, midX, r1+rH, bp) // Vert Split
    c.drawLine(m, r1+rH/2, m+w, r1+rH/2, bp) // Horiz Split

    // Seller (Top Left)
    p.textAlign=Paint.Align.LEFT; p.textSize=12f; p.isFakeBoldText=true
    c.drawText(sName, m+5, r1+15, p)
    p.isFakeBoldText=false; p.textSize=10f
    c.drawText(sAddr, m+5, r1+30, p)
    c.drawText("GSTIN: $sGst", m+5, r1+45, p)

    // Buyer (Bottom Left)
    p.isFakeBoldText=true; p.textSize=12f
    c.drawText("Buyer: $bName", m+5, r1+rH/2+15, p)
    p.isFakeBoldText=false; p.textSize=10f
    c.drawText(bAddr, m+5, r1+rH/2+30, p)
    c.drawText("GSTIN: $bGst  State: $bState", m+5, r1+rH/2+45, p)

    // Meta Data (Right Side)
    val qX = midX + w/4
    val line1 = r1+rH/4; val line2 = r1 + 2*rH/4; val line3 = r1 + 3*rH/4
    c.drawLine(midX, line1, m+w, line1, bp)
    c.drawLine(midX, line2, m+w, line2, bp)
    c.drawLine(midX, line3, m+w, line3, bp)
    c.drawLine(qX, r1, qX, r1+rH, bp)

    fun cell(l:String, v:String, x:Float, y:Float) { c.drawText(l, x+2, y+12, p); p.isFakeBoldText=true; c.drawText(v, qX+2, y+12, p); p.isFakeBoldText=false }
    cell("Invoice No", invNo, midX, r1)
    cell("Date", date, midX, line1)
    cell("Delivery Note", delNote, midX, line2)
    cell("Terms/Mode", payMode, midX, line3)

    // Table Header
    val tTop = r1+rH; val hH=20f
    c.drawLine(m, tTop, m+w, tTop, bp); c.drawLine(m, tTop+hH, m+w, tTop+hH, bp)
    val c1=m; val w1=30f; val c2=c1+w1; val w2=200f; val c3=c2+w2; val w3=50f
    val c4=c3+w3; val w4=60f; val c5=c4+w4; val w5=70f; val c6=c5+w5; val w6=50f; val c7=c6+w6; val w7=95f

    fun vLine(top:Float, bot:Float) { c.drawLine(c2,top,c2,bot,bp); c.drawLine(c3,top,c3,bot,bp); c.drawLine(c4,top,c4,bot,bp); c.drawLine(c5,top,c5,bot,bp); c.drawLine(c6,top,c6,bot,bp); c.drawLine(c7,top,c7,bot,bp) }
    vLine(tTop, tTop+hH)

    p.isFakeBoldText=true; p.textAlign=Paint.Align.CENTER
    c.drawText("SI", c1+w1/2, tTop+14, p); c.drawText("Description", c2+w2/2, tTop+14, p)
    c.drawText("HSN", c3+w3/2, tTop+14, p); c.drawText("Qty", c4+w4/2, tTop+14, p)
    c.drawText("Rate", c5+w5/2, tTop+14, p); c.drawText("Per", c6+w6/2, tTop+14, p)
    c.drawText("Amount", c7+w7/2, tTop+14, p)

    // Items
    p.isFakeBoldText=false; var y=tTop+hH; var sub=0.0
    items.forEachIndexed { i, it ->
        val amt = it.qty * it.rate; sub+=amt
        c.drawText("${i+1}", c1+w1/2, y+14, p)
        p.textAlign=Paint.Align.LEFT; c.drawText(it.desc, c2+5, y+14, p)
        p.textAlign=Paint.Align.CENTER; c.drawText(it.hsn, c3+w3/2, y+14, p)
        c.drawText(it.qty.toString(), c4+w4/2, y+14, p)
        c.drawText(it.rate.toString(), c5+w5/2, y+14, p)
        c.drawText(it.unit, c6+w6/2, y+14, p)
        p.textAlign=Paint.Align.RIGHT; c.drawText(String.format("%.2f", amt), m+w-5, y+14, p)
        y+=20f
    }
    val fTop = m+h-200f; vLine(tTop+hH, fTop); c.drawLine(m, fTop, m+w, fTop, bp)

    // Totals (Right Side)
    y=fTop; val tX=c7; p.textAlign=Paint.Align.RIGHT
    fun row(l:String, v:String) { c.drawText(l, tX-10, y+14, p); c.drawText(v, m+w-5, y+14, p); c.drawLine(tX, y, tX, y+20, bp); c.drawLine(tX, y+20, m+w, y+20, bp); y+=20f }

    row("Total", String.format("%.2f", sub))
    if(labour>0) row("Labour Charges", String.format("%.2f", labour))
    if(cartage>0) row("Cartage Service", String.format("%.2f", cartage))
    val taxable = sub+labour+cartage
    val tax = taxable*0.18 // 18% assumption
    row("SGST (9%)", String.format("%.2f", tax/2))
    row("CGST (9%)", String.format("%.2f", tax/2))
    val gTotal = taxable+tax
    p.isFakeBoldText=true
    c.drawText("Grand Total", tX-10, y+14, p); c.drawText("₹ ${String.format("%.0f", gTotal)}", m+w-5, y+14, p)
    c.drawLine(tX, y, tX, m+h, bp)

    // Footer (Left Side)
    val fY = fTop+20; p.textAlign=Paint.Align.LEFT; p.isFakeBoldText=false
    c.drawText("Amount in words: ${convertNumToWords(gTotal.toLong())} Only", m+5, fY, p)
    val bankY = fY+40; c.drawLine(m, bankY, tX, bankY, bp)
    c.drawText("Bank Details:", m+5, bankY+15, p)
    p.isFakeBoldText=true; c.drawText("$sBank, IFSC: $sIfsc", m+5, bankY+30, p)

    val decY = bankY+60; c.drawLine(m, decY, tX, decY, bp)
    p.isFakeBoldText=false; p.textSize=8f
    c.drawText("Declaration: We declare this invoice shows the actual price of goods.", m+5, decY+15, p)
    c.drawText("Subject to $sJuris Jurisdiction", m+5, decY+25, p)

    // Computer Gen Note
    val sigY = decY; c.drawLine(c5, sigY, tX, sigY, bp)
    p.textSize=10f; p.isFakeBoldText=true; p.textAlign=Paint.Align.CENTER
    c.drawText("For, $sName", (c5+tX)/2+30, sigY+15, p)
    p.isFakeBoldText=false; p.textSize=8f
    c.drawText("Computer generated invoice", (c5+tX)/2+30, m+h-20, p)
    c.drawText("No signature required", (c5+tX)/2+30, m+h-10, p)

    doc.finishPage(page)
    val f = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "PenKhata_${System.currentTimeMillis()}.pdf")
    doc.writeTo(FileOutputStream(f)); doc.close()
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="application/pdf"; putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", f)); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share"))
}

fun convertNumToWords(n: Long): String { return "$n Rupees" }
