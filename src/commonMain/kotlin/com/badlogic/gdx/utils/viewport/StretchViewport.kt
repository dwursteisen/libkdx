package com.badlogic.gdx.utils.viewport

/** A ScalingViewport that uses [Scaling.stretch] so it does not keep the aspect ratio, the world is scaled to take the whole
 * screen.
 * @author Daniel Holderbaum
 * @author Nathan Sweet
 */
class StretchViewport : com.badlogic.gdx.utils.viewport.ScalingViewport {

    /** Creates a new viewport using a new [OrthographicCamera].  */
    constructor(worldWidth: Float, worldHeight: Float) : super(com.badlogic.gdx.utils.Scaling.stretch, worldWidth, worldHeight) {}

    constructor(worldWidth: Float, worldHeight: Float, camera: com.badlogic.gdx.graphics.Camera?) : super(com.badlogic.gdx.utils.Scaling.stretch, worldWidth, worldHeight, camera) {}
}
