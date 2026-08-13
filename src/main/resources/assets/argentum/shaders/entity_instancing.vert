#version 120

attribute vec3 aPosition;
attribute vec2 aTexCoord;
attribute vec3 aNormal;
attribute vec4 aModel0;
attribute vec4 aModel1;
attribute vec4 aModel2;
attribute vec4 aModel3;
attribute vec3 aLightCoord;
attribute vec4 aColor;
attribute vec4 aVertexColor;
attribute float aEffectTime;
attribute vec4 aOverlay;
uniform int uGlintPass;
uniform int uChargePass;
uniform int uItemGlintPass;
uniform float uItemGlintOffset;
uniform vec3 uLightDirection0;
uniform vec3 uLightDirection1;

varying vec2 vTexCoord;
varying vec2 vLightCoord;
varying float vTextureLayer;
varying float vLighting;
varying float vFogDistance;
varying vec4 vColor;
varying vec4 vOverlay;

void main() {
    mat4 model = mat4(aModel0, aModel1, aModel2, aModel3);
    vec4 eyePosition = gl_ModelViewMatrix * model * vec4(aPosition, 1.0);
    vec3 normal = normalize(gl_NormalMatrix * mat3(model) * aNormal);
    float light0 = max(dot(normal, uLightDirection0), 0.0);
    float light1 = max(dot(normal, uLightDirection1), 0.0);

    gl_Position = gl_ProjectionMatrix * eyePosition;
    if (uGlintPass >= 0) {
        float angle = radians(30.0 - float(uGlintPass) * 60.0);
        vec2 uv = aTexCoord + vec2(0.0, aEffectTime * (0.001 + float(uGlintPass) * 0.003) * 20.0);
        vTexCoord = vec2(cos(angle) * uv.x - sin(angle) * uv.y,
                sin(angle) * uv.x + cos(angle) * uv.y) / 3.0;
    } else if (uItemGlintPass >= 0) {
        float angle = radians(uItemGlintPass == 0 ? -50.0 : 10.0);
        vec2 uv = vec2(cos(angle) * aTexCoord.x - sin(angle) * aTexCoord.y,
                sin(angle) * aTexCoord.x + cos(angle) * aTexCoord.y);
        vTexCoord = (uv + vec2(uItemGlintOffset, 0.0)) * 8.0;
    } else if (uChargePass == 1) {
        vTexCoord = aTexCoord + vec2(aEffectTime * 0.01);
    } else if (uChargePass == 2) {
        vTexCoord = aTexCoord + vec2(cos(aEffectTime * 0.02) * 3.0, aEffectTime * 0.01);
    } else {
        vTexCoord = (gl_TextureMatrix[0] * vec4(aTexCoord, 0.0, 1.0)).xy;
    }
    vLightCoord = (gl_TextureMatrix[1] * vec4(aLightCoord.xy, 0.0, 1.0)).xy;
    vTextureLayer = aLightCoord.z;
    vLighting = min(1.0, (light0 + light1) * 0.6 + 0.4);
    vFogDistance = abs(eyePosition.z);
    vColor = aColor * aVertexColor;
    vOverlay = aOverlay;
}
