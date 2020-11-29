package com.example.mypos


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Log.e
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.mypos.slavesdk.*
import kotlinx.android.synthetic.main.activity_printing.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.toast
import org.json.JSONObject
import java.io.UnsupportedEncodingException

class PrintingActivity : AppCompatActivity() {

   /* private var printingLastItem = false
    val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(this)
    }
    val viewModel: MainViewModel by lazy {
        ViewModelProviders.of(this).get(MainViewModel::class.java)
    }

    override fun onBackPressed() {

    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printing)
       // POSHandler.setApplicationContext(this)
        //POSHandler.setLanguage(Language.LITHUANIAN)
        //POSHandler.setLanguage()
       /* VolleyLog.DEBUG = true
        if (checkCoarsePermission()) {
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
        } else {
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
        tvStatuses.setText(testUri())
        tvStatuses.setOnLongClickListener {
            initFackePrint(getTestBundle())
            true
        }*/
        // testUri()
    }
/*
    private fun initFackePrint(intent: Bundle) {
        val ticketJson = intent.getString(Constants.RECEIPT_DATA)
        tvStatuses.text = ticketJson
        viewModel.setIsCardPayment(intent.getBoolean(Constants.CARD_PAYMENT, false))
        viewModel.setEndpointLink(intent.getString(Constants.ENDPOINT))
        viewModel.setAmount(intent.getDouble(Constants.AMOUNT, 0.0))
        viewModel.setIsCardPayment(intent.getBoolean(Constants.CARD_PAYMENT, false))
        viewModel.setDataJson(ticketJson)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == Constants.MY_PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tvStatuses.text = getString(R.string.permission_granted_message)
                tvStatuses.setTextColor(Color.BLACK)
                btnForceConnect.visibility = View.VISIBLE
                permissionButton.visibility = View.INVISIBLE
                btnForceConnect.setOnClickListener {
                    POSHandler.getInstance().connectDevice(this@PrintingActivity)
                }
            } else {
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

            if (it != null && isPosConnected()) {
                if (!viewModel.isCardPay()) {
                    addFinishListener()
                    printReceipt()
                } else {
                    openPayment()
                }
            } else {
                POSHandler.getInstance().setPOSReadyListener {
                    updateConnectButtonStatus(true)
                    if (!viewModel.isCardPay()) {
                        addFinishListener()
                        printReceipt()
                    } else {
                        openPayment()
                    }
                }
                POSHandler.getInstance().connectDevice(this)
            }
        })
    }

    private fun openPayment() {
        waitForTransactionComplete()
        POSHandler.getInstance().purchase(
            viewModel.getAmount(),
            "999999999",
            POSHandler.RECEIPT_PRINT_AFTER_CONFIRMATION
        )
    }

    private fun addFinishListener() {
        POSHandler.getInstance().setPOSInfoListener(object : POSInfoListener {
            override fun onTransactionComplete(transactionData: TransactionData?) {
            }

            override fun onPOSInfoReceived(command: Int, status: Int, description: String?) {
                e("posInfo", description)
                e("posInfo", "command $command")
                e("posInfo", "status $status")
                when (status) {
                    POSHandler.POS_STATUS_SUCCESS_PRINT_RECEIPT -> {
                        if (printingLastItem) {
                            runOnUiThread {
                                //tvStatuses.text = description
                                makeSuccessRequestThanClose()
                            }
                            // finish()
                        }
                    }
                    POSHandler.POS_STATUS_ACTIVATION_REQUIRED -> {
                        runOnUiThread {
                            sendError("POS_STATUS_ACTIVATION_REQUIRED")
                        }
                    }
                    POSHandler.POS_STATUS_CARD_CHIP_ERROR -> {
                        runOnUiThread {
                            sendError("POS_STATUS_CARD_CHIP_ERROR")
                        }
                    }
                    POSHandler.POS_STATUS_INTERNAL_ERROR -> {
                        runOnUiThread {
                            sendError("POS_STATUS_INTERNAL_ERROR")
                        }
                    }
                    POSHandler.POS_STATUS_INVALID_PIN -> {
                        runOnUiThread {
                            sendError("POS_STATUS_INVALID_PIN")
                        }
                    }
                    POSHandler.POS_STATUS_NOT_SUPPORTED_CARD -> {
                        runOnUiThread {
                            sendError("POS_STATUS_NOT_SUPPORTED_CARD")
                        }
                    }
                    POSHandler.POS_STATUS_NO_CARD_FOUND -> {
                        runOnUiThread {
                            sendError("POS_STATUS_NO_CARD_FOUND")
                        }
                    }
                    POSHandler.POS_STATUS_USER_CANCEL -> {
                        runOnUiThread {
                            sendError("POS_STATUS_USER_CANCEL")
                        }
                    }
                    POSHandler.POS_STATUS_WRONG_AMOUNT -> {
                        runOnUiThread {
                            sendError("POS_STATUS_WRONG_AMOUNT")
                        }
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
                        Handler().postDelayed({
                            //todo
                            e("OnPrint", "onTransactionComplete")
                            recheck()
                        }, 4000)
                    }
                }
            }

            override fun onPOSInfoReceived(command: Int, status: Int, description: String?) {
                e("posInfo", description)
                e("posInfo", "command $command")
                e("posInfo", "status $status")
                when (status) {
                    POSHandler.POS_STATUS_SUCCESS_PRINT_RECEIPT -> {
                        if (printingLastItem) {
                            runOnUiThread {
                                //tvStatuses.text = description
                                makeSuccessRequestThanClose()
                            }
                            // finish()
                        }
                    }
                    POSHandler.POS_STATUS_ACTIVATION_REQUIRED -> {
                        runOnUiThread {
                            sendError("POS_STATUS_ACTIVATION_REQUIRED")
                        }
                    }
                    POSHandler.POS_STATUS_CARD_CHIP_ERROR -> {
                        runOnUiThread {
                            sendError("POS_STATUS_CARD_CHIP_ERROR")
                        }
                    }
                    POSHandler.POS_STATUS_INTERNAL_ERROR -> {
                        runOnUiThread {
                            sendError("POS_STATUS_INTERNAL_ERROR")
                        }
                    }
                    POSHandler.POS_STATUS_INVALID_PIN -> {
                        runOnUiThread {
                            sendError("POS_STATUS_INVALID_PIN")
                        }
                    }
                    POSHandler.POS_STATUS_NOT_SUPPORTED_CARD -> {
                        runOnUiThread {
                            sendError("POS_STATUS_NOT_SUPPORTED_CARD")
                        }
                    }
                    POSHandler.POS_STATUS_NO_CARD_FOUND -> {
                        runOnUiThread {
                            sendError("POS_STATUS_NO_CARD_FOUND")
                        }
                    }
                    POSHandler.POS_STATUS_USER_CANCEL -> {
                        runOnUiThread {
                            sendError("POS_STATUS_USER_CANCEL")
                        }
                    }
                    POSHandler.POS_STATUS_WRONG_AMOUNT -> {
                        runOnUiThread {
                            sendError("POS_STATUS_WRONG_AMOUNT")
                        }
                    }
                }
            }
        })
    }

    private fun recheck() {
        printReceipt()
    }

    private fun makeSuccessRequestThanClose() {
        val jsonBody = JSONObject()
        jsonBody.put("status", "success")
        jsonBody.put("info", "Operation done successfully")
        jsonBody.put(Constants.CARD_PAYMENT, viewModel.isCardPay())
        val requestBody = jsonBody.toString()
        Log.e("log", "makeSuccessRequestThanClose + ${viewModel.getEndpoint()}")
        val postRequest = object : StringRequest(
            Method.POST, viewModel.getEndpoint(),
            Response.Listener { response ->
                // response
                Log.d("Response", response)
                this.moveTaskToBack(true);
            },
            Response.ErrorListener {
                // error
                it.networkResponse.statusCode
                if (it != null && it.localizedMessage != null) {
                    Log.d("Error.Response", it.localizedMessage)
                    viewModel.setErrorMessage(it.localizedMessage)
                }
                makeErrorCall(it.networkResponse.statusCode.toString())
            }
        ) {
            override fun getBody(): ByteArray? {
                return try {
                    requestBody.toByteArray()
                } catch (e: UnsupportedEncodingException) {
                    null
                }
            }


            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }
        }
        requestQueue.add(postRequest)
    }

    private fun makeErrorCall(code: String) {
        val jsonBody = JSONObject()
        jsonBody.put("data", viewModel.getTicketDataJson())
        jsonBody.put("error_code", code)
        val requestBody = jsonBody.toString()

        val post = object : StringRequest(Method.POST, "",
            Response.Listener {
                this.moveTaskToBack(true);

        }, Response.ErrorListener {
                this.moveTaskToBack(true);
        }) {
            override fun getBody(): ByteArray? {
                return try {
                    requestBody.toByteArray()
                } catch (e: UnsupportedEncodingException) {
                    null
                }
            }

            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }
        }
        requestQueue.add(post)
    }


    private fun sendError(message: String) {
        val jsonBody = JSONObject()
        jsonBody.put("status", "failure")
        jsonBody.put("info", message)
        jsonBody.put(Constants.CARD_PAYMENT, viewModel.isCardPay())
        Log.e("log", "sendError + ${viewModel.getEndpoint()}")
        val requestBody = jsonBody.toString()

        val postRequest = object : StringRequest(
            Method.POST, viewModel.getEndpoint(),
            Response.Listener { response ->
                // response
                Log.d("Response", response)
                this.moveTaskToBack(true);
            },
            Response.ErrorListener {
                // error
                if (it != null && it.localizedMessage != null) {
                    Log.d("Error.Response", it.localizedMessage)
                    viewModel.setErrorMessage(it.localizedMessage)
                }
                makeErrorCall(it.networkResponse.statusCode.toString())
            }
        ) {
            override fun getBody(): ByteArray? {
                return try {
                    requestBody.toByteArray()
                } catch (e: UnsupportedEncodingException) {
                    null
                }
            }

            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }
        }
        requestQueue.add(postRequest)
    }

    //todo update, po kiekvieno turi buti pauzze
    private fun printReceipt() {

        val fullDataList = viewModel.observeFullDataList().value
        for (x in fullDataList!!.indices) {
            if (x >= fullDataList.size - 1) {
                printingLastItem = true
            }
            printPartOfReceipt(fullDataList[x])
        }
        // printingLastItem = true
//        if (fullDataList!!.size < 30 && fullDataList.size != 1) {
//            printingLastItem = true
//            printPartOfReceipt(fullDataList)
//        } else {
//            printingLastItem = false
//            val chunkedList = fullDataList.chunked(30)
//            for (x in 0 until chunkedList.size) {
//                Handler().postDelayed({
//                    if (x + 1 == chunkedList.size) {
//                        printingLastItem = true
//                    }
//                    printPartOfReceipt(chunkedList[x])
//                }, (6000 * x).toLong())
//            }
//        }
    }

    private fun printPartOfReceipt(list: Ticket) {
        e("posInfo", "print START")
        val receiptData = ReceiptData()
        list.listOfDataItems.forEach {
            receiptData.addEmptyRow()
            receiptData.addRow(
                it.text,
                it.align,
                it.fontSize
            )
        }

        receiptData.addEmptyRow()
        receiptData.addEmptyRow()
        receiptData.addEmptyRow()
        POSHandler.getInstance().printReceipt(receiptData)
        Thread.sleep(5000)
        e("posInfo", "print END")
    }

    private fun isPosConnected(): Boolean {
        val connectionStatus = POSHandler.getInstance().isConnected
        updateConnectButtonStatus(connectionStatus)
        return connectionStatus
    }

    private fun updateConnectButtonStatus(connectionStatus: Boolean) {
        if (connectionStatus) {
            btnForceConnect.text = getString(R.string.connected_message)
            btnForceConnect.backgroundColor = Color.GREEN
        }
    }

    private fun checkForIncomingExtras(extras: Bundle) {
        if (extras.getString(Constants.RECEIPT_DATA) != null) {
            val ticketJson = intent.getStringExtra(Constants.RECEIPT_DATA)
            viewModel.saveDataObjectJson(ticketJson)
            tvStatuses.text = ticketJson
            viewModel.setIsCardPayment(intent.getBooleanExtra(Constants.CARD_PAYMENT, false))
            viewModel.setEndpointLink(intent.getStringExtra(Constants.ENDPOINT))
            viewModel.setAmount(intent.getDoubleExtra(Constants.AMOUNT, 0.0))
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

    fun getTestBundle(): Bundle {
        val bundle = Bundle()

        val listOfData = getListOfDataTEST()
        val listGson = Gson().toJson(listOfData)
        Log.e("jsonExample", listGson)

        bundle.putString(Constants.RECEIPT_DATA, listGson)
        bundle.putBoolean(Constants.CARD_PAYMENT, true)
        bundle.putDouble(Constants.AMOUNT, 00.01)
        bundle.putString(
            Constants.ENDPOINT,
            "https://express.artme.lt/api/orders/paid?selected_payment=BANK&card_payment=true&order_id=136"
        )
        // intentForData.putExtra(Constants.ENDPOINT_POST_DATA, "endpointPostData")
        //Log.e("genUri", intentForData.toUri(Intent.URI_INTENT_SCHEME))

        return bundle
    }

    private fun testUri(): String {
        val listOfData = getListOfDataTEST()
        val listGson = Gson().toJson(listOfData)
        Log.e("jsonExample", listGson)
        val intentForData = Intent("com.example.mypos")
        intentForData.addCategory(Intent.CATEGORY_DEFAULT)
        intentForData.addCategory(Intent.CATEGORY_BROWSABLE)
        intentForData.putExtra(Constants.RECEIPT_DATA, listGson)
        intentForData.putExtra(Constants.CARD_PAYMENT, true)
        intentForData.putExtra(Constants.AMOUNT, 00.01)
        intentForData.putExtra(
            Constants.ENDPOINT,
            "https://express.artme.lt/api/orders/paid?selected_payment=BANK&card_payment=true&order_id=136"
        )
        // intentForData.putExtra(Constants.ENDPOINT_POST_DATA, "endpointPostData")
        Log.e("genUri", intentForData.toUri(Intent.URI_INTENT_SCHEME))
        return intentForData.toUri(Intent.URI_INTENT_SCHEME)
    }

    private fun getListOfDataTEST(): List<Ticket> {
        val listOfTestt = mutableListOf<Ticket>()
        val itemOne = ArrayList<DataItem>()
            .apply {
                this.add(DataItem(ReceiptData.Align.RIGHT, ReceiptData.FontSize.SINGLE, "One"))
                this.add(DataItem(ReceiptData.Align.CENTER, ReceiptData.FontSize.DOUBLE, "tWO"))
                this.add(DataItem(ReceiptData.Align.LEFT, ReceiptData.FontSize.SINGLE, "THREE"))
            }
//        val two = ArrayList<DataItem>()
//            .apply {
//                this.add(DataItem(ReceiptData.Align.RIGHT,ReceiptData.FontSize.SINGLE,"One"))
//                this.add(DataItem(ReceiptData.Align.CENTER,ReceiptData.FontSize.DOUBLE,"tWO"))
//                this.add(DataItem(ReceiptData.Align.LEFT,ReceiptData.FontSize.SINGLE,"THREE"))
//            }
//
//        val three = ArrayList<DataItem>()
//            .apply {
//                this.add(DataItem(ReceiptData.Align.RIGHT,ReceiptData.FontSize.SINGLE,"One"))
//                this.add(DataItem(ReceiptData.Align.CENTER,ReceiptData.FontSize.DOUBLE,"tWO"))
//                this.add(DataItem(ReceiptData.Align.LEFT,ReceiptData.FontSize.SINGLE,"THREE"))
//            }
        val ticketOne = Ticket(itemOne)
//        val ticketTwo = Ticket(two)
//        val ticketThree = Ticket(three)

        listOfTestt.add(ticketOne)
//        listOfTestt.add(ticketTwo)
//        listOfTestt.add(ticketThree)

        return listOfTestt
    }*/
}
