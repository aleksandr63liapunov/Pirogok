package com.example.english.service;

import com.example.english.config.BotConfig;
import com.example.english.model.Cocktails;
import com.example.english.model.Ingredients;
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
    private final CocktailsService cocktailsService;
    private final IngredientsService ingredientsService;

    // Хранение текущего языка пользователя
    private final Map<Long, String> userLanguageMap = new ConcurrentHashMap<>();

    public TelegrammBot(BotConfig botConfig, IngredientsService ingredientsService, TranslationService translationService, CocktailsService cocktailsService, UserRepository userRepository) {
        this.userRepository = userRepository;
        this.ingredientsService = ingredientsService;
        this.botConfig = botConfig;
        this.translationService = translationService;
        this.cocktailsService = cocktailsService;
        List<BotCommand> listOfCommand = new ArrayList<>();
        listOfCommand.add(new BotCommand("/start", "Welcome message"));
        listOfCommand.add(new BotCommand("/test", "Get a random word and translation"));
        listOfCommand.add(new BotCommand("/change_language", "Switch language"));
        listOfCommand.add(new BotCommand("/cocktails", "Get list of cocktails")); // Новая команда
        listOfCommand.add(new BotCommand("/ingredient", "Get ingredient"));

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
                case "⬅️ Назад" -> sendMessage(chatId, "Вы вернулись в главное меню. Используйте команды для взаимодействия.");
                case "/cocktails" -> handleCocktails(chatId); // Добавляем обработку новой команды
                case "Calculate Ingredients" -> requestPersonCount(chatId); // Новый метод для запроса количества людей
                default -> {
                    if (isNumeric(messageText)) { // Если пользователь ввел число
                        handleIngredientsCalculation(chatId, Integer.parseInt(messageText));
                    } else {
                        // Проверка на коктейли и перевод
                        boolean isCocktailFound = handleIngredientsIfCocktail(chatId, messageText);
                        if (!isCocktailFound) {
                            handleTranslation(chatId, messageText);
                        }
                    }
                }
            }
        }
    }

    private void requestPersonCount(long chatId) {
        sendMessage(chatId, "Введите количество человек для расчета ингредиентов:");
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void handleIngredientsCalculation(long chatId, int personCount) {
        try {
            // Используем первый коктейль как пример
            Cocktails cocktail = cocktailsService.getAll().get(0); // Замените на выбор коктейля пользователем
            Map<String, Double> ingredients = ingredientsService.getIngredientsForPerson(cocktail, personCount);

            StringBuilder response = new StringBuilder("Ингредиенты для ").append(personCount).append(" человек:\n");
            ingredients.forEach((ingredient, amount) ->
                    response.append("• ").append(ingredient).append(": ").append(amount).append(" л\n")
            );

            sendMessage(chatId, response.toString());
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка при расчете ингредиентов: " + e.getMessage());
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

    //    private void handleCocktails(long chatId) {
//        List<Cocktails> cocktails = cocktailsService.getAll();
//        if (cocktails.isEmpty()) {
//            sendMessage(chatId, "No cocktails found.");
//        } else {
//            StringBuilder response = new StringBuilder("🍹 Меню коктейлей :\n");
//            for (Cocktails cocktail : cocktails) {
//                response.append("• ").append(cocktail.getName()).append("\n");
//            }
//            response.append("\nВведите название коктейля, чтобы узнать его ингредиенты.");
//            sendMessage(chatId, response.toString());
//        }
//    }
    private void handleCocktails(long chatId) {
        List<Cocktails> cocktails = cocktailsService.getAll();
        if (cocktails.isEmpty()) {
            sendMessage(chatId, "No cocktails found.");
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🍹 Выберите коктейль:");

        // Создаем клавиатуру с коктейлями
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true); // Удобная клавиатура

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        for (Cocktails cocktail : cocktails) {
            KeyboardRow row = new KeyboardRow();
            row.add(cocktail.getName()); // Добавляем название коктейля в каждую строку
            keyboardRows.add(row);
        }
        KeyboardRow backRow = new KeyboardRow();
        backRow.add("⬅️ Назад");
        keyboardRows.add(backRow);

        // Устанавливаем клавиатуру
        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private boolean handleIngredientsIfCocktail(long chatId, String cocktailName) {
        // Найти коктейль по имени
        List<Cocktails> cocktails = cocktailsService.getAll();
        Cocktails selectedCocktail = cocktails.stream()
                .filter(c -> c.getName().equalsIgnoreCase(cocktailName))
                .findFirst()
                .orElse(null);

        if (selectedCocktail == null) {
            return false; // Коктейль не найден
        }

        // Найти ингредиенты для коктейля
        Ingredients ingredients = ingredientsService.findIngredientsByCocktails(selectedCocktail);
        if (ingredients == null) {
            sendMessage(chatId, "Для коктейля '" + cocktailName + "' ингредиенты не найдены.");
        } else {
            StringBuilder response = new StringBuilder("🍹 Ингредиенты для коктейля '")
                    .append(cocktailName)
                    .append("':\n");

            if (ingredients.getRome() > 0)
                response.append("• Ром: ").append(ingredients.getRome()).append("\n");
            if (ingredients.getAperol() > 0)
                response.append("• Апероль: ").append(ingredients.getAperol()).append("\n");
            if (ingredients.getSparkling_wine() > 0)
                response.append("• Игристое вино: ").append(ingredients.getSparkling_wine()).append("\n");
            if (ingredients.getJin() > 0)
                response.append("• Джин: ").append(ingredients.getJin()).append("\n");
            if (ingredients.getWiskey() > 0)
                response.append("• Виски: ").append(ingredients.getWiskey()).append("\n");
            if (ingredients.getTequila() > 0)
                response.append("• Текила: ").append(ingredients.getTequila()).append("\n");
            if (ingredients.getTreeple_sec() > 0)
                response.append("• Апельсин лик: ").append(ingredients.getTreeple_sec()).append("\n");
            if (ingredients.getSparkling_water() > 0)
                response.append("• Вода газ: ").append(ingredients.getSparkling_water()).append("\n");
            if (ingredients.getLimon() > 0)
                response.append("• Лимон: ").append(ingredients.getLimon()).append("\n");
            if (ingredients.getPineapple_Juice() > 0)
                response.append("• Анан сок: ").append(ingredients.getPineapple_Juice()).append("\n");
            if (ingredients.getCranberry_Juice() > 0)
                response.append("• Kлюкв сок: ").append(ingredients.getCranberry_Juice()).append("\n");
            if (ingredients.getOrange_Juice() > 0)
                response.append("• Апельсин сок: ").append(ingredients.getOrange_Juice()).append("\n");
            if (ingredients.getTonic() > 0)
                response.append("• Тоник: ").append(ingredients.getTonic()).append("\n");
            // Добавьте остальные ингредиенты по аналогии

            sendMessage(chatId, response.toString());
        }
        return true; // Коктейль найден и обработан
    }


    private void startCommandReceived(long chatId, String name) {
        String answer = "Здаров, " + name + "! Send '/test' to get a random word or '/change_language' to switch language.";
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
        row1.add("/cocktails"); // Добавляем кнопку Cocktails
        KeyboardRow row2 = new KeyboardRow();
        row2.add("Calculate Ingredients");

        keyboardRows.add(row1);
        keyboardRows.add(row2);

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
