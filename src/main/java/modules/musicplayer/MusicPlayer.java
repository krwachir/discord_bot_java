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
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.IVoiceState;
import utils.PropertiesUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class MusicPlayer {

	private static final int INITIAL_VOL = 15;
	private static final int MAX_ALLOWED_VOL = 20;

	private static final int PLAYLIST_SIZE_LIMIT_FOR_DJ = 20;

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
			if (!isMusicTxtChannelOK(event))
				return;
			if (!checkPermission(event, "user"))
				return;
			play(event, args);
			deleteMessage(event);
		});

		CommandHandler.commandMap.put("pause", (event, args) -> {
			if (!isMusicTxtChannelOK(event))
				return;
			if (!checkPermission(event, "dj"))
				return;
			pause(event, args);
			deleteMessage(event);
		});

		CommandHandler.commandMap.put("skip", (event, args) -> {
			if (!isMusicTxtChannelOK(event))
				return;
			if (!checkPermission(event, "dj"))
				return;
			skip(event, args);
			deleteMessage(event);
		});

		CommandHandler.commandMap.put("shuffle", (event, args) -> {
			if (!isMusicTxtChannelOK(event))
				return;
			if (!checkPermission(event, "dj"))
				return;
			shuffle(event, args);
			deleteMessage(event);
		});

		CommandHandler.commandMap.put("clearq", (event, args) -> {
			if (!isMusicTxtChannelOK(event))
				return;
			if (!checkPermission(event, "dj"))
				return;
			clearQueue(event, args);
			deleteMessage(event);
		});

		CommandHandler.commandMap.put("list", (event, args) -> {
			if (!isMusicTxtChannelOK(event))
				return;
			if (!checkPermission(event, "user"))
				return;
			printList(event, args);
			deleteMessage(event);
		});

		CommandHandler.commandMap.put("ls", (event, args) -> {
			if (!isMusicTxtChannelOK(event))
				return;
			if (!checkPermission(event, "user"))
				return;
			printList(event, args);
			deleteMessage(event);
		});

		CommandHandler.commandMap.put("vol", (event, args) -> {
			if (!isMusicTxtChannelOK(event))
				return;
			if (!checkPermission(event, "dj"))
				return;
			setVolume(event, args);
			deleteMessage(event);
		});

		CommandHandler.commandMap.put("set_music", (event, args) -> {
			if (!checkPermission(event, "owner"))
				return;
			set(event, args);
			deleteMessage(event);
		});

		CommandHandler.commandMap.put("stop", (event, args) -> {
			if (!isMusicTxtChannelOK(event))
				return;
			if (!checkPermission(event, "dj"))
				return;
			stop(event, args);
			deleteMessage(event);
		});

		CommandHandler.commandMap.put("calldj", (event, args) -> {
			if (!isMusicTxtChannelOK(event))
				return;
			if (!checkPermission(event, "user"))
				return;
			callDJ(event, args);
			deleteMessage(event);
		});

		CommandHandler.commandMap.put("help", (event, args) -> {
			if (!isMusicTxtChannelOK(event))
				return;
			if (!checkPermission(event, "user"))
				return;
			help(event, args);
			deleteMessage(event);
		});

	}

	private void help(MessageReceivedEvent event, List<String> args) {
		IMessage message = event.getMessage();
		BotUtils.sendMessage(message.getChannel(),
				"```" + "Music Commands" + "\n For users:" + "\n  /play\tadd a song to the current queue"
						+ "\n  /skip\tskip the current playing track if it's requested by the user"
						+ "\n  /calldj\tmention DJs to call them for assistances" 
						+ "\n For DJs:"
						+ "\n  /play\tadd a song or a playlist (up to 20 songs), resume if pausing"
						+ "\n  /pause\tpause the current playing track" + "\n  /skip\tskip the current playing track"
						+ "\n  /shuffle\treorder all tracks in the queue randomly"
						+ "\n  /clearq\tclear all tracks waitng in queue"
						+ "\n  /stop\tend the current playing track and clear all tracks in queue"
						+ "\n  /list or /ls\tdisplay tracks in queue"
						+ "\n  /vol\tset the volume, up or down by percent"
						+ "```");

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

	private boolean isMusicTxtChannelOK(MessageEvent event) {
		IMessage message = event.getMessage();
		IChannel music_txt_channel = getGuildMusicTxtChannel(message.getGuild());

		if (music_txt_channel == null) {
			BotUtils.sendMessage(message.getChannel(),
					":x: `No designated text channel for music!\nPlease ask the owner or bot's creator to set with command:` ```/set music_txt_channel <channelName or channelID>```");
			return false;
		} else if (!music_txt_channel.getStringID().equals(message.getChannel().getStringID())) {
			BotUtils.sendMessage(message.getChannel(),
					":no_entry_sign: `Please use music commands in text channel: `" + music_txt_channel);
			return false;
		}

		return true;
	}

	private boolean checkPermission(MessageEvent event, String lowestAllowedRank) {
		IMessage message = event.getMessage();
		IUser requester = message.getAuthor();

		Set<IUser> bans = getBans(message.getGuild());
		if (bans.contains(message.getAuthor())) {
			BotUtils.sendMessage(message.getChannel(), ":no_entry_sign: `You're ban from using music commands`");
			return false;
		}

		if (requester.getStringID().equals(Main.Bot.CREATOR_ID)
				|| requester.getStringID().equals(message.getGuild().getOwner().getStringID())) {
			return true;
		}

		if (lowestAllowedRank.equals("dj")) {
			Set<IUser> djs = getDjs(message.getGuild());
			if (djs.contains(message.getAuthor())) {
				return true;
			}
		}

		if (lowestAllowedRank.equals("user"))
			return true;

		sendMessageToChannel(message.getChannel(), "You don't have this permission!", "");
		return false;
	}

	private void deleteMessage(MessageReceivedEvent event) {
		IMessage message = event.getMessage();
		message.delete();

	}

	private void play(MessageEvent event, List<String> args) {
		IMessage message = event.getMessage();

		GuildMusicManager musicManager = getGuildAudioPlayer(message.getGuild());
		if (musicManager.player.isPaused() && args.size() == 0) {
			musicManager.player.setPaused(false);
			sendMessageToChannel(message.getChannel(), ":arrow_forward: Resumed!", "");
			return;
		} else {
			String inputTxt = message.getContent()
					.substring(message.getContent().indexOf(" "), message.getContent().length()).trim();
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

	private void stop(MessageReceivedEvent event, List<String> args) {
		IMessage message = event.getMessage();

		GuildMusicManager musicManager = getGuildAudioPlayer(message.getGuild());
		musicManager.scheduler.clearQueue();

		musicManager = getGuildAudioPlayer(message.getChannel().getGuild());
		musicManager.scheduler.nextTrack();

		sendMessageToChannel(message.getChannel(), ":stop_button:  Stopped!", "");

	}

	private void pause(MessageEvent event, List<String> args) {
		IMessage message = event.getMessage();

		GuildMusicManager musicManager = getGuildAudioPlayer(message.getGuild());
		musicManager.player.setPaused(true);
		sendMessageToChannel(message.getChannel(), ":pause_button: Paused!", "");
	}

	private void skip(MessageEvent event, List<String> args) {
		IMessage message = event.getMessage();
		GuildMusicManager musicManager = getGuildAudioPlayer(message.getGuild());
		TrackDetail trackDetail = musicManager.scheduler.currentTrackDetail;
		if (!checkPermission(event, "dj") && !trackDetail.getRequesterLongID().equals(message.getAuthor().getLongID()))
			sendMessageToChannel(message.getChannel(), ":x: You cannot skip other people's track unless you're a DJ!", "");
		else
			skipTrack(message.getChannel());
		
	}

	private void shuffle(MessageEvent event, List<String> args) {
		IMessage message = event.getMessage();

		GuildMusicManager musicManager = getGuildAudioPlayer(message.getGuild());
		musicManager.scheduler.shuffle();
		sendMessageToChannel(message.getChannel(), ":twisted_rightwards_arrows: Playlist Shuffled!", "");
	}

	private void clearQueue(MessageEvent event, List<String> args) {
		IMessage message = event.getMessage();

		GuildMusicManager musicManager = getGuildAudioPlayer(message.getGuild());
		musicManager.scheduler.clearQueue();

		sendMessageToChannel(message.getChannel(), ":arrow_heading_down: Queue Cleared!", "");

	}

	private void printList(MessageEvent event, List<String> args) {
		IMessage message = event.getMessage();

		GuildMusicManager musicManager = getGuildAudioPlayer(message.getGuild());
		Queue<AudioTrack> AudioTracks = musicManager.scheduler.queue;
		sendMessageToChannel(message.getChannel(), "In queue: " + AudioTracks.size() + " track(s)", "");
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
				sendMessageToChannel(message.getChannel(), detailStr, "", false);
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

		sendMessageToChannel(message.getChannel(), detailStr, "", false);

	}

	private void set(MessageEvent event, List<String> args) {
		IMessage message = event.getMessage();
		if (args.size() < 2)
			return;

		IGuild guild = message.getGuild();
		IUser requester = message.getAuthor();
		IUser guildOwner = guild.getOwner();

		if (!requester.getStringID().equals(Main.Bot.CREATOR_ID)
				&& !requester.getStringID().equals(guildOwner.getStringID())) {
			sendMessageToChannel(message.getChannel(), "You don't have this permission!", "");
			return;
		}

		String key = args.get(0).trim().toLowerCase();
		String value = args.get(1).trim();

		if (args.get(0).trim().equals("null"))
			PropertiesUtil.removeGuildProperty(guild, args.get(0).trim());

		switch (key) {
		case "add_dj":
			Set<IUser> currentDJs = getDjs(guild);
			for (IUser mention : message.getMentions()) {
				if (mention.isBot())
					continue;
				else
					currentDJs.add(mention);
			}

			String newDJStr = null;
			for (IUser dj : currentDJs) {
				if (newDJStr == null)
					newDJStr = dj.getStringID();
				else
					newDJStr += "," + dj.getStringID();
			}

			PropertiesUtil.setGuildProperty(guild, "music_dj", newDJStr);
			break;

		case "remove_dj":
			removeUserFrom(message, "music_dj");
			break;

		case "ban":
			Set<IUser> currentBans = getBans(guild);
			for (IUser mention : message.getMentions()) {
				if (mention.isBot())
					continue;
				else
					currentBans.add(mention);
			}

			String newBanStr = null;
			for (IUser ban : currentBans) {
				if (newBanStr == null)
					newBanStr = ban.getStringID();
				else
					newBanStr += "," + ban.getStringID();
			}

			PropertiesUtil.setGuildProperty(guild, "music_ban", newBanStr);
			removeUserFrom(message, "music_dj");
			break;

		case "unban":
			removeUserFrom(message, "music_ban");
			break;

		default:
			PropertiesUtil.setGuildProperty(guild, key, value);
			break;
		}

	}

	private void removeUserFrom(IMessage message, String property) {
		String guildStr = PropertiesUtil.getGuildConfig(message.getGuild(), property);
		if (guildStr != null) {
			for (IUser mention : message.getMentions()) {
				if (mention.isBot())
					continue;

				try {
					guildStr = guildStr.replaceAll(mention.getStringID(), "");
					guildStr = guildStr.trim();
					if (guildStr.substring(0, 0).equals(","))
						guildStr = guildStr.substring(1, guildStr.length());

					if (guildStr.substring(guildStr.length(), guildStr.length()).equals(","))
						guildStr = guildStr.substring(0, guildStr.length() - 1);
				} catch (Exception e) {
				}
			}

			guildStr = guildStr.trim();
			if (guildStr.length() == 0) {
				PropertiesUtil.removeGuildProperty(message.getGuild(), property);
			} else {
				PropertiesUtil.setGuildProperty(message.getGuild(), property, guildStr);
			}
		}
	}

	private void setVolume(MessageEvent event, List<String> args) {
		IMessage message = event.getMessage();
		if (args.size() == 1) {
			try {
				int volPercent = Integer.parseInt(args.get(0).trim());
				GuildMusicManager musicManager = getGuildAudioPlayer(message.getGuild());
				musicManager.player.setVolume(MAX_ALLOWED_VOL * volPercent / 100);

				sendMessageToChannel(message.getChannel(), ":signal_strength: Volume is set to " + volPercent + "%",
						"");
			} catch (Exception e) {
			}
		}
	}

	private void callDJ(MessageReceivedEvent event, List<String> args) {
		IMessage message = event.getMessage();
		Set<IUser> djs = getDjs(message.getGuild());
		String callDJStr = ":loudspeaker: Call DJ(s): ";
		for (IUser dj : djs) {
			callDJStr += " " + dj;
		}

		sendMessageToChannel(message.getChannel(), callDJStr, "");

	}

	public static String getStringTrackDetail(TrackDetail trackDetail) {

		return trackDetail.audioTrack.getInfo().title + "\trequested by: "
				+ Main.Bot.getClient().getUserByID(trackDetail.requesterLongID);

	}

	private void loadAndPlay(final TrackDetail trackDetail, final String trackUrl) {
		GuildMusicManager musicManager = getGuildAudioPlayer(trackDetail.getGuild());

		playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(AudioTrack track) {
				sendMessageToChannel(trackDetail.getTxtChannel(),
						":arrow_heading_up: Adding to queue " + track.getInfo().title + "\t by "
								+ trackDetail.getGuild().getUserByID(trackDetail.requesterLongID),
						"");
				play(trackDetail, musicManager, track);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {

				int trackCount = playlist.getTracks().size();
				boolean canAdd = false;
				if (trackDetail.getRequesterLongID().equals(Main.Bot.CREATOR_ID)
						|| trackDetail.getRequesterLongID().equals(trackDetail.getGuild().getOwnerLongID())) {
					canAdd = true;
				} else if (getDjs(trackDetail.getGuild())
						.contains(trackDetail.getGuild().getUserByID(trackDetail.getRequesterLongID()))
						&& trackCount <= PLAYLIST_SIZE_LIMIT_FOR_DJ) {
					canAdd = true;
				}

				if (canAdd) {
					sendMessageToChannel(trackDetail.getTxtChannel(),
							":arrow_heading_up: Adding to queue " + playlist.getTracks().size()
									+ " track(s)\tfrom playlist: " + playlist.getName() + "\t by "
									+ trackDetail.getGuild().getUserByID(trackDetail.requesterLongID),
							"");

					for (AudioTrack track : playlist.getTracks()) {
						TrackDetail td = new TrackDetail(trackDetail.requesterMessage);
						trackDetail.setTrack(track);
						play(td, musicManager, track);
					}
				} else {
					sendMessageToChannel(trackDetail.getTxtChannel(),
							":x: You either don't have permission or the playlist size is exceed limit!", "");
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

	protected Set<IUser> getDjs(IGuild guild) {
		return gets(guild, "music_dj");
	}

	protected Set<IUser> getBans(IGuild guild) {
		return gets(guild, "music_ban");
	}
	
	protected Set<IUser> gets(IGuild guild, String getStr) {
		Set<IUser> users = new HashSet<>();

		String guildStr = PropertiesUtil.getGuildConfig(guild, getStr);
		if (guildStr == null)
			return users;

		String[] userStrs = guildStr.split(",");
		for (String djStr : userStrs) {
			try {
				Long DJLongID = Long.parseLong(djStr.trim());
				IUser djUser = null;

				try {
					djUser = guild.getUserByID(DJLongID);
				} catch (Exception e) {
				}

				if (djUser != null) {
					users.add(djUser);
				}

			} catch (Exception e) {}
		}
		return users;
	}

	private void play(TrackDetail trackDetail, GuildMusicManager musicManager, AudioTrack track) {
		trackDetail.setTrack(track);
		connectToVoiceChannel(trackDetail);
		musicManager.scheduler.queue(trackDetail);
	}

	private void skipTrack(IChannel channel) {
		GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
		musicManager.scheduler.nextTrack();
		sendMessageToChannel(channel, ":track_next: Skipped to next track.", "");
	}

	public static void sendMessageToChannel(IChannel channel, String message, String maskType) {
		sendMessageToChannel(channel, message, maskType, false);
	}

	public static void sendMessageToChannel(IChannel channel, String message, String maskType, boolean setHeader) {
		try {
			channel.sendMessage(setHeader ? (":headphones: `MusicPlayer` ") : "" + maskType + message + maskType);
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