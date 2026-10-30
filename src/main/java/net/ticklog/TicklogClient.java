package net.ticklog;

import net.fabricmc.api.ClientModInitializer;

public final class TicklogClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Stats.INSTANCE.register();
        new Tracker().register();
    }
}
