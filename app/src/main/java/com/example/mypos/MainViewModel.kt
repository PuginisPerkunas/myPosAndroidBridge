package com.example.mypos

import android.util.Log.e
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainViewModel : ViewModel() {

    private val fullDataList = MutableLiveData<List<Ticket>>()
    private val errorMessage = MutableLiveData<String>()
    private var isCardPay = false
    private var endpointLink = ""
    private var amount = 0.0

    private var ticketDataJson = ""

    fun setErrorMessage(message: String?) {
        errorMessage.value = message
    }

    fun setIsCardPayment(isCardPayment: Boolean) {
        isCardPay = isCardPayment
    }

    fun isCardPay() : Boolean{
        return isCardPay
    }

    fun observeFullDataList(): MutableLiveData<List<Ticket>>{
        return fullDataList
    }

    fun observeErrorMessage() : MutableLiveData<String>{
        return errorMessage
    }

    fun setDataJson(ticketJson: String?) {
        if(ticketJson != null){
            val dataType = object : TypeToken<List<Ticket>>() {}.type
            val dataList : List<Ticket> = Gson().fromJson(ticketJson, dataType)
            setDataList(dataList)
        }else{
            setErrorMessage("Json was null")
        }
    }

    private fun setDataList(dataList: List<Ticket> ?) {
        e("dataList", fullDataList.value.toString())
        fullDataList.value = dataList
    }

    fun setEndpointLink(endpoint: String?) {
        e("endpoint",endpoint)
        if(endpoint != null && endpoint.isNotEmpty()){
            endpointLink = endpoint
        }
    }

    fun setAmount(amount: Double) {
        e("endpoint",amount.toString())
        this.amount = amount
    }

    fun getAmount() : String{
        return amount.toString()
    }

    fun getEndpoint(): String {
        return endpointLink
    }

    fun saveDataObjectJson(ticketJson: String) {
        ticketDataJson = ticketJson
    }

    fun getTicketDataJson() = ticketDataJson

}