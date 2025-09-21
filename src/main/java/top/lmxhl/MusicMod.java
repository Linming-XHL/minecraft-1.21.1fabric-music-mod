package top.lmxhl;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lmxhl.command.MusicCommand;

public class MusicMod implements ModInitializer {
    public static final String MOD_ID = "musicmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // 注册指令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MusicCommand.register(dispatcher);
        });
        
        LOGGER.info("服务器音乐播放模组已加载 - 作者: 临明小狐狸");
    }
}
    