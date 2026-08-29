package dev.rdh.cera.modules.random;

import dev.rdh.cera.Cera;
import dev.rdh.cera.entity.EntityContext;
import dev.rdh.cera.entity.SpawnSnapshot;
import dev.rdh.cera.mixin.VillagerEntityAccessor;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.InventoryBlockEntity;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.mob.monster.SlimeEntity;
import net.minecraft.entity.living.mob.passive.VillagerEntity;
import net.minecraft.entity.living.mob.passive.animal.SheepEntity;
import net.minecraft.entity.living.mob.passive.animal.tameable.WolfEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public final class Subject {
    private final Entity entity;
    private final BlockEntity blockEntity;
    private NbtCompound nbt;
    private int metadata = -1;

    private Subject(Entity entity, BlockEntity blockEntity) {
        this.entity = entity;
        this.blockEntity = blockEntity;
    }

    public static Subject current() {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getEntityRenderDispatcher().cera$getEntityContext().current();
        if (entity != null) return new Subject(entity, null);
        BlockEntity blockEntity = BlockEntityRenderDispatcher.INSTANCE.cera$getBlockEntityContext().current();
        return blockEntity == null ? null : new Subject(null, blockEntity);
    }

    public Object key() {
        return this.entity != null ? this.entity.getUuid() : this.blockEntity.getPos();
    }

    public int seed() {
        if (this.entity != null) return EntityContext.seed(this.entity.getUuid());
        return Cera.random(this.blockEntity.getPos(), metadata());
    }

    public World world() {
        return this.entity != null ? this.entity.world : this.blockEntity.getWorld();
    }

    public BlockPos spawnPos() {
        if (this.entity != null) {
            SpawnSnapshot snapshot = snapshot();
            return snapshot == null ? null : snapshot.pos();
        }
        return this.blockEntity.getPos();
    }

    public Biome spawnBiome() {
        if (this.entity != null) {
            SpawnSnapshot snapshot = snapshot();
            return snapshot == null ? null : snapshot.biome();
        }
        World world = this.world();
        return world == null ? null : world.getBiome(this.blockEntity.getPos());
    }

    public String name() {
        if (this.entity != null) {
            return this.entity.hasCustomName() ? this.entity.getCustomName() : null;
        }
        return this.blockEntity instanceof InventoryBlockEntity inventory && inventory.hasCustomName()
                ? inventory.getName() : null;
    }

    public boolean isLiving() {
        return this.entity instanceof LivingEntity;
    }

    public int health() {
        return this.entity instanceof LivingEntity living ? (int) living.getHealth() : -1;
    }

    public int maxHealth() {
        return this.entity instanceof LivingEntity living ? (int) living.getMaxHealth() : -1;
    }

    public Boolean baby() {
        return this.entity instanceof LivingEntity living ? living.isBaby() : null;
    }

    public DyeColor color() {
        if (this.entity instanceof WolfEntity wolf) return wolf.getCollarColor();
        if (this.entity instanceof SheepEntity sheep) return sheep.getColor();
        return null;
    }

    public int slimeSize() {
        return this.entity instanceof SlimeEntity slime ? slime.getSize() : -1;
    }

    public int profession() {
        return this.entity instanceof VillagerEntity villager ? villager.getProfession() : -1;
    }

    public int career() {
        return this.entity instanceof VillagerEntity villager ? ((VillagerEntityAccessor) villager).cera$getCareer() : -1;
    }

    public NbtCompound nbt() {
        if (this.nbt == null) {
            this.nbt = new NbtCompound();
            if (this.entity != null) this.entity.writeNbt(this.nbt);
            else this.blockEntity.writeNbt(this.nbt);
        }
        return this.nbt;
    }

    public BlockState blockState() {
        World world = this.world();
        if (world == null) return null;
        BlockPos pos;
        if (this.entity != null) {
            pos = new BlockPos(MathHelper.floor(this.entity.x), MathHelper.floor(this.entity.y - 0.5), MathHelper.floor(this.entity.z));
        } else {
            pos = this.blockEntity.getPos();
        }
        return world.getBlockState(pos);
    }

    public int metadata() {
        if (this.metadata < 0 && this.blockEntity != null) {
            this.metadata = this.blockEntity.getBlockMetadata();
        }
        return this.metadata;
    }

    private SpawnSnapshot snapshot() {
        return this.entity.world instanceof ClientWorld client
                ? client.cera$getSpawnSnapshots().get(this.entity.getUuid()) : null;
    }
}
