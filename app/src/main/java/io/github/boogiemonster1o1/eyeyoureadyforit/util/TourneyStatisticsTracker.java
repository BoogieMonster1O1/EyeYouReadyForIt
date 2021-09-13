package io.github.boogiemonster1o1.eyeyoureadyforit.util;

import discord4j.common.util.Snowflake;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.DataDao;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.UserStatistic;

import java.util.HashMap;
import java.util.Map;

public final class TourneyStatisticsTracker {
	//shitcode of the highest order
	// agreed ^ 100%

	private final HashMap<Snowflake, UserStatistic> statsMap = new HashMap<>();
	private static final Map<Snowflake, Map<Snowflake, TourneyStatisticsTracker>> TOURNEY_STATISTICS_TRACKER_MAP = new HashMap<>();
	private int missed;
	private final Snowflake guildId;
	private final Snowflake channelId;

	public TourneyStatisticsTracker(Snowflake guildId, Snowflake channelId) {
		this.guildId = guildId;
		this.channelId = channelId;
	}

	public static TourneyStatisticsTracker get(Snowflake guildId, Snowflake channelId) {
		return TOURNEY_STATISTICS_TRACKER_MAP.computeIfAbsent(guildId, (gid) -> new HashMap<>()).computeIfAbsent(channelId, (cid) -> new TourneyStatisticsTracker(guildId, channelId));
	}

	public static void reset(Snowflake guildId, Snowflake channelId) {
		TOURNEY_STATISTICS_TRACKER_MAP.get(guildId).put(channelId, new TourneyStatisticsTracker(guildId, channelId));
	}

	public void addCorrect(Snowflake user) {
		statsMap.put(user, statsMap.getOrDefault(user, new UserStatistic()).add(new UserStatistic(1, 0, 0, 0, 0)));
	}

	public void addWrong(Snowflake user) {
		statsMap.put(user, statsMap.getOrDefault(user, new UserStatistic()).add(new UserStatistic(0, 1, 0, 0, 0)));
	}

	public void addHint(Snowflake user) {
		statsMap.put(user, statsMap.getOrDefault(user, new UserStatistic()).add(new UserStatistic(0, 0, 1, 0, 0)));
	}

	public void setWon(Snowflake user) {
		statsMap.put(user, statsMap.getOrDefault(user, new UserStatistic()).add(new UserStatistic(0, 0, 0, 1, 0)));
	}

	public void addMissed() {
		this.missed++;
	}

	public void commit() {
		DataSource.get().withExtension(DataDao.class, dao -> {
			for (Map.Entry<Snowflake, UserStatistic> entry : statsMap.entrySet()) {
				dao.addTourneyUserStats(
						guildId.asString(),
						entry.getKey().asLong(),
						entry.getValue().getCorrectAnswers(),
						entry.getValue().getWrongAnswers(),
						entry.getValue().getHintUses(),
						entry.getValue().getGamesWon()
				);
			}

			dao.addTourneyGuildStats(guildId.asString(), missed);
			return null;
		});
	}

	public Snowflake getGuildId() {
		return guildId;
	}

	public Snowflake getChannelId() {
		return channelId;
	}
}
