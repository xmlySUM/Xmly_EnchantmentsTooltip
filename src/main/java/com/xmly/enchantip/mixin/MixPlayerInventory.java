package com.xmly.enchantip.mixin;

import com.xmly.enchantip.client.implement.InterfacePlayerInventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({Player.class})
public class MixPlayerInventory implements InterfacePlayerInventory {
    @Shadow
    @Final
    private Inventory inventory;

    @Override
    public Inventory Xmly_EnchantmentsTooltip$getInv() {
        return this.inventory;
    }
}
