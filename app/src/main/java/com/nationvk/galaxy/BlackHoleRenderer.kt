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

    fun updateVisuals(zoom: Float, speed: Float, intensity: Float, particles: Float) {
        galaxyRenderer.zoom = zoom
        galaxyRenderer.speed = speed
        galaxyRenderer.intensity = intensity
        galaxyRenderer.particleDensity = particles
        requestRender()
    }

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
    private var surfaceWidth = 1
    private var surfaceHeight = 1
    @Volatile var zoom = 1.0f
    @Volatile var speed = 1.0f
    @Volatile var intensity = 1.0f
    @Volatile var particleDensity = 1.0f
    private var zoomLocation = 0
    private var speedLocation = 0
    private var intensityLocation = 0
    private var particleLocation = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.008f, 0.008f, 0.025f, 1f)
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.put(vertices).position(0)
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        resolutionLocation = GLES20.glGetUniformLocation(program, "uResolution")
        timeLocation = GLES20.glGetUniformLocation(program, "uTime")
        zoomLocation = GLES20.glGetUniformLocation(program, "uZoom")
        speedLocation = GLES20.glGetUniformLocation(program, "uSpeed")
        intensityLocation = GLES20.glGetUniformLocation(program, "uIntensity")
        particleLocation = GLES20.glGetUniformLocation(program, "uParticles")
        startedAt = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (program == 0) return
        GLES20.glUseProgram(program)
        GLES20.glUniform2f(resolutionLocation, surfaceWidth.toFloat(), surfaceHeight.toFloat())
        val elapsed = (System.nanoTime() - startedAt) / 1_000_000_000f
        GLES20.glUniform1f(timeLocation, elapsed)
        GLES20.glUniform1f(zoomLocation, zoom)
        GLES20.glUniform1f(speedLocation, speed)
        GLES20.glUniform1f(intensityLocation, intensity)
        GLES20.glUniform1f(particleLocation, particleDensity)
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
            uniform float uZoom;
            uniform float uSpeed;
            uniform float uIntensity;
            uniform float uParticles;

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
                vec2 uv = (vUv - 0.5) * vec2(uResolution.x/uResolution.y, 1.0) / max(uZoom, 0.5);
                float t = uTime * 0.30 * uSpeed;
                float r = length(uv);
                float a = atan(uv.y, uv.x);

                // Deep-space star field with subtle twinkle.
                vec2 starCell = floor((uv + 1.5) * uResolution * 0.42);
                float starSeed = hash(starCell);
                float stars = step(0.9965, starSeed);
                float twinkle = 0.55 + 0.45*sin(uTime*2.2 + starSeed*90.0);
                vec3 col = vec3(0.004, 0.006, 0.022);
                col += stars * twinkle * vec3(0.45,0.66,1.0) * uIntensity;

                // Gravitationally warped background and a soft outer corona.
                float bend = 0.075 / max(r, 0.06);
                float warpedAngle = a + bend;
                float warpedR = r + bend*0.17;
                float lensWave = sin(warpedAngle*9.0 + t*0.7);
                float corona = exp(-pow((r-0.34)*8.0,2.0));
                col += vec3(0.13,0.20,0.72)*corona*(0.22+0.12*lensWave);

                // Wide, tilted accretion disk with moving turbulent bands.
                float diskY = uv.y + 0.075*sin(uv.x*2.4 + t*0.8);
                float diskR = length(vec2(uv.x, diskY*2.0));
                float angle = atan(diskY*2.0, uv.x);
                float turbulence = noise(vec2(angle*6.0 - t*2.8, diskR*27.0 + t*2.1));
                float spiral = 0.5 + 0.5*sin(angle*5.0 - t*2.0 + diskR*24.0);
                float ring = exp(-pow((diskR-0.39)*15.0, 2.0));
                float inner = smoothstep(0.145, 0.215, diskR);
                float outer = 1.0-smoothstep(0.49, 0.62, diskR);
                float brightness = ring * inner * outer * (0.34 + 0.72*turbulence + 0.22*spiral);
                vec3 hot = mix(vec3(0.30,0.08,0.92), vec3(0.08,0.72,1.0), 0.5+0.5*sin(angle-t));
                hot = mix(hot, vec3(1.0,0.67,0.35), pow(max(0.0,cos(angle-t*0.9)), 5.0)*0.72);
                col += hot * brightness * 2.45 * uIntensity;

                // Orbiting luminous knots and animated particle streaks.
                for (int i=0; i<12; i++) {
                    if (float(i) > uParticles * 12.0) continue;
                    float fi = float(i);
                    float orbitA = t*(0.35+fi*0.025) + fi*0.897;
                    float orbitR = 0.27 + 0.018*sin(t*1.4+fi*2.0);
                    vec2 particle = vec2(cos(orbitA), sin(orbitA)*0.48)*orbitR;
                    float glow = exp(-length(uv-particle)*65.0);
                    col += mix(vec3(0.12,0.55,1.0),vec3(1.0,0.42,0.18),fi/7.0)*glow*0.75;
                }

                // Bright photon ring, pulsing halo and deep event horizon.
                float pulse = 0.82 + 0.18*sin(uTime*2.0);
                float photon = exp(-pow((r-0.165)*150.0, 2.0));
                col += vec3(0.42,0.76,1.0)*photon*pulse*1.15*uIntensity;
                float horizon = smoothstep(0.153, 0.177, r);
                col *= horizon;
                float halo = exp(-pow((r-0.205)*11.0,2.0));
                col += vec3(0.28,0.13,0.78)*halo*(0.20+0.08*sin(uTime*1.5));

                // Subtle radial energy rays.
                float rays = pow(max(0.0, sin(a*18.0+t*0.35)), 18.0);
                col += vec3(0.10,0.24,0.72)*rays*corona*0.18;
                col *= 1.0 - smoothstep(0.58, 1.05, length(uv))*0.62;
                gl_FragColor = vec4(col, 1.0);
            }
        """
    }
}
