package com.martist.vitamove.core.domain.utils;

import java.security.SecureRandom;

public class PasswordGenerator {

    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
    private static final String NUMBER = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()_+-=[]{}|;:,.<>?";


    private static final String ALL_CHARS = CHAR_LOWER + CHAR_UPPER + NUMBER + SPECIAL;

    private static final SecureRandom random = new SecureRandom();


    public static String generateStrongPassword() {
        return generatePassword(12);
    }


    public static String generatePassword(int length) {
        if (length < 8) {
            length = 8;
        }

        StringBuilder password = new StringBuilder(length);


        password.append(getRandomChar(CHAR_LOWER));
        password.append(getRandomChar(CHAR_UPPER));
        password.append(getRandomChar(NUMBER));
        password.append(getRandomChar(SPECIAL));


        for (int i = 4; i < length; i++) {
            password.append(getRandomChar(ALL_CHARS));
        }


        return shuffleString(password.toString());
    }

    private static char getRandomChar(String charPool) {
        return charPool.charAt(random.nextInt(charPool.length()));
    }

    private static String shuffleString(String input) {
        char[] characters = input.toCharArray();


        for (int i = characters.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = characters[i];
            characters[i] = characters[j];
            characters[j] = temp;
        }

        return new String(characters);
    }
}