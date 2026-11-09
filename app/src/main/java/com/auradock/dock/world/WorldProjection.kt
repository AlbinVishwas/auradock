package com.auradock.dock.world

/** A place on the earth, in degrees. */
data class GeoPoint(val lat: Double, val lon: Double)

/**
 * The map's projection, in the design frame's own units (915 x 412, equirectangular: 2.1774 px per degree both ways, longitude 0 at
 * x 476.9, the equator at y 228.8). Fitted to the frames' London, San Francisco and Navi Mumbai dots.
 */
object WorldProjection {
    const val DESIGN_WIDTH = 915f
    const val DESIGN_HEIGHT = 412f
    const val PX_PER_DEGREE = 2.1774f
    const val X_OF_LON_0 = 476.9f
    const val Y_OF_LAT_0 = 228.8f

    /** The dot grid: 14 px apart, the first dot at (92, 66). */
    const val GRID = 14f
    const val GRID_X0 = 92f
    const val GRID_Y0 = 66f

    fun x(lon: Double): Float = X_OF_LON_0 + PX_PER_DEGREE * lon.toFloat()
    fun y(lat: Double): Float = Y_OF_LAT_0 - PX_PER_DEGREE * lat.toFloat()
    fun lonOfX(x: Float): Double = ((x - X_OF_LON_0) / PX_PER_DEGREE).toDouble()
    fun latOfY(y: Float): Double = ((Y_OF_LAT_0 - y) / PX_PER_DEGREE).toDouble()
}
