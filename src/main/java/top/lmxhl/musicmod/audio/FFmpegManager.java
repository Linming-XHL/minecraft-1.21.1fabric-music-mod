package top.lmxhl.musicmod.audio;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.slf4j.Logger;
import top.lmxhl.musicmod.MusicMod;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FFmpegManager {
    private static final Logger LOGGER = MusicMod.LOGGER;
    private static final String FFMPEG_DIR_NAME = "ffmpeg";
    private static final String FFMPEG_CONFIG_DIR = "musicmod";
    private static String ffmpegPath;
    private static boolean isServerEnvironment;
    
    // 基础下载源
    private static final String BASE_DOWNLOAD_URL = "https://gh.927223.xyz/https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/";
    
    public static void initialize(MinecraftServer server) {
        // 检测是否为服务器环境
        isServerEnvironment = server != null;
        
        if (isServerEnvironment) {
            LOGGER.info("检测到服务器环境，将不使用FFmpeg");
            return;
        }
        
        // 查找或下载FFmpeg
        findOrDownloadFFmpeg();
    }
    
    private static void findOrDownloadFFmpeg() {
        // 检查配置目录中是否有FFmpeg
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(FFMPEG_CONFIG_DIR).resolve(FFMPEG_DIR_NAME);
        String osType = getOSType();
        String archType = getArchitectureType();
        String executableName = osType.contains("win") ? "ffmpeg.exe" : "ffmpeg";
        
        // 检查系统路径
        if (checkSystemPathForFFmpeg(executableName)) {
            LOGGER.info("在系统路径中找到FFmpeg");
            return;
        }
        
        // 检查配置目录
        File ffmpegFile = new File(configDir.toFile(), getOSDirectory() + File.separator + archType + File.separator + executableName);
        if (ffmpegFile.exists() && ffmpegFile.canExecute()) {
            ffmpegPath = ffmpegFile.getAbsolutePath();
            LOGGER.info("在配置目录中找到FFmpeg: " + ffmpegPath);
            return;
        }
        
        // 没有找到FFmpeg，开始下载
        LOGGER.info("未找到FFmpeg，准备下载适合当前系统的版本...");
        downloadFFmpeg(configDir, osType, archType);
    }
    
    private static boolean checkSystemPathForFFmpeg(String executableName) {
        try {
            Process process = new ProcessBuilder(executableName, "-version").start();
            if (process.waitFor() == 0) {
                ffmpegPath = executableName;
                return true;
            }
        } catch (IOException | InterruptedException e) {
            // FFmpeg不在系统路径中
        }
        return false;
    }
    
    private static void downloadFFmpeg(Path targetDir, String osType, String archType) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                // 创建目标目录
                File ffmpegDir = new File(targetDir.toFile(), getOSDirectory() + File.separator + archType);
                if (!ffmpegDir.exists()) {
                    ffmpegDir.mkdirs();
                }
                
                // 获取对应系统和架构的下载链接
                String downloadUrl = getDownloadUrlForOSAndArch(osType, archType);
                if (downloadUrl == null) {
                    LOGGER.error("不支持的操作系统或架构，无法下载FFmpeg");
                    return;
                }
                
                String fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
                File tempFile = new File(targetDir.toFile(), fileName);
                
                // 下载文件
                downloadFileWithProgress(downloadUrl, tempFile);
                
                // 解压文件
                extractFFmpeg(tempFile, ffmpegDir);
                
                // 清理临时文件
                if (tempFile.exists()) {
                    tempFile.delete();
                }
                
                // 设置FFmpeg路径
                String executableName = osType.contains("win") ? "ffmpeg.exe" : "ffmpeg";
                File ffmpegFile = findFFmpegExecutable(ffmpegDir, executableName);
                if (ffmpegFile != null && ffmpegFile.exists()) {
                    ffmpegFile.setExecutable(true);
                    ffmpegPath = ffmpegFile.getAbsolutePath();
                    LOGGER.info("FFmpeg下载并安装成功: " + ffmpegPath);
                } else {
                    LOGGER.error("FFmpeg下载成功，但未找到可执行文件");
                }
                
            } catch (Exception e) {
                LOGGER.error("下载FFmpeg时出错: " + e.getMessage());
                e.printStackTrace();
            }
        });
        executor.shutdown();
    }
    
    private static File findFFmpegExecutable(File searchDir, String executableName) {
        // 递归查找解压目录中的可执行文件
        if (searchDir.isDirectory()) {
            File[] files = searchDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        File found = findFFmpegExecutable(file, executableName);
                        if (found != null) {
                            return found;
                        }
                    } else if (file.getName().equals(executableName)) {
                        return file;
                    }
                }
            }
        }
        return null;
    }
    
    private static void downloadFileWithProgress(String urlString, File targetFile) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        
        int fileSize = connection.getContentLength();
        LOGGER.info("开始下载FFmpeg (" + formatFileSize(fileSize) + ")");
        
        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(targetFile)) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalRead = 0;
            
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                
                // 每下载5%进度或至少1MB数据时更新日志
                if (fileSize > 0 && (totalRead * 100 / fileSize) % 5 == 0 || totalRead % (1024 * 1024) == 0) {
                    int progress = (int) (totalRead * 100 / fileSize);
                    LOGGER.info("FFmpeg下载进度: " + progress + "% (" + formatFileSize(totalRead) + "/" + formatFileSize(fileSize) + ")");
                }
            }
        }
        connection.disconnect();
    }
    
    private static void extractFFmpeg(File archiveFile, File targetDir) throws IOException, ArchiveException {
        LOGGER.info("正在解压FFmpeg文件: " + archiveFile.getName());
        
        if (archiveFile.getName().endsWith(".zip")) {
            extractZip(archiveFile, targetDir);
        } else if (archiveFile.getName().endsWith(".tar.xz")) {
            extractTarXz(archiveFile, targetDir);
        } else {
            throw new IllegalArgumentException("不支持的压缩格式: " + archiveFile.getName());
        }
    }
    
    private static void extractZip(File zipFile, File targetDir) throws IOException, ArchiveException {
        try (InputStream fis = new FileInputStream(zipFile);
             ZipArchiveInputStream zis = new ZipArchiveInputStream(fis)) {
            
            ArchiveEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File entryFile = new File(targetDir, entry.getName());
                
                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    // 确保父目录存在
                    if (!entryFile.getParentFile().exists()) {
                        entryFile.getParentFile().mkdirs();
                    }
                    
                    try (OutputStream os = new FileOutputStream(entryFile)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
            }
        }
    }
    
    private static void extractTarXz(File tarXzFile, File targetDir) throws IOException, ArchiveException {
        try (InputStream fis = new FileInputStream(tarXzFile);
             XZCompressorInputStream xzis = new XZCompressorInputStream(fis);
             TarArchiveInputStream tais = new TarArchiveInputStream(xzis)) {
            
            ArchiveEntry entry;
            while ((entry = tais.getNextEntry()) != null) {
                File entryFile = new File(targetDir, entry.getName());
                
                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    // 确保父目录存在
                    if (!entryFile.getParentFile().exists()) {
                        entryFile.getParentFile().mkdirs();
                    }
                    
                    try (OutputStream os = new FileOutputStream(entryFile)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = tais.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
            }
        }
    }
    
    private static String getOSType() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "windows";
        } else if (os.contains("mac")) {
            return "macos";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return "linux";
        } else {
            return "unknown";
        }
    }
    
    private static String getArchitectureType() {
        String arch = System.getProperty("os.arch").toLowerCase();
        
        // 检查ARM架构
        if (arch.contains("arm")) {
            if (arch.contains("64") || arch.contains("aarch64")) {
                return "arm64";
            } else {
                return "arm";
            }
        }
        
        // 检查x86架构
        if (arch.contains("64") || arch.equals("amd64")) {
            return "x64";
        } else if (arch.contains("86")) {
            return "x86";
        }
        
        return arch;
    }
    
    private static String getOSDirectory() {
        String os = getOSType();
        switch (os) {
            case "windows": return "windows";
            case "macos": return "macos";
            case "linux": return "linux";
            default: return "unknown";
        }
    }
    
    private static String getDownloadUrlForOSAndArch(String osType, String archType) {
        // 选择共享版本以减小文件大小
        String licenseType = "lgpl-shared";
        String fileExtension;
        String osPrefix;
        
        switch (osType) {
            case "windows":
                osPrefix = "win";
                fileExtension = ".zip";
                break;
            case "linux":
                osPrefix = "linux";
                fileExtension = ".tar.xz";
                break;
            case "macos":
                // 假设macOS使用与Linux相同的命名模式
                osPrefix = "macos";
                fileExtension = ".tar.xz";
                break;
            default:
                return null;
        }
        
        // 处理架构名称映射
        String archSuffix = switch (archType) {
            case "x64", "amd64" -> "64";
            case "arm64" -> "arm64";
            default -> archType;
        };
        
        // 使用最新的稳定版本8.0
        String version = "n8.0";
        
        // 构建文件名
        String fileName = String.format("ffmpeg-%s-latest-%s%s-%s-%s%s",
                version,
                osPrefix,
                archSuffix,
                licenseType,
                version,
                fileExtension);
        
        // 检查文件名是否符合提供的列表
        if (isValidFileName(fileName)) {
            return BASE_DOWNLOAD_URL + fileName;
        }
        
        // 如果找不到特定版本，尝试使用master版本
        fileName = String.format("ffmpeg-master-latest-%s%s-%s%s",
                osPrefix,
                archSuffix,
                licenseType,
                fileExtension);
        
        if (isValidFileName(fileName)) {
            return BASE_DOWNLOAD_URL + fileName;
        }
        
        LOGGER.warn("未找到适合 " + osType + " " + archType + " 的FFmpeg版本");
        return null;
    }
    
    // 验证文件名是否在提供的列表中
    private static boolean isValidFileName(String fileName) {
        // 简化检查，实际应根据提供的完整列表验证
        String[] validPrefixes = {
            "ffmpeg-master-latest-",
            "ffmpeg-n7.1-latest-",
            "ffmpeg-n8.0-latest-"
        };
        
        String[] validLicenses = {
            "-gpl-", "-gpl-shared-",
            "-lgpl-", "-lgpl-shared-"
        };
        
        String[] validExtensions = {".zip", ".tar.xz"};
        
        for (String prefix : validPrefixes) {
            if (fileName.startsWith(prefix)) {
                for (String license : validLicenses) {
                    if (fileName.contains(license)) {
                        for (String ext : validExtensions) {
                            if (fileName.endsWith(ext)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
    
    public static String getFFmpegPath() {
        return ffmpegPath;
    }
    
    public static boolean isFFmpegAvailable() {
        // 服务器环境下不使用FFmpeg
        if (isServerEnvironment) {
            return false;
        }
        return ffmpegPath != null && new File(ffmpegPath).canExecute();
    }
    
    public static boolean isServerEnvironment() {
        return isServerEnvironment;
    }
}
    