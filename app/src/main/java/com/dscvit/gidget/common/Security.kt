package com.dscvit.gidget.common

import android.content.Context

/**
 * @fun getToken - Personal Access Token of user
 * @fun getClientId - Client ID of Gidget
 * @fun getClientSecret - Client Secret of Gidget
 * @author Rishav Naskar - Default Personal Access(PA) Token is the PA token of the developer
 */
class Security(val context: Context) {
    companion object {
        const val defaultToken = "--Enter default PA token here--"
    }

    fun getToken(): String =
        Utils(context).getPAToken() ?: defaultToken

    fun getClientId(): String = "96e5c43cea8ef82433bf"

    fun getClientSecret(): String = "6f6d6e16b55781963dfa0aad8e7b3a336b86879d"
}