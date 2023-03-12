package com.commuto.interfacedesktop

import androidx.compose.runtime.mutableStateListOf
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import net.folivo.trixnity.clientserverapi.client.MatrixClientServerApiClient
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.Event
import org.web3j.protocol.core.DefaultBlockParameterNumber
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.http.HttpService
import java.math.BigInteger
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * A class containing tests used for prototyping app startup code
 */
class StartupTests {

    /**
     * Test the process of associating groups of Matrix events with on-chain blocks by timestamp.
     *
     * We begin by getting the latest block number X, and then subtracting 100 from that number.
     * Then we get the timestamp of the X - 100th block, and treating that as a "last-parsed block".
     * We get this block's timestamp, and then get that of the X-99th block.
     * Then, from a random (but popular) Matrix room we get all events with origin server timestamps greater than or
     * equal to that of the X - 100th block, and less than that of the X - 99th block, and associate these with
     * the X - 99th block.
     *
     * Eventually, these pairs of blocks and their associated p2p events will be fed into BlockchainService and
     * P2PService at startup.
     */
    @Test
    fun testMatrixEventAndBlockGrouping(): Unit = runBlocking {

        // web3 object for interacting with Ethereum Mainnet
        val web3 = CommutoWeb3j(HttpService("https://eth.llamarpc.com"))

        val newestBlockNumber = web3.ethBlockNumber().send().blockNumber

        var lastParsedBlockNumber = newestBlockNumber - BigInteger.valueOf(100L) - BigInteger.ONE

        val lastParsedBlock = web3.ethGetBlockByNumber(
            DefaultBlockParameterNumber(lastParsedBlockNumber),
            false
        ).send().block

        // Ethereum block timestamps are given in seconds, whereas matrix timestamps are given in milliseconds
        val lastParsedBlockTimestamp = lastParsedBlock.timestamp * BigInteger.valueOf(1000L)

        // A Matrix Client object for interacting with matrix homeserver
        val mxClient = MatrixClientServerApiClient(
            baseUrl = Url("https://matrix.org"),
            httpClientFactory = {
                HttpClient(it).config {
                    install(HttpTimeout) {
                        socketTimeoutMillis = 60_000
                    }
                }
            }
        ).apply { accessToken.value = "syt_amltbXl0_HhdFuaDgAnbgtKfstPQQ_3jzXFF" }

        val newestBatchToken = mxClient.sync.sync().getOrThrow().nextBatch
        var earliestBatchToken = newestBatchToken

        // Get the 50 newest events
        var getEventsResponse = mxClient.rooms.getEvents(
            roomId = RoomId(full = "!iuyQXswfjgxQMZGrfQ:matrix.org"),
            from = earliestBatchToken,
            dir = GetEvents.Direction.BACKWARDS,
            limit = 50L,
        ).getOrThrow()
        // Set the earliest batch token to that at the beginning of the 50 newest events
        getEventsResponse.end?.let {
            earliestBatchToken = it
        }

        /*
        Move chronologically backwards through the timeline in 50 message chunks, setting the earliest batch token to
        that at the beginning of each chunk, and stop as soon as we receive a chunk with a last (oldest) event that has
        an origin server timestamp earlier than that of the last parsed block
         */
        while (BigInteger.valueOf(getEventsResponse.chunk!!.last().originTimestamp) >= lastParsedBlockTimestamp) {
            getEventsResponse = mxClient.rooms.getEvents(
                roomId = RoomId(full = "!iuyQXswfjgxQMZGrfQ:matrix.org"),
                from = earliestBatchToken,
                dir = GetEvents.Direction.BACKWARDS,
                limit = 50L,
            ).getOrThrow()
            earliestBatchToken = getEventsResponse.end ?: break
        }

        // The first event chronologically after this token should be older than the last parsed block
        var lastParsedBatchToken = earliestBatchToken
        var processedAllEvents = false
        val eventsQueue = LinkedList<Event.RoomEvent<*>>()
        /*
        The second element in each of these pairs is a list of events with timestamps earlier than that of the block in
        the same pair, and not earlier than that of the block in the preceding pair
         */
        val blocksAndEvents = mutableStateListOf<Pair<EthBlock.Block, List<Event.RoomEvent<*>>>>()

        /*
        Move chronologically forward through all not parsed blocks
         */
        while (lastParsedBlockNumber < newestBlockNumber) {
            val blockToParse = web3.ethGetBlockByNumber(
                DefaultBlockParameterNumber(lastParsedBlockNumber + BigInteger.ONE),
                false
            ).send().block

            var haveEventInBlockTimespan = true
            val eventsInBlockTimespan = mutableListOf<Event.RoomEvent<*>>()

            // Get p2p events that were sent before the current block was mined
            while (haveEventInBlockTimespan) {
                // Check in the queue for existing events
                val event = eventsQueue.peek()
                if (event != null &&
                    BigInteger.valueOf(event.originTimestamp) < blockToParse.timestamp * BigInteger.valueOf(1000L)
                ) {
                    /*
                    We have a p2p event that occurred before the current block was mined, so remove it from the queue
                    and add it to the list of p2p events for this block
                     */
                    eventsInBlockTimespan.add(eventsQueue.poll())
                } else if (event != null &&
                    BigInteger.valueOf(event.originTimestamp) >= blockToParse.timestamp * BigInteger.valueOf(1000L)
                    ) {
                    /*
                    We have a p2p event, but it occurred after or at the same time as the current block was mined, so we
                    leave it in the queue. Additionally, since we receive events in chronological order, then no more
                    p2p events could have occurred before the current block was mined.
                     */
                    haveEventInBlockTimespan = false
                } else {
                    // We don't have a p2p event, so either we check for more
                    if (!processedAllEvents) {
                        getEventsResponse = runBlocking {
                            mxClient.rooms.getEvents(
                                roomId = RoomId(full = "!iuyQXswfjgxQMZGrfQ:matrix.org"),
                                from = lastParsedBatchToken,
                                dir = GetEvents.Direction.FORWARDS,
                                limit = 50L,
                            ).getOrThrow()
                        }

                        val eventChunk = getEventsResponse.chunk ?: throw Exception(
                            "Event chunk was null for event " +
                                    "response from $earliestBatchToken"
                        )

                        /*
                        Try to add any new events to our event queue. Note that if we have already gotten all events,
                        then the body of this forEach won't be executed.
                         */
                        eventChunk.forEach {
                            eventsQueue.add(it)
                        }

                        /*
                        If our getEvents request returned an end batch token, then there are still newer events that
                        we can get, so we update the token and continue (since we should have just filled our eventChunk
                        up with new events to handle). If we don't have an end token, then we have gotten all new events
                        and so we set the processedAllEvents flag and the haveEventInBlockTimespan flag to indicate this
                         */
                        getEventsResponse.end?.let {
                            lastParsedBatchToken = it
                        } ?: run {
                            haveEventInBlockTimespan = false
                            processedAllEvents = true
                        }
                    } else {
                        // We have already processed all events, so there aren't any more to associate with this block
                        haveEventInBlockTimespan = false
                    }
                }
            }
            blocksAndEvents.add(Pair(blockToParse, eventsInBlockTimespan))
            lastParsedBlockNumber += BigInteger.ONE
        }

        assertEquals(blocksAndEvents.size, 100)
        assertNotEquals(blocksAndEvents.first().second.size, 0)

    }

}