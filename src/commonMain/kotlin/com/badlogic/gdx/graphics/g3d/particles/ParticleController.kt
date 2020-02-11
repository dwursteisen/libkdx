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
package com.badlogic.gdx.graphics.g3d.particles

import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.ChannelDescriptor
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.ChannelInitializer
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.FloatChannel
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.IntChannel
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.ObjectChannel
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.ColorInitializer
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.Rotation2dInitializer
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.Rotation3dInitializer
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.ScaleInitializer
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.TextureRegionInitializer
import com.badlogic.gdx.graphics.g3d.particles.ParticleControllerComponent
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader.ParticleEffectLoadParameter
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader.ParticleEffectSaveParameter
import kotlin.jvm.Throws

/**
 * Base class of all the particle controllers. Encapsulate the generic structure of a controller and methods to update the
 * particles simulation.
 *
 * @author Inferno
 */
class ParticleController() : Json.Serializable, ResourceData.Configurable<Any?> {

    /**
     * Name of the controller
     */
    var name: String? = null

    /**
     * Controls the emission of the particles
     */
    var emitter: Emitter? = null

    /**
     * Update the properties of the particles
     */
    var influencers: Array<Influencer?>?

    /**
     * Controls the graphical representation of the particles
     */
    var renderer: ParticleControllerRenderer<*, *>? = null

    /**
     * Particles components
     */
    var particles: ParallelArray? = null
    var particleChannels: ParticleChannels? = null

    /**
     * Current transform of the controller DO NOT CHANGE MANUALLY
     */
    var transform: Matrix4?

    /**
     * Transform flags
     */
    var scale: Vector3?

    /**
     * Not used by the simulation, it should represent the bounding box containing all the particles
     */
    protected var boundingBox: BoundingBox? = null

    /**
     * Time step, DO NOT CHANGE MANUALLY
     */
    var deltaTime = 0f
    var deltaTimeSqr = 0f

    constructor(name: String?, emitter: Emitter?, renderer: ParticleControllerRenderer<*, *>?, vararg influencers: Influencer?) : this() {
        this.name = name
        this.emitter = emitter
        this.renderer = renderer
        particleChannels = ParticleChannels()
        this.influencers = Array<Influencer?>(influencers)
    }

    /**
     * Sets the delta used to step the simulation
     */
    private fun setTimeStep(timeStep: Float) {
        deltaTime = timeStep
        deltaTimeSqr = deltaTime * deltaTime
    }

    /**
     * Sets the current transformation to the given one.
     *
     * @param transform the new transform matrix
     */
    fun setTransform(transform: Matrix4?) {
        this.transform.set(transform)
        transform.getScale(scale)
    }

    /**
     * Sets the current transformation.
     */
    fun setTransform(x: Float, y: Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float, scale: Float) {
        transform.set(x, y, z, qx, qy, qz, qw, scale, scale, scale)
        this.scale.set(scale, scale, scale)
    }

    /**
     * Post-multiplies the current transformation with a rotation matrix represented by the given quaternion.
     */
    fun rotate(rotation: Quaternion?) {
        transform.rotate(rotation)
    }

    /**
     * Post-multiplies the current transformation with a rotation matrix by the given angle around the given axis.
     *
     * @param axis  the rotation axis
     * @param angle the rotation angle in degrees
     */
    fun rotate(axis: Vector3?, angle: Float) {
        transform.rotate(axis, angle)
    }

    /**
     * Postmultiplies the current transformation with a translation matrix represented by the given translation.
     */
    fun translate(translation: Vector3?) {
        transform.translate(translation)
    }

    fun setTranslation(translation: Vector3?) {
        transform.setTranslation(translation)
    }

    /**
     * Postmultiplies the current transformation with a scale matrix represented by the given scale on x,y,z.
     */
    fun scale(scaleX: Float, scaleY: Float, scaleZ: Float) {
        transform.scale(scaleX, scaleY, scaleZ)
        transform.getScale(scale)
    }

    /**
     * Postmultiplies the current transformation with a scale matrix represented by the given scale vector.
     */
    fun scale(scale: Vector3?) {
        scale(scale.x, scale.y, scale.z)
    }

    /**
     * Postmultiplies the current transformation with the given matrix.
     */
    fun mul(transform: Matrix4?) {
        this.transform.mul(transform)
        this.transform.getScale(scale)
    }

    /**
     * Set the given matrix to the current transformation matrix.
     */
    fun getTransform(transform: Matrix4?) {
        transform.set(this.transform)
    }

    val isComplete: Boolean
        get() = emitter.isComplete()

    /**
     * Initialize the controller. All the sub systems will be initialized and binded to the controller. Must be called before any
     * other method.
     */
    fun init() {
        bind()
        if (particles != null) {
            end()
            particleChannels!!.resetIds()
        }
        allocateChannels(emitter.maxParticleCount)
        emitter.init()
        for (influencer in influencers!!) influencer.init()
        renderer.init()
    }

    protected fun allocateChannels(maxParticleCount: Int) {
        particles = ParallelArray(maxParticleCount)
        // Alloc additional channels
        emitter.allocateChannels()
        for (influencer in influencers!!) influencer.allocateChannels()
        renderer.allocateChannels()
    }

    /**
     * Bind the sub systems to the controller Called once during the init phase.
     */
    protected fun bind() {
        emitter.set(this)
        for (influencer in influencers!!) influencer.set(this)
        renderer.set(this)
    }

    /**
     * Start the simulation.
     */
    fun start() {
        emitter.start()
        for (influencer in influencers!!) influencer.start()
    }

    /**
     * Reset the simulation.
     */
    fun reset() {
        end()
        start()
    }

    /**
     * End the simulation.
     */
    fun end() {
        for (influencer in influencers!!) influencer.end()
        emitter.end()
    }

    /**
     * Generally called by the Emitter. This method will notify all the sub systems that a given amount of particles has been
     * activated.
     */
    fun activateParticles(startIndex: Int, count: Int) {
        emitter.activateParticles(startIndex, count)
        for (influencer in influencers!!) influencer.activateParticles(startIndex, count)
    }

    /**
     * Generally called by the Emitter. This method will notify all the sub systems that a given amount of particles has been
     * killed.
     */
    fun killParticles(startIndex: Int, count: Int) {
        emitter.killParticles(startIndex, count)
        for (influencer in influencers!!) influencer.killParticles(startIndex, count)
    }
    /**
     * Updates the particles data
     */
    /**
     * Updates the particles data
     */
    @JvmOverloads
    fun update(deltaTime: Float = Gdx.graphics.getDeltaTime()) {
        setTimeStep(deltaTime)
        emitter.update()
        for (influencer in influencers!!) influencer.update()
    }

    /**
     * Updates the renderer used by this controller, usually this means the particles will be draw inside a batch.
     */
    fun draw() {
        if (particles!!.size > 0) {
            renderer.update()
        }
    }

    /**
     * @return a copy of this controller
     */
    fun copy(): ParticleController? {
        val emitter: Emitter = emitter.copy() as Emitter
        val influencers: Array<Influencer?> = arrayOfNulls<Influencer?>(influencers!!.size)
        var i = 0
        for (influencer in this.influencers!!) {
            influencers[i++] = influencer.copy() as Influencer
        }
        return ParticleController(String(name), emitter, renderer.copy() as ParticleControllerRenderer<*, *>,
            *influencers)
    }

    fun dispose() {
        emitter.dispose()
        for (influencer in influencers!!) influencer.dispose()
    }

    /**
     * @return a copy of this controller, should be used after the particle effect has been loaded.
     */
    fun getBoundingBox(): BoundingBox? {
        if (boundingBox == null) boundingBox = BoundingBox()
        calculateBoundingBox()
        return boundingBox
    }

    /**
     * Updates the bounding box using the position channel.
     */
    protected fun calculateBoundingBox() {
        boundingBox.clr()
        val positionChannel: FloatChannel = particles!!.getChannel(ParticleChannels.Position)
        var pos = 0
        val c: Int = positionChannel!!.strideSize * particles!!.size
        while (pos < c) {
            boundingBox.ext(positionChannel!!.data!!.get(pos + ParticleChannels.XOffset), positionChannel!!.data!!.get(pos
                + ParticleChannels.YOffset), positionChannel!!.data!!.get(pos + ParticleChannels.ZOffset))
            pos += positionChannel!!.strideSize
        }
    }

    /**
     * @return the index of the Influencer of the given type.
     */
    private fun <K : Influencer?> findIndex(type: java.lang.Class<K?>?): Int {
        for (i in 0 until influencers!!.size) {
            val influencer: Influencer? = influencers!![i]
            if (ClassReflection.isAssignableFrom(type, influencer.getClass())) {
                return i
            }
        }
        return -1
    }

    /**
     * @return the influencer having the given type.
     */
    fun <K : Influencer?> findInfluencer(influencerClass: java.lang.Class<K?>?): K? {
        val index = findIndex<K?>(influencerClass)
        return if (index > -1) influencers!![index] else null
    }

    /**
     * Removes the Influencer of the given type.
     */
    fun <K : Influencer?> removeInfluencer(type: java.lang.Class<K?>?) {
        val index = findIndex<K?>(type)
        if (index > -1) influencers.removeIndex(index)
    }

    /**
     * Replaces the Influencer of the given type with the one passed as parameter.
     */
    fun <K : Influencer?> replaceInfluencer(type: java.lang.Class<K?>?, newInfluencer: K?): Boolean {
        val index = findIndex<K?>(type)
        if (index > -1) {
            influencers.insert(index, newInfluencer)
            influencers.removeIndex(index + 1)
            return true
        }
        return false
    }

    fun write(json: Json?) {
        json.writeValue("name", name)
        json.writeValue("emitter", emitter, Emitter::class.java)
        json.writeValue("influencers", influencers, Array::class.java, Influencer::class.java)
        json.writeValue("renderer", renderer, ParticleControllerRenderer::class.java)
    }

    fun read(json: Json?, jsonMap: JsonValue?) {
        name = json.readValue("name", String::class.java, jsonMap)
        emitter = json.readValue("emitter", Emitter::class.java, jsonMap)
        influencers.addAll(json.readValue("influencers", Array::class.java, Influencer::class.java, jsonMap))
        renderer = json.readValue("renderer", ParticleControllerRenderer::class.java, jsonMap)
    }

    fun save(manager: AssetManager?, data: ResourceData<*>?) {
        emitter.save(manager, data)
        for (influencer in influencers!!) influencer.save(manager, data)
        renderer.save(manager, data)
    }

    fun load(manager: AssetManager?, data: ResourceData<*>?) {
        emitter.load(manager, data)
        for (influencer in influencers!!) influencer.load(manager, data)
        renderer.load(manager, data)
    }

    companion object {
        /**
         * the default time step used to update the simulation
         */
        protected const val DEFAULT_TIME_STEP = 1f / 60
    }

    init {
        transform = Matrix4()
        scale = Vector3(1, 1, 1)
        influencers = Array<Influencer?>(true, 3, Influencer::class.java)
        setTimeStep(DEFAULT_TIME_STEP)
    }
}
