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
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class InvItem(val desc: String, val serial: String, val hsn: String, val qty: Double, val rate: Double, val unit: String, val taxRate: Double)
data class LedgerEntry(val id: Long, val date: String, val party: String, val type: String, val amount: Double, val desc: String)

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
            NavigationBarItem(selected = currentScreen=="dashboard", onClick = { currentScreen="dashboard" }, icon = { Icon(Icons.Default.EditNote, "Bill") }, label = { Text("Entry") })
            NavigationBarItem(selected = currentScreen=="ledger", onClick = { currentScreen="ledger" }, icon = { Icon(Icons.Default.AccountBalanceWallet, "Ledger") }, label = { Text("Ledger") })
            NavigationBarItem(selected = currentScreen=="reports", onClick = { currentScreen="reports" }, icon = { Icon(Icons.Default.Assessment, "GST") }, label = { Text("GST") })
            NavigationBarItem(selected = currentScreen=="history", onClick = { currentScreen="history" }, icon = { Icon(Icons.Default.History, "Files") }, label = { Text("Files") })
            NavigationBarItem(selected = currentScreen=="settings", onClick = { currentScreen="settings" }, icon = { Icon(Icons.Default.Settings, "Set") }, label = { Text("Set") })
        }}
    ) { p -> Box(Modifier.padding(p)) { when(currentScreen) {
        "dashboard" -> InvoiceScreen(false)
        "ledger" -> LedgerScreen()
        "reports" -> GSTReportScreen()
        "history" -> FileHistoryScreen()
        else -> SettingsScreen(savedPin) { new -> prefs.edit().putString("appPin", new).apply(); savedPin = new }
    }}}
}

@Composable
fun LoginScreen(pin: String, unlock: () -> Unit) {
    var input by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("LOCKED", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(value=input, onValueChange={input=it}, visualTransformation=PasswordVisualTransformation(), keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword), modifier=Modifier.padding(20.dp))
        Button(onClick={if(input==pin) unlock()}) { Text("UNLOCK") }
    }
}

@Composable
fun InvoiceScreen(isQuote: Boolean) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("penkhata_data", android.content.Context.MODE_PRIVATE) }
    var invNo by remember { mutableStateOf("1") }; var date by remember { mutableStateOf(SimpleDateFormat("dd-MMM-yyyy").format(Date())) }
    var payMode by remember { mutableStateOf("") }; var delNote by remember { mutableStateOf("") }
    var bName by remember { mutableStateOf("") }; var bAddr by remember { mutableStateOf("") }
    var bGst by remember { mutableStateOf("") }; var bState by remember { mutableStateOf("") }
    var iDesc by remember { mutableStateOf("") }; var iSerial by remember { mutableStateOf("") }
    var iHsn by remember { mutableStateOf("") }; var iQty by remember { mutableStateOf("1") }
    var iRate by remember { mutableStateOf("") }; var iUnit by remember { mutableStateOf("pcs") }
    var iTax by remember { mutableStateOf(prefs.getString("defTax", "18") ?: "18") }
    var items by remember { mutableStateOf(listOf<InvItem>()) }

    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("New Invoice", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { invNo="1"; bName=""; bAddr=""; bGst=""; items=emptyList(); Toast.makeText(ctx, "Cleared", Toast.LENGTH_SHORT).show() }) { Text("RESET", color = Color.Red) }
        }
        Card(Modifier.padding(vertical=5.dp)) { Column(Modifier.padding(10.dp)) {
            Row { OutlinedTextField(invNo, {invNo=it}, label={Text("Inv No")}, modifier=Modifier.weight(1f)); Spacer(Modifier.width(5.dp)); OutlinedTextField(date, {date=it}, label={Text("Date")}, modifier=Modifier.weight(1f)) }
            OutlinedTextField(payMode, {payMode=it}, label={Text("Pay Mode")}, modifier=Modifier.fillMaxWidth())
            OutlinedTextField(delNote, {delNote=it}, label={Text("Delivery Note")}, modifier=Modifier.fillMaxWidth())
        }}
        Card(Modifier.padding(vertical=5.dp)) { Column(Modifier.padding(10.dp)) {
            Text("Buyer Details", fontWeight=FontWeight.Bold)
            OutlinedTextField(bName, {bName=it}, label={Text("Name")}, modifier=Modifier.fillMaxWidth())
            OutlinedTextField(bAddr, {bAddr=it}, label={Text("Address/Phone")}, modifier=Modifier.fillMaxWidth())
            Row { OutlinedTextField(bGst, {bGst=it}, label={Text("GSTIN")}, modifier=Modifier.weight(1f)); Spacer(Modifier.width(5.dp)); OutlinedTextField(bState, {bState=it}, label={Text("State Code")}, modifier=Modifier.weight(1f)) }
        }}
        Card(Modifier.padding(vertical=5.dp)) { Column(Modifier.padding(10.dp)) {
            Text("Add Item", fontWeight=FontWeight.Bold)
            OutlinedTextField(iDesc, {iDesc=it}, label={Text("Item Name")}, modifier=Modifier.fillMaxWidth())
            OutlinedTextField(iSerial, {iSerial=it}, label={Text("Serial / IMEI")}, modifier=Modifier.fillMaxWidth())
            Row { OutlinedTextField(iHsn, {iHsn=it}, label={Text("HSN")}, modifier=Modifier.weight(1f)); Spacer(Modifier.width(5.dp)); OutlinedTextField(iUnit, {iUnit=it}, label={Text("Unit")}, modifier=Modifier.weight(1f)) }
            Row { OutlinedTextField(iQty, {iQty=it}, label={Text("Qty")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), modifier=Modifier.weight(1f)); Spacer(Modifier.width(5.dp)); OutlinedTextField(iRate, {iRate=it}, label={Text("Rate (Inc. Tax)")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), modifier=Modifier.weight(1f)) }
            Row(Modifier.padding(top=5.dp)) { OutlinedTextField(iTax, {iTax=it}, label={Text("GST %")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), modifier=Modifier.weight(1f)); Spacer(Modifier.width(5.dp)); Button(onClick={ if(iDesc.isNotEmpty()){ items=items+InvItem(iDesc,iSerial,iHsn,iQty.toDoubleOrNull()?:1.0,iRate.toDoubleOrNull()?:0.0,iUnit,iTax.toDoubleOrNull()?:18.0); iDesc=""; iSerial=""; iQty="1"; iRate="" } }, modifier=Modifier.weight(1f).height(55.dp)) { Text("ADD") } }
        }}
        items.forEachIndexed{ idx, it -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("${idx+1}. ${it.desc} (${it.qty} ${it.unit})", Modifier.weight(1f)); IconButton(onClick={ val m=items.toMutableList(); m.removeAt(idx); items=m }) { Icon(Icons.Default.Delete,"Del",tint=Color.Red) } }; Divider() }
        Spacer(Modifier.height(20.dp))
        Button(onClick={
            val sName = prefs.getString("sName", "") ?: ""
            if(sName.isEmpty()) Toast.makeText(ctx, "Setup Settings First!", Toast.LENGTH_SHORT).show()
            else {
                if(!isQuote) saveInvoiceData(ctx, invNo, bName, bGst, items)
                createPdf(ctx, isQuote, invNo, date, payMode, delNote, sName, prefs.getString("sAddr","")?:"", prefs.getString("sGst","")?:"", prefs.getString("sBank","")?:"", prefs.getString("sIfsc","")?:"", prefs.getString("sJuris","")?:"", bName, bAddr, bGst, bState, items)
            }
        }, modifier=Modifier.fillMaxWidth(), colors=ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("GENERATE PDF") }
        Spacer(Modifier.height(60.dp))
    }
}

// --- SECONDARY SCREENS ---
@Composable fun LedgerScreen() { val ctx=LocalContext.current; var p by remember{mutableStateOf("")}; var t by remember{mutableStateOf("DEBIT")}; var a by remember{mutableStateOf("")}; var d by remember{mutableStateOf("")}; var e by remember{mutableStateOf(loadLedger(ctx))}; Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())){ Text("LEDGER",fontSize=24.sp,fontWeight=FontWeight.Bold); Card(Modifier.padding(vertical=10.dp)){Column(Modifier.padding(10.dp)){ OutlinedTextField(p,{p=it},label={Text("Party")},modifier=Modifier.fillMaxWidth()); Row{Button(onClick={t="DEBIT"},colors=ButtonDefaults.buttonColors(containerColor=if(t=="DEBIT")Color.Red else Color.Gray),modifier=Modifier.weight(1f)){Text("DEBIT")}; Spacer(Modifier.width(5.dp)); Button(onClick={t="CREDIT"},colors=ButtonDefaults.buttonColors(containerColor=if(t=="CREDIT")Color.Green else Color.Gray),modifier=Modifier.weight(1f)){Text("CREDIT")}}; OutlinedTextField(a,{a=it},label={Text("Amount")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth()); OutlinedTextField(d,{d=it},label={Text("Note")},modifier=Modifier.fillMaxWidth()); Button(onClick={if(p.isNotEmpty()&&a.isNotEmpty()){val en=LedgerEntry(System.currentTimeMillis(),SimpleDateFormat("dd-MM").format(Date()),p,t,a.toDouble(),d); saveLedgerEntry(ctx,en); e=loadLedger(ctx); p="";a=""}},modifier=Modifier.fillMaxWidth()){Text("ADD")}}}; e.reversed().forEach{Row(Modifier.padding(8.dp).fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(it.party,fontWeight=FontWeight.Bold);Text(it.desc,fontSize=10.sp)}; Column(horizontalAlignment=Alignment.End){Text(it.type,color=if(it.type=="DEBIT")Color.Red else Color.Green,fontSize=10.sp);Text("₹${it.amount}",fontWeight=FontWeight.Bold)}};Divider()}; Spacer(Modifier.height(50.dp))}}
@Composable fun GSTReportScreen() { val ctx=LocalContext.current; val d=remember{loadGstData(ctx)}; Column(Modifier.padding(16.dp)){ Text("GST DASHBOARD",fontSize=24.sp,fontWeight=FontWeight.Bold); Card(Modifier.padding(vertical=10.dp)){Column(Modifier.padding(16.dp)){ Text("Summary",fontWeight=FontWeight.Bold); Divider(Modifier.padding(vertical=5.dp)); Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text("Sales:");Text("₹${d["total"]}")}; Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text("Tax:");Text("₹${d["tax"]}")}}}}}
@Composable fun FileHistoryScreen() { val ctx=LocalContext.current; val d=ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS); var f by remember{mutableStateOf(d?.listFiles()?.filter{it.extension=="pdf"}?.sortedByDescending{it.lastModified()}?:emptyList())}; LazyColumn(Modifier.padding(16.dp)){items(f){fl->Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f).clickable{ctx.startActivity(Intent(Intent.ACTION_VIEW).apply{setDataAndType(FileProvider.getUriForFile(ctx,"${ctx.packageName}.provider",fl),"application/pdf");addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)})}){Text(fl.name,fontWeight=FontWeight.Bold);Text(SimpleDateFormat("dd MMM HH:mm").format(Date(fl.lastModified())),fontSize=10.sp)}; IconButton(onClick={if(fl.delete())f=d?.listFiles()?.filter{it.extension=="pdf"}?.sortedByDescending{it.lastModified()}?:emptyList()}){Icon(Icons.Default.Delete,"Del",tint=Color.Red)}};Divider()}} }
@Composable fun SettingsScreen(currentPin: String, onPinSave: (String)->Unit) { val ctx=LocalContext.current; val prefs=remember{ctx.getSharedPreferences("penkhata_data",Context.MODE_PRIVATE)}; var sName by remember{mutableStateOf(prefs.getString("sName","")?:"")}; var sAddr by remember{mutableStateOf(prefs.getString("sAddr","")?:"")}; var sGst by remember{mutableStateOf(prefs.getString("sGst","")?:"")}; var sBank by remember{mutableStateOf(prefs.getString("sBank","")?:"")}; var sIfsc by remember{mutableStateOf(prefs.getString("sIfsc","")?:"")}; var sJuris by remember{mutableStateOf(prefs.getString("sJuris","")?:"")}; var defTax by remember{mutableStateOf(prefs.getString("defTax","18")?:"18")}; var newPin by remember{mutableStateOf(currentPin)}; Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())){Text("SETTINGS",fontSize=24.sp,fontWeight=FontWeight.Bold); OutlinedTextField(newPin,{newPin=it},label={Text("Set Login PIN")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),modifier=Modifier.fillMaxWidth()); OutlinedTextField(sName,{sName=it},label={Text("Firm Name")},modifier=Modifier.fillMaxWidth()); OutlinedTextField(sAddr,{sAddr=it},label={Text("Address")},modifier=Modifier.fillMaxWidth()); OutlinedTextField(sGst,{sGst=it},label={Text("GSTIN")},modifier=Modifier.fillMaxWidth()); OutlinedTextField(sBank,{sBank=it},label={Text("Bank Name & Acc")},modifier=Modifier.fillMaxWidth()); OutlinedTextField(sIfsc,{sIfsc=it},label={Text("IFSC")},modifier=Modifier.fillMaxWidth()); OutlinedTextField(sJuris,{sJuris=it},label={Text("Jurisdiction")},modifier=Modifier.fillMaxWidth()); OutlinedTextField(defTax,{defTax=it},label={Text("Default Tax %")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth()); Button(onClick={prefs.edit().putString("sName",sName).putString("sAddr",sAddr).putString("sGst",sGst).putString("sBank",sBank).putString("sIfsc",sIfsc).putString("sJuris",sJuris).putString("defTax",defTax).apply(); onPinSave(newPin); Toast.makeText(ctx,"Saved",Toast.LENGTH_SHORT).show()},modifier=Modifier.fillMaxWidth()){Text("SAVE")}; Spacer(Modifier.height(60.dp))} }

// --- LOGIC ---
fun loadGstData(ctx: Context): Map<String,String> { var total=0.0; var tax=0.0; ctx.filesDir.listFiles()?.filter{it.name.startsWith("inv_")}?.forEach{val j=JSONObject(it.readText()); total+=j.getDouble("total"); tax+=j.getDouble("tax")}; return mapOf("total" to String.format("%.2f",total),"tax" to String.format("%.2f",tax)) }
fun saveInvoiceData(ctx: Context, invNo: String, bName: String, bGst: String, items: List<InvItem>) { val j=JSONObject(); j.put("d",Date().toString()); j.put("inv",invNo); var t=0.0; var x=0.0; items.forEach{val v=it.qty*it.rate; t+=v; x+=v-(v/(1+it.taxRate/100))}; j.put("total",t); j.put("tax",x); File(ctx.filesDir,"inv_${System.currentTimeMillis()}.json").writeText(j.toString()) }
fun saveLedgerEntry(ctx: Context, e: LedgerEntry) { val j=JSONObject(); j.put("date",e.date); j.put("party",e.party); j.put("type",e.type); j.put("amt",e.amount); j.put("desc",e.desc); File(ctx.filesDir,"led_${System.currentTimeMillis()}.json").writeText(j.toString()) }
fun loadLedger(ctx: Context): List<LedgerEntry> { val l=mutableListOf<LedgerEntry>(); ctx.filesDir.listFiles()?.filter{it.name.startsWith("led_")}?.forEach{val j=JSONObject(it.readText()); l.add(LedgerEntry(0,j.getString("date"),j.getString("party"),j.getString("type"),j.getDouble("amt"),j.getString("desc")))}; return l }
fun convertToWords(num: Long): String {
    if(num==0L) return "Zero Rupees Only"
    val u=arrayOf("","One","Two","Three","Four","Five","Six","Seven","Eight","Nine","Ten","Eleven","Twelve","Thirteen","Fourteen","Fifteen","Sixteen","Seventeen","Eighteen","Nineteen")
    val t=arrayOf("","","Twenty","Thirty","Forty","Fifty","Sixty","Seventy","Eighty","Ninety")
    fun rec(n:Long):String{
        if(n<20) return u[n.toInt()]
        if(n<100) return t[(n/10).toInt()] + " " + u[(n%10).toInt()]
        if(n<1000) return u[(n/100).toInt()] + " Hundred " + rec(n%100)
        if(n<100000) return rec(n/1000) + " Thousand " + rec(n%1000)
        if(n<10000000) return rec(n/100000) + " Lakh " + rec(n%100000)
        return rec(n/10000000) + " Crore " + rec(n%10000000)
    }
    return rec(num).trim() + " Rupees Only"
}
fun drawMultiLineText(c: Canvas, text: String, x: Float, y: Float, p: Paint, width: Float) {
    if(p.measureText(text)<width){c.drawText(text,x,y,p);return}
    val words=text.split(" "); var line=""; var cy=y
    for(w in words){ if(p.measureText(line+w)<width) line+="$w " else { c.drawText(line,x,cy,p); line="$w "; cy+=p.textSize+2f } }
    if(line.isNotEmpty()) c.drawText(line,x,cy,p)
}

fun createPdf(ctx: Context, isQuote: Boolean, invNo: String, date: String, payMode: String, delNote: String,
              sName: String, sAddr: String, sGst: String, sBank: String, sIfsc: String, sJuris: String,
              bName: String, bAddr: String, bGst: String, bState: String, items: List<InvItem>) {
    val doc = PdfDocument(); val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val c = page.canvas; val p = Paint(); val bp = Paint().apply{style=Paint.Style.STROKE; strokeWidth=1f}
    val m=20f; val w=555f; val h=800f; val midX=m+w/2
    c.drawRect(m, m, m+w, m+h, bp)
    p.isFakeBoldText=true; p.textAlign=Paint.Align.CENTER; p.textSize=14f
    c.drawText(if(isQuote) "QUOTATION" else "TAX INVOICE", midX, m+15, p)
    c.drawLine(m, m+20, m+w, m+20, bp)
    val r1=m+20; val rH=145f // HEIGHT INCREASED TO 145f
    c.drawLine(midX, r1, midX, r1+rH, bp); c.drawLine(m, r1+rH/2, m+w, r1+rH/2, bp)
    p.textAlign=Paint.Align.LEFT; p.textSize=12f; p.isFakeBoldText=true
    c.drawText(sName, m+5, r1+15, p); p.isFakeBoldText=false; p.textSize=10f
    c.drawText(sAddr, m+5, r1+30, p); c.drawText("GSTIN: $sGst", m+5, r1+45, p)
    c.drawText("Buyer: $bName", m+5, r1+rH/2+15, p)
    drawMultiLineText(c, bAddr, m+5, r1+rH/2+30, p, w/2-10)
    if(bGst.isNotEmpty()) c.drawText("GSTIN: $bGst", m+5, r1+rH/2+60, p)
    if(bState.isNotEmpty()) c.drawText("State: $bState", m+5, r1+rH-12, p)
    val qX=midX+w/4; val line1=r1+rH/4; val line2=r1+2*rH/4; val line3=r1+3*rH/4
    c.drawLine(midX,line1,m+w,line1,bp); c.drawLine(midX,line2,m+w,line2,bp); c.drawLine(midX,line3,m+w,line3,bp); c.drawLine(qX,r1,qX,r1+rH,bp)
    fun cell(l:String, v:String, x:Float, y:Float) { c.drawText(l,x+2,y+12,p); p.isFakeBoldText=true; c.drawText(v,qX+2,y+12,p); p.isFakeBoldText=false }
    cell("No",invNo,midX,r1); cell("Date",date,midX,line1); cell("Note",delNote,midX,line2); cell("Terms",payMode,midX,line3)
    val tTop=r1+rH; val hH=20f; c.drawLine(m,tTop,m+w,tTop,bp); c.drawLine(m,tTop+hH,m+w,tTop+hH,bp)
    val c1=m;val w1=30f;val c2=c1+w1;val w2=200f;val c3=c2+w2;val w3=50f;val c4=c3+w3;val w4=60f;val c5=c4+w4;val w5=70f;val c6=c5+w5;val w6=50f;val c7=c6+w6;val w7=95f
    fun vLine(top:Float,bot:Float){c.drawLine(c2,top,c2,bot,bp);c.drawLine(c3,top,c3,bot,bp);c.drawLine(c4,top,c4,bot,bp);c.drawLine(c5,top,c5,bot,bp);c.drawLine(c6,top,c6,bot,bp);c.drawLine(c7,top,c7,bot,bp)}
    vLine(tTop,tTop+hH); p.isFakeBoldText=true; p.textAlign=Paint.Align.CENTER
    c.drawText("SI",c1+w1/2,tTop+14,p); c.drawText("Desc",c2+w2/2,tTop+14,p); c.drawText("HSN",c3+w3/2,tTop+14,p); c.drawText("Qty",c4+w4/2,tTop+14,p); c.drawText("Rate",c5+w5/2,tTop+14,p); c.drawText("Per",c6+w6/2,tTop+14,p); c.drawText("Amt",c7+w7/2,tTop+14,p)
    p.isFakeBoldText=false; var y=tTop+hH; var gTotal=0.0; var totalTaxable=0.0; var totalTax=0.0
    items.forEachIndexed{i,it->
        val rh=if(it.serial.isNotEmpty()) 30f else 20f
        val rowInc=it.qty*it.rate; val taxF=1+(it.taxRate/100); val rowBase=rowInc/taxF; val rowTax=rowInc-rowBase
        gTotal+=rowInc; totalTaxable+=rowBase; totalTax+=rowTax
        c.drawText("${i+1}",c1+w1/2,y+14,p); p.textAlign=Paint.Align.LEFT
        c.drawText(it.desc,c2+5,y+14,p); if(it.serial.isNotEmpty()){val ps=Paint(p);ps.textSize=8f;c.drawText("SR/IMEI: ${it.serial}",c2+5,y+26,ps)}
        p.textAlign=Paint.Align.CENTER; c.drawText(it.hsn,c3+w3/2,y+14,p); c.drawText(it.qty.toString(),c4+w4/2,y+14,p)
        c.drawText(String.format("%.2f",rowBase),c5+w5/2,y+14,p); c.drawText(it.unit,c6+w6/2,y+14,p)
        p.textAlign=Paint.Align.RIGHT; c.drawText(String.format("%.2f",rowBase*it.qty),m+w-5,y+14,p); y+=rh
    }
    val fTop=m+h-200f; vLine(tTop+hH,fTop); c.drawLine(m,fTop,m+w,fTop,bp); y=fTop; val tX=c7; p.textAlign=Paint.Align.RIGHT
    fun row(l:String,v:String){c.drawText(l,tX-10,y+14,p);c.drawText(v,m+w-5,y+14,p);c.drawLine(tX,y,tX,y+20,bp);c.drawLine(tX,y+20,m+w,y+20,bp);y+=20f}
    row("Total Value",String.format("%.2f",totalTaxable)); row("SGST",String.format("%.2f",totalTax/2)); row("CGST",String.format("%.2f",totalTax/2))
    p.isFakeBoldText=true; c.drawText("Grand Total",tX-10,y+14,p); c.drawText("₹ ${String.format("%.0f",gTotal)}",m+w-5,y+14,p); c.drawLine(tX,y,tX,m+h,bp)
    val fY=fTop+20; p.textAlign=Paint.Align.LEFT; p.isFakeBoldText=false
    c.drawText("Amount: ${convertToWords(gTotal.toLong())}",m+5,fY,p)
    if(sBank.isNotEmpty()){ val by=fY+40; c.drawLine(m,by,tX,by,bp); c.drawText("Bank Details:",m+5,by-25,p); p.isFakeBoldText=true; c.drawText("$sBank | $sIfsc",m+5,by-10,p) }
    val dy=fY+80; c.drawLine(m,dy,tX,dy,bp); p.isFakeBoldText=false; p.textSize=8f
    c.drawText("Declaration: We declare this invoice shows the actual price of goods.",m+5,dy+12,p)
    c.drawText("Subject to $sJuris Jurisdiction",m+5,dy+22,p); p.isFakeBoldText=true
    c.drawText("GOODS ONCE SOLD CANNOT BE RETURNED",m+5,dy+35,p)
    val sigY=dy; c.drawLine(c5,sigY,m+w,sigY,bp)
    p.textSize=10f; p.isFakeBoldText=true; p.textAlign=Paint.Align.CENTER
    val sigX = (c5 + m + w) / 2
    c.drawText("For, $sName",sigX,sigY+50,p) // Y INCREASED TO +50
    c.drawText("Auth. Signatory",sigX,m+h-10,p)
    p.isFakeBoldText=false; p.textSize=8f
    c.drawText("Computer generated invoice.",midX,m+h+15,p)
    doc.finishPage(page); val n=if(isQuote)"Quote" else "Inv"; val f=File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),"${n}_${System.currentTimeMillis()}.pdf")
    doc.writeTo(FileOutputStream(f)); doc.close()
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).apply{setDataAndType(FileProvider.getUriForFile(ctx,"${ctx.packageName}.provider",f),"application/pdf");addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"View"))
}
