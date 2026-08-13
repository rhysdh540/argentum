#version 130

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

in vec2 vTexCoord;
in vec2 vLightCoord;
in float vTextureLayer;
in float vLighting;
in float vFogDistance;
in vec4 vColor;
in vec4 vOverlay;

#ifndef LEGACY
out vec4 fragColor;
#else
#define fragColor gl_FragColor
#endif

#ifdef LEGACY
#define textureArray texture2DArray
#else
#define textureArray texture
#endif

void main() {
    vec4 color = texture(uTexture, vTexCoord);
#ifdef TEXTURE_ARRAY
    if (uTextureArrayEnabled) {
        color = textureArray(uTextureArray, vec3(vTexCoord, vTextureLayer));
    }
#endif
    color *= vColor;
    if (color.a <= 0.1) {
        discard;
    }

    if (!uEmissive) {
        color.rgb *= texture(uLightmap, vLightCoord).rgb * vLighting;
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
    fragColor = mix(uFogColor, color, fog);
}
