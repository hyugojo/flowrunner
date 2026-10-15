package com.flowrunner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class FlowRunnerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FlowConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(WallMovement::tick);
    }
}
