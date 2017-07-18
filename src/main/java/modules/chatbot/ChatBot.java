package modules.chatbot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.chatterbotapi.ChatterBot;
import com.google.code.chatterbotapi.ChatterBotFactory;
import com.google.code.chatterbotapi.ChatterBotSession;
import com.google.code.chatterbotapi.ChatterBotType;

import Main.Bot;
import Main.CommandHandler;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

public class ChatBot {
	
	ChatterBot bot;
	Map<Long, ChatterBotSession> userBotSessions;
	
	private static final Logger log = LoggerFactory.getLogger(ChatBot.class);
	
	public ChatBot(){
		
		userBotSessions = new HashMap<>();
		ChatterBotFactory factory = new ChatterBotFactory();
       /* ChatterBot bot1 = factory.create(ChatterBotType.CLEVERBOT);
        ChatterBotSession bot1session = bot1.createSession();*/

        try {
        	//bot = factory.create(ChatterBotType.CLEVERBOT, "CC3c13sKaHrkAdnw8DK1ZdsL4PA");
			bot = factory.create(ChatterBotType.PANDORABOTS, "b0dafd24ee35a477");
			// Register music Commands
			CommandHandler.commandMap.put("<@!" + Bot.BOT_ID + ">", (event, args) -> {
				try {
					chat(event, args);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private void chat(MessageReceivedEvent event, List<String> args) throws Exception {
		IMessage message = event.getMessage();
		IUser user = event.getAuthor();
		String inputTxt = message.getContent().substring(message.getContent().indexOf(" "), message.getContent().length()).trim();
		
		ChatterBotSession botSession = userBotSessions.get(user.getLongID());
		if (botSession == null) {
			botSession = bot.createSession();
			userBotSessions.put(user.getLongID(), botSession);
		}
		
		String reply;
		try {
			reply = botSession.think(inputTxt);
		} catch (Exception e) {
			botSession = bot.createSession();
			userBotSessions.put(user.getLongID(), botSession);
			
			reply = botSession.think(inputTxt);
		}
		
		if (reply != null)
			sendMessageToChannel(message, reply);
		
		
	}
	
	public static void sendMessageToChannel(IMessage message, String messageStr) {
		try {
			message.getChannel().sendMessage(message.getAuthor() + " " + messageStr);
		} catch (Exception e) {
			log.warn("Failed to send message {} to {}", message, message.getChannel().getName(), e);
		}
	}
}
