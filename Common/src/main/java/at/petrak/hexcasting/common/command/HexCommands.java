package at.petrak.hexcasting.common.command;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class HexCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent evt) {
        var dp = evt.getDispatcher();
        ListPatternsCommand.register(dp);
        RecalcPatternsCommand.register(dp);
        BrainsweepCommand.register(dp);
    }
}
