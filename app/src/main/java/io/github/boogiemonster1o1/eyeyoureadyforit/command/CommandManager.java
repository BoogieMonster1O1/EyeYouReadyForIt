package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import org.reactivestreams.Publisher;
import org.reflections.Reflections;
import reactor.core.publisher.Mono;

@SuppressWarnings("NullableProblems")
public final class CommandManager {
	private static final Map<String, CommandHandler> COMMAND_MAP = new ConcurrentHashMap<>();
	private static final Reflections reflections = new Reflections("io.github.boogiemonster1o1.eyeyoureadyforit.command.commands");

	public static void init() {
		COMMAND_MAP.clear();
		for (Class<? extends CommandHandler> commandClass : reflections.getSubTypesOf(CommandHandler.class)) {
			try {
				CommandHandler handler = commandClass.getConstructor().newInstance();
				COMMAND_MAP.put(handler.getName(), handler);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		App.CLIENT.on(new ReactiveEventAdapter() {
			@Override
			public Publisher<?> onChatInputInteraction(ChatInputInteractionEvent event) {
				return accept(event);
			}
		}).subscribe();
	}

	public static Publisher<?> accept(ChatInputInteractionEvent event) {
		if (event.getInteraction().getGuildId().isEmpty()) {
			return event.reply("You can only run this commandHandler in a guild");
		}

		CommandHandler commandHandler = COMMAND_MAP.get(event.getCommandName());

		GuildSpecificData gsd = GuildSpecificData.get(event.getInteraction().getGuildId().orElseThrow());
		if (commandHandler == null) {
			return Mono.empty();
		}

		return commandHandler.handle(event);
	}

	public static Mono<?> registerSlashCommands() {
		App.LOGGER.info("REGISTERING COMMANDS YEE HAW");

		for (CommandHandler handler : COMMAND_MAP.values()) {
			switch (handler.getType()) {
				case GLOBAL_COMMAND:
					App.CLIENT.getRestClient()
							.getApplicationService()
							.createGlobalApplicationCommand(
									App.CLIENT.getRestClient().getApplicationId().block(),
									handler.asRequest()
							)
							.doOnError(Throwable::printStackTrace)
							.onErrorResume(e -> Mono.empty())
							.block();
					break;

				case ADMIN_COMMAND:
					App.CLIENT.getRestClient()
							.getApplicationService()
							.createGuildApplicationCommand(
									App.CLIENT.getRestClient().getApplicationId().block(),
									859274373084479508L,
									handler.asRequest()
							)
							.doOnError(Throwable::printStackTrace)
							.onErrorResume(e -> Mono.empty())
							.block();
					break;
			}
		}

		return Mono.empty();
	}
}
