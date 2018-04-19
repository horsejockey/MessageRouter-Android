package io.tesseractgroup.messagerouter

import java.lang.ref.WeakReference

/**
 * MessageRouterApp
 * Created by matt on 2/22/18.
 */
typealias Recipient = Any

@Suppress("UNCHECKED_CAST")
/**
A class for sending messages of type T to the registered recipients. Can be a
replacement to many types of delegate callbacks.
 */
class MessageRouter<T> {

    private var entries = listOf< MessageRouterEntry<T, Recipient> >()
    private val lock = java.lang.Object()

    /**
    Convenience function for add(_:_:). Simply takes a function that will
    receive all messages for the life time of this instance, or until the
    returned entry is removed.

    - parameter function: The function to receive any messages.
    - returns: An opaque object that can be used to stop any further messages.
     */
//    fun add(function: (T)->Unit) : MessageRouterEntry<T, Recipient> {
//        return add(this as Recipient, function, true)
//    }

    /**
    The given function will receive any messages for the life time of `object`.
    Ensures that only one callback will be saved for the provided `object` instance.
    Won't be called if there is already an entry for this recipient

    - parameter object: The object that owns the given function.
    - parameter function: The function that will be called with any messages. Typically a function on `object`.
    - returns: An opaque object that can be used to stop any further messages.
     */
    fun <R: Recipient> add(recipient: R, function: (T)->Unit, allowMultipleEntries: Boolean = false) : MessageRouterEntry<T, Recipient> {
        val entry = MessageRouterEntry<T, R>(recipient, function) as MessageRouterEntry<T, Recipient>
        synchronized(lock){
            entries = entries.filter { it.getRecipient() != null && (allowMultipleEntries || it.getRecipient() != entry.getRecipient()) }
            entries += entry
        }
        return entry
    }

    /**
    Removes the given entry from the list of recipients.

    - parameter entry: The entry to remove.
     */
    fun remove(entry: MessageRouterEntry<T, Recipient>){
        synchronized(lock){
            entries = entries.filter { it.getRecipient() != null && it !== entry }
        }
    }

    /**
    Removes all entries for the provided recipient.

    - parameter recipient: The recipient to remove.
     */
    fun remove(recipient: Recipient){
        synchronized(lock){
            entries = entries.filter { it.getRecipient() != null && it.getRecipient() !== recipient }
        }
    }

    /**
    Sends the given message to all the registered recipients.

    - parameter message: The message to send to the recipients.
     */
    fun send(message: T){
        var handlers = listOf<(T)->Unit>()

        synchronized(lock){
            val newEntries = mutableListOf<MessageRouterEntry<T, Recipient>>()

            for(entry in entries){
                val recipient = entry.getRecipient()
                if (recipient != null){
                    handlers += entry.function
                    newEntries.add(entry)
                }
            }

            entries = newEntries
        }

        for(handler in handlers){
            handler(message)
        }
    }


    /**
    Convenience method for getting a copy of the entries. This is intended only
    for testing. Since `entries` is private.

    - returns: A copy of the registered recipient entries.
     */
    fun copyEntries() : List< MessageRouterEntry<T, Recipient> > {
        var copiedEntries = listOf< MessageRouterEntry<T, Recipient> >()

        synchronized(lock) {
            for (entry in entries) {
                copiedEntries += entry
            }
        }

        return entries
    }

}

/// Opaque object for tracking message recipient info.
class MessageRouterEntry<T, in R :Recipient>(recipient: R, val function: (T) -> Unit) {

    private var recipient: WeakReference<R>? = null

    init {
        this.recipient = WeakReference(recipient)
    }

    fun getRecipient() : Any? {
        return recipient?.get()
    }
}
