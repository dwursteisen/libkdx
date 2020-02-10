package com.badlogic.gdx.utils.viewport

/** A ScalingViewport that uses [Scaling.fit] so it keeps the aspect ratio by scaling the world up to fit the screen, adding
 * black bars (letterboxing) for the remaining space.
 * @author Daniel Holderbaum
 * @author Nathan Sweet
 */
class FitViewport : com.badlogic.gdx.utils.viewport.ScalingViewport {

    /** Creates a new viewport using a new [OrthographicCamera].  */
    constructor(worldWidth: Float, worldHeight: Float) : super(com.badlogic.gdx.utils.Scaling.fit, worldWidth, worldHeight) {}

    constructor(worldWidth: Float, worldHeight: Float, camera: com.badlogic.gdx.graphics.Camera?) : super(com.badlogic.gdx.utils.Scaling.fit, worldWidth, worldHeight, camera) {}
}
