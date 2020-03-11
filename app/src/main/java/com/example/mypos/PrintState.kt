package com.example.mypos

data class PrintState(
    val effect: Effect? = null,
    val dataList: List<Ticket> = emptyList(),
    val cardPayment: Boolean = false,
    val amount: Double = 0.0,
    val endpointLink: String = ""
): State<PrintState, Event> {
    override fun reduce(event: Event): PrintState {
        return when(event){
            Event.EffectHandled -> copy(effect = null)
            Event.PermissionRequestNeeded -> copy(effect = Effect.RequestPermission)
            Event.PosConnectionNeeded -> copy(effect = Effect.ConnectPosAndSubscribe)
            Event.PosConnected -> copy(effect = Effect.CollectIncomeData)
            is Event.ErrorHappened -> copy(effect = Effect.ThrowErrorMessageAndClose(event.message))
            is Event.DataCollected -> {
                event.endpointLink?.let { endpoint ->
                    if(endpoint.isNotEmpty()){
                        if(event.amount > 0.0){
                            if(event.dataList.isNotEmpty()){
                                copy(
                                    effect = if(event.cardPayment) Effect.StartCardPayment else Effect.PrintIncomeReceipt,
                                    dataList = event.dataList,
                                    cardPayment = event.cardPayment,
                                    amount = event.amount,
                                    endpointLink = endpoint
                                )
                            } else copy(effect = Effect.ThrowErrorMessageAndClose("Produktu sarasas tuscias!"))
                        } else copy(effect = Effect.ThrowErrorMessageAndClose("Kaina negali buti 0!"))
                    } else copy(effect = Effect.ThrowErrorMessageAndClose("Baigties nuoroda nebu gauta!"))
                } ?: copy(effect = Effect.ThrowErrorMessageAndClose("Baigties nuoroda nebu gauta!"))
            }
            Event.TransactionCompleted -> copy(effect = Effect.PrintIncomeReceipt)
            Event.AllPrintingDone -> copy(effect = Effect.SendConfirmAndClose)
            is Event.ErrorCallRequested -> copy(effect = Effect.SendErrorToServer(event.message))
        }
    }
}


sealed class Event {
    class ErrorHappened(val message: String) : Event()
    class DataCollected(
        val dataList: List<Ticket>,
        val cardPayment: Boolean,
        val endpointLink: String?,
        val amount: Double
    ) : Event()

    class ErrorCallRequested(val message: String) : Event()
    object EffectHandled : Event()
    object PermissionRequestNeeded : Event()
    object PosConnectionNeeded : Event()
    object PosConnected : Event()
    object TransactionCompleted : Event()
    object AllPrintingDone : Event()
}

sealed class Effect {
    class ThrowErrorMessageAndClose(val message: String) : Effect()
    class SendErrorToServer(val statusCode: String) : Effect()
    object RequestPermission : Effect()
    object ConnectPosAndSubscribe : Effect()
    object CollectIncomeData : Effect()
    object StartCardPayment : Effect()
    object PrintIncomeReceipt : Effect()
    object SendConfirmAndClose : Effect()
}