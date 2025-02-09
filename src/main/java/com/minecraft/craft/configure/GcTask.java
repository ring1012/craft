package com.minecraft.craft.configure;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GcTask {

    @Scheduled(initialDelay = 2 * 60 * 1000, fixedRate = 20 * 60 * 1000)
    public void forceGc() {
        System.gc(); // 调用 GC
        System.out.println("Manually triggered GC at: " + System.currentTimeMillis());
    }
}
