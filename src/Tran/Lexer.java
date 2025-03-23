package Tran;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;


public class Lexer {
    private final TextManager textManager;
    private final Map<String, Token.TokenTypes> keywords;
    private final Stack<Integer> indentationLevels;
    private int currentIndentation;


    public Lexer(String input) {
        this.textManager = new TextManager(input);
        this.keywords = new HashMap<>();
        this.indentationLevels = new Stack<>();
        this.currentIndentation = 0;


        // initialize keywords
        keywords.put("implements", Token.TokenTypes.IMPLEMENTS);
        keywords.put("class", Token.TokenTypes.CLASS);
        keywords.put("interface", Token.TokenTypes.INTERFACE);
        keywords.put("loop", Token.TokenTypes.LOOP);
        keywords.put("if", Token.TokenTypes.IF);
        keywords.put("else", Token.TokenTypes.ELSE);
        keywords.put("new", Token.TokenTypes.NEW);
        keywords.put("private", Token.TokenTypes.PRIVATE);
        keywords.put("shared", Token.TokenTypes.SHARED);
        keywords.put("construct", Token.TokenTypes.CONSTRUCT);
    }

    public List<Token> Lex() throws SyntaxErrorException {
        List<Token> tokens = new LinkedList<>();
        indentationLevels.push(0);

        while (!textManager.isAtEnd()) {
            char currentChar = textManager.peekCharacter();

            if (Character.isLetter(currentChar)) {
                Token wordToken = readWord();
                System.out.println("Word Token: " + wordToken);
                tokens.add(wordToken);
            } else if (Character.isDigit(currentChar) || (currentChar == '.' && Character.isDigit(textManager.peekCharacter(1)))) {
                Token numberToken = readNumber();
                System.out.println("Number Token: " + numberToken);
                tokens.add(numberToken);
            } else if (currentChar == '\n') {
                textManager.getCharacter(); // consume newline character first
                Token newlineToken = new Token(Token.TokenTypes.NEWLINE, textManager.getLineNumber(), textManager.getColumnNumber());
                System.out.println("Newline Token: " + newlineToken);
                tokens.add(newlineToken);
                handleIndentation(tokens);
            } else if (Character.isWhitespace(currentChar)) {
                textManager.getCharacter();
            } else if (currentChar == '{') {
                readComment();
            } else if (currentChar == '"') {
                Token stringToken = readQuotedString();
                System.out.println("String Token: " + stringToken);
                tokens.add(stringToken);
            } else if (currentChar == '\'') {
                Token charToken = readQuotedCharacter();
                System.out.println("Character Token: " + charToken);
                tokens.add(charToken);
            } else {
                Token punctuationToken = readPunctuation();
                System.out.println("Punctuation Token: " + punctuationToken);
                tokens.add(punctuationToken);
            }
        }

        // DEDENT
        while (currentIndentation > 0) {
            Token dedentToken = new Token(Token.TokenTypes.DEDENT, textManager.getLineNumber(), textManager.getColumnNumber());
            System.out.println("DEDENT Token: " + dedentToken);
            tokens.add(dedentToken);
            currentIndentation--;
        }

        return tokens;
    }

    private Token readWord() {
        StringBuilder buffer = new StringBuilder();
        int line = textManager.getLineNumber();
        int column = textManager.getColumnNumber();

        // read first char (must be a letter)
        if (Character.isLetter(textManager.peekCharacter())) {
            buffer.append(textManager.getCharacter());
        }

        // read subsequent chars (letters or digits)
        while (!textManager.isAtEnd() && Character.isLetterOrDigit(textManager.peekCharacter())) {
            buffer.append(textManager.getCharacter());
        }

        String word = buffer.toString();

        // check if word is keyword
        if (keywords.containsKey(word)) {
            return new Token(keywords.get(word), line, column);
        } else {
            return new Token(Token.TokenTypes.WORD, line, column, word);
        }
    }

    private Token readNumber() {
        StringBuilder buffer = new StringBuilder();
        int line = textManager.getLineNumber();
        int column = textManager.getColumnNumber();


        // read digits before decimal
        while (!textManager.isAtEnd() && (Character.isDigit(textManager.peekCharacter()) || textManager.peekCharacter() == '.')) {
            buffer.append(textManager.getCharacter());
        }

        // check for negative sign after first
        if (buffer.length() == 1 && buffer.charAt(0) == '-') {
            // read digits after negative
            while (!textManager.isAtEnd() && Character.isDigit(textManager.peekCharacter())) {
                buffer.append(textManager.getCharacter());
            }
        }
        return new Token(Token.TokenTypes.NUMBER, line, column, buffer.toString());
    }


    private void handleIndentation(List<Token> tokens) throws SyntaxErrorException {
        int spaces = 0;

        // Count spaces and tabs at the beginning of the line
        while (!textManager.isAtEnd() && (textManager.peekCharacter() == ' ' || textManager.peekCharacter() == '\t')) {
            char currentChar = textManager.getCharacter();
            if (currentChar == ' ') {
                spaces++;
            } else if (currentChar == '\t') {
                spaces += 4; // Treat a tab as 4 spaces
            }
        }

        // Skip indentation if the line is empty (e.g., just a newline)
        if (textManager.isAtEnd() || textManager.peekCharacter() == '\n') {
            return;
        }

        // Check if the indentation is valid (must be a multiple of 4)
        if (spaces % 4 != 0) {
            throw new SyntaxErrorException("Indentation must be a multiple of 4", textManager.getLineNumber(), textManager.getColumnNumber());
        }

        int indentLevel = spaces / 4;

        // Generate INDENT tokens if the indentation level increases
        if (indentLevel > currentIndentation) {
            for (int i = currentIndentation; i < indentLevel; i++) {
                tokens.add(new Token(Token.TokenTypes.INDENT, textManager.getLineNumber(), textManager.getColumnNumber()));
            }
            indentationLevels.push(currentIndentation);
            currentIndentation = indentLevel;
        }
        // Generate DEDENT tokens if the indentation level decreases
        else if (indentLevel < currentIndentation) {
            while (currentIndentation > indentLevel) {
                tokens.add(new Token(Token.TokenTypes.DEDENT, textManager.getLineNumber(), textManager.getColumnNumber()));
                currentIndentation = indentationLevels.pop();
            }
        }
    }


    private void readComment() throws SyntaxErrorException {
        int line = textManager.getLineNumber();
        int column = textManager.getColumnNumber();
        textManager.getCharacter(); // consume '{'

        while (!textManager.isAtEnd() && textManager.peekCharacter() != '}') {
            textManager.getCharacter();
        }

        if (textManager.isAtEnd()) {
            throw new SyntaxErrorException("Unterminated comment", line, column);
        }

        textManager.getCharacter(); // consume '}'
    }

    private Token readQuotedString() throws SyntaxErrorException {
        StringBuilder buffer = new StringBuilder();
        int line = textManager.getLineNumber();
        int column = textManager.getColumnNumber();
        textManager.getCharacter(); // consume '"'

        while (!textManager.isAtEnd() && textManager.peekCharacter() != '"') {
            buffer.append(textManager.getCharacter());
        }

        if (textManager.isAtEnd()) {
            throw new SyntaxErrorException("Unterminated string", line, column);
        }

        textManager.getCharacter();
        return new Token(Token.TokenTypes.QUOTEDSTRING, line, column, buffer.toString());
    }

    private Token readQuotedCharacter() throws SyntaxErrorException {
        StringBuilder buffer = new StringBuilder();
        int line = textManager.getLineNumber();
        int column = textManager.getColumnNumber();
        textManager.getCharacter();

        if (!textManager.isAtEnd() && textManager.peekCharacter() != '\'') {
            buffer.append(textManager.getCharacter());
        } else {
            throw new SyntaxErrorException("Empty character literal", line, column);
        }

        if (textManager.isAtEnd() || textManager.peekCharacter() != '\'') {
            throw new SyntaxErrorException("Unterminated character literal", line, column);
        }

        textManager.getCharacter();

        if (buffer.length() != 1) {
            throw new SyntaxErrorException("Character literal must contain exactly one character", line, column);
        }

        return new Token(Token.TokenTypes.QUOTEDCHARACTER, line, column, buffer.toString());
    }

    private Token readPunctuation() throws SyntaxErrorException {
        int line = textManager.getLineNumber();
        int column = textManager.getColumnNumber();
        char currentChar = textManager.getCharacter();

        switch (currentChar) {
            case '=':
                if (textManager.peekCharacter() == '=') {
                    textManager.getCharacter();
                    return new Token(Token.TokenTypes.EQUAL, line, column);
                } else {
                    return new Token(Token.TokenTypes.ASSIGN, line, column);
                }
            case '>':
                if (textManager.peekCharacter() == '=') {
                    textManager.getCharacter();
                    return new Token(Token.TokenTypes.GREATERTHANEQUAL, line, column);
                } else {
                    return new Token(Token.TokenTypes.GREATERTHAN, line, column);
                }
            case '<':
                if (textManager.peekCharacter() == '=') {
                    textManager.getCharacter();
                    return new Token(Token.TokenTypes.LESSTHANEQUAL, line, column);
                } else {
                    return new Token(Token.TokenTypes.LESSTHAN, line, column);
                }
            case '!':
                if (textManager.peekCharacter() == '=') {
                    textManager.getCharacter();
                    return new Token(Token.TokenTypes.NOTEQUAL, line, column);
                } else {
                    throw new SyntaxErrorException("Unexpected character: " + currentChar, line, column);
                }
            case '(':
                return new Token(Token.TokenTypes.LPAREN, line, column);
            case ')':
                return new Token(Token.TokenTypes.RPAREN, line, column);
            case ':':
                return new Token(Token.TokenTypes.COLON, line, column);
            case '.':
                return new Token(Token.TokenTypes.DOT, line, column);
            case '+':
                return new Token(Token.TokenTypes.PLUS, line, column);
            case '-':
                return new Token(Token.TokenTypes.MINUS, line, column);
            case '*':
                return new Token(Token.TokenTypes.TIMES, line, column);
            case '/':
                return new Token(Token.TokenTypes.DIVIDE, line, column);
            case '%':
                return new Token(Token.TokenTypes.MODULO, line, column);
            case ',':
                return new Token(Token.TokenTypes.COMMA, line, column);
            default:
                throw new SyntaxErrorException("Unexpected character: " + currentChar, line, column);
        }
    }
}