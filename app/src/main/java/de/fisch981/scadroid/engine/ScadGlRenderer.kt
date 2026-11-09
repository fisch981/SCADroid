package de.fisch981.scadroid.engine

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class ScadGlRenderer : GLSurfaceView.Renderer {

    @Volatile
    var currentMesh: TriangleMesh = TriangleMesh.EMPTY

    var yaw = 45f       // Drehung um Z
    var pitch = 30f     // Neigung über XY-Ebene
    var distance = 80f  // Kameraabstand
    var panX = 0f
    var panY = 0f
    var panZ = 0f

    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    private var solidProgram = 0
    private var uMVPMatrixLoc = 0
    private var uModelMatrixLoc = 0

    private var lineProgram = 0
    private var uLineMVPLoc = 0
    private var uLineColorLoc = 0

    private var gridVertexBuffer: FloatBuffer? = null
    private var gridVertexCount = 0

    private var axesVertexBuffer: FloatBuffer? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // CAD Dark Slate Hintergrund
        GLES30.glClearColor(0.10f, 0.11f, 0.13f, 1.0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)

        initShaders()
        buildGridAndAxes()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / max(height.toFloat(), 1f)
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, 1f, 1000f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // Kamera-Positionierung im sphärischen System (Z-Up wie in OpenSCAD)
        val radPitch = Math.toRadians(pitch.toDouble()).toFloat()
        val radYaw = Math.toRadians(yaw.toDouble()).toFloat()

        val eyeX = panX + distance * cos(radPitch) * sin(radYaw)
        val eyeY = panY - distance * cos(radPitch) * cos(radYaw)
        val eyeZ = panZ + distance * sin(radPitch)

        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, panX, panY, panZ, 0f, 0f, 1f)

        // 1. Grid & Achsen zeichnen
        drawGridAndAxes()

        // 2. OpenSCAD 3D-Mesh zeichnen
        val mesh = currentMesh
        if (mesh.triangleCount > 0) {
            drawMesh(mesh)
        }
    }

    private fun drawGridAndAxes() {
        GLES30.glUseProgram(lineProgram)
        GLES30.glDisable(GLES30.GL_CULL_FACE)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        GLES30.glUniformMatrix4fv(uLineMVPLoc, 1, false, mvpMatrix, 0)

        // Grid-Linien
        gridVertexBuffer?.let { buf ->
            buf.position(0)
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, buf)
            GLES30.glEnableVertexAttribArray(0)

            GLES30.glUniform4f(uLineColorLoc, 0.22f, 0.24f, 0.28f, 1.0f)
            GLES30.glLineWidth(1.0f)
            GLES30.glDrawArrays(GLES30.GL_LINES, 0, gridVertexCount)
        }

        // Koordinatenachsen (X = Rot, Y = Grün, Z = Blau)
        axesVertexBuffer?.let { buf ->
            buf.position(0)
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, buf)
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glLineWidth(3.0f)

            // X-Achse (Rot)
            GLES30.glUniform4f(uLineColorLoc, 0.90f, 0.25f, 0.20f, 1.0f)
            GLES30.glDrawArrays(GLES30.GL_LINES, 0, 2)

            // Y-Achse (Grün)
            GLES30.glUniform4f(uLineColorLoc, 0.25f, 0.75f, 0.30f, 1.0f)
            GLES30.glDrawArrays(GLES30.GL_LINES, 2, 2)

            // Z-Achse (Blau)
            GLES30.glUniform4f(uLineColorLoc, 0.20f, 0.55f, 0.95f, 1.0f)
            GLES30.glDrawArrays(GLES30.GL_LINES, 4, 2)
        }
    }

    private fun drawMesh(mesh: TriangleMesh) {
        GLES30.glUseProgram(solidProgram)
        GLES30.glEnable(GLES30.GL_CULL_FACE)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        GLES30.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(uModelMatrixLoc, 1, false, modelMatrix, 0)

        // Vertices
        mesh.vertexBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, mesh.vertexBuffer)
        GLES30.glEnableVertexAttribArray(0)

        // Normals
        mesh.normalBuffer.position(0)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 0, mesh.normalBuffer)
        GLES30.glEnableVertexAttribArray(1)

        // Colors
        mesh.colorBuffer.position(0)
        GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, 0, mesh.colorBuffer)
        GLES30.glEnableVertexAttribArray(2)

        // 1. Opaque Pass (Normale CSG-Geometrie)
        if (mesh.opaqueVertexCount > 0) {
            GLES30.glDisable(GLES30.GL_BLEND)
            GLES30.glDepthMask(true)
            GLES30.glEnable(GLES30.GL_CULL_FACE)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, mesh.opaqueVertexCount)
        }

        // 2. Transparent Overlay Pass ('#' Highlight/Debug & '%' Background Modifier)
        val overlayCount = mesh.vertexCount - mesh.opaqueVertexCount
        if (overlayCount > 0) {
            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            GLES30.glEnable(GLES30.GL_POLYGON_OFFSET_FILL)
            GLES30.glPolygonOffset(-1.0f, -1.0f)
            GLES30.glDepthMask(false)
            GLES30.glEnable(GLES30.GL_CULL_FACE)
            GLES30.glCullFace(GLES30.GL_BACK)

            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, mesh.opaqueVertexCount, overlayCount)

            GLES30.glDepthMask(true)
            GLES30.glDisable(GLES30.GL_POLYGON_OFFSET_FILL)
            GLES30.glDisable(GLES30.GL_BLEND)
        }

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glDisableVertexAttribArray(2)
    }

    fun resetCamera(mesh: TriangleMesh) {
        panX = mesh.center.x
        panY = mesh.center.y
        panZ = mesh.center.z
        distance = max(mesh.boundingRadius * 2.8f, 30f)
        pitch = 30f
        yaw = 45f
    }

    /**
     * Verschiebt den 3D-Drehpunkt (Pan) der Kamera anhand von 2-Finger-Touchbewegungen.
     * Berücksichtigt die aktuelle Ausrichtung von Yaw und Pitch der Kamera.
     */
    fun panCamera(deltaScreenX: Float, deltaScreenY: Float) {
        val scale = (distance / 400f).coerceIn(0.04f, 0.40f)
        val radPitch = Math.toRadians(pitch.toDouble()).toFloat()
        val radYaw = Math.toRadians(yaw.toDouble()).toFloat()

        // Kamera-Rechts-Vektor
        val rightX = cos(radYaw)
        val rightY = sin(radYaw)

        // Kamera-Aufwärts-Vektor (Z-Up System)
        val upX = -sin(radPitch) * sin(radYaw)
        val upY = sin(radPitch) * cos(radYaw)
        val upZ = cos(radPitch)

        panX -= (rightX * deltaScreenX - upX * deltaScreenY) * scale
        panY -= (rightY * deltaScreenX - upY * deltaScreenY) * scale
        panZ -= (-upZ * deltaScreenY) * scale
    }

    private fun buildGridAndAxes() {
        val size = 60f
        val step = 10f
        val lines = mutableListOf<Float>()

        var x = -size
        while (x <= size) {
            lines.add(x); lines.add(-size); lines.add(0f)
            lines.add(x); lines.add(size); lines.add(0f)
            x += step
        }
        var y = -size
        while (y <= size) {
            lines.add(-size); lines.add(y); lines.add(0f)
            lines.add(size); lines.add(y); lines.add(0f)
            y += step
        }

        gridVertexCount = lines.size / 3
        gridVertexBuffer = ByteBuffer.allocateDirect(lines.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(lines.toFloatArray())
                position(0)
            }

        // Achsen: (0,0,0)->(X,0,0), (0,0,0)->(0,Y,0), (0,0,0)->(0,0,Z)
        val axisLen = 40f
        val axes = floatArrayOf(
            0f, 0f, 0f, axisLen, 0f, 0f,
            0f, 0f, 0f, 0f, axisLen, 0f,
            0f, 0f, 0f, 0f, 0f, axisLen
        )
        axesVertexBuffer = ByteBuffer.allocateDirect(axes.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(axes)
                position(0)
            }
    }

    private fun initShaders() {
        val vShaderCode = """
            #version 300 es
            layout(location = 0) in vec3 aPosition;
            layout(location = 1) in vec3 aNormal;
            layout(location = 2) in vec4 aColor;
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            out vec3 vNormal;
            out vec4 vColor;
            void main() {
                vNormal = normalize(mat3(uModelMatrix) * aNormal);
                vColor = aColor;
                gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
            }
        """.trimIndent()

        val fShaderCode = """
            #version 300 es
            precision mediump float;
            in vec3 vNormal;
            in vec4 vColor;
            out vec4 fragColor;
            void main() {
                vec3 n = gl_FrontFacing ? normalize(vNormal) : -normalize(vNormal);
                float ambFactor = vColor.a < 0.99 ? 0.45 : 0.38;
                float diffFactor1 = vColor.a < 0.99 ? 0.45 : 0.65;
                float diffFactor2 = vColor.a < 0.99 ? 0.15 : 0.25;

                vec3 ambient = ambFactor * vColor.rgb;
                vec3 lightDir1 = normalize(vec3(0.5, -0.6, 1.0));
                float diff1 = max(dot(n, lightDir1), 0.0);
                vec3 diffuse1 = diff1 * vColor.rgb * diffFactor1;
                
                vec3 lightDir2 = normalize(vec3(-0.6, 0.5, 0.4));
                float diff2 = max(dot(n, lightDir2), 0.0);
                vec3 diffuse2 = diff2 * vColor.rgb * diffFactor2;

                vec3 finalColor = clamp(ambient + diffuse1 + diffuse2, 0.0, 1.0);
                fragColor = vec4(finalColor, vColor.a);
            }
        """.trimIndent()

        solidProgram = createProgram(vShaderCode, fShaderCode)
        uMVPMatrixLoc = GLES30.glGetUniformLocation(solidProgram, "uMVPMatrix")
        uModelMatrixLoc = GLES30.glGetUniformLocation(solidProgram, "uModelMatrix")

        // Line Shader
        val lineV = """
            #version 300 es
            layout(location = 0) in vec3 aPosition;
            uniform mat4 uMVPMatrix;
            void main() {
                gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
            }
        """.trimIndent()

        val lineF = """
            #version 300 es
            precision mediump float;
            uniform vec4 uColor;
            out vec4 fragColor;
            void main() {
                fragColor = uColor;
            }
        """.trimIndent()

        lineProgram = createProgram(lineV, lineF)
        uLineMVPLoc = GLES30.glGetUniformLocation(lineProgram, "uMVPMatrix")
        uLineColorLoc = GLES30.glGetUniformLocation(lineProgram, "uColor")
    }

    private fun createProgram(vCode: String, fCode: String): Int {
        val vShader = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER)
        GLES30.glShaderSource(vShader, vCode)
        GLES30.glCompileShader(vShader)

        val fShader = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER)
        GLES30.glShaderSource(fShader, fCode)
        GLES30.glCompileShader(fShader)

        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, vShader)
        GLES30.glAttachShader(prog, fShader)
        GLES30.glLinkProgram(prog)
        return prog
    }
}
