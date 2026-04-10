package org.aiagents.chatmemory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;

public class ServiceWithPersistentMemoryExample {

    interface ChatBot {
        String chat(@MemoryId Integer memoryId, @UserMessage String question);
    }
    public static void main(String[] args) {
        ChatModel model = Models.OLLAMA_BASE_MODEL;

        ChatMemoryStore store = new FileBasedChatMemoryStore("chat-memory-storage");

        ChatBot chatBot = AiServices.builder(ChatBot.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(10)
                        .chatMemoryStore(store)
                        .build())
                .build();

//        String answer = chatBot.chat(1, "Hello! My name is Mario.");
//        System.out.println(answer);

        // Now, comment out the two lines above, uncomment the two lines below, and run again.

         String answerWithName = chatBot.chat(1, "What is my name?");
         System.out.println(answerWithName);
    }

    static class FileBasedChatMemoryStore implements ChatMemoryStore {

        private final Path storagePath;

        public FileBasedChatMemoryStore(String storageDirectory) {
            try {
                storagePath = Paths.get(storageDirectory);
                Files.createDirectories(storagePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create storage directory", e);
            }
        }

        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            Path filePath = storagePath.resolve(memoryId + ".json");
            try {
                if (Files.exists(filePath)) {
                    String json = Files.readString(filePath);
                    return messagesFromJson(json);
                }
                return List.of();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read messages", e);
            }
        }

        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            Path filePath = storagePath.resolve(memoryId + ".json");
            try {
                String json = messagesToJson(messages);
                Files.writeString(filePath, json);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write messages", e);
            }
        }

        @Override
        public void deleteMessages(Object memoryId) {
            Path filePath = storagePath.resolve(memoryId + ".json");
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete messages", e);
            }
        }
    }
}
