package com.xmly.enchantip.command;

import com.xmly.enchantip.handler.HandlerConfig;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.xmly.enchantip.Xmly_EnchantmentsTooltip.MOD_ID;
import static com.xmly.enchantip.Xmly_EnchantmentsTooltip.MOD_NAME;

public class ServerCommand {

    public ServerCommand() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {

        event.getDispatcher().register(Commands.literal(MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(context -> {
                            boolean success = HandlerConfig.reloadFromFile();
                            context.getSource().sendSuccess(() ->
                                    Component.literal(MOD_NAME + " config file reload: " + success), true);
                            return 1;
                        })
                )
                .then(Commands.literal("print")
                        .executes(ctx -> {
                            for (String line : HandlerConfig.getConfigText("\nServer Config:\n不合并附魔等级: ").split("\n")) {
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal(line), true
                                );
                            }
                            return 1;
                        })
                )
        );
    }
}
