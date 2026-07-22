#version 120

attribute vec2 aCorner;
attribute vec4 aBounds0;
attribute vec4 aBounds1;
attribute vec2 aShadow;

varying vec2 vTexCoord;
varying float vAlpha;
varying float vFogDistance;

void main() {
    vec3 position = vec3(
        mix(aBounds0.x, aBounds0.w, aCorner.x),
        aBounds0.y,
        mix(aBounds0.z, aBounds1.x, aCorner.y)
    );
    vec4 eyePosition = gl_ModelViewMatrix * vec4(position, 1.0);
    gl_Position = gl_ProjectionMatrix * eyePosition;
    vTexCoord = (gl_TextureMatrix[0] * vec4(
        mix(aBounds1.y, aBounds1.w, aCorner.x),
        mix(aBounds1.z, aShadow.x, aCorner.y),
        0.0,
        1.0
    )).xy;
    vAlpha = aShadow.y;
    vFogDistance = abs(eyePosition.z);
}
