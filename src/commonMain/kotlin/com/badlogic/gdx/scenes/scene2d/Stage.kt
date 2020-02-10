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

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputListener
import java.lang.RuntimeException

/**
 * A 2D scene graph containing hierarchies of [actors][Actor]. Stage handles the viewport and distributes input events.
 *
 *
 * [.setViewport] controls the coordinates used within the stage and sets up the camera used to convert between
 * stage coordinates and screen coordinates.
 *
 *
 * A stage must receive input events so it can distribute them to actors. This is typically done by passing the stage to
 * [Gdx.input.setInputProcessor][Input.setInputProcessor]. An [InputMultiplexer] may
 * be used to handle input events before or after the stage does. If an actor handles an event by returning true from the input
 * method, then the stage's input method will also return true, causing subsequent InputProcessors to not receive the event.
 *
 *
 * The Stage and its constituents (like Actors and Listeners) are not thread-safe and should only be updated and queried from a
 * single thread (presumably the main render thread). Methods should be reentrant, so you can update Actors and Stages from within
 * callbacks and handlers.
 *
 * @author mzechner
 * @author Nathan Sweet
 */
class Stage(viewport: Viewport?, batch: Batch?) : InputAdapter(), Disposable {

    private var viewport: Viewport?
    private val batch: Batch?
    private var ownsBatch = false
    private var root: Group?
    private val tempCoords: Vector2? = Vector2()
    private val pointerOverActors: Array<Actor?>? = arrayOfNulls<Actor?>(20)
    private val pointerTouched: BooleanArray? = BooleanArray(20)
    private val pointerScreenX: IntArray? = IntArray(20)
    private val pointerScreenY: IntArray? = IntArray(20)
    private var mouseScreenX = 0
    private var mouseScreenY = 0
    private var mouseOverActor: Actor? = null
    private var keyboardFocus: Actor? = null
    private var scrollFocus: Actor? = null
    val touchFocuses: SnapshotArray<TouchFocus?>? = SnapshotArray(true, 4, TouchFocus::class.java)

    /**
     * If true, any actions executed during a call to [.act]) will result in a call to [Graphics.requestRendering]
     * . Widgets that animate or otherwise require additional rendering may check this setting before calling
     * [Graphics.requestRendering]. Default is true.
     */
    var actionsRequestRendering = true
    private var debugShapes: ShapeRenderer? = null
    private var debugInvisible = false
    private var debugAll = false
    private var debugUnderMouse = false
    private var debugParentUnderMouse = false
    private var debugTableUnderMouse: Debug? = Debug.none

    /**
     * The default color that can be used by actors to draw debug lines.
     */
    val debugColor: Color? = Color(0, 1, 0, 0.85f)

    /**
     * Creates a stage with a [ScalingViewport] set to [Scaling.stretch]. The stage will use its own [Batch]
     * which will be disposed when the stage is disposed.
     */
    constructor() : this(ScalingViewport(Scaling.stretch, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), OrthographicCamera()),
        SpriteBatch()) {
        ownsBatch = true
    }

    /**
     * Creates a stage with the specified viewport. The stage will use its own [Batch] which will be disposed when the stage
     * is disposed.
     */
    constructor(viewport: Viewport?) : this(viewport, SpriteBatch()) {
        ownsBatch = true
    }

    fun draw() {
        val camera: Camera = viewport.getCamera()
        camera.update()
        if (!root.isVisible()) return
        val batch: Batch? = batch
        batch.setProjectionMatrix(camera.combined)
        batch.begin()
        root!!.draw(batch, 1)
        batch.end()
        if (debug) drawDebug()
    }

    private fun drawDebug() {
        if (debugShapes == null) {
            debugShapes = ShapeRenderer()
            debugShapes.setAutoShapeType(true)
        }
        if (debugUnderMouse || debugParentUnderMouse || debugTableUnderMouse !== Debug.none) {
            screenToStageCoordinates(tempCoords.set(Gdx.input.getX(), Gdx.input.getY()))
            var actor: Actor = hit(tempCoords.x, tempCoords.y, true) ?: return
            if (debugParentUnderMouse && actor.parent != null) actor = actor.parent
            if (debugTableUnderMouse === Debug.none) actor!!.setDebug(true) else {
                while (actor != null) {
                    if (actor is Table) break
                    actor = actor.parent
                }
                if (actor == null) return
                (actor as Table).debug(debugTableUnderMouse)
            }
            if (debugAll && actor is Group) (actor as Group)!!.debugAll()
            disableDebug(root, actor)
        } else {
            if (debugAll) root!!.debugAll()
        }
        Gdx.gl.glEnable(GL20.GL_BLEND)
        debugShapes.setProjectionMatrix(viewport.getCamera().combined)
        debugShapes.begin()
        root!!.drawDebug(debugShapes)
        debugShapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    /**
     * Disables debug on all actors recursively except the specified actor and any children.
     */
    private fun disableDebug(actor: Actor?, except: Actor?) {
        if (actor === except) return
        actor!!.setDebug(false)
        if (actor is Group) {
            val children: SnapshotArray<Actor?> = (actor as Group?)!!.children
            var i = 0
            val n: Int = children.size
            while (i < n) {
                disableDebug(children.get(i), except)
                i++
            }
        }
    }
    /**
     * Calls the [Actor.act] method on each actor in the stage. Typically called each frame. This method also fires
     * enter and exit events.
     *
     * @param delta Time in seconds since the last frame.
     */
    /**
     * Calls [.act] with [Graphics.getDeltaTime], limited to a minimum of 30fps.
     */
    @JvmOverloads
    fun act(delta: Float = java.lang.Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f)) {
        // Update over actors. Done in act() because actors may change position, which can fire enter/exit without an input event.
        var pointer = 0
        val n = pointerOverActors!!.size
        while (pointer < n) {
            val overLast: Actor? = pointerOverActors[pointer]
            // Check if pointer is gone.
            if (!pointerTouched!![pointer]) {
                if (overLast != null) {
                    pointerOverActors[pointer] = null
                    screenToStageCoordinates(tempCoords.set(pointerScreenX!![pointer], pointerScreenY!![pointer]))
                    // Exit over last.
                    val event: InputEvent = Pools.obtain(InputEvent::class.java)
                    event.setType(InputEvent.Type.exit)
                    event.setStage(this)
                    event.setStageX(tempCoords.x)
                    event.setStageY(tempCoords.y)
                    event!!.setRelatedActor(overLast)
                    event.setPointer(pointer)
                    overLast.fire(event)
                    Pools.free(event)
                }
                pointer++
                continue
            }
            // Update over actor for the pointer.
            pointerOverActors[pointer] = fireEnterAndExit(overLast, pointerScreenX!![pointer], pointerScreenY!![pointer], pointer)
            pointer++
        }
        // Update over actor for the mouse on the desktop.
        val type: ApplicationType = Gdx.app.getType()
        if (type === ApplicationType.Desktop || type === ApplicationType.Applet || type === ApplicationType.WebGL) mouseOverActor = fireEnterAndExit(mouseOverActor, mouseScreenX, mouseScreenY, -1)
        root!!.act(delta)
    }

    private fun fireEnterAndExit(overLast: Actor?, screenX: Int, screenY: Int, pointer: Int): Actor? {
        // Find the actor under the point.
        screenToStageCoordinates(tempCoords.set(screenX, screenY))
        val over: Actor? = hit(tempCoords.x, tempCoords.y, true)
        if (over === overLast) return overLast

        // Exit overLast.
        if (overLast != null) {
            val event: InputEvent = Pools.obtain(InputEvent::class.java)
            event.setStage(this)
            event.setStageX(tempCoords.x)
            event.setStageY(tempCoords.y)
            event.setPointer(pointer)
            event.setType(InputEvent.Type.exit)
            event!!.setRelatedActor(over)
            overLast.fire(event)
            Pools.free(event)
        }
        // Enter over.
        if (over != null) {
            val event: InputEvent = Pools.obtain(InputEvent::class.java)
            event.setStage(this)
            event.setStageX(tempCoords.x)
            event.setStageY(tempCoords.y)
            event.setPointer(pointer)
            event.setType(InputEvent.Type.enter)
            event!!.setRelatedActor(overLast)
            over.fire(event)
            Pools.free(event)
        }
        return over
    }

    /**
     * Applies a touch down event to the stage and returns true if an actor in the scene [handled][Event.handle] the
     * event.
     */
    fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (!isInsideViewport(screenX, screenY)) return false
        pointerTouched!![pointer] = true
        pointerScreenX!![pointer] = screenX
        pointerScreenY!![pointer] = screenY
        screenToStageCoordinates(tempCoords.set(screenX, screenY))
        val event: InputEvent = Pools.obtain(InputEvent::class.java)
        event.setType(Type.touchDown)
        event.setStage(this)
        event.setStageX(tempCoords.x)
        event.setStageY(tempCoords.y)
        event.setPointer(pointer)
        event.setButton(button)
        val target: Actor? = hit(tempCoords.x, tempCoords.y, true)
        if (target == null) {
            if (root.getTouchable() === Touchable.enabled) root.fire(event)
        } else target.fire(event)
        val handled: Boolean = event.isHandled()
        Pools.free(event)
        return handled
    }

    /**
     * Applies a touch moved event to the stage and returns true if an actor in the scene [handled][Event.handle] the
     * event. Only [listeners][InputListener] that returned true for touchDown will receive this event.
     */
    fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        pointerScreenX!![pointer] = screenX
        pointerScreenY!![pointer] = screenY
        mouseScreenX = screenX
        mouseScreenY = screenY
        if (touchFocuses.size === 0) return false
        screenToStageCoordinates(tempCoords.set(screenX, screenY))
        val event: InputEvent = Pools.obtain(InputEvent::class.java)
        event.setType(Type.touchDragged)
        event.setStage(this)
        event.setStageX(tempCoords.x)
        event.setStageY(tempCoords.y)
        event.setPointer(pointer)
        val touchFocuses: SnapshotArray<TouchFocus?>? = touchFocuses
        val focuses: Array<TouchFocus?> = touchFocuses.begin()
        var i = 0
        val n: Int = touchFocuses.size
        while (i < n) {
            val focus = focuses[i]
            if (focus!!.pointer != pointer) {
                i++
                continue
            }
            if (!touchFocuses.contains(focus, true)) {
                i++
                continue  // Touch focus already gone.
            }
            event.setTarget(focus.target)
            event.setListenerActor(focus.listenerActor)
            if (focus.listener!!.handle(event)) event.handle()
            i++
        }
        touchFocuses.end()
        val handled: Boolean = event.isHandled()
        Pools.free(event)
        return handled
    }

    /**
     * Applies a touch up event to the stage and returns true if an actor in the scene [handled][Event.handle] the event.
     * Only [listeners][InputListener] that returned true for touchDown will receive this event.
     */
    fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        pointerTouched!![pointer] = false
        pointerScreenX!![pointer] = screenX
        pointerScreenY!![pointer] = screenY
        if (touchFocuses.size === 0) return false
        screenToStageCoordinates(tempCoords.set(screenX, screenY))
        val event: InputEvent = Pools.obtain(InputEvent::class.java)
        event.setType(Type.touchUp)
        event.setStage(this)
        event.setStageX(tempCoords.x)
        event.setStageY(tempCoords.y)
        event.setPointer(pointer)
        event.setButton(button)
        val touchFocuses: SnapshotArray<TouchFocus?>? = touchFocuses
        val focuses: Array<TouchFocus?> = touchFocuses.begin()
        var i = 0
        val n: Int = touchFocuses.size
        while (i < n) {
            val focus = focuses[i]
            if (focus!!.pointer != pointer || focus.button != button) {
                i++
                continue
            }
            if (!touchFocuses.removeValue(focus, true)) {
                i++
                continue  // Touch focus already gone.
            }
            event.setTarget(focus.target)
            event.setListenerActor(focus.listenerActor)
            if (focus.listener!!.handle(event)) event.handle()
            Pools.free(focus)
            i++
        }
        touchFocuses.end()
        val handled: Boolean = event.isHandled()
        Pools.free(event)
        return handled
    }

    /**
     * Applies a mouse moved event to the stage and returns true if an actor in the scene [handled][Event.handle] the
     * event. This event only occurs on the desktop.
     */
    fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        mouseScreenX = screenX
        mouseScreenY = screenY
        if (!isInsideViewport(screenX, screenY)) return false
        screenToStageCoordinates(tempCoords.set(screenX, screenY))
        val event: InputEvent = Pools.obtain(InputEvent::class.java)
        event.setStage(this)
        event.setType(Type.mouseMoved)
        event.setStageX(tempCoords.x)
        event.setStageY(tempCoords.y)
        var target: Actor? = hit(tempCoords.x, tempCoords.y, true)
        if (target == null) target = root
        target!!.fire(event)
        val handled: Boolean = event.isHandled()
        Pools.free(event)
        return handled
    }

    /**
     * Applies a mouse scroll event to the stage and returns true if an actor in the scene [handled][Event.handle] the
     * event. This event only occurs on the desktop.
     */
    fun scrolled(amount: Int): Boolean {
        val target: Actor = if (scrollFocus == null) root else scrollFocus
        screenToStageCoordinates(tempCoords.set(mouseScreenX, mouseScreenY))
        val event: InputEvent = Pools.obtain(InputEvent::class.java)
        event.setStage(this)
        event.setType(InputEvent.Type.scrolled)
        event.setScrollAmount(amount)
        event.setStageX(tempCoords.x)
        event.setStageY(tempCoords.y)
        target!!.fire(event)
        val handled: Boolean = event.isHandled()
        Pools.free(event)
        return handled
    }

    /**
     * Applies a key down event to the actor that has [keyboard focus][Stage.setKeyboardFocus], if any, and returns
     * true if the event was [handled][Event.handle].
     */
    fun keyDown(keyCode: Int): Boolean {
        val target: Actor = if (keyboardFocus == null) root else keyboardFocus
        val event: InputEvent = Pools.obtain(InputEvent::class.java)
        event.setStage(this)
        event.setType(InputEvent.Type.keyDown)
        event.setKeyCode(keyCode)
        target!!.fire(event)
        val handled: Boolean = event.isHandled()
        Pools.free(event)
        return handled
    }

    /**
     * Applies a key up event to the actor that has [keyboard focus][Stage.setKeyboardFocus], if any, and returns true
     * if the event was [handled][Event.handle].
     */
    fun keyUp(keyCode: Int): Boolean {
        val target: Actor = if (keyboardFocus == null) root else keyboardFocus
        val event: InputEvent = Pools.obtain(InputEvent::class.java)
        event.setStage(this)
        event.setType(InputEvent.Type.keyUp)
        event.setKeyCode(keyCode)
        target!!.fire(event)
        val handled: Boolean = event.isHandled()
        Pools.free(event)
        return handled
    }

    /**
     * Applies a key typed event to the actor that has [keyboard focus][Stage.setKeyboardFocus], if any, and returns
     * true if the event was [handled][Event.handle].
     */
    fun keyTyped(character: Char): Boolean {
        val target: Actor = if (keyboardFocus == null) root else keyboardFocus
        val event: InputEvent = Pools.obtain(InputEvent::class.java)
        event.setStage(this)
        event.setType(InputEvent.Type.keyTyped)
        event.setCharacter(character)
        target!!.fire(event)
        val handled: Boolean = event.isHandled()
        Pools.free(event)
        return handled
    }

    /**
     * Adds the listener to be notified for all touchDragged and touchUp events for the specified pointer and button. Touch focus
     * is added automatically when true is returned from [ touchDown][InputListener.touchDown]. The specified actors will be used as the [listener actor][Event.getListenerActor] and
     * [target][Event.getTarget] for the touchDragged and touchUp events.
     */
    fun addTouchFocus(listener: EventListener?, listenerActor: Actor?, target: Actor?, pointer: Int, button: Int) {
        val focus: TouchFocus = Pools.obtain(TouchFocus::class.java)
        focus.listenerActor = listenerActor
        focus.target = target
        focus.listener = listener
        focus.pointer = pointer
        focus.button = button
        touchFocuses.add(focus)
    }

    /**
     * Removes touch focus for the specified listener, pointer, and button. Note the listener will not receive a touchUp event
     * when this method is used.
     */
    fun removeTouchFocus(listener: EventListener?, listenerActor: Actor?, target: Actor?, pointer: Int, button: Int) {
        val touchFocuses: SnapshotArray<TouchFocus?>? = touchFocuses
        for (i in touchFocuses.size - 1 downTo 0) {
            val focus: TouchFocus = touchFocuses.get(i)
            if (focus.listener === listener && focus.listenerActor === listenerActor && focus.target === target && focus.pointer == pointer && focus.button == button) {
                touchFocuses.removeIndex(i)
                Pools.free(focus)
            }
        }
    }

    /**
     * Cancels touch focus for all listeners with the specified listener actor.
     *
     * @see .cancelTouchFocus
     */
    fun cancelTouchFocus(listenerActor: Actor?) {
        val event: InputEvent = Pools.obtain(InputEvent::class.java)
        event.setStage(this)
        event.setType(InputEvent.Type.touchUp)
        event.setStageX(Int.MIN_VALUE)
        event.setStageY(Int.MIN_VALUE)

        // Cancel all current touch focuses for the specified listener, allowing for concurrent modification, and never cancel the
        // same focus twice.
        val touchFocuses: SnapshotArray<TouchFocus?>? = touchFocuses
        val items: Array<TouchFocus?> = touchFocuses.begin()
        var i = 0
        val n: Int = touchFocuses.size
        while (i < n) {
            val focus = items[i]
            if (focus!!.listenerActor !== listenerActor) {
                i++
                continue
            }
            if (!touchFocuses.removeValue(focus, true)) {
                i++
                continue  // Touch focus already gone.
            }
            event.setTarget(focus!!.target)
            event.setListenerActor(focus!!.listenerActor)
            event.setPointer(focus!!.pointer)
            event.setButton(focus!!.button)
            focus!!.listener!!.handle(event)
            i++
        }
        touchFocuses.end()
        Pools.free(event)
    }

    /**
     * Removes all touch focus listeners, sending a touchUp event to each listener. Listeners typically expect to receive a
     * touchUp event when they have touch focus. The location of the touchUp is [Integer.MIN_VALUE]. Listeners can use
     * [InputEvent.isTouchFocusCancel] to ignore this event if needed.
     */
    fun cancelTouchFocus() {
        cancelTouchFocusExcept(null, null)
    }

    /**
     * Cancels touch focus for all listeners except the specified listener.
     *
     * @see .cancelTouchFocus
     */
    fun cancelTouchFocusExcept(exceptListener: EventListener?, exceptActor: Actor?) {
        val event: InputEvent = Pools.obtain(InputEvent::class.java)
        event.setStage(this)
        event.setType(InputEvent.Type.touchUp)
        event.setStageX(Int.MIN_VALUE)
        event.setStageY(Int.MIN_VALUE)

        // Cancel all current touch focuses except for the specified listener, allowing for concurrent modification, and never
        // cancel the same focus twice.
        val touchFocuses: SnapshotArray<TouchFocus?>? = touchFocuses
        val items: Array<TouchFocus?> = touchFocuses.begin()
        var i = 0
        val n: Int = touchFocuses.size
        while (i < n) {
            val focus = items[i]
            if (focus!!.listener === exceptListener && focus!!.listenerActor === exceptActor) {
                i++
                continue
            }
            if (!touchFocuses.removeValue(focus, true)) {
                i++
                continue  // Touch focus already gone.
            }
            event.setTarget(focus!!.target)
            event.setListenerActor(focus!!.listenerActor)
            event.setPointer(focus!!.pointer)
            event.setButton(focus!!.button)
            focus!!.listener!!.handle(event)
            i++
        }
        touchFocuses.end()
        Pools.free(event)
    }

    /**
     * Adds an actor to the root of the stage.
     *
     * @see Group.addActor
     */
    fun addActor(actor: Actor?) {
        root!!.addActor(actor)
    }

    /**
     * Adds an action to the root of the stage.
     *
     * @see Group.addAction
     */
    fun addAction(action: Action?) {
        root.addAction(action)
    }

    /**
     * Returns the root's child actors.
     *
     * @see Group.getChildren
     */
    val actors: Array<com.badlogic.gdx.scenes.scene2d.Actor?>?
        get() = root!!.children

    /**
     * Adds a listener to the root.
     *
     * @see Actor.addListener
     */
    fun addListener(listener: EventListener?): Boolean {
        return root.addListener(listener)
    }

    /**
     * Removes a listener from the root.
     *
     * @see Actor.removeListener
     */
    fun removeListener(listener: EventListener?): Boolean {
        return root.removeListener(listener)
    }

    /**
     * Adds a capture listener to the root.
     *
     * @see Actor.addCaptureListener
     */
    fun addCaptureListener(listener: EventListener?): Boolean {
        return root.addCaptureListener(listener)
    }

    /**
     * Removes a listener from the root.
     *
     * @see Actor.removeCaptureListener
     */
    fun removeCaptureListener(listener: EventListener?): Boolean {
        return root.removeCaptureListener(listener)
    }

    /**
     * Removes the root's children, actions, and listeners.
     */
    fun clear() {
        unfocusAll()
        root!!.clear()
    }

    /**
     * Removes the touch, keyboard, and scroll focused actors.
     */
    fun unfocusAll() {
        setScrollFocus(null)
        setKeyboardFocus(null)
        cancelTouchFocus()
    }

    /**
     * Removes the touch, keyboard, and scroll focus for the specified actor and any descendants.
     */
    fun unfocus(actor: Actor?) {
        cancelTouchFocus(actor)
        if (scrollFocus != null && scrollFocus.isDescendantOf(actor)) setScrollFocus(null)
        if (keyboardFocus != null && keyboardFocus.isDescendantOf(actor)) setKeyboardFocus(null)
    }

    /**
     * Sets the actor that will receive key events.
     *
     * @param actor May be null.
     * @return true if the unfocus and focus events were not cancelled by a [FocusListener].
     */
    fun setKeyboardFocus(actor: Actor?): Boolean {
        if (keyboardFocus === actor) return true
        val event: FocusEvent = Pools.obtain(FocusEvent::class.java)
        event.setStage(this)
        event.setType(FocusEvent.Type.keyboard)
        val oldKeyboardFocus: Actor? = keyboardFocus
        if (oldKeyboardFocus != null) {
            event.setFocused(false)
            event.setRelatedActor(actor)
            oldKeyboardFocus.fire(event)
        }
        var success: Boolean = !event.isCancelled()
        if (success) {
            keyboardFocus = actor
            if (actor != null) {
                event.setFocused(true)
                event.setRelatedActor(oldKeyboardFocus)
                actor.fire(event)
                success = !event.isCancelled()
                if (!success) keyboardFocus = oldKeyboardFocus
            }
        }
        Pools.free(event)
        return success
    }

    /**
     * Gets the actor that will receive key events.
     *
     * @return May be null.
     */
    fun getKeyboardFocus(): Actor? {
        return keyboardFocus
    }

    /**
     * Sets the actor that will receive scroll events.
     *
     * @param actor May be null.
     * @return true if the unfocus and focus events were not cancelled by a [FocusListener].
     */
    fun setScrollFocus(actor: Actor?): Boolean {
        if (scrollFocus === actor) return true
        val event: FocusEvent = Pools.obtain(FocusEvent::class.java)
        event.setStage(this)
        event.setType(FocusEvent.Type.scroll)
        val oldScrollFocus: Actor? = scrollFocus
        if (oldScrollFocus != null) {
            event.setFocused(false)
            event.setRelatedActor(actor)
            oldScrollFocus.fire(event)
        }
        var success: Boolean = !event.isCancelled()
        if (success) {
            scrollFocus = actor
            if (actor != null) {
                event.setFocused(true)
                event.setRelatedActor(oldScrollFocus)
                actor.fire(event)
                success = !event.isCancelled()
                if (!success) scrollFocus = oldScrollFocus
            }
        }
        Pools.free(event)
        return success
    }

    /**
     * Gets the actor that will receive scroll events.
     *
     * @return May be null.
     */
    fun getScrollFocus(): Actor? {
        return scrollFocus
    }

    fun getBatch(): Batch? {
        return batch
    }

    fun getViewport(): Viewport? {
        return viewport
    }

    fun setViewport(viewport: Viewport?) {
        this.viewport = viewport
    }

    /**
     * The viewport's world width.
     */
    val width: Float
        get() = viewport.worldWidth

    /**
     * The viewport's world height.
     */
    val height: Float
        get() = viewport.worldHeight

    /**
     * The viewport's camera.
     */
    val camera: Camera?
        get() = viewport.getCamera()

    /**
     * Returns the root group which holds all actors in the stage.
     */
    fun getRoot(): Group? {
        return root
    }

    /**
     * Replaces the root group. This can be useful, for example, to subclass the root group to be notified by
     * [Group.childrenChanged].
     */
    fun setRoot(root: Group?) {
        if (root.parent != null) root.parent.removeActor(root, false)
        this.root = root
        root.setParent(null)
        root!!.setStage(this)
    }

    /**
     * Returns the [Actor] at the specified location in stage coordinates. Hit testing is performed in the order the actors
     * were inserted into the stage, last inserted actors being tested first. To get stage coordinates from screen coordinates, use
     * [.screenToStageCoordinates].
     *
     * @param touchable If true, the hit detection will respect the [touchability][Actor.setTouchable].
     * @return May be null if no actor was hit.
     */
    fun hit(stageX: Float, stageY: Float, touchable: Boolean): Actor? {
        root.parentToLocalCoordinates(tempCoords.set(stageX, stageY))
        return root!!.hit(tempCoords.x, tempCoords.y, touchable)
    }

    /**
     * Transforms the screen coordinates to stage coordinates.
     *
     * @param screenCoords Input screen coordinates and output for resulting stage coordinates.
     */
    fun screenToStageCoordinates(screenCoords: Vector2?): Vector2? {
        viewport.unproject(screenCoords)
        return screenCoords
    }

    /**
     * Transforms the stage coordinates to screen coordinates.
     *
     * @param stageCoords Input stage coordinates and output for resulting screen coordinates.
     */
    fun stageToScreenCoordinates(stageCoords: Vector2?): Vector2? {
        viewport.project(stageCoords)
        stageCoords.y = viewport.screenHeight - stageCoords.y
        return stageCoords
    }

    /**
     * Transforms the coordinates to screen coordinates. The coordinates can be anywhere in the stage since the transform matrix
     * describes how to convert them. The transform matrix is typically obtained from [Batch.getTransformMatrix] during
     * [Actor.draw].
     *
     * @see Actor.localToStageCoordinates
     */
    fun toScreenCoordinates(coords: Vector2?, transformMatrix: Matrix4?): Vector2? {
        return viewport.toScreenCoordinates(coords, transformMatrix)
    }

    /**
     * Calculates window scissor coordinates from local coordinates using the batch's current transformation matrix.
     *
     * @see ScissorStack.calculateScissors
     */
    fun calculateScissors(localRect: Rectangle?, scissorRect: Rectangle?) {
        val transformMatrix: Matrix4
        transformMatrix = if (debugShapes != null && debugShapes.isDrawing()) debugShapes.getTransformMatrix() else batch.getTransformMatrix()
        viewport.calculateScissors(transformMatrix, localRect, scissorRect)
    }

    /**
     * If true, debug lines are shown for actors even when [Actor.isVisible] is false.
     */
    fun setDebugInvisible(debugInvisible: Boolean) {
        this.debugInvisible = debugInvisible
    }

    /**
     * If true, debug lines are shown for all actors.
     */
    fun setDebugAll(debugAll: Boolean) {
        if (this.debugAll == debugAll) return
        this.debugAll = debugAll
        if (debugAll) debug = true else root!!.setDebug(false, true)
    }

    fun isDebugAll(): Boolean {
        return debugAll
    }

    /**
     * If true, debug is enabled only for the actor under the mouse. Can be combined with [.setDebugAll].
     */
    fun setDebugUnderMouse(debugUnderMouse: Boolean) {
        if (this.debugUnderMouse == debugUnderMouse) return
        this.debugUnderMouse = debugUnderMouse
        if (debugUnderMouse) debug = true else root!!.setDebug(false, true)
    }

    /**
     * If true, debug is enabled only for the parent of the actor under the mouse. Can be combined with
     * [.setDebugAll].
     */
    fun setDebugParentUnderMouse(debugParentUnderMouse: Boolean) {
        if (this.debugParentUnderMouse == debugParentUnderMouse) return
        this.debugParentUnderMouse = debugParentUnderMouse
        if (debugParentUnderMouse) debug = true else root!!.setDebug(false, true)
    }

    /**
     * If not [Debug.none], debug is enabled only for the first ascendant of the actor under the mouse that is a table. Can
     * be combined with [.setDebugAll].
     *
     * @param debugTableUnderMouse May be null for [Debug.none].
     */
    fun setDebugTableUnderMouse(debugTableUnderMouse: Debug?) {
        var debugTableUnderMouse: Debug? = debugTableUnderMouse
        if (debugTableUnderMouse == null) debugTableUnderMouse = Debug.none
        if (this.debugTableUnderMouse === debugTableUnderMouse) return
        this.debugTableUnderMouse = debugTableUnderMouse
        if (debugTableUnderMouse !== Debug.none) debug = true else root!!.setDebug(false, true)
    }

    /**
     * If true, debug is enabled only for the first ascendant of the actor under the mouse that is a table. Can be combined with
     * [.setDebugAll].
     */
    fun setDebugTableUnderMouse(debugTableUnderMouse: Boolean) {
        setDebugTableUnderMouse(if (debugTableUnderMouse) Debug.all else Debug.none)
    }

    fun dispose() {
        clear()
        if (ownsBatch) batch.dispose()
    }

    /**
     * Check if screen coordinates are inside the viewport's screen area.
     */
    protected fun isInsideViewport(screenX: Int, screenY: Int): Boolean {
        var screenY = screenY
        val x0: Int = viewport.getScreenX()
        val x1: Int = x0 + viewport.screenWidth
        val y0: Int = viewport.getScreenY()
        val y1: Int = y0 + viewport.screenHeight
        screenY = Gdx.graphics.getHeight() - 1 - screenY
        return screenX >= x0 && screenX < x1 && screenY >= y0 && screenY < y1
    }

    /**
     * Internal class for managing touch focus. Public only for GWT.
     *
     * @author Nathan Sweet
     */
    class TouchFocus : Poolable {

        var listener: EventListener? = null
        var listenerActor: Actor? = null
        var target: Actor? = null
        var pointer = 0
        var button = 0
        fun reset() {
            listenerActor = null
            listener = null
            target = null
        }
    }

    companion object {
        /**
         * True if any actor has ever had debug enabled.
         */
        var debug = false
    }

    /**
     * Creates a stage with the specified viewport and batch. This can be used to avoid creating a new batch (which can be
     * somewhat slow) if multiple stages are used during an application's life time.
     *
     * @param batch Will not be disposed if [.dispose] is called, handle disposal yourself.
     */
    init {
        if (viewport == null) throw IllegalArgumentException("viewport cannot be null.")
        if (batch == null) throw IllegalArgumentException("batch cannot be null.")
        this.viewport = viewport
        this.batch = batch
        root = Group()
        root.setStage(this)
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true)
    }
}
