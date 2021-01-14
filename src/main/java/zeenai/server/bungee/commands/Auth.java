package zeenai.server.bungee.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.Configuration;
import zeenai.server.bungee.Events;
import zeenai.server.bungee.State;
import zeenai.server.bungee.ZNIPlugin;
import zeenai.server.bungee.ZontreckDevConnection;

import java.security.*;
import java.time.Instant;
import java.util.Random;

public class Auth extends Command {

    public Auth(String name) {
        super(name);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(new TextComponent("Correct usage: /auth <string:Password>\nNew Account: /auth <string:Password> <string:challenge>"));
            return;
        }

        ZontreckDevConnection ZDC = new ZontreckDevConnection();
        // do validation stuff
        ProxiedPlayer p = ZNIPlugin.getInstance().getProxy().getInstance().getPlayer(sender.getName());
        ZontreckDevConnection.Reply rep = ZDC.SendRequest(false, true, "auth.php?user="+sender.getName()+"&pass="+args[0]+"&uuid="+p.getUniqueId().toString(), "");

        if(rep.body.compareToIgnoreCase("No Such User")==0){
            // Check arg count, and make account if applicable
            if(args.length==2){
                // make account
                rep = ZDC.SendRequest(false, true, "register.php?user="+sender.getName()+"&pass="+args[0]+"&challenge="+args[1]+"&uuid="+p.getUniqueId().toString(), "");
                String[] spl = rep.body.split(";;");
                if(spl[0].compareToIgnoreCase("Account Exists")==0){
                    sender.sendMessage(new TextComponent("ERROR CODE 0x01: ACCOUNT NOT EXIST, REGISTER RETURNED EXIST.\n\n"+ChatColor.RED+"Please ping Aria on discord for assistance."));
                } else if(spl[0].compareToIgnoreCase("Created") == 0){
                    sender.sendMessage(new TextComponent("SUCCESS\n\nYour account was created successfully. However you must first confirm to sign in. Activation link: "));
                    BaseComponent[] comp = TextComponent.fromLegacyText(spl[1], ChatColor.BLUE);
                    sender.sendMessage(comp[0]);
                    Events.playerStates.remove(sender.getName());
                    Events.playerStates.put(sender.getName(), State.POST_AUTH_NEW);
                }
            }else{
                sender.sendMessage(new TextComponent("Please use this command usage: /auth <string:Password> <string:Challenge>"));
            }
        } else if(rep.body.compareToIgnoreCase("Locked") == 0){
            sender.sendMessage(new TextComponent("The account is locked due to too many failed login attempts. Ping @Aria on discord for assistance. You may also ping any moderators listed on Discord."));
        } else if(rep.body.compareToIgnoreCase("Login Failed") == 0){
            sender.sendMessage(new TextComponent("Login has failed"));
        } else if(rep.body.compareToIgnoreCase("Not Activated") == 0){
            sender.sendMessage(new TextComponent(ChatColor.RED+"Your account is not activated. Please re-create the account for your activation link."));
        } else if(rep.body.compareToIgnoreCase("Proceed") == 0){
            sender.sendMessage(new TextComponent(ChatColor.GREEN+"Success!"+ChatColor.AQUA+"\n\nServer capacity will be checked"));
            Events.playerStates.remove(sender.getName());
            Events.playerStates.put(sender.getName(), State.POST_AUTH);
            String IPAddress = p.getSocketAddress().toString();
            IPAddress = IPAddress.substring(0, IPAddress.indexOf(":"));
            ZNIPlugin.getInstance().CFG.set(sender.getName()+".rememberFor", Instant.now().getEpochSecond()+(60*60*24*ZNIPlugin.getInstance().CFG.getInt("eaqms.rememberForDays")));
            ZNIPlugin.getInstance().CFG.set(sender.getName()+".lastIP", IPAddress);
            ZNIPlugin.getInstance().save();
        }else {
            sender.sendMessage(new TextComponent("Unknown response: '"+rep.body+"'"));
        }
    }
    
}