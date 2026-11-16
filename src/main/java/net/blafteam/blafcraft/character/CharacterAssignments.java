package net.blafteam.blafcraft.character;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.blafteam.blafcraft.BlafCraft;
import net.blafteam.blafcraft.component.ModAttachments;
import net.blafteam.blafcraft.feature.frigby.RelicCharge;
import net.blafteam.blafcraft.social.SocialData;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = BlafCraft.MODID)
public class CharacterAssignments {
    public static CharacterKit get(ServerPlayer player) {
        String assignedId = player.getData(ModAttachments.CHARACTER_ID.get());
        if (!assignedId.isEmpty()) {
            // Неизвестный ID из старого сохранения закрывает доступ, пока админ не переназначит набор.
            return CharacterKit.byId(assignedId).orElse(CharacterKit.NONE);
        }

        // Совместимость со старыми мирами. Явное назначение, включая none, важнее ника.
        // Добавлять новых игроков сюда не нужно: команда сохраняет выбор в данных игрока.
        return switch (player.getGameProfile().getName()) {
            case "Frigby" -> CharacterKit.FRIGBY;
            case "Nice_00" -> CharacterKit.FRYNOTE;
            case "LAVDER208" -> CharacterKit.LAVDER;
            case "Dev" -> CharacterKit.DEV;
            default -> CharacterKit.NONE;
        };
    }

    private static void syncHud(ServerPlayer player) {
        CharacterKit kit = get(player);
        SocialData.get(player.serverLevel().getServer()).rememberFrigby(player.getUUID(), kit == CharacterKit.FRIGBY);
        PacketDistributor.sendToPlayer(player, new CharacterHudPayload(kit.id()));
        RelicCharge.sync(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncHud(player);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("blafcraft")
                .then(Commands.literal("character")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("kit", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (CharacterKit kit : CharacterKit.values()) {
                                                if (kit.id().startsWith(builder.getRemaining())) builder.suggest(kit.id());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                            String id = StringArgumentType.getString(context, "kit");
                                            if (CharacterKit.byId(id).isEmpty()) {
                                                context.getSource().sendFailure(Component.literal("Неизвестный персонаж: " + id));
                                                return 0;
                                            }
                                            player.setData(ModAttachments.CHARACTER_ID.get(), id);
                                            syncHud(player);
                                            context.getSource().sendSuccess(() -> Component.literal(
                                                    player.getGameProfile().getName() + " → " + id), true);
                                            return 1;
                                        })))));
    }
}
