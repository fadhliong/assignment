package org.awesomegic.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class InputValidator {
    public static LocalDate parseAndValidateDate(String dateString) {
        try {
            if (!dateString.matches("\\d{8}")) {
                throw new IllegalArgumentException("Date must be in yyyyMMdd format");
            }

            DateTimeFormatter formatter = DateTimeFormatter.BASIC_ISO_DATE;
            LocalDate date = LocalDate.parse(dateString, formatter);

            return date;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format", e);
        }
    }
}
