package Main;


import java.io.File;
import sx.blah.discord.api.IDiscordClient;
import utils.PropertiesUtil;

public class Bot{
	public static final String VERSION = "1.0.4";
	public static final String CREATOR_ID = "242526051442491392";
	
	public static IDiscordClient client;
	public static String USER_PATH;
	public static String BOT_TOKEN;
	public static String BOT_ID;
	public static String BOT_NAME;
	public static String BOT_AVATAR;
	
	public static void main(String[] args) { 

		USER_PATH = new File("").getAbsolutePath();
		USER_PATH = USER_PATH.replaceAll("\\\\", "/");
		PropertiesUtil pUtil = new PropertiesUtil(USER_PATH + "/bot.properties");
		
		if (!pUtil.fileExists)
			System.exit(0);
			
		BOT_TOKEN = pUtil.get("bot_token");
		BOT_AVATAR = pUtil.get("bot_avatar");
		BOT_NAME = pUtil.get("bot_name");
		
		client = BotUtils.getBuiltDiscordClient(BOT_TOKEN);
		client.getDispatcher().registerListener(new CommandHandler());
		client.getDispatcher().registerListener(new Initializer());
		client.login();
		
		client.idle();
		
		
	}

	public static IDiscordClient getClient() {
		return client;
	}
	
}
