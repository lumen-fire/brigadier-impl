package me.lumen;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.function.BiFunction;

public class Main {
    private static boolean running = true;
    private static final CommandDispatcher<PrintStream> dispatcher = new CommandDispatcher<>();

    private static final LiteralArgumentBuilder<PrintStream> stopCommand = literal("stop")
            .executes(context -> {
                context.getSource().println("Stopping session...");
                running = false;
                return Command.SINGLE_SUCCESS;
            });

    private static final LiteralArgumentBuilder<PrintStream> operationCommand = literal("operation")
            .then(argument("a", DoubleArgumentType.doubleArg())
                    .then(operationNode('+', Double::sum))
                    .then(operationNode('-', (a, b) -> a - b))
                    .then(operationNode('*', (a, b) -> a * b))
                    .then(operationNode('/', (a, b) -> a / b))
                    .then(operationNode('^', Math::pow))
            );

    private static final LiteralArgumentBuilder<PrintStream> printCommand = literal("print")
            .then(argument("message", StringArgumentType.greedyString())
                    .executes(context -> {
                        String message = StringArgumentType.getString(context, "message");
                        context.getSource().println(message);
                        return Command.SINGLE_SUCCESS;
                    })
            );

    private static final LiteralArgumentBuilder<PrintStream> helpCommand = literal("help")
            .executes(context -> {
                Map<CommandNode<PrintStream>, String> usageMap = dispatcher.getSmartUsage(dispatcher.getRoot(), context.getSource());

                context.getSource().println("Commands:");
                for (String usage : usageMap.values()){
                    context.getSource().println(" - " + usage);
                }
                return usageMap.size();
            });

    private static final LiteralArgumentBuilder<PrintStream> resultCommand = literal("result")
            .then(argument("command", StringArgumentType.greedyString())
                    .executes(context -> {
                        String command = StringArgumentType.getString(context, "command");
                        int result = dispatcher.execute(command, context.getSource());
                        context.getSource().println("result of command '" + command + "': " + result);
                        return result;
                    })
            );

    private static boolean hiddenCommandEnabled = false;
    private static final LiteralArgumentBuilder<PrintStream> hiddenCommand = literal("secret-command")
            .requires(ignored -> hiddenCommandEnabled)
            .executes(context -> {
                context.getSource().println("Congratulations! You have used the secret command");
                return Command.SINGLE_SUCCESS;
            });

    private static final String password = "cat_lover_" + new Random().nextInt(10) + "01";
    private static final DynamicCommandExceptionType WRONG_PASSWORD = new DynamicCommandExceptionType(object ->
            new LiteralMessage(object + " is not the password!")
    );
    private static final LiteralArgumentBuilder<PrintStream> enableHiddenCommand = literal("enable-secret-command")
            .then(argument("password", StringArgumentType.string())
                    .then(argument("enabled", BoolArgumentType.bool())
                            .executes(context -> {
                                String attempt = StringArgumentType.getString(context, "password");
                                if (!attempt.equals(password)){
                                    throw WRONG_PASSWORD.create(attempt);
                                }
                                boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                hiddenCommandEnabled = enabled;
                                context.getSource().println("Set secret command enabled to " + enabled);
                                return Command.SINGLE_SUCCESS;
                            })
                    )
            );

    private static final LiteralArgumentBuilder<PrintStream> lengthCommand = literal("length")
            .then(argument("string", StringArgumentType.greedyString())
                    .executes(context -> {
                        String string = StringArgumentType.getString(context, "string");
                        int length = string.length();
                        context.getSource().println("Length of string '" + string + "': " + length + " chars");
                        return length;
                    })
            );


    private static final List<LiteralArgumentBuilder<PrintStream>> commands = List.of(stopCommand, operationCommand, printCommand, helpCommand, resultCommand, hiddenCommand, enableHiddenCommand, lengthCommand);

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        PrintStream stream = System.out;

        for (LiteralArgumentBuilder<PrintStream> command : commands){
            dispatcher.register(command);
        }

        stream.println("Use 'help' to display all commands");
        stream.println("Enter a command...");
        while (running){
            if (scanner.hasNext()) {
                String command = scanner.nextLine();
                try {
                    dispatcher.execute(command, stream);
                } catch (CommandSyntaxException e) {
                    stream.println(e.getMessage());
                }
            }
        }
    }

    private static LiteralArgumentBuilder<PrintStream> literal(String literal){
        return LiteralArgumentBuilder.literal(literal);
    }

    private static <T> RequiredArgumentBuilder<PrintStream, T> argument(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    private static LiteralArgumentBuilder<PrintStream> operationNode(char symbol, BiFunction<Double, Double, Double> operation){
        return literal(String.valueOf(symbol))
                .then(argument("b", DoubleArgumentType.doubleArg())
                        .executes(context -> {
                            double a = DoubleArgumentType.getDouble(context, "a");
                            double b = DoubleArgumentType.getDouble(context, "b");
                            double result = operation.apply(a, b);
                            context.getSource().println(result);
                            return (int) Math.round(result);
                        })
                );
    }
}