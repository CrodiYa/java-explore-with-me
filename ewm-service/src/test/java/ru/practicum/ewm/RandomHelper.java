package ru.practicum.ewm;

public class RandomHelper {
    private static final String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final java.util.Random random = new java.util.Random();

    public static String getRandomString() {
        int randomLength = random.nextInt(10) + 2;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < randomLength; i++) {
            int index = random.nextInt(letters.length());
            sb.append(letters.charAt(index));
        }
        return sb.toString();
    }

    public static String getRandomEmail() {
        return getRandomString() + "@" + getRandomString() + ".com";
    }
}
