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
package com.badlogic.gdx.scenes.scene2d

import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage.TouchFocus
import java.lang.RuntimeException

/**
 * 2D scene graph node. An actor has a position, rectangular size, origin, scale, rotation, Z index, and color. The position
 * corresponds to the unrotated, unscaled bottom left corner of the actor. The position is relative to the actor's parent. The
 * origin is relative to the position and is used for scale and rotation.
 *
 *
 * An actor has a list of in progress [actions][Action] that are applied to the actor (often over time). These are generally
 * used to change the presentation of the actor (moving it, resizing it, etc). See [.act], [Action], and its
 * many subclasses.
 *
 *
 * An actor has two kinds of listeners associated with it: "capture" and regular. The listeners are notified of events the actor
 * or its children receive. The regular listeners are designed to allow an actor to respond to events that have been delivered.
 * The capture listeners are designed to allow a parent or container actor to handle events before child actors. See [.fire]
 * for more details.
 *
 *
 * An [InputListener] can receive all the basic input events. More complex listeners (like [ClickListener] and
 * [ActorGestureListener]) can listen for and combine primitive events and recognize complex interactions like multi-touch
 * or pinch.
 *
 * @author mzechner
 * @author Nathan Sweet
 */
class Actor {

    private var stage: Stage? = null
    var parent: Group? = null
    private val listeners: DelayedRemovalArray<EventListener?>? = DelayedRemovalArray(0)
    private val captureListeners: DelayedRemovalArray<EventListener?>? = DelayedRemovalArray(0)
    private val actions: Array<Action?>? = Array(0)
    /**
     * @return May be null.
     * @see .setName
     */
    /**
     * Set the actor's name, which is used for identification convenience and by [.toString].
     *
     * @param name May be null.
     * @see Group.findActor
     */
    var name: String? = null
    private var touchable: Touchable? = Touchable.enabled

    /**
     * If false, the actor will not be drawn and will not receive touch events. Default is true.
     */
    var isVisible = true
    private var debug = false
    var x = 0f
    var y = 0f
    var width = 0f
    var height = 0f
    var originX = 0f
    var originY = 0f
    var scaleX = 1f
    var scaleY = 1f
    var rotation = 0f
    val color: Color? = Color(1, 1, 1, 1)
    /**
     * Returns an application specific object for convenience, or null.
     */
    /**
     * Sets an application specific object for convenience.
     */
    var userObject: Any? = null

    /**
     * Draws the actor. The batch is configured to draw in the parent's coordinate system.
     * [ This draw method][Batch.draw] is convenient to draw a rotated and scaled TextureRegion. [Batch.begin] has already been called on
     * the batch. If [Batch.end] is called to draw without the batch then [Batch.begin] must be called before the
     * method returns.
     *
     *
     * The default implementation does nothing.
     *
     * @param parentAlpha The parent alpha, to be multiplied with this actor's alpha, allowing the parent's alpha to affect all
     * children.
     */
    fun draw(batch: Batch?, parentAlpha: Float) {}

    /**
     * Updates the actor based on time. Typically this is called each frame by [Stage.act].
     *
     *
     * The default implementation calls [Action.act] on each action and removes actions that are complete.
     *
     * @param delta Time in seconds since the last frame.
     */
    fun act(delta: Float) {
        val actions: Array<Action?>? = actions
        if (actions!!.size === 0) return
        if (stage != null && stage.getActionsRequestRendering()) Gdx.graphics.requestRendering()
        try {
            var i = 0
            while (i < actions!!.size) {
                val action: Action? = actions[i]
                if (action!!.act(delta) && i < actions.size) {
                    val current: Action? = actions[i]
                    val actionIndex = if (current === action) i else actions.indexOf(action, true)
                    if (actionIndex != -1) {
                        actions.removeIndex(actionIndex)
                        action!!.setActor(null)
                        i--
                    }
                }
                i++
            }
        } catch (ex: RuntimeException) {
            val context = toString()
            throw RuntimeException("Actor: " + context.substring(0, java.lang.Math.min(context.length, 128)), ex)
        }
    }

    /**
     * Sets this actor as the event [target][Event.setTarget] and propagates the event to this actor and ancestor
     * actors as necessary. If this actor is not in the stage, the stage must be set before calling this method.
     *
     *
     * Events are fired in 2 phases:
     *
     *  1. The first phase (the "capture" phase) notifies listeners on each actor starting at the root and propagating downward to
     * (and including) this actor.
     *  1. The second phase notifies listeners on each actor starting at this actor and, if [Event.getBubbles] is true,
     * propagating upward to the root.
     *
     * If the event is [stopped][Event.stop] at any time, it will not propagate to the next actor.
     *
     * @return true if the event was [cancelled][Event.cancel].
     */
    fun fire(event: Event?): Boolean {
        if (event!!.getStage() == null) event!!.setStage(getStage())
        event.setTarget(this)

        // Collect ancestors so event propagation is unaffected by hierarchy changes.
        val ancestors: Array<Group?> = Pools.obtain(Array::class.java)
        var parent: Group? = parent
        while (parent != null) {
            ancestors.add(parent)
            parent = parent.parent
        }
        return try {
            // Notify all parent capture listeners, starting at the root. Ancestors may stop an event before children receive it.
            val ancestorsArray: Array<Any?> = ancestors.items
            for (i in ancestors.size - 1 downTo 0) {
                val currentTarget: Group? = ancestorsArray[i] as Group?
                currentTarget.notify(event, true)
                if (event!!.isStopped()) return event!!.isCancelled()
            }

            // Notify the target capture listeners.
            notify(event, true)
            if (event!!.isStopped()) return event!!.isCancelled()

            // Notify the target listeners.
            notify(event, false)
            if (!event.getBubbles()) return event!!.isCancelled()
            if (event!!.isStopped()) return event!!.isCancelled()

            // Notify all parent listeners, starting at the target. Children may stop an event before ancestors receive it.
            var i = 0
            val n = ancestors.size
            while (i < n) {
                (ancestorsArray[i] as Group?).notify(event, false)
                if (event!!.isStopped()) return event!!.isCancelled()
                i++
            }
            event!!.isCancelled()
        } finally {
            ancestors.clear()
            Pools.free(ancestors)
        }
    }

    /**
     * Notifies this actor's listeners of the event. The event is not propagated to any parents. Before notifying the listeners,
     * this actor is set as the [listener actor][Event.getListenerActor]. The event [target][Event.setTarget]
     * must be set before calling this method. If this actor is not in the stage, the stage must be set before calling this method.
     *
     * @param capture If true, the capture listeners will be notified instead of the regular listeners.
     * @return true of the event was [cancelled][Event.cancel].
     */
    fun notify(event: Event?, capture: Boolean): Boolean {
        if (event.getTarget() == null) throw java.lang.IllegalArgumentException("The event target cannot be null.")
        val listeners: DelayedRemovalArray<EventListener?> = if (capture) captureListeners else listeners
        if (listeners.size === 0) return event!!.isCancelled()
        event!!.setListenerActor(this)
        event.setCapture(capture)
        if (event!!.getStage() == null) event!!.setStage(stage)
        try {
            listeners.begin()
            var i = 0
            val n: Int = listeners.size
            while (i < n) {
                val listener: EventListener = listeners.get(i)
                if (listener!!.handle(event)) {
                    event!!.handle()
                    if (event is InputEvent) {
                        val inputEvent: InputEvent? = event as InputEvent?
                        if (inputEvent.getType() === Type.touchDown) {
                            event.getStage()!!.addTouchFocus(listener, this, inputEvent.getTarget(), inputEvent.getPointer(),
                                inputEvent.getButton())
                        }
                    }
                }
                i++
            }
            listeners.end()
        } catch (ex: RuntimeException) {
            val context = toString()
            throw RuntimeException("Actor: " + context.substring(0, java.lang.Math.min(context.length, 128)), ex)
        }
        return event!!.isCancelled()
    }

    /**
     * Returns the deepest [visible][.isVisible] (and optionally, [touchable][.getTouchable]) actor that contains
     * the specified point, or null if no actor was hit. The point is specified in the actor's local coordinate system (0,0 is the
     * bottom left of the actor and width,height is the upper right).
     *
     *
     * This method is used to delegate touchDown, mouse, and enter/exit events. If this method returns null, those events will not
     * occur on this Actor.
     *
     *
     * The default implementation returns this actor if the point is within this actor's bounds and this actor is visible.
     *
     * @param touchable If true, hit detection will respect the [touchability][.setTouchable].
     * @see Touchable
     */
    fun hit(x: Float, y: Float, touchable: Boolean): Actor? {
        if (touchable && this.touchable !== Touchable.enabled) return null
        if (!isVisible) return null
        return if (x >= 0 && x < width && y >= 0 && y < height) this else null
    }

    /**
     * Removes this actor from its parent, if it has a parent.
     *
     * @see Group.removeActor
     */
    fun remove(): Boolean {
        return if (parent != null) parent.removeActor(this, true) else false
    }

    /**
     * Add a listener to receive events that [hit][.hit] this actor. See [.fire].
     *
     * @see InputListener
     *
     * @see ClickListener
     */
    fun addListener(listener: EventListener?): Boolean {
        if (listener == null) throw java.lang.IllegalArgumentException("listener cannot be null.")
        if (!listeners.contains(listener, true)) {
            listeners.add(listener)
            return true
        }
        return false
    }

    fun removeListener(listener: EventListener?): Boolean {
        if (listener == null) throw java.lang.IllegalArgumentException("listener cannot be null.")
        return listeners.removeValue(listener, true)
    }

    fun getListeners(): DelayedRemovalArray<EventListener?>? {
        return listeners
    }

    /**
     * Adds a listener that is only notified during the capture phase.
     *
     * @see .fire
     */
    fun addCaptureListener(listener: EventListener?): Boolean {
        if (listener == null) throw java.lang.IllegalArgumentException("listener cannot be null.")
        if (!captureListeners.contains(listener, true)) captureListeners.add(listener)
        return true
    }

    fun removeCaptureListener(listener: EventListener?): Boolean {
        if (listener == null) throw java.lang.IllegalArgumentException("listener cannot be null.")
        return captureListeners.removeValue(listener, true)
    }

    fun getCaptureListeners(): DelayedRemovalArray<EventListener?>? {
        return captureListeners
    }

    fun addAction(action: Action?) {
        action!!.setActor(this)
        actions.add(action)
        if (stage != null && stage.getActionsRequestRendering()) Gdx.graphics.requestRendering()
    }

    fun removeAction(action: Action?) {
        if (actions.removeValue(action, true)) action!!.setActor(null)
    }

    fun getActions(): Array<Action?>? {
        return actions
    }

    /**
     * Returns true if the actor has one or more actions.
     */
    fun hasActions(): Boolean {
        return actions!!.size > 0
    }

    /**
     * Removes all actions on this actor.
     */
    fun clearActions() {
        for (i in actions!!.size - 1 downTo 0) actions[i]!!.setActor(null)
        actions.clear()
    }

    /**
     * Removes all listeners on this actor.
     */
    fun clearListeners() {
        listeners.clear()
        captureListeners.clear()
    }

    /**
     * Removes all actions and listeners on this actor.
     */
    fun clear() {
        clearActions()
        clearListeners()
    }

    /**
     * Returns the stage that this actor is currently in, or null if not in a stage.
     */
    fun getStage(): Stage? {
        return stage
    }

    /**
     * Called by the framework when this actor or any parent is added to a group that is in the stage.
     *
     * @param stage May be null if the actor or any parent is no longer in a stage.
     */
    fun setStage(stage: Stage?) {
        this.stage = stage
    }

    /**
     * Returns true if this actor is the same as or is the descendant of the specified actor.
     */
    fun isDescendantOf(actor: Actor?): Boolean {
        if (actor == null) throw java.lang.IllegalArgumentException("actor cannot be null.")
        var parent: Actor? = this
        do {
            if (parent === actor) return true
            parent = parent!!.parent
        } while (parent != null)
        return false
    }

    /**
     * Returns true if this actor is the same as or is the ascendant of the specified actor.
     */
    fun isAscendantOf(actor: Actor?): Boolean {
        var actor: Actor? = actor ?: throw java.lang.IllegalArgumentException("actor cannot be null.")
        do {
            if (actor === this) return true
            actor = actor!!.parent
        } while (actor != null)
        return false
    }

    /**
     * Returns this actor or the first ascendant of this actor that is assignable with the specified type, or null if none were
     * found.
     */
    fun <T : Actor?> firstAscendant(type: java.lang.Class<T?>?): T? {
        if (type == null) throw java.lang.IllegalArgumentException("actor cannot be null.")
        var actor: Actor? = this
        do {
            if (ClassReflection.isInstance(type, actor)) return actor as T?
            actor = actor!!.parent
        } while (actor != null)
        return null
    }

    /**
     * Returns true if the actor's parent is not null.
     */
    fun hasParent(): Boolean {
        return parent != null
    }

    /**
     * Returns the parent actor, or null if not in a group.
     */
    fun getParent(): Group? {
        return parent
    }

    /**
     * Called by the framework when an actor is added to or removed from a group.
     *
     * @param parent May be null if the actor has been removed from the parent.
     */
    fun setParent(parent: Group?) {
        this.parent = parent
    }

    /**
     * Returns true if input events are processed by this actor.
     */
    fun isTouchable(): Boolean {
        return touchable === Touchable.enabled
    }

    fun getTouchable(): Touchable? {
        return touchable
    }

    /**
     * Determines how touch events are distributed to this actor. Default is [Touchable.enabled].
     */
    fun setTouchable(touchable: Touchable?) {
        this.touchable = touchable
    }

    /**
     * Returns true if this actor and all ancestors are visible.
     */
    fun ancestorsVisible(): Boolean {
        var actor: Actor? = this
        do {
            if (!actor!!.isVisible) return false
            actor = actor.parent
        } while (actor != null)
        return true
    }

    /**
     * Returns true if this actor is the [keyboard focus][Stage.getKeyboardFocus] actor.
     */
    fun hasKeyboardFocus(): Boolean {
        val stage: Stage? = getStage()
        return stage != null && stage.getKeyboardFocus() === this
    }

    /**
     * Returns true if this actor is the [scroll focus][Stage.getScrollFocus] actor.
     */
    fun hasScrollFocus(): Boolean {
        val stage: Stage? = getStage()
        return stage != null && stage.getScrollFocus() === this
    }

    /**
     * Returns true if this actor is a target actor for touch focus.
     *
     * @see Stage.addTouchFocus
     */
    val isTouchFocusTarget: Boolean
        get() {
            val stage: Stage = getStage() ?: return false
            var i = 0
            val n: Int = stage.touchFocuses.size
            while (i < n) {
                if (stage.touchFocuses.get(i).target === this) return true
                i++
            }
            return false
        }

    /**
     * Returns true if this actor is a listener actor for touch focus.
     *
     * @see Stage.addTouchFocus
     */
    val isTouchFocusListener: Boolean
        get() {
            val stage: Stage = getStage() ?: return false
            var i = 0
            val n: Int = stage.touchFocuses.size
            while (i < n) {
                if (stage.touchFocuses.get(i).listenerActor === this) return true
                i++
            }
            return false
        }

    /**
     * Returns the X position of the actor's left edge.
     */
    fun getX(): Float {
        return x
    }

    /**
     * Returns the X position of the specified [alignment][Align].
     */
    fun getX(alignment: Int): Float {
        var x = x
        if (alignment and right.toInt() !== 0) x += width else if (alignment and left === 0) //
            x += width / 2
        return x
    }

    fun setX(x: Float) {
        if (this.x != x) {
            this.x = x
            positionChanged()
        }
    }

    /**
     * Sets the x position using the specified [alignment][Align]. Note this may set the position to non-integer
     * coordinates.
     */
    fun setX(x: Float, alignment: Int) {
        var x = x
        if (alignment and right.toInt() !== 0) x -= width else if (alignment and left === 0) //
            x -= width / 2
        if (this.x != x) {
            this.x = x
            positionChanged()
        }
    }

    /**
     * Returns the Y position of the actor's bottom edge.
     */
    fun getY(): Float {
        return y
    }

    fun setY(y: Float) {
        if (this.y != y) {
            this.y = y
            positionChanged()
        }
    }

    /**
     * Sets the y position using the specified [alignment][Align]. Note this may set the position to non-integer
     * coordinates.
     */
    fun setY(y: Float, alignment: Int) {
        var y = y
        if (alignment and top.toInt() !== 0) y -= height else if (alignment and bottom === 0) //
            y -= height / 2
        if (this.y != y) {
            this.y = y
            positionChanged()
        }
    }

    /**
     * Returns the Y position of the specified [alignment][Align].
     */
    fun getY(alignment: Int): Float {
        var y = y
        if (alignment and top.toInt() !== 0) y += height else if (alignment and bottom === 0) //
            y += height / 2
        return y
    }

    /**
     * Sets the position of the actor's bottom left corner.
     */
    fun setPosition(x: Float, y: Float) {
        if (this.x != x || this.y != y) {
            this.x = x
            this.y = y
            positionChanged()
        }
    }

    /**
     * Sets the position using the specified [alignment][Align]. Note this may set the position to non-integer
     * coordinates.
     */
    fun setPosition(x: Float, y: Float, alignment: Int) {
        var x = x
        var y = y
        if (alignment and right.toInt() !== 0) x -= width else if (alignment and left === 0) //
            x -= width / 2
        if (alignment and top.toInt() !== 0) y -= height else if (alignment and bottom === 0) //
            y -= height / 2
        if (this.x != x || this.y != y) {
            this.x = x
            this.y = y
            positionChanged()
        }
    }

    /**
     * Add x and y to current position
     */
    fun moveBy(x: Float, y: Float) {
        if (x != 0f || y != 0f) {
            this.x += x
            this.y += y
            positionChanged()
        }
    }

    fun getWidth(): Float {
        return width
    }

    fun setWidth(width: Float) {
        if (this.width != width) {
            this.width = width
            sizeChanged()
        }
    }

    fun getHeight(): Float {
        return height
    }

    fun setHeight(height: Float) {
        if (this.height != height) {
            this.height = height
            sizeChanged()
        }
    }

    /**
     * Returns y plus height.
     */
    val top: Float
        get() = y + height

    /**
     * Returns x plus width.
     */
    val right: Float
        get() = x + width

    /**
     * Called when the actor's position has been changed.
     */
    protected fun positionChanged() {}

    /**
     * Called when the actor's size has been changed.
     */
    protected fun sizeChanged() {}

    /**
     * Called when the actor's rotation has been changed.
     */
    protected fun rotationChanged() {}

    /**
     * Sets the width and height.
     */
    fun setSize(width: Float, height: Float) {
        if (this.width != width || this.height != height) {
            this.width = width
            this.height = height
            sizeChanged()
        }
    }

    /**
     * Adds the specified size to the current size.
     */
    fun sizeBy(size: Float) {
        if (size != 0f) {
            width += size
            height += size
            sizeChanged()
        }
    }

    /**
     * Adds the specified size to the current size.
     */
    fun sizeBy(width: Float, height: Float) {
        if (width != 0f || height != 0f) {
            this.width += width
            this.height += height
            sizeChanged()
        }
    }

    /**
     * Set bounds the x, y, width, and height.
     */
    fun setBounds(x: Float, y: Float, width: Float, height: Float) {
        if (this.x != x || this.y != y) {
            this.x = x
            this.y = y
            positionChanged()
        }
        if (this.width != width || this.height != height) {
            this.width = width
            this.height = height
            sizeChanged()
        }
    }

    /**
     * Sets the origin position which is relative to the actor's bottom left corner.
     */
    fun setOrigin(originX: Float, originY: Float) {
        this.originX = originX
        this.originY = originY
    }

    /**
     * Sets the origin position to the specified [alignment][Align].
     */
    fun setOrigin(alignment: Int) {
        originX = if (alignment and left !== 0) 0f else if (alignment and right.toInt() !== 0) width else width / 2
        originY = if (alignment and bottom !== 0) 0f else if (alignment and top.toInt() !== 0) height else height / 2
    }

    /**
     * Sets the scale for both X and Y
     */
    fun setScale(scaleXY: Float) {
        scaleX = scaleXY
        scaleY = scaleXY
    }

    /**
     * Sets the scale X and scale Y.
     */
    fun setScale(scaleX: Float, scaleY: Float) {
        this.scaleX = scaleX
        this.scaleY = scaleY
    }

    /**
     * Adds the specified scale to the current scale.
     */
    fun scaleBy(scale: Float) {
        scaleX += scale
        scaleY += scale
    }

    /**
     * Adds the specified scale to the current scale.
     */
    fun scaleBy(scaleX: Float, scaleY: Float) {
        this.scaleX += scaleX
        this.scaleY += scaleY
    }

    fun getRotation(): Float {
        return rotation
    }

    fun setRotation(degrees: Float) {
        if (rotation != degrees) {
            rotation = degrees
            rotationChanged()
        }
    }

    /**
     * Adds the specified rotation to the current rotation.
     */
    fun rotateBy(amountInDegrees: Float) {
        if (amountInDegrees != 0f) {
            rotation = (rotation + amountInDegrees) % 360
            rotationChanged()
        }
    }

    fun setColor(color: Color?) {
        this.color.set(color)
    }

    fun setColor(r: Float, g: Float, b: Float, a: Float) {
        color.set(r, g, b, a)
    }

    /**
     * Returns the color the actor will be tinted when drawn. The returned instance can be modified to change the color.
     */
    fun getColor(): Color? {
        return color
    }

    /**
     * Changes the z-order for this actor so it is in front of all siblings.
     */
    fun toFront() {
        setZIndex(Int.MAX_VALUE)
    }

    /**
     * Changes the z-order for this actor so it is in back of all siblings.
     */
    fun toBack() {
        setZIndex(0)
    }

    /**
     * Sets the z-index of this actor. The z-index is the index into the parent's [children][Group.getChildren], where a
     * lower index is below a higher index. Setting a z-index higher than the number of children will move the child to the front.
     * Setting a z-index less than zero is invalid.
     *
     * @return true if the z-index changed.
     */
    fun setZIndex(index: Int): Boolean {
        var index = index
        if (index < 0) throw java.lang.IllegalArgumentException("ZIndex cannot be < 0.")
        val parent: Group = parent ?: return false
        val children: Array<Actor?> = parent.children
        if (children.size === 1) return false
        index = java.lang.Math.min(index, children.size - 1)
        if (children[index] === this) return false
        if (!children.removeValue(this, true)) return false
        children.insert(index, this)
        return true
    }

    /**
     * Returns the z-index of this actor.
     *
     * @see .setZIndex
     */
    val zIndex: Int
        get() {
            val parent: Group = parent ?: return -1
            return parent.children.indexOf(this, true)
        }
    /**
     * Clips the specified screen aligned rectangle, specified relative to the transform matrix of the stage's Batch. The
     * transform matrix and the stage's camera must not have rotational components. Calling this method must be followed by a call
     * to [.clipEnd] if true is returned.
     *
     * @return false if the clipping area is zero and no drawing should occur.
     * @see ScissorStack
     */
    /**
     * Calls [.clipBegin] to clip this actor's bounds.
     */
    @JvmOverloads
    fun clipBegin(x: Float = this.x, y: Float = this.y, width: Float = this.width, height: Float = this.height): Boolean {
        if (width <= 0 || height <= 0) return false
        val stage: Stage = stage ?: return false
        val tableBounds: Rectangle = Rectangle.tmp
        tableBounds.x = x
        tableBounds.y = y
        tableBounds.width = width
        tableBounds.height = height
        val scissorBounds: Rectangle = Pools.obtain(Rectangle::class.java)
        stage.calculateScissors(tableBounds, scissorBounds)
        if (ScissorStack.pushScissors(scissorBounds)) return true
        Pools.free(scissorBounds)
        return false
    }

    /**
     * Ends clipping begun by [.clipBegin].
     */
    fun clipEnd() {
        Pools.free(ScissorStack.popScissors())
    }

    /**
     * Transforms the specified point in screen coordinates to the actor's local coordinate system.
     *
     * @see Stage.screenToStageCoordinates
     */
    fun screenToLocalCoordinates(screenCoords: Vector2?): Vector2? {
        val stage: Stage = stage ?: return screenCoords
        return stageToLocalCoordinates(stage.screenToStageCoordinates(screenCoords))
    }

    /**
     * Transforms the specified point in the stage's coordinates to the actor's local coordinate system.
     */
    fun stageToLocalCoordinates(stageCoords: Vector2?): Vector2? {
        if (parent != null) parent.stageToLocalCoordinates(stageCoords)
        parentToLocalCoordinates(stageCoords)
        return stageCoords
    }

    /**
     * Converts the coordinates given in the parent's coordinate system to this actor's coordinate system.
     */
    fun parentToLocalCoordinates(parentCoords: Vector2?): Vector2? {
        val rotation = rotation
        val scaleX = scaleX
        val scaleY = scaleY
        val childX = x
        val childY = y
        if (rotation == 0f) {
            if (scaleX == 1f && scaleY == 1f) {
                parentCoords.x -= childX
                parentCoords.y -= childY
            } else {
                val originX = originX
                val originY = originY
                parentCoords.x = (parentCoords.x - childX - originX) / scaleX + originX
                parentCoords.y = (parentCoords.y - childY - originY) / scaleY + originY
            }
        } else {
            val cos = java.lang.Math.cos(rotation * MathUtils.degreesToRadians) as Float
            val sin = java.lang.Math.sin(rotation * MathUtils.degreesToRadians) as Float
            val originX = originX
            val originY = originY
            val tox: Float = parentCoords.x - childX - originX
            val toy: Float = parentCoords.y - childY - originY
            parentCoords.x = (tox * cos + toy * sin) / scaleX + originX
            parentCoords.y = (tox * -sin + toy * cos) / scaleY + originY
        }
        return parentCoords
    }

    /**
     * Transforms the specified point in the actor's coordinates to be in screen coordinates.
     *
     * @see Stage.stageToScreenCoordinates
     */
    fun localToScreenCoordinates(localCoords: Vector2?): Vector2? {
        val stage: Stage = stage ?: return localCoords
        return stage.stageToScreenCoordinates(localToAscendantCoordinates(null, localCoords))
    }

    /**
     * Transforms the specified point in the actor's coordinates to be in the stage's coordinates.
     */
    fun localToStageCoordinates(localCoords: Vector2?): Vector2? {
        return localToAscendantCoordinates(null, localCoords)
    }

    /**
     * Transforms the specified point in the actor's coordinates to be in the parent's coordinates.
     */
    fun localToParentCoordinates(localCoords: Vector2?): Vector2? {
        val rotation = -rotation
        val scaleX = scaleX
        val scaleY = scaleY
        val x = x
        val y = y
        if (rotation == 0f) {
            if (scaleX == 1f && scaleY == 1f) {
                localCoords.x += x
                localCoords.y += y
            } else {
                val originX = originX
                val originY = originY
                localCoords.x = (localCoords.x - originX) * scaleX + originX + x
                localCoords.y = (localCoords.y - originY) * scaleY + originY + y
            }
        } else {
            val cos = java.lang.Math.cos(rotation * MathUtils.degreesToRadians) as Float
            val sin = java.lang.Math.sin(rotation * MathUtils.degreesToRadians) as Float
            val originX = originX
            val originY = originY
            val tox: Float = (localCoords.x - originX) * scaleX
            val toy: Float = (localCoords.y - originY) * scaleY
            localCoords.x = tox * cos + toy * sin + originX + x
            localCoords.y = tox * -sin + toy * cos + originY + y
        }
        return localCoords
    }

    /**
     * Converts coordinates for this actor to those of a parent actor. The ascendant does not need to be a direct parent.
     */
    fun localToAscendantCoordinates(ascendant: Actor?, localCoords: Vector2?): Vector2? {
        var actor: Actor? = this
        do {
            actor!!.localToParentCoordinates(localCoords)
            actor = actor.parent
            if (actor === ascendant) break
        } while (actor != null)
        return localCoords
    }

    /**
     * Converts coordinates for this actor to those of another actor, which can be anywhere in the stage.
     */
    fun localToActorCoordinates(actor: Actor?, localCoords: Vector2?): Vector2? {
        localToStageCoordinates(localCoords)
        return actor!!.stageToLocalCoordinates(localCoords)
    }

    /**
     * Draws this actor's debug lines if [.getDebug] is true.
     */
    fun drawDebug(shapes: ShapeRenderer?) {
        drawDebugBounds(shapes)
    }

    /**
     * Draws a rectange for the bounds of this actor if [.getDebug] is true.
     */
    protected fun drawDebugBounds(shapes: ShapeRenderer?) {
        if (!debug) return
        shapes.set(ShapeType.Line)
        if (stage != null) shapes.setColor(stage.getDebugColor())
        shapes.rect(x, y, originX, originY, width, height, scaleX, scaleY, rotation)
    }

    /**
     * If true, [.drawDebug] will be called for this actor.
     */
    fun setDebug(enabled: Boolean) {
        debug = enabled
        if (enabled) Stage.debug = true
    }

    fun getDebug(): Boolean {
        return debug
    }

    /**
     * Calls [.setDebug] with `true`.
     */
    fun debug(): Actor? {
        setDebug(true)
        return this
    }

    override fun toString(): String {
        var name = name
        if (name == null) {
            name = javaClass.getName()
            val dotIndex = name.lastIndexOf('.')
            if (dotIndex != -1) name = name.substring(dotIndex + 1)
        }
        return name
    }
}
