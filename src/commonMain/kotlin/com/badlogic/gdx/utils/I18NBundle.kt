package com.badlogic.gdx.utils


/**
 * A {@code I18NBundle} provides {@code Locale}-specific resources loaded from property files. A bundle contains a number of named
 * resources, whose names and values are {@code Strings}. A bundle may have a parent bundle, and when a resource is not found in a
 * bundle, the parent bundle is searched for the resource. If the fallback mechanism reaches the base bundle and still can't find
 * the resource it throws a {@code MissingResourceException}.
 *
 * <ul>
 * <li>All bundles for the same group of resources share a common base bundle. This base bundle acts as the root and is the last
 * fallback in case none of its children was able to respond to a request.</li>
 * <li>The first level contains changes between different languages. Only the differences between a language and the language of
 * the base bundle need to be handled by a language-specific {@code I18NBundle}.</li>
 * <li>The second level contains changes between different countries that use the same language. Only the differences between a
 * country and the country of the language bundle need to be handled by a country-specific {@code I18NBundle}.</li>
 * <li>The third level contains changes that don't have a geographic reason (e.g. changes that where made at some point in time
 * like {@code PREEURO} where the currency of come countries changed. The country bundle would return the current currency (Euro)
 * and the {@code PREEURO} variant bundle would return the old currency (e.g. DM for Germany).</li>
 * </ul>
 *
 * <strong>Examples</strong>
 * <ul>
 * <li>BaseName (base bundle)
 * <li>BaseName_de (german language bundle)
 * <li>BaseName_fr (french language bundle)
 * <li>BaseName_de_DE (bundle with Germany specific resources in german)
 * <li>BaseName_de_CH (bundle with Switzerland specific resources in german)
 * <li>BaseName_fr_CH (bundle with Switzerland specific resources in french)
 * <li>BaseName_de_DE_PREEURO (bundle with Germany specific resources in german of the time before the Euro)
 * <li>BaseName_fr_FR_PREEURO (bundle with France specific resources in french of the time before the Euro)
 * </ul>
 * <p>
 * It's also possible to create variants for languages or countries. This can be done by just skipping the country or language
 * abbreviation: BaseName_us__POSIX or BaseName__DE_PREEURO. But it's not allowed to circumvent both language and country:
 * BaseName___VARIANT is illegal.
 *
 * @author davebaol
 * @see PropertiesUtils
 */
class I18NBundle {
    private val DEFAULT_ENCODING = "UTF-8"

    // Locale.ROOT does not exist in Android API level 8
    private val ROOT_LOCALE: Locale = Locale("", "", "")

    private var simpleFormatter = false
    private var exceptionOnMissingKey = true

    /**
     * The parent of this `I18NBundle` that is used if this bundle doesn't include the requested resource.
     */
    private var parent: I18NBundle? = null

    /**
     * The locale for this bundle.
     */
    private var locale: Locale? = null

    /**
     * The properties for this bundle.
     */
    private var properties: ObjectMap<String?, String?>? = null

    /**
     * The formatter used for argument replacement.
     */
    private var formatter: TextFormatter? = null

    /**
     * Returns the flag indicating whether to use the simplified message pattern syntax (default is false). This flag is always
     * assumed to be true on GWT backend.
     */
    fun getSimpleFormatter(): Boolean {
        return simpleFormatter
    }

    /**
     * Sets the flag indicating whether to use the simplified message pattern. The flag must be set before calling the factory
     * methods `createBundle`. Notice that this method has no effect on the GWT backend where it's always assumed to be true.
     */
    fun setSimpleFormatter(enabled: Boolean) {
        simpleFormatter = enabled
    }

    /**
     * Returns the flag indicating whether to throw a [MissingResourceException] from the [get(key)][.get]
     * method if no string for the given key can be found. If this flag is `false` the missing key surrounded by `???`
     * is returned.
     */
    fun getExceptionOnMissingKey(): Boolean {
        return exceptionOnMissingKey
    }

    /**
     * Sets the flag indicating whether to throw a [MissingResourceException] from the [get(key)][.get] method
     * if no string for the given key can be found. If this flag is `false` the missing key surrounded by `???` is
     * returned.
     */
    fun setExceptionOnMissingKey(enabled: Boolean) {
        exceptionOnMissingKey = enabled
    }

    /**
     * Creates a new bundle using the specified `baseFileHandle`, the default locale and the default encoding "UTF-8".
     *
     * @param baseFileHandle the file handle to the base of the bundle
     * @return a bundle for the given base file handle and the default locale
     * @throws NullPointerException     if `baseFileHandle` is `null`
     * @throws MissingResourceException if no bundle for the specified base file handle can be found
     */
    fun createBundle(baseFileHandle: FileHandle?): I18NBundle? {
        return createBundleImpl(baseFileHandle, Locale.getDefault(), DEFAULT_ENCODING)
    }

    /**
     * Creates a new bundle using the specified `baseFileHandle` and `locale`; the default encoding "UTF-8"
     * is used.
     *
     * @param baseFileHandle the file handle to the base of the bundle
     * @param locale         the locale for which a bundle is desired
     * @return a bundle for the given base file handle and locale
     * @throws NullPointerException     if `baseFileHandle` or `locale` is `null`
     * @throws MissingResourceException if no bundle for the specified base file handle can be found
     */
    fun createBundle(baseFileHandle: FileHandle?, locale: Locale?): I18NBundle? {
        return createBundleImpl(baseFileHandle, locale, DEFAULT_ENCODING)
    }

    /**
     * Creates a new bundle using the specified `baseFileHandle` and `encoding`; the default locale is used.
     *
     * @param baseFileHandle the file handle to the base of the bundle
     * @param encoding       the charter encoding
     * @return a bundle for the given base file handle and locale
     * @throws NullPointerException     if `baseFileHandle` or `encoding` is `null`
     * @throws MissingResourceException if no bundle for the specified base file handle can be found
     */
    fun createBundle(baseFileHandle: FileHandle?, encoding: String?): I18NBundle? {
        return createBundleImpl(baseFileHandle, Locale.getDefault(), encoding)
    }

    /**
     * Creates a new bundle using the specified `baseFileHandle`, `locale` and `encoding`.
     *
     * @param baseFileHandle the file handle to the base of the bundle
     * @param locale         the locale for which a bundle is desired
     * @param encoding       the charter encoding
     * @return a bundle for the given base file handle and locale
     * @throws NullPointerException     if `baseFileHandle`, `locale` or `encoding` is
     * `null`
     * @throws MissingResourceException if no bundle for the specified base file handle can be found
     */
    fun createBundle(baseFileHandle: FileHandle?, locale: Locale?, encoding: String?): I18NBundle? {
        return createBundleImpl(baseFileHandle, locale, encoding)
    }

    private fun createBundleImpl(baseFileHandle: FileHandle?, locale: Locale?, encoding: String?): I18NBundle? {
        if (baseFileHandle == null || locale == null || encoding == null) throw NullPointerException()
        var bundle: I18NBundle? = null
        var baseBundle: I18NBundle? = null
        var targetLocale: Locale? = locale
        do {
            // Create the candidate locales
            val candidateLocales: List<Locale> = getCandidateLocales(targetLocale)

            // Load the bundle and its parents recursively
            bundle = loadBundleChain(baseFileHandle, encoding, candidateLocales, 0, baseBundle)

            // Check the loaded bundle (if any)
            if (bundle != null) {
                val bundleLocale: Locale? = bundle.getLocale() // WTH? GWT can't access bundle.locale directly
                val isBaseBundle: Boolean = bundleLocale.equals(ROOT_LOCALE)
                if (!isBaseBundle || bundleLocale.equals(locale)) {
                    // Found the bundle for the requested locale
                    break
                }
                if (candidateLocales.size() === 1 && bundleLocale.equals(candidateLocales[0])) {
                    // Found the bundle for the only candidate locale
                    break
                }
                if (isBaseBundle && baseBundle == null) {
                    // Store the base bundle and keep on processing the remaining fallback locales
                    baseBundle = bundle
                }
            }

            // Set next fallback locale
            targetLocale = getFallbackLocale(targetLocale)
        } while (targetLocale != null)
        if (bundle == null) {
            if (baseBundle == null) {
                // No bundle found
                throw MissingResourceException("Can't find bundle for base file handle " + baseFileHandle.path().toString() + ", locale "
                    + locale, baseFileHandle.toString() + "_" + locale, "")
            }
            // Set the base bundle to be returned
            bundle = baseBundle
        }
        return bundle
    }

    /**
     * Returns a `List` of `Locale`s as candidate locales for the given `locale`. This method is
     * called by the `createBundle` factory method each time the factory method tries finding a resource bundle for a
     * target `Locale`.
     *
     *
     *
     * The sequence of the candidate locales also corresponds to the runtime resource lookup path (also known as the <I>parent
     * chain</I>), if the corresponding resource bundles for the candidate locales exist and their parents are not defined by
     * loaded resource bundles themselves. The last element of the list is always the [root locale][Locale.ROOT], meaning
     * that the base bundle is the terminal of the parent chain.
     *
     *
     *
     * If the given locale is equal to `Locale.ROOT` (the root locale), a `List` containing only the root
     * `Locale` is returned. In this case, the `createBundle` factory method loads only the base bundle as
     * the resulting resource bundle.
     *
     *
     *
     * This implementation returns a `List` containing `Locale`s in the following sequence:
     *
     * <pre>
     * Locale(language, country, variant)
     * Locale(language, country)
     * Locale(language)
     * Locale.ROOT
    </pre> *
     *
     *
     * where `language`, `country` and `variant` are the language, country and variant values of
     * the given `locale`, respectively. Locales where the final component values are empty strings are omitted.
     *
     *
     *
     * For example, if the given base name is "Messages" and the given `locale` is
     * `Locale("ja",&nbsp;"",&nbsp;"XX")`, then a `List` of `Locale`s:
     *
     * <pre>
     * Locale("ja", "", "XX")
     * Locale("ja")
     * Locale.ROOT
    </pre> *
     *
     *
     * is returned. And if the resource bundles for the "ja" and "" `Locale`s are found, then the runtime resource
     * lookup path (parent chain) is:
     *
     * <pre>
     * Messages_ja -> Messages
    </pre> *
     *
     * @param locale the locale for which a resource bundle is desired
     * @return a `List` of candidate `Locale`s for the given `locale`
     * @throws NullPointerException if `locale` is `null`
     */
    private fun getCandidateLocales(locale: Locale): List<Locale> {
        val language: String = locale.getLanguage()
        val country: String = locale.getCountry()
        val variant: String = locale.getVariant()
        val locales: List<Locale> = ArrayList<Locale>(4)
        if (variant.length > 0) {
            locales.add(locale)
        }
        if (country.length > 0) {
            locales.add(if (locales.isEmpty()) locale else Locale(language, country))
        }
        if (language.length > 0) {
            locales.add(if (locales.isEmpty()) locale else Locale(language))
        }
        locales.add(ROOT_LOCALE)
        return locales
    }

    /**
     * Returns a `Locale` to be used as a fallback locale for further bundle searches by the `createBundle`
     * factory method. This method is called from the factory method every time when no resulting bundle has been found for
     * `baseFileHandler` and `locale`, where locale is either the parameter for `createBundle` or
     * the previous fallback locale returned by this method.
     *
     *
     *
     * This method returns the [default &lt;code&gt;Locale&lt;/code&gt;][Locale.getDefault] if the given `locale` isn't
     * the default one. Otherwise, `null` is returned.
     *
     * @param locale the `Locale` for which `createBundle` has been unable to find any resource bundles
     * (except for the base bundle)
     * @return a `Locale` for the fallback search, or `null` if no further fallback search is needed.
     * @throws NullPointerException if `locale` is `null`
     */
    private fun getFallbackLocale(locale: Locale): Locale? {
        val defaultLocale: Locale = Locale.getDefault()
        return if (locale.equals(defaultLocale)) null else defaultLocale
    }

    private fun loadBundleChain(baseFileHandle: FileHandle, encoding: String, candidateLocales: List<Locale>,
                                candidateIndex: Int, baseBundle: I18NBundle?): I18NBundle? {
        val targetLocale: Locale = candidateLocales[candidateIndex]
        var parent: I18NBundle? = null
        if (candidateIndex != candidateLocales.size() - 1) {
            // Load recursively the parent having the next candidate locale
            parent = loadBundleChain(baseFileHandle, encoding, candidateLocales, candidateIndex + 1, baseBundle)
        } else if (baseBundle != null && targetLocale.equals(ROOT_LOCALE)) {
            return baseBundle
        }

        // Load the bundle
        val bundle = loadBundle(baseFileHandle, encoding, targetLocale)
        if (bundle != null) {
            bundle.parent = parent
            return bundle
        }
        return parent
    }

    // Tries to load the bundle for the given locale.
    private fun loadBundle(baseFileHandle: FileHandle, encoding: String, targetLocale: Locale): I18NBundle? {
        var bundle: I18NBundle? = null
        var reader: Reader? = null
        try {
            val fileHandle: FileHandle = toFileHandle(baseFileHandle, targetLocale)
            if (checkFileExistence(fileHandle)) {
                // Instantiate the bundle
                bundle = I18NBundle()

                // Load bundle properties from the stream with the specified encoding
                reader = fileHandle.reader(encoding)
                bundle.load(reader)
            }
        } catch (e: IOException) {
            throw GdxRuntimeException(e)
        } finally {
            StreamUtils.closeQuietly(reader)
        }
        bundle?.setLocale(targetLocale)
        return bundle
    }

    // On Android this is much faster than fh.exists(), see https://github.com/libgdx/libgdx/issues/2342
    // Also this should fix a weird problem on iOS, see https://github.com/libgdx/libgdx/issues/2345
    private fun checkFileExistence(fh: FileHandle): Boolean {
        return try {
            fh.read().close()
            true
        } catch (e: java.lang.Exception) {
            false
        }
    }

    /**
     * Load the properties from the specified reader.
     *
     * @param reader the reader
     * @throws IOException if an error occurred when reading from the input stream.
     */
    // NOTE:
    // This method can't be private otherwise GWT can't access it from loadBundle()
    @Throws(IOException::class)
    protected fun load(reader: Reader?) {
        properties = ObjectMap()
        PropertiesUtils.load(properties, reader)
    }

    /**
     * Converts the given `baseFileHandle` and `locale` to the corresponding file handle.
     *
     *
     *
     * This implementation returns the `baseFileHandle`'s sibling with following value:
     *
     * <pre>
     * baseFileHandle.name() + &quot;_&quot; + language + &quot;_&quot; + country + &quot;_&quot; + variant + &quot;.properties&quot;
    </pre> *
     *
     *
     * where `language`, `country` and `variant` are the language, country and variant values of
     * `locale`, respectively. Final component values that are empty Strings are omitted along with the preceding '_'.
     * If all of the values are empty strings, then `baseFileHandle.name()` is returned with ".properties" appended.
     *
     * @param baseFileHandle the file handle to the base of the bundle
     * @param locale         the locale for which a resource bundle should be loaded
     * @return the file handle for the bundle
     * @throws NullPointerException if `baseFileHandle` or `locale` is `null`
     */
    private fun toFileHandle(baseFileHandle: FileHandle, locale: Locale): FileHandle {
        val sb = StringBuilder(baseFileHandle.name())
        if (!locale.equals(ROOT_LOCALE)) {
            val language: String = locale.getLanguage()
            val country: String = locale.getCountry()
            val variant: String = locale.getVariant()
            val emptyLanguage = "" == language
            val emptyCountry = "" == country
            val emptyVariant = "" == variant
            if (!(emptyLanguage && emptyCountry && emptyVariant)) {
                sb.append('_')
                if (!emptyVariant) {
                    sb.append(language).append('_').append(country).append('_').append(variant)
                } else if (!emptyCountry) {
                    sb.append(language).append('_').append(country)
                } else {
                    sb.append(language)
                }
            }
        }
        return baseFileHandle.sibling(sb.append(".properties").toString())
    }

    /**
     * Returns the locale of this bundle. This method can be used after a call to `createBundle()` to determine whether
     * the resource bundle returned really corresponds to the requested locale or is a fallback.
     *
     * @return the locale of this bundle
     */
    fun getLocale(): Locale? {
        return locale
    }

    /**
     * Sets the bundle locale. This method is private because a bundle can't change the locale during its life.
     *
     * @param locale
     */
    private fun setLocale(locale: Locale) {
        this.locale = locale
        formatter = TextFormatter(locale, !simpleFormatter)
    }

    /**
     * Gets a string for the given key from this bundle or one of its parents.
     *
     * @param key the key for the desired string
     * @return the string for the given key or the key surrounded by `???` if it cannot be found and
     * [.getExceptionOnMissingKey] returns `false`
     * @throws NullPointerException     if `key` is `null`
     * @throws MissingResourceException if no string for the given key can be found and [.getExceptionOnMissingKey]
     * returns `true`
     */
    operator fun get(key: String): String {
        var result = properties!![key]
        if (result == null) {
            if (parent != null) result = parent!![key]
            if (result == null) {
                return if (exceptionOnMissingKey) throw MissingResourceException("Can't find bundle key $key", this.javaClass.getName(), key) else "???$key???"
            }
        }
        return result
    }

    /**
     * Gets the string with the specified key from this bundle or one of its parent after replacing the given arguments if they
     * occur.
     *
     * @param key  the key for the desired string
     * @param args the arguments to be replaced in the string associated to the given key.
     * @return the string for the given key formatted with the given arguments
     * @throws NullPointerException     if `key` is `null`
     * @throws MissingResourceException if no string for the given key can be found
     */
    fun format(key: String, vararg args: Any?): String? {
        return formatter!!.format(get(key), *args)
    }

    /**
     * Sets the value of all localized strings to String placeholder so hardcoded, unlocalized values can be easily spotted.
     * The I18NBundle won't be able to reset values after calling debug and should only be using during testing.
     *
     * @param placeholder
     */
    fun debug(placeholder: String?) {
        val keys = properties!!.keys() ?: return
        for (s in keys) {
            properties!!.put(s, placeholder)
        }
    }
}
