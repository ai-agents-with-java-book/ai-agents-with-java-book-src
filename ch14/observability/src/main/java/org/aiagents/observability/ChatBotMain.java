package org.aiagents.observability;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

@QuarkusMain
public class ChatBotMain implements QuarkusApplication {

    private static InputStream originalStdin;

    @Inject
    Agents.ExpertsChatbot chatbot;

    public static void main(String... args) {
        originalStdin = System.in;
        Quarkus.run(ChatBotMain.class, args);
    }

    @Override
    public int run(String... args) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(originalStdin));
        System.out.println("Expert ChatBot ready. Type your question (or 'exit' to quit):");
        System.out.print("\n> ");
        String input = readInput(reader);
        String response = chatbot.chat("main", input);
        System.out.println("\n" + response);
        return 0;
    }

    private static String readInput(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            String input = reader.readLine();
            sb.append(input);
            if (input.isEmpty() || input.contains("\n")) {
                break;
            }
        }
        return sb.toString().trim();
    }
}
