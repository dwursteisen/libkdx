package com.badlogic.gdx.scenes.scene2d.actions

/**
 * Adds a listener to the actor for a specific event type and does not complete until [.handle] returns true.
 *
 * @author JavadocMD
 * @author Nathan Sweet
 */
abstract class EventAction<T : Event?>(eventClass: java.lang.Class<out T?>?) : Action() {

    val eventClass: java.lang.Class<out T?>?
    var result = false
    var isActive = false
    private val listener: EventListener? = object : EventListener() {
        fun handle(event: Event?): Boolean {
            if (!isActive || !ClassReflection.isInstance(eventClass, event)) return false
            result = this@EventAction.handle(event as T?)
            return result
        }
    }

    fun restart() {
        result = false
        isActive = false
    }

    fun setTarget(newTarget: Actor?) {
        if (target != null) target.removeListener(listener)
        super.setTarget(newTarget)
        if (newTarget != null) newTarget.addListener(listener)
    }

    /**
     * Called when the specific type of event occurs on the actor.
     *
     * @return true if the event should be considered [handled][Event.handle] and this EventAction considered complete.
     */
    abstract fun handle(event: T?): Boolean
    fun act(delta: Float): Boolean {
        isActive = true
        return result
    }

    init {
        this.eventClass = eventClass
    }
}
