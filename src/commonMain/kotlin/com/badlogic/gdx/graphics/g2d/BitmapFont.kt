package com.badlogic.gdx.graphics.g2d


/**
 * Renders bitmap fonts. The font consists of 2 files: an image file or {@link TextureRegion} containing the glyphs and a file in
 * the AngleCode BMFont text format that describes where each glyph is on the image.
 * <p>
 * Text is drawn using a {@link Batch}. Text can be cached in a {@link BitmapFontCache} for faster rendering of static text, which
 * saves needing to compute the location of each glyph each frame.
 * <p>
 * * The texture for a BitmapFont loaded from a file is managed. {@link #dispose()} must be called to free the texture when no
 * longer needed. A BitmapFont loaded using a {@link TextureRegion} is managed if the region's texture is managed. Disposing the
 * BitmapFont disposes the region's texture, which may not be desirable if the texture is still being used elsewhere.
 * <p>
 * The code was originally based on Matthias Mann's TWL BitmapFont class. Thanks for sharing, Matthias! :)
 *
 * @author Nathan Sweet
 * @author Matthias Mann
 */
class BitmapFont {

    private val LOG2_PAGE_SIZE = 9
    private val PAGE_SIZE = 1 shl LOG2_PAGE_SIZE
    private val PAGES = 0x10000 / PAGE_SIZE

    val data: BitmapFontData? = null
    var regions: Array<TextureRegion>? = null
    private val cache: BitmapFontCache? = null
    private val flipped = false
    var integer = false
    private val ownsTexture = false


    /**
     * Creates a BitmapFont using the default 15pt Arial font included in the libgdx JAR file. This is convenient to easily
     * display text without bothering without generating a bitmap font yourself.
     */
    public BitmapFont() {
        this(Gdx.files.classpath("com/badlogic/gdx/utils/arial-15.fnt"), Gdx.files.classpath("com/badlogic/gdx/utils/arial-15.png"),
            false, true);
    }

    /**
     * Creates a BitmapFont using the default 15pt Arial font included in the libgdx JAR file. This is convenient to easily
     * display text without bothering without generating a bitmap font yourself.
     *
     * @param flip If true, the glyphs will be flipped for use with a perspective where 0,0 is the upper left corner.
     */
    public BitmapFont(boolean flip) {
        this(Gdx.files.classpath("com/badlogic/gdx/utils/arial-15.fnt"), Gdx.files.classpath("com/badlogic/gdx/utils/arial-15.png"),
            flip, true);
    }

    /**
     * Creates a BitmapFont with the glyphs relative to the specified region. If the region is null, the glyph textures are loaded
     * from the image file given in the font file. The {@link #dispose()} method will not dispose the region's texture in this
     * case!
     * <p>
     * The font data is not flipped.
     *
     * @param fontFile the font definition file
     * @param region   The texture region containing the glyphs. The glyphs must be relative to the lower left corner (ie, the region
     *                 should not be flipped). If the region is null the glyph images are loaded from the image path in the font file.
     */
    public BitmapFont(FileHandle fontFile, TextureRegion region) {
        this(fontFile, region, false);
    }

    /**
     * Creates a BitmapFont with the glyphs relative to the specified region. If the region is null, the glyph textures are loaded
     * from the image file given in the font file. The {@link #dispose()} method will not dispose the region's texture in this
     * case!
     *
     * @param region The texture region containing the glyphs. The glyphs must be relative to the lower left corner (ie, the region
     *               should not be flipped). If the region is null the glyph images are loaded from the image path in the font file.
     * @param flip   If true, the glyphs will be flipped for use with a perspective where 0,0 is the upper left corner.
     */
    public BitmapFont(FileHandle fontFile, TextureRegion region, boolean flip) {
        this(new BitmapFontData(fontFile, flip), region, true);
    }

    /**
     * Creates a BitmapFont from a BMFont file. The image file name is read from the BMFont file and the image is loaded from the
     * same directory. The font data is not flipped.
     */
    public BitmapFont(FileHandle fontFile) {
        this(fontFile, false);
    }

    /**
     * Creates a BitmapFont from a BMFont file. The image file name is read from the BMFont file and the image is loaded from the
     * same directory.
     *
     * @param flip If true, the glyphs will be flipped for use with a perspective where 0,0 is the upper left corner.
     */
    public BitmapFont(FileHandle fontFile, boolean flip) {
        this(new BitmapFontData(fontFile, flip), (TextureRegion) null, true);
    }

    /**
     * Creates a BitmapFont from a BMFont file, using the specified image for glyphs. Any image specified in the BMFont file is
     * ignored.
     *
     * @param flip If true, the glyphs will be flipped for use with a perspective where 0,0 is the upper left corner.
     */
    public BitmapFont(FileHandle fontFile, FileHandle imageFile, boolean flip) {
        this(fontFile, imageFile, flip, true);
    }

    /**
     * Creates a BitmapFont from a BMFont file, using the specified image for glyphs. Any image specified in the BMFont file is
     * ignored.
     *
     * @param flip    If true, the glyphs will be flipped for use with a perspective where 0,0 is the upper left corner.
     * @param integer If true, rendering positions will be at integer values to avoid filtering artifacts.
     */
    public BitmapFont(FileHandle fontFile, FileHandle imageFile, boolean flip, boolean integer) {
        this(new BitmapFontData(fontFile, flip), new TextureRegion(new Texture(imageFile, false)), integer);
        ownsTexture = true;
    }

    /**
     * Constructs a new BitmapFont from the given {@link BitmapFontData} and {@link TextureRegion}. If the TextureRegion is null,
     * the image path(s) will be read from the BitmapFontData. The dispose() method will not dispose the texture of the region(s)
     * if the region is != null.
     * <p>
     * Passing a single TextureRegion assumes that your font only needs a single texture page. If you need to support multiple
     * pages, either let the Font read the images themselves (by specifying null as the TextureRegion), or by specifying each page
     * manually with the TextureRegion[] constructor.
     *
     * @param integer If true, rendering positions will be at integer values to avoid filtering artifacts.
     */
    public BitmapFont(BitmapFontData data, TextureRegion region, boolean integer) {
        this(data, region != null ? Array.with(region) : null, integer);
    }

    /**
     * Constructs a new BitmapFont from the given {@link BitmapFontData} and array of {@link TextureRegion}. If the TextureRegion
     * is null or empty, the image path(s) will be read from the BitmapFontData. The dispose() method will not dispose the texture
     * of the region(s) if the regions array is != null and not empty.
     *
     * @param integer If true, rendering positions will be at integer values to avoid filtering artifacts.
     */
    public BitmapFont(BitmapFontData data, Array<TextureRegion> pageRegions, boolean integer) {
        this.flipped = data.flipped;
        this.data = data;
        this.integer = integer;

        if (pageRegions == null || pageRegions.size == 0) {
            if (data.imagePaths == null)
                throw new IllegalArgumentException("If no regions are specified, the font data must have an images path.");

            // Load each path.
            int n = data.imagePaths.length;
            regions = new Array(n);
            for (int i = 0; i < n; i++) {
                FileHandle file;
                if (data.fontFile == null)
                    file = Gdx.files.internal(data.imagePaths[i]);
                else
                    file = Gdx.files.getFileHandle(data.imagePaths[i], data.fontFile.type());
                regions.add(new TextureRegion(new Texture(file, false)));
            }
            ownsTexture = true;
        } else {
            regions = pageRegions;
            ownsTexture = false;
        }

        cache = newFontCache();

        load(data);
    }

    protected fun load(data: BitmapFontData) {
        for (page in data.glyphs) {
            if (page == null) continue
            for (glyph in page) if (glyph != null) data.setGlyphRegion(glyph, regions!![glyph.page])
        }
        if (data.missingGlyph != null) data.setGlyphRegion(data.missingGlyph, regions!![data.missingGlyph.page])
    }

    /**
     * Draws text at the specified position.
     *
     * @see BitmapFontCache.addText
     */
    fun draw(batch: Batch?, str: CharSequence?, x: Float, y: Float): GlyphLayout? {
        cache!!.clear()
        val layout = cache.addText(str!!, x, y)
        cache.draw(batch!!)
        return layout
    }

    /**
     * Draws text at the specified position.
     *
     * @see BitmapFontCache.addText
     */
    fun draw(batch: Batch?, str: CharSequence?, x: Float, y: Float, targetWidth: Float, halign: Int, wrap: Boolean): GlyphLayout? {
        cache!!.clear()
        val layout = cache.addText(str!!, x, y, targetWidth, halign, wrap)
        cache.draw(batch!!)
        return layout
    }

    /**
     * Draws text at the specified position.
     *
     * @see BitmapFontCache.addText
     */
    fun draw(batch: Batch?, str: CharSequence?, x: Float, y: Float, start: Int, end: Int, targetWidth: Float, halign: Int,
             wrap: Boolean): GlyphLayout? {
        cache!!.clear()
        val layout = cache.addText(str!!, x, y, start, end, targetWidth, halign, wrap)
        cache.draw(batch!!)
        return layout
    }

    /**
     * Draws text at the specified position.
     *
     * @see BitmapFontCache.addText
     */
    fun draw(batch: Batch?, str: CharSequence?, x: Float, y: Float, start: Int, end: Int, targetWidth: Float, halign: Int,
             wrap: Boolean, truncate: String?): GlyphLayout? {
        cache!!.clear()
        val layout = cache.addText(str!!, x, y, start, end, targetWidth, halign, wrap, truncate)
        cache.draw(batch!!)
        return layout
    }

    /**
     * Draws text at the specified position.
     *
     * @see BitmapFontCache.addText
     */
    fun draw(batch: Batch?, layout: GlyphLayout?, x: Float, y: Float) {
        cache!!.clear()
        cache.addText(layout!!, x, y)
        cache.draw(batch!!)
    }

    /**
     * Returns the color of text drawn with this font.
     */
    fun getColor(): Color? {
        return cache!!.getColor()
    }

    /**
     * A convenience method for setting the font color. The color can also be set by modifying [.getColor].
     */
    fun setColor(color: Color?) {
        cache!!.getColor().set(color)
    }

    /**
     * A convenience method for setting the font color. The color can also be set by modifying [.getColor].
     */
    fun setColor(r: Float, g: Float, b: Float, a: Float) {
        cache!!.getColor()[r, g, b] = a
    }

    fun getScaleX(): Float {
        return data.scaleX
    }

    fun getScaleY(): Float {
        return data.scaleY
    }

    /**
     * Returns the first texture region. This is included for backwards compatibility, and for convenience since most fonts only
     * use one texture page. For multi-page fonts, use [.getRegions].
     *
     * @return the first texture region
     */
    fun getRegion(): TextureRegion? {
        return regions!!.first()
    }

    /**
     * Returns the array of TextureRegions that represents each texture page of glyphs.
     *
     * @return the array of texture regions; modifying it may produce undesirable results
     */
    fun getRegions(): Array<TextureRegion?>? {
        return regions
    }

    /**
     * Returns the texture page at the given index.
     *
     * @return the texture page at the given index
     */
    fun getRegion(index: Int): TextureRegion? {
        return regions!![index]
    }

    /**
     * Returns the line height, which is the distance from one line of text to the next.
     */
    fun getLineHeight(): Float {
        return data.lineHeight
    }

    /**
     * Returns the x-advance of the space character.
     */
    fun getSpaceXadvance(): Float {
        return data.spaceXadvance
    }

    /**
     * Returns the x-height, which is the distance from the top of most lowercase characters to the baseline.
     */
    fun getXHeight(): Float {
        return data.xHeight
    }

    /**
     * Returns the cap height, which is the distance from the top of most uppercase characters to the baseline. Since the drawing
     * position is the cap height of the first line, the cap height can be used to get the location of the baseline.
     */
    fun getCapHeight(): Float {
        return data.capHeight
    }

    /**
     * Returns the ascent, which is the distance from the cap height to the top of the tallest glyph.
     */
    fun getAscent(): Float {
        return data.ascent
    }

    /**
     * Returns the descent, which is the distance from the bottom of the glyph that extends the lowest to the baseline. This
     * number is negative.
     */
    fun getDescent(): Float {
        return data.descent
    }

    /**
     * Returns true if this BitmapFont has been flipped for use with a y-down coordinate system.
     */
    fun isFlipped(): Boolean {
        return flipped
    }

    /**
     * Disposes the texture used by this BitmapFont's region IF this BitmapFont created the texture.
     */
    fun dispose() {
        if (ownsTexture) {
            for (i in 0 until regions!!.size) regions!![i].getTexture()!!.dispose()
        }
    }

    /**
     * Backing data for a [BitmapFont].
     */
    class BitmapFontData {

        /**
         * The name of the font, or null.
         */
        var name: String? = null

        /**
         * An array of the image paths, for multiple texture pages.
         */
        var imagePaths: Array<String?>?
        var fontFile: FileHandle? = null
        var flipped = false
        var padTop = 0f
        var padRight = 0f
        var padBottom = 0f
        var padLeft = 0f

        /**
         * The distance from one line of text to the next. To set this value, use [.setLineHeight].
         */
        var lineHeight = 0f

        /**
         * The distance from the top of most uppercase characters to the baseline. Since the drawing position is the cap height of
         * the first line, the cap height can be used to get the location of the baseline.
         */
        var capHeight = 1f

        /**
         * The distance from the cap height to the top of the tallest glyph.
         */
        var ascent = 0f

        /**
         * The distance from the bottom of the glyph that extends the lowest to the baseline. This number is negative.
         */
        var descent = 0f

        /**
         * The distance to move down when \n is encountered.
         */
        var down = 0f

        /**
         * Multiplier for the line height of blank lines. down * blankLineHeight is used as the distance to move down for a blank
         * line.
         */
        var blankLineScale = 1f
        var scaleX = 1f
        var scaleY = 1f
        var markupEnabled = false

        /**
         * The amount to add to the glyph X position when drawing a cursor between glyphs. This field is not set by the BMFont
         * file, it needs to be set manually depending on how the glyphs are rendered on the backing textures.
         */
        var cursorX = 0f
        val glyphs: Array<Array<BitmapFont.Glyph?>?> = arrayOfNulls<Array<BitmapFont.Glyph?>?>(BitmapFont.PAGES)

        /**
         * The glyph to display for characters not in the font. May be null.
         */
        var missingGlyph: BitmapFont.Glyph? = null

        /**
         * The width of the space character.
         */
        var spaceXadvance = 0f

        /**
         * The x-height, which is the distance from the top of most lowercase characters to the baseline.
         */
        var xHeight = 1f

        /**
         * Additional characters besides whitespace where text is wrapped. Eg, a hypen (-).
         */
        var breakChars: CharArray?
        var xChars = charArrayOf('x', 'e', 'a', 'o', 'n', 's', 'r', 'c', 'u', 'm', 'v', 'w', 'z')
        var capChars = charArrayOf('M', 'N', 'B', 'D', 'C', 'E', 'F', 'K', 'A', 'G', 'H', 'I', 'J', 'L', 'O', 'P', 'Q', 'R', 'S',
            'T', 'U', 'V', 'W', 'X', 'Y', 'Z')

        /**
         * Creates an empty BitmapFontData for configuration before calling [.load], to subclass, or to
         * populate yourself, e.g. using stb-truetype or FreeType.
         */
        constructor() {}
        constructor(fontFile: FileHandle, flip: Boolean) {
            this.fontFile = fontFile
            flipped = flip
            load(fontFile, flip)
        }

        fun load(fontFile: FileHandle, flip: Boolean) {
            check(imagePaths == null) { "Already loaded." }
            name = fontFile.nameWithoutExtension()
            val reader = BufferedReader(InputStreamReader(fontFile.read()), 512)
            try {
                var line: String = reader.readLine() ?: throw GdxRuntimeException("File is empty.") // info
                line = line.substring(line.indexOf("padding=") + 8)
                val padding = line.substring(0, line.indexOf(' ')).split(",", 4.toBoolean()).toTypedArray()
                if (padding.size != 4) throw GdxRuntimeException("Invalid padding.")
                padTop = padding[0].toInt().toFloat()
                padRight = padding[1].toInt().toFloat()
                padBottom = padding[2].toInt().toFloat()
                padLeft = padding[3].toInt().toFloat()
                val padY = padTop + padBottom
                line = reader.readLine()
                if (line == null) throw GdxRuntimeException("Missing common header.")
                val common: Array<String?> = line.split(" ", 9.toBoolean()).toTypedArray() // At most we want the 6th element; i.e. "page=N"

                // At least lineHeight and base are required.
                if (common.size < 3) throw GdxRuntimeException("Invalid common header.")
                if (!common[1]!!.startsWith("lineHeight=")) throw GdxRuntimeException("Missing: lineHeight")
                lineHeight = common[1]!!.substring(11).toInt().toFloat()
                if (!common[2]!!.startsWith("base=")) throw GdxRuntimeException("Missing: base")
                val baseLine = common[2]!!.substring(5).toInt().toFloat()
                var pageCount = 1
                if (common.size >= 6 && common[5] != null && common[5]!!.startsWith("pages=")) {
                    try {
                        pageCount = java.lang.Math.max(1, common[5]!!.substring(6).toInt())
                    } catch (ignored: NumberFormatException) { // Use one page.
                    }
                }
                imagePaths = arrayOfNulls(pageCount)

                // Read each page definition.
                for (p in 0 until pageCount) {
                    // Read each "page" info line.
                    line = reader.readLine()
                    if (line == null) throw GdxRuntimeException("Missing additional page definitions.")

                    // Expect ID to mean "index".
                    var matcher: Matcher = Pattern.compile(".*id=(\\d+)").matcher(line)
                    if (matcher.find()) {
                        val id: String = matcher.group(1)
                        try {
                            val pageID = id.toInt()
                            if (pageID != p) throw GdxRuntimeException("Page IDs must be indices starting at 0: $id")
                        } catch (ex: NumberFormatException) {
                            throw GdxRuntimeException("Invalid page id: $id", ex)
                        }
                    }
                    matcher = Pattern.compile(".*file=\"?([^\"]+)\"?").matcher(line)
                    if (!matcher.find()) throw GdxRuntimeException("Missing: file")
                    val fileName: String = matcher.group(1)
                    imagePaths!![p] = fontFile.parent().child(fileName).path().replaceAll("\\\\", "/")
                }
                descent = 0f
                while (true) {
                    line = reader.readLine()
                    if (line == null) break // EOF
                    if (line.startsWith("kernings ")) break // Starting kernings block.
                    if (line.startsWith("metrics ")) break // Starting metrics block.
                    if (!line.startsWith("char ")) continue
                    val glyph: BitmapFont.Glyph = BitmapFont.Glyph()
                    val tokens = StringTokenizer(line, " =")
                    tokens.nextToken()
                    tokens.nextToken()
                    val ch: Int = tokens.nextToken().toInt()
                    if (ch <= 0) missingGlyph = glyph else if (ch <= java.lang.Character.MAX_VALUE.toInt()) setGlyph(ch, glyph) else continue
                    glyph.id = ch
                    tokens.nextToken()
                    glyph.srcX = tokens.nextToken().toInt()
                    tokens.nextToken()
                    glyph.srcY = tokens.nextToken().toInt()
                    tokens.nextToken()
                    glyph.width = tokens.nextToken().toInt()
                    tokens.nextToken()
                    glyph.height = tokens.nextToken().toInt()
                    tokens.nextToken()
                    glyph.xoffset = tokens.nextToken().toInt()
                    tokens.nextToken()
                    if (flip) glyph.yoffset = tokens.nextToken().toInt() else glyph.yoffset = -(glyph.height + tokens.nextToken().toInt())
                    tokens.nextToken()
                    glyph.xadvance = tokens.nextToken().toInt()

                    // Check for page safely, it could be omitted or invalid.
                    if (tokens.hasMoreTokens()) tokens.nextToken()
                    if (tokens.hasMoreTokens()) {
                        try {
                            glyph.page = tokens.nextToken().toInt()
                        } catch (ignored: NumberFormatException) {
                        }
                    }
                    if (glyph.width > 0 && glyph.height > 0) descent = java.lang.Math.min(baseLine + glyph.yoffset, descent)
                }
                descent += padBottom
                while (true) {
                    line = reader.readLine()
                    if (line == null) break
                    if (!line.startsWith("kerning ")) break
                    val tokens = StringTokenizer(line, " =")
                    tokens.nextToken()
                    tokens.nextToken()
                    val first: Int = tokens.nextToken().toInt()
                    tokens.nextToken()
                    val second: Int = tokens.nextToken().toInt()
                    if (first < 0 || first > java.lang.Character.MAX_VALUE.toInt() || second < 0 || second > java.lang.Character.MAX_VALUE.toInt()) continue
                    val glyph: BitmapFont.Glyph? = getGlyph(first.toChar())
                    tokens.nextToken()
                    val amount: Int = tokens.nextToken().toInt()
                    if (glyph != null) { // Kernings may exist for glyph pairs not contained in the font.
                        glyph.setKerning(second, amount)
                    }
                }
                var hasMetricsOverride = false
                var overrideAscent = 0f
                var overrideDescent = 0f
                var overrideDown = 0f
                var overrideCapHeight = 0f
                var overrideLineHeight = 0f
                var overrideSpaceXAdvance = 0f
                var overrideXHeight = 0f

                // Metrics override
                if (line != null && line.startsWith("metrics ")) {
                    hasMetricsOverride = true
                    val tokens = StringTokenizer(line, " =")
                    tokens.nextToken()
                    tokens.nextToken()
                    overrideAscent = tokens.nextToken().toFloat()
                    tokens.nextToken()
                    overrideDescent = tokens.nextToken().toFloat()
                    tokens.nextToken()
                    overrideDown = tokens.nextToken().toFloat()
                    tokens.nextToken()
                    overrideCapHeight = tokens.nextToken().toFloat()
                    tokens.nextToken()
                    overrideLineHeight = tokens.nextToken().toFloat()
                    tokens.nextToken()
                    overrideSpaceXAdvance = tokens.nextToken().toFloat()
                    tokens.nextToken()
                    overrideXHeight = tokens.nextToken().toFloat()
                }
                var spaceGlyph: BitmapFont.Glyph? = getGlyph(' ')
                if (spaceGlyph == null) {
                    spaceGlyph = BitmapFont.Glyph()
                    spaceGlyph.id = ' '.toInt()
                    var xadvanceGlyph: BitmapFont.Glyph? = getGlyph('l')
                    if (xadvanceGlyph == null) xadvanceGlyph = firstGlyph
                    spaceGlyph.xadvance = xadvanceGlyph.xadvance
                    setGlyph(' '.toInt(), spaceGlyph)
                }
                if (spaceGlyph.width == 0) {
                    spaceGlyph.width = (padLeft + spaceGlyph.xadvance + padRight)
                    spaceGlyph.xoffset = (-padLeft).toInt()
                }
                spaceXadvance = spaceGlyph.xadvance.toFloat()
                var xGlyph: BitmapFont.Glyph? = null
                for (xChar in xChars) {
                    xGlyph = getGlyph(xChar)
                    if (xGlyph != null) break
                }
                if (xGlyph == null) xGlyph = firstGlyph
                xHeight = xGlyph.height - padY
                var capGlyph: BitmapFont.Glyph? = null
                for (capChar in capChars) {
                    capGlyph = getGlyph(capChar)
                    if (capGlyph != null) break
                }
                if (capGlyph == null) {
                    for (page in glyphs) {
                        if (page == null) continue
                        for (glyph in page) {
                            if (glyph == null || glyph.height == 0 || glyph.width == 0) continue
                            capHeight = java.lang.Math.max(capHeight, glyph.height.toFloat())
                        }
                    }
                } else capHeight = capGlyph.height.toFloat()
                capHeight -= padY
                ascent = baseLine - capHeight
                down = -lineHeight
                if (flip) {
                    ascent = -ascent
                    down = -down
                }
                if (hasMetricsOverride) {
                    ascent = overrideAscent
                    descent = overrideDescent
                    down = overrideDown
                    capHeight = overrideCapHeight
                    lineHeight = overrideLineHeight
                    spaceXadvance = overrideSpaceXAdvance
                    xHeight = overrideXHeight
                }
            } catch (ex: java.lang.Exception) {
                throw GdxRuntimeException("Error loading font file: $fontFile", ex)
            } finally {
                StreamUtils.closeQuietly(reader)
            }
        }

        fun setGlyphRegion(glyph: BitmapFont.Glyph, region: TextureRegion) {
            val texture: Texture? = region.getTexture()
            val invTexWidth: Float = 1.0f / texture.getWidth()
            val invTexHeight: Float = 1.0f / texture.getHeight()
            var offsetX = 0f
            var offsetY = 0f
            val u = region.u
            val v = region.v
            val regionWidth = region.getRegionWidth().toFloat()
            val regionHeight = region.getRegionHeight().toFloat()
            if (region is AtlasRegion) {
                // Compensate for whitespace stripped from left and top edges.
                val atlasRegion: AtlasRegion = region as AtlasRegion
                offsetX = atlasRegion.offsetX
                offsetY = atlasRegion.originalHeight - atlasRegion.packedHeight - atlasRegion.offsetY
            }
            var x: Float = glyph.srcX.toFloat()
            var x2: Float = glyph.srcX + glyph.width.toFloat()
            var y: Float = glyph.srcY.toFloat()
            var y2: Float = glyph.srcY + glyph.height.toFloat()

            // Shift glyph for left and top edge stripped whitespace. Clip glyph for right and bottom edge stripped whitespace.
            // Note if the font region has padding, whitespace stripping must not be used.
            if (offsetX > 0) {
                x -= offsetX
                if (x < 0) {
                    glyph.width += x.toInt()
                    glyph.xoffset -= x.toInt()
                    x = 0f
                }
                x2 -= offsetX
                if (x2 > regionWidth) {
                    glyph.width -= x2 - regionWidth.toInt()
                    x2 = regionWidth
                }
            }
            if (offsetY > 0) {
                y -= offsetY
                if (y < 0) {
                    glyph.height += y.toInt()
                    if (glyph.height < 0) glyph.height = 0
                    y = 0f
                }
                y2 -= offsetY
                if (y2 > regionHeight) {
                    val amount = y2 - regionHeight
                    glyph.height -= amount.toInt()
                    glyph.yoffset += amount.toInt()
                    y2 = regionHeight
                }
            }
            glyph.u = u + x * invTexWidth
            glyph.u2 = u + x2 * invTexWidth
            if (flipped) {
                glyph.v = v + y * invTexHeight
                glyph.v2 = v + y2 * invTexHeight
            } else {
                glyph.v2 = v + y * invTexHeight
                glyph.v = v + y2 * invTexHeight
            }
        }

        /**
         * Sets the line height, which is the distance from one line of text to the next.
         */
        fun setLineHeight(height: Float) {
            lineHeight = height * scaleY
            down = if (flipped) lineHeight else -lineHeight
        }

        fun setGlyph(ch: Int, glyph: BitmapFont.Glyph?) {
            var page: Array<BitmapFont.Glyph?>? = glyphs[ch / BitmapFont.PAGE_SIZE]
            if (page == null) {
                page = arrayOfNulls<BitmapFont.Glyph>(BitmapFont.PAGE_SIZE)
                glyphs[ch / BitmapFont.PAGE_SIZE] = page
            }
            page!![ch and BitmapFont.PAGE_SIZE - 1] = glyph
        }

        val firstGlyph: BitmapFont.Glyph?
            get() {
                for (page in glyphs) {
                    if (page == null) continue
                    for (glyph in page) {
                        if (glyph == null || glyph.height == 0 || glyph.width == 0) continue
                        return glyph
                    }
                }
                throw GdxRuntimeException("No glyphs found.")
            }

        /**
         * Returns true if the font has the glyph, or if the font has a [.missingGlyph].
         */
        fun hasGlyph(ch: Char): Boolean {
            return if (missingGlyph != null) true else getGlyph(ch) != null
        }

        /**
         * Returns the glyph for the specified character, or null if no such glyph exists. Note that
         * [.getGlyphs] should be be used to shape a string of characters into a list
         * of glyphs.
         */
        fun getGlyph(ch: Char): BitmapFont.Glyph? {
            val page: Array<BitmapFont.Glyph?>? = glyphs[ch.toInt() / BitmapFont.PAGE_SIZE]
            return page?.get(ch.toInt() and BitmapFont.PAGE_SIZE - 1)
        }

        /**
         * Using the specified string, populates the glyphs and positions of the specified glyph run.
         *
         * @param str       Characters to convert to glyphs. Will not contain newline or color tags. May contain "[[" for an escaped left
         * square bracket.
         * @param lastGlyph The glyph immediately before this run, or null if this is run is the first on a line of text.
         */
        fun getGlyphs(run: GlyphRun, str: CharSequence, start: Int, end: Int, lastGlyph: BitmapFont.Glyph?) {
            var start = start
            var lastGlyph: BitmapFont.Glyph? = lastGlyph
            val markupEnabled = markupEnabled
            val scaleX = scaleX
            val missingGlyph: BitmapFont.Glyph? = missingGlyph
            val glyphs: Array<BitmapFont.Glyph> = run.glyphs
            val xAdvances: FloatArray = run.xAdvances

            // Guess at number of glyphs needed.
            glyphs.ensureCapacity(end - start)
            xAdvances.ensureCapacity(end - start + 1)
            while (start < end) {
                val ch = str[start++]
                if (ch == '\r') continue  // Ignore.
                var glyph: BitmapFont.Glyph? = getGlyph(ch)
                if (glyph == null) {
                    if (missingGlyph == null) continue
                    glyph = missingGlyph
                }
                glyphs.add(glyph)
                if (lastGlyph == null) // First glyph on line, adjust the position so it isn't drawn left of 0.
                    xAdvances.add(if (glyph.fixedWidth) 0 else -glyph.xoffset * scaleX - padLeft) else xAdvances.add((lastGlyph.xadvance + lastGlyph.getKerning(ch)) * scaleX)
                lastGlyph = glyph

                // "[[" is an escaped left square bracket, skip second character.
                if (markupEnabled && ch == '[' && start < end && str[start] == '[') start++
            }
            if (lastGlyph != null) {
                val lastGlyphWidth: Float = if (lastGlyph.fixedWidth) lastGlyph.xadvance * scaleX else (lastGlyph.width + lastGlyph.xoffset) * scaleX - padRight
                xAdvances.add(lastGlyphWidth)
            }
        }

        /**
         * Returns the first valid glyph index to use to wrap to the next line, starting at the specified start index and
         * (typically) moving toward the beginning of the glyphs array.
         */
        fun getWrapIndex(glyphs: Array<BitmapFont.Glyph>, start: Int): Int {
            var i = start - 1
            var ch = glyphs[i].id as Char
            if (isWhitespace(ch)) return i
            if (isBreakChar(ch)) i--
            while (i > 0) {
                ch = glyphs[i].id
                if (isBreakChar(ch)) return i + 1
                if (isWhitespace(ch)) return i + 1
                i--
            }
            return 0
        }

        fun isBreakChar(c: Char): Boolean {
            if (breakChars == null) return false
            for (br in breakChars!!) if (c == br) return true
            return false
        }

        fun isWhitespace(c: Char): Boolean {
            return when (c) {
                '\n', '\r', '\t', ' ' -> true
                else -> false
            }
        }

        /**
         * Returns the image path for the texture page at the given index (the "id" in the BMFont file).
         */
        fun getImagePath(index: Int): String? {
            return imagePaths!![index]
        }

        fun getFontFile(): FileHandle? {
            return fontFile
        }

        /**
         * Scales the font by the specified amounts on both axes
         *
         *
         * Note that smoother scaling can be achieved if the texture backing the BitmapFont is using [TextureFilter.Linear].
         * The default is Nearest, so use a BitmapFont constructor that takes a [TextureRegion].
         *
         * @throws IllegalArgumentException if scaleX or scaleY is zero.
         */
        fun setScale(scaleX: Float, scaleY: Float) {
            if (scaleX == 0f) throw java.lang.IllegalArgumentException("scaleX cannot be 0.")
            if (scaleY == 0f) throw java.lang.IllegalArgumentException("scaleY cannot be 0.")
            val x = scaleX / this.scaleX
            val y = scaleY / this.scaleY
            lineHeight *= y
            spaceXadvance *= x
            xHeight *= y
            capHeight *= y
            ascent *= y
            descent *= y
            down *= y
            padLeft *= x
            padRight *= x
            padTop *= y
            padBottom *= y
            this.scaleX = scaleX
            this.scaleY = scaleY
        }

        /**
         * Scales the font by the specified amount in both directions.
         *
         * @throws IllegalArgumentException if scaleX or scaleY is zero.
         * @see .setScale
         */
        fun setScale(scaleXY: Float) {
            setScale(scaleXY, scaleXY)
        }

        /**
         * Sets the font's scale relative to the current scale.
         *
         * @throws IllegalArgumentException if the resulting scale is zero.
         * @see .setScale
         */
        fun scale(amount: Float) {
            setScale(scaleX + amount, scaleY + amount)
        }

        override fun toString(): String {
            return (if (name != null) name!! else super.toString())
        }
    }
}
