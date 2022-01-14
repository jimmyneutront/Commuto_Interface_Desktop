package com.commuto.interfacedesktop

import io.ktor.http.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import net.folivo.trixnity.client.api.MatrixApiClient
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.Event.MessageEvent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextMessageEventContent
import kotlin.test.Test

class MatrixTests {

    @Test
    fun testGetUsersRooms() {
        runBlocking {
            val matrixRestClient = MatrixApiClient(
                baseUrl = Url("http://matrix.org"),
            ).apply { accessToken.value = "" }
            matrixRestClient.rooms.getJoinedRooms().getOrThrow().collect { value -> println(value) }
        }
    }

    @Test
    fun testSendingMessage() {
        runBlocking {
            val matrixRestClient = MatrixApiClient(
                baseUrl = Url("http://matrix.org"),
            ).apply { accessToken.value = "" }
            val CINRoomId = RoomId("!WEuJJHaRpDvkbSveLu:matrix.org")
            val messageContent = "test_message"
            val eventId = matrixRestClient.rooms.sendMessageEvent(
                roomId = CINRoomId,
                eventContent = TextMessageEventContent(messageContent)
            ).getOrThrow()
            val retrievedEvent = matrixRestClient.rooms.getEvent(
                roomId = CINRoomId,
                eventId= eventId
            ).getOrThrow()
            assert(retrievedEvent is MessageEvent)
            assert(retrievedEvent.content.equals(TextMessageEventContent(messageContent)))
        }
    }
}