package net.shuyanmc.ccngo.eventbenchmark;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mod.EventBusSubscriber
public class BenchmarkCommand {
    private static final List<Consumer<DummyEvent>> LISTENERS = new ArrayList<>();
    private static long startTime;
    private static final int TEST_REPETITIONS = 10; // 重复测试次数

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("eventbenchmark")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("test1")
                    .executes(ctx -> {
                        runScenarioWithAverage(ctx.getSource(), 1);
                        return 1;
                    })
                )
                .then(Commands.literal("test2")
                    .executes(ctx -> {
                        runScenarioWithAverage(ctx.getSource(), 2);
                        return 1;
                    })
                )
                .then(Commands.literal("reset")
                    .executes(ctx -> {
                        reset();
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("§a已重置所有监听器"), 
                            false
                        );
                        return 1;
                    })
                )
        );
    }

    // 运行测试场景并计算平均结果
    private static void runScenarioWithAverage(CommandSourceStack source, int scenario) {
        source.sendSuccess(() -> Component.literal("§6[性能测试] §a开始执行10次测试..."), false);
        
        // 预热运行（避免JIT编译影响结果）
        if (scenario == 1) testScenario1();
        else testScenario2();
        reset();
        
        // 执行10次测试
        double totalRegister10000 = 0;
        double totalRegister1000 = 0;
        double totalPost10000 = 0;
        
        for (int i = 0; i < TEST_REPETITIONS; i++) {
            if (scenario == 1) {
                testScenario1();
                totalRegister10000 += getLastDuration();
            } else {
                testScenario2();
                totalRegister1000 += getLastDuration(0);
                totalPost10000 += getLastDuration(1);
            }
            reset();
        }
        
        // 计算平均值
        if (scenario == 1) {
            double avgRegister10000 = totalRegister10000 / TEST_REPETITIONS;
            printAverageResult(source, "注册10000监听器", avgRegister10000);
        } else {
            double avgRegister1000 = totalRegister1000 / TEST_REPETITIONS;
            double avgPost10000 = totalPost10000 / TEST_REPETITIONS;
            printAverageResult(source, "注册1000监听器", avgRegister1000);
            printAverageResult(source, "发布10000事件", avgPost10000);
        }
    }

    // 测试场景1: 注册10000监听器(0事件)
    private static void testScenario1() {
        IEventBus bus = MinecraftForge.EVENT_BUS;
        
        startTimer();
        for (int i = 0; i < 10000; i++) {
            Consumer<DummyEvent> listener = event -> {};
            LISTENERS.add(listener);
            bus.addListener(listener);
        }
        recordDuration("注册10000监听器");
    }

    // 测试场景2: 注册1000监听器 + 发布10000事件
    private static void testScenario2() {
        IEventBus bus = MinecraftForge.EVENT_BUS;
        
        // 注册监听器
        startTimer();
        for (int i = 0; i < 1000; i++) {
            Consumer<DummyEvent> listener = event -> {};
            LISTENERS.add(listener);
            bus.addListener(listener);
        }
        recordDuration("注册1000监听器");

        // 发布事件
        startTimer();
        for (int i = 0; i < 10000; i++) {
            bus.post(new DummyEvent());
        }
        recordDuration("发布10000事件");
    }

    private static void reset() {
        // 注销所有监听器
        IEventBus bus = MinecraftForge.EVENT_BUS;
        for (Consumer<DummyEvent> listener : LISTENERS) {
            bus.unregister(listener);
        }
        LISTENERS.clear();
    }

    // 存储最近几次测试的时间
    private static final List<Double> lastDurations = new ArrayList<>();
    
    private static void startTimer() {
        startTime = System.nanoTime();
    }
    
    private static void recordDuration(String operation) {
        long duration = System.nanoTime() - startTime;
        double ms = duration / 1_000_000.0;
        lastDurations.add(ms);
        System.out.printf("[事件性能] %s: %.3f ms%n", operation, ms);
    }
    
    private static double getLastDuration() {
        return lastDurations.isEmpty() ? 0 : lastDurations.get(0);
    }
    
    private static double getLastDuration(int index) {
        return (lastDurations.size() > index) ? lastDurations.get(index) : 0;
    }
    
    private static void printAverageResult(CommandSourceStack source, String operation, double avgMs) {
        // 控制台输出
        System.out.printf("[平均性能] %s: %.3f ms (10次测试平均)%n", operation, avgMs);
        
        // 游戏内聊天栏输出
        String message = String.format("§6[平均性能] §a%s: §b%.3f ms §7(10次测试平均)", operation, avgMs);
        source.sendSuccess(() -> Component.literal(message), false);
    }
}