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
package com.badlogic.gdx.graphics.g2d

import PixmapPacker.PixmapPackerRectangle
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.badlogic.gdx.graphics.g2d.PixmapPacker.GuillotineStrategy
import com.badlogic.gdx.graphics.g2d.PixmapPacker.GuillotineStrategy.GuillotinePage
import com.badlogic.gdx.graphics.g2d.PixmapPacker.PackStrategy
import com.badlogic.gdx.graphics.g2d.PixmapPacker.PixmapPackerRectangle
import com.badlogic.gdx.graphics.g2d.PixmapPacker.SkylineStrategy.SkylinePage
import com.badlogic.gdx.graphics.g2d.PixmapPackerIO.ImageFormat
import com.badlogic.gdx.graphics.g2d.PixmapPackerIO.SaveParameters
import java.lang.RuntimeException
import kotlin.jvm.Throws

class ParticleEmitter {
    private val delayValue: RangedNumericValue? = RangedNumericValue()
    private val lifeOffsetValue: IndependentScaledNumericValue? = IndependentScaledNumericValue()
    private val durationValue: RangedNumericValue? = RangedNumericValue()
    private val lifeValue: IndependentScaledNumericValue? = IndependentScaledNumericValue()
    private val emissionValue: ScaledNumericValue? = ScaledNumericValue()
    val xScale: ScaledNumericValue? = ScaledNumericValue()
    val yScale: ScaledNumericValue? = ScaledNumericValue()
    val rotation: ScaledNumericValue? = ScaledNumericValue()
    val velocity: ScaledNumericValue? = ScaledNumericValue()
    val angle: ScaledNumericValue? = ScaledNumericValue()
    val wind: ScaledNumericValue? = ScaledNumericValue()
    val gravity: ScaledNumericValue? = ScaledNumericValue()
    val transparency: ScaledNumericValue? = ScaledNumericValue()
    val tint: GradientColorValue? = GradientColorValue()
    val xOffsetValue: RangedNumericValue? = ScaledNumericValue()
    val yOffsetValue: RangedNumericValue? = ScaledNumericValue()
    private val spawnWidthValue: ScaledNumericValue? = ScaledNumericValue()
    private val spawnHeightValue: ScaledNumericValue? = ScaledNumericValue()
    val spawnShape: SpawnShapeValue? = SpawnShapeValue()
    private var xSizeValues: Array<RangedNumericValue?>?
    private var ySizeValues: Array<RangedNumericValue?>?
    private var motionValues: Array<RangedNumericValue?>?
    private var accumulator = 0f
    private var sprites: Array<Sprite?>? = null
    var spriteMode: SpriteMode? = SpriteMode.single
    protected var particles: Array<Particle?>?
        private set
    var minParticleCount = 0
    private var maxParticleCount = 4
    var x = 0f
        private set
    var y = 0f
        private set
    var name: String? = null
    var imagePaths: Array<String?>? = null
    var activeCount = 0
        private set
    private var active: BooleanArray?
    private var firstUpdate = false
    private var flipX = false
    private var flipY = false
    private var updateFlags = 0
    private var allowCompletion = false
    private var bounds: BoundingBox? = null
    private var emission = 0
    private var emissionDiff = 0
    private var emissionDelta = 0
    private var lifeOffset = 0
    private var lifeOffsetDiff = 0
    private var life = 0
    private var lifeDiff = 0
    private var spawnWidth = 0f
    private var spawnWidthDiff = 0f
    private var spawnHeight = 0f
    private var spawnHeightDiff = 0f
    var duration = 1f
    var durationTimer = 0f
    private var delay = 0f
    private var delayTimer = 0f
    var isAttached = false
    var isContinuous = false
    var isAligned = false
    var isBehind = false
    var isAdditive = true
    var isPremultipliedAlpha = false
    var cleansUpBlendFunction = true

    constructor() {
        initialize()
    }

    constructor(reader: BufferedReader?) {
        initialize()
        load(reader)
    }

    constructor(emitter: ParticleEmitter?) {
        sprites = Array(emitter!!.sprites)
        name = emitter.name
        imagePaths = Array(emitter.imagePaths)
        setMaxParticleCount(emitter.maxParticleCount)
        minParticleCount = emitter.minParticleCount
        delayValue.load(emitter.delayValue)
        durationValue.load(emitter.durationValue)
        emissionValue.load(emitter.emissionValue)
        lifeValue.load(emitter.lifeValue)
        lifeOffsetValue.load(emitter.lifeOffsetValue)
        xScale.load(emitter.xScale)
        yScale.load(emitter.yScale)
        rotation.load(emitter.rotation)
        velocity.load(emitter.velocity)
        angle.load(emitter.angle)
        wind.load(emitter.wind)
        gravity.load(emitter.gravity)
        transparency.load(emitter.transparency)
        tint.load(emitter.tint)
        xOffsetValue.load(emitter.xOffsetValue)
        yOffsetValue.load(emitter.yOffsetValue)
        spawnWidthValue.load(emitter.spawnWidthValue)
        spawnHeightValue.load(emitter.spawnHeightValue)
        spawnShape.load(emitter.spawnShape)
        isAttached = emitter.isAttached
        isContinuous = emitter.isContinuous
        isAligned = emitter.isAligned
        isBehind = emitter.isBehind
        isAdditive = emitter.isAdditive
        isPremultipliedAlpha = emitter.isPremultipliedAlpha
        cleansUpBlendFunction = emitter.cleansUpBlendFunction
        spriteMode = emitter.spriteMode
        setPosition(emitter.x, emitter.y)
    }

    private fun initialize() {
        sprites = Array()
        imagePaths = Array()
        durationValue.isAlwaysActive = true
        emissionValue.isAlwaysActive = true
        lifeValue.isAlwaysActive = true
        xScale.isAlwaysActive = true
        transparency.isAlwaysActive = true
        spawnShape.isAlwaysActive = true
        spawnWidthValue.isAlwaysActive = true
        spawnHeightValue.isAlwaysActive = true
    }

    fun setMaxParticleCount(maxParticleCount: Int) {
        this.maxParticleCount = maxParticleCount
        active = BooleanArray(maxParticleCount)
        activeCount = 0
        particles = arrayOfNulls<Particle?>(maxParticleCount)
    }

    fun addParticle() {
        val activeCount = activeCount
        if (activeCount == maxParticleCount) return
        val active = active
        var i = 0
        val n = active!!.size
        while (i < n) {
            if (!active[i]) {
                activateParticle(i)
                active[i] = true
                this.activeCount = activeCount + 1
                break
            }
            i++
        }
    }

    fun addParticles(count: Int) {
        var count = count
        count = java.lang.Math.min(count, maxParticleCount - activeCount)
        if (count == 0) return
        val active = active
        var index = 0
        val n = active!!.size
        outer@ for (i in 0 until count) {
            while (index < n) {
                if (!active[index]) {
                    activateParticle(index)
                    active[index++] = true
                    continue@outer
                }
                index++
            }
            break
        }
        activeCount += count
    }

    fun update(delta: Float) {
        accumulator += delta * 1000
        if (accumulator < 1) return
        val deltaMillis = accumulator.toInt()
        accumulator -= deltaMillis.toFloat()
        if (delayTimer < delay) {
            delayTimer += deltaMillis.toFloat()
        } else {
            var done = false
            if (firstUpdate) {
                firstUpdate = false
                addParticle()
            }
            if (durationTimer < duration) durationTimer += deltaMillis.toFloat() else {
                if (!isContinuous || allowCompletion) done = true else restart()
            }
            if (!done) {
                emissionDelta += deltaMillis
                var emissionTime = emission + emissionDiff * emissionValue!!.getScale(durationTimer / duration)
                if (emissionTime > 0) {
                    emissionTime = 1000 / emissionTime
                    if (emissionDelta >= emissionTime) {
                        var emitCount = (emissionDelta / emissionTime).toInt()
                        emitCount = java.lang.Math.min(emitCount, maxParticleCount - activeCount)
                        emissionDelta -= emitCount * emissionTime.toInt()
                        emissionDelta %= emissionTime.toInt()
                        addParticles(emitCount)
                    }
                }
                if (activeCount < minParticleCount) addParticles(minParticleCount - activeCount)
            }
        }
        val active = active
        var activeCount = activeCount
        val particles = particles
        var i = 0
        val n = active!!.size
        while (i < n) {
            if (active[i] && !updateParticle(particles!![i], delta, deltaMillis)) {
                active[i] = false
                activeCount--
            }
            i++
        }
        this.activeCount = activeCount
    }

    fun draw(batch: Batch?) {
        if (isPremultipliedAlpha) {
            batch!!.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA)
        } else if (isAdditive) {
            batch!!.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        } else {
            batch!!.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        }
        val particles = particles
        val active = active
        var i = 0
        val n = active!!.size
        while (i < n) {
            if (active[i]) particles!![i].draw(batch)
            i++
        }
        if (cleansUpBlendFunction && (isAdditive || isPremultipliedAlpha)) batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    /**
     * Updates and draws the particles. This is slightly more efficient than calling [.update] and
     * [.draw] separately.
     */
    fun draw(batch: Batch?, delta: Float) {
        accumulator += delta * 1000
        if (accumulator < 1) {
            draw(batch)
            return
        }
        val deltaMillis = accumulator.toInt()
        accumulator -= deltaMillis.toFloat()
        if (isPremultipliedAlpha) {
            batch!!.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA)
        } else if (isAdditive) {
            batch!!.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        } else {
            batch!!.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        }
        val particles = particles
        val active = active
        var activeCount = activeCount
        var i = 0
        val n = active!!.size
        while (i < n) {
            if (active[i]) {
                val particle = particles!![i]
                if (updateParticle(particle, delta, deltaMillis)) particle.draw(batch) else {
                    active[i] = false
                    activeCount--
                }
            }
            i++
        }
        this.activeCount = activeCount
        if (cleansUpBlendFunction && (isAdditive || isPremultipliedAlpha)) batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        if (delayTimer < delay) {
            delayTimer += deltaMillis.toFloat()
            return
        }
        if (firstUpdate) {
            firstUpdate = false
            addParticle()
        }
        if (durationTimer < duration) durationTimer += deltaMillis.toFloat() else {
            if (!isContinuous || allowCompletion) return
            restart()
        }
        emissionDelta += deltaMillis
        var emissionTime = emission + emissionDiff * emissionValue!!.getScale(durationTimer / duration)
        if (emissionTime > 0) {
            emissionTime = 1000 / emissionTime
            if (emissionDelta >= emissionTime) {
                var emitCount = (emissionDelta / emissionTime).toInt()
                emitCount = java.lang.Math.min(emitCount, maxParticleCount - activeCount)
                emissionDelta -= emitCount * emissionTime.toInt()
                emissionDelta %= emissionTime.toInt()
                addParticles(emitCount)
            }
        }
        if (activeCount < minParticleCount) addParticles(minParticleCount - activeCount)
    }

    fun start() {
        firstUpdate = true
        allowCompletion = false
        restart()
    }

    fun reset() {
        emissionDelta = 0
        durationTimer = duration
        val active = active
        var i = 0
        val n = active!!.size
        while (i < n) {
            active[i] = false
            i++
        }
        activeCount = 0
        start()
    }

    private fun restart() {
        delay = if (delayValue.active) delayValue!!.newLowValue() else 0
        delayTimer = 0f
        durationTimer -= duration
        duration = durationValue!!.newLowValue()
        emission = emissionValue.newLowValue() as Int
        emissionDiff = emissionValue!!.newHighValue().toInt()
        if (!emissionValue.isRelative) emissionDiff -= emission
        if (!lifeValue!!.isIndependent) generateLifeValues()
        if (!lifeOffsetValue!!.isIndependent) generateLifeOffsetValues()
        spawnWidth = spawnWidthValue.newLowValue()
        spawnWidthDiff = spawnWidthValue!!.newHighValue()
        if (!spawnWidthValue.isRelative) spawnWidthDiff -= spawnWidth
        spawnHeight = spawnHeightValue.newLowValue()
        spawnHeightDiff = spawnHeightValue!!.newHighValue()
        if (!spawnHeightValue.isRelative) spawnHeightDiff -= spawnHeight
        updateFlags = 0
        if (angle.active && angle!!.timeline!!.size > 1) updateFlags = updateFlags or UPDATE_ANGLE
        if (velocity.active) updateFlags = updateFlags or UPDATE_VELOCITY
        if (xScale!!.timeline!!.size > 1) updateFlags = updateFlags or UPDATE_SCALE
        if (yScale.active && yScale!!.timeline!!.size > 1) updateFlags = updateFlags or UPDATE_SCALE
        if (rotation.active && rotation!!.timeline!!.size > 1) updateFlags = updateFlags or UPDATE_ROTATION
        if (wind.active) updateFlags = updateFlags or UPDATE_WIND
        if (gravity.active) updateFlags = updateFlags or UPDATE_GRAVITY
        if (tint!!.timeline!!.size > 1) updateFlags = updateFlags or UPDATE_TINT
        if (spriteMode == SpriteMode.animated) updateFlags = updateFlags or UPDATE_SPRITE
    }

    protected fun newParticle(sprite: Sprite?): Particle? {
        return Particle(sprite)
    }

    private fun activateParticle(index: Int) {
        var sprite: Sprite? = null
        when (spriteMode) {
            SpriteMode.single, SpriteMode.animated -> sprite = sprites!!.first()
            SpriteMode.random -> sprite = sprites!!.random()
        }
        var particle = particles!![index]
        if (particle == null) {
            particle = newParticle(sprite)
            particles!![index] = particle
            particle.flip(flipX, flipY)
        } else {
            particle.set(sprite)
        }
        val percent = durationTimer / duration
        val updateFlags = updateFlags
        if (lifeValue!!.isIndependent) generateLifeValues()
        if (lifeOffsetValue!!.isIndependent) generateLifeOffsetValues()
        particle!!.life = life + (lifeDiff * lifeValue.getScale(percent)) as Int
        particle.currentLife = particle.life
        if (velocity.active) {
            particle.velocity = velocity.newLowValue()
            particle.velocityDiff = velocity!!.newHighValue()
            if (!velocity.isRelative) particle.velocityDiff -= particle.velocity
        }
        particle.angle = angle.newLowValue()
        particle.angleDiff = angle!!.newHighValue()
        if (!angle.isRelative) particle.angleDiff -= particle.angle
        var angle = 0f
        if (updateFlags and UPDATE_ANGLE == 0) {
            angle = particle.angle + particle.angleDiff * angle.getScale(0f)
            particle.angle = angle
            particle.angleCos = MathUtils.cosDeg(angle)
            particle.angleSin = MathUtils.sinDeg(angle)
        }
        val spriteWidth = sprite!!.width
        val spriteHeight = sprite.height
        particle.xScale = xScale.newLowValue() / spriteWidth
        particle.xScaleDiff = xScale!!.newHighValue() / spriteWidth
        if (!xScale.isRelative) particle.xScaleDiff -= particle.xScale
        if (yScale.active) {
            particle.yScale = yScale.newLowValue() / spriteHeight
            particle.yScaleDiff = yScale!!.newHighValue() / spriteHeight
            if (!yScale.isRelative) particle.yScaleDiff -= particle.yScale
            particle.setScale(particle.xScale + particle.xScaleDiff * xScale.getScale(0f),
                particle.yScale + particle.yScaleDiff * yScale.getScale(0f))
        } else {
            particle.setScale(particle.xScale + particle.xScaleDiff * xScale.getScale(0f))
        }
        if (rotation.active) {
            particle.rotation = rotation.newLowValue()
            particle.rotationDiff = rotation!!.newHighValue()
            if (!rotation.isRelative) particle.rotationDiff -= particle.rotation
            var rotation = particle.rotation + particle.rotationDiff * rotation.getScale(0f)
            if (isAligned) rotation += angle
            particle.setRotation(rotation)
        }
        if (wind.active) {
            particle.wind = wind.newLowValue()
            particle.windDiff = wind!!.newHighValue()
            if (!wind.isRelative) particle.windDiff -= particle.wind
        }
        if (gravity.active) {
            particle.gravity = gravity.newLowValue()
            particle.gravityDiff = gravity!!.newHighValue()
            if (!gravity.isRelative) particle.gravityDiff -= particle.gravity
        }
        var color = particle.tint
        if (color == null) {
            color = FloatArray(3)
            particle.tint = color
        }
        val temp = tint!!.getColor(0f)
        color[0] = temp!![0]
        color[1] = temp[1]
        color[2] = temp[2]
        particle.transparency = transparency.newLowValue()
        particle.transparencyDiff = transparency!!.newHighValue() - particle.transparency

        // Spawn.
        var x = x
        if (xOffsetValue.active) x += xOffsetValue!!.newLowValue()
        var y = y
        if (yOffsetValue.active) y += yOffsetValue!!.newLowValue()
        when (spawnShape!!.shape) {
            SpawnShape.square -> {
                val width = spawnWidth + spawnWidthDiff * spawnWidthValue!!.getScale(percent)
                val height = spawnHeight + spawnHeightDiff * spawnHeightValue!!.getScale(percent)
                x += MathUtils.random(width) - width / 2
                y += MathUtils.random(height) - height / 2
            }
            SpawnShape.ellipse -> {
                val width = spawnWidth + spawnWidthDiff * spawnWidthValue!!.getScale(percent)
                val height = spawnHeight + spawnHeightDiff * spawnHeightValue!!.getScale(percent)
                val radiusX = width / 2
                val radiusY = height / 2
                if (radiusX == 0f || radiusY == 0f) break
                val scaleY = radiusX / radiusY
                if (spawnShape.isEdges) {
                    val spawnAngle: Float
                    spawnAngle = when (spawnShape.side) {
                        SpawnEllipseSide.top -> -MathUtils.random(179f)
                        SpawnEllipseSide.bottom -> MathUtils.random(179f)
                        else -> MathUtils.random(360f)
                    }
                    val cosDeg: Float = MathUtils.cosDeg(spawnAngle)
                    val sinDeg: Float = MathUtils.sinDeg(spawnAngle)
                    x += cosDeg * radiusX
                    y += sinDeg * radiusX / scaleY
                    if (updateFlags and UPDATE_ANGLE == 0) {
                        particle.angle = spawnAngle
                        particle.angleCos = cosDeg
                        particle.angleSin = sinDeg
                    }
                } else {
                    val radius2 = radiusX * radiusX
                    while (true) {
                        val px: Float = MathUtils.random(width) - radiusX
                        val py: Float = MathUtils.random(width) - radiusX
                        if (px * px + py * py <= radius2) {
                            x += px
                            y += py / scaleY
                            break
                        }
                    }
                }
            }
            SpawnShape.line -> {
                val width = spawnWidth + spawnWidthDiff * spawnWidthValue!!.getScale(percent)
                val height = spawnHeight + spawnHeightDiff * spawnHeightValue!!.getScale(percent)
                if (width != 0f) {
                    val lineX: Float = width * MathUtils.random()
                    x += lineX
                    y += lineX * (height / width)
                } else y += height * MathUtils.random()
            }
        }
        particle.setBounds(x - spriteWidth / 2, y - spriteHeight / 2, spriteWidth, spriteHeight)
        var offsetTime = (lifeOffset + lifeOffsetDiff * lifeOffsetValue.getScale(percent)) as Int
        if (offsetTime > 0) {
            if (offsetTime >= particle.currentLife) offsetTime = particle.currentLife - 1
            updateParticle(particle, offsetTime / 1000f, offsetTime)
        }
    }

    private fun updateParticle(particle: Particle?, delta: Float, deltaMillis: Int): Boolean {
        val life = particle!!.currentLife - deltaMillis
        if (life <= 0) return false
        particle.currentLife = life
        val percent = 1 - particle.currentLife / particle.life.toFloat()
        val updateFlags = updateFlags
        if (updateFlags and UPDATE_SCALE != 0) {
            if (yScale.active) {
                particle.setScale(particle.xScale + particle.xScaleDiff * xScale!!.getScale(percent),
                    particle.yScale + particle.yScaleDiff * yScale!!.getScale(percent))
            } else {
                particle.setScale(particle.xScale + particle.xScaleDiff * xScale!!.getScale(percent))
            }
        }
        if (updateFlags and UPDATE_VELOCITY != 0) {
            val velocity = (particle.velocity + particle.velocityDiff * velocity!!.getScale(percent)) * delta
            var velocityX: Float
            var velocityY: Float
            if (updateFlags and UPDATE_ANGLE != 0) {
                val angle = particle.angle + particle.angleDiff * angle!!.getScale(percent)
                velocityX = velocity * MathUtils.cosDeg(angle)
                velocityY = velocity * MathUtils.sinDeg(angle)
                if (updateFlags and UPDATE_ROTATION != 0) {
                    var rotation = particle.rotation + particle.rotationDiff * rotation!!.getScale(percent)
                    if (isAligned) rotation += angle
                    particle.setRotation(rotation)
                }
            } else {
                velocityX = velocity * particle.angleCos
                velocityY = velocity * particle.angleSin
                if (isAligned || updateFlags and UPDATE_ROTATION != 0) {
                    var rotation = particle.rotation + particle.rotationDiff * rotation!!.getScale(percent)
                    if (isAligned) rotation += particle.angle
                    particle.setRotation(rotation)
                }
            }
            if (updateFlags and UPDATE_WIND != 0) velocityX += (particle.wind + particle.windDiff * wind!!.getScale(percent)) * delta
            if (updateFlags and UPDATE_GRAVITY != 0) velocityY += (particle.gravity + particle.gravityDiff * gravity!!.getScale(percent)) * delta
            particle.translate(velocityX, velocityY)
        } else {
            if (updateFlags and UPDATE_ROTATION != 0) particle.setRotation(particle.rotation + particle.rotationDiff * rotation!!.getScale(percent))
        }
        val color: FloatArray?
        color = if (updateFlags and UPDATE_TINT != 0) tint!!.getColor(percent) else particle.tint
        if (isPremultipliedAlpha) {
            val alphaMultiplier: Float = if (isAdditive) 0 else 1.toFloat()
            val a = particle.transparency + particle.transparencyDiff * transparency!!.getScale(percent)
            particle.setColor(color!![0] * a, color[1] * a, color[2] * a, a * alphaMultiplier)
        } else {
            particle.setColor(color!![0], color[1], color[2],
                particle.transparency + particle.transparencyDiff * transparency!!.getScale(percent))
        }
        if (updateFlags and UPDATE_SPRITE != 0) {
            val frame: Int = java.lang.Math.min((percent * sprites!!.size).toInt(), sprites!!.size - 1)
            if (particle.frame != frame) {
                val sprite = sprites!![frame]
                val prevSpriteWidth: Float = particle.width
                val prevSpriteHeight: Float = particle.height
                particle.setRegion(sprite)
                particle.setSize(sprite!!.width, sprite.height)
                particle.setOrigin(sprite!!.originX, sprite.originY)
                particle.translate((prevSpriteWidth - sprite!!.width) / 2, (prevSpriteHeight - sprite.height) / 2)
                particle.frame = frame
            }
        }
        return true
    }

    private fun generateLifeValues() {
        life = lifeValue.newLowValue() as Int
        lifeDiff = lifeValue.newHighValue() as Int
        if (!lifeValue.isRelative) lifeDiff -= life
    }

    private fun generateLifeOffsetValues() {
        lifeOffset = if (lifeOffsetValue.active) lifeOffsetValue.newLowValue() as Int else 0
        lifeOffsetDiff = lifeOffsetValue.newHighValue() as Int
        if (!lifeOffsetValue.isRelative) lifeOffsetDiff -= lifeOffset
    }

    fun setPosition(x: Float, y: Float) {
        if (isAttached) {
            val xAmount = x - this.x
            val yAmount = y - this.y
            val active = active
            var i = 0
            val n = active!!.size
            while (i < n) {
                if (active[i]) particles!![i].translate(xAmount, yAmount)
                i++
            }
        }
        this.x = x
        this.y = y
    }

    fun setSprites(sprites: Array<Sprite?>?) {
        this.sprites = sprites
        if (sprites!!.size === 0) return
        var i = 0
        val n = particles!!.size
        while (i < n) {
            val particle = particles!![i] ?: break
            var sprite: Sprite? = null
            when (spriteMode) {
                SpriteMode.single -> sprite = sprites!!.first()
                SpriteMode.random -> sprite = sprites!!.random()
                SpriteMode.animated -> {
                    val percent = 1 - particle.currentLife / particle.life.toFloat()
                    particle.frame = java.lang.Math.min((percent * sprites!!.size).toInt(), sprites.size - 1)
                    sprite = sprites[particle.frame]
                }
            }
            particle.setRegion(sprite)
            particle.setOrigin(sprite!!.originX, sprite.originY)
            i++
        }
    }

    /**
     * Ignores the [continuous][.setContinuous] setting until the emitter is started again. This allows the emitter
     * to stop smoothly.
     */
    fun allowCompletion() {
        allowCompletion = true
        durationTimer = duration
    }

    fun getSprites(): Array<Sprite?>? {
        return sprites
    }

    fun getLife(): ScaledNumericValue? {
        return lifeValue
    }

    fun getEmission(): ScaledNumericValue? {
        return emissionValue
    }

    fun getDuration(): RangedNumericValue? {
        return durationValue
    }

    fun getDelay(): RangedNumericValue? {
        return delayValue
    }

    fun getLifeOffset(): ScaledNumericValue? {
        return lifeOffsetValue
    }

    fun getSpawnWidth(): ScaledNumericValue? {
        return spawnWidthValue
    }

    fun getSpawnHeight(): ScaledNumericValue? {
        return spawnHeightValue
    }

    /**
     * @return Whether this ParticleEmitter automatically returns the [Batch][com.badlogic.gdx.graphics.g2d.Batch]'s blend
     * function to the alpha-blending default (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA) when done drawing.
     */
    fun cleansUpBlendFunction(): Boolean {
        return cleansUpBlendFunction
    }

    /**
     * Set whether to automatically return the [Batch][com.badlogic.gdx.graphics.g2d.Batch]'s blend function to the
     * alpha-blending default (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA) when done drawing. Is true by default. If set to false, the
     * Batch's blend function is left as it was for drawing this ParticleEmitter, which prevents the Batch from being flushed
     * repeatedly if consecutive ParticleEmitters with the same additive or pre-multiplied alpha state are drawn in a row.
     *
     *
     * IMPORTANT: If set to false and if the next object to use this Batch expects alpha blending, you are responsible for setting
     * the Batch's blend function to (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA) before that next object is drawn.
     *
     * @param cleansUpBlendFunction
     */
    fun setCleansUpBlendFunction(cleansUpBlendFunction: Boolean) {
        this.cleansUpBlendFunction = cleansUpBlendFunction
    }

    fun getMaxParticleCount(): Int {
        return maxParticleCount
    }

    val isComplete: Boolean
        get() {
            if (isContinuous && !allowCompletion) return false
            return if (delayTimer < delay) false else durationTimer >= duration && activeCount == 0
        }

    val percentComplete: Float
        get() = if (delayTimer < delay) 0 else java.lang.Math.min(1f, durationTimer / duration)

    fun setFlip(flipX: Boolean, flipY: Boolean) {
        this.flipX = flipX
        this.flipY = flipY
        if (particles == null) return
        var i = 0
        val n = particles!!.size
        while (i < n) {
            val particle = particles!![i]
            particle?.flip(flipX, flipY)
            i++
        }
    }

    fun flipY() {
        angle!!.setHigh(-angle.highMin, -angle.highMax)
        angle.setLow(-angle.lowMin, -angle.lowMax)
        gravity!!.setHigh(-gravity.highMin, -gravity.highMax)
        gravity.setLow(-gravity.lowMin, -gravity.lowMax)
        wind!!.setHigh(-wind.highMin, -wind.highMax)
        wind.setLow(-wind.lowMin, -wind.lowMax)
        rotation!!.setHigh(-rotation.highMin, -rotation.highMax)
        rotation.setLow(-rotation.lowMin, -rotation.lowMax)
        yOffsetValue!!.setLow(-yOffsetValue.lowMin, -yOffsetValue.lowMax)
    }

    /**
     * Returns the bounding box for all active particles. z axis will always be zero.
     */
    val boundingBox: BoundingBox?
        get() {
            if (bounds == null) bounds = BoundingBox()
            val particles = particles
            val active = active
            val bounds: BoundingBox? = bounds
            bounds.inf()
            var i = 0
            val n = active!!.size
            while (i < n) {
                if (active[i]) {
                    val r: Rectangle = particles!![i].boundingRectangle
                    bounds.ext(r.x, r.y, 0)
                    bounds.ext(r.x + r.width, r.y + r.height, 0)
                }
                i++
            }
            return bounds
        }

    protected fun getXSizeValues(): Array<RangedNumericValue?>? {
        if (xSizeValues == null) {
            xSizeValues = arrayOfNulls<RangedNumericValue?>(3)
            xSizeValues!![0] = xScale
            xSizeValues!![1] = spawnWidthValue
            xSizeValues!![2] = xOffsetValue
        }
        return xSizeValues
    }

    protected fun getYSizeValues(): Array<RangedNumericValue?>? {
        if (ySizeValues == null) {
            ySizeValues = arrayOfNulls<RangedNumericValue?>(3)
            ySizeValues!![0] = yScale
            ySizeValues!![1] = spawnHeightValue
            ySizeValues!![2] = yOffsetValue
        }
        return ySizeValues
    }

    protected fun getMotionValues(): Array<RangedNumericValue?>? {
        if (motionValues == null) {
            motionValues = arrayOfNulls<RangedNumericValue?>(3)
            motionValues!![0] = velocity
            motionValues!![1] = wind
            motionValues!![2] = gravity
        }
        return motionValues
    }

    /**
     * Permanently scales the size of the emitter by scaling all its ranged values related to size.
     */
    fun scaleSize(scale: Float) {
        if (scale == 1f) return
        scaleSize(scale, scale)
    }

    /**
     * Permanently scales the size of the emitter by scaling all its ranged values related to size.
     */
    fun scaleSize(scaleX: Float, scaleY: Float) {
        if (scaleX == 1f && scaleY == 1f) return
        for (value in getXSizeValues()!!) value!!.scale(scaleX)
        for (value in getYSizeValues()!!) value!!.scale(scaleY)
    }

    /**
     * Permanently scales the speed of the emitter by scaling all its ranged values related to motion.
     */
    fun scaleMotion(scale: Float) {
        if (scale == 1f) return
        for (value in getMotionValues()!!) value!!.scale(scale)
    }

    /**
     * Sets all size-related ranged values to match those of the template emitter.
     */
    fun matchSize(template: ParticleEmitter?) {
        matchXSize(template)
        matchYSize(template)
    }

    /**
     * Sets all horizontal size-related ranged values to match those of the template emitter.
     */
    fun matchXSize(template: ParticleEmitter?) {
        val values = getXSizeValues()
        val templateValues = template!!.getXSizeValues()
        for (i in values!!.indices) {
            values[i]!!.set(templateValues!![i])
        }
    }

    /**
     * Sets all vertical size-related ranged values to match those of the template emitter.
     */
    fun matchYSize(template: ParticleEmitter?) {
        val values = getYSizeValues()
        val templateValues = template!!.getYSizeValues()
        for (i in values!!.indices) {
            values[i]!!.set(templateValues!![i])
        }
    }

    /**
     * Sets all motion-related ranged values to match those of the template emitter.
     */
    fun matchMotion(template: ParticleEmitter?) {
        val values = getMotionValues()
        val templateValues = template!!.getMotionValues()
        for (i in values!!.indices) {
            values[i]!!.set(templateValues!![i])
        }
    }

    @Throws(IOException::class)
    fun save(output: Writer?) {
        output.write("""
    $name
    
    """.trimIndent())
        output.write("- Delay -\n")
        delayValue!!.save(output)
        output.write("- Duration - \n")
        durationValue!!.save(output)
        output.write("- Count - \n")
        output.write("min: $minParticleCount\n")
        output.write("max: $maxParticleCount\n")
        output.write("- Emission - \n")
        emissionValue!!.save(output)
        output.write("- Life - \n")
        lifeValue!!.save(output)
        output.write("- Life Offset - \n")
        lifeOffsetValue!!.save(output)
        output.write("- X Offset - \n")
        xOffsetValue!!.save(output)
        output.write("- Y Offset - \n")
        yOffsetValue!!.save(output)
        output.write("- Spawn Shape - \n")
        spawnShape!!.save(output)
        output.write("- Spawn Width - \n")
        spawnWidthValue!!.save(output)
        output.write("- Spawn Height - \n")
        spawnHeightValue!!.save(output)
        output.write("- X Scale - \n")
        xScale!!.save(output)
        output.write("- Y Scale - \n")
        yScale!!.save(output)
        output.write("- Velocity - \n")
        velocity!!.save(output)
        output.write("- Angle - \n")
        angle!!.save(output)
        output.write("- Rotation - \n")
        rotation!!.save(output)
        output.write("- Wind - \n")
        wind!!.save(output)
        output.write("- Gravity - \n")
        gravity!!.save(output)
        output.write("- Tint - \n")
        tint!!.save(output)
        output.write("- Transparency - \n")
        transparency!!.save(output)
        output.write("- Options - \n")
        output.write("""
    attached: ${isAttached}
    
    """.trimIndent())
        output.write("""
    continuous: ${isContinuous}
    
    """.trimIndent())
        output.write("""
    aligned: ${isAligned}
    
    """.trimIndent())
        output.write("""
    additive: ${isAdditive}
    
    """.trimIndent())
        output.write("""
    behind: ${isBehind}
    
    """.trimIndent())
        output.write("""
    premultipliedAlpha: ${isPremultipliedAlpha}
    
    """.trimIndent())
        output.write("""
    spriteMode: ${spriteMode.toString()}
    
    """.trimIndent())
        output.write("- Image Paths -\n")
        for (imagePath in imagePaths!!) {
            output.write("""
    $imagePath
    
    """.trimIndent())
        }
        output.write("\n")
    }

    @Throws(IOException::class)
    fun load(reader: BufferedReader?) {
        try {
            name = readString(reader, "name")
            reader.readLine()
            delayValue.load(reader)
            reader.readLine()
            durationValue.load(reader)
            reader.readLine()
            minParticleCount = readInt(reader, "minParticleCount")
            setMaxParticleCount(readInt(reader, "maxParticleCount"))
            reader.readLine()
            emissionValue.load(reader)
            reader.readLine()
            lifeValue.load(reader)
            reader.readLine()
            lifeOffsetValue.load(reader)
            reader.readLine()
            xOffsetValue.load(reader)
            reader.readLine()
            yOffsetValue.load(reader)
            reader.readLine()
            spawnShape.load(reader)
            reader.readLine()
            spawnWidthValue.load(reader)
            reader.readLine()
            spawnHeightValue.load(reader)
            var line: String? = reader.readLine()
            if (line!!.trim { it <= ' ' } == "- Scale -") {
                xScale.load(reader)
                yScale.setActive(false)
            } else {
                xScale.load(reader)
                reader.readLine()
                yScale.load(reader)
            }
            reader.readLine()
            velocity.load(reader)
            reader.readLine()
            angle.load(reader)
            reader.readLine()
            rotation.load(reader)
            reader.readLine()
            wind.load(reader)
            reader.readLine()
            gravity.load(reader)
            reader.readLine()
            tint.load(reader)
            reader.readLine()
            transparency.load(reader)
            reader.readLine()
            isAttached = readBoolean(reader, "attached")
            isContinuous = readBoolean(reader, "continuous")
            isAligned = readBoolean(reader, "aligned")
            isAdditive = readBoolean(reader, "additive")
            isBehind = readBoolean(reader, "behind")

            // Backwards compatibility
            line = reader.readLine()
            if (line.startsWith("premultipliedAlpha")) {
                isPremultipliedAlpha = readBoolean(line)
                line = reader.readLine()
            }
            if (line.startsWith("spriteMode")) {
                spriteMode = SpriteMode.valueOf(readString(line)!!)
                line = reader.readLine()
            }
            var imagePaths: Array<String?>? = Array()
            while (reader.readLine().also({ line = it }) != null && !line!!.isEmpty()) {
                imagePaths.add(line)
            }
            imagePaths = imagePaths
        } catch (ex: RuntimeException) {
            if (name == null) throw ex
            throw RuntimeException("Error parsing emitter: $name", ex)
        }
    }

    class Particle(sprite: Sprite?) : Sprite(sprite) {
        var life = 0
        var currentLife = 0
        var xScale = 0f
        var xScaleDiff = 0f
        var yScale = 0f
        var yScaleDiff = 0f
        var rotation = 0f
        var rotationDiff = 0f
        var velocity = 0f
        var velocityDiff = 0f
        var angle = 0f
        var angleDiff = 0f
        var angleCos = 0f
        var angleSin = 0f
        var transparency = 0f
        var transparencyDiff = 0f
        var wind = 0f
        var windDiff = 0f
        var gravity = 0f
        var gravityDiff = 0f
        var tint: FloatArray?
        var frame = 0
    }

    class ParticleValue {
        var active = false
        var isAlwaysActive = false

        fun isActive(): Boolean {
            return isAlwaysActive || active
        }

        fun setActive(active: Boolean) {
            this.active = active
        }

        @Throws(IOException::class)
        fun save(output: Writer?) {
            if (!isAlwaysActive) output.write("active: $active\n") else active = true
        }

        @Throws(IOException::class)
        fun load(reader: BufferedReader?) {
            active = if (!isAlwaysActive) readBoolean(reader, "active") else true
        }

        fun load(value: ParticleValue?) {
            active = value!!.active
            isAlwaysActive = value.isAlwaysActive
        }
    }

    class NumericValue : ParticleValue() {
        var value = 0f

        @Throws(IOException::class)
        override fun save(output: Writer?) {
            super.save(output)
            if (!active) return
            output.write("value: $value\n")
        }

        @Throws(IOException::class)
        override fun load(reader: BufferedReader?) {
            super.load(reader)
            if (!active) return
            value = readFloat(reader, "value")
        }

        fun load(value: NumericValue?) {
            super.load(value)
            this.value = value!!.value
        }
    }

    class RangedNumericValue : ParticleValue() {
        var lowMin = 0f
        var lowMax = 0f
        fun newLowValue(): Float {
            return lowMin + (lowMax - lowMin) * MathUtils.random()
        }

        fun setLow(value: Float) {
            lowMin = value
            lowMax = value
        }

        fun setLow(min: Float, max: Float) {
            lowMin = min
            lowMax = max
        }

        /**
         * permanently scales the range by a scalar.
         */
        fun scale(scale: Float) {
            lowMin *= scale
            lowMax *= scale
        }

        fun set(value: RangedNumericValue?) {
            lowMin = value!!.lowMin
            lowMax = value.lowMax
        }

        @Throws(IOException::class)
        override fun save(output: Writer?) {
            super.save(output)
            if (!active) return
            output.write("lowMin: $lowMin\n")
            output.write("lowMax: $lowMax\n")
        }

        @Throws(IOException::class)
        override fun load(reader: BufferedReader?) {
            super.load(reader)
            if (!active) return
            lowMin = readFloat(reader, "lowMin")
            lowMax = readFloat(reader, "lowMax")
        }

        fun load(value: RangedNumericValue?) {
            super.load(value)
            lowMax = value!!.lowMax
            lowMin = value.lowMin
        }
    }

    class ScaledNumericValue : RangedNumericValue() {
        var scaling: FloatArray? = floatArrayOf(1f)
        var timeline: FloatArray? = floatArrayOf(0f)
        var highMin = 0f
        var highMax = 0f
        var isRelative = false
        fun newHighValue(): Float {
            return highMin + (highMax - highMin) * MathUtils.random()
        }

        fun setHigh(value: Float) {
            highMin = value
            highMax = value
        }

        fun setHigh(min: Float, max: Float) {
            highMin = min
            highMax = max
        }

        override fun scale(scale: Float) {
            super.scale(scale)
            highMin *= scale
            highMax *= scale
        }

        override fun set(value: RangedNumericValue?) {
            if (value is ScaledNumericValue) set(value as ScaledNumericValue?) else super.set(value)
        }

        fun set(value: ScaledNumericValue?) {
            super.set(value)
            highMin = value!!.highMin
            highMax = value.highMax
            if (scaling!!.size != value.scaling!!.size) scaling = Arrays.copyOf(value.scaling, value.scaling!!.size) else java.lang.System.arraycopy(value.scaling, 0, scaling, 0, scaling!!.size)
            if (timeline!!.size != value.timeline!!.size) timeline = Arrays.copyOf(value.timeline, value.timeline!!.size) else java.lang.System.arraycopy(value.timeline, 0, timeline, 0, timeline!!.size)
            isRelative = value.isRelative
        }

        fun getScale(percent: Float): Float {
            var endIndex = -1
            val timeline = timeline
            val n = timeline!!.size
            for (i in 1 until n) {
                val t = timeline[i]
                if (t > percent) {
                    endIndex = i
                    break
                }
            }
            if (endIndex == -1) return scaling!![n - 1]
            val scaling = scaling
            val startIndex = endIndex - 1
            val startValue = scaling!![startIndex]
            val startTime = timeline[startIndex]
            return startValue + (scaling[endIndex] - startValue) * ((percent - startTime) / (timeline[endIndex] - startTime))
        }

        @Throws(IOException::class)
        override fun save(output: Writer?) {
            super.save(output)
            if (!active) return
            output.write("highMin: $highMin\n")
            output.write("highMax: $highMax\n")
            output.write("""
    relative: ${isRelative}
    
    """.trimIndent())
            output.write("""
    scalingCount: ${scaling!!.size}
    
    """.trimIndent())
            for (i in scaling!!.indices) output.write("""
    scaling$i: ${scaling!![i]}
    
    """.trimIndent())
            output.write("""
    timelineCount: ${timeline!!.size}
    
    """.trimIndent())
            for (i in timeline!!.indices) output.write("""
    timeline$i: ${timeline!![i]}
    
    """.trimIndent())
        }

        @Throws(IOException::class)
        override fun load(reader: BufferedReader?) {
            super.load(reader)
            if (!active) return
            highMin = readFloat(reader, "highMin")
            highMax = readFloat(reader, "highMax")
            isRelative = readBoolean(reader, "relative")
            scaling = FloatArray(readInt(reader, "scalingCount"))
            for (i in scaling!!.indices) scaling!![i] = readFloat(reader, "scaling$i")
            timeline = FloatArray(readInt(reader, "timelineCount"))
            for (i in timeline!!.indices) timeline!![i] = readFloat(reader, "timeline$i")
        }

        fun load(value: ScaledNumericValue?) {
            super.load(value)
            highMax = value!!.highMax
            highMin = value.highMin
            scaling = FloatArray(value.scaling!!.size)
            java.lang.System.arraycopy(value.scaling, 0, scaling, 0, scaling!!.size)
            timeline = FloatArray(value.timeline!!.size)
            java.lang.System.arraycopy(value.timeline, 0, timeline, 0, timeline!!.size)
            isRelative = value.isRelative
        }
    }

    class IndependentScaledNumericValue : ScaledNumericValue() {
        var isIndependent = false

        override fun set(value: RangedNumericValue?) {
            if (value is IndependentScaledNumericValue) set(value as IndependentScaledNumericValue?) else super.set(value)
        }

        override fun set(value: ScaledNumericValue?) {
            if (value is IndependentScaledNumericValue) set(value as IndependentScaledNumericValue?) else super.set(value)
        }

        fun set(value: IndependentScaledNumericValue?) {
            super.set(value)
            isIndependent = value!!.isIndependent
        }

        @Throws(IOException::class)
        override fun save(output: Writer?) {
            super.save(output)
            output.write("""
    independent: ${isIndependent}
    
    """.trimIndent())
        }

        @Throws(IOException::class)
        override fun load(reader: BufferedReader?) {
            super.load(reader)
            // For backwards compatibility, independent property may not be defined
            if (reader.markSupported()) reader.mark(100)
            val line: String = reader.readLine() ?: throw IOException("Missing value: independent")
            if (line.contains("independent")) isIndependent = java.lang.Boolean.parseBoolean(readString(line)) else if (reader.markSupported()) reader.reset() else {
                // @see java.io.BufferedReader#markSupported may return false in some platforms (such as GWT),
                // in that case backwards commpatibility is not possible
                val errorMessage = "The loaded particle effect descriptor file uses an old invalid format. " +
                    "Please download the latest version of the Particle Editor tool and recreate the file by" +
                    " loading and saving it again."
                Gdx.app.error("ParticleEmitter", errorMessage)
                throw IOException(errorMessage)
            }
        }

        fun load(value: IndependentScaledNumericValue?) {
            super.load(value)
            isIndependent = value!!.isIndependent
        }
    }

    class GradientColorValue : ParticleValue() {
        /**
         * @return the r, g and b values for every timeline position
         */
        /**
         * @param colors the r, g and b values for every timeline position
         */
        var colors: FloatArray? = floatArrayOf(1f, 1f, 1f)
        var timeline: FloatArray? = floatArrayOf(0f)

        fun getColor(percent: Float): FloatArray? {
            var startIndex = 0
            var endIndex = -1
            val timeline = timeline
            val n = timeline!!.size
            for (i in 1 until n) {
                val t = timeline[i]
                if (t > percent) {
                    endIndex = i
                    break
                }
                startIndex = i
            }
            val startTime = timeline[startIndex]
            startIndex *= 3
            val r1 = colors!![startIndex]
            val g1 = colors!![startIndex + 1]
            val b1 = colors!![startIndex + 2]
            if (endIndex == -1) {
                temp!![0] = r1
                temp[1] = g1
                temp[2] = b1
                return temp
            }
            val factor = (percent - startTime) / (timeline[endIndex] - startTime)
            endIndex *= 3
            temp!![0] = r1 + (colors!![endIndex] - r1) * factor
            temp[1] = g1 + (colors!![endIndex + 1] - g1) * factor
            temp[2] = b1 + (colors!![endIndex + 2] - b1) * factor
            return temp
        }

        @Throws(IOException::class)
        override fun save(output: Writer?) {
            super.save(output)
            if (!active) return
            output.write("""
    colorsCount: ${colors!!.size}
    
    """.trimIndent())
            for (i in colors!!.indices) output.write("""
    colors$i: ${colors!![i]}
    
    """.trimIndent())
            output.write("""
    timelineCount: ${timeline!!.size}
    
    """.trimIndent())
            for (i in timeline!!.indices) output.write("""
    timeline$i: ${timeline!![i]}
    
    """.trimIndent())
        }

        @Throws(IOException::class)
        override fun load(reader: BufferedReader?) {
            super.load(reader)
            if (!active) return
            colors = FloatArray(readInt(reader, "colorsCount"))
            for (i in colors!!.indices) colors!![i] = readFloat(reader, "colors$i")
            timeline = FloatArray(readInt(reader, "timelineCount"))
            for (i in timeline!!.indices) timeline!![i] = readFloat(reader, "timeline$i")
        }

        fun load(value: GradientColorValue?) {
            super.load(value)
            colors = FloatArray(value!!.colors!!.size)
            java.lang.System.arraycopy(value.colors, 0, colors, 0, colors!!.size)
            timeline = FloatArray(value.timeline!!.size)
            java.lang.System.arraycopy(value.timeline, 0, timeline, 0, timeline!!.size)
        }

        companion object {
            private val temp: FloatArray? = FloatArray(4)
        }

        init {
            isAlwaysActive = true
        }
    }

    class SpawnShapeValue : ParticleValue() {
        var shape: SpawnShape? = SpawnShape.point
        var isEdges = false
        var side: SpawnEllipseSide? = SpawnEllipseSide.both

        @Throws(IOException::class)
        override fun save(output: Writer?) {
            super.save(output)
            if (!active) return
            output.write("shape: $shape\n")
            if (shape == SpawnShape.ellipse) {
                output.write("""
    edges: ${isEdges}
    
    """.trimIndent())
                output.write("side: $side\n")
            }
        }

        @Throws(IOException::class)
        override fun load(reader: BufferedReader?) {
            super.load(reader)
            if (!active) return
            shape = SpawnShape.valueOf(readString(reader, "shape")!!)
            if (shape == SpawnShape.ellipse) {
                isEdges = readBoolean(reader, "edges")
                side = SpawnEllipseSide.valueOf(readString(reader, "side")!!)
            }
        }

        fun load(value: SpawnShapeValue?) {
            super.load(value)
            shape = value!!.shape
            isEdges = value.isEdges
            side = value.side
        }
    }

    enum class SpawnShape {
        point, line, square, ellipse
    }

    enum class SpawnEllipseSide {
        both, top, bottom
    }

    enum class SpriteMode {
        single, random, animated
    }

    companion object {
        private const val UPDATE_SCALE = 1 shl 0
        private const val UPDATE_ANGLE = 1 shl 1
        private const val UPDATE_ROTATION = 1 shl 2
        private const val UPDATE_VELOCITY = 1 shl 3
        private const val UPDATE_WIND = 1 shl 4
        private const val UPDATE_GRAVITY = 1 shl 5
        private const val UPDATE_TINT = 1 shl 6
        private const val UPDATE_SPRITE = 1 shl 7

        @Throws(IOException::class)
        fun readString(line: String?): String? {
            return line!!.substring(line.indexOf(":") + 1).trim { it <= ' ' }
        }

        @Throws(IOException::class)
        fun readString(reader: BufferedReader?, name: String?): String? {
            val line: String = reader.readLine() ?: throw IOException("Missing value: $name")
            return readString(line)
        }

        @Throws(IOException::class)
        fun readBoolean(line: String?): Boolean {
            return java.lang.Boolean.parseBoolean(readString(line))
        }

        @Throws(IOException::class)
        fun readBoolean(reader: BufferedReader?, name: String?): Boolean {
            return java.lang.Boolean.parseBoolean(readString(reader, name))
        }

        @Throws(IOException::class)
        fun readInt(reader: BufferedReader?, name: String?): Int {
            return readString(reader, name)!!.toInt()
        }

        @Throws(IOException::class)
        fun readFloat(reader: BufferedReader?, name: String?): Float {
            return readString(reader, name)!!.toFloat()
        }
    }
}
