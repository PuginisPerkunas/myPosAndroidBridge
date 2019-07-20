package com.example.mypos

class TicketData {
    var companyName = "KOMPANIJA"
    var companyCode = "99999999"
    var companyPhoneNumber = "86 444 999"
    var ticketSeries = "BLC 99"
    var ticketNumber = "9999"
    var productsType = "MAITINIMAS"
    var productsList = mutableListOf<String>()
    var priceNumbers = "99,99 EUR"
    var priceWords = "DEVYNEZDESIM DEVYNI EURAI IR DEVYNEZDESIM DEVYNI CENTAI"

    companion object{
        const val COMPANY_NAME_EXTRA = "company_name"
        const val COMPANY_CODE = "company_code"
        const val COMPANY_PHONE_NUMBER = "company_phone_number"
        const val TICKET_SERIES = "ticket_series"
        const val TICKET_NUMBER = "ticket_number"
        const val PRODUCTS_TYPE = "products_type"
        const val PRODUCTS_LIST = "products_list"
        const val PRICE_NUMBERS = "price_numbers"
        const val PRICE_WORDS = "price_words"
    }
}