package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.RequiresPermission;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;

@Command("audioplayer")
public class UtilityCommands {

    @RequiresPermission("audioplayer.apply")
    @Command("clear")
    public void clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (!(itemInHand.getItem() instanceof RecordItem) && !(itemInHand.getItem() instanceof InstrumentItem)) {
            context.getSource().sendFailure(Component.literal("Недопустимый предмет"));
            return;
        }

        if (!itemInHand.hasTag()) {
            context.getSource().sendFailure(Component.literal("ПРедмет не содержит данных NBT"));
            return;
        }

        CompoundTag tag = itemInHand.getTag();

        if (tag == null) {
            return;
        }

        if (!tag.contains("CustomSound")) {
            context.getSource().sendFailure(Component.literal("У предмета нет пользовательского звука"));
            return;
        }

        tag.remove("CustomSound");
        tag.remove("CustomSoundRange");
        tag.remove("IsStaticCustomSound");

        if (itemInHand.getItem() instanceof InstrumentItem) {
            tag.putString("instrument", "minecraft:ponder_goat_horn");
        }

        tag.remove(ItemStack.TAG_DISPLAY);
        tag.remove("HideFlags");

        context.getSource().sendSuccess(() -> Component.literal("Предмет успешно очищен"), false);
    }

    @Command("id")
    public void id(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (!(itemInHand.getItem() instanceof RecordItem) && !(itemInHand.getItem() instanceof InstrumentItem)) {
            context.getSource().sendFailure(Component.literal("Недопустимый предмет"));
            return;
        }

        if (!itemInHand.hasTag()) {
            context.getSource().sendFailure(Component.literal("У предмета нет пользовательского звука"));
            return;
        }

        CompoundTag tag = itemInHand.getTag();

        if (tag == null) {
            return;
        }

        if (!tag.contains("CustomSound")) {
            context.getSource().sendFailure(Component.literal("У предмета нет пользовательского звука"));
            return;
        }

        context.getSource().sendSuccess(() -> UploadCommands.sendUUIDMessage(tag.getUUID("CustomSound"), Component.literal("Successfully extracted sound ID.")), false);
    }

}
