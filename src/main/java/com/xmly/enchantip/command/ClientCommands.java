package com.xmly.enchantip.command;

import com.xmly.enchantip.client.config.EnchantConfigScreen;
import com.xmly.enchantip.handler.HandlerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.xmly.enchantip.Xmly_EnchantmentsTooltip.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
public class ClientCommands {

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {

        event.getDispatcher().register(Commands.literal(MOD_ID)
                .then(Commands.literal("config")
                        .then(Commands.literal("edit")
                                .executes(ctx -> {
                                    Minecraft mc = Minecraft.getInstance();
                                    mc.tell(() -> mc.setScreen(new EnchantConfigScreen(mc.screen)));
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("Edit " + MOD_ID + " config."), true
                                    );
                                    return 1;
                                })
                        )
                        .then(Commands.literal("print")
                                .executes(ctx -> {
                                    Minecraft mc = Minecraft.getInstance();
                                    for (String line : HandlerConfig.getConfigText("\nClient Config:\n附魔ToolTip: ").split("\n")) {
                                        mc.gui.getChat().addMessage(Component.literal(line));
                                    }
                                    return 1;
                                })
                        )
                )
        );
    }
}