package zeenai.server.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import zeenai.server.bungee.Events;
import zeenai.server.bungee.State;
import zeenai.server.bungee.ZNIPlugin;

public class Logout extends Command {

    public Logout(String name) {
        super(name);
    }

    @Override
    public void execute(CommandSender sender, String[] args)
    {
        // Logout the current player
        ZNIPlugin.getInstance().CFG.set(sender.getName()+".rememberFor", null);
        ZNIPlugin.dest.put(sender.getName(), "Primary");
        Events.playerStates.put(sender.getName(), State.JOIN);

        ProxiedPlayer player = ZNIPlugin.getInstance().getProxy().getPlayer(sender.getName());
        player.connect(ZNIPlugin.getInstance().getProxy().getServerInfo("limbo"));
    }
    
}