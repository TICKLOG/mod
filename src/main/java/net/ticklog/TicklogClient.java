package net.ticklog;

import net.fabricmc.api.ClientModInitializer;

public final class TicklogClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        new Tracker().register();
    }
}
