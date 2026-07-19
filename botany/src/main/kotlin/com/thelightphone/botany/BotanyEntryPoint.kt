package com.thelightphone.botany

import com.thelightphone.sdk.EntryPoint
import com.thelightphone.sdk.LightEntryPoint
import com.thelightphone.sdk.shared.LightServerData
import kotlinx.coroutines.flow.StateFlow

@EntryPoint
object BotanyEntryPoint : LightEntryPoint {
    override suspend fun onToolCreate(
        serverData: StateFlow<LightServerData?>,
    ) {
        // No push credentials to register and no server-side state to sync for Botany.
    }
}
