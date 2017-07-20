package modules.musicplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import modules.musicplayer.MusicPlayer.TrackDetail;
import sx.blah.discord.handle.obj.IChannel;

/**
 * This class schedules tracks for the audio player. It contains the queue of
 * tracks.
 */
public class TrackScheduler extends AudioEventAdapter {
	private final AudioPlayer player;
	public Queue<AudioTrack> queue;
	public Map<AudioTrack, TrackDetail> trackDetails;
	public TrackDetail currentTrackDetail;

	/**
	 * @param player
	 *            The audio player this scheduler uses
	 */
	public TrackScheduler(AudioPlayer player) {
		this.player = player;
		this.queue = new LinkedBlockingQueue<>();
		this.trackDetails = new HashMap<>();
		this.currentTrackDetail = null;
	}

	/**
	 * Add the next track to queue or play right away if nothing is in the queue.
	 *
	 * @param trackDetail
	 *            The track to play or add to queue.
	 */
	public void queue(TrackDetail trackDetail) {
		// Calling startTrack with the noInterrupt set to true will start the track only
		// if nothing is currently playing. If
		// something is playing, it returns false and does nothing. In that case the
		// player was already playing so this
		// track goes to the queue instead.

		if (!player.startTrack(trackDetail.getTrack(), true)) {
			queue.offer(trackDetail.getTrack());
			trackDetails.put(trackDetail.getTrack(), trackDetail);
		} else {
			showDetail(trackDetail, false);
		}

	}

	/**
	 * Start the next track, stopping the current one if it is playing.
	 */
	public void nextTrack() {
		// Start the next track, regardless of if something is already playing or not.
		// In case queue was empty, we are
		// giving null to startTrack, which is a valid argument and will simply stop the
		// player.
		AudioTrack track = queue.poll();
		player.startTrack(track, false);

		try {
			currentTrackDetail = trackDetails.get(track);
			trackDetails.remove(track);
			showDetail(currentTrackDetail, true);
		} catch (Exception e) {
		}
	}

	private void showDetail(TrackDetail trackDetail, boolean sendMessage) {
		try {
			IChannel txtChannel = MusicPlayer.getGuildMusicTxtChannel(trackDetail.getGuild());
			if (txtChannel != null) {
				MusicPlayer.sendMessageToChannel(trackDetail.getTxtChannel(), ":arrow_forward: Playing: " + MusicPlayer.getStringTrackDetail(trackDetail),
						"");
			}

		} catch (Exception e) {
		}
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		// Only start the next track if the end reason is suitable for it (FINISHED or
		// LOAD_FAILED)
		if (endReason.mayStartNext) {
			nextTrack();
		}
	}

	public void clearQueue() {
		queue.clear();
		trackDetails.clear();
	}

	public void shuffle() {
		ArrayList<AudioTrack> trackList = new ArrayList<>();
		trackList.addAll(queue);
		queue.clear();

		Collections.shuffle(trackList);
		for (AudioTrack track : trackList)
			queue.offer(track);
	}

}