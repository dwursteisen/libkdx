package com.badlogic.gdx.utils.viewport

/** A ScalingViewport that uses [Scaling.fill] so it keeps the aspect ratio by scaling the world up to take the whole screen
 * (some of the world may be off screen).
 * @author Daniel Holderbaum
 * @author Nathan Sweet
 */
class FillViewport : com.badlogic.gdx.utils.viewport.ScalingViewport {

    /** Creates a new viewport using a new [OrthographicCamera].  */
    constructor(worldWidth: Float, worldHeight: Float) : super(com.badlogic.gdx.utils.Scaling.fill, worldWidth, worldHeight) {}

    constructor(worldWidth: Float, worldHeight: Float, camera: com.badlogic.gdx.graphics.Camera?) : super(com.badlogic.gdx.utils.Scaling.fill, worldWidth, worldHeight, camera) {}
}
