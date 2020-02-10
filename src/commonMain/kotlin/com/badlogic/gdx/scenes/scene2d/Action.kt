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
import com.badlogic.gdx.scenes.scene2d.Stage.TouchFocus
import java.lang.RuntimeException

/**
 * Actions attach to an [Actor] and perform some task, often over time.
 *
 * @author Nathan Sweet
 */
abstract class Action : Poolable {

    /**
     * The actor this action is attached to, or null if it is not attached.
     */
    protected var actor: Actor? = null

    /**
     * The actor this action targets, or null if a target has not been set.
     */
    protected var target: Actor? = null
    private var pool: Pool? = null

    /**
     * Updates the action based on time. Typically this is called each frame by [Actor.act].
     *
     * @param delta Time in seconds since the last frame.
     * @return true if the action is done. This method may continue to be called after the action is done.
     */
    abstract fun act(delta: Float): Boolean

    /**
     * Sets the state of the action so it can be run again.
     */
    fun restart() {}

    /**
     * Sets the actor this action is attached to. This also sets the [target][.setTarget] actor if it is null. This
     * method is called automatically when an action is added to an actor. This method is also called with null when an action is
     * removed from an actor.
     *
     *
     * When set to null, if the action has a [pool][.setPool] then the action is [returned][Pool.free] to
     * the pool (which calls [.reset]) and the pool is set to null. If the action does not have a pool, [.reset] is
     * not called.
     *
     *
     * This method is not typically a good place for an action subclass to query the actor's state because the action may not be
     * executed for some time, eg it may be [delayed][DelayAction]. The actor's state is best queried in the first call to
     * [.act]. For a [TemporalAction], use TemporalAction#begin().
     */
    fun setActor(actor: Actor?) {
        this.actor = actor
        if (target == null) setTarget(actor)
        if (actor == null) {
            if (pool != null) {
                pool.free(this)
                pool = null
            }
        }
    }

    /**
     * @return null if the action is not attached to an actor.
     */
    fun getActor(): Actor? {
        return actor
    }

    /**
     * Sets the actor this action will manipulate. If no target actor is set, [.setActor] will set the target actor
     * when the action is added to an actor.
     */
    fun setTarget(target: Actor?) {
        this.target = target
    }

    /**
     * @return null if the action has no target.
     */
    fun getTarget(): Actor? {
        return target
    }

    /**
     * Resets the optional state of this action to as if it were newly created, allowing the action to be pooled and reused. State
     * required to be set for every usage of this action or computed during the action does not need to be reset.
     *
     *
     * The default implementation calls [.restart].
     *
     *
     * If a subclass has optional state, it must override this method, call super, and reset the optional state.
     */
    fun reset() {
        actor = null
        target = null
        pool = null
        restart()
    }

    fun getPool(): Pool? {
        return pool
    }

    /**
     * Sets the pool that the action will be returned to when removed from the actor.
     *
     * @param pool May be null.
     * @see .setActor
     */
    fun setPool(pool: Pool?) {
        this.pool = pool
    }

    override fun toString(): String {
        var name: String? = javaClass.getName()
        val dotIndex = name!!.lastIndexOf('.')
        if (dotIndex != -1) name = name.substring(dotIndex + 1)
        if (name.endsWith("Action")) name = name.substring(0, name.length - 6)
        return name
    }
}
