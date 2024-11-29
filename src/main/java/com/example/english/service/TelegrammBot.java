package com.example.english.service;

import com.example.english.config.BotConfig;
import com.example.english.model.UserBotPiro;
import com.example.english.model.WordTranslation;
import com.example.english.repo.UserRepository;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CurrentTimestamp;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component

public class TelegrammBot extends TelegramLongPollingBot {
    private final BotConfig botConfig;
    private final TranslationService translationService;
    private final UserRepository userRepository;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<Long, Boolean> userTestStatusMap = new ConcurrentHashMap<>();

    // Хранение текущего языка пользователя
    private final Map<Long, String> userLanguageMap = new ConcurrentHashMap<>();

    public TelegrammBot(BotConfig botConfig, TranslationService translationService, UserRepository userRepository) {
        this.userRepository = userRepository;
        this.botConfig = botConfig;
        this.translationService = translationService;

        List<BotCommand> listOfCommand = new ArrayList<>();
        listOfCommand.add(new BotCommand("/start", "Welcome message"));
        listOfCommand.add(new BotCommand("/test", "Get a random word and translation"));
        listOfCommand.add(new BotCommand("/change_language", "Switch language"));
        try {
            execute(new SetMyCommands(listOfCommand, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start" -> startCommandReceived(chatId, update.getMessage().getChat().getUserName());
                case "/test" -> handleTest(chatId);
                case "/change_language" -> showLanguageOptions(chatId);
                case "/pause_resume" -> toggleTestStatus(chatId);
                case "English" -> setLanguage(chatId, "en");
                case "Russian" -> setLanguage(chatId, "ru");
                default -> handleTranslation(chatId, messageText);
            }
        }
    }
    private void toggleTestStatus(long chatId) {
        boolean currentStatus = userTestStatusMap.getOrDefault(chatId, false); // Получаем текущее состояние
        userTestStatusMap.put(chatId, !currentStatus); // Меняем его

        String statusMessage = currentStatus
                ? "Test paused. Press /pause_resume to continue."
                : "Test resumed.";
        sendMessage(chatId, statusMessage);
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Hi, " + name + "! Send '/test' to get a random word or '/change_language' to switch language.";
        sendMessage(chatId, answer);
        userLanguageMap.put(chatId, "en"); // Устанавливаем язык по умолчанию
    }

    private void handleTest(long chatId) {
        List<WordTranslation> words = new ArrayList<>(translationService.getAllWordsRandom());
        if (words.isEmpty()) {
            sendMessage(chatId, "No words found in the database.");
            return;
        }

        userTestStatusMap.put(chatId, true); // Устанавливаем начальное состояние — активен

        scheduler.scheduleAtFixedRate(new Runnable() {
            private int index = 0;

            @Override
            public void run() {
                // Проверяем, активен ли тест для пользователя
                if (!userTestStatusMap.getOrDefault(chatId, false)) {
                    return; // Если тест приостановлен, ничего не делаем
                }

                if (index < words.size()) {
                    WordTranslation wordTranslation = words.get(index);
                    String word = wordTranslation.getWord();
                    String translation = wordTranslation.getTranslation();

                    sendMessage(chatId, "Word: " + word);
                    scheduler.schedule(() -> sendMessage(chatId, "Translation: " + translation), 3, TimeUnit.SECONDS);

                    index++;
                } else {
                    sendMessage(chatId, "End of words! You've gone through all the words.");
                    scheduler.shutdown(); // Завершаем выполнение
                }
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    private void handleTranslation(long chatId, String word) {
        String userLanguage = userLanguageMap.getOrDefault(chatId, "en"); // Язык пользователя
        String fromLang = userLanguage; // Исходный язык
        String toLang = userLanguage.equals("en") ? "ru" : "en"; // Язык перевода

        // Выполняем перевод
        String translationWithExamples = translationService.translateWord(word, fromLang, toLang);
        sendMessage(chatId, "Translation:\n" + translationWithExamples);
    }

    private void showLanguageOptions(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Choose your language:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("English");
        row.add("Russian");
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void setLanguage(long chatId, String language) {
        userLanguageMap.put(chatId, language);
        sendMessage(chatId, "Language switched to " + (language.equals("en") ? "English" : "Russian") + ".");
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("/start");
        row1.add("/test");
        row1.add("/pause_resume");
        row1.add("/change_language");

//        KeyboardRow row2 = new KeyboardRow();
//        row2.add("Get Example Sentence");

        keyboardRows.add(row1);
//        keyboardRows.add(row2);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getBotToken();
    }
}
