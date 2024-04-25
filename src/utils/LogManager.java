package utils;

import java.io.OutputStream;
import java.io.PrintStream;

public class LogManager {

    private static final PrintStream originalOut = System.out; // Save the original System.out
    private static final PrintStream dummyOut = new PrintStream(new OutputStream() {
        public void write(int b) {
            // Do nothing
        }
    });

    private static boolean isOutputVisible = true;

    public static void toggleOutput() {
        if (isOutputVisible) {
            System.setOut(dummyOut);
        } else {
            System.setOut(originalOut);
        }
        isOutputVisible = !isOutputVisible;
    }

    public static void logImportant(String message) {
        logImportant(message, true);
    }

    public static void logImportant(String message, boolean newLine) {
        if (!isOutputVisible) {
            toggleOutput();
            if (newLine) {
                System.out.println(message);
            } else {
                System.out.print(message);
            }
            toggleOutput();
        } else {
            if (newLine) {
                System.out.println(message);
            } else {
                System.out.print(message);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("This should be visible");

        toggleOutput();
        System.out.println("This should not be visible");

        toggleOutput();
        System.out.println("This should be visible again");
    }
}
