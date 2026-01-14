package com.example.penkhata

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LedgerScreen() {
    val ctx = LocalContext.current
    var party by remember { mutableStateOf("") }; var type by remember { mutableStateOf("DEBIT") }
    var amt by remember { mutableStateOf("") }; var desc by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(loadLedger(ctx)) }

    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("LEDGER BOOK", fontSize=24.sp, fontWeight=FontWeight.Bold)
        Card(Modifier.padding(vertical=10.dp)) { Column(Modifier.padding(10.dp)) {
            OutlinedTextField(party, {party=it}, label={Text("Party Name")}, modifier=Modifier.fillMaxWidth())
            Row { Button(onClick={type="DEBIT"}, colors=ButtonDefaults.buttonColors(containerColor=if(type=="DEBIT") Color.Red else Color.Gray), modifier=Modifier.weight(1f)){Text("DEBIT (-)")}; Spacer(Modifier.width(5.dp)); Button(onClick={type="CREDIT"}, colors=ButtonDefaults.buttonColors(containerColor=if(type=="CREDIT") Color.Green else Color.Gray), modifier=Modifier.weight(1f)){Text("CREDIT (+)")} }
            OutlinedTextField(amt, {amt=it}, label={Text("Amount")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), modifier=Modifier.fillMaxWidth())
            OutlinedTextField(desc, {desc=it}, label={Text("Note")}, modifier=Modifier.fillMaxWidth())
            Button(onClick={ if(party.isNotEmpty() && amt.isNotEmpty()) { val e = LedgerEntry(System.currentTimeMillis(), SimpleDateFormat("dd-MM").format(Date()), party, type, amt.toDouble(), desc); saveLedgerEntry(ctx, e); entries = loadLedger(ctx); party=""; amt=""; desc="" } }, modifier=Modifier.fillMaxWidth()) { Text("ADD ENTRY") }
        }}
        entries.reversed().forEach {
            Row(Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                Column { Text(it.party, fontWeight=FontWeight.Bold); Text(it.desc, fontSize=10.sp) }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) { Text(it.type, color=if(it.type=="DEBIT") Color.Red else Color.Green, fontSize=10.sp); Text("₹${it.amount}", fontWeight=FontWeight.Bold) }
            }
            Divider()
        }
        Spacer(Modifier.height(50.dp))
    }
}

@Composable
fun GSTReportScreen() {
    val ctx = LocalContext.current; val data = remember { loadGstData(ctx) }
    Column(Modifier.padding(16.dp)) {
        Text("GST DASHBOARD", fontSize=24.sp, fontWeight=FontWeight.Bold)
        Card(Modifier.padding(vertical=10.dp)) { Column(Modifier.padding(16.dp)) {
            Text("Current Month Summary", fontWeight=FontWeight.Bold)
            Divider(Modifier.padding(vertical=5.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Total Sales:"); Text("₹${data["total"]}") }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Total Tax:"); Text("₹${data["tax"]}") }
        }}
    }
}

@Composable
fun FileHistoryScreen() {
    val ctx = LocalContext.current; val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
    var files by remember { mutableStateOf(dir?.listFiles()?.filter { it.extension == "pdf" }?.sortedByDescending { it.lastModified() } ?: emptyList()) }
    LazyColumn(Modifier.padding(16.dp)) {
        item { Text("SAVED FILES", fontSize=24.sp, fontWeight=FontWeight.Bold) }
        items(files) { file ->
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable {
                    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
                    ctx.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/pdf"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) })
                }) { Text(file.name, fontWeight=FontWeight.Bold); Text(SimpleDateFormat("dd MMM HH:mm").format(Date(file.lastModified())), fontSize=10.sp) }
                IconButton(onClick={ if(file.delete()) files = dir?.listFiles()?.filter { it.extension == "pdf" }?.sortedByDescending { it.lastModified() } ?: emptyList() }) { Icon(Icons.Default.Delete, "Del", tint=Color.Red) }
            }
            Divider()
        }
        item { Spacer(Modifier.height(50.dp)) }
    }
}

@Composable
fun SettingsScreen(currentPin: String, onPinSave: (String)->Unit) {
    val ctx = LocalContext.current; val prefs = remember { ctx.getSharedPreferences("penkhata_data", Context.MODE_PRIVATE) }
    var sName by remember { mutableStateOf(prefs.getString("sName", "") ?: "") }; var sAddr by remember { mutableStateOf(prefs.getString("sAddr", "") ?: "") }
    var sGst by remember { mutableStateOf(prefs.getString("sGst", "") ?: "") }; var sBank by remember { mutableStateOf(prefs.getString("sBank", "") ?: "") }
    var sIfsc by remember { mutableStateOf(prefs.getString("sIfsc", "") ?: "") }; var sJuris by remember { mutableStateOf(prefs.getString("sJuris", "") ?: "") }
    var defTax by remember { mutableStateOf(prefs.getString("defTax", "18") ?: "18") }; var newPin by remember { mutableStateOf(currentPin) }
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("SETTINGS", fontSize=24.sp, fontWeight=FontWeight.Bold)
        OutlinedTextField(newPin, {newPin=it}, label={Text("Set Login PIN")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword), modifier=Modifier.fillMaxWidth())
        OutlinedTextField(sName, {sName=it}, label={Text("Firm Name")}, modifier=Modifier.fillMaxWidth())
        OutlinedTextField(sAddr, {sAddr=it}, label={Text("Address")}, modifier=Modifier.fillMaxWidth())
        OutlinedTextField(sGst, {sGst=it}, label={Text("GSTIN")}, modifier=Modifier.fillMaxWidth())
        OutlinedTextField(sBank, {sBank=it}, label={Text("Bank Name & Acc")}, modifier=Modifier.fillMaxWidth())
        OutlinedTextField(sIfsc, {sIfsc=it}, label={Text("IFSC")}, modifier=Modifier.fillMaxWidth())
        OutlinedTextField(sJuris, {sJuris=it}, label={Text("Jurisdiction")}, modifier=Modifier.fillMaxWidth())
        OutlinedTextField(defTax, {defTax=it}, label={Text("Default Tax %")}, keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), modifier=Modifier.fillMaxWidth())
        Button(onClick={ prefs.edit().putString("sName",sName).putString("sAddr",sAddr).putString("sGst",sGst).putString("sBank",sBank).putString("sIfsc",sIfsc).putString("sJuris",sJuris).putString("defTax",defTax).apply(); onPinSave(newPin); Toast.makeText(ctx,"Saved",Toast.LENGTH_SHORT).show() }, modifier=Modifier.fillMaxWidth()) { Text("SAVE ALL") }
        Spacer(Modifier.height(60.dp))
    }
}
