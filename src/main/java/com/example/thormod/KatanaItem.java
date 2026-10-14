package com.example.thormod;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class KatanaItem extends Item {
    /** Zusatzschaden (Netherite-Basis kommt obendrauf). */
    public static final float BONUS_DAMAGE = 10.0F;
    /** Sehr hoher Angriffsspeed = kein Angriffs-Cooldown, Spam-Klicken wie in 1.8. */
    public static final float ATTACK_SPEED = 20.0F;

    public KatanaItem(Properties props) {
        super(props);
    }

    /** Rechtsklick: Schild + Unsterblichkeit, bis das Katana aus der Hand genommen wird. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && !PlayerEffects.isShielded(player)) {
            PlayerEffects.activateShield(player);
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }
}
