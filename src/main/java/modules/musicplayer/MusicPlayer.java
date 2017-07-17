package modules.musicplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import Main.BotUtils;
import Main.CommandHandler;
import modules.youtubeapi.Search;

import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.IVoiceState;
import utils.PropertiesUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class MusicPlayer {

	private static final int INITIAL_VOL = 10;
	private static final int MAX_ALLOWED_VOL = 20;

	private static final Logger log = LoggerFactory.getLogger(MusicPlayer.class);

	public static void main(String[] args) throws Exception {
		IDiscordClient client = new ClientBuilder().withToken(System.getProperty("botToken")).login();

		client.getDispatcher().registerListener(new MusicPlayer());
	}

	private final AudioPlayerManager playerManager;
	private final Map<Long, GuildMusicManager> musicManagers;
	private final Map<Long, TrackDetail> trackDetails;

	public MusicPlayer() {
		this.musicManagers = new HashMap<>();
		this.trackDetails = new HashMap<>();
		this.playerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerRemoteSources(playerManager);
		AudioSourceManagers.registerLocalSource(playerManager);

		// Register music Commands
		CommandHandler.commandMap.put("play", (event, args) -> {
			play(event, args);
		});

		CommandHandler.commandMap.put("pause", (event, args) -> {
			pause(event, args);
		});

		CommandHandler.commandMap.put("skip", (event, args) -> {
			skip(event, args);
		});

		CommandHandler.commandMap.put("shuffle", (event, args) -> {
			shuffle(event, args);
		});

		CommandHandler.commandMap.put("clearq", (event, args) -> {
			clearQueue(event, args);
		});

		CommandHandler.commandMap.put("list", (event, args) -> {
			printList(event, args);
		});

		CommandHandler.commandMap.put("ls", (event, args) -> {
			printList(event, args);
		});

		CommandHandler.commandMap.put("vol", (event, args) -> {
			setVolume(event, args);
		});

		CommandHandler.commandMap.put("set", (event, args) -> {
			set(event, args);
		});

	}

	private synchronized GuildMusicManager getGuildAudioPlayer(IGuild guild) {
		long guildId = guild.getLongID();
		GuildMusicManager musicManager = musicManagers.get(guildId);

		if (musicManager == null) {
			musicManager = new GuildMusicManager(playerManager);
			musicManager.player.setVolume(INITIAL_VOL);
			musicManagers.put(guildId, musicManager);
			leaveVoiceChannel(guild);
			// connectToVoiceChannel(guild);

			IChannel musicTxtChannel = getGuildMusicTxtChannel(guild);
			if (musicTxtChannel != null) {
				try {
					musicTxtChannel.changeTopic("");
				} catch (Exception e) {
				}
			}
		}

		guild.getAudioManager().setAudioProvider(musicManager.getAudioProvider());

		return musicManager;
	}

	private boolean isMusicTxtChannelOK(IMessage message) {
		IChannel music_txt_channel = getGuildMusicTxtChannel(message.getGuild());
		if (music_txt_channel == null) {
			BotUtils.sendMessage(message.getChannel(), ":x: `No designated text channel for music!\nPlease ask the owner or bot's creator to set with command:` ```/set music_txt_channel <channelName or channelID>```");
			return false;
		} else if (!music_txt_channel.getStringID().equals(message.getChannel().getStringID())) {
			BotUtils.sendMessage(message.getChannel(), ":no_entry_sign: `Please use music commands in text channel: `" + music_txt_channel);
			return false;
		}
		return true;
	}
	
	private void play(MessageEvent event, List<String> args) {
		IMessage message = event.getMessage();
		if (!isMusicTxtChannelOK(message))
			return;

		GuildMusicManager musicManager = getGuildAudioPlayer(message.getGuild());
		if (musicManager.player.isPaused() && args.size() == 0) {
			musicManager.player.setPaused(false);
			sendMessageToChannel(message.getChannel(), "Resumed!", "`");
			return;
		} else {
			String inputTxt = message.getContent().substring(message.getContent().indexOf(" "), message.getContent().length()).trim();
			UrlValidator defaultValidator = new UrlValidator(); // default schemes
			if (!defaultValidator.isValid(inputTxt)) {
				inputTxt = Search.search(inputTxt);
			}
			if (inputTxt != null) {
				TrackDetail trackDetail = new TrackDetail(message);
				trackDetails.put(message.getLongID(), trackDetail);
				loadAndPlay(trackDetail, inputTxt);
			}
			message.delete();
		}

	}

	private void pause(MessageEvent event, List<String> args) {
		IMessage message = event.getMessage();
		if (!isMusicTxtChannelOK(message))
			return;
		
		GuildMusicManager musicManager = getGuildAudioPlayer(message.getGuild());
		musicManager.player.setPaused(true);
		sendMessageToChannel(message.getChannel(), "Paused!", "`");
	}

	private void skip(MessageEvent event, List<String> args) {
		IMessage message = event.getMessage();
		if (!isMusicTxtChannelOK(message))
			return;
		
		skipTrack(message.getChannel());
	}

	private void shuffle(MessageEvent event, List<String> args) {
		IMessage message = event.getMessage();
		if (!isMusicTxtChannelOK(message))
			return;
		
		GuildMusicManager musicManager = getGuildAudioPlayer(message.getGuild());
		musicManager.scheduler.shuffle();
		sendMessageToChannel(message.getChannel(), "Playlist Shuffled!", "`");
	}

	private void clearQueue(MessageEvent event, List<String> args) {
		IMessage message = event.getMessage();
		if (!isMusicTxtChannelOK(message))
			return;
		
		GuildMusicManager musicManager = getGuildAudioPlayer(message.getGuild());
		musicManager.scheduler.clearQueue();

		sendMessageToChannel(message.getChannel(), "Queue Cleared!", "`");

	}

	private void printList(MessageEvent event, List<String> args) {
		IMessage message = event.getMessage();
		if (!isMusicTxtChannelOK(message))
			return;
		
		GuildMusicManager musicManager = getGuildAudioPlayer(message.getGuild());
		Queue<AudioTrack> AudioTracks = musicManager.scheduler.queue;
		sendMessageToChannel(message.getChannel(), "In queue: " + AudioTracks.size() + " track(s)", "`");
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String detailStr = "\n";
		int i = 1;
		for (AudioTrack audioTrack : AudioTracks) {
			TrackDetail trackDetail = musicManager.scheduler.trackDetails.get(audioTrack);
			detailStr += "[" + (i) + "]" + "\t" + getStringTrackDetail(trackDetail);

			if (i % 10 == 0) {
				sendMessageToChannel(message.getChannel(), detailStr, "");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				detailStr = "\n";
			} else {
				detailStr += "\n";
			}
			i++;
		}

		sendMessageToChannel(message.getChannel(), detailStr, "");

	}

	private void set(MessageEvent event, List<String> args) {
		IMessage message = event.getMessage();
		if (args.size() != 2)
			return;

		IGuild guild = message.getGuild();
		IUser requester = message.getAuthor();
		IUser guildOwner = guild.getOwner();

		if (requester.getStringID().equals(Main.Bot.CREATOR_ID)
				|| requester.getStringID().equals(guildOwner.getStringID())) {
			if (args.get(0).trim().equals("null"))
				PropertiesUtil.removeGuildProperty(guild, args.get(0).trim());
			else {
				PropertiesUtil.setGuildProperty(guild, args.get(0).trim(), args.get(1).trim());
			}

		} else {
			sendMessageToChannel(message.getChannel(), "You don't have this permission!", "`");
		}
	}

	private void setVolume(MessageEvent event, List<String> args) {
		IMessage message = event.getMessage();
		if (args.size() == 2) {
			try {
				int volPercent = Integer.parseInt(args.get(0).trim());
				GuildMusicManager musicManager = getGuildAudioPlayer(message.getGuild());
				musicManager.player.setVolume(MAX_ALLOWED_VOL * volPercent / 100);

				sendMessageToChannel(message.getChannel(), "Volume is set to " + volPercent + "%", "`");
			} catch (Exception e) {
			}
		}
	}

	public String getStringTrackDetail(TrackDetail trackDetail) {

		return trackDetail.audioTrack.getInfo().title + "\trequested by: "
				+ Main.Bot.getClient().getUserByID(trackDetail.requesterLongID);

	}

	private void loadAndPlay(final TrackDetail trackDetail, final String trackUrl) {
		GuildMusicManager musicManager = getGuildAudioPlayer(trackDetail.getGuild());

		playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(AudioTrack track) {
				sendMessageToChannel(trackDetail.getTxtChannel(), "Adding to queue " + track.getInfo().title + "\t by "
						+ trackDetail.getGuild().getUserByID(trackDetail.requesterLongID), "");
				play(trackDetail, musicManager, track);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {

				sendMessageToChannel(trackDetail.getTxtChannel(),
						"Adding to queue " + playlist.getTracks().size() + " track(s)\tfrom playlist: "
								+ playlist.getName() + "\t by "
								+ trackDetail.getGuild().getUserByID(trackDetail.requesterLongID),
						"");

				for (AudioTrack track : playlist.getTracks()) {
					TrackDetail td = new TrackDetail(trackDetail.requesterMessage);
					trackDetail.setTrack(track);
					play(td, musicManager, track);
				}
			}

			@Override
			public void noMatches() {
				sendMessageToChannel(trackDetail.getTxtChannel(), "Nothing found by " + trackUrl, "`");
			}

			@Override
			public void loadFailed(FriendlyException exception) {
				sendMessageToChannel(trackDetail.getTxtChannel(), "Could not play: " + exception.getMessage(), "`");
			}
		});
	}

	private void play(TrackDetail trackDetail, GuildMusicManager musicManager, AudioTrack track) {
		trackDetail.setTrack(track);
		connectToVoiceChannel(trackDetail);
		musicManager.scheduler.queue(trackDetail);
	}

	private void skipTrack(IChannel channel) {
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
		musicManager.scheduler.nextTrack();

		sendMessageToChannel(channel, "Skipped to next track.", "`");
	}

	public static void sendMessageToChannel(IChannel channel, String message, String maskType) {
		try {
			channel.sendMessage(":musical_note: `MusicPlayer` " + maskType + message + maskType);
		} catch (Exception e) {
			log.warn("Failed to send message {} to {}", message, channel.getName(), e);
		}
	}

	private static void connectToVoiceChannel(TrackDetail trackDetail) {
		connectToVoiceChannel(trackDetail.getGuild());
	}

	private static void connectToVoiceChannel(IGuild guild) {
		String voice_channel_str = PropertiesUtil.getGuildConfig(guild, "music_voice_channel");
		if (voice_channel_str != null) {
			IVoiceChannel voice_channel = null;
			try {
				voice_channel = guild.getVoiceChannelsByName(voice_channel_str).get(0);
			} catch (Exception e) {
			}

			if (voice_channel == null) {
				Long txtChannelLongID = Long.parseLong(voice_channel_str);
				voice_channel = guild.getVoiceChannelByID(txtChannelLongID);
			}

			if (voice_channel != null) {
				voice_channel.join();
				return;
			}
		}

		for (IVoiceChannel vc : guild.getVoiceChannels()) {
			if (vc.getName().equals("General")) {
				if (vc.isConnected())
					return;
				else {
					vc.join();
					return;
				}
			}
		}
	}

	public static IChannel getGuildMusicTxtChannel(IGuild guild) {
		String music_txt_channel = PropertiesUtil.getGuildConfig(guild, "music_txt_channel");
		if (music_txt_channel != null) {
			IChannel txtChannel = null;
			try {
				txtChannel = guild.getChannelsByName(music_txt_channel).get(0);
			} catch (Exception e) {
			}
			if (txtChannel == null) {
				Long txtChannelLongID = Long.parseLong(music_txt_channel);
				txtChannel = guild.getChannelByID(txtChannelLongID);
			}

			if (txtChannel != null) {
				return txtChannel;
			}

		}

		return null;
	}

	private static void leaveVoiceChannel(IGuild guild) {
		for (IVoiceChannel vc : guild.getVoiceChannels()) {
			if (vc.getName().equals("General")) {
				if (vc.isConnected())
					vc.leave();
			}
		}
	}

	class TrackDetail {
		IMessage requesterMessage;
		Long requesterLongID;
		AudioTrack audioTrack;

		public TrackDetail(IMessage message) {
			this.requesterMessage = message;
			this.requesterLongID = message.getAuthor().getLongID();
		}

		public AudioTrack getTrack() {
			return this.audioTrack;
		}

		public void setTrack(AudioTrack audioTrack) {
			this.audioTrack = audioTrack;
		}

		public IVoiceChannel getVoiceChannel() {
			try {
				IVoiceState voiceState = requesterMessage.getAuthor()
						.getVoiceStateForGuild(requesterMessage.getGuild());
				return voiceState.getChannel();
			} catch (Exception e) {
				return null;
			}

		}

		public IGuild getGuild() {
			return requesterMessage.getGuild();
		}

		public IChannel getTxtChannel() {
			return requesterMessage.getChannel();
		}

		public Long getRequesterLongID() {
			return requesterLongID;
		}
	}
}