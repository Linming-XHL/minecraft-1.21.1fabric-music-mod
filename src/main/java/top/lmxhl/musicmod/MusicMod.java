package top.lmxhl.musicmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lmxhl.musicmod.audio.AudioPlayer;
import top.lmxhl.musicmod.audio.FFmpegManager;
import top.lmxhl.musicmod.command.MusicCommand;

public class MusicMod implements ModInitializer {
    public static final String MOD_ID = "musicmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    @Override
    public void onInitialize() {
        LOGGER.info("临明小狐狸音乐Mod加载中...");
        LOGGER.info("网站: https://lmxhl.top");
        
        // 注册命令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MusicCommand.register(dispatcher);
        });
        
        // 服务器启动时初始化FFmpeg管理器
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            FFmpegManager.initialize(server);
            AudioPlayer.initialize();
        });
        
        // 服务器停止时清理资源
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            AudioPlayer.stop();
        });
        
        LOGGER.info("临明小狐狸音乐Mod加载完成!");
    }
}
    