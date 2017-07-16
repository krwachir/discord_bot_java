package Main;


import java.awt.Image;
import java.io.File;

import javax.swing.ImageIcon;

import musicplayer.MusicPlayer;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.util.DiscordException;
import utils.PropertiesUtil;

public class Bot {
	public static Bot INSTANCE;
	public IDiscordClient client;
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
		INSTANCE = login(BOT_TOKEN); 
		INSTANCE.client.getDispatcher().registerListener(new MusicPlayer());
		
		BOT_AVATAR = pUtil.get("bot_avatar");
		if (BOT_AVATAR != null) {
			try {INSTANCE.client.changeAvatar(sx.blah.discord.util.Image.forFile(new File(BOT_AVATAR)));} catch (Exception e) {}
		}
		
	}

	public Bot(IDiscordClient client) {
		this.client = client; 
	}

	public static Bot login(String token) {
		Bot bot = null; 

		ClientBuilder builder = new ClientBuilder();
		builder.withToken(token);
		try {
			IDiscordClient client = builder.login(); 
			bot = new Bot(client);
		} catch (DiscordException e) {
			System.err.println("Error occurred while logging in!");
			e.printStackTrace();
		}

		return bot;
	}

	public static IDiscordClient getClient() {
		return INSTANCE.client;
	}
}
