package com.example.mypos

import com.mypos.slavesdk.ReceiptData

data class DataItem(
    val align : ReceiptData.Align,
    val fontSize : ReceiptData.FontSize,
    val text : String
)

