package ru.example.finance.console;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandParserTest {
    private final CommandParser parser = new CommandParser();

    @Test
    void parsesQuotedArgumentsAndEscapedQuotes() {
        List<String> result = parser.parse(
                "expense \"Коммунальные услуги\" 3000 \"Счёт \\\"дом\\\"\""
        );

        assertEquals(List.of("expense", "Коммунальные услуги", "3000", "Счёт \"дом\""), result);
    }

    @Test
    void rejectsUnclosedQuote() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("income \"Зарплата 1000"));
    }
}
