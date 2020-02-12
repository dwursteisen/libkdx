package com.badlogic.gdx.graphics.g2d

/**
 * See <a href="http://www.badlogicgames.com/wordpress/?p=1255">http://www.badlogicgames.com/wordpress/?p=1255</a>
 *
 * @author mzechner
 */
class ParticleEffect {
    private var emitters: Array<ParticleEmitter>? = null
    private var bounds: BoundingBox? = null
    private var ownsTexture = false
    protected var xSizeScale = 1f
    protected var ySizeScale = 1f
    protected var motionScale = 1f

    fun ParticleEffect() {
        emitters = Array(8)
    }

    fun ParticleEffect(effect: ParticleEffect) {
        emitters = Array(true, effect.emitters!!.size)
        var i = 0
        val n = effect.emitters!!.size
        while (i < n) {
            emitters.add(newEmitter(effect.emitters!![i]))
            i++
        }
    }

    fun start() {
        var i = 0
        val n = emitters!!.size
        while (i < n) {
            emitters!![i].start()
            i++
        }
    }

    /**
     * Resets the effect so it can be started again like a new effect. Any changes to
     * scale are reverted. See [.reset].
     */
    fun reset() {
        reset(true)
    }

    /**
     * Resets the effect so it can be started again like a new effect.
     *
     * @param resetScaling Whether to restore the original size and motion parameters if they were scaled. Repeated scaling
     * and resetting may introduce error.
     */
    fun reset(resetScaling: Boolean) {
        var i = 0
        val n = emitters!!.size
        while (i < n) {
            emitters!![i].reset()
            i++
        }
        if (resetScaling && (xSizeScale != 1f || ySizeScale != 1f || motionScale != 1f)) {
            scaleEffect(1f / xSizeScale, 1f / ySizeScale, 1f / motionScale)
            motionScale = 1f
            ySizeScale = motionScale
            xSizeScale = ySizeScale
        }
    }

    fun update(delta: Float) {
        var i = 0
        val n = emitters!!.size
        while (i < n) {
            emitters!![i].update(delta)
            i++
        }
    }

    fun draw(spriteBatch: Batch?) {
        var i = 0
        val n = emitters!!.size
        while (i < n) {
            emitters!![i].draw(spriteBatch)
            i++
        }
    }

    fun draw(spriteBatch: Batch?, delta: Float) {
        var i = 0
        val n = emitters!!.size
        while (i < n) {
            emitters!![i].draw(spriteBatch, delta)
            i++
        }
    }

    fun allowCompletion() {
        var i = 0
        val n = emitters!!.size
        while (i < n) {
            emitters!![i].allowCompletion()
            i++
        }
    }

    fun isComplete(): Boolean {
        var i = 0
        val n = emitters!!.size
        while (i < n) {
            val emitter = emitters!![i]
            if (!emitter.isComplete()) return false
            i++
        }
        return true
    }

    fun setDuration(duration: Int) {
        var i = 0
        val n = emitters!!.size
        while (i < n) {
            val emitter = emitters!![i]
            emitter.setContinuous(false)
            emitter.duration = duration.toFloat()
            emitter.durationTimer = 0f
            i++
        }
    }

    fun setPosition(x: Float, y: Float) {
        var i = 0
        val n = emitters!!.size
        while (i < n) {
            emitters!![i].setPosition(x, y)
            i++
        }
    }

    fun setFlip(flipX: Boolean, flipY: Boolean) {
        var i = 0
        val n = emitters!!.size
        while (i < n) {
            emitters!![i].setFlip(flipX, flipY)
            i++
        }
    }

    fun flipY() {
        var i = 0
        val n = emitters!!.size
        while (i < n) {
            emitters!![i].flipY()
            i++
        }
    }

    fun getEmitters(): Array<ParticleEmitter>? {
        return emitters
    }

    /**
     * Returns the emitter with the specified name, or null.
     */
    fun findEmitter(name: String): ParticleEmitter? {
        var i = 0
        val n = emitters!!.size
        while (i < n) {
            val emitter = emitters!![i]
            if (emitter.getName() == name) return emitter
            i++
        }
        return null
    }

    @Throws(IOException::class)
    fun save(output: Writer) {
        var index = 0
        var i = 0
        val n = emitters!!.size
        while (i < n) {
            val emitter = emitters!![i]
            if (index++ > 0) output.write("\n")
            emitter.save(output)
            i++
        }
    }

    fun load(effectFile: FileHandle, imagesDir: FileHandle?) {
        loadEmitters(effectFile)
        loadEmitterImages(imagesDir)
    }

    fun load(effectFile: FileHandle, atlas: TextureAtlas) {
        load(effectFile, atlas, null)
    }

    fun load(effectFile: FileHandle, atlas: TextureAtlas, atlasPrefix: String?) {
        loadEmitters(effectFile)
        loadEmitterImages(atlas, atlasPrefix)
    }

    fun loadEmitters(effectFile: FileHandle) {
        val input: InputStream = effectFile.read()
        emitters.clear()
        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(InputStreamReader(input), 512)
            while (true) {
                val emitter: ParticleEmitter = newEmitter(reader)
                emitters.add(emitter)
                if (reader.readLine() == null) break
            }
        } catch (ex: IOException) {
            throw GdxRuntimeException("Error loading effect: $effectFile", ex)
        } finally {
            StreamUtils.closeQuietly(reader)
        }
    }

    fun loadEmitterImages(atlas: TextureAtlas) {
        loadEmitterImages(atlas, null)
    }

    fun loadEmitterImages(atlas: TextureAtlas, atlasPrefix: String?) {
        var i = 0
        val n = emitters!!.size
        while (i < n) {
            val emitter = emitters!![i]
            if (emitter.getImagePaths().size == 0) {
                i++
                continue
            }
            val sprites = Array<Sprite?>()
            for (imagePath in emitter.getImagePaths()) {
                var imageName: String = File(imagePath.replace('\\', '/')).getName()
                val lastDotIndex = imageName.lastIndexOf('.')
                if (lastDotIndex != -1) imageName = imageName.substring(0, lastDotIndex)
                if (atlasPrefix != null) imageName = atlasPrefix + imageName
                val sprite = atlas.createSprite(imageName)
                    ?: throw java.lang.IllegalArgumentException("SpriteSheet missing image: $imageName")
                sprites.add(sprite)
            }
            emitter.setSprites(sprites)
            i++
        }
    }

    fun loadEmitterImages(imagesDir: FileHandle) {
        ownsTexture = true
        val loadedSprites: ObjectMap<String, Sprite> = ObjectMap<String, Sprite>(emitters!!.size)
        var i = 0
        val n = emitters!!.size
        while (i < n) {
            val emitter = emitters!![i]
            if (emitter.getImagePaths().size == 0) {
                i++
                continue
            }
            val sprites = Array<Sprite?>()
            for (imagePath in emitter.getImagePaths()) {
                val imageName: String = File(imagePath.replace('\\', '/')).getName()
                var sprite: Sprite? = loadedSprites.get(imageName)
                if (sprite == null) {
                    sprite = Sprite(loadTexture(imagesDir.child(imageName)))
                    loadedSprites.put(imageName, sprite)
                }
                sprites.add(sprite)
            }
            emitter.setSprites(sprites)
            i++
        }
    }

    @Throws(IOException::class)
    protected fun newEmitter(reader: BufferedReader?): ParticleEmitter? {
        return ParticleEmitter(reader)
    }

    protected fun newEmitter(emitter: ParticleEmitter?): ParticleEmitter? {
        return ParticleEmitter(emitter)
    }

    protected fun loadTexture(file: FileHandle?): Texture? {
        return Texture(file, false)
    }

    /**
     * Disposes the texture for each sprite for each ParticleEmitter.
     */
    fun dispose() {
        if (!ownsTexture) return
        var i = 0
        val n = emitters!!.size
        while (i < n) {
            val emitter = emitters!![i]
            for (sprite in emitter.getSprites()!!) {
                sprite.getTexture().dispose()
            }
            i++
        }
    }

    /**
     * Returns the bounding box for all active particles. z axis will always be zero.
     */
    fun getBoundingBox(): BoundingBox? {
        if (bounds == null) bounds = BoundingBox()
        val bounds: BoundingBox? = bounds
        bounds.inf()
        for (emitter in emitters!!) bounds.ext(emitter.getBoundingBox())
        return bounds
    }

    /**
     * Permanently scales all the size and motion parameters of all the emitters in this effect. If this effect originated from a
     * [ParticleEffectPool], the scale will be reset when it is returned to the pool.
     */
    fun scaleEffect(scaleFactor: Float) {
        scaleEffect(scaleFactor, scaleFactor, scaleFactor)
    }

    /**
     * Permanently scales all the size and motion parameters of all the emitters in this effect. If this effect originated from a
     * [ParticleEffectPool], the scale will be reset when it is returned to the pool.
     */
    fun scaleEffect(scaleFactor: Float, motionScaleFactor: Float) {
        scaleEffect(scaleFactor, scaleFactor, motionScaleFactor)
    }

    /**
     * Permanently scales all the size and motion parameters of all the emitters in this effect. If this effect originated from a
     * [ParticleEffectPool], the scale will be reset when it is returned to the pool.
     */
    fun scaleEffect(xSizeScaleFactor: Float, ySizeScaleFactor: Float, motionScaleFactor: Float) {
        xSizeScale *= xSizeScaleFactor
        ySizeScale *= ySizeScaleFactor
        motionScale *= motionScaleFactor
        for (particleEmitter in emitters!!) {
            particleEmitter.scaleSize(xSizeScaleFactor, ySizeScaleFactor)
            particleEmitter.scaleMotion(motionScaleFactor)
        }
    }

    /**
     * Sets the [cleansUpBlendFunction][com.badlogic.gdx.graphics.g2d.ParticleEmitter.setCleansUpBlendFunction]
     * parameter on all [ParticleEmitters][com.badlogic.gdx.graphics.g2d.ParticleEmitter] currently in this ParticleEffect.
     *
     *
     * IMPORTANT: If set to false and if the next object to use this Batch expects alpha blending, you are responsible for setting
     * the Batch's blend function to (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA) before that next object is drawn.
     *
     * @param cleanUpBlendFunction
     */
    fun setEmittersCleanUpBlendFunction(cleanUpBlendFunction: Boolean) {
        var i = 0
        val n = emitters!!.size
        while (i < n) {
            emitters!![i].setCleansUpBlendFunction(cleanUpBlendFunction)
            i++
        }
    }
}
