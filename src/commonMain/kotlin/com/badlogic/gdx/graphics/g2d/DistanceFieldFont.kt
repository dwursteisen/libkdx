package com.badlogic.gdx.graphics.g2d

class DistanceFieldFont {
    private var distanceFieldSmoothing = 0f

    fun DistanceFieldFont(data: BitmapFont.BitmapFontData?, pageRegions: Array<TextureRegion?>?, integer: Boolean) {
        super(data, pageRegions, integer)
    }

    fun DistanceFieldFont(data: BitmapFont.BitmapFontData?, region: TextureRegion?, integer: Boolean) {
        super(data, region, integer)
    }

    fun DistanceFieldFont(fontFile: FileHandle?, flip: Boolean) {
        super(fontFile, flip)
    }

    fun DistanceFieldFont(fontFile: FileHandle?, imageFile: FileHandle?, flip: Boolean, integer: Boolean) {
        super(fontFile, imageFile, flip, integer)
    }

    fun DistanceFieldFont(fontFile: FileHandle?, imageFile: FileHandle?, flip: Boolean) {
        super(fontFile, imageFile, flip)
    }

    fun DistanceFieldFont(fontFile: FileHandle?, region: TextureRegion?, flip: Boolean) {
        super(fontFile, region, flip)
    }

    fun DistanceFieldFont(fontFile: FileHandle?, region: TextureRegion?) {
        super(fontFile, region)
    }

    fun DistanceFieldFont(fontFile: FileHandle?) {
        super(fontFile)
    }

    protected fun load(data: BitmapFont.BitmapFontData?) {
        super.load(data)

        // Distance field font rendering requires font texture to be filtered linear.
        val regions: Array<TextureRegion> = getRegions()
        for (region in regions) region.getTexture()!!.setFilter(TextureFilter.Linear, TextureFilter.Linear)
    }

    fun newFontCache(): BitmapFontCache? {
        return DistanceFieldFontCache(this, integer)
    }

    /**
     * @return The distance field smoothing factor for this font.
     */
    fun getDistanceFieldSmoothing(): Float {
        return distanceFieldSmoothing
    }

    /**
     * @param distanceFieldSmoothing Set the distance field smoothing factor for this font. SpriteBatch needs to have this shader
     * set for rendering distance field fonts.
     */
    fun setDistanceFieldSmoothing(distanceFieldSmoothing: Float) {
        this.distanceFieldSmoothing = distanceFieldSmoothing
    }

    /**
     * Returns a new instance of the distance field shader, see https://github.com/libgdx/libgdx/wiki/Distance-field-fonts if the
     * u_smoothing uniform > 0.0. Otherwise the same code as the default SpriteBatch shader is used.
     */
    fun createDistanceFieldShader(): ShaderProgram? {
        val vertexShader = """attribute vec4 ${ShaderProgram.POSITION_ATTRIBUTE.toString()};
attribute vec4 ${ShaderProgram.COLOR_ATTRIBUTE.toString()};
attribute vec2 ${ShaderProgram.TEXCOORD_ATTRIBUTE.toString()}0;
uniform mat4 u_projTrans;
varying vec4 v_color;
varying vec2 v_texCoords;

void main() {
	v_color = ${ShaderProgram.COLOR_ATTRIBUTE.toString()};
	v_color.a = v_color.a * (255.0/254.0);
	v_texCoords = ${ShaderProgram.TEXCOORD_ATTRIBUTE.toString()}0;
	gl_Position =  u_projTrans * ${ShaderProgram.POSITION_ATTRIBUTE.toString()};
}
"""
        val fragmentShader = """#ifdef GL_ES
	precision mediump float;
	precision mediump int;
#endif

uniform sampler2D u_texture;
uniform float u_smoothing;
varying vec4 v_color;
varying vec2 v_texCoords;

void main() {
	if (u_smoothing > 0.0) {
		float smoothing = 0.25 / u_smoothing;
		float distance = texture2D(u_texture, v_texCoords).a;
		float alpha = smoothstep(0.5 - smoothing, 0.5 + smoothing, distance);
		gl_FragColor = vec4(v_color.rgb, alpha * v_color.a);
	} else {
		gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
	}
}
"""
        val shader = ShaderProgram(vertexShader, fragmentShader)
        if (!shader.isCompiled()) throw java.lang.IllegalArgumentException("Error compiling distance field shader: " + shader.getLog())
        return shader
    }

    /**
     * Provides a font cache that uses distance field shader for rendering fonts. Attention: breaks batching because uniform is
     * needed for smoothing factor, so a flush is performed before and after every font rendering.
     *
     * @author Florian Falkner
     */
    private class DistanceFieldFontCache : BitmapFontCache {

        constructor(font: DistanceFieldFont) : super(font, font.usesIntegerPositions()) {}
        constructor(font: DistanceFieldFont?, integer: Boolean) : super(font, integer) {}

        private val smoothingFactor: Float
            private get() {
                val font = super.getFont() as DistanceFieldFont
                return font.getDistanceFieldSmoothing() * font.getScaleX()
            }

        private fun setSmoothingUniform(spriteBatch: Batch, smoothing: Float) {
            spriteBatch.flush()
            spriteBatch.getShader().setUniformf("u_smoothing", smoothing)
        }

        override fun draw(spriteBatch: Batch) {
            setSmoothingUniform(spriteBatch, smoothingFactor)
            super.draw(spriteBatch)
            setSmoothingUniform(spriteBatch, 0f)
        }

        override fun draw(spriteBatch: Batch, start: Int, end: Int) {
            setSmoothingUniform(spriteBatch, smoothingFactor)
            super.draw(spriteBatch, start, end)
            setSmoothingUniform(spriteBatch, 0f)
        }
    }
}
