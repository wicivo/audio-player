package de.maxhenkel.audioplayer.command;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.Name;
import de.maxhenkel.admiral.annotations.RequiresPermission;
import de.maxhenkel.audioplayer.AudioManager;
import de.maxhenkel.audioplayer.AudioPlayer;
import de.maxhenkel.audioplayer.Filebin;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.net.UnknownHostException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Command("audioplayer")
public class UploadCommands {

    public static final Pattern SOUND_FILE_PATTERN = Pattern.compile("^[a-z0-9_ -]+.((wav)|(mp3))$", Pattern.CASE_INSENSITIVE);

    @RequiresPermission("audioplayer.upload")
    @Command
    public void audioPlayer(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() ->
                        Component.literal("Загрузить аудиофайл через Filebin ")
                                .append(Component.literal("здесь").withStyle(style -> {
                                    return style
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/audioplayer upload"))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Нажмите, чтобы показать подробнее")));
                                }).withStyle(ChatFormatting.GREEN))
                                .append(".")
                , false);
        context.getSource().sendSuccess(() ->
                        Component.literal("Загрузить аудиофайл с доступом к файловой системе сервера(Доступно только для администрации) ")
                                .append(Component.literal("здесь").withStyle(style -> {
                                    return style
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/audioplayer serverfile"))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Нажмите, чтобы показать подробнее")));
                                }).withStyle(ChatFormatting.GREEN))
                                .append(".")
                , false);
        context.getSource().sendSuccess(() ->
                        Component.literal("Загрузить аудиофайл с URL-адреса ")
                                .append(Component.literal("здесь").withStyle(style -> {
                                    return style
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/audioplayer url"))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Нажмите, чтобы показать подробнее")));
                                }).withStyle(ChatFormatting.GREEN))
                                .append(".")
                , false);
    }

    @RequiresPermission("audioplayer.upload")
    @Command("upload")
    @Command("filebin")
    public void filebin(CommandContext<CommandSourceStack> context) {
        UUID uuid = UUID.randomUUID();
        String uploadURL = Filebin.getBin(uuid);

        MutableComponent msg = Component.literal("Нажмите на ")
                .append(Component.literal("эту ссылку")
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, uploadURL))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Нажмите чтобы открыть")));
                        })
                        .withStyle(ChatFormatting.GREEN)
                )
                .append(" и загрузите свой звук в формате ")
                .append(Component.literal("mp3").withStyle(ChatFormatting.GRAY))
                .append(" или ")
                .append(Component.literal("wav").withStyle(ChatFormatting.GRAY))
                .append(".\n")
                .append("После загрузки файла, нажмите ")
                .append(Component.literal("сюда")
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/audioplayer filebin " + uuid))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Нажмите, чтобы подтвердить загрузку")));
                        })
                        .withStyle(ChatFormatting.GREEN)
                )
                .append(".");

        context.getSource().sendSuccess(() -> msg, false);
    }

    @RequiresPermission("audioplayer.upload")
    @Command("filebin")
    public void filebinUpload(CommandContext<CommandSourceStack> context, @Name("id") UUID sound) {
        new Thread(() -> {
            try {
                context.getSource().sendSuccess(() -> Component.literal("Скачивание звука, пожалуйста, подождите..."), false);
                Filebin.downloadSound(context.getSource().getServer(), sound);
                context.getSource().sendSuccess(() -> sendUUIDMessage(sound, Component.literal("Звук успешно скачан.")), false);
            } catch (Exception e) {
                AudioPlayer.LOGGER.warn("{} не удалось скачать звук: {}", context.getSource().getTextName(), e.getMessage());
                context.getSource().sendFailure(Component.literal("Не удалось скачать звук: %s".formatted(e.getMessage())));
            }
        }).start();
    }

    @RequiresPermission("audioplayer.upload")
    @Command("url")
    public void url(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() ->
                        Component.literal("Если у вас есть прямая ссылка на ")
                                .append(Component.literal(".mp3").withStyle(ChatFormatting.GRAY))
                                .append(" или ")
                                .append(Component.literal(".wav").withStyle(ChatFormatting.GRAY))
                                .append(" файл, введите следующую команду: ")
                                .append(Component.literal("/audioplayer url <ссылка в кавычках>").withStyle(ChatFormatting.GRAY).withStyle(style -> {
                                    return style
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/audioplayer url "))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Нажмите для заполнения команды")));
                                }))
                                .append(".")
                , false);
    }

    @RequiresPermission("audioplayer.upload")
    @Command("url")
    public void urlUpload(CommandContext<CommandSourceStack> context, @Name("url") String url) {
        UUID sound = UUID.randomUUID();
        new Thread(() -> {
            try {
                context.getSource().sendSuccess(() -> Component.literal("Скачивание звука, пожалуйста, подождите..."), false);
                AudioManager.saveSound(context.getSource().getServer(), sound, url);
                context.getSource().sendSuccess(() -> sendUUIDMessage(sound, Component.literal("Звук успешно скачан.")), false);
            } catch (UnknownHostException e) {
                AudioPlayer.LOGGER.warn("{} не удалось скачать звук: {}", context.getSource().getTextName(), e.toString());
                context.getSource().sendFailure(Component.literal("Не удалось скачать звук: Неизвестный хост"));
            } catch (UnsupportedAudioFileException e) {
                AudioPlayer.LOGGER.warn("{} не удалось скачать звук: {}", context.getSource().getTextName(), e.toString());
                context.getSource().sendFailure(Component.literal("Не удалось скачать звук: Недопустимый формат файла"));
            } catch (Exception e) {
                AudioPlayer.LOGGER.warn("{} не удалось скачать звук: {}", context.getSource().getTextName(), e.toString());
                context.getSource().sendFailure(Component.literal("Не удалось скачать звук: %s".formatted(e.getMessage())));
            }
        }).start();
    }

    @RequiresPermission("audioplayer.upload")
    @Command("serverfile")
    public void serverFile(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() ->
                        Component.literal("Загрузите ")
                                .append(Component.literal(".mp3").withStyle(ChatFormatting.GRAY))
                                .append(" или ")
                                .append(Component.literal(".wav").withStyle(ChatFormatting.GRAY))
                                .append(" файл в ")
                                .append(Component.literal(AudioManager.getUploadFolder().toAbsolutePath().toString()).withStyle(ChatFormatting.GRAY))
                                .append(" на сервер и выполните команду ")
                                .append(Component.literal("/audioplayer serverfile \"yourfile.mp3\"").withStyle(ChatFormatting.GRAY).withStyle(style -> {
                                    return style
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/audioplayer serverfile "))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Нажмите для заполнения команды")));
                                }))
                                .append(".")
                , false);
    }

    @RequiresPermission("audioplayer.upload")
    @Command("serverfile")
    public void serverFileUpload(CommandContext<CommandSourceStack> context, @Name("filename") String fileName) {
        Matcher matcher = SOUND_FILE_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            context.getSource().sendFailure(Component.literal("Недопустимое имя файла! Допустимыми символами являются ")
                    .append(Component.literal("A-Z").withStyle(ChatFormatting.GRAY))
                    .append(", ")
                    .append(Component.literal("0-9").withStyle(ChatFormatting.GRAY))
                    .append(", ")
                    .append(Component.literal("_").withStyle(ChatFormatting.GRAY))
                    .append(" и ")
                    .append(Component.literal("-").withStyle(ChatFormatting.GRAY))
                    .append(". Имя также должно заканчиваться на ")
                    .append(Component.literal(".mp3").withStyle(ChatFormatting.GRAY))
                    .append(" или ")
                    .append(Component.literal(".wav").withStyle(ChatFormatting.GRAY))
                    .append(".")
            );
            return;
        }
        UUID uuid = UUID.randomUUID();
        new Thread(() -> {
            Path file = AudioManager.getUploadFolder().resolve(fileName);
            try {
                AudioManager.saveSound(context.getSource().getServer(), uuid, file);
                context.getSource().sendSuccess(() -> sendUUIDMessage(uuid, Component.literal("Звук успешно скопирован.")), false);
                context.getSource().sendSuccess(() -> Component.literal("Временный файл уделён ").append(Component.literal(fileName).withStyle(ChatFormatting.GRAY)).append("."), false);
            } catch (NoSuchFileException e) {
                context.getSource().sendFailure(Component.literal("Не удалось найти файл ").append(Component.literal(fileName).withStyle(ChatFormatting.GRAY)).append("."));
            } catch (Exception e) {
                AudioPlayer.LOGGER.warn("{} не удалось скопировать звук: {}", context.getSource().getTextName(), e.getMessage());
                context.getSource().sendFailure(Component.literal("Не удалось скопировать звук: %s".formatted(e.getMessage())));
            }
        }).start();
    }

    public static MutableComponent sendUUIDMessage(UUID soundID, MutableComponent component) {
        return component.append(" ")
                .append(ComponentUtils.wrapInSquareBrackets(Component.literal("Скопировать ID"))
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, soundID.toString()))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Скопировать ID звука")));
                        })
                        .withStyle(ChatFormatting.GREEN)
                )
                .append(" ")
                .append(ComponentUtils.wrapInSquareBrackets(Component.literal("Установить этот звук на пластинку"))
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/audioplayer musicdisc %s".formatted(soundID.toString())))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Установить этот звук на пластинку")));
                        })
                        .withStyle(ChatFormatting.GREEN)
                ).append(" ")
                .append(ComponentUtils.wrapInSquareBrackets(Component.literal("Установить на козий рог"))
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/audioplayer goathorn %s".formatted(soundID.toString())))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Установить на козий рог")));
                        })
                        .withStyle(ChatFormatting.GREEN)
                );
    }

}
