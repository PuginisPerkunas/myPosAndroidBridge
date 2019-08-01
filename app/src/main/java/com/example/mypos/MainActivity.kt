package com.example.mypos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
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
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    var dataList = mutableListOf<String>()
    var dataListChuncked: List<List<String>>? = null
    val MY_PERMISSIONS_REQUEST_LOCATION = 99
    var isCardPayment = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!checkCoarsePermission()) {
            btnTryAgain.visibility = View.INVISIBLE
            progressBar1.visibility = View.INVISIBLE
            btnPerrmision.setOnClickListener {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION
                )
            }
        } else {
            btnPerrmision.visibility = View.INVISIBLE
        }
        getIncomingData()
        reprintprint()
        btnTryAgain.setOnClickListener {
            testUri()
            reprintprint()
        }

        btnConnect.setOnClickListener {
            POSHandler.getInstance().connectDevice(this@MainActivity)
        }

        POSHandler.getInstance().setPOSInfoListener(object : POSInfoListener {
            override fun onTransactionComplete(transactionData: TransactionData?) {
                if(transactionData != null ){
                    //arba cia saus po transaction
                    if(isCardPayment){
                        printTicket()
                    }
                }
            }

            override fun onPOSInfoReceived(command: Int, status: Int, description: String?) {
                Log.e("posInfo", description)
                Log.e("posInfo", "command $command")
                Log.e("posInfo", "status $status")
                if(status == POSHandler.POS_STATUS_SUCCESS_PURCHASE){
                    //arba saus sitoje vietoje
                    if(isCardPayment){
                        printTicket()
                    }
                }
                if (status == POSHandler.POS_STATUS_SUCCESS_PRINT_RECEIPT) {
                    finish()
                }
            }

        })

        POSHandler.getInstance().setPOSReadyListener {
            if(!isCardPayment){
                printTicket()
            }
            //Todo only for testing
            else{
                Handler().postDelayed({
                    printTicket()
                },6000)
            }
        }

    }

    private fun reprintprint() {
        POSHandler.setConnectionType(ConnectionType.BLUETOOTH)
        POSHandler.setApplicationContext(this)
        if (POSHandler.getInstance().isConnected) {
            Log.e("posInfo", "connected")
            if(!isCardPayment){
                printTicket()
            }
            //Todo only for testing
            else{
                Handler().postDelayed({
                    printTicket()
                },6000)
            }
        } else {
            Log.e("posInfo", "disconnected")
            POSHandler.getInstance().connectDevice(this)
        }
    }

    private fun checkCoarsePermission(): Boolean {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun getIncomingData() {

        if (intent.getStringExtra(Constants.RECEIPT_DATA) != null) {
            isCardPayment = intent.getBooleanExtra(Constants.CARD_PAYMENT,false)
            val ticketJson = intent.getStringExtra(Constants.RECEIPT_DATA)
            val dataType = object : TypeToken<List<String>>() {}.type
            dataList = Gson().fromJson(ticketJson, dataType)
            if(dataList.size > 0){
                if(isCardPayment){
                    dataList.add("Atsiskaytymas kortele")
                }else{
                    dataList.add("Atsiskaitymas grynaisiais")
                }
            }
            if (dataList.size > 30) {
                dataListChuncked = dataList.chunked(30)
            }
        } else {
            toast("Data was null null")
        }
    }

    private fun printTicket() {
        if(dataList.size < 30 && dataList.size != 1){
            printPartOfReciept(dataList)
        } else if(dataListChuncked != null && dataListChuncked!!.isNotEmpty()){
            for (x in 0..dataListChuncked!!.size){
                Handler().postDelayed({
                    printPartOfReciept(dataListChuncked!![x])
                },(6000 * x).toLong())
            }
        }else{
            toast("error while getting data")
        }

    }

    private fun printPartOfReciept(list: List<String>) {
        Log.e("posInfo", "print START")
        val receiptData = ReceiptData()
        list.forEach {
            receiptData.addEmptyRow()
            receiptData.addRow(
                it,
                ReceiptData.Align.LEFT,
                ReceiptData.FontSize.SINGLE
            )
        }
        receiptData.addEmptyRow()
        receiptData.addEmptyRow()
        receiptData.addEmptyRow()
        receiptData.addEmptyRow()
        POSHandler.getInstance().printReceipt(receiptData)
        Log.e("posInfo", "print END")
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun getFullProductsString(productsList: MutableList<String>): String {
        return TextUtils.join(", ", productsList)
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy/M/dd hh:mm:ss", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    private fun testUri(): String {
        val listOfData  = getListOfDataTEST()
        val listGson = Gson().toJson(listOfData)
        Log.e("jsonExample", listGson)
        val intentForData = Intent("com.example.mypos")
        intentForData.addCategory(Intent.CATEGORY_DEFAULT)
        intentForData.addCategory(Intent.CATEGORY_BROWSABLE)
        intentForData.putExtra(Constants.RECEIPT_DATA, listGson)
        intentForData.putExtra(Constants.CARD_PAYMENT, false)
        Log.e("genUri", intentForData.toUri(Intent.URI_INTENT_SCHEME))
        return intentForData.toUri(Intent.URI_INTENT_SCHEME)
    }

    private fun getListOfDataTEST(): MutableList<String> {
        val mutableDataList = mutableListOf<String>()
        mutableDataList.add("9999999")
        mutableDataList.add("Imones Pavadinimas")
        mutableDataList.add("862100000")
        mutableDataList.add("2019/01/01 16:55")
        mutableDataList.add("Pinigu isdavimas")
        mutableDataList.add("Paskirtis: Maitinimas")
        mutableDataList.add("Viena preke, antra preke, trecia preke, t.t")
        mutableDataList.add("Pinigus gavau:________")
        mutableDataList.add("Pinigus priemiau:_____")
        return mutableDataList
    }
}
