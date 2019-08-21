package com.example.mypos

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainViewModel : ViewModel() {

    private val fullDataList = MutableLiveData<List<String>>()
    private val errorMessage = MutableLiveData<String>()
    private var isCardPay = false
    private var endpointLink = ""
    private var amount = 0.0

    fun setErrorMessage(message: String?) {
        errorMessage.value = message
    }

    fun setIsCardPayment(isCardPayment: Boolean) {
        isCardPay = isCardPayment
    }

    fun isCardPay() : Boolean{
        return isCardPay
    }

    fun observeFullDataList(): MutableLiveData<List<String>>{
        return fullDataList
    }

    fun observeErrorMessage() : MutableLiveData<String>{
        return errorMessage
    }

    fun setDataJson(ticketJson: String?) {
        if(ticketJson != null){
            val dataType = object : TypeToken<List<String>>() {}.type
            val dataList : List<String> = Gson().fromJson(ticketJson, dataType)
            setDataList(dataList)
        }else{
            setErrorMessage("Json was null")
        }
    }

    private fun setDataList(dataList: List<String>?) {
        fullDataList.value = dataList
    }

    fun setEndpointLink(endpoint: String?) {
        if(endpoint != null && endpoint.isNotEmpty()){
            endpointLink = endpoint
        }
    }

    fun setAmount(amount: Double) {
        this.amount = amount
    }

    fun getAmount() : String{
        return amount.toString()
    }

    fun getEndpoint(): String {
        return endpointLink
    }

}