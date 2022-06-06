package com.commuto.interfacedesktop.p2p

import io.ktor.http.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.Event
import javax.inject.Singleton

@Singleton
class P2PService {

    // Matrix REST client
    private val mxClient = MatrixClientServerApiClient(
        baseUrl = Url("")
    ).apply { accessToken.value = ""}

    // The token at the end of the last batch of non-empty messages
    private var lastNonEmptyBatchToken = ""

    private fun updateLastNonEmptyBatchToken(newToken: String) {
        lastNonEmptyBatchToken = newToken
    }

    // The job in which P2PService listens for messages
    private var listenJob: Job = Job()

    @OptIn(DelicateCoroutinesApi::class)
    fun listen() {
        listenJob = GlobalScope.launch {
            listenLoop()
        }
    }

    fun stopListening() {
        listenJob.cancel()
    }

    suspend fun listenLoop() {
        while (true) {
            val syncResponse = mxClient.sync.sync(timeout = 60_000)
            updateLastNonEmptyBatchToken(syncResponse.getOrThrow().nextBatch)
            parseEvents(mxClient.rooms.getEvents(
                roomId = RoomId(full = "!WEuJJHaRpDvkbSveLu:matrix.org"),
                from = lastNonEmptyBatchToken,
                dir = GetEvents.Direction.BACKWARDS
            ).getOrThrow().chunk!!)
        }
    }

    private fun parseEvents(events: List<Event.RoomEvent<*>>) {
        val messageEvents = events.filterIsInstance<Event.MessageEvent<*>>()
        for (event in messageEvents) {
            print(event)
        }
    }
}