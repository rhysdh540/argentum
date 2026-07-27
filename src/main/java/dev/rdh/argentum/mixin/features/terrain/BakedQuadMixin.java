package dev.rdh.argentum.mixin.features.terrain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.texture.TextureAtlasSprite;
import net.minecraft.client.resource.model.BakedQuad;
import net.minecraft.util.math.Direction;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import org.embeddedt.embeddium.impl.util.ModelQuadUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.rdh.argentum.impl.extensions.TextureAtlasExtension;
import dev.rdh.argentum.impl.render.terrain.compile.PrimitiveModelUtil;

@Mixin(BakedQuad.class)
public abstract class BakedQuadMixin implements BakedQuadView {
    @Shadow @Final
    protected int[] vertices;
    @Shadow @Final
    protected int tintIndex;
    @Shadow @Final
    protected Direction face;

    private int celeritas$flags;
    private int celeritas$normal;
    private ModelQuadFacing celeritas$normalFace;
    private TextureAtlasSprite celeritas$sprite;

    @Override
    public float getX(int index) {
        return Float.intBitsToFloat(this.vertices[index * 7]);
    }

    @Override
    public float getY(int index) {
        return Float.intBitsToFloat(this.vertices[index * 7 + 1]);
    }

    @Override
    public float getZ(int index) {
        return Float.intBitsToFloat(this.vertices[index * 7 + 2]);
    }

    @Override
    public int getColor(int index) {
        return this.vertices[index * 7 + 3];
    }

    @Override
    public float getTexU(int index) {
        return Float.intBitsToFloat(this.vertices[index * 7 + 4]);
    }

    @Override
    public float getTexV(int index) {
        return Float.intBitsToFloat(this.vertices[index * 7 + 5]);
    }

    @Override
    public int getLight(int index) {
        return this.vertices[index * 7 + 6];
    }

    @Override
    public int getFlags() {
        int flags = this.celeritas$flags;
        if ((flags & ModelQuadFlags.IS_POPULATED) == 0) {
            this.celeritas$flags = flags = ModelQuadFlags.getQuadFlags(this, this.getLightFace(), flags);
        }
        return flags;
    }

    @Override
    public void addFlags(int flags) {
        this.celeritas$flags |= flags;
    }

    @Override
    public int getColorIndex() {
        return this.tintIndex;
    }

    @Override
    public Object celeritas$getSprite() {
        if (this.celeritas$sprite == null) {
            float u = 0.0F;
            float v = 0.0F;
            for (int i = 0; i < 4; i++) {
                u += this.getTexU(i);
                v += this.getTexV(i);
            }
            TextureAtlasExtension atlas = (TextureAtlasExtension)Minecraft.getInstance().getBlocksAtlas();
            TextureAtlasSprite sprite = atlas.celeritas$findFromUV(u * 0.25F, v * 0.25F);
            this.celeritas$sprite = sprite;
            if (sprite != null && this.isInside(sprite)) {
                this.celeritas$flags |= ModelQuadFlags.IS_TRUSTED_SPRITE;
            }
        }
        return this.celeritas$sprite;
    }

    private boolean isInside(TextureAtlasSprite sprite) {
        for (int i = 0; i < 4; i++) {
            float u = this.getTexU(i);
            float v = this.getTexV(i);
            if (u < sprite.getUMin() || u > sprite.getUMax() || v < sprite.getVMin() || v > sprite.getVMax()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ModelQuadFacing getLightFace() {
        return PrimitiveModelUtil.fromDirection(this.face);
    }

    @Override
    public ModelQuadFacing getNormalFace() {
        if (this.celeritas$normalFace == null) {
            this.celeritas$normalFace = ModelQuadUtil.findNormalFace(this.getComputedFaceNormal());
        }
        return this.celeritas$normalFace;
    }

    @Override
    public int getForgeNormal(int index) {
        return 0;
    }

    @Override
    public int getComputedFaceNormal() {
        if (this.celeritas$normal == 0) {
            this.celeritas$normal = ModelQuadUtil.calculateNormal(this);
        }
        return this.celeritas$normal;
    }

    @Override
    public boolean hasShade() {
        return true;
    }

    @Override
    public int getVerticesCount() {
        return 4;
    }

    @Override
    public SpriteTransparencyLevel getTransparencyLevel() {
        Object sprite = this.celeritas$getSprite();
        return sprite == null ? null : SpriteTransparencyLevel.Holder.getTransparencyLevel(sprite);
    }

}
