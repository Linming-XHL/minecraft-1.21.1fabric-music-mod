package top.lmxhl.musicmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import top.lmxhl.musicmod.audio.AudioInfo;
import top.lmxhl.musicmod.audio.AudioPlayer;
import top.lmxhl.musicmod.audio.FFmpegManager;
import top.lmxhl.musicmod.network.NeteaseMusicAPI;

public class MusicCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("music")
            .requires(source -> source.hasPermissionLevel(2)) // 需要管理员权限
            .then(CommandManager.literal("play")
                .then(CommandManager.argument("url", StringArgumentType.greedyString())
                    .executes(MusicCommand::playUrl)))
            .then(CommandManager.literal("pause")
                .executes(MusicCommand::pause))
            .then(CommandManager.literal("info")
                .executes(MusicCommand::info))
            .then(CommandManager.literal("help")
                .executes(MusicCommand::help))
            .then(CommandManager.literal("netease")
                .then(CommandManager.argument("search", StringArgumentType.greedyString())
                    .executes(MusicCommand::neteaseSearch))));
    }
    
    private static int playUrl(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        // 检查是否在服务器环境
        if (FFmpegManager.isServerEnvironment()) {
            source.sendFeedback(() -> Text.literal("§c服务器环境不支持音乐播放"), false);
            return 0;
        }
        
        // 检查FFmpeg是否可用
        if (!FFmpegManager.isFFmpegAvailable()) {
            source.sendFeedback(() -> Text.literal("§cFFmpeg不可用，无法播放音乐"), false);
            return 0;
        }
        
        // 检查是否已经在播放
        if (AudioPlayer.isPlaying()) {
            source.sendFeedback(() -> Text.literal("§c已有音乐在播放，请先使用/music pause暂停"), false);
            return 0;
        }
        
        String url = StringArgumentType.getString(context, "url");
        
        // 提取文件名作为显示名称
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf('?'));
        }
        
        // 尝试播放
        if (AudioPlayer.play(url, fileName)) {
            source.sendFeedback(() -> Text.literal("§a开始播放音乐: " + fileName), true);
            AudioPlayer.sendMessageToAllPlayers(source.getPlayer(), Text.literal("§6" + source.getPlayer().getName().getString() + " 开始播放音乐: " + fileName));
            return 1;
        } else {
            source.sendFeedback(() -> Text.literal("§c播放音乐失败"), false);
            return 0;
        }
    }
    
    private static int pause(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        if (AudioPlayer.isPlaying()) {
            AudioPlayer.pause();
            source.sendFeedback(() -> Text.literal("§a已暂停播放"), true);
            AudioPlayer.sendMessageToAllPlayers(source.getPlayer(), Text.literal("§6" + source.getPlayer().getName().getString() + " 暂停了音乐播放"));
            return 1;
        } else if (AudioPlayer.isPaused()) {
            if (AudioPlayer.resume()) {
                source.sendFeedback(() -> Text.literal("§a已恢复播放"), true);
                AudioPlayer.sendMessageToAllPlayers(source.getPlayer(), Text.literal("§6" + source.getPlayer().getName().getString() + " 恢复了音乐播放"));
                return 1;
            } else {
                source.sendFeedback(() -> Text.literal("§c恢复播放失败"), false);
                return 0;
            }
        } else {
            source.sendFeedback(() -> Text.literal("§c没有正在播放的音乐"), false);
            return 0;
        }
    }
    
    private static int info(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        AudioInfo currentAudio = AudioPlayer.getCurrentAudio();
        
        if (currentAudio == null) {
            source.sendFeedback(() -> Text.literal("§c没有正在播放或暂停的音乐"), false);
            return 0;
        }
        
        source.sendFeedback(() -> Text.literal("§6音乐信息:"), false);
        source.sendFeedback(() -> Text.literal("§a名称: " + currentAudio.getSongName()), false);
        
        if (!currentAudio.getSinger().isEmpty()) {
            source.sendFeedback(() -> Text.literal("§a歌手: " + currentAudio.getSinger()), false);
        }
        
        String status = AudioPlayer.isPlaying() ? "播放中" : "已暂停";
        source.sendFeedback(() -> Text.literal("§a状态: " + status), false);
        
        String currentTime = AudioInfo.formatTime(currentAudio.getCurrentPosition());
        source.sendFeedback(() -> Text.literal("§a进度: " + currentTime), false);
        
        return 1;
    }
    
    private static int help(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("§6===== 音乐指令帮助 ====="), false);
        source.sendFeedback(() -> Text.literal("§a/music play <URL> §7- 播放指定URL的音频"), false);
        source.sendFeedback(() -> Text.literal("§a/music pause §7- 暂停或恢复当前播放的音频"), false);
        source.sendFeedback(() -> Text.literal("§a/music info §7- 查看当前播放的音乐信息"), false);
        source.sendFeedback(() -> Text.literal("§a/music help §7- 显示帮助信息"), false);
        source.sendFeedback(() -> Text.literal("§a/music netease <搜索词> §7- 搜索并播放网易云音乐"), false);
        source.sendFeedback(() -> Text.literal("§6======================="), false);
        
        return 1;
    }
    
    private static int neteaseSearch(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        // 检查是否在服务器环境
        if (FFmpegManager.isServerEnvironment()) {
            source.sendFeedback(() -> Text.literal("§c服务器环境不支持音乐播放"), false);
            return 0;
        }
        
        // 检查FFmpeg是否可用
        if (!FFmpegManager.isFFmpegAvailable()) {
            source.sendFeedback(() -> Text.literal("§cFFmpeg不可用，无法播放音乐"), false);
            return 0;
        }
        
        // 检查是否已经在播放
        if (AudioPlayer.isPlaying()) {
            source.sendFeedback(() -> Text.literal("§c已有音乐在播放，请先使用/music pause暂停"), false);
            return 0;
        }
        
        String searchTerm = StringArgumentType.getString(context, "search");
        source.sendFeedback(() -> Text.literal("§a正在搜索网易云音乐: " + searchTerm), false);
        
        // 调用API搜索音乐
        NeteaseMusicAPI.MusicInfo musicInfo = NeteaseMusicAPI.searchMusic(searchTerm);
        
        if (musicInfo == null) {
            source.sendFeedback(() -> Text.literal("§c搜索音乐失败，请尝试其他关键词"), false);
            return 0;
        }
        
        // 尝试播放找到的音乐
        if (AudioPlayer.play(musicInfo.getUrl(), musicInfo.getSongName() + ".mp3", 
                           musicInfo.getSongName(), musicInfo.getSinger())) {
            source.sendFeedback(() -> Text.literal("§a开始播放: " + musicInfo.getSongName() + " - " + musicInfo.getSinger()), true);
            AudioPlayer.sendMessageToAllPlayers(source.getPlayer(), 
                Text.literal("§6" + source.getPlayer().getName().getString() + " 开始播放: " + musicInfo.getSongName() + " - " + musicInfo.getSinger()));
            return 1;
        } else {
            source.sendFeedback(() -> Text.literal("§c播放音乐失败"), false);
            return 0;
        }
    }
}
    