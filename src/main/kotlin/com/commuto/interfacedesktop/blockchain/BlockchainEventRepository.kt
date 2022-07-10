package com.commuto.interfacedesktop.blockchain

/**
 * A wrapper around a [MutableList] of blockchain events.
 * @property events The [List] of events that this class wraps.
 */
open class BlockchainEventRepository<EventType> {

    private val events: MutableList<EventType> = mutableListOf()

    /**
     * Appends a new [EventType] to the [MutableList] that this class wraps.
     */
    open fun append(element: EventType) {
        events.add(element)
    }

    /**
     * Removes [elementToRemove] from the [MutableList] that this class wraps.
     */
    open fun remove(elementToRemove: EventType) {
        events.removeIf {
            it == elementToRemove
        }
    }
}