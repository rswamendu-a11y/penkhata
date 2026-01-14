package com.example.penkhata

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class InvItem(val desc: String, val serial: String, val hsn: String, val qty: Double, val rate: Double, val unit: String, val taxRate: Double)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF2E7D32))) { MainAppScreen() } }
    }
}

@Composable
fun MainAppScreen() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("penkhata_data", Context.MODE_PRIVATE) }
    var savedPin by remember { mutableStateOf(prefs.getString("appPin", "") ?: "") }
    var isLocked by remember { mutableStateOf(savedPin.isNotEmpty()) }
    var currentScreen by remember { mutableStateOf("dashboard") }

    if (isLocked) LoginScreen(savedPin) { isLocked = false }
    else Scaffold(
        bottomBar = { NavigationBar {
            NavigationBarItem(selected = currentScreen=="dashboard", onClick = { currentScreen="dashboard" }, icon = { Icon(Icons.Default.EditNote, "Entry") }, label = { Text("Entry") })
            NavigationBarItem(selected = currentScreen=="history", onClick = { currentScreen="history" }, icon = { Icon(Icons.Default.History, "Reports") }, label = { Text("Reports") })
            NavigationBarItem(selected = currentScreen=="settings", onClick = { currentScreen="settings" }, icon = { Icon(Icons.Default.Settings, "Setup") }, label = { Text("Settings") })
        }}
    ) { p -> Box(Modifier.padding(p)) { when(currentScreen) {
        "dashboard" -> DashboardScreen()
        "history" -> HistoryScreen()
        else -> SettingsScreen(savedPin) { newPin -> prefs.edit().putString("appPin", newPin).apply(); savedPin = newPin }
    }}}
}

@Composable
fun LoginScreen(correctPin: String, onUnlock: () -> Unit) {
    var inputPin by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(30.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("ENTER PIN", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value = inputPin, onValueChange = { inputPin = it }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        Button(onClick = { if(inputPin == correctPin) onUnlock() }, modifier = Modifier.fillMaxWidth()) { Text("UNLOCK") }
    }
}

@Composable
fun DashboardScreen() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("penkhata_data", Context.MODE_PRIVATE) }
    val defTax = prefs.getString("defTax", "18") ?: "18"
    var invNo by remember { mutableStateOf("1") }
    var date by remember { mutableStateOf(SimpleDateFormat("dd-MMM-yyyy").format(Date())) }
    var payMode by remember { mutableStateOf("") }; var delNote by remember { mutableStateOf("") }
    var bName by remember { mutableStateOf("") }; var bAddr by remember { mutableStateOf("") }
    var bGst by remember { mutableStateOf("") }; var bState by remember { mutableStateOf("") }
    var iDesc by remember { mutableStateOf("") }; var iSerial by remember { mutableStateOf("") }
    var iHsn by remember { mutableStateOf("") }; var iQty by remember { mutableStateOf("") }
    var iRate by remember { mutableStateOf("") }; var iUnit by remember { mutableStateOf("pcs") }
    var iTax by remember { mutableStateOf(defTax) }; var items by remember { mutableStateOf(listOf<InvItem>()) }

    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("New Invoice", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { invNo="1"; bName=""; bAddr=""; bGst=""; items=emptyList(); Toast.makeText(ctx, "Form Cleared", Toast.LENGTH_SHORT).show() }) { Text("RESET", color = Color.Red) }
        }
        Card(Modifier.padding(vertical=5.dp)) { Column(Modifier.padding(10.dp)) {
            Row { OutlinedTextField(invNo, {invNo=it}, label={Text("Inv No")}, modifier=Modifier.weight(1f)); Spacer(Modifier.width(5.dp)); OutlinedTextField(date, {date=it}, label={Text("Date")}, modifier=Modifier.weight(1f)) }
            OutlinedTextField(payMode, {payMode=it}, label={Text("Pay Mode")}, modifier=Modifier.fillMaxWidth())
            OutlinedTextField(delNote, {delNote=it}, label={Text("Delivery Note")}, modifier=Modifier.fillMaxWidth())
        }}
        Card(Modifier.padding(vertical=5.dp)) { Column(Modifier.padding(10.dp)) {
            Text("Buyer", fontWeight = FontWeight.Bold)
            OutlinedTextField(bName, {bName=it}, label={Text("Name")}, modifier=Modifier.fillMaxWidth())
            OutlinedTextField(bAddr, {bAddr=it}, label={Text("Address")}, modifier=Modifier.fillMaxWidth())
            Row { OutlinedTextField(bGst, {bGst=it}, label={Text("GSTIN")}, modifier=Modifier.weight(1f)); Spacer(Modifier.width(5.dp)); OutlinedTextField(bState, {bState=it}, label={Text("State Code")}, modifier=Modifier.weight(1f)) }
        }}
        Card(Modifier.padding(vertical=5.dp)) { Column(Modifier.padding(10.dp)) {
            Text("Add Item", fontWeight = FontWeight.Bold)
            OutlinedTextField(iDesc, {iDesc=it}, label={Text("Item Name")}, modifier=Modifier.fillMaxWidth())
            OutlinedTextField(iSerial, {iSerial=it}, label={Text("Serial/IMEI")}, modifier=Modifier.fillMaxWidth())
            Row { OutlinedTextField(iHsn, {iHsn=it}, label={Text("HSN")}, modifier=Modifier.weight(1f)); Spacer(Modifier.width(5.dp)); OutlinedTextField(iUnit, {iUnit=it}, label={Text("Unit")}, modifier=Modifier.weight(1f)) }
            Row { OutlinedTextField(iQty, {iQty=it}, label={Text("Qty")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), modifier=Modifier.weight(1f)); Spacer(Modifier.width(5.dp)); OutlinedTextField(iRate, {iRate=it}, label={Text("Rate (Inc Tax)")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), modifier=Modifier.weight(1f)) }
            Row(Modifier.padding(top=5.dp)) { OutlinedTextField(iTax, {iTax=it}, label={Text("GST %")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), modifier=Modifier.weight(1f)); Spacer(Modifier.width(5.dp)); Button(onClick={ if(iDesc.isNotEmpty()){ items=items+InvItem(iDesc,iSerial,iHsn,iQty.toDoubleOrNull()?:0.0,iRate.toDoubleOrNull()?:0.0,iUnit,iTax.toDoubleOrNull()?:18.0); iDesc=""; iSerial=""; iQty=""; iRate="" } }, modifier=Modifier.weight(1f).height(55.dp)) { Text("ADD") } }
        }}
        items.forEachIndexed{ idx, it ->
            Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${idx+1}. ${it.desc} (${it.qty} ${it.unit})", modifier = Modifier.weight(1f))
                IconButton(onClick = { val mut = items.toMutableList(); mut.removeAt(idx); items = mut }) { Icon(Icons.Default.Delete, "Remove", tint = Color.Red) }
            }
            Divider()
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick={
            val sName = prefs.getString("sName", "") ?: ""
            if(sName.isEmpty()) Toast.makeText(ctx,"Setup Settings First!",Toast.LENGTH_LONG).show()
            else createFinalPdf(ctx, invNo, date, payMode, delNote, sName, prefs.getString("sAddr","")?:"", prefs.getString("sGst","")?:"", prefs.getString("sBank","")?:"", prefs.getString("sIfsc","")?:"", prefs.getString("sJuris","")?:"", bName, bAddr, bGst, bState, items)
        }, modifier=Modifier.fillMaxWidth(), colors=ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("GENERATE PDF") }
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
fun HistoryScreen() {
    val ctx = LocalContext.current
    val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
    val files = remember { dir?.listFiles()?.filter { it.extension == "pdf" }?.sortedByDescending { it.lastModified() } ?: emptyList() }
    LazyColumn(Modifier.padding(16.dp)) {
        item { Text("Reports (History)", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom=16.dp)) }
        items(files) { file ->
            Card(Modifier.fillMaxWidth().padding(bottom=8.dp).clickable {
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
                ctx.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/pdf"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) })
            }) { Column(Modifier.padding(16.dp)) { Text(file.name, fontWeight = FontWeight.Bold); Text(SimpleDateFormat("dd MMM yyyy HH:mm").format(Date(file.lastModified())), fontSize=12.sp, color=Color.Gray) } }
        }
    }
}

@Composable
fun SettingsScreen(currentPin: String, onPinSave: (String)->Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("penkhata_data", Context.MODE_PRIVATE) }
    var sName by remember { mutableStateOf(prefs.getString("sName", "") ?: "") }
    var sAddr by remember { mutableStateOf(prefs.getString("sAddr", "") ?: "") }
    var sGst by remember { mutableStateOf(prefs.getString("sGst", "") ?: "") }
    var sBank by remember { mutableStateOf(prefs.getString("sBank", "") ?: "") }
    var sIfsc by remember { mutableStateOf(prefs.getString("sIfsc", "") ?: "") }
    var sJuris by remember { mutableStateOf(prefs.getString("sJuris", "") ?: "") }
    var defTax by remember { mutableStateOf(prefs.getString("defTax", "18") ?: "18") }
    var newPin by remember { mutableStateOf(currentPin) }

    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Settings", fontSize=24.sp, fontWeight=FontWeight.Bold)
        Card(Modifier.padding(top=10.dp)) { Column(Modifier.padding(10.dp)) {
            Text("Security", fontWeight = FontWeight.Bold)
            OutlinedTextField(newPin, {newPin=it}, label={Text("Set Login PIN")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword), modifier=Modifier.fillMaxWidth())
        }}
        Card(Modifier.padding(top=10.dp)) { Column(Modifier.padding(10.dp)) {
            Text("Seller Info", fontWeight = FontWeight.Bold)
            OutlinedTextField(sName, {sName=it}, label={Text("Firm Name")}, modifier=Modifier.fillMaxWidth())
            OutlinedTextField(sAddr, {sAddr=it}, label={Text("Address")}, modifier=Modifier.fillMaxWidth())
            OutlinedTextField(sGst, {sGst=it}, label={Text("GSTIN")}, modifier=Modifier.fillMaxWidth())
            OutlinedTextField(sBank, {sBank=it}, label={Text("Bank Name & Acc")}, modifier=Modifier.fillMaxWidth())
            OutlinedTextField(sIfsc, {sIfsc=it}, label={Text("IFSC")}, modifier=Modifier.fillMaxWidth())
            OutlinedTextField(sJuris, {sJuris=it}, label={Text("Jurisdiction")}, modifier=Modifier.fillMaxWidth())
            OutlinedTextField(defTax, {defTax=it}, label={Text("Default Tax %")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), modifier=Modifier.fillMaxWidth())
        }}
        Spacer(Modifier.height(20.dp))
        Button(onClick={ prefs.edit().putString("sName",sName).putString("sAddr",sAddr).putString("sGst",sGst).putString("sBank",sBank).putString("sIfsc",sIfsc).putString("sJuris",sJuris).putString("defTax", defTax).apply(); onPinSave(newPin); Toast.makeText(ctx, "Saved!", Toast.LENGTH_SHORT).show() }, modifier=Modifier.fillMaxWidth()) { Text("SAVE ALL") }
        Spacer(Modifier.height(60.dp))
    }
}

fun convertToWords(num: Long): String {
    if (num == 0L) return "Zero"
    val units = arrayOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen")
    val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")
    fun recurse(n: Long): String {
        return when {
            n < 20 -> units[n.toInt()]
            n < 100 -> tens[(n / 10).toInt()] + " " + units[(n % 10).toInt()]
            n < 1000 -> units[(n / 100).toInt()] + " Hundred " + recurse(n % 100)
            n < 100000 -> recurse(n / 1000) + " Thousand " + recurse(n % 1000)
            n < 10000000 -> recurse(n / 100000) + " Lakh " + recurse(n % 100000)
            else -> recurse(n / 10000000) + " Crore " + recurse(n % 10000000)
        }
    }
    return recurse(num).trim() + " Rupees Only"
}

fun createFinalPdf(ctx: Context, invNo: String, date: String, payMode: String, delNote: String,
                  sName: String, sAddr: String, sGst: String, sBank: String, sIfsc: String, sJuris: String,
                  bName: String, bAddr: String, bGst: String, bState: String, items: List<InvItem>) {
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val c = page.canvas
    val p = Paint(); val bp = Paint().apply{style=Paint.Style.STROKE; strokeWidth=1f}
    val m=20f; val w=555f; val h=800f; val midX=m+w/2
    c.drawRect(m, m, m+w, m+h, bp)
    p.isFakeBoldText=true; p.textAlign=Paint.Align.CENTER; p.textSize=14f
    c.drawText("GST INVOICE", midX, m+15, p)
    c.drawLine(m, m+20, m+w, m+20, bp)
    val r1=m+20; val rH=120f
    c.drawLine(midX, r1, midX, r1+rH, bp); c.drawLine(m, r1+rH/2, m+w, r1+rH/2, bp)
    p.textAlign=Paint.Align.LEFT; p.textSize=12f; p.isFakeBoldText=true
    c.drawText(sName, m+5, r1+15, p)
    p.isFakeBoldText=false; p.textSize=10f
    c.drawText(sAddr, m+5, r1+30, p); c.drawText("GSTIN: $sGst", m+5, r1+45, p)
    c.drawText("Buyer: $bName", m+5, r1+rH/2+15, p)
    c.drawText(bAddr, m+5, r1+rH/2+30, p); c.drawText("GSTIN: $bGst  State: $bState", m+5, r1+rH/2+45, p)
    val qX = midX + w/4
    val line1 = r1+rH/4; val line2 = r1 + 2*rH/4; val line3 = r1 + 3*rH/4
    c.drawLine(midX, line1, m+w, line1, bp); c.drawLine(midX, line2, m+w, line2, bp); c.drawLine(midX, line3, m+w, line3, bp); c.drawLine(qX, r1, qX, r1+rH, bp)
    fun cell(l:String, v:String, x:Float, y:Float) { c.drawText(l, x+2, y+12, p); p.isFakeBoldText=true; c.drawText(v, qX+2, y+12, p); p.isFakeBoldText=false }
    cell("Invoice No", invNo, midX, r1); cell("Date", date, midX, line1); cell("Delivery Note", delNote, midX, line2); cell("Terms/Mode", payMode, midX, line3)
    val tTop = r1+rH; val hH=20f
    c.drawLine(m, tTop, m+w, tTop, bp); c.drawLine(m, tTop+hH, m+w, tTop+hH, bp)
    val c1=m; val w1=30f; val c2=c1+w1; val w2=200f; val c3=c2+w2; val w3=50f; val c4=c3+w3; val w4=60f; val c5=c4+w4; val w5=70f; val c6=c5+w5; val w6=50f; val c7=c6+w6; val w7=95f
    fun vLine(top:Float, bot:Float) { c.drawLine(c2,top,c2,bot,bp); c.drawLine(c3,top,c3,bot,bp); c.drawLine(c4,top,c4,bot,bp); c.drawLine(c5,top,c5,bot,bp); c.drawLine(c6,top,c6,bot,bp); c.drawLine(c7,top,c7,bot,bp) }
    vLine(tTop, tTop+hH)
    p.isFakeBoldText=true; p.textAlign=Paint.Align.CENTER
    c.drawText("SI", c1+w1/2, tTop+14, p); c.drawText("Description", c2+w2/2, tTop+14, p); c.drawText("HSN", c3+w3/2, tTop+14, p); c.drawText("Qty", c4+w4/2, tTop+14, p); c.drawText("Rate", c5+w5/2, tTop+14, p); c.drawText("Per", c6+w6/2, tTop+14, p); c.drawText("Amount", c7+w7/2, tTop+14, p)
    p.isFakeBoldText=false; var y=tTop+hH
    var totalInclusive=0.0; var totalTaxable=0.0; var totalCGST=0.0; var totalSGST=0.0; var grandTotal=0.0
    items.forEachIndexed { i, it ->
        val rowTotalInclusive = it.qty * it.rate
        val taxFactor = 1 + (it.taxRate / 100)
        val taxableRate = it.rate / taxFactor
        val taxableAmount = taxableRate * it.qty
        val taxAmount = rowTotalInclusive - taxableAmount
        totalTaxable += taxableAmount; totalCGST += (taxAmount / 2); totalSGST += (taxAmount / 2); grandTotal += rowTotalInclusive
        c.drawText("${i+1}", c1+w1/2, y+14, p); p.textAlign=Paint.Align.LEFT
        c.drawText(it.desc, c2+5, y+14, p)
        if(it.serial.isNotEmpty()) { val pSmall = Paint(p); pSmall.textSize=8f; c.drawText("Sr/IMEI: ${it.serial}", c2+5, y+24, pSmall) }
        p.textAlign=Paint.Align.CENTER
        c.drawText(it.hsn, c3+w3/2, y+14, p); c.drawText(it.qty.toString(), c4+w4/2, y+14, p)
        c.drawText(String.format("%.2f", taxableRate), c5+w5/2, y+14, p); c.drawText(it.unit, c6+w6/2, y+14, p)
        p.textAlign=Paint.Align.RIGHT; c.drawText(String.format("%.2f", taxableAmount), m+w-5, y+14, p); y+=20f
    }
    val fTop = m+h-200f; vLine(tTop+hH, fTop); c.drawLine(m, fTop, m+w, fTop, bp)
    y=fTop; val tX=c7; p.textAlign=Paint.Align.RIGHT
    fun row(l:String, v:String) { c.drawText(l, tX-10, y+14, p); c.drawText(v, m+w-5, y+14, p); c.drawLine(tX, y, tX, y+20, bp); c.drawLine(tX, y+20, m+w, y+20, bp); y+=20f }
    row("Total Value", String.format("%.2f", totalTaxable)); row("SGST", String.format("%.2f", totalSGST)); row("CGST", String.format("%.2f", totalCGST))
    p.isFakeBoldText=true; c.drawText("Grand Total", tX-10, y+14, p); c.drawText("₹ ${String.format("%.0f", grandTotal)}", m+w-5, y+14, p); c.drawLine(tX, y, tX, m+h, bp)
    val fY = fTop+20; p.textAlign=Paint.Align.LEFT; p.isFakeBoldText=false
    c.drawText("Amount in words: ${convertToWords(grandTotal.toLong())}", m+5, fY, p)
    if(sBank.isNotEmpty()) {
        val bankY = fY+40; c.drawLine(m, bankY, tX, bankY, bp)
        c.drawText("Bank Details:", m+5, bankY+15, p)
        p.isFakeBoldText=true; c.drawText("$sBank, IFSC: $sIfsc", m+5, bankY+30, p)
    }
    val decY = fY+80; c.drawLine(m, decY, tX, decY, bp)
    p.isFakeBoldText=false; p.textSize=8f
    c.drawText("Declaration: We declare this invoice shows the actual price of goods.", m+5, decY+12, p)
    c.drawText("Subject to $sJuris Jurisdiction", m+5, decY+22, p)
    p.isFakeBoldText=true; c.drawText("GOODS ONCE SOLD CANNOT BE RETURNED", m+5, decY+35, p)
    val sigY = decY; c.drawLine(c5, sigY, tX, sigY, bp)
    p.textSize=10f; p.isFakeBoldText=true; p.textAlign=Paint.Align.CENTER
    val signCenterX = (c5 + tX) / 2
    c.drawText("For, $sName", signCenterX, sigY+15, p); c.drawText("Auth. Signatory", signCenterX, m+h-10, p)
    p.isFakeBoldText=false; p.textSize=8f
    c.drawText("Computer generated invoice. No signature required.", midX, m+h+15, p)
    doc.finishPage(page)
    val f = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "PenKhata_${System.currentTimeMillis()}.pdf")
    doc.writeTo(FileOutputStream(f)); doc.close()
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).apply { setDataAndType(FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", f), "application/pdf"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Open Invoice"))
}
