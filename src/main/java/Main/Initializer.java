package Main;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import modules.chatbot.ChatBot;
import modules.musicplayer.MusicPlayer;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent;

public class Initializer {
	@sx.blah.discord.api.events.EventSubscriber
    public void onReady(ReadyEvent event) {
		Bot.BOT_ID = Bot.client.getOurUser().getStringID();
		
		if (Bot.BOT_AVATAR != null) {
			try {Bot.client.changeAvatar(sx.blah.discord.util.Image.forFile(new File(Bot.USER_PATH + Bot.BOT_AVATAR)));} catch (Exception e) {}
		}
		
		new MusicPlayer();
		new ChatBot();
		
		Bot.client.online();
		System.out.println("Ready!");
    }

    @sx.blah.discord.api.events.EventSubscriber
    public void onDisconnect(DisconnectedEvent  event) {
        CompletableFuture.runAsync(() -> {
        	Bot.client.login();
        });
    }
}
