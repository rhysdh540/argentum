#version 120

uniform sampler2D uTexture;
uniform int uFogMode;
uniform vec4 uFogColor;
uniform vec3 uFogParameters;

varying vec2 vTexCoord;
varying float vAlpha;
varying float vFogDistance;

void main() {
    vec4 color = texture2D(uTexture, vTexCoord);
    color.a *= vAlpha;
    if (color.a <= 0.1) {
        discard;
    }

    float fog = 1.0;
    if (uFogMode == 9729) {
        fog = clamp((uFogParameters.y - vFogDistance) / (uFogParameters.y - uFogParameters.x), 0.0, 1.0);
    } else if (uFogMode == 2048) {
        fog = clamp(exp(-uFogParameters.z * vFogDistance), 0.0, 1.0);
    } else if (uFogMode == 2049) {
        float density = uFogParameters.z * vFogDistance;
        fog = clamp(exp(-density * density), 0.0, 1.0);
    }
    color.rgb = mix(uFogColor.rgb, color.rgb, fog);
    gl_FragColor = color;
}
