package top.lmxhl.music;

import com.google.gson.annotations.SerializedName;

public class NeteaseMusicResponse {
    @SerializedName("code")
    private int code;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("data")
    private Data data;
    
    @SerializedName("time")
    private String time;
    
    @SerializedName("pid")
    private int pid;
    
    @SerializedName("tips")
    private String tips;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Data getData() {
        return data;
    }

    public String getTime() {
        return time;
    }

    public int getPid() {
        return pid;
    }

    public String getTips() {
        return tips;
    }
    
    public static class Data {
        @SerializedName("id")
        private long id;
        
        @SerializedName("song")
        private String song;
        
        @SerializedName("singer")
        private String singer;
        
        @SerializedName("album")
        private String album;
        
        @SerializedName("time")
        private String time;
        
        @SerializedName("quality")
        private String quality;
        
        @SerializedName("cover")
        private String cover;
        
        @SerializedName("interval")
        private String interval;
        
        @SerializedName("link")
        private String link;
        
        @SerializedName("size")
        private String size;
        
        @SerializedName("kbps")
        private String kbps;
        
        @SerializedName("url")
        private String url;

        public long getId() {
            return id;
        }

        public String getSong() {
            return song;
        }

        public String getSinger() {
            return singer;
        }

        public String getAlbum() {
            return album;
        }

        public String getTime() {
            return time;
        }

        public String getQuality() {
            return quality;
        }

        public String getCover() {
            return cover;
        }

        public String getInterval() {
            return interval;
        }

        public String getLink() {
            return link;
        }

        public String getSize() {
            return size;
        }

        public String getKbps() {
            return kbps;
        }

        public String getUrl() {
            return url;
        }
    }
}
    