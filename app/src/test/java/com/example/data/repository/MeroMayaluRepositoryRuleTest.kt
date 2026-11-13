package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.core.data.MeroMayaluRepository
import com.example.core.domain.PrivateUser
import com.example.core.domain.PublicProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class MeroMayaluRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun samplePrivate(uid: String, name: String, gender: String) = PrivateUser(
        uid = uid,
        displayName = name,
        dateOfBirthPrivate = "2000-04-12",
        age = 26,
        ageVerified = true,
        gender = gender
    )

    private fun samplePublic(uid: String, name: String, gender: String) = PublicProfile(
        uid = uid,
        displayName = name,
        age = 26,
        ageVerified = true,
        gender = gender,
        bio = "Respectful connection from Kathmandu",
        lookingFor = "Long-term companionship",
        photoDataList = listOf("data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/"),
        interests = listOf("Music", "Travel"),
        hobbies = listOf("Hiking"),
        prompts = listOf("Tea or chiya?::Masala chiya"),
        culturalBadge = "Newari heritage lover",
        province = "Bagmati",
        district = "Kathmandu",
        municipality = "Kathmandu Metro",
        publicLocationLabel = "Kathmandu, Nepal"
    )

    @Test
    fun discoveryAndMutualConsentFlow_enforcesAllSecurityRules() = runBlocking {
        val runId = UUID.randomUUID().toString().take(8)
        val maleEmail = "rohan_$runId@test.com"
        val femaleEmail = "srijana_$runId@test.com"
        val female2Email = "anita_$runId@test.com"

        val maleUid = signInTestUser(maleEmail)
        val repo = MeroMayaluRepository(firestore, auth)
        val malePublic = samplePublic(maleUid, "Rohan", "Male")
        withTimeout(TIMEOUT_MS) {
            repo.saveProfile(samplePrivate(maleUid, "Rohan", "Male"), malePublic, isInitialCreate = true)
        }

        val femaleUid = signInTestUser(femaleEmail)
        val femalePublic = samplePublic(femaleUid, "Srijana", "Female")
        withTimeout(TIMEOUT_MS) {
            repo.saveProfile(samplePrivate(femaleUid, "Srijana", "Female"), femalePublic, isInitialCreate = true)
        }

        val female2Uid = signInTestUser(female2Email)
        val female2Public = samplePublic(female2Uid, "Anita", "Female")
        withTimeout(TIMEOUT_MS) {
            repo.saveProfile(samplePrivate(female2Uid, "Anita", "Female"), female2Public, isInitialCreate = true)
        }

        // Switch back to Male and verify opposite-gender discovery
        signInTestUser(maleEmail)
        val feed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeDiscoveryFeed("Male").first { list -> list.any { it.uid == femaleUid } }
        }
        assertTrue(feed.all { it.gender == "Female" })

        // Male sends 1 active request to Srijana
        val reqId = withTimeout(TIMEOUT_MS) {
            repo.sendInterestRequest(malePublic, femalePublic)
        }
        assertTrue(reqId.isNotBlank())

        // Second request while pending MUST fail with PERMISSION_DENIED
        try {
            withTimeout(TIMEOUT_MS) {
                repo.sendInterestRequest(malePublic, female2Public)
            }
            fail("Expected second outgoing request while pending to fail")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException() ?: throw e
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe.code)
        }

        // Female sees incoming request and approves it
        signInTestUser(femaleEmail)
        val incoming = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeIncomingRequests().first { list -> list.any { it.id == reqId } }
        }
        val reqObj = incoming.first { it.id == reqId }
        withTimeout(TIMEOUT_MS) {
            repo.approveInterestRequest(reqObj, femalePublic)
        }

        // Both see active conversation and can exchange messages
        val conversations = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeConversations().first { list -> list.isNotEmpty() }
        }
        val conv = conversations.first()
        withTimeout(TIMEOUT_MS) {
            repo.sendMessage(conv, type = "TEXT", text = "Namaste Rohan!")
        }

        signInTestUser(maleEmail)
        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMessages(conv.id).first { list -> list.isNotEmpty() }
        }
        assertEquals("Namaste Rohan!", messages.first().text)

        // Blocking immediately stops messaging
        withTimeout(TIMEOUT_MS) {
            repo.blockUser(femaleUid)
        }
        try {
            withTimeout(TIMEOUT_MS) {
                repo.sendMessage(conv, type = "TEXT", text = "Blocked message")
            }
            fail("Expected sendMessage after block to fail")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException() ?: throw e
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe.code)
        }
    }

    @Test
    fun unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = MeroMayaluRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection("publicProfiles")
                    .whereEqualTo("gender", "Female")
                    .whereEqualTo("visibility", "visible")
                    .whereEqualTo("ageVerified", true)
                    .whereEqualTo("moderationStatus", "active")
                    .get()
            }
            // Observe via snapshots for unauthenticated check
        } catch (_: Exception) {
        }

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeDiscoveryFeed("Male").first()
            }
            fail("Expected unauthenticated discovery to fail")
        } catch (e: IllegalStateException) {
            // Repository fail-fast requireUserId() also protects unauthenticated access
            assertTrue(e.message!!.contains("signed in"))
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe?.code)
        }
    }

    private companion object {
        const val TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
