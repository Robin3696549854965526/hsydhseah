package com.example.thormod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ThorHammerItem extends Item {
    public static final double RANGE = 100.0;

    public ThorHammerItem(Properties props) {
        super(props);
    }

    /** Rechtsklick: Blitz auf den angeschauten Block. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        HitResult hit = player.pick(RANGE, 0.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }

        if (level instanceof ServerLevel serverLevel) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
            if (bolt != null) {
                bolt.snapTo(Vec3.atBottomCenterOf(blockHit.getBlockPos().above()));
                if (player instanceof ServerPlayer sp) {
                    bolt.setCause(sp);
                }
                serverLevel.addFreshEntity(bolt);
            }
            player.getCooldowns().addCooldown(player.getItemInHand(hand), 20);
        }
        return InteractionResult.SUCCESS;
    }
}
