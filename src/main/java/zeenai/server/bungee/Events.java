package zeenai.server.bungee;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class Events implements Listener
{
    public static Map<String, State> playerStates = new LinkedHashMap<String, State>();

    List<String> list = new ArrayList<String>();
    ServerInfo limbo = ProxyServer.getInstance().getServerInfo("limbo") == null ? ProxyServer.getInstance().getServers().entrySet().iterator().next().getValue() : ProxyServer.getInstance().getServerInfo("limbo");

    @EventHandler
    public void on(PostLoginEvent ev){
        
        list.add(ev.getPlayer().getName());
        
    }

    @EventHandler
    public void onSend(ServerConnectEvent e){
        ProxiedPlayer p = e.getPlayer();
        if(!list.contains(p.getName()))return;

        list.remove(p.getName());

        final String originalTarget = e.getTarget().getName();
        e.setTarget(limbo);

        p.sendMessage(new TextComponent(ZNIPlugin.getInstance().CFG.getString("lang.PEND_VERIFY")));
        ZNIPlugin.dest.put(p.getName(), "Primary");

        playerStates.put(p.getName(), State.JOIN);
    }

    @EventHandler
    public void disconnect(PlayerDisconnectEvent ev){
        ZNIPlugin.dest.remove(ev.getPlayer().getName());
        Events.playerStates.remove(ev.getPlayer().getName());
    }

    public static void checkInformation(){
        
    }
}