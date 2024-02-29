package restaurant;

import java.util.*;
import java.util.function.Consumer;

public class CommandHandler {
    private final Map<String, Command> commands = new HashMap<>();
    private final List<Command> orderedCommands = new ArrayList<>();

    public CommandResponse handleMessage(String message) {
        var commandString = message.contains(" ") ? message.substring(0, message.indexOf(" ")) : message;
        var argumentString = message.contains(" ") ? message.substring(commandString.length() + 1) : "";

        var result = new ArrayList<String>();
        var command = commands.get(commandString);

        if (command != null) {
            int index = 0;
            boolean satisfied = false;

            while (true) {
                if (index >= command.params.length && !argumentString.isEmpty()) {
                    return new CommandResponse(ResponseType.manyArguments, command, commandString);
                } else if (argumentString.isEmpty()) break;

                if (command.params[index].optional || index >= command.params.length - 1 || command.params[index + 1].optional) {
                    satisfied = true;
                }

                if (command.params[index].variadic) {
                    result.add(argumentString);
                    break;
                }

                int next = argumentString.indexOf(" ");
                if (next == -1) {
                    if (!satisfied) {
                        return new CommandResponse(ResponseType.fewArguments, command, commandString);
                    }
                    result.add(argumentString);
                    break;
                } else {
                    var arg = argumentString.substring(0, next);
                    argumentString = argumentString.substring(arg.length() + 1);
                    result.add(arg);
                }

                index++;
            }

            if (!satisfied && command.params.length > 0 && !command.params[0].optional) {
                return new CommandResponse(ResponseType.fewArguments, command, commandString);
            }

            command.runner.accept(result.toArray(String[]::new));

            return new CommandResponse(ResponseType.valid, command, commandString);
        } else {
            return new CommandResponse(ResponseType.unknownCommand, null, commandString);
        }
    }

    public void removeCommand(String text) {
        var command = commands.get(text);
        if (command == null) return;

        commands.remove(text);
        orderedCommands.remove(command);
    }

    public void register(String text, String description, Consumer<String[]> runner) {
        register(text, "", description, runner);
    }

    public void register(String text, String params, String description, Consumer<String[]> runner) {
        orderedCommands.removeIf(command -> command.text.equals(text));

        var command = new Command(text, params, description, runner);
        commands.put(text, command);
        orderedCommands.add(command);
    }


    public List<Command> getCommandList() {
        return orderedCommands;
    }

    public enum ResponseType {
        unknownCommand, fewArguments, manyArguments, valid
    }

    public static class Command {
        public final String text;
        public final String paramText;
        public final String description;
        public final CommandParam[] params;
        public final Consumer<String[]> runner;

        public Command(String text, String paramText, String description, Consumer<String[]> runner) {
            this.text = text;
            this.paramText = paramText;
            this.runner = runner;
            this.description = description;

            if (paramText.isEmpty()) {
                params = new CommandParam[0];
                return;
            }

            var split = paramText.split(" ");
            params = new CommandParam[split.length];

            boolean hadOptional = false;

            for (int i = 0; i < params.length; i++) {
                var param = split[i];
                if (param.length() <= 2) throw new IllegalArgumentException();

                char left = param.charAt(0), right = param.charAt(param.length() - 1);
                boolean optional, variadic = false;

                if (left == '<' && right == '>') {
                    if (hadOptional)
                        throw new IllegalArgumentException();
                    optional = false;
                } else if (left == '[' && right == ']') {
                    optional = true;
                } else {
                    throw new IllegalArgumentException();
                }

                if (optional)
                    hadOptional = true;

                var name = param.substring(1, param.length() - 1);
                if (name.endsWith("...")) {
                    if (i != params.length - 1)
                        throw new IllegalArgumentException();

                    name = name.substring(0, name.length() - 3);
                    variadic = true;
                }

                params[i] = new CommandParam(name, optional, variadic);
            }
        }
    }

    public record CommandParam(String name, boolean optional, boolean variadic) {
    }

    public record CommandResponse(ResponseType type, Command command, String runCommand) {
    }
}