package io.tesseractgroup.messagerouter

import org.amshove.kluent.shouldEqual
import org.junit.Assert
import org.junit.Test
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationTargetException

/**
 * MessageRouterApp
 * Created by matt on 2/22/18.
 */
/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class MessageRouterTests {

    var router = MessageRouter<Int>()

    @Test
    fun testAddFunction() {
        router.copyEntries().count() shouldEqual 0
        router.add { }
        router.copyEntries().count() shouldEqual 1
    }

    @Test
    fun testAddRecipient() {
        router.copyEntries().count() shouldEqual 0
        val recipient = MessageRouterTestHelper()
        router.add(recipient, recipient.doNothing)
        router.copyEntries().count() shouldEqual 1
    }

    @Test
    fun testRemoveFunction() {
        router.copyEntries().count() shouldEqual 0
        val entry = router.add { }
        router.copyEntries().count() shouldEqual 1
        router.remove(entry)
        router.copyEntries().count() shouldEqual 0
    }

    @Test
    fun testRemoveRecipient() {
        router.copyEntries().count() shouldEqual 0
        val recipient = MessageRouterTestHelper()
        val entry = router.add(recipient, recipient.doNothing )
        router.copyEntries().count() shouldEqual 1
        router.remove(entry)
        router.copyEntries().count() shouldEqual 0
    }

    @Test
    fun testSend() {
        // Sends a varying number of messages to a varying number of recipients.
        for (recipientCount in 0..3) {
            for (messageCount in 0..3) {
                send(recipientCount, messageCount)
            }
        }
    }

    @Test
    fun testSendAfterRemove() {
        val entry = router.add { Assert.fail("Should not be called") }
        router.remove(entry)
        router.send(42)
    }

    data class TupleMessage(val string: String, val int: Int)

    @Test
    fun testSendTuple() {
        var count = 0
        val tupleRouter = MessageRouter<TupleMessage>()
        tupleRouter.add { count += 1 }
        tupleRouter.send(TupleMessage("Hello", 42))
        count shouldEqual 1
    }

    @Test
    fun testMemoryLeak() {
        // Creates a recipient in this scope.
        var recipient: MessageRouterTestHelper? = MessageRouterTestHelper()

        router.add(recipient!!, recipient!!.fail)
        val verifier = MemoryLeakVerifier(recipient)
        router.remove(recipient)
        recipient = null
        verifier.assertGarbageCollected("Should b gone")
        router.send(42)
    }

    // MARK: - Helpers

    private fun send(recipientCount: Int, messageCount: Int) {
        var count = 0
        val message = 42

        // Adds n recipients.
        if (recipientCount > 0) {
            for (i in 1..recipientCount) {
                router.add { n ->
                    count += 1;
                    n shouldEqual message
                }
            }
        }

        // Sends m messages to n recipients.
        if (messageCount > 0) {
            for (i in 1..messageCount) {
                router.send(message)
            }
        }
        count shouldEqual recipientCount * messageCount
    }
}

private class MessageRouterTestHelper {

    var hello = "hello"

    val fail = { int: Int ->
        hello = "Goodbye"
        Assert.fail("Should not be called.")
    }

    val doNothing = { int: Int ->
        hello = "goodbye"
    }
}

class MemoryLeakVerifier(testObject: Any) {

    private val reference: WeakReference<Any>

    val testObject: Any?
        get() = reference.get()

    init {
        this.reference = WeakReference(testObject)
    }

    /**
     * Attempts to perform a full garbage collection so that all weak references will be removed. Usually only
     * a single GC is required, but there have been situations where some unused memory is not cleared up on the
     * first pass. This method performs a full garbage collection and then validates that the weak reference
     * now has been cleared. If it hasn't then the thread will sleep for 50 milliseconds and then retry up to
     * 10 more times. If after this the object still has not been collected then the assertion will fail.
     *
     *
     * Based upon the method described in: http://www.javaworld.com/javaworld/javatips/jw-javatip130.html
     */
    fun assertGarbageCollected(name: String) {
        val runtime = Runtime.getRuntime()
        for (i in 0 until MAX_GC_ITERATIONS) {
            runtime.runFinalization()
            runtime.gc()
            if (testObject == null)
                break

            // Pause for a while and then go back around the loop to try again...
            try {
                Thread.sleep(GC_SLEEP_TIME.toLong())
            } catch (e: InterruptedException) {
                // Ignore any interrupts and just try again...
            } catch (e: InvocationTargetException) {
                // Ignore any interrupts and just try again...
            }

        }
        testObject shouldEqual null
    }

    companion object {
        private val MAX_GC_ITERATIONS = 50
        private val GC_SLEEP_TIME = 100
    }
}
