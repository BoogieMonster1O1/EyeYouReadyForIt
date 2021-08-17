package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.EyeEntry;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ModeContext;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.TourneyData;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public final class TourneyCommand {
	public static Publisher<?> handle(SlashCommandEvent event, GuildSpecificData gsd) {
		if (gsd.isTourney()) {
			event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**There is already a tourney**"));
		}
		int rounds = (int) event.getOption("rounds").orElseThrow().getValue().orElseThrow().asLong();
		if (rounds < 3) {
			event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**Choose a valid number equal to or above 3**"));
		}
		boolean disableHints = event.getOption("hintsdisabled").flatMap(ApplicationCommandInteractionOption::getValue).map(ApplicationCommandInteractionOptionValue::asBoolean).orElse(false);
		TourneyData tourneyData = new TourneyData(rounds, disableHints);
		gsd.setTourneyData(tourneyData);

		next(gsd, 0L, event.getInteraction().getChannel(), true);

		return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage("Let the games begin"));
	}

	public static void next(GuildSpecificData gsd, long answerer, Mono<MessageChannel> channelMono, boolean justStarted) {
		TourneyData data = gsd.getTourneyData();
		int round;
		if (justStarted) {
			channelMono.flatMap(channel -> channel.createMessage("Starting Tourney")).subscribe();
			data.setRound(round = 0);
		} else {
			data.getLeaderboard()[data.getRound()] = answerer;
			if (data.getRound() + 1 >= data.getMaxRounds()) {
				channelMono.flatMap(channel -> channel.createEmbed(spec -> {
					spec.setTitle("Tourney is over :crab:");
					long[] distincts = Arrays.stream(data.getLeaderboard()).distinct().filter(l -> l != 0).toArray();
					List<Long> leaderboard = Arrays.stream(data.getLeaderboard()).boxed().collect(Collectors.toList());
					if (distincts.length == 0) {
						spec.addField("Leaderboard", "No participants :/", false);
					} else if (distincts.length == 1) {
						spec.addField("Leaderboard", ":first_place: <@" + distincts[0] + "> - " + leaderboard.stream().mapToLong(l -> l).filter(l -> l != 0).count(), false);
					} else {
						ModeContext first = getMostCommon(leaderboard);
						ModeContext second = getMostCommon(leaderboard.stream().filter(l -> l != first.getMode()).collect(Collectors.toList()));
						String boardMessage = String.format(":first_place: <@%s> - %s\n" +
								":second_place: <@%s> - %s", first.getMode(), first.getCount(), second.getMode(), second.getCount());
						if(distincts.length >= 3) {
							ModeContext third = getMostCommon(leaderboard.stream().filter(l -> l != first.getMode()).filter(l -> l != second.getMode()).collect(Collectors.toList()));
							boardMessage += String.format("\n:third_place: <@%s> - %s", third.getMode(), third.getCount());
						}
						spec.addField("Leaderboard", boardMessage, false);
					}
				})).subscribe(mess -> {
					gsd.reset();
					gsd.setTourneyData(null);
				});
				return;
			}
			gsd.reset();
			data.setRound(round = data.getRound() + 1);
		}
		EyeEntry entry = EyeEntry.getRandom();
		channelMono.flatMap(channel -> channel.createMessage("Round #" + (data.getRound() + 1))).subscribe();
		channelMono.flatMap(channel -> channel.createEmbed(spec -> {
			App.createEyesEmbed(entry, spec);
		})).subscribe(message1 -> {
			synchronized (GuildSpecificData.LOCK) {
				gsd.setCurrent(entry);
				gsd.setMessageId(message1.getId());
			}
		});

		Schedulers.parallel().schedule(() -> {
			if (round >= data.getMaxRounds()) {
				return;
			}
			if (data.getLeaderboard()[round] == 0L) {
				Mono<Message> mess = channelMono.flatMap(channel -> channel.createMessage(spec -> spec.setContent("Nobody guessed in time...")));
				mess.subscribe(mess1 -> next(gsd, 0L, mess1.getChannel(), false));
			}
		}, 30, TimeUnit.SECONDS);
	}

	private static ModeContext getMostCommon(List<? extends Number> participants) {
		if(participants.stream().distinct().count() == 1) return new ModeContext(participants.get(0).longValue(), Collections.frequency(participants, participants.get(0)));

		List<Long> list = new ArrayList<>();
		list = participants.stream().map(Number::longValue).collect(Collectors.toList());

		long mode = 0;
		int modeCount = 0;
		HashMap<Long, Integer> hashMap = new HashMap<>();

		for(long i : list) {
			hashMap.put(i, hashMap.getOrDefault(i, 0) + 1);
		}

		for(Map.Entry<Long, Integer> entry : hashMap.entrySet()) {
			if(entry.getValue() > modeCount) {
				mode = entry.getKey();
				modeCount = entry.getValue();
			}
		}

		return new ModeContext(mode, modeCount);
	}
}