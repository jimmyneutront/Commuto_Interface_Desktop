package com.commuto.interfacedesktop

import com.commuto.interfacedesktop.db.DatabaseDriverFactory
import com.commuto.interfacedesktop.dbService.DBService
import com.commuto.interfacedesktop.kmService.KMService
import com.commuto.interfacedesktop.kmService.kmTypes.KeyPair
import com.commuto.interfacedesktop.kmService.kmTypes.PublicKey
import com.commuto.interfacedesktop.kmService.kmTypes.newSymmetricKey
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.folivo.trixnity.client.api.MatrixApiClient
import net.folivo.trixnity.client.api.rooms.Direction
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.Event.MessageEvent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextMessageEventContent
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.CompletableFuture
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
    fun testCINMessageListening() {
        //Setup decoder
        val decoder = Base64.getDecoder()

        //Restore the maker's public and private key
        val makerPubKeyB64 = "MIIBCgKCAQEAnnDB4zV2llEwwLHw7c934eV7t69Om52dpLcuctXtOtjGsaKyOAV96egmxX6+C+MptFST3yX4wO6qK3/NSuOHWBXIHkhQGZEdTHOn4HE9hHdw2axJ0F9GQKZeT8t8kw+58+n+nlbQUaFHUw5iypl3WiI1K7En4XV2egfXGk9ujElMqXZO/eFun3eAM+asT1g7o/k2ysOpY5X+sqesLsJ0gzaGH4jfDVuWifS5YhdgFKkBi1i3U1tfPdc3sN53uNCPEhxjjuOuYH5I3WI9VzjpJezYoSlwzI4hYNOjY0cWzZM9kqeUt93KzTpvX4FeQigT9UO20cs23M5NbIW4q7lA4wIDAQAB"
        val makerPrivKeyB64 = "MIIEogIBAAKCAQEAnnDB4zV2llEwwLHw7c934eV7t69Om52dpLcuctXtOtjGsaKyOAV96egmxX6+C+MptFST3yX4wO6qK3/NSuOHWBXIHkhQGZEdTHOn4HE9hHdw2axJ0F9GQKZeT8t8kw+58+n+nlbQUaFHUw5iypl3WiI1K7En4XV2egfXGk9ujElMqXZO/eFun3eAM+asT1g7o/k2ysOpY5X+sqesLsJ0gzaGH4jfDVuWifS5YhdgFKkBi1i3U1tfPdc3sN53uNCPEhxjjuOuYH5I3WI9VzjpJezYoSlwzI4hYNOjY0cWzZM9kqeUt93KzTpvX4FeQigT9UO20cs23M5NbIW4q7lA4wIDAQABAoIBACWe/ZLfS4DG144x0lUNedhUPsuvXzl5NAj8DBXtcQ6TkZ51VN8TgsHrQ2WKwkKdVnZAzPnkEMxy/0oj5xG8tBL43RM/tXFUsUHJhpe3G9Xb7JprG/3T2aEZP/Sviy16QvvFWJWtZHq1knOIy3Fy/lGTJM/ymVciJpc0TGGtccDyeQDBxaoQrr1r4Q9q5CMED/kEXq5KNLmzbfB1WInQZJ7wQhtyyAJiXJxKIeR3hVGR1dfBJGSbIIgYA5sYv8HPnXrorU7XEgDWLkILjSNgCvaGOgC5B4sgTB1pmwPQ173ee3gbn+PCai6saU9lciXeCteQp9YRBBWfwl+DDy5oGsUCgYEA0TB+kXbUgFyatxI46LLYRFGYTHgOPZz6Reu2ZKRaVNWC75NHyFTQdLSxvYLnQTnKGmjLapCTUwapiEAB50tLSko/uVcf4bG44EhCfL4S8hmfS3uCczokhhBjR/tZxnamXb/T1Wn2X06QsPSYQQmZB7EoQ6G0u/K792YgGn/qh+cCgYEAweUWInTK5nIAGyA/k0v0BNOefNTvfgV25wfR6nvXM3SJamHUTuO8wZntekD/epd4EewTP57rEb9kCzwdQnMkAaT1ejr7pQE4RFAZcL86o2C998QS0k25fw5xUhRiOIxSMqK7RLkAlRsThel+6BzHQ+jHxB06te3yyIjxnqP576UCgYA7tvAqbhVzHvw7TkRYiNUbi39CNPM7u1fmJcdHK3NtzBU4dn6DPVLUPdCPHJMPF4QNzeRjYynrBXfXoQ3qDKBNcKyIJ8q+DpGL1JTGLywRWCcU0QkIA4zxiDQPFD0oXi5XjK7XuQvPYQoEuY3M4wSAIZ4w0DRbgosNsGVxqxoz+QKBgClYh3LLguTHFHy0ULpBLQTGd3pZEcTGt4cmZL3isI4ZYKAdwl8cMwj5oOk76P6kRAdWVvhvE+NR86xtojOkR95N5catwzF5ZB01E2e2b3OdUoT9+6F6z35nfwSoshUq3vBLQTGzXYtuHaillNk8IcW6YrbQIM/gsK/Qe+1/O/G9AoGAYJhKegiRuasxY7ig1viAdYmhnCbtKhOa6qsq4cvI4avDL+Qfcgq6E8V5xgUsPsl2QUGz4DkBDw+E0D1Z4uT60y2TTTPbK7xmDs7KZy6Tvb+UKQNYlxL++DKbjFvxz6VJg17btqid8sP+LMhT3oqfRSakyGS74Bn3NBpLUeonYkQ="
        val makerKeyPair = KeyPair(decoder.decode(makerPubKeyB64), decoder.decode(makerPrivKeyB64))
        val makerPublicKey = PublicKey(decoder.decode(makerPubKeyB64))

        //Restore the taker's public and private key
        val takerPubKeyB64 = "MIIBCgKCAQEAstQwQCanMBPJIEj1Mjc1m80sL3eJ/y1SDM3iVoDk2oNN6WOZly0GWbv1xjNMM94U8GLnYrzEGUek2IKcicBAVYhwsegeVo2DHOts72g6GpVWOPKndpT87raKCqSkd+IqR2OWAo+olGWmjWgAbesH/ojqJPNHaKlhi4b0JSwNAMfTP2HqcN2lXLXnSbR7F7MnrvjHbUxEUulthmX1mLId/7bznQ2hjyUP2yOQY92C7DFwVl/J33YV2F1GJbx5xGqB/cRRB+0hTRoqQvHscZAlGykWIVgvrdPw2JOsadQVePUhDBU5jvS5qyD6JxAlRWgN7FZsMTFLVM2XNW40N3jMIwIDAQAB"
        val takerPrivKeyB64 = "MIIEowIBAAKCAQEAstQwQCanMBPJIEj1Mjc1m80sL3eJ/y1SDM3iVoDk2oNN6WOZly0GWbv1xjNMM94U8GLnYrzEGUek2IKcicBAVYhwsegeVo2DHOts72g6GpVWOPKndpT87raKCqSkd+IqR2OWAo+olGWmjWgAbesH/ojqJPNHaKlhi4b0JSwNAMfTP2HqcN2lXLXnSbR7F7MnrvjHbUxEUulthmX1mLId/7bznQ2hjyUP2yOQY92C7DFwVl/J33YV2F1GJbx5xGqB/cRRB+0hTRoqQvHscZAlGykWIVgvrdPw2JOsadQVePUhDBU5jvS5qyD6JxAlRWgN7FZsMTFLVM2XNW40N3jMIwIDAQABAoIBADez/Kue3qkNILMbxrSzmdFEIaVPeP6xYUN3xi7ny2F9UQGH8smyTq4Y7D+mru/hF2ihhi2tWu/87w458QS/i8qYy3G/OeQABH03oCEauC6bodXvT9aSJg89cNZL3qcxHbZLAOkfUoWW/EBDyw5yDXVttHF6Dh491JKfoOELTamWD4KxIScR/Nf6ih6UqB/SwmLz1X5+fZpW4iGZXIRsPzOzDtDmoSGajNXoi0Ln2x9DkUeXpx9r7TTT9DBT0jTLbCUiB3LYU4I/VR6upm0bDUKKRi9VTkQjOAV5rD3qdoraPVRCSzjUVqCwL7jqfunXsG/hhRccD+Di5pXaCuPeOsECgYEA3p4LLVHDzLhF269oUcvflMoBUVKo9UNHL/wmyujdV+RwFi5J2sxVLgKHsdKHCy7FdrDmxax7Mrgh57KS3+zfdDhs98w181JLwgFxzEAxIP2PnHd4P3NEbxCxnxhILW4fEotUVzJWjjhEHXe5QhOW2z2yIZIOEqBzFfRx33kWrbMCgYEAzaUrDMaTkIkOoVI7BbNS7n5CBWL/DaPOID1UiL4eHWngeoOwaeI+CB0cxSrxngykue0xM3aI3KVFaeIYSdn7DZAxWAS3U143VApgLxgLyxZBtVX18HYiTZQx/PiTczMH6kFA5z0L7iNlf0uQrQQJgDzM6QY0kKasufoss+Baj9ECgYA1BjvvTXxvtKyfCQa2BPN6QytRLXklAiNgoJS03AZsuvKfteLNhMH9NYkQp+6WkUtjW/t7tfuaNxWMVJJ7V7ZZvl7mHvPywvVcfm+WkOuiygJ86E/x/Qid08Ia/POkLoikKB+srUbElU5UHoI35OaXzfgx2tITSbhf0FuXOQZX1QKBgAj7A4xFR8ByG89ztdwj3qVHoj51+klwM9o4k259Tvdd3k27XoLhPHBCRTVfELokNzVfZFyo+oUYOpXLJ+BhwpLvDxiW7CKZ5LSo11Z3KFywFiKDJIBhyFG2/Q/dEyNewSO7wcfXZKP7q70JYcIMgRW2kgRDHxyKCtT8VeNtEsdhAoGBAJHzNruW/ZS31o0rvQxHu8tBcd8osTsPNZBhuHs60mbPFRHwBaU8JSofl4XjR8B7K9vjYtxVYMEsIX6NqNf1JMXGDva/cTCHXyPuiCnuUMbHkK0YpsFxQABwYA+sOSlujwJwMNPu4ylzHL1HDyv9m4x74/NM20zDFW6MB/zD6G0c"
        val takerKeyPair = KeyPair(decoder.decode(takerPubKeyB64), decoder.decode(takerPrivKeyB64))
        val takerPublicKey = PublicKey(decoder.decode(takerPubKeyB64))

        //Restore offer id
        val offerId = decoder.decode("9tGMGTr0SbuySqE0QOsAMQ==")

        //Create payment details
        val makerPaymentDetails = SerializableUSD_SWIFT_Details(
            "USD-SWIFT",
            "Make Ker",
            "2039482",
            "MAK3940"
        )
        val takerPaymentDetails = SerializableUSD_SWIFT_Details(
            "USD-SWIFT",
            "Take Ker",
            "2039482",
            "TAK3940"
        )

        //Begin tracking messages that have been found
        var foundKeyAnnouncement = false
        var foundTakerInfo = false
        var foundMakerInfo = false

        val matrixRestClient = MatrixApiClient(
            baseUrl = Url("http://matrix.org"),
        ).apply { accessToken.value = "" }
        runBlocking {
            //Perform initial sync, getting 10 most recent messages
            val response = matrixRestClient.sync.syncOnce(timeout = 60000).getOrThrow()
            val CINRoomId = RoomId("!WEuJJHaRpDvkbSveLu:matrix.org")
            val CINRoom = response.room!!.join!!.get(CINRoomId)
            val previousBatch = CINRoom!!.timeline!!.previousBatch!!
            val latestEvents = CINRoom!!.timeline!!.events!!

            //Search for public key announcement message in 10 latest messages
            for (event in latestEvents) {
                if (event.content is TextMessageEventContent) {
                    val restoredPublicKey = parsePublicKeyAnnouncement((event.content as TextMessageEventContent).body,
                        makerKeyPair.interfaceId, offerId)
                    if (restoredPublicKey != null) {
                        foundKeyAnnouncement = true
                        assert(Arrays.equals(restoredPublicKey.interfaceId, makerKeyPair.interfaceId))
                    }
                }
            }

            var latestNonEmptyBatch = previousBatch

            //Search for public key announcement message in incoming messages
            while (!foundKeyAnnouncement) {
                val response = matrixRestClient.rooms.getEvents(CINRoomId,
                    latestNonEmptyBatch,
                    Direction.FORWARD,
                    null,
                    1_000_000_000,
                    null,
                    null
                ).getOrThrow()
                val messages = response.chunk!!
                for (message in messages) {
                    if (message.content is TextMessageEventContent) {
                        val restoredPublicKey = parsePublicKeyAnnouncement((message.content as TextMessageEventContent).body,
                            makerKeyPair.interfaceId, offerId)
                        if (restoredPublicKey != null) {
                            foundKeyAnnouncement = true
                            assert(Arrays.equals(restoredPublicKey.interfaceId, makerKeyPair.interfaceId))
                            break
                        } else {
                            delay(2000L)
                        }
                    }
                }
                if (!foundKeyAnnouncement) {
                    if (response.end != null && response.end != latestNonEmptyBatch) {
                        latestNonEmptyBatch = response.end!!
                    }
                }
            }

            //Search for taker info message in incoming events
            while (!foundTakerInfo) {
                val response = matrixRestClient.rooms.getEvents(CINRoomId,
                    latestNonEmptyBatch,
                    Direction.FORWARD,
                    null,
                    1_000_000_000,
                    null,
                    null
                ).getOrThrow()
                val messages = response.chunk!!
                for (message in messages) {
                    if (message.content is TextMessageEventContent) {
                        val parsingResults = parseTakerInfoMessage((message.content as TextMessageEventContent).body,
                        makerKeyPair, takerKeyPair.interfaceId, offerId)
                        if (parsingResults != null) {
                            foundTakerInfo = true
                            assert(Arrays.equals(parsingResults.first.interfaceId, takerKeyPair.interfaceId))
                            assert(parsingResults.second.equals(takerPaymentDetails))
                            break
                        } else {
                            delay(2000L)
                        }
                    }
                }
                if (!foundTakerInfo) {
                    if (response.end != null && response.end != latestNonEmptyBatch) {
                        latestNonEmptyBatch = response.end!!
                    }
                }
            }

            //Search for maker info message in incoming events
            while (!foundMakerInfo) {
                val response = matrixRestClient.rooms.getEvents(CINRoomId,
                    latestNonEmptyBatch,
                    Direction.FORWARD,
                    null,
                    1_000_000_000,
                    null,
                    null
                ).getOrThrow()
                val messages = response.chunk!!
                for (message in messages) {
                    if (message.content is TextMessageEventContent) {
                        val parsedPaymentDetails = parseMakerInfoMessage((message.content as TextMessageEventContent).body,
                            takerKeyPair, makerPublicKey, offerId)
                        if (parsedPaymentDetails != null) {
                            foundMakerInfo = true
                            assert(parsedPaymentDetails.equals(makerPaymentDetails))
                            break
                        } else {
                            delay(2000L)
                        }
                    }
                }
                if (!foundMakerInfo) {
                    if (response.end != null && response.end != latestNonEmptyBatch) {
                        latestNonEmptyBatch = response.end!!
                    }
                }
            }
        }
        assert(foundKeyAnnouncement)
        assert(foundTakerInfo)
        assert(foundMakerInfo)
    }

    @Test
    fun testCINMessageHandling() {
        //Setup decoder
        val decoder = Base64.getDecoder()

        //Restore the maker's public and private key
        val makerPubKeyB64 = "MIIBCgKCAQEAnnDB4zV2llEwwLHw7c934eV7t69Om52dpLcuctXtOtjGsaKyOAV96egmxX6+C+MptFST3yX4wO6qK3/NSuOHWBXIHkhQGZEdTHOn4HE9hHdw2axJ0F9GQKZeT8t8kw+58+n+nlbQUaFHUw5iypl3WiI1K7En4XV2egfXGk9ujElMqXZO/eFun3eAM+asT1g7o/k2ysOpY5X+sqesLsJ0gzaGH4jfDVuWifS5YhdgFKkBi1i3U1tfPdc3sN53uNCPEhxjjuOuYH5I3WI9VzjpJezYoSlwzI4hYNOjY0cWzZM9kqeUt93KzTpvX4FeQigT9UO20cs23M5NbIW4q7lA4wIDAQAB"
        val makerPrivKeyB64 = "MIIEogIBAAKCAQEAnnDB4zV2llEwwLHw7c934eV7t69Om52dpLcuctXtOtjGsaKyOAV96egmxX6+C+MptFST3yX4wO6qK3/NSuOHWBXIHkhQGZEdTHOn4HE9hHdw2axJ0F9GQKZeT8t8kw+58+n+nlbQUaFHUw5iypl3WiI1K7En4XV2egfXGk9ujElMqXZO/eFun3eAM+asT1g7o/k2ysOpY5X+sqesLsJ0gzaGH4jfDVuWifS5YhdgFKkBi1i3U1tfPdc3sN53uNCPEhxjjuOuYH5I3WI9VzjpJezYoSlwzI4hYNOjY0cWzZM9kqeUt93KzTpvX4FeQigT9UO20cs23M5NbIW4q7lA4wIDAQABAoIBACWe/ZLfS4DG144x0lUNedhUPsuvXzl5NAj8DBXtcQ6TkZ51VN8TgsHrQ2WKwkKdVnZAzPnkEMxy/0oj5xG8tBL43RM/tXFUsUHJhpe3G9Xb7JprG/3T2aEZP/Sviy16QvvFWJWtZHq1knOIy3Fy/lGTJM/ymVciJpc0TGGtccDyeQDBxaoQrr1r4Q9q5CMED/kEXq5KNLmzbfB1WInQZJ7wQhtyyAJiXJxKIeR3hVGR1dfBJGSbIIgYA5sYv8HPnXrorU7XEgDWLkILjSNgCvaGOgC5B4sgTB1pmwPQ173ee3gbn+PCai6saU9lciXeCteQp9YRBBWfwl+DDy5oGsUCgYEA0TB+kXbUgFyatxI46LLYRFGYTHgOPZz6Reu2ZKRaVNWC75NHyFTQdLSxvYLnQTnKGmjLapCTUwapiEAB50tLSko/uVcf4bG44EhCfL4S8hmfS3uCczokhhBjR/tZxnamXb/T1Wn2X06QsPSYQQmZB7EoQ6G0u/K792YgGn/qh+cCgYEAweUWInTK5nIAGyA/k0v0BNOefNTvfgV25wfR6nvXM3SJamHUTuO8wZntekD/epd4EewTP57rEb9kCzwdQnMkAaT1ejr7pQE4RFAZcL86o2C998QS0k25fw5xUhRiOIxSMqK7RLkAlRsThel+6BzHQ+jHxB06te3yyIjxnqP576UCgYA7tvAqbhVzHvw7TkRYiNUbi39CNPM7u1fmJcdHK3NtzBU4dn6DPVLUPdCPHJMPF4QNzeRjYynrBXfXoQ3qDKBNcKyIJ8q+DpGL1JTGLywRWCcU0QkIA4zxiDQPFD0oXi5XjK7XuQvPYQoEuY3M4wSAIZ4w0DRbgosNsGVxqxoz+QKBgClYh3LLguTHFHy0ULpBLQTGd3pZEcTGt4cmZL3isI4ZYKAdwl8cMwj5oOk76P6kRAdWVvhvE+NR86xtojOkR95N5catwzF5ZB01E2e2b3OdUoT9+6F6z35nfwSoshUq3vBLQTGzXYtuHaillNk8IcW6YrbQIM/gsK/Qe+1/O/G9AoGAYJhKegiRuasxY7ig1viAdYmhnCbtKhOa6qsq4cvI4avDL+Qfcgq6E8V5xgUsPsl2QUGz4DkBDw+E0D1Z4uT60y2TTTPbK7xmDs7KZy6Tvb+UKQNYlxL++DKbjFvxz6VJg17btqid8sP+LMhT3oqfRSakyGS74Bn3NBpLUeonYkQ="
        val makerKeyPair = KeyPair(decoder.decode(makerPubKeyB64), decoder.decode(makerPrivKeyB64))
        val makerPublicKey = PublicKey(decoder.decode(makerPubKeyB64))

        //Restore the taker's public and private key
        val takerPubKeyB64 = "MIIBCgKCAQEAstQwQCanMBPJIEj1Mjc1m80sL3eJ/y1SDM3iVoDk2oNN6WOZly0GWbv1xjNMM94U8GLnYrzEGUek2IKcicBAVYhwsegeVo2DHOts72g6GpVWOPKndpT87raKCqSkd+IqR2OWAo+olGWmjWgAbesH/ojqJPNHaKlhi4b0JSwNAMfTP2HqcN2lXLXnSbR7F7MnrvjHbUxEUulthmX1mLId/7bznQ2hjyUP2yOQY92C7DFwVl/J33YV2F1GJbx5xGqB/cRRB+0hTRoqQvHscZAlGykWIVgvrdPw2JOsadQVePUhDBU5jvS5qyD6JxAlRWgN7FZsMTFLVM2XNW40N3jMIwIDAQAB"
        val takerPrivKeyB64 = "MIIEowIBAAKCAQEAstQwQCanMBPJIEj1Mjc1m80sL3eJ/y1SDM3iVoDk2oNN6WOZly0GWbv1xjNMM94U8GLnYrzEGUek2IKcicBAVYhwsegeVo2DHOts72g6GpVWOPKndpT87raKCqSkd+IqR2OWAo+olGWmjWgAbesH/ojqJPNHaKlhi4b0JSwNAMfTP2HqcN2lXLXnSbR7F7MnrvjHbUxEUulthmX1mLId/7bznQ2hjyUP2yOQY92C7DFwVl/J33YV2F1GJbx5xGqB/cRRB+0hTRoqQvHscZAlGykWIVgvrdPw2JOsadQVePUhDBU5jvS5qyD6JxAlRWgN7FZsMTFLVM2XNW40N3jMIwIDAQABAoIBADez/Kue3qkNILMbxrSzmdFEIaVPeP6xYUN3xi7ny2F9UQGH8smyTq4Y7D+mru/hF2ihhi2tWu/87w458QS/i8qYy3G/OeQABH03oCEauC6bodXvT9aSJg89cNZL3qcxHbZLAOkfUoWW/EBDyw5yDXVttHF6Dh491JKfoOELTamWD4KxIScR/Nf6ih6UqB/SwmLz1X5+fZpW4iGZXIRsPzOzDtDmoSGajNXoi0Ln2x9DkUeXpx9r7TTT9DBT0jTLbCUiB3LYU4I/VR6upm0bDUKKRi9VTkQjOAV5rD3qdoraPVRCSzjUVqCwL7jqfunXsG/hhRccD+Di5pXaCuPeOsECgYEA3p4LLVHDzLhF269oUcvflMoBUVKo9UNHL/wmyujdV+RwFi5J2sxVLgKHsdKHCy7FdrDmxax7Mrgh57KS3+zfdDhs98w181JLwgFxzEAxIP2PnHd4P3NEbxCxnxhILW4fEotUVzJWjjhEHXe5QhOW2z2yIZIOEqBzFfRx33kWrbMCgYEAzaUrDMaTkIkOoVI7BbNS7n5CBWL/DaPOID1UiL4eHWngeoOwaeI+CB0cxSrxngykue0xM3aI3KVFaeIYSdn7DZAxWAS3U143VApgLxgLyxZBtVX18HYiTZQx/PiTczMH6kFA5z0L7iNlf0uQrQQJgDzM6QY0kKasufoss+Baj9ECgYA1BjvvTXxvtKyfCQa2BPN6QytRLXklAiNgoJS03AZsuvKfteLNhMH9NYkQp+6WkUtjW/t7tfuaNxWMVJJ7V7ZZvl7mHvPywvVcfm+WkOuiygJ86E/x/Qid08Ia/POkLoikKB+srUbElU5UHoI35OaXzfgx2tITSbhf0FuXOQZX1QKBgAj7A4xFR8ByG89ztdwj3qVHoj51+klwM9o4k259Tvdd3k27XoLhPHBCRTVfELokNzVfZFyo+oUYOpXLJ+BhwpLvDxiW7CKZ5LSo11Z3KFywFiKDJIBhyFG2/Q/dEyNewSO7wcfXZKP7q70JYcIMgRW2kgRDHxyKCtT8VeNtEsdhAoGBAJHzNruW/ZS31o0rvQxHu8tBcd8osTsPNZBhuHs60mbPFRHwBaU8JSofl4XjR8B7K9vjYtxVYMEsIX6NqNf1JMXGDva/cTCHXyPuiCnuUMbHkK0YpsFxQABwYA+sOSlujwJwMNPu4ylzHL1HDyv9m4x74/NM20zDFW6MB/zD6G0c"
        val takerKeyPair = KeyPair(decoder.decode(takerPubKeyB64), decoder.decode(takerPrivKeyB64))
        val takerPublicKey = PublicKey(decoder.decode(takerPubKeyB64))

        //Restore offer id
        val offerId = decoder.decode("9tGMGTr0SbuySqE0QOsAMQ==")

        //Create payment details
        val makerPaymentDetails = SerializableUSD_SWIFT_Details(
            "USD-SWIFT",
            "Make Ker",
            "2039482",
            "MAK3940"
        )
        val takerPaymentDetails = SerializableUSD_SWIFT_Details(
            "USD-SWIFT",
            "Take Ker",
            "2039482",
            "TAK3940"
        )

        //Begin tracking messages that have been found
        var foundKeyAnnouncement = false
        var foundTakerInfo = false
        var foundMakerInfo = false

        val matrixRestClient = MatrixApiClient(
            baseUrl = Url("http://matrix.org"),
        ).apply { accessToken.value = "" }
        runBlocking {
            //Perform initial sync, getting 10 most recent messages
            val response = matrixRestClient.sync.syncOnce(timeout = 60000).getOrThrow()
            val CINRoomId = RoomId("!WEuJJHaRpDvkbSveLu:matrix.org")
            val CINRoom = response.room!!.join!!.get(CINRoomId)
            val previousBatch = CINRoom!!.timeline!!.previousBatch
            val latestEvents = CINRoom!!.timeline!!.events!!

            //Search for all three swap-related messages in 10 most recent messages
            for (event in latestEvents) {
                if (event.content is TextMessageEventContent) {
                    if (!foundKeyAnnouncement) {
                        val restoredPublicKey = parsePublicKeyAnnouncement((event.content as TextMessageEventContent).body,
                            makerKeyPair.interfaceId, offerId)
                        if (restoredPublicKey != null) {
                            foundKeyAnnouncement = true
                            assert(Arrays.equals(restoredPublicKey.interfaceId, makerKeyPair.interfaceId))
                        }
                    } else if (!foundTakerInfo) {
                        val parsingResults = parseTakerInfoMessage((event.content as TextMessageEventContent).body,
                            makerKeyPair, takerKeyPair.interfaceId, offerId)
                        if (parsingResults != null) {
                            foundTakerInfo = true
                            assert(Arrays.equals(parsingResults.first.interfaceId, takerKeyPair.interfaceId))
                            assert(takerPaymentDetails.equals(parsingResults.second))
                        }
                    } else if (!foundMakerInfo) {
                        val restoredMakerInfo = parseMakerInfoMessage((event.content as TextMessageEventContent).body,
                            takerKeyPair, makerPublicKey, offerId)
                        if (restoredMakerInfo != null) {
                            foundMakerInfo = true
                            assert(makerPaymentDetails.equals(restoredMakerInfo))
                        }
                    }
                }
            }
            assert(foundKeyAnnouncement)
            assert(foundTakerInfo)
            assert(foundMakerInfo)
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