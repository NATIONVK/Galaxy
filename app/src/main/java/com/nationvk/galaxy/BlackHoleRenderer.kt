package com.nationvk.galaxy

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GalaxyGLSurfaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {
    private val galaxyRenderer = BlackHoleRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(galaxyRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }

    override fun onPause() {
        super.onPause()
    }
}

private class BlackHoleRenderer : GLSurfaceView.Renderer {
    private val vertices = floatArrayOf(
        -1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f
    )
    private lateinit var vertexBuffer: FloatBuffer
    private var program = 0
    private var startedAt = 0L
    private var resolutionLocation = 0
    private var timeLocation = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.008f, 0.008f, 0.025f, 1f)
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.put(vertices).position(0)
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        resolutionLocation = GLES20.glGetUniformLocation(program, "uResolution")
        timeLocation = GLES20.glGetUniformLocation(program, "uTime")
        startedAt = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (program == 0) return
        GLES20.glUseProgram(program)
        GLES20.glUniform2f(resolutionLocation, width.toFloat(), height.toFloat())
        val elapsed = (System.nanoTime() - startedAt) / 1_000_000_000f
        GLES20.glUniform1f(timeLocation, elapsed)
        val position = GLES20.glGetAttribLocation(program, "aPosition")
        GLES20.glEnableVertexAttribArray(position)
        GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(position)
    }

    private fun createProgram(vertex: String, fragment: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertex)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragment)
        if (vs == 0 || fs == 0) return 0
        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs)
            GLES20.glAttachShader(it, fs)
            GLES20.glLinkProgram(it)
            val status = IntArray(1)
            GLES20.glGetProgramiv(it, GLES20.GL_LINK_STATUS, status, 0)
            if (status[0] == 0) {
                android.util.Log.e("GalaxyGL", GLES20.glGetProgramInfoLog(it))
                GLES20.glDeleteProgram(it)
                return 0
            }
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            android.util.Log.e("GalaxyGL", GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec2 aPosition;
            varying vec2 vUv;
            void main() {
                vUv = aPosition * 0.5 + 0.5;
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision highp float;
            varying vec2 vUv;
            uniform vec2 uResolution;
            uniform float uTime;

            float hash(vec2 p) {
                return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
            }

            float noise(vec2 p) {
                vec2 i = floor(p), f = fract(p);
                f = f*f*(3.0-2.0*f);
                return mix(mix(hash(i), hash(i+vec2(1.0,0.0)), f.x),
                           mix(hash(i+vec2(0.0,1.0)), hash(i+vec2(1.0,1.0)), f.x), f.y);
            }

            void main() {
                vec2 uv = (vUv - 0.5) * vec2(uResolution.x/uResolution.y, 1.0);
                float t = uTime * 0.22;
                float r = length(uv);
                float a = atan(uv.y, uv.x);

                // Approximate gravitational lensing: bend background rays around the photon ring.
                float bend = 0.055 / max(r, 0.055);
                vec2 warped = vec2(cos(a + bend), sin(a + bend)) * (r + bend*0.20);
                float stars = step(0.9975, hash(floor((warped + 0.5)*uResolution*0.55)));
                vec3 col = vec3(0.006, 0.008, 0.025) + stars*vec3(0.48,0.62,1.0);

                // Thin, tilted accretion disk with turbulent luminous filaments.
                float diskY = uv.y + 0.045*sin(uv.x*3.0 + t);
                float diskR = length(vec2(uv.x, diskY*2.5));
                float angle = atan(diskY*2.5, uv.x);
                float turbulence = noise(vec2(angle*5.0 - t*2.0, diskR*34.0 + t*1.7));
                float ring = exp(-pow((diskR-0.285)*24.0, 2.0));
                float inner = smoothstep(0.145, 0.205, diskR);
                float outer = 1.0-smoothstep(0.39, 0.49, diskR);
                float brightness = ring * inner * outer * (0.42 + 0.85*turbulence);
                vec3 hot = mix(vec3(0.28,0.10,0.95), vec3(0.10,0.78,1.0), 0.5+0.5*sin(angle-t));
                hot = mix(hot, vec3(1.0,0.76,0.48), pow(max(0.0,cos(angle-t)), 5.0)*0.65);
                col += hot * brightness * 2.2;

                // Photon ring and event horizon.
                float photon = exp(-pow((r-0.158)*190.0, 2.0));
                col += vec3(0.30,0.68,1.0)*photon*0.8;
                float horizon = smoothstep(0.151, 0.165, r);
                col *= horizon;
                float halo = exp(-pow((r-0.19)*15.0,2.0));
                col += vec3(0.23,0.12,0.65)*halo*0.18;

                // Soft vignette.
                col *= 1.0 - smoothstep(0.35, 0.85, length(uv))*0.75;
                gl_FragColor = vec4(col, 1.0);
            }
        """
    }
}
