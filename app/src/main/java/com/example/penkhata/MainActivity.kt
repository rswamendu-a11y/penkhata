package com.example.penkhata

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment // <--- FIXED IMPORT
import java.text.SimpleDateFormat
import java.util.*

// Data Models
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
    val prefs = remember { ctx.getSharedPreferences("penkhata_data", android.content.Context.MODE_PRIVATE) }
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
        Text("PENKHATA LOCKED", fontWeight = FontWeight.Bold, fontSize = 24.sp)
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
