/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.badlogic.gdx.scenes.scene2d.utils

/**
 * Manages drag and drop operations through registered drag sources and drop targets.
 *
 * @author Nathan Sweet
 */
class DragAndDrop {

    /**
     * Returns the current drag source, or null.
     */
    var dragSource: Source? = null

    /**
     * Returns the current drag payload, or null.
     */
    var dragPayload: Payload? = null
    var dragActor: Actor? = null
    var target: Target? = null
    var isValidTarget = false
    val targets: Array<Target> = Array()
    val sourceListeners: ObjectMap<Source, DragListener> = ObjectMap()
    private var tapSquareSize = 8f
    private var button = 0
    var dragActorX = 0f
    var dragActorY = 0f
    var touchOffsetX = 0f
    var touchOffsetY = 0f
    var dragValidTime: Long = 0

    /**
     * Time in milliseconds that a drag must take before a drop will be considered valid. This ignores an accidental drag and drop
     * that was meant to be a click. Default is 250.
     */
    var dragTime = 250
    var activePointer = -1
    var cancelTouchFocus = true
    var keepWithinStage = true
    fun addSource(source: Source) {
        val listener: DragListener = object : DragListener() {
            override fun dragStart(event: InputEvent, x: Float, y: Float, pointer: Int) {
                if (activePointer != -1) {
                    event.stop()
                    return
                }
                activePointer = pointer
                dragValidTime = java.lang.System.currentTimeMillis() + dragTime
                dragSource = source
                dragPayload = source.dragStart(event, getTouchDownX(), getTouchDownY(), pointer)
                event.stop()
                if (cancelTouchFocus && dragPayload != null) {
                    val stage: Stage = source.getActor().getStage()
                    if (stage != null) stage.cancelTouchFocusExcept(this, source.getActor())
                }
            }

            override fun drag(event: InputEvent, x: Float, y: Float, pointer: Int) {
                if (dragPayload == null) return
                if (pointer != activePointer) return
                source.drag(event, x, y, pointer)
                val stage: Stage = event.getStage()
                if (dragActor != null) {
                    dragActor.remove() // Remove so it cannot be hit (Touchable.disabled isn't enough).
                    dragActor = null
                }

                // Find target.
                var newTarget: Target? = null
                isValidTarget = false
                val stageX: Float = event.getStageX() + touchOffsetX
                val stageY: Float = event.getStageY() + touchOffsetY
                var hit: Actor = event.getStage().hit(stageX, stageY, true) // Prefer touchable actors.
                if (hit == null) hit = event.getStage().hit(stageX, stageY, false)
                if (hit != null) {
                    var i = 0
                    val n = targets.size
                    while (i < n) {
                        val target = targets[i]
                        if (!target.actor.isAscendantOf(hit)) {
                            i++
                            continue
                        }
                        newTarget = target
                        target.actor.stageToLocalCoordinates(tmpVector.set(stageX, stageY))
                        break
                        i++
                    }
                }
                // If over a new target, notify the former target that it's being left behind.
                if (newTarget !== target) {
                    if (target != null) target!!.reset(source, dragPayload)
                    target = newTarget
                }
                // Notify new target of drag.
                if (newTarget != null) isValidTarget = newTarget.drag(source, dragPayload, tmpVector.x, tmpVector.y, pointer)

                // Add and position the drag actor.
                var actor: Actor? = null
                if (target != null) actor = if (isValidTarget) dragPayload!!.validDragActor else dragPayload!!.invalidDragActor
                if (actor == null) actor = dragPayload!!.dragActor
                dragActor = actor
                if (actor == null) return
                stage.addActor(actor)
                var actorX: Float = event.getStageX() - actor.getWidth() + dragActorX
                var actorY: Float = event.getStageY() + dragActorY
                if (keepWithinStage) {
                    if (actorX < 0) actorX = 0f
                    if (actorY < 0) actorY = 0f
                    if (actorX + actor.getWidth() > stage.getWidth()) actorX = stage.getWidth() - actor.getWidth()
                    if (actorY + actor.getHeight() > stage.getHeight()) actorY = stage.getHeight() - actor.getHeight()
                }
                actor.setPosition(actorX, actorY)
            }

            override fun dragStop(event: InputEvent, x: Float, y: Float, pointer: Int) {
                if (pointer != activePointer) return
                activePointer = -1
                if (dragPayload == null) return
                if (java.lang.System.currentTimeMillis() < dragValidTime) isValidTarget = false
                if (dragActor != null) dragActor.remove()
                if (isValidTarget) {
                    val stageX: Float = event.getStageX() + touchOffsetX
                    val stageY: Float = event.getStageY() + touchOffsetY
                    target!!.actor.stageToLocalCoordinates(tmpVector.set(stageX, stageY))
                    target!!.drop(source, dragPayload, tmpVector.x, tmpVector.y, pointer)
                }
                source.dragStop(event, x, y, pointer, dragPayload, if (isValidTarget) target else null)
                if (target != null) target!!.reset(source, dragPayload)
                dragSource = null
                dragPayload = null
                target = null
                isValidTarget = false
                dragActor = null
            }
        }
        listener.setTapSquareSize(tapSquareSize)
        listener.setButton(button)
        source.actor.addCaptureListener(listener)
        sourceListeners.put(source, listener)
    }

    fun removeSource(source: Source) {
        val dragListener: DragListener = sourceListeners.remove(source)
        source.actor.removeCaptureListener(dragListener)
    }

    fun addTarget(target: Target?) {
        targets.add(target)
    }

    fun removeTarget(target: Target?) {
        targets.removeValue(target, true)
    }

    /**
     * Removes all targets and sources.
     */
    fun clear() {
        targets.clear()
        for (entry in sourceListeners.entries()) entry.key.actor.removeCaptureListener(entry.value)
        sourceListeners.clear()
    }

    /**
     * Cancels the touch focus for everything except the specified source.
     */
    fun cancelTouchFocusExcept(except: Source) {
        val listener: DragListener = sourceListeners.get(except) ?: return
        val stage: Stage = except.getActor().getStage()
        if (stage != null) stage.cancelTouchFocusExcept(listener, except.getActor())
    }

    /**
     * Sets the distance a touch must travel before being considered a drag.
     */
    fun setTapSquareSize(halfTapSquareSize: Float) {
        tapSquareSize = halfTapSquareSize
    }

    /**
     * Sets the button to listen for, all other buttons are ignored. Default is [Buttons.LEFT]. Use -1 for any button.
     */
    fun setButton(button: Int) {
        this.button = button
    }

    fun setDragActorPosition(dragActorX: Float, dragActorY: Float) {
        this.dragActorX = dragActorX
        this.dragActorY = dragActorY
    }

    /**
     * Sets an offset in stage coordinates from the touch position which is used to determine the drop location. Default is
     * 0,0.
     */
    fun setTouchOffset(touchOffsetX: Float, touchOffsetY: Float) {
        this.touchOffsetX = touchOffsetX
        this.touchOffsetY = touchOffsetY
    }

    val isDragging: Boolean
        get() = dragPayload != null

    /**
     * Returns the current drag actor, or null.
     */
    fun getDragActor(): Actor? {
        return dragActor
    }

    /**
     * Returns true if a drag is in progress and the [drag time][.setDragTime] has elapsed since the drag started.
     */
    val isDragValid: Boolean
        get() = dragPayload != null && java.lang.System.currentTimeMillis() >= dragValidTime

    /**
     * When true (default), the [Stage.cancelTouchFocus] touch focus} is cancelled if
     * [dragStart][Source.dragStart] returns non-null. This ensures the DragAndDrop is the only
     * touch focus listener, eg when the source is inside a [ScrollPane] with flick scroll enabled.
     */
    fun setCancelTouchFocus(cancelTouchFocus: Boolean) {
        this.cancelTouchFocus = cancelTouchFocus
    }

    fun setKeepWithinStage(keepWithinStage: Boolean) {
        this.keepWithinStage = keepWithinStage
    }

    /**
     * A source where a payload can be dragged from.
     *
     * @author Nathan Sweet
     */
    abstract class Source(actor: Actor?) {

        val actor: Actor

        /**
         * Called when a drag is started on the source. The coordinates are in the source's local coordinate system.
         *
         * @return If null the drag will not affect any targets.
         */
        abstract fun dragStart(event: InputEvent?, x: Float, y: Float, pointer: Int): Payload?

        /**
         * Called repeatedly during a drag which started on this source.
         */
        fun drag(event: InputEvent?, x: Float, y: Float, pointer: Int) {}

        /**
         * Called when a drag for the source is stopped. The coordinates are in the source's local coordinate system.
         *
         * @param payload null if dragStart returned null.
         * @param target  null if not dropped on a valid target.
         */
        fun dragStop(event: InputEvent?, x: Float, y: Float, pointer: Int, payload: Payload?, target: Target?) {}
        fun getActor(): Actor {
            return actor
        }

        init {
            if (actor == null) throw java.lang.IllegalArgumentException("actor cannot be null.")
            this.actor = actor
        }
    }

    /**
     * A target where a payload can be dropped to.
     *
     * @author Nathan Sweet
     */
    abstract class Target(actor: Actor?) {

        val actor: Actor

        /**
         * Called when the payload is dragged over the target. The coordinates are in the target's local coordinate system.
         *
         * @return true if this is a valid target for the payload.
         */
        abstract fun drag(source: Source?, payload: Payload?, x: Float, y: Float, pointer: Int): Boolean

        /**
         * Called when the payload is no longer over the target, whether because the touch was moved or a drop occurred. This is
         * called even if [.drag] returned false.
         */
        fun reset(source: Source?, payload: Payload?) {}

        /**
         * Called when the payload is dropped on the target. The coordinates are in the target's local coordinate system. This is
         * not called if [.drag] returned false.
         */
        abstract fun drop(source: Source?, payload: Payload?, x: Float, y: Float, pointer: Int)
        fun getActor(): Actor {
            return actor
        }

        init {
            if (actor == null) throw java.lang.IllegalArgumentException("actor cannot be null.")
            this.actor = actor
            val stage: Stage = actor.getStage()
            if (stage != null && actor === stage.getRoot()) throw java.lang.IllegalArgumentException("The stage root cannot be a drag and drop target.")
        }
    }

    /**
     * The payload of a drag and drop operation. Actors can be optionally provided to follow the cursor and change when over a
     * target. Such Actors will be added and removed from the stage automatically during the drag operation. Care should be taken
     * when using the source Actor as a payload drag actor.
     */
    class Payload {

        var dragActor: Actor? = null
        var validDragActor: Actor? = null
        var invalidDragActor: Actor? = null
        var `object`: Any? = null
            set(object) {
                field = `object`
            }

        fun setDragActor(dragActor: Actor?) {
            this.dragActor = dragActor
        }

        fun getDragActor(): Actor? {
            return dragActor
        }

        fun setValidDragActor(validDragActor: Actor?) {
            this.validDragActor = validDragActor
        }

        fun getValidDragActor(): Actor? {
            return validDragActor
        }

        fun setInvalidDragActor(invalidDragActor: Actor?) {
            this.invalidDragActor = invalidDragActor
        }

        fun getInvalidDragActor(): Actor? {
            return invalidDragActor
        }
    }

    companion object {
        val tmpVector: Vector2 = Vector2()
    }
}
