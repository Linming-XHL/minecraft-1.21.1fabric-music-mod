package top.lmxhl.musicmod.audio;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import top.lmxhl.musicmod.MusicMod;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AudioPlayer {
    private static final Logger LOGGER = MusicMod.LOGGER;
    private static Process ffmpegProcess;
    private static AudioInfo currentAudio;
    private static boolean isPlaying = false;
    private static Future<?> playTask;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    public static void initialize() {
        // 初始化播放器
    }
    
    public static synchronized boolean play(String url, String fileName) {
        return play(url, fileName, null, null);
    }
    
    public static synchronized boolean play(String url, String fileName, String songName, String singer) {
        // 检查是否已经在播放
        if (isPlaying) {
            return false;
        }
        
        // 检查FFmpeg是否可用
        if (!FFmpegManager.isFFmpegAvailable()) {
            LOGGER.error("无法播放音频，FFmpeg不可用");
            return false;
        }
        
        // 停止任何可能正在运行的进程
        stop();
        
        try {
            // 创建音频信息对象
            currentAudio = singer != null ? 
                new AudioInfo(url, fileName, songName, singer) : 
                new AudioInfo(url, fileName);
            
            // 获取FFmpeg路径
            String ffmpegPath = FFmpegManager.getFFmpegPath();
            
            // 构建FFmpeg命令：将URL流转为Minecraft可播放的格式
            String[] command = {
                ffmpegPath,
                "-i", url,
                "-f", "s16le",
                "-ar", "44100",
                "-ac", "2",
                "-", // 输出到stdout
                "-hide_banner",
                "-loglevel", "error"
            };
            
            // 启动FFmpeg进程
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            ffmpegProcess = processBuilder.start();
            
            // 启动播放任务
            isPlaying = true;
            playTask = executor.submit(() -> {
                try {
                    // 这里应该将FFmpeg的输出连接到Minecraft的音频系统
                    // 实际实现需要使用Minecraft的SoundSystem或相关API
                    consumeFFmpegOutput();
                    
                    // 播放结束后清理
                    synchronized (AudioPlayer.class) {
                        isPlaying = false;
                        currentAudio = null;
                        ffmpegProcess = null;
                    }
                } catch (Exception e) {
                    LOGGER.error("音频播放出错: " + e.getMessage());
                    stop();
                }
            });
            
            return true;
        } catch (IOException e) {
            LOGGER.error("启动FFmpeg进程失败: " + e.getMessage());
            stop();
            return false;
        }
    }
    
    private static void consumeFFmpegOutput() throws IOException {
        // 消耗FFmpeg的输出流，防止缓冲区阻塞
        try (java.io.InputStream in = ffmpegProcess.getInputStream()) {
            byte[] buffer = new byte[4096];
            while (isPlaying && in.read(buffer) != -1) {
                // 在实际实现中，这里应该将音频数据发送到Minecraft的音频系统
            }
        }
    }
    
    public static synchronized void pause() {
        if (isPlaying && currentAudio != null) {
            if (ffmpegProcess != null) {
                ffmpegProcess.destroyForcibly();
                ffmpegProcess = null;
            }
            currentAudio.pause();
            isPlaying = false;
            if (playTask != null) {
                playTask.cancel(true);
                playTask = null;
            }
        }
    }
    
    public static synchronized boolean resume() {
        if (!isPlaying && currentAudio != null) {
            currentAudio.resume();
            return play(currentAudio.getUrl(), currentAudio.getFileName(), 
                      currentAudio.getSongName(), currentAudio.getSinger());
        }
        return false;
    }
    
    public static synchronized void stop() {
        if (ffmpegProcess != null) {
            ffmpegProcess.destroyForcibly();
            ffmpegProcess = null;
        }
        isPlaying = false;
        currentAudio = null;
        if (playTask != null) {
            playTask.cancel(true);
            playTask = null;
        }
    }
    
    public static synchronized boolean isPlaying() {
        return isPlaying;
    }
    
    public static synchronized boolean isPaused() {
        return !isPlaying && currentAudio != null && currentAudio.isPaused();
    }
    
    public static synchronized AudioInfo getCurrentAudio() {
        return currentAudio;
    }
    
    public static void sendMessageToAllPlayers(ServerPlayerEntity source, Text message) {
        // 实际实现需要获取所有在线玩家并发送消息
        // 这里仅作为示例
        if (source != null) {
            source.getServer().getPlayerManager().broadcast(message, false);
        }
    }
}
    