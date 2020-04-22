package com.example.mypos

data class PrintState(
    val effect: Effect? = null,
    val dataList: List<Ticket> = emptyList(),
    val cardPayment: Boolean = false,
    val amount: String = "",
    val endpointLink: String = ""
    ) : State<PrintState, Event> {
    override fun reduce(event: Event): PrintState {
        return when (event) {
            Event.EffectHandled -> copy(effect = null)
            Event.PermissionRequestNeeded -> copy(effect = Effect.RequestPermission)
            Event.PosConnectionNeeded -> copy(effect = Effect.ConnectPosAndSubscribe)
            Event.PosConnected -> copy(effect = Effect.CollectIncomeData)
            is Event.ErrorHappened -> copy(effect = Effect.ThrowErrorMessageAndClose(event.message))
            is Event.DataCollected -> {
                if (!event.endpointLink.isNullOrEmpty()) {
                    //if (event.dataList.isNotEmpty()) {
                        if (!event.cardPayment) {
                            copy(
                                effect = Effect.PrintIncomeReceipt,
                                dataList = event.dataList,
                                cardPayment = event.cardPayment,
                                amount = "",
                                endpointLink = event.endpointLink
                            )
                        } else {
                            if (!event.amount.isNullOrEmpty()) {
                                copy(
                                    effect = Effect.StartCardPayment,
                                    dataList = event.dataList,
                                    cardPayment = event.cardPayment,
                                    amount = event.amount.replace("'",""),
                                    endpointLink = event.endpointLink
                                )
                            } else copy(effect = Effect.ThrowErrorMessageAndClose("Kaina negali buti 0 atsiskaitant kortele!"))
                        }
                  //  } else copy(effect = Effect.ThrowErrorMessageAndClose("Produktu sarasas tuscias!"))
                } else copy(effect = Effect.ThrowErrorMessageAndClose("Baigties nuoroda nebuvo gauta!"))
            }
            Event.TransactionCompleted -> copy(effect = Effect.PrintIncomeReceipt)
            Event.AllPrintingDone -> copy(effect = Effect.SendConfirmAndClose)
            is Event.ErrorCallRequested -> copy(effect = Effect.SendErrorToServer(event.message))
            Event.PosRequestRetry -> copy(effect = Effect.CollectIncomeData)
        }
    }
}


sealed class Event {
    class ErrorHappened(val message: String) : Event()
    class DataCollected(
        val dataList: List<Ticket>,
        val cardPayment: Boolean,
        val endpointLink: String?,
        val amount: String?
    ) : Event()

    class ErrorCallRequested(val message: String) : Event()

    object EffectHandled : Event()
    object PermissionRequestNeeded : Event()
    object PosConnectionNeeded : Event()
    object PosConnected : Event()
    object TransactionCompleted : Event()
    object AllPrintingDone : Event()
    object PosRequestRetry : Event()
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