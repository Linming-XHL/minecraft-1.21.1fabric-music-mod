package top.lmxhl.music;

import com.google.gson.Gson;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import top.lmxhl.MusicMod;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MusicPlayer {
    private static MusicPlayer instance;
    
    private boolean isPlaying;
    private String currentUrl;
    private String currentTitle;
    private String currentArtist;
    private Instant startTime;
    private Instant pauseTime;
    private Duration pausedDuration = Duration.ZERO;
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final ScheduledExecutorService scheduler;
    
    private MusicPlayer() {
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.isPlaying = false;
    }
    
    public static synchronized MusicPlayer getInstance() {
        if (instance == null) {
            instance = new MusicPlayer();
        }
        return instance;
    }
    
    public boolean playUrl(String url, String title, String artist) {
        if (isPlaying) {
            return false;
        }
        
        this.currentUrl = url;
        this.currentTitle = title;
        this.currentArtist = artist;
        this.startTime = Instant.now();
        this.pausedDuration = Duration.ZERO;
        this.isPlaying = true;
        
        // 这里可以添加实际播放逻辑
        
        return true;
    }
    
    public boolean pause() {
        if (!isPlaying) {
            return false;
        }
        
        this.pauseTime = Instant.now();
        this.isPlaying = false;
        return true;
    }
    
    public boolean resume() {
        if (isPlaying || currentUrl == null) {
            return false;
        }
        
        if (pauseTime != null) {
            pausedDuration = pausedDuration.plus(Duration.between(pauseTime, Instant.now()));
        }
        this.isPlaying = true;
        return true;
    }
    
    public void stop() {
        this.isPlaying = false;
        this.currentUrl = null;
        this.currentTitle = null;
        this.currentArtist = null;
        this.startTime = null;
        this.pauseTime = null;
        this.pausedDuration = Duration.ZERO;
    }
    
    public void searchAndPlayNetease(String keyword, ServerPlayerEntity player) {
        scheduler.execute(() -> {
            try {
                String encodedKeyword = java.net.URLEncoder.encode(keyword, "UTF-8");
                String url = "https://api.vkeys.cn/v2/music/netease?word=" + encodedKeyword + "&choose=1&quality=2";
                
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        player.sendMessage(Text.literal("§c获取音乐信息失败: 服务器返回错误"), false);
                        return;
                    }
                    
                    String responseBody = response.body().string();
                    NeteaseMusicResponse musicResponse = gson.fromJson(responseBody, NeteaseMusicResponse.class);
                    
                    if (musicResponse.getCode() != 200 || musicResponse.getData() == null) {
                        player.sendMessage(Text.literal("§c未找到相关音乐: " + musicResponse.getMessage()), false);
                        return;
                    }
                    
                    NeteaseMusicResponse.Data data = musicResponse.getData();
                    if (data.getUrl() == null || data.getUrl().isEmpty()) {
                        player.sendMessage(Text.literal("§c未找到音乐播放链接"), false);
                        return;
                    }
                    
                    // 尝试播放找到的音乐
                    boolean success = playUrl(data.getUrl(), data.getSong(), data.getSinger());
                    if (success) {
                        broadcastMessage(Text.literal("§a正在播放: " + data.getSong() + " - " + data.getSinger()));
                    } else {
                        player.sendMessage(Text.literal("§c播放失败，请先暂停当前音乐"), false);
                    }
                }
            } catch (IOException e) {
                MusicMod.LOGGER.error("网易云音乐搜索失败", e);
                player.sendMessage(Text.literal("§c搜索音乐时发生错误"), false);
            }
        });
    }
    
    public String getPlaybackInfo() {
        if (currentUrl == null) {
            return "§c当前没有播放音乐";
        }
        
        Duration duration = Duration.ZERO;
        if (isPlaying) {
            duration = Duration.between(startTime, Instant.now()).minus(pausedDuration);
        } else if (pauseTime != null) {
            duration = Duration.between(startTime, pauseTime).minus(pausedDuration);
        }
        
        long seconds = duration.getSeconds() % 60;
        long minutes = (duration.getSeconds() / 60) % 60;
        
        String status = isPlaying ? "§a正在播放" : "§e已暂停";
        String title = currentTitle != null ? currentTitle : getFileNameFromUrl(currentUrl);
        String artist = currentArtist != null ? " - " + currentArtist : "";
        
        return String.format("%s: %s%s §7| 进度: %02d:%02d", 
                status, title, artist, minutes, seconds);
    }
    
    private String getFileNameFromUrl(String url) {
        try {
            java.net.URL u = new java.net.URL(url);
            String path = u.getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        } catch (Exception e) {
            return url;
        }
    }
    
    public void broadcastMessage(Text message) {
        if (MusicMod.LOGGER.isDebugEnabled()) {
            MusicMod.LOGGER.info("[音乐广播] " + message.getString());
        }
        // 实际项目中需要获取所有在线玩家并发送消息
        // 这里简化处理，仅作为示例
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
}
    