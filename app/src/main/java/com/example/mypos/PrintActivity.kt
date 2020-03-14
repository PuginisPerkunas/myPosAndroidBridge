package com.example.mypos

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mypos.slavesdk.*
import kotlinx.android.synthetic.main.activity_print.*
import org.json.JSONObject
import java.io.UnsupportedEncodingException

private const val FIRST_PRINT_DELAY_DEFAULT = 1000L
private const val PRINTER_WAIT_DELAY_DEFAULT = 5000L
private const val PRINTER_WAIT_DELAY_LONG = 10000L
private const val BACK_BUTTON_DELAY = 120000L
private const val ERROR_LINK_STAGE = "https://express-stag.artme.lt/pos-error"
private const val ERROR_LINK_PROD = "https://admin.delivery.picagroup.lt/pos-error"

class PrintActivity : AppCompatActivity() {

    private var printingLastReciept: Boolean = false
    private val dataType = object : TypeToken<List<Ticket>>() {}.type
    private var backHandler: Handler = Handler()
    private var backRunnable: Runnable = Runnable {
        finish()
    }

    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(this)
    }

    private val machine by lazy {
        StateMachine(
            PrintState()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_print)
        POSHandler.setApplicationContext(this)
        POSHandler.setLanguage(Language.LITHUANIAN)
        VolleyLog.DEBUG = true
        subscribeMachine()
    }

    override fun onResume() {
        super.onResume()
        if (checkCoarsePermission()) {
            //Have permission, we can continue
            machine.transition(Event.PosConnectionNeeded)
        } else {
            //Need permission
            machine.transition(Event.PermissionRequestNeeded)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == Constants.MY_PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Permission ok
                machine.transition(Event.PosConnectionNeeded)
            } else {
                //Request permission again
                machine.transition(Event.PermissionRequestNeeded)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onBackPressed() {
        //on click closes app after 2 min
        backHandler.removeCallbacks(backRunnable)
        backHandler.postDelayed(
            backRunnable,
            BACK_BUTTON_DELAY
        )
    }

    private fun subscribeMachine() {
        machine.subscribe { _, newState ->
            newState.effect?.let { Log.e("currentState", newState.effect::class.java.simpleName) }
            when (val effect = newState.effect) {
                Effect.RequestPermission -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        Constants.MY_PERMISSIONS_REQUEST_LOCATION
                    )
                    machine.transition(Event.EffectHandled)
                }
                Effect.ConnectPosAndSubscribe -> {
                    if (POSHandler.getInstance().isConnected) {
                        textView.text = "Pos jau prijungtas, tikrinami duomenys..."
                        Handler().postDelayed({
                            machine.transition(Event.PosConnected)
                        }, PRINTER_WAIT_DELAY_DEFAULT)
                    } else {
                        POSHandler.getInstance().connectDevice(this)
                        POSHandler.getInstance().setPOSReadyListener {
                            textView.text = "Tikrinami duomenys..."
                            Handler().postDelayed({
                                machine.transition(Event.PosConnected)
                            }, PRINTER_WAIT_DELAY_DEFAULT)
                        }
                    }
                    POSHandler.getInstance().setPOSInfoListener(object : POSInfoListener {
                        override fun onTransactionComplete(transactionData: TransactionData?) {
                            if (transactionData != null) {
                                runOnUiThread {
                                    Handler().postDelayed({
                                        machine.transition(Event.TransactionCompleted)
                                    }, PRINTER_WAIT_DELAY_LONG)
                                }
                            }
                        }

                        override fun onPOSInfoReceived(
                            command: Int,
                            status: Int,
                            description: String?
                        ) {
                            Log.e("onPOSInfoReceived", "command $command")
                            Log.e("onPOSInfoReceived", "status $status")
                            Log.e("onPOSInfoReceived", "description $description")
                            when (status) {
                                POSHandler.POS_STATUS_SUCCESS_PRINT_RECEIPT -> {
                                    if (printingLastReciept) {
                                        runOnUiThread {
                                            textView.text = "Siunciamas patvirtinimas"
                                            machine.transition(Event.AllPrintingDone)
                                        }
                                    }
                                }
                                POSHandler.POS_STATUS_DOWNLOADING_CERTIFICATES_IN_PROGRESS -> {
                                    if (printingLastReciept) {
                                        runOnUiThread {
                                            textView.text = "Atsiunciami sertifikatai..."
                                            machine.transition(Event.EffectHandled)
                                        }
                                    }
                                }
                                POSHandler.POS_STATUS_DOWNLOADING_CERTIFICATES_COMPLETED -> {
                                    if (printingLastReciept) {
                                        runOnUiThread {
                                            textView.text = "Sertifikatai atsiusti, tesiama..."
                                            machine.transition(Event.EffectHandled)
                                        }
                                    }
                                }
                                POSHandler.POS_STATUS_NO_PAPER -> {
                                    if (printingLastReciept) {
                                        runOnUiThread {
                                            textView.text = "Pakeiskite spauzdinimo popieriu!"
                                            enableRetryButton()
                                            machine.transition(Event.EffectHandled)
                                        }
                                    }
                                }
                                POSHandler.POS_STATUS_INVALID_PIN -> {
                                    if (printingLastReciept) {
                                        runOnUiThread {
                                            textView.text = "Netinkamas kodas!"
                                            enableRetryButton()
                                            machine.transition(Event.EffectHandled)
                                        }
                                    }
                                }
                                POSHandler.POS_STATUS_TERMINAL_BUSY -> {/* ignore this case */
                                }
                                else -> {
                                    runOnUiThread {
                                        machine.transition(Event.ErrorCallRequested(status.toString()))
                                    }
                                }
                            }
                        }

                    })
                    machine.transition(Event.EffectHandled)
                }
                Effect.CollectIncomeData -> {
                    intent.extras?.getString(Constants.RECEIPT_DATA)?.let { dataJson ->
                        val dataList: List<Ticket> = Gson().fromJson(dataJson, dataType)
                        val isCardPayment = intent.getBooleanExtra(Constants.CARD_PAYMENT, false)
                        val endpointLink = intent.getStringExtra(Constants.ENDPOINT)
                        val amount = intent.getDoubleExtra(Constants.AMOUNT, 0.0)
                        // Pass data for machine to continue
                        Log.e("loger", "dataJson $dataJson")
                        Log.e("loger", "isCardPayment $isCardPayment")
                        Log.e("loger", "endpointLink $endpointLink")
                        Log.e("loger", "amount $amount")
                        machine.transition(
                            Event.DataCollected(
                                dataList,
                                isCardPayment,
                                endpointLink,
                                amount
                            )
                        )
                    } ?: machine.transition(Event.ErrorHappened("Duomenys nepasieke aplikacijos!"))
                }
                is Effect.ThrowErrorMessageAndClose -> {
                    textView.text = effect.message
                    Handler().postDelayed({
                        runOnUiThread {
                            this.finish()
                        }
                    }, 5000)
                    machine.transition(Event.EffectHandled)
                }
                Effect.StartCardPayment -> {
                    POSHandler.getInstance().purchase(
                        newState.amount.toString(),
                        "999999999",
                        POSHandler.RECEIPT_PRINT_AFTER_CONFIRMATION
                    )
                    machine.transition(Event.EffectHandled)
                }
                Effect.PrintIncomeReceipt -> {
                    textView.text = "Spauzdinama..."
                    Handler().postDelayed({
                        val lastIndexInDataList = newState.dataList.lastIndex
                        newState.dataList.mapIndexed { index, ticket ->
                            if (index == lastIndexInDataList) {
                                printingLastReciept = true
                            }
                            printPartOfReceipt(ticket, index, lastIndexInDataList)
                        }
                    }, FIRST_PRINT_DELAY_DEFAULT)
                    machine.transition(Event.EffectHandled)
                }
                Effect.SendConfirmAndClose -> {
                    val jsonBody = JSONObject()
                    jsonBody.put("status", "success")
                    jsonBody.put("info", "Operation done successfully")
                    jsonBody.put(Constants.CARD_PAYMENT, newState.cardPayment)
                    val requestBody = jsonBody.toString()
                    sendDataToServer(
                        requestBody = requestBody,
                        endpointLink = newState.endpointLink
                    )
                    machine.transition(Event.EffectHandled)
                }
                is Effect.SendErrorToServer -> {
                    val jsonBody = JSONObject()
                    jsonBody.put("statusCode", effect.statusCode)
                    jsonBody.put("isCardPayment", newState.cardPayment)
                    jsonBody.put("amount", newState.amount)
                    jsonBody.put("data", Gson().toJson(newState.dataList))
                    jsonBody.put("endpoint", newState.endpointLink)
                    val requestBody = jsonBody.toString()
                    sendDataToServer(
                        requestBody = requestBody,
                        endpointLink = ERROR_LINK_STAGE,
                        isFalbackCall = true
                    )
                    machine.transition(Event.EffectHandled)
                }
            }
        }.disposedBy(this)
    }

    private fun enableRetryButton() {
        retry.visibility = View.VISIBLE
        retry.isEnabled = true
        retry.setOnClickListener {
            retry.visibility = View.INVISIBLE
            retry.isEnabled = false
            machine.transition(Event.PosRequestRetry)
        }
    }

    private fun checkCoarsePermission(): Boolean {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun printPartOfReceipt(ticket: Ticket, currentItemIndex: Int, lastItemIndex: Int) {
        val receiptData = ReceiptData()
        ticket.listOfDataItems.forEach {
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
        // Tricky part, need to check connection and busy status, because of
        // sometimes, in beginning getting that pos is connected, but in real life it's not and
        // because of this getting java.io.IOException: Broken pipe error from android (and no, try
        // catch not working here because its coming like warning).
        // So additional check here helps prevent that situation, BUT increase wait time
        if (POSHandler.getInstance().isConnected) {
            if (!POSHandler.getInstance().isTerminalBusy) {
                POSHandler.getInstance().printReceipt(receiptData)
                Thread.sleep(PRINTER_WAIT_DELAY_DEFAULT)
            } else {
                Handler().postDelayed({
                    POSHandler.getInstance().printReceipt(receiptData)
                    Thread.sleep(PRINTER_WAIT_DELAY_DEFAULT)
                }, PRINTER_WAIT_DELAY_DEFAULT)
            }
        } else {
            printingLastReciept = false
            machine.transition(Event.PosConnectionNeeded)
        }
    }

    private fun sendDataToServer(
        requestBody: String,
        endpointLink: String,
        isFalbackCall: Boolean = false
    ) {
        val postRequest = object : StringRequest(
            Method.POST, endpointLink,
            Response.Listener { response ->
                Log.d("Response", response)
                //this line not close app, but comes back to previous screen
                machine.transition(event = Event.EffectHandled)
                this.moveTaskToBack(true)
            },
            Response.ErrorListener {
                // error
                if (!isFalbackCall) {
                    it.networkResponse.statusCode
                    textView.text = it.localizedMessage ?: "Klaida susijusi su serveriu..."
                    machine.transition(Event.ErrorCallRequested(it.networkResponse.statusCode.toString()))
                } else finish()
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
}
