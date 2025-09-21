package top.lmxhl.musicmod.audio;

public class AudioInfo {
    private String url;
    private String fileName;
    private String songName;
    private String singer;
    private long startTime;
    private long duration; // 音频总时长(毫秒)
    private boolean isPaused;
    
    public AudioInfo(String url, String fileName) {
        this.url = url;
        this.fileName = fileName;
        this.startTime = System.currentTimeMillis();
        this.isPaused = false;
    }
    
    public AudioInfo(String url, String fileName, String songName, String singer) {
        this(url, fileName);
        this.songName = songName;
        this.singer = singer;
    }
    
    // 获取当前播放进度(毫秒)
    public long getCurrentPosition() {
        if (isPaused) {
            return startTime; // 暂停时startTime保存的是暂停时的进度
        }
        return System.currentTimeMillis() - startTime;
    }
    
    // 暂停时调用，保存当前进度
    public void pause() {
        if (!isPaused) {
            long currentPos = getCurrentPosition();
            this.startTime = currentPos; // 暂停时将startTime改为当前进度
            this.isPaused = true;
        }
    }
    
    // 恢复播放时调用
    public void resume() {
        if (isPaused) {
            long pausedPos = startTime; // 恢复时获取暂停时保存的进度
            this.startTime = System.currentTimeMillis() - pausedPos;
            this.isPaused = false;
        }
    }
    
    // 格式化时间(毫秒 -> mm:ss)
    public static String formatTime(long milliseconds) {
        long seconds = (milliseconds / 1000) % 60;
        long minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    // Getters and Setters
    public String getUrl() {
        return url;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public String getSongName() {
        return songName != null ? songName : fileName;
    }
    
    public String getSinger() {
        return singer != null ? singer : "";
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public void setDuration(long duration) {
        this.duration = duration;
    }
}
    