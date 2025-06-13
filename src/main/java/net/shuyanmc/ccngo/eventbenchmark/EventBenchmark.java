package net.shuyanmc.ccngo.eventbenchmark;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("eventbenchmark")
public class EventBenchmark {
    public static final String MODID = "eventbenchmark";
    
    public EventBenchmark() {
        // 使用推荐的获取上下文方式
        var ctx = FMLJavaModLoadingContext.get();
        ctx.getModEventBus().addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // 无需在此调用命令注册
    }
}