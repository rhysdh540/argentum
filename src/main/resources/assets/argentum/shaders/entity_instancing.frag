#version 120
#ifdef TEXTURE_ARRAY
#extension GL_EXT_texture_array : require
#endif

uniform sampler2D uTexture;
uniform sampler2D uLightmap;
#ifdef TEXTURE_ARRAY
uniform sampler2DArray uTextureArray;
uniform bool uTextureArrayEnabled;
#endif
uniform int uFogMode;
uniform vec4 uFogColor;
uniform vec3 uFogParameters;
uniform bool uEmissive;

varying vec2 vTexCoord;
varying vec2 vLightCoord;
varying float vTextureLayer;
varying float vLighting;
varying float vFogDistance;
varying vec4 vColor;
varying vec4 vOverlay;

void main() {
    vec4 color;
#ifdef TEXTURE_ARRAY
    if (uTextureArrayEnabled) {
        color = texture2DArray(uTextureArray, vec3(vTexCoord, vTextureLayer));
    } else {
        color = texture2D(uTexture, vTexCoord);
    }
#else
    color = texture2D(uTexture, vTexCoord);
#endif
    color *= vColor;
    if (color.a <= 0.1) {
        discard;
    }

    if (!uEmissive) {
        color.rgb *= texture2D(uLightmap, vLightCoord).rgb * vLighting;
    }
    color.rgb = mix(color.rgb, vOverlay.rgb, vOverlay.a);
    float fog = 1.0;
    if (uFogMode == 9729) {
        fog = clamp((uFogParameters.y - vFogDistance) / (uFogParameters.y - uFogParameters.x), 0.0, 1.0);
    } else if (uFogMode == 2048) {
        fog = clamp(exp(-uFogParameters.z * vFogDistance), 0.0, 1.0);
    } else if (uFogMode == 2049) {
        float density = uFogParameters.z * vFogDistance;
        fog = clamp(exp(-density * density), 0.0, 1.0);
    }
    gl_FragColor = mix(uFogColor, color, fog);
}
