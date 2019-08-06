package com.example.mypos

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log.e
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.mypos.slavesdk.POSHandler
import com.mypos.slavesdk.POSInfoListener
import com.mypos.slavesdk.ReceiptData
import com.mypos.slavesdk.TransactionData
import kotlinx.android.synthetic.main.activity_printing.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.toast

class PrintingActivity : AppCompatActivity() {

    private var printingLastItem = false

    val viewModel: MainViewModel by lazy {
        ViewModelProviders.of(this).get(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printing)
        POSHandler.setApplicationContext(this)
        if(checkCoarsePermission()){
            setObservers()
            if (intent.extras != null) {
                checkForIncomingExtras(intent.extras!!)
            } else {
                viewModel.setErrorMessage("Intent extras was null")
            }

            btnForceConnect.setOnClickListener {
                POSHandler.getInstance().connectDevice(this@PrintingActivity)
                POSHandler.getInstance().setPOSReadyListener {
                    updateConnectButtonStatus(true)
                }
            }
        }else{
            tvStatuses.setTextColor(Color.RED)
            tvStatuses.text = getString(R.string.permission_needed_message)
            btnForceConnect.visibility = View.INVISIBLE
            permissionButton.visibility = View.VISIBLE
            permissionButton.setOnClickListener {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    Constants.MY_PERMISSIONS_REQUEST_LOCATION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == Constants.MY_PERMISSIONS_REQUEST_LOCATION){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                tvStatuses.text = getString(R.string.permission_granted_message)
                tvStatuses.setTextColor(Color.BLACK)
                btnForceConnect.visibility = View.VISIBLE
                permissionButton.visibility = View.INVISIBLE
                btnForceConnect.setOnClickListener {
                    POSHandler.getInstance().connectDevice(this@PrintingActivity)
                }
            }else{
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    Constants.MY_PERMISSIONS_REQUEST_LOCATION
                )
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun setObservers() {
        viewModel.observeErrorMessage().observe(this, Observer {
            tvStatuses.text = it
            toast(it)
        })
        viewModel.observeFullDataList().observe(this, Observer {
            addFinishListener()
            if (it != null && isPosConnected()) {
                if (!viewModel.isCardPay()) {
                    printReceipt()
                } else {
                    waitForTransactionComplete()
                }
            } else {
                POSHandler.getInstance().setPOSReadyListener {
                    updateConnectButtonStatus(true)
                    if (!viewModel.isCardPay()) {
                        printReceipt()
                    } else {
                        waitForTransactionComplete()
                    }
                }
                POSHandler.getInstance().connectDevice(this)
            }
        })
    }

    private fun addFinishListener() {
        POSHandler.getInstance().setPOSInfoListener(object : POSInfoListener {
            override fun onTransactionComplete(transactionData: TransactionData?) {
            }

            override fun onPOSInfoReceived(command: Int, status: Int, description: String?) {
                e("posInfo", description)
                e("posInfo", "command $command")
                e("posInfo", "status $status")
                if (status == POSHandler.POS_STATUS_SUCCESS_PRINT_RECEIPT) {
                    if(printingLastItem){
                        runOnUiThread {
                            tvStatuses.text = description
                        }
                        finish()
                    }
                }
            }
        })
    }

    private fun waitForTransactionComplete() {
        tvStatuses.text = "Waiting until payment is complete"
        e("cardPay", "this will be card pay")
        POSHandler.getInstance().setPOSInfoListener(object : POSInfoListener {
            override fun onTransactionComplete(transactionData: TransactionData?) {
                if (transactionData != null) {
                    runOnUiThread {
                        printReceipt()
                    }
                }
            }

            override fun onPOSInfoReceived(command: Int, status: Int, description: String?) {
                e("posInfo", description)
                e("posInfo", "command $command")
                e("posInfo", "status $status")
                if (status == POSHandler.POS_STATUS_SUCCESS_PURCHASE) {
                    //arba saus sitoje vietoje
                    if (viewModel.isCardPay()) {
                        runOnUiThread {
                            printReceipt()
                        }
                    }
                }
                if (status == POSHandler.POS_STATUS_SUCCESS_PRINT_RECEIPT) {
                    if(printingLastItem){
                        finish()
                    }
                }
            }
        })
    }

    private fun printReceipt() {
        val fullDataList = viewModel.observeFullDataList().value
        if (fullDataList!!.size < 30 && fullDataList.size != 1) {
            printingLastItem = true
            printPartOfReceipt(fullDataList)
        } else {
            printingLastItem = false
            val chunkedList = fullDataList.chunked(30)
            for (x in 0 until chunkedList.size) {
                Handler().postDelayed({
                    if(x + 1 == chunkedList.size){
                        printingLastItem = true
                    }
                    printPartOfReceipt(chunkedList[x])
                }, (6000 * x).toLong())
            }
        }
    }

    private fun printPartOfReceipt(list: List<String>) {
        e("posInfo", "print START")
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
        if(printingLastItem){
            receiptData.addEmptyRow()
            receiptData.addEmptyRow()
            receiptData.addEmptyRow()
            receiptData.addEmptyRow()
        }
        POSHandler.getInstance().printReceipt(receiptData)
        e("posInfo", "print END")
    }

    private fun isPosConnected(): Boolean {
        val connectionStatus = POSHandler.getInstance().isConnected
        updateConnectButtonStatus(connectionStatus)
        return connectionStatus
    }

    private fun updateConnectButtonStatus(connectionStatus: Boolean) {
        if(connectionStatus){
            btnForceConnect.text = getString(R.string.connected_message)
            btnForceConnect.backgroundColor= Color.GREEN
        }
    }

    private fun checkForIncomingExtras(extras: Bundle) {
        if (extras.getString(Constants.RECEIPT_DATA) != null) {
            val ticketJson = intent.getStringExtra(Constants.RECEIPT_DATA)
            tvStatuses.text = ticketJson
            viewModel.setIsCardPayment(intent.getBooleanExtra(Constants.CARD_PAYMENT, false))
            viewModel.setDataJson(ticketJson)
        } else {
            viewModel.setErrorMessage("Error while getting data, null")
        }
    }

    private fun checkCoarsePermission(): Boolean {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
    }
}
