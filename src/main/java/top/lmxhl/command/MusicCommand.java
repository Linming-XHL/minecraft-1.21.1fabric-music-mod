package top.lmxhl.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import top.lmxhl.music.MusicPlayer;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class MusicCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("music")
                .requires(source -> source.hasPermissionLevel(2)) // 需要管理员权限
                .then(literal("play")
                        .then(argument("url", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String url = StringArgumentType.getString(context, "url");
                                    MusicPlayer player = MusicPlayer.getInstance();
                                    
                                    if (player.isPlaying()) {
                                        context.getSource().sendFeedback(
                                                () -> Text.literal("§c当前已有音乐在播放，请先使用/music pause暂停"), 
                                                false);
                                        return 0;
                                    }
                                    
                                    String fileName = getFileNameFromUrl(url);
                                    boolean success = player.playUrl(url, fileName, null);
                                    
                                    if (success) {
                                        player.broadcastMessage(Text.literal("§a开始播放音乐: " + fileName));
                                        return 1;
                                    } else {
                                        context.getSource().sendFeedback(
                                                () -> Text.literal("§c播放失败，请检查URL是否有效"), 
                                                false);
                                        return 0;
                                    }
                                })
                        )
                )
                .then(literal("pause")
                        .executes(context -> {
                            MusicPlayer player = MusicPlayer.getInstance();
                            if (player.pause()) {
                                player.broadcastMessage(Text.literal("§e音乐已暂停"));
                                return 1;
                            } else {
                                context.getSource().sendFeedback(
                                        () -> Text.literal("§c当前没有播放中的音乐"), 
                                        false);
                                return 0;
                            }
                        })
                )
                .then(literal("info")
                        .executes(context -> {
                            MusicPlayer player = MusicPlayer.getInstance();
                            context.getSource().sendFeedback(
                                    () -> Text.literal(player.getPlaybackInfo()), 
                                    false);
                            return 1;
                        })
                )
                .then(literal("help")
                        .executes(context -> {
                            sendHelpMessage(context.getSource());
                            return 1;
                        })
                )
                .then(literal("netease")
                        .then(argument("searchTerm", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String searchTerm = StringArgumentType.getString(context, "searchTerm");
                                    MusicPlayer player = MusicPlayer.getInstance();
                                    
                                    if (player.isPlaying()) {
                                        context.getSource().sendFeedback(
                                                () -> Text.literal("§c当前已有音乐在播放，请先使用/music pause暂停"), 
                                                false);
                                        return 0;
                                    }
                                    
                                    context.getSource().sendFeedback(
                                            () -> Text.literal("§e正在搜索网易云音乐: " + searchTerm), 
                                            false);
                                    player.searchAndPlayNetease(searchTerm, context.getSource().getPlayer());
                                    return 1;
                                })
                        )
                )
                .executes(context -> {
                    sendHelpMessage(context.getSource());
                    return 1;
                })
        );
    }
    
    private static void sendHelpMessage(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("§6===== 音乐指令帮助 ====="), false);
        source.sendFeedback(() -> Text.literal("§a/music play <URL> §7- 播放指定URL的音乐"), false);
        source.sendFeedback(() -> Text.literal("§a/music pause §7- 暂停当前播放的音乐"), false);
        source.sendFeedback(() -> Text.literal("§a/music info §7- 查看当前播放信息和进度"), false);
        source.sendFeedback(() -> Text.literal("§a/music help §7- 显示本帮助信息"), false);
        source.sendFeedback(() -> Text.literal("§a/music netease <搜索词> §7- 搜索并播放网易云音乐"), false);
        source.sendFeedback(() -> Text.literal("§6======================="), false);
    }
    
    private static String getFileNameFromUrl(String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            String path = u.getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        } catch (Exception e) {
            return url;
        }
    }
}
    