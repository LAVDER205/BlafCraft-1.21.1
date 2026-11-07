package net.blafteam.blafcraft.duel;

import net.blafteam.blafcraft.BlafCraft;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = BlafCraft.MODID)
public final class DuelCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("duel")
                .executes(context -> DuelService.showMenu(context.getSource().getPlayerOrException()))
                .then(Commands.literal("challenge").then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> DuelService.challenge(context.getSource().getPlayerOrException(),
                                EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("accept").then(Commands.argument("token", UuidArgument.uuid())
                        .executes(context -> DuelService.respond(context.getSource().getPlayerOrException(),
                                UuidArgument.getUuid(context, "token"), true))))
                .then(Commands.literal("decline").then(Commands.argument("token", UuidArgument.uuid())
                        .executes(context -> DuelService.respond(context.getSource().getPlayerOrException(),
                                UuidArgument.getUuid(context, "token"), false))))
                .then(Commands.literal("stop").then(Commands.argument("session", UuidArgument.uuid())
                        .executes(context -> DuelService.stop(context.getSource().getPlayerOrException(),
                                UuidArgument.getUuid(context, "session"))))));
    }
}
