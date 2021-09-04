package io.github.boogiemonster1o1.eyeyoureadyforit.data;

import java.util.ArrayList;
import java.util.List;

import discord4j.common.util.Snowflake;

public class ChannelSpecificData {
	private Snowflake messageId;
	private EyeEntry current;
	private final Snowflake channelId;
	private final GuildSpecificData gsd;
	private TourneyData tourneyData = null;
	private final List<Snowflake> hintUsers = new ArrayList<>();

	public ChannelSpecificData(Snowflake channelId, GuildSpecificData gsd) {
		this.channelId = channelId;
		this.gsd = gsd;
	}

	public GuildSpecificData getGuildSpecificData() {
		return gsd;
	}

	public Snowflake getChannelId() {
		return channelId;
	}

	public Snowflake getMessageId() {
		return messageId;
	}

	public void setMessageId(Snowflake messageId) {
		this.messageId = messageId;
	}

	public EyeEntry getCurrent() {
		return current;
	}

	public boolean isTourney() {
		return this.tourneyData != null;
	}

	public void setTourneyData(TourneyData tourneyData) {
		this.tourneyData = tourneyData;
	}

	public TourneyData getTourneyData() {
		return tourneyData;
	}

	public void setCurrent(EyeEntry current) {
		this.current = current;
	}

	public void addHintUser(Snowflake snowflake) {
		this.hintUsers.add(snowflake);
	}

	public boolean usedHint(Snowflake snowflake) {
		return this.hintUsers.contains(snowflake);
	}

	public void reset() {
		synchronized (GuildSpecificData.LOCK) {
			this.setCurrent(null);
			this.setMessageId(null);
			hintUsers.clear();
		}
	}
}
