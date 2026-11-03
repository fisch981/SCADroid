package de.fisch981.scadroid.engine

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun plus(v: Vector3) = Vector3(x + v.x, y + v.y, z + v.z)
    operator fun minus(v: Vector3) = Vector3(x - v.x, y - v.y, z - v.z)
    operator fun unaryMinus() = Vector3(-x, -y, -z)
    operator fun times(s: Float) = Vector3(x * s, y * s, z * s)
    operator fun div(s: Float) = Vector3(x / s, y / s, z / s)

    fun dot(v: Vector3): Float = x * v.x + y * v.y + z * v.z

    fun cross(v: Vector3): Vector3 = Vector3(
        y * v.z - z * v.y,
        z * v.x - x * v.z,
        x * v.y - y * v.x
    )

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): Vector3 {
        val len = length()
        return if (len > 1e-6f) this / len else Vector3(0f, 0f, 1f)
    }

    fun rotated(degX: Float, degY: Float, degZ: Float): Vector3 {
        var res = this
        // Rotate X
        if (degX != 0f) {
            val rad = Math.toRadians(degX.toDouble()).toFloat()
            val c = cos(rad)
            val s = sin(rad)
            res = Vector3(res.x, res.y * c - res.z * s, res.y * s + res.z * c)
        }
        // Rotate Y
        if (degY != 0f) {
            val rad = Math.toRadians(degY.toDouble()).toFloat()
            val c = cos(rad)
            val s = sin(rad)
            res = Vector3(res.x * c + res.z * s, res.y, -res.x * s + res.z * c)
        }
        // Rotate Z
        if (degZ != 0f) {
            val rad = Math.toRadians(degZ.toDouble()).toFloat()
            val c = cos(rad)
            val s = sin(rad)
            res = Vector3(res.x * c - res.y * s, res.x * s + res.y * c, res.z)
        }
        return res
    }

    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val UNIT_X = Vector3(1f, 0f, 0f)
        val UNIT_Y = Vector3(0f, 1f, 0f)
        val UNIT_Z = Vector3(0f, 0f, 1f)
    }
}
