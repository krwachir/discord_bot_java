package Main;


import java.io.File;
import java.util.concurrent.CompletableFuture;

import modules.musicplayer.MusicPlayer;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent;
import utils.PropertiesUtil;

public class Bot {
	public static IDiscordClient client;
	public static final String CREATOR_ID = "242526051442491392";
	
	public static String USER_PATH;
	public static String BOT_TOKEN;
	public static String BOT_AVATAR;
	
	public static void main(String[] args) { 

		USER_PATH = new File("").getAbsolutePath();
		USER_PATH = USER_PATH.replaceAll("\\\\", "/");
		PropertiesUtil pUtil = new PropertiesUtil(USER_PATH + "/bot.properties");
		
		if (!pUtil.fileExists)
			System.exit(0);
			
		BOT_TOKEN = pUtil.get("bot_token");
		client = BotUtils.getBuiltDiscordClient(BOT_TOKEN);
		
		client.getDispatcher().registerListener(new CommandHandler());
		new MusicPlayer();

		client.login();
		
		
		BOT_AVATAR = pUtil.get("bot_avatar");
		if (BOT_AVATAR != null) {
			try {client.changeAvatar(sx.blah.discord.util.Image.forFile(new File(USER_PATH + BOT_AVATAR)));} catch (Exception e) {}
		}
		
	}

	public static IDiscordClient getClient() {
		return client;
	}
	
	@sx.blah.discord.api.events.EventSubscriber
    public void onReady(ReadyEvent event) {
       // log.info("*** Discord bot armed ***");
    }

    @sx.blah.discord.api.events.EventSubscriber
    public void onDisconnect(DisconnectedEvent  event) {
        CompletableFuture.runAsync(() -> {
        	client.login();
        });
    }

  
	
	
}
