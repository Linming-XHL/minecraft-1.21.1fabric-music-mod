package top.lmxhl.musicmod.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import top.lmxhl.musicmod.MusicMod;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class NeteaseMusicAPI {
    private static final Logger LOGGER = MusicMod.LOGGER;
    private static final String API_URL = "https://api.vkeys.cn/v2/music/netease?word=%s&choose=1&quality=2";
    
    public static MusicInfo searchMusic(String keyword) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 对搜索词进行URL编码
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8.name());
            String url = String.format(API_URL, encodedKeyword);
            
            HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                
                // 解析JSON响应
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                
                if (json.has("code") && json.get("code").getAsInt() == 200) {
                    JsonObject data = json.getAsJsonObject("data");
                    
                    return new MusicInfo(
                        data.get("url").getAsString(),
                        data.get("song").getAsString(),
                        data.get("singer").getAsString(),
                        data.get("album").getAsString(),
                        data.get("interval").getAsString()
                    );
                } else {
                    String message = json.has("message") ? json.get("message").getAsString() : "未知错误";
                    LOGGER.error("网易云音乐API请求失败: " + message);
                    return null;
                }
            }
        } catch (IOException e) {
            LOGGER.error("网易云音乐API调用出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public static class MusicInfo {
        private final String url;
        private final String songName;
        private final String singer;
        private final String album;
        private final String duration;
        
        public MusicInfo(String url, String songName, String singer, String album, String duration) {
            this.url = url;
            this.songName = songName;
            this.singer = singer;
            this.album = album;
            this.duration = duration;
        }
        
        public String getUrl() {
            return url;
        }
        
        public String getSongName() {
            return songName;
        }
        
        public String getSinger() {
            return singer;
        }
        
        public String getAlbum() {
            return album;
        }
        
        public String getDuration() {
            return duration;
        }
    }
}
    