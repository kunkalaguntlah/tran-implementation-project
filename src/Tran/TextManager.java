package Tran;


public class TextManager {
    private final String input;
    private int position;
    private int lineNumber;
    private int columnNumber;


    public TextManager(String input) {
        this.input = input;
        this.position = 0;
        this.lineNumber = 1;
        this.columnNumber = 1;
    }


    public boolean isAtEnd() {
        return position >= input.length();
    }


    public char peekCharacter() {
        if (isAtEnd()) {
            return '\0';
        }
        return input.charAt(position);
    }


    public char peekCharacter(int dist) {
        if (position + dist >= input.length()) {
            return '\0';
        }
        return input.charAt(position + dist);
    }


    public char getCharacter() {
        if (isAtEnd()) {
            return '\0';
        }
        char currentChar = input.charAt(position++);
        if (currentChar == '\n') {
            lineNumber++;
            columnNumber = 1;
        } else {
            columnNumber++;
        }
        return currentChar;
    }


    public int getLineNumber() {
        return lineNumber;
    }


    public int getColumnNumber() {
        return columnNumber;
    }
}
