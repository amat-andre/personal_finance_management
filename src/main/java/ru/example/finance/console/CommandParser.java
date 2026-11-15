package ru.example.finance.console;

import java.util.ArrayList;
import java.util.List;

/** Разбор команды с поддержкой аргументов в двойных кавычках. */
public final class CommandParser {
    public List<String> parse(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;
        boolean tokenStarted = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                insideQuotes = !insideQuotes;
                tokenStarted = true;
            } else if (character == '\\' && insideQuotes && index + 1 < line.length()
                    && (line.charAt(index + 1) == '"' || line.charAt(index + 1) == '\\')) {
                current.append(line.charAt(++index));
                tokenStarted = true;
            } else if (Character.isWhitespace(character) && !insideQuotes) {
                if (tokenStarted) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    tokenStarted = false;
                }
            } else {
                current.append(character);
                tokenStarted = true;
            }
        }
        if (insideQuotes) {
            throw new IllegalArgumentException("Не закрыта двойная кавычка");
        }
        if (tokenStarted) {
            tokens.add(current.toString());
        }
        return tokens;
    }
}
