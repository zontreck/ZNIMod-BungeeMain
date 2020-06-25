package zeenai.server.bungee;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import zeenai.server.bungee.commands.Auth;
import zeenai.server.bungee.commands.Logout;

public class ZNIPlugin extends Plugin
{

    private static ZNIPlugin inst = null;
    public static LinkedHashMap<String,String> dest = new LinkedHashMap<String,String>();
    public static ZNIPlugin getInstance(){
        return inst;
    }

    public Configuration CFG;


    @Override
    public void onEnable()
    {
        inst=this;
        getLogger().info("Zontreck.dev Enforced Authentication and Queue Management System (ZdEAQMS) has been initialized!");

        process();

        getProxy().getPluginManager().registerCommand(this, new Auth("auth"));
        getProxy().getPluginManager().registerCommand(this, new Logout("logout"));
        getProxy().getPluginManager().registerListener(this, new Events());

        getProxy().getScheduler().schedule(this, ()->{
            int i=0;
            Map<String,String> map = new LinkedHashMap<String,String>(dest);
            for (Entry<String,String> _Entry : map.entrySet()) {
                try{
                    i++;

                    ProxiedPlayer _player = getProxy().getPlayer(_Entry.getKey());
                    if(_player == null){
                        dest.remove(_Entry.getKey());
                        continue;
                    }

                }catch(Exception e){
                    dest.remove(_Entry.getKey());
                    e.printStackTrace();
                }
            }
            i=0;
            for (Entry<String,State> playerState : Events.playerStates.entrySet()) {
                i++;
                if(playerState.getValue() == State.JOIN){
                    // check authentication status
                    State st = State.JOIN;
                    if(!CFG.contains(playerState.getKey())){
                        st = State.PRE_AUTH;
                    } else {
                        if(Instant.now().getEpochSecond() >= CFG.getLong(playerState.getKey()+".rememberFor")){

                            st = State.PRE_AUTH;
                            CFG.set(playerState.getKey()+".rememberFor",null);
                            save();
                        } else {
                            String IPAddress = getProxy().getInstance().getPlayer(playerState.getKey()).getSocketAddress().toString();
                            IPAddress = IPAddress.substring(0, IPAddress.indexOf(":"));
                            if(CFG.getString(playerState.getKey()+".lastIP") == IPAddress){
                                st = State.JOIN;
                                CFG.set(playerState.getKey()+".lastIP",null);
                                CFG.set(playerState.getKey()+".rememberFor",null);
                                save();
                                getProxy().getInstance().getPlayer(playerState.getKey()).sendMessage(new TextComponent("You have logged in with a different IP. Reauthentication is required"));
                            }else 
                                st = State.ENQUEUED;
                        }
                    }
                    String SendText = "";
                    if(st == State.PRE_AUTH) SendText = CFG.getString("lang.AUTH_REQUIRED");
                    // Queue will have a different status sender
                    getProxy().getPlayer(playerState.getKey()).sendMessage(new TextComponent(SendText));

                    Events.playerStates.remove(playerState.getKey());
                    Events.playerStates.put(playerState.getKey(), st);
                } else if(playerState.getValue() == State.POST_AUTH){
                    // authenticated
                    getProxy().getPlayer(playerState.getKey()).sendMessage(new TextComponent(CFG.getString("lang.ADD_TO_QUEUE")));
                    Events.playerStates.remove(playerState.getKey());
                    Events.playerStates.put(playerState.getKey(), State.ENQUEUED);
                } else if(playerState.getValue() == State.ENQUEUED){
                    // check player count, 
                    int current = getProxy().getInstance().getOnlineCount();
                    int allowed = CFG.getInt("queue.maxPlayers");

                    if(dest.isEmpty())continue;


                    if(allowed > current-dest.size()){
                        // login this player
                        Events.playerStates.remove(playerState.getKey());
                        dest.remove(playerState.getKey());

                        ProxiedPlayer player = getProxy().getInstance().getPlayer(playerState.getKey());
                        player.sendMessage(new TextComponent(CFG.getString("lang.JOINING")));
                        player.connect(getProxy().getInstance().getServerInfo(CFG.getString("eaqms.destination")));
                    } else {
                        // In queue
                        getProxy().getInstance().getPlayer(playerState.getKey()).sendMessage(new TextComponent(CFG.getString("lang.CURRENT_QUEUE_POSITION")+i));
                    }
                    
                } else if(playerState.getValue() == State.POST_AUTH_NEW){
                    // new account made. Inform user to authenticate again after confirming email
                    getProxy().getPlayer(playerState.getKey()).sendMessage(new TextComponent("After confirming your account simply reauthenticate!"));
                    Events.playerStates.remove(playerState.getKey());
                    Events.playerStates.put(playerState.getKey(), State.PRE_AUTH);
                }
            }
            Events.checkInformation(); // check if queued, and if the server has open slots, etc,etc
        },1,1,TimeUnit.SECONDS);
    }

    void process(){
        try
        {
            loadConfig();
        }catch(IOException e){
            if(!getDataFolder().exists()){
                getDataFolder().mkdir();
            }
            File f = new File(getDataFolder(), "config.yml");

            if(!f.exists()){
                try(InputStream is = getResourceAsStream("config.yml")){
                    CFG = new Configuration();
                    CFG.set("queue.maxPlayers",30);
                    CFG.set("eaqms.url", "https://mc.zontreck.dev/");
                    CFG.set("eaqms.rememberForDays",7);
                    CFG.set("lang.PEND_VERIFY", "Welcome to Zontreck.Dev Enforced Authentication and Queue Management System (ZdEAQMS)\n\nPlease wait!");
                    CFG.set("lang.ADD_TO_QUEUE", "You are now being added to the queue.");
                    CFG.set("lang.CURRENT_QUEUE_POSITION", "Your position in queue is: ");
                    CFG.set("lang.JOINING", "Congrats! Joining server now!");
                    CFG.set("lang.AUTH_REQUIRED", ChatColor.RED+"Authentication is required before proceeding\n\n"+ChatColor.GREEN+"To authenticate: /auth <string:znipassword>");
                    CFG.set("lang.DISCORD", "\nWhile you are waiting to connect, join our discord!\nhttps://discord.gg/h3SDpEp");
                    CFG.set("eaqms.destination", "Primary");

                    save();
                }catch(IOException ev){
                    ev.printStackTrace();
                }
            }
        }
    }

    public void save(){
        try{
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(CFG, new File(getDataFolder(), "config.yml"));
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    void loadConfig() throws IOException
    {
        CFG = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
    }

    
}