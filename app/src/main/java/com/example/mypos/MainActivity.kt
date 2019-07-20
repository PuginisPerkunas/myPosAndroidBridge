package com.example.mypos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.text.SimpleDateFormat
import java.util.*
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mypos.slavesdk.*
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.toast


class MainActivity : AppCompatActivity() {

    var data : TicketData? = null
    val MY_PERMISSIONS_REQUEST_LOCATION = 99

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if(!checkCoarsePermission()){
            btnTryAgain.visibility = View.INVISIBLE
            progressBar1.visibility = View.INVISIBLE
            btnPerrmision.setOnClickListener {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION
                )
            }
        }else{
            btnPerrmision.visibility = View.INVISIBLE
        }
        data = getIncomingData()
        reprintprint()
        btnTryAgain.setOnClickListener {
            reprintprint()
        }

        POSHandler.getInstance().setPOSInfoListener(object : POSInfoListener{
            override fun onTransactionComplete(transactionData: TransactionData?) {
                Log.e("posInfo", transactionData.toString())
            }

            override fun onPOSInfoReceived(command: Int, status: Int, description: String?) {
                Log.e("posInfo", description)
                Log.e("posInfo", "command $command")
                Log.e("posInfo", "status $status")
                if(status == POSHandler.POS_STATUS_SUCCESS_PRINT_RECEIPT){
                    finish()
                }
            }

        })

        POSHandler.getInstance().setPOSReadyListener {
          if(data != null){
                printTicket(data!!)
            }else{
                toast("Data was null")
            }
        }
    }

    private fun reprintprint(){
        POSHandler.setConnectionType(ConnectionType.BLUETOOTH)
        POSHandler.setApplicationContext(this)
        if(data != null){
            if(POSHandler.getInstance().isConnected){
                Log.e("posInfo", "connected")
                printTicket(data!!)
            }else{
                Log.e("posInfo", "disconnected")
                POSHandler.getInstance().connectDevice(this)
            }
        }else{
            toast("Data was null")
            //finish()
        }
    }

    private fun checkCoarsePermission(): Boolean {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun getIncomingData(): TicketData? {
        val ticketData = TicketData()
        if(intent.getStringExtra(TicketData.COMPANY_NAME_EXTRA) != null){
            ticketData.companyName = intent.getStringExtra(TicketData.COMPANY_NAME_EXTRA)
        }else {
            toast("COMPANY_NAME_EXTRA null")
            return null
        }
        if(intent.getStringExtra(TicketData.COMPANY_CODE) != null){
            ticketData.companyCode = intent.getStringExtra(TicketData.COMPANY_CODE)
        }else {
            toast("COMPANY_CODE null")
            return null
        }
        if(intent.getStringExtra(TicketData.COMPANY_PHONE_NUMBER) != null){
            ticketData.companyPhoneNumber = intent.getStringExtra(TicketData.COMPANY_PHONE_NUMBER)
        }else {
            toast("COMPANY_PHONE_NUMBER null")
            return null
        }
        if(intent.getStringExtra(TicketData.PRICE_NUMBERS) != null){
            ticketData.priceNumbers = intent.getStringExtra(TicketData.PRICE_NUMBERS)
        }else {
            toast("PRICE_NUMBERS null")
            return null
        }
        if(intent.getStringExtra(TicketData.PRICE_WORDS) != null){
            ticketData.priceWords = intent.getStringExtra(TicketData.PRICE_WORDS)
        }else {
            toast("PRICE_WORDS null")
            return null
        }
        if(intent.getStringExtra(TicketData.PRODUCTS_TYPE) != null){
            ticketData.productsType = intent.getStringExtra(TicketData.PRODUCTS_TYPE)
        }else {
            toast("PRODUCTS_TYPE null")
            return null
        }
        if(intent.getStringExtra(TicketData.TICKET_NUMBER) != null){
            ticketData.ticketNumber = intent.getStringExtra(TicketData.TICKET_NUMBER)
        }else {
            toast("TICKET_NUMBER null")
            return null
        }
        if(intent.getStringExtra(TicketData.TICKET_SERIES) != null){
            ticketData.ticketSeries = intent.getStringExtra(TicketData.TICKET_SERIES)
        }else {
            toast("TICKET_SERIES null")
            return null
        }
        if(intent.getStringExtra(TicketData.PRODUCTS_LIST) != null){
            val json = intent.getStringExtra(TicketData.PRODUCTS_LIST)
            val listType = object : TypeToken<List<String>>() { }.type
            val list = Gson().fromJson<List<String>>(json, listType)
            ticketData.productsList = list.toMutableList()
        }else {
            toast("PRODUCTS_LIST null")
            return null
        }
        return ticketData
    }

    private fun printTicket(data : TicketData){
        Log.e("posInfo", "printing")
        val receipt = ReceiptData()
        receipt.addEmptyRow()
        receipt.addRow(
            data.companyName,
            ReceiptData.Align.LEFT,
            ReceiptData.FontSize.SINGLE
        )
        receipt.addRow(
            data.companyCode,
            ReceiptData.Align.LEFT,
            ReceiptData.FontSize.SINGLE
        )
        receipt.addRow(
            data.companyPhoneNumber,
            ReceiptData.Align.LEFT,
            ReceiptData.FontSize.SINGLE
        )
        receipt.addEmptyRow()
        receipt.addRow(
            "Pinigu priemimo  kvitas",
            ReceiptData.Align.CENTER,
            ReceiptData.FontSize.DOUBLE
        )
        receipt.addEmptyRow()
        receipt.addRow(
            "Serija ${data.ticketSeries}   Nr. ${data.ticketNumber}",
            ReceiptData.Align.LEFT,
            ReceiptData.FontSize.SINGLE
        )
        receipt.addEmptyRow()
        receipt.addRow(
            getCurrentDate(),
            ReceiptData.Align.CENTER,
            ReceiptData.FontSize.SINGLE
        )
        //Todo hardcoded list
        receipt.addEmptyRow()
        receipt.addRow(
            "Sumoketa uz: ${getFullProductsString(data.productsList)}",
            ReceiptData.Align.CENTER,
            ReceiptData.FontSize.SINGLE
        )
        receipt.addEmptyRow()
        receipt.addRow(
            "Sumoketa suma skaiciais: ${data.priceNumbers}",
            ReceiptData.Align.LEFT,
            ReceiptData.FontSize.SINGLE
        )
        receipt.addEmptyRow()
        receipt.addRow(
            "Sumoketa suma zodziais: ${data.priceWords}",
            ReceiptData.Align.LEFT,
            ReceiptData.FontSize.SINGLE
        )
        receipt.addEmptyRow()
        receipt.addRow(
            "Pinigus sumokejau: ________________________",
            ReceiptData.Align.LEFT,
            ReceiptData.FontSize.SINGLE
        )
        receipt.addEmptyRow()
        receipt.addRow(
            "Pinigus gavau: __________________________",
            ReceiptData.Align.LEFT,
            ReceiptData.FontSize.SINGLE
        )
        receipt.addEmptyRow()
        receipt.addEmptyRow()
        receipt.addEmptyRow()
        receipt.addEmptyRow()
        receipt.addEmptyRow()
        receipt.addEmptyRow()
        POSHandler.getInstance().printReceipt(receipt)
        Log.e("posInfo", "print done")

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun getFullProductsString(productsList: MutableList<String>): String {
        return TextUtils.join(", ", productsList)
    }

    private fun getCurrentDate() :String{
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy/M/dd hh:mm:ss", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    private fun testUri() : String {
        val ticketData = TicketData()
        ticketData.productsList.add("Vienas produktas")
        ticketData.productsList.add("Antras produktas")
        ticketData.productsList.add("Test")
        ticketData.productsList.add("Ilgo pavadinimo produktas")
        ticketData.productsList.add("mantiruote")
        ticketData.productsList.add("Bokstas")
        val listGson = Gson().toJson(ticketData.productsList)
        val intentForData = Intent("com.example.mypos")
        intentForData.addCategory(Intent.CATEGORY_DEFAULT)
        intentForData.addCategory(Intent.CATEGORY_BROWSABLE)
        intentForData.putExtra(TicketData.COMPANY_NAME_EXTRA,ticketData.companyName)
        intentForData.putExtra(TicketData.COMPANY_CODE,ticketData.companyCode)
        intentForData.putExtra(TicketData.COMPANY_PHONE_NUMBER,ticketData.companyPhoneNumber)
        intentForData.putExtra(TicketData.TICKET_SERIES,ticketData.ticketSeries)
        intentForData.putExtra(TicketData.TICKET_NUMBER,ticketData.ticketNumber)
        intentForData.putExtra(TicketData.PRODUCTS_TYPE,ticketData.productsType)
        intentForData.putExtra(TicketData.PRICE_WORDS,ticketData.priceWords)
        intentForData.putExtra(TicketData.PRICE_NUMBERS,ticketData.priceNumbers)
        intentForData.putExtra(TicketData.PRODUCTS_LIST,listGson)
        Log.e("genUri", intentForData.toUri(Intent.URI_INTENT_SCHEME))
        return intentForData.toUri(Intent.URI_INTENT_SCHEME)
    }
}
