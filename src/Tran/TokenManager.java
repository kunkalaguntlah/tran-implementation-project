package Tran;
import java.util.List;
import java.util.Optional;

public class TokenManager {
    private final List<Token> tokens;
    private int currentIndex;

    public TokenManager(List<Token> tokens) {
        this.tokens = tokens;
        this.currentIndex = 0;
    }

    public boolean done() {
        return currentIndex >= tokens.size();
    }

    public Optional<Token> matchAndRemove(Token.TokenTypes t) {
        if (!done() && tokens.get(currentIndex).getType() == t) {
            return Optional.of(tokens.get(currentIndex++));
        }
        return Optional.empty();
    }

    public Optional<Token> peek(int i) {
        if (currentIndex + i < tokens.size()) {
            return Optional.of(tokens.get(currentIndex + i));
        }
        return Optional.empty();
    }

    public Optional<Token> peekCurrent() {
        return peek(0);
    }

    public void consume() {
        if (!done()) {
            currentIndex++;
        }
    }

    public void require(Token.TokenTypes t) throws SyntaxErrorException {
        if (done() || tokens.get(currentIndex).getType() != t) {
            throw new SyntaxErrorException(
                    "Expected token: " + t + ", but found: " + (done() ? "EOF" : tokens.get(currentIndex).getType()),
                    getCurrentLine(),
                    getCurrentColumnNumber()
            );
        }
        consume();
    }

    public void skipNewLines() {
        while (!done() && tokens.get(currentIndex).getType() == Token.TokenTypes.NEWLINE) {
            currentIndex++;
        }
    }

    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second) {
        Optional<Token> firstToken = peek(0);
        Optional<Token> secondToken = peek(1);
        return firstToken.isPresent() && firstToken.get().getType() == first &&
                secondToken.isPresent() && secondToken.get().getType() == second;
    }

    public boolean nextIsEither(Token.TokenTypes... types) {
        Optional<Token> nextToken = peek(0);
        if (nextToken.isPresent()) {
            for (Token.TokenTypes type : types) {
                if (nextToken.get().getType() == type) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getCurrentLine() {
        if (!done()) {
            return tokens.get(currentIndex).getLineNumber();
        }
        return -1;
    }

    public int getCurrentColumnNumber() {
        if (!done()) {
            return tokens.get(currentIndex).getColumnNumber();
        }
        return -1;
    }
    public void skipIndent() {
        while (!done() && tokens.get(currentIndex).getType() == Token.TokenTypes.INDENT) {
            currentIndex++;
        }
    }

    public void skipDedent() {
        while (!done() && tokens.get(currentIndex).getType() == Token.TokenTypes.DEDENT) {
            currentIndex++;
        }
    }
}