package com.badlogic.gdx.assets

import com.badlogic.gdx.assets.loaders.AssetLoader
import com.badlogic.gdx.assets.loaders.BitmapFontLoader
import com.badlogic.gdx.assets.loaders.CubemapLoader
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.assets.loaders.I18NBundleLoader
import com.badlogic.gdx.assets.loaders.MusicLoader
import com.badlogic.gdx.assets.loaders.ParticleEffectLoader
import com.badlogic.gdx.assets.loaders.PixmapLoader
import com.badlogic.gdx.assets.loaders.ShaderProgramLoader
import com.badlogic.gdx.assets.loaders.SkinLoader
import com.badlogic.gdx.assets.loaders.SoundLoader
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader
import com.badlogic.gdx.assets.loaders.TextureLoader
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import kotlin.jvm.Synchronized
import kotlin.reflect.KClass

class AssetManager : Disposable {
    val assets: ObjectMap<java.lang.Class, ObjectMap<String, RefCountedContainer>> = ObjectMap()
    val assetTypes: ObjectMap<String, java.lang.Class> = ObjectMap()
    val assetDependencies: ObjectMap<String, Array<String>> = ObjectMap()
    val injected: ObjectSet<String> = ObjectSet()

    val loaders: ObjectMap<java.lang.Class, ObjectMap<String, AssetLoader>> = ObjectMap()
    val loadQueue: Array<AssetDescriptor<*>> = Array()
    var executor: AsyncExecutor? = null

    val tasks: Stack<AssetLoadingTask> = Stack()
    var listener: AssetErrorListener? = null
    var loaded = 0
    var toLoad = 0
    var peakTasks = 0

    val resolver: FileHandleResolver? = null

    var log: Logger = Logger("AssetManager", Application.LOG_NONE)

    /**
     * Creates a new AssetManager with all default loaders.
     */
    fun AssetManager() {
        this(InternalFileHandleResolver())
    }

    /**
     * Creates a new AssetManager with all default loaders.
     */
    fun AssetManager(resolver: FileHandleResolver?) {
        this(resolver, true)
    }

    /**
     * Creates a new AssetManager with optionally all default loaders. If you don't add the default loaders then you do have to
     * manually add the loaders you need, including any loaders they might depend on.
     *
     * @param defaultLoaders whether to add the default loaders
     */
    fun AssetManager(resolver: FileHandleResolver?, defaultLoaders: Boolean) {
        this.resolver = resolver
        if (defaultLoaders) {
            setLoader<T, P>(BitmapFont::class.java, BitmapFontLoader(resolver))
            setLoader<T, P>(Music::class.java, MusicLoader(resolver))
            setLoader<T, P>(Pixmap::class.java, PixmapLoader(resolver))
            setLoader<T, P>(Sound::class.java, SoundLoader(resolver))
            setLoader<T, P>(TextureAtlas::class.java, TextureAtlasLoader(resolver))
            setLoader<T, P>(Texture::class.java, TextureLoader(resolver!!))
            setLoader<T, P>(Skin::class.java, SkinLoader(resolver))
            setLoader<T, P>(ParticleEffect::class.java, ParticleEffectLoader(resolver))
            setLoader<T, P>(ParticleEffect::class.java,
                com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader(resolver))
            setLoader<T, P>(PolygonRegion::class.java, PolygonRegionLoader(resolver))
            setLoader<T, P>(I18NBundle::class.java, I18NBundleLoader(resolver))
            setLoader<T, P>(Model::class.java, ".g3dj", G3dModelLoader(JsonReader(), resolver))
            setLoader<T, P>(Model::class.java, ".g3db", G3dModelLoader(UBJsonReader(), resolver))
            setLoader<T, P>(Model::class.java, ".obj", ObjLoader(resolver))
            setLoader<T, P>(ShaderProgram::class.java, ShaderProgramLoader(resolver))
            setLoader<T, P>(Cubemap::class.java, CubemapLoader(resolver))
        }
        executor = AsyncExecutor(1, "AssetManager")
    }

    /**
     * Returns the [FileHandleResolver] for which this AssetManager was loaded with.
     *
     * @return the file handle resolver which this AssetManager uses
     */
    fun getFileHandleResolver(): FileHandleResolver? {
        return resolver
    }

    /**
     * @param fileName the asset file name
     * @return the asset
     */
    @Synchronized
    operator fun <T> get(fileName: String?): T? {
        val type: java.lang.Class<T?> = assetTypes.get(fileName)
            ?: throw GdxRuntimeException("Asset not loaded: $fileName")
        val assetsByType: ObjectMap<String?, RefCountedContainer?> = assets.get(type)
            ?: throw GdxRuntimeException("Asset not loaded: $fileName")
        val assetContainer: RefCountedContainer = assetsByType.get(fileName)
            ?: throw GdxRuntimeException("Asset not loaded: $fileName")
        return assetContainer.getObject(type) ?: throw GdxRuntimeException("Asset not loaded: $fileName")
    }

    /**
     * @param fileName the asset file name
     * @param type     the asset type
     * @return the asset
     */
    @Synchronized
    operator fun <T> get(fileName: String?, type: java.lang.Class<T?>?): T? {
        val assetsByType: ObjectMap<String?, RefCountedContainer?> = assets.get(type)
            ?: throw GdxRuntimeException("Asset not loaded: $fileName")
        val assetContainer: RefCountedContainer = assetsByType.get(fileName)
            ?: throw GdxRuntimeException("Asset not loaded: $fileName")
        return assetContainer.getObject(type) ?: throw GdxRuntimeException("Asset not loaded: $fileName")
    }

    /**
     * @param type the asset type
     * @return all the assets matching the specified type
     */
    @Synchronized
    fun <T> getAll(type: java.lang.Class<T?>?, out: Array<T?>?): Array<T?>? {
        val assetsByType: ObjectMap<String?, RefCountedContainer?> = assets.get(type)
        if (assetsByType != null) {
            for (asset in assetsByType.entries()) {
                out.add(asset.value.getObject(type))
            }
        }
        return out
    }

    /**
     * @param assetDescriptor the asset descriptor
     * @return the asset
     */
    @Synchronized
    operator fun <T> get(assetDescriptor: AssetDescriptor<T?>?): T? {
        return get(assetDescriptor!!.fileName, assetDescriptor.type)
    }

    /**
     * Returns true if an asset with the specified name is loading, queued to be loaded, or has been loaded.
     */
    @Synchronized
    operator fun contains(fileName: String?): Boolean {
        if (tasks.size() > 0 && tasks.firstElement().assetDesc.fileName.equals(fileName)) return true
        for (i in 0 until loadQueue.size) if (loadQueue[i].fileName.equals(fileName)) return true
        return isLoaded(fileName)
    }

    /**
     * Returns true if an asset with the specified name and type is loading, queued to be loaded, or has been loaded.
     */
    @Synchronized
    fun contains(fileName: String?, type: java.lang.Class?): Boolean {
        if (tasks.size() > 0) {
            val assetDesc: AssetDescriptor<*> = tasks.firstElement().assetDesc
            if (assetDesc.type == type && assetDesc.fileName == fileName) return true
        }
        for (i in 0 until loadQueue.size) {
            val assetDesc = loadQueue[i]
            if (assetDesc.type == type && assetDesc.fileName == fileName) return true
        }
        return isLoaded(fileName, type)
    }

    /**
     * Removes the asset and all its dependencies, if they are not used by other assets.
     *
     * @param fileName the file name
     */
    @Synchronized
    fun unload(fileName: String?) {
        // check if it's currently processed (and the first element in the stack, thus not a dependency)
        // and cancel if necessary
        if (tasks.size() > 0) {
            val currAsset: AssetLoadingTask = tasks.firstElement()
            if (currAsset.assetDesc!!.fileName == fileName) {
                currAsset.cancel = true
                log.info("Unload (from tasks): $fileName")
                return
            }
        }

        // check if it's in the queue
        var foundIndex = -1
        for (i in 0 until loadQueue.size) {
            if (loadQueue[i].fileName.equals(fileName)) {
                foundIndex = i
                break
            }
        }
        if (foundIndex != -1) {
            toLoad--
            loadQueue.removeIndex(foundIndex)
            log.info("Unload (from queue): $fileName")
            return
        }

        // get the asset and its type
        val type: java.lang.Class = assetTypes.get(fileName) ?: throw GdxRuntimeException("Asset not loaded: $fileName")
        val assetRef: RefCountedContainer = assets.get(type).get(fileName)

        // if it is reference counted, decrement ref count and check if we can really get rid of it.
        assetRef.decRefCount()
        if (assetRef.refCount <= 0) {
            log.info("Unload (dispose): $fileName")

            // if it is disposable dispose it
            if (assetRef.getObject<Any?>(Any::class.java) is Disposable) (assetRef.getObject<Any?>(Any::class.java) as Disposable?)!!.dispose()

            // remove the asset from the manager.
            assetTypes.remove(fileName)
            assets.get(type).remove(fileName)
        } else {
            log.info("Unload (decrement): $fileName")
        }

        // remove any dependencies (or just decrement their ref count).
        val dependencies: Array<String?> = assetDependencies.get(fileName)
        if (dependencies != null) {
            for (dependency in dependencies) {
                if (isLoaded(dependency)) unload(dependency)
            }
        }
        // remove dependencies if ref count < 0
        if (assetRef.refCount <= 0) {
            assetDependencies.remove(fileName)
        }
    }

    /**
     * @param asset the asset
     * @return whether the asset is contained in this manager
     */
    @Synchronized
    fun <T> containsAsset(asset: T?): Boolean {
        val assetsByType: ObjectMap<String?, RefCountedContainer?> = assets.get(asset.javaClass) ?: return false
        for (fileName in assetsByType.keys()) {
            val otherAsset: T? = assetsByType.get(fileName).getObject(Any::class.java)
            if (otherAsset === asset || asset == otherAsset) return true
        }
        return false
    }

    /**
     * @param asset the asset
     * @return the filename of the asset or null
     */
    @Synchronized
    fun <T> getAssetFileName(asset: T?): String? {
        for (assetType in assets.keys()) {
            val assetsByType: ObjectMap<String?, RefCountedContainer?> = assets.get(assetType)
            for (fileName in assetsByType.keys()) {
                val otherAsset: T? = assetsByType.get(fileName).getObject(Any::class.java)
                if (otherAsset === asset || asset == otherAsset) return fileName
            }
        }
        return null
    }

    /**
     * @param assetDesc the AssetDescriptor of the asset
     * @return whether the asset is loaded
     */
    @Synchronized
    fun isLoaded(assetDesc: AssetDescriptor<*>?): Boolean {
        return isLoaded(assetDesc!!.fileName)
    }

    /**
     * @param fileName the file name of the asset
     * @return whether the asset is loaded
     */
    @Synchronized
    fun isLoaded(fileName: String?): Boolean {
        return if (fileName == null) false else assetTypes.containsKey(fileName)
    }

    /**
     * @param fileName the file name of the asset
     * @return whether the asset is loaded
     */
    @Synchronized
    fun isLoaded(fileName: String?, type: java.lang.Class?): Boolean {
        val assetsByType: ObjectMap<String?, RefCountedContainer?> = assets.get(type) ?: return false
        val assetContainer: RefCountedContainer = assetsByType.get(fileName) ?: return false
        return assetContainer.getObject<Any?>(type) != null
    }

    /**
     * Returns the default loader for the given type
     *
     * @param type The type of the loader to get
     * @return The loader capable of loading the type, or null if none exists
     */
    fun <T> getLoader(type: java.lang.Class<T?>?): AssetLoader? {
        return getLoader(type, null)
    }

    /**
     * Returns the loader for the given type and the specified filename. If no loader exists for the specific filename, the
     * default loader for that type is returned.
     *
     * @param type     The type of the loader to get
     * @param fileName The filename of the asset to get a loader for, or null to get the default loader
     * @return The loader capable of loading the type and filename, or null if none exists
     */
    fun <T> getLoader(type: java.lang.Class<T?>?, fileName: String?): AssetLoader? {
        val loaders: ObjectMap<String?, AssetLoader?> = loaders.get(type)
        if (loaders == null || loaders.size < 1) return null
        if (fileName == null) return loaders.get("")
        var result: AssetLoader? = null
        var l = -1
        for (entry in loaders.entries()) {
            if (entry.key.length() > l && fileName.endsWith(entry.key)) {
                result = entry.value
                l = entry.key.length()
            }
        }
        return result
    }

    /**
     * Adds the given asset to the loading queue of the AssetManager.
     *
     * @param fileName the file name (interpretation depends on [AssetLoader])
     * @param type     the type of the asset.
     */
    @Synchronized
    fun <T> load(fileName: String?, type: java.lang.Class<T?>?) {
        load(fileName, type, null)
    }

    /**
     * Adds the given asset to the loading queue of the AssetManager.
     *
     * @param fileName  the file name (interpretation depends on [AssetLoader])
     * @param type      the type of the asset.
     * @param parameter parameters for the AssetLoader.
     */
    @Synchronized
    fun <T> load(fileName: String?, type: KClass<T>, parameter: AssetLoaderParameters<T>?) {
        val loader: AssetLoader = getLoader(type, fileName)
            ?: throw GdxRuntimeException("No loader for type: " + ClassReflection.getSimpleName(type))

        // reset stats
        if (loadQueue.size === 0) {
            loaded = 0
            toLoad = 0
            peakTasks = 0
        }

        // check if an asset with the same name but a different type has already been added.

        // check preload queue
        for (i in 0 until loadQueue.size) {
            val desc = loadQueue[i]
            if (desc.fileName == fileName && desc.type != type) throw GdxRuntimeException(
                "Asset with name '" + fileName + "' already in preload queue, but has different type (expected: "
                    + ClassReflection.getSimpleName(type) + ", found: " + ClassReflection.getSimpleName(desc.type) + ")")
        }

        // check task list
        for (i in 0 until tasks.size()) {
            val desc: AssetDescriptor<*> = tasks.get(i).assetDesc
            if (desc.fileName == fileName && desc.type != type) throw GdxRuntimeException(
                "Asset with name '" + fileName + "' already in task list, but has different type (expected: "
                    + ClassReflection.getSimpleName(type) + ", found: " + ClassReflection.getSimpleName(desc.type) + ")")
        }

        // check loaded assets
        val otherType: java.lang.Class = assetTypes.get(fileName)
        if (otherType != null && otherType != type) throw GdxRuntimeException("Asset with name '" + fileName + "' already loaded, but has different type (expected: "
            + ClassReflection.getSimpleName(type) + ", found: " + ClassReflection.getSimpleName(otherType) + ")")
        toLoad++
        val assetDesc: AssetDescriptor<*> = AssetDescriptor<Any?>(fileName, type, parameter)
        loadQueue.add(assetDesc)
        log.debug("Queued: $assetDesc")
    }

    /**
     * Adds the given asset to the loading queue of the AssetManager.
     *
     * @param desc the [AssetDescriptor]
     */
    @Synchronized
    fun load(desc: AssetDescriptor<*>?) {
        load<Any?>(desc!!.fileName, desc.type, desc.params)
    }

    /**
     * Updates the AssetManager, keeping it loading any assets in the preload queue.
     *
     * @return true if all loading is finished.
     */
    @Synchronized
    fun update(): Boolean {
        return try {
            if (tasks.size() === 0) {
                // loop until we have a new task ready to be processed
                while (loadQueue.size !== 0 && tasks.size() === 0) {
                    nextTask()
                }
                // have we not found a task? We are done!
                if (tasks.size() === 0) return true
            }
            updateTask() && loadQueue.size === 0 && tasks.size() === 0
        } catch (t: Throwable) {
            handleTaskError(t)
            loadQueue.size === 0
        }
    }

    /**
     * Updates the AssetManager continuously for the specified number of milliseconds, yielding the CPU to the loading thread
     * between updates. This may block for less time if all loading tasks are complete. This may block for more time if the portion
     * of a single task that happens in the GL thread takes a long time.
     *
     * @return true if all loading is finished.
     */
    fun update(millis: Int): Boolean {
        val endTime: Long = TimeUtils.millis() + millis
        while (true) {
            val done = update()
            if (done || TimeUtils.millis() > endTime) return done
            ThreadUtils.yield()
        }
    }

    /**
     * Returns true when all assets are loaded. Can be called from any thread.
     */
    @Synchronized
    fun isFinished(): Boolean {
        return loadQueue.size === 0 && tasks.size() === 0
    }

    /**
     * Blocks until all assets are loaded.
     */
    fun finishLoading() {
        log.debug("Waiting for loading to complete...")
        while (!update()) ThreadUtils.yield()
        log.debug("Loading complete.")
    }

    /**
     * Blocks until the specified asset is loaded.
     *
     * @param assetDesc the AssetDescriptor of the asset
     */
    fun <T> finishLoadingAsset(assetDesc: AssetDescriptor<*>?): T? {
        return finishLoadingAsset(assetDesc!!.fileName)
    }

    /**
     * Blocks until the specified asset is loaded.
     *
     * @param fileName the file name (interpretation depends on [AssetLoader])
     */
    fun <T> finishLoadingAsset(fileName: String?): T? {
        log.debug("Waiting for asset to be loaded: $fileName")
        while (true) {
            synchronized(this) {
                val type: java.lang.Class<T?> = assetTypes.get(fileName)
                if (type != null) {
                    val assetsByType: ObjectMap<String?, RefCountedContainer?> = assets.get(type)
                    if (assetsByType != null) {
                        val assetContainer: RefCountedContainer = assetsByType.get(fileName)
                        if (assetContainer != null) {
                            val asset: T = assetContainer.getObject(type)
                            if (asset != null) {
                                log.debug("Asset loaded: $fileName")
                                return asset
                            }
                        }
                    }
                }
                update()
            }
            ThreadUtils.yield()
        }
    }

    @Synchronized
    fun injectDependencies(parentAssetFilename: String?, dependendAssetDescs: Array<AssetDescriptor<*>?>?) {
        val injected: ObjectSet<String?> = injected
        for (desc in dependendAssetDescs!!) {
            if (injected.contains(desc!!.fileName)) continue  // Ignore subsequent dependencies if there are duplicates.
            injected.add(desc.fileName)
            injectDependency(parentAssetFilename, desc)
        }
        injected.clear(32)
    }

    @Synchronized
    private fun injectDependency(parentAssetFilename: String?, dependendAssetDesc: AssetDescriptor<*>?) {
        // add the asset as a dependency of the parent asset
        var dependencies: Array<String?>? = assetDependencies.get(parentAssetFilename)
        if (dependencies == null) {
            dependencies = Array()
            assetDependencies.put(parentAssetFilename, dependencies)
        }
        dependencies.add(dependendAssetDesc!!.fileName)

        // if the asset is already loaded, increase its reference count.
        if (isLoaded(dependendAssetDesc!!.fileName)) {
            log.debug("Dependency already loaded: $dependendAssetDesc")
            val type: java.lang.Class = assetTypes.get(dependendAssetDesc.fileName)
            val assetRef: RefCountedContainer = assets.get(type).get(dependendAssetDesc.fileName)
            assetRef.incRefCount()
            incrementRefCountedDependencies(dependendAssetDesc.fileName)
        } else {
            log.info("Loading dependency: $dependendAssetDesc")
            addTask(dependendAssetDesc)
        }
    }

    /**
     * Removes a task from the loadQueue and adds it to the task stack. If the asset is already loaded (which can happen if it was
     * a dependency of a previously loaded asset) its reference count will be increased.
     */
    private fun nextTask() {
        val assetDesc: AssetDescriptor<*> = loadQueue.removeIndex(0)

        // if the asset not meant to be reloaded and is already loaded, increase its reference count
        if (isLoaded(assetDesc.fileName)) {
            log.debug("Already loaded: $assetDesc")
            val type: java.lang.Class = assetTypes.get(assetDesc.fileName)
            val assetRef: RefCountedContainer = assets.get(type).get(assetDesc.fileName)
            assetRef.incRefCount()
            incrementRefCountedDependencies(assetDesc.fileName)
            if (assetDesc.params != null && assetDesc.params.loadedCallback != null) {
                assetDesc.params.loadedCallback!!.finishedLoading(this, assetDesc.fileName, assetDesc.type)
            }
            loaded++
        } else {
            // else add a new task for the asset.
            log.info("Loading: $assetDesc")
            addTask(assetDesc)
        }
    }

    /**
     * Adds a [AssetLoadingTask] to the task stack for the given asset.
     *
     * @param assetDesc
     */
    private fun addTask(assetDesc: AssetDescriptor<*>?) {
        val loader: AssetLoader = getLoader<Any?>(assetDesc!!.type, assetDesc.fileName)
            ?: throw GdxRuntimeException("No loader for type: " + ClassReflection.getSimpleName(assetDesc.type))
        tasks.push(AssetLoadingTask(this, assetDesc, loader, executor))
        peakTasks++
    }

    /**
     * Adds an asset to this AssetManager
     */
    protected fun <T> addAsset(fileName: String?, type: java.lang.Class<T?>?, asset: T?) {
        // add the asset to the filename lookup
        assetTypes.put(fileName, type)

        // add the asset to the type lookup
        var typeToAssets: ObjectMap<String?, RefCountedContainer?>? = assets.get(type)
        if (typeToAssets == null) {
            typeToAssets = ObjectMap<String?, RefCountedContainer?>()
            assets.put(type, typeToAssets)
        }
        typeToAssets.put(fileName, RefCountedContainer(asset))
    }

    /**
     * Updates the current task on the top of the task stack.
     *
     * @return true if the asset is loaded or the task was cancelled.
     */
    private fun updateTask(): Boolean {
        val task: AssetLoadingTask = tasks.peek()
        var complete = true
        try {
            complete = task.cancel || task.update()
        } catch (ex: RuntimeException) {
            task.cancel = true
            taskFailed(task.assetDesc, ex)
        }

        // if the task has been cancelled or has finished loading
        if (complete) {
            // increase the number of loaded assets and pop the task from the stack
            if (tasks.size() === 1) {
                loaded++
                peakTasks = 0
            }
            tasks.pop()
            if (task.cancel) return true
            addAsset<Any?>(task.assetDesc!!.fileName, task.assetDesc!!.type, task.getAsset())

            // otherwise, if a listener was found in the parameter invoke it
            if (task.assetDesc!!.params != null && task.assetDesc!!.params.loadedCallback != null) {
                task.assetDesc!!.params.loadedCallback!!.finishedLoading(this, task.assetDesc!!.fileName, task.assetDesc!!.type)
            }
            val endTime: Long = TimeUtils.nanoTime()
            log.debug("Loaded: " + (endTime - task.startTime) / 1000000f + "ms " + task.assetDesc)
            return true
        }
        return false
    }

    /**
     * Called when a task throws an exception during loading. The default implementation rethrows the exception. A subclass may
     * supress the default implementation when loading assets where loading failure is recoverable.
     */
    protected fun taskFailed(assetDesc: AssetDescriptor<*>?, ex: RuntimeException?) {
        throw ex!!
    }

    private fun incrementRefCountedDependencies(parent: String?) {
        val dependencies: Array<String?> = assetDependencies.get(parent) ?: return
        for (dependency in dependencies) {
            val type: java.lang.Class = assetTypes.get(dependency)
            val assetRef: RefCountedContainer = assets.get(type).get(dependency)
            assetRef.incRefCount()
            incrementRefCountedDependencies(dependency)
        }
    }

    /**
     * Handles a runtime/loading error in [.update] by optionally invoking the [AssetErrorListener].
     *
     * @param t
     */
    private fun handleTaskError(t: Throwable?) {
        log.error("Error loading asset.", t)
        if (tasks.isEmpty()) throw GdxRuntimeException(t)

        // pop the faulty task from the stack
        val task: AssetLoadingTask = tasks.pop()
        val assetDesc = task.assetDesc

        // remove all dependencies
        if (task.dependenciesLoaded && task.dependencies != null) {
            for (desc in task.dependencies!!) {
                unload(desc!!.fileName)
            }
        }

        // clear the rest of the stack
        tasks.clear()

        // inform the listener that something bad happened
        if (listener != null) {
            listener!!.error(assetDesc, t)
        } else {
            throw GdxRuntimeException(t)
        }
    }

    /**
     * Sets a new [AssetLoader] for the given type.
     *
     * @param type   the type of the asset
     * @param loader the loader
     */
    @Synchronized
    fun <T, P : AssetLoaderParameters<T?>?> setLoader(type: java.lang.Class<T?>?, loader: AssetLoader<T?, P?>?) {
        setLoader(type, null, loader)
    }

    /**
     * Sets a new [AssetLoader] for the given type.
     *
     * @param type   the type of the asset
     * @param suffix the suffix the filename must have for this loader to be used or null to specify the default loader.
     * @param loader the loader
     */
    @Synchronized
    fun <T, P : AssetLoaderParameters<T?>?> setLoader(type: java.lang.Class<T?>?, suffix: String?,
                                                      loader: AssetLoader<T?, P?>?) {
        if (type == null) throw java.lang.IllegalArgumentException("type cannot be null.")
        if (loader == null) throw java.lang.IllegalArgumentException("loader cannot be null.")
        log.debug("Loader set: " + ClassReflection.getSimpleName(type).toString() + " -> " + ClassReflection.getSimpleName(loader.getClass()))
        var loaders: ObjectMap<String?, AssetLoader?>? = loaders.get(type)
        if (loaders == null) this.loaders.put(type, ObjectMap<String?, AssetLoader?>().also({ loaders = it }))
        loaders.put(suffix ?: "", loader)
    }

    /**
     * @return the number of loaded assets
     */
    @Synchronized
    fun getLoadedAssets(): Int {
        return assetTypes.size
    }

    /**
     * @return the number of currently queued assets
     */
    @Synchronized
    fun getQueuedAssets(): Int {
        return loadQueue.size + tasks.size()
    }

    /**
     * @return the progress in percent of completion.
     */
    @Synchronized
    fun getProgress(): Float {
        if (toLoad == 0) return 1
        var fractionalLoaded = loaded.toFloat()
        if (peakTasks > 0) {
            fractionalLoaded += (peakTasks - tasks.size()) / peakTasks.toFloat()
        }
        return java.lang.Math.min(1f, fractionalLoaded / toLoad.toFloat())
    }

    /**
     * Sets an [AssetErrorListener] to be invoked in case loading an asset failed.
     *
     * @param listener the listener or null
     */
    @Synchronized
    fun setErrorListener(listener: AssetErrorListener?) {
        this.listener = listener
    }

    /**
     * Disposes all assets in the manager and stops all asynchronous loading.
     */
    @Synchronized
    override fun dispose() {
        log.debug("Disposing.")
        clear()
        executor.dispose()
    }

    /**
     * Clears and disposes all assets and the preloading queue.
     */
    @Synchronized
    fun clear() {
        loadQueue.clear()
        while (!update());
        val dependencyCount: ObjectIntMap<String?> = ObjectIntMap<String?>()
        while (assetTypes.size > 0) {
            // for each asset, figure out how often it was referenced
            dependencyCount.clear()
            val assets: Array<String?> = assetTypes.keys().toArray()
            for (asset in assets) {
                dependencyCount.put(asset, 0)
            }
            for (asset in assets) {
                val dependencies: Array<String?> = assetDependencies.get(asset) ?: continue
                for (dependency in dependencies) {
                    var count: Int = dependencyCount.get(dependency, 0)
                    count++
                    dependencyCount.put(dependency, count)
                }
            }

            // only dispose of assets that are root assets (not referenced)
            for (asset in assets) {
                if (dependencyCount.get(asset, 0) === 0) {
                    unload(asset)
                }
            }
        }
        assets.clear()
        assetTypes.clear()
        assetDependencies.clear()
        loaded = 0
        toLoad = 0
        peakTasks = 0
        loadQueue.clear()
        tasks.clear()
    }

    /**
     * @return the [Logger] used by the [AssetManager]
     */
    fun getLogger(): Logger? {
        return log
    }

    fun setLogger(logger: Logger?) {
        log = logger
    }

    /**
     * Returns the reference count of an asset.
     *
     * @param fileName
     */
    @Synchronized
    fun getReferenceCount(fileName: String?): Int {
        val type: java.lang.Class = assetTypes.get(fileName) ?: throw GdxRuntimeException("Asset not loaded: $fileName")
        return assets.get(type).get(fileName).refCount
    }

    /**
     * Sets the reference count of an asset.
     *
     * @param fileName
     */
    @Synchronized
    fun setReferenceCount(fileName: String?, refCount: Int) {
        val type: java.lang.Class = assetTypes.get(fileName) ?: throw GdxRuntimeException("Asset not loaded: $fileName")
        assets.get(type).get(fileName).refCount = refCount
    }

    /**
     * @return a string containing ref count and dependency information for all assets.
     */
    @Synchronized
    fun getDiagnostics(): String? {
        val sb: java.lang.StringBuilder = java.lang.StringBuilder(256)
        for (fileName in assetTypes.keys()) {
            if (sb.length > 0) sb.append("\n")
            sb.append(fileName)
            sb.append(", ")
            val type: java.lang.Class = assetTypes.get(fileName)
            val assetRef: RefCountedContainer = assets.get(type).get(fileName)
            val dependencies: Array<String?> = assetDependencies.get(fileName)
            sb.append(ClassReflection.getSimpleName(type))
            sb.append(", refs: ")
            sb.append(assetRef.refCount)
            if (dependencies != null) {
                sb.append(", deps: [")
                for (dep in dependencies) {
                    sb.append(dep)
                    sb.append(",")
                }
                sb.append("]")
            }
        }
        return sb.toString()
    }

    /**
     * @return the file names of all loaded assets.
     */
    @Synchronized
    fun getAssetNames(): Array<String?>? {
        return assetTypes.keys().toArray()
    }

    /**
     * @return the dependencies of an asset or null if the asset has no dependencies.
     */
    @Synchronized
    fun getDependencies(fileName: String?): Array<String?>? {
        return assetDependencies.get(fileName)
    }

    /**
     * @return the type of a loaded asset.
     */
    @Synchronized
    fun getAssetType(fileName: String?): java.lang.Class? {
        return assetTypes.get(fileName)
    }
}
