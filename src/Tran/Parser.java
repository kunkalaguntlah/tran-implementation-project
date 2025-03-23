package Tran;

import AST.*;
import java.util.*;

public class Parser {
    private final TokenManager tokenManager;
    private final TranNode tranNode;

    public Parser(TranNode top, List<Token> tokens) {
        this.tranNode = top;
        this.tokenManager = new TokenManager(tokens);
    }

    public void Tran() throws SyntaxErrorException {
        tokenManager.skipIndent();
        tokenManager.skipNewLines();

        while (!tokenManager.done()) {
            Optional<InterfaceNode> interfaceNode = Interface();
            if (interfaceNode.isPresent()) {
                tranNode.Interfaces.add(interfaceNode.get());
            } else {
                Optional<ClassNode> classNode = Class();
                if (classNode.isPresent()) {
                    tranNode.Classes.add(classNode.get());
                } else if (!tokenManager.done()) {
                    throw syntaxError("Expected interface or class declaration");
                }
            }
            tokenManager.skipIndent();
            tokenManager.skipNewLines();
        }
    }

    private Optional<ClassNode> Class() throws SyntaxErrorException {
        if (tokenManager.matchAndRemove(Token.TokenTypes.CLASS).isEmpty()) return Optional.empty();

        Optional<Token> classNameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (classNameToken.isEmpty()) throw syntaxError("Expected class name");

        ClassNode classNode = new ClassNode();
        classNode.name = classNameToken.get().getValue();

        if (tokenManager.matchAndRemove(Token.TokenTypes.IMPLEMENTS).isPresent()) {
            while (true) {
                Optional<Token> interfaceToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
                if (interfaceToken.isEmpty()) throw syntaxError("Expected interface name after 'implements'");
                classNode.interfaces.add(interfaceToken.get().getValue());
                if (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isEmpty()) break;
            }
        }
        tokenManager.skipNewLines();
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) throw syntaxError("Expected indentation after class declaration");

        while (!tokenManager.done() && tokenManager.peekCurrent().isPresent() && tokenManager.peekCurrent().get().getType() != Token.TokenTypes.DEDENT) {
            Optional<MemberNode> member = Member();
            if (member.isPresent()) {
                classNode.members.add(member.get());
                tokenManager.skipNewLines();
            } else {
                Optional<ConstructorNode> constructor = Constructor();
                if (constructor.isPresent()) {
                    classNode.constructors.add(constructor.get());
                    tokenManager.skipNewLines();
                } else {
                    Optional<MethodDeclarationNode> method = MethodDeclaration();
                    if (method.isPresent()) {
                        classNode.methods.add(method.get());
                        tokenManager.skipNewLines();
                    } else {
                        throw syntaxError("Expected member, constructor, or method declaration");
                    }
                }
            }
        }
        tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);
        return Optional.of(classNode);
    }

    private Optional<ConstructorNode> Constructor() throws SyntaxErrorException {
        if (tokenManager.matchAndRemove(Token.TokenTypes.CONSTRUCT).isEmpty()) return Optional.empty();

        ConstructorNode constructorNode = new ConstructorNode();

        if (tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()) {
            constructorNode.parameters = ParameterVariableDeclarations().orElse(new ArrayList<>());
            if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) throw syntaxError("Expected ')' after constructor parameters");
        }

        tokenManager.skipNewLines();
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent()) {
            while (!tokenManager.done() && tokenManager.peekCurrent().isPresent() && tokenManager.peekCurrent().get().getType() != Token.TokenTypes.DEDENT) {
                Optional<StatementNode> statement = Statement();
                if (statement.isPresent()) {
                    constructorNode.statements.add(statement.get());
                } else {
                    throw syntaxError("Expected statement in constructor body");
                }
                tokenManager.skipNewLines();
            }
            tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);
        }
        return Optional.of(constructorNode);
    }

    private Optional<MemberNode> Member() throws SyntaxErrorException {
        if (tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)) {
            return Optional.empty(); // It's a method, not a member
        }

        Optional<Token> typeToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (typeToken.isEmpty()) return Optional.empty();

        Optional<Token> nameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (nameToken.isEmpty()) throw syntaxError("Expected member name");

        MemberNode memberNode = new MemberNode();
        memberNode.declaration = new VariableDeclarationNode();
        memberNode.declaration.type = typeToken.get().getValue();
        memberNode.declaration.name = nameToken.get().getValue();
        return Optional.of(memberNode);
    }

    private Optional<MethodDeclarationNode> MethodDeclaration() throws SyntaxErrorException {
        boolean isShared = tokenManager.matchAndRemove(Token.TokenTypes.SHARED).isPresent();
        boolean isPrivate = tokenManager.matchAndRemove(Token.TokenTypes.PRIVATE).isPresent();

        Optional<Token> methodNameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (methodNameToken.isEmpty()) return Optional.empty();

        MethodDeclarationNode methodNode = new MethodDeclarationNode();
        methodNode.isShared = isShared;
        methodNode.isPrivate = isPrivate;
        methodNode.name = methodNameToken.get().getValue();

        if (tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()) {
            methodNode.parameters = ParameterVariableDeclarations().orElse(new ArrayList<>());
            if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) throw syntaxError("Expected ')' after method parameters");
        }

        if (tokenManager.matchAndRemove(Token.TokenTypes.COLON).isPresent()) {
            methodNode.returns = ReturnVariableDeclarations().orElse(new ArrayList<>());
        }

        tokenManager.skipNewLines();
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent()) {
            while (!tokenManager.done() && tokenManager.peekCurrent().isPresent() && tokenManager.peekCurrent().get().getType() != Token.TokenTypes.DEDENT) {
                Optional<StatementNode> statement = Statement();
                if (statement.isPresent()) {
                    methodNode.statements.add(statement.get());
                } else {
                    Optional<VariableDeclarationNode> localVar = VariableDeclaration();
                    if (localVar.isPresent()) {
                        methodNode.locals.add(localVar.get());
                    } else {
                        throw syntaxError("Expected statement or local variable declaration in method body");
                    }
                }
                tokenManager.skipNewLines();
            }
            tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);
        }
        return Optional.of(methodNode);
    }

    private Optional<VariableDeclarationNode> VariableDeclaration() throws SyntaxErrorException {
        Optional<Token> typeToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (typeToken.isEmpty()) return Optional.empty();

        Optional<Token> nameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (nameToken.isEmpty()) throw syntaxError("Expected variable name");

        VariableDeclarationNode variable = new VariableDeclarationNode();
        variable.type = typeToken.get().getValue();
        variable.name = nameToken.get().getValue();
        return Optional.of(variable);
    }

    private Optional<StatementNode> Statement() throws SyntaxErrorException {
        Optional<IfNode> ifNode = If();
        if (ifNode.isPresent()) {
            return Optional.of(ifNode.get());
        }

        Optional<LoopNode> loopNode = Loop();
        if (loopNode.isPresent()) {
            return Optional.of(loopNode.get());
        }

        return disambiguate();
    }

    private Optional<IfNode> If() throws SyntaxErrorException {
        if (tokenManager.matchAndRemove(Token.TokenTypes.IF).isEmpty()) return Optional.empty();

        IfNode ifNode = new IfNode();
        ifNode.condition = BoolExpTerm();

        if (ifNode.condition == null) {
            throw syntaxError("Expected condition in if statement");
        }

        tokenManager.skipNewLines();
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isPresent()) {
            while (!tokenManager.done() && tokenManager.peekCurrent().isPresent() && tokenManager.peekCurrent().get().getType() != Token.TokenTypes.DEDENT) {
                Optional<StatementNode> statement = Statement();
                if (statement.isPresent()) {
                    ifNode.statements.add(statement.get());
                } else {
                    throw syntaxError("Expected statement in if body");
                }
                tokenManager.skipNewLines();
            }
            tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);
        } else {
            throw syntaxError("Expected indentation after if statement");
        }
        return Optional.of(ifNode);
    }

    private Optional<LoopNode> Loop() throws SyntaxErrorException {
        if (tokenManager.matchAndRemove(Token.TokenTypes.LOOP).isEmpty()) return Optional.empty();

        LoopNode loopNode = new LoopNode();
        loopNode.expression = Expression();

        if (loopNode.expression == null) {
            throw syntaxError("Expected expression in loop statement");
        }

        tokenManager.skipNewLines();
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) {
            throw syntaxError("Expected indentation after loop statement");
        }

        while (!tokenManager.done() && tokenManager.peekCurrent().isPresent() && tokenManager.peekCurrent().get().getType() != Token.TokenTypes.DEDENT) {
            Optional<StatementNode> statement = Statement();
            if (statement.isPresent()) {
                loopNode.statements.add(statement.get());
            } else {
                throw syntaxError("Expected statement in loop body");
            }
            tokenManager.skipNewLines();
        }

        tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);
        return Optional.of(loopNode);
    }

    private ExpressionNode BoolExpTerm() throws SyntaxErrorException {
        ExpressionNode left = BoolExpFactor();
        if (left == null) return null;

        while (true) {
            Optional<Token> opToken = tokenManager.matchAndRemove(Token.TokenTypes.EQUAL);
            if (opToken.isEmpty()) {
                opToken = tokenManager.matchAndRemove(Token.TokenTypes.NOTEQUAL);
                if (opToken.isEmpty()) break;
            }

            ExpressionNode right = BoolExpFactor();
            if (right == null) throw syntaxError("Expected boolean expression after operator");

            BooleanOpNode booleanOpNode = new BooleanOpNode();
            booleanOpNode.left = left;
            booleanOpNode.right = right;
            booleanOpNode.op = opToken.get().getType() == Token.TokenTypes.EQUAL ?
                    BooleanOpNode.BooleanOperations.and :
                    BooleanOpNode.BooleanOperations.or;
            left = booleanOpNode;
        }
        return left;
    }

    private ExpressionNode BoolExpFactor() throws SyntaxErrorException {
        ExpressionNode left = Expression();
        if (left == null) return null;

        Optional<Token> opToken = tokenManager.matchAndRemove(Token.TokenTypes.EQUAL);
        if (opToken.isEmpty()) {
            opToken = tokenManager.matchAndRemove(Token.TokenTypes.NOTEQUAL);
            if (opToken.isEmpty()) {
                opToken = tokenManager.matchAndRemove(Token.TokenTypes.LESSTHAN);
                if (opToken.isEmpty()) {
                    opToken = tokenManager.matchAndRemove(Token.TokenTypes.LESSTHANEQUAL);
                    if (opToken.isEmpty()) {
                        opToken = tokenManager.matchAndRemove(Token.TokenTypes.GREATERTHAN);
                        if (opToken.isEmpty()) {
                            opToken = tokenManager.matchAndRemove(Token.TokenTypes.GREATERTHANEQUAL);
                            if (opToken.isEmpty()) return left; // no comparison operator return left expression
                        }
                    }
                }
            }
        }

        ExpressionNode right = Expression();
        if (right == null) throw syntaxError("Expected expression after comparison operator");

        CompareNode compareNode = new CompareNode();
        compareNode.left = left;
        compareNode.right = right;
        compareNode.op = getCompareOperation(opToken.get().getType());
        return compareNode;
    }
    private Optional<VariableReferenceNode> VariableReference() throws SyntaxErrorException {
        Optional<Token> nameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (nameToken.isPresent()) {
            VariableReferenceNode variableReferenceNode = new VariableReferenceNode();
            variableReferenceNode.name = nameToken.get().getValue();
            return Optional.of(variableReferenceNode);
        }
        return Optional.empty();
    }

    private Optional<MethodCallExpressionNode> MethodCallExpression() throws SyntaxErrorException {
        // stubbed out for now
        return Optional.empty();
    }

    private Optional<StatementNode> disambiguate() throws SyntaxErrorException {
        // to parse a method call
        Optional<MethodCallExpressionNode> methodCallExpression = MethodCallExpression();
        if (methodCallExpression.isPresent()) {
            MethodCallStatementNode methodCallStatementNode = new MethodCallStatementNode(methodCallExpression.get());
            return Optional.of(methodCallStatementNode);
        }

        Optional<VariableReferenceNode> variableReference = VariableReference();
        if (variableReference.isEmpty()) {
            return Optional.empty();
        }

        // check if assignment
        if (tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN).isPresent()) {
            AssignmentNode assignmentNode = new AssignmentNode();
            assignmentNode.target = variableReference.get();
            assignmentNode.expression = Expression();
            if (assignmentNode.expression == null) {
                throw syntaxError("Expected expression after '='");
            }
            return Optional.of(assignmentNode);
        }

        // if not assignment treat as method call without return values
        MethodCallStatementNode methodCallStatementNode = new MethodCallStatementNode();
        methodCallStatementNode.methodName = variableReference.get().name;
        return Optional.of(methodCallStatementNode);
    }

    private CompareNode.CompareOperations getCompareOperation(Token.TokenTypes type) {
        switch (type) {
            case EQUAL: return CompareNode.CompareOperations.eq;
            case NOTEQUAL: return CompareNode.CompareOperations.ne;
            case LESSTHAN: return CompareNode.CompareOperations.lt;
            case LESSTHANEQUAL: return CompareNode.CompareOperations.le;
            case GREATERTHAN: return CompareNode.CompareOperations.gt;
            case GREATERTHANEQUAL: return CompareNode.CompareOperations.ge;
            default: throw new IllegalArgumentException("Unexpected token type: " + type);
        }
    }

    private ExpressionNode Expression() throws SyntaxErrorException {
        ExpressionNode left = Term();
        // handle additional terms with + or -
        while (true) {
            Optional<Token> opToken = tokenManager.matchAndRemove(Token.TokenTypes.PLUS);
            if (opToken.isEmpty()) {
                opToken = tokenManager.matchAndRemove(Token.TokenTypes.MINUS);
                if (opToken.isEmpty()) break;
            }
            ExpressionNode right = Term();
            if (right == null) throw syntaxError("Expected expression after operator");

            MathOpNode mathOpNode = new MathOpNode();
            mathOpNode.left = left;
            mathOpNode.right = right;
            mathOpNode.op = opToken.get().getType() == Token.TokenTypes.PLUS ?
                    MathOpNode.MathOperations.add : MathOpNode.MathOperations.subtract;
            left = mathOpNode;
        }
        return left;
    }

    private ExpressionNode Term() throws SyntaxErrorException {
        // start with first factor
        ExpressionNode left = Factor();
        // handle additional factors with *, /, or %
        while (true) {
            Optional<Token> opToken = tokenManager.matchAndRemove(Token.TokenTypes.TIMES);
            if (opToken.isEmpty()) {
                opToken = tokenManager.matchAndRemove(Token.TokenTypes.DIVIDE);
                if (opToken.isEmpty()) {
                    opToken = tokenManager.matchAndRemove(Token.TokenTypes.MODULO);
                    if (opToken.isEmpty()) break;
                }
            }

            ExpressionNode right = Factor();
            if (right == null) throw syntaxError("Expected expression after operator");

            MathOpNode mathOpNode = new MathOpNode();
            mathOpNode.left = left;
            mathOpNode.right = right;
            mathOpNode.op = opToken.get().getType() == Token.TokenTypes.TIMES ?
                    MathOpNode.MathOperations.multiply :
                    (opToken.get().getType() == Token.TokenTypes.DIVIDE ?
                            MathOpNode.MathOperations.divide : MathOpNode.MathOperations.modulo);
            left = mathOpNode;
        }
        return left;
    }

    private ExpressionNode Factor() throws SyntaxErrorException {
        // handle variable references or literals or method calls
        Optional<VariableReferenceNode> variableReference = VariableReference();
        if (variableReference.isPresent()) {
            return variableReference.get();
        }
        // handle numeric literals
        Optional<Token> numberToken = tokenManager.matchAndRemove(Token.TokenTypes.NUMBER);
        if (numberToken.isPresent()) {
            NumericLiteralNode numericLiteralNode = new NumericLiteralNode();
            numericLiteralNode.value = Float.parseFloat(numberToken.get().getValue());
            return numericLiteralNode;
        }
        // handle string literals
        Optional<Token> stringToken = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDSTRING);
        if (stringToken.isPresent()) {
            StringLiteralNode stringLiteralNode = new StringLiteralNode();
            stringLiteralNode.value = stringToken.get().getValue();
            return stringLiteralNode;
        }
        // handle character literals
        Optional<Token> charToken = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDCHARACTER);
        if (charToken.isPresent()) {
            CharLiteralNode charLiteralNode = new CharLiteralNode();
            charLiteralNode.value = charToken.get().getValue().charAt(0);
            return charLiteralNode;
        }
        // handle method calls (stubbed for now)
        Optional<MethodCallExpressionNode> methodCall = MethodCallExpression();
        if (methodCall.isPresent()) {
            return methodCall.get();
        }
        // parentheses
        if (tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()) {
            ExpressionNode expression = Expression();
            if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) {
                throw syntaxError("Expected ')' after expression");
            }
            return expression;
        }
        // 'new' expressions
        if (tokenManager.matchAndRemove(Token.TokenTypes.NEW).isPresent()) {
            NewNode newNode = new NewNode();
            Optional<Token> classNameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
            if (classNameToken.isEmpty()) throw syntaxError("Expected class name after 'new'");
            newNode.className = classNameToken.get().getValue();

            if (tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()) {
                while (!tokenManager.done() && tokenManager.peekCurrent().isPresent() && tokenManager.peekCurrent().get().getType() != Token.TokenTypes.RPAREN) {
                    newNode.parameters.add(Expression());
                    if (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isEmpty()) break;
                }
                if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) {
                    throw syntaxError("Expected ')' after constructor parameters");
                }
            }
            return newNode;
        }

        throw syntaxError("Expected expression");
    }

    private Optional<VariableReferenceNode> VariableNameValue() throws SyntaxErrorException {
        Optional<Token> nameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (nameToken.isPresent()) {
            VariableReferenceNode variableReferenceNode = new VariableReferenceNode();
            variableReferenceNode.name = nameToken.get().getValue();
            return Optional.of(variableReferenceNode);
        }
        return Optional.empty();
    }

    private Optional<InterfaceNode> Interface() throws SyntaxErrorException {
        if (tokenManager.matchAndRemove(Token.TokenTypes.INTERFACE).isEmpty()) return Optional.empty();

        Optional<Token> interfaceNameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (interfaceNameToken.isEmpty()) throw syntaxError("Expected interface name");

        InterfaceNode interfaceNode = new InterfaceNode();
        interfaceNode.name = interfaceNameToken.get().getValue();

        tokenManager.skipNewLines();
        if (tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()) throw syntaxError("Expected indentation after interface declaration");

        while (!tokenManager.done() && tokenManager.peekCurrent().isPresent() && tokenManager.peekCurrent().get().getType() != Token.TokenTypes.DEDENT) {
            Optional<MethodHeaderNode> methodHeader = MethodHeader();
            if (methodHeader.isPresent()) {
                interfaceNode.methods.add(methodHeader.get());
            } else {
                throw syntaxError("Expected method declaration");
            }
            tokenManager.skipNewLines();
        }
        tokenManager.matchAndRemove(Token.TokenTypes.DEDENT);
        return Optional.of(interfaceNode);
    }

    private Optional<MethodHeaderNode> MethodHeader() throws SyntaxErrorException {
        Optional<Token> methodNameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (methodNameToken.isEmpty()) return Optional.empty();

        MethodHeaderNode methodHeaderNode = new MethodHeaderNode();
        methodHeaderNode.name = methodNameToken.get().getValue();

        if (tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isPresent()) {
            methodHeaderNode.parameters = ParameterVariableDeclarations().orElse(new ArrayList<>());
            if (tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()) throw syntaxError("Expected ')' after method parameters");
        }
        if (tokenManager.matchAndRemove(Token.TokenTypes.COLON).isPresent()) {
            methodHeaderNode.returns = ReturnVariableDeclarations().orElse(new ArrayList<>());
        }
        tokenManager.skipNewLines();
        return Optional.of(methodHeaderNode);
    }

    private Optional<List<VariableDeclarationNode>> ParameterVariableDeclarations() throws SyntaxErrorException {
        List<VariableDeclarationNode> parameters = new ArrayList<>();
        while (true) {
            Optional<VariableDeclarationNode> parameter = ParameterVariableDeclaration();
            if (parameter.isEmpty()) break;
            parameters.add(parameter.get());
            if (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isEmpty()) break;
        }
        return Optional.of(parameters);
    }

    private Optional<VariableDeclarationNode> ParameterVariableDeclaration() throws SyntaxErrorException {
        Optional<Token> typeToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (typeToken.isEmpty()) return Optional.empty();

        Optional<Token> nameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (nameToken.isEmpty()) throw syntaxError("Expected parameter name");

        VariableDeclarationNode parameter = new VariableDeclarationNode();
        parameter.type = typeToken.get().getValue();
        parameter.name = nameToken.get().getValue();
        return Optional.of(parameter);
    }

    private Optional<List<VariableDeclarationNode>> ReturnVariableDeclarations() throws SyntaxErrorException {
        List<VariableDeclarationNode> returns = new ArrayList<>();
        while (true) {
            Optional<VariableDeclarationNode> returnVar = ReturnVariableDeclaration();
            if (returnVar.isEmpty()) break;
            returns.add(returnVar.get());
            if (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isEmpty()) break;
        }
        return Optional.of(returns);
    }

    private Optional<VariableDeclarationNode> ReturnVariableDeclaration() throws SyntaxErrorException {
        Optional<Token> typeToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (typeToken.isEmpty()) return Optional.empty();

        Optional<Token> nameToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (nameToken.isEmpty()) throw syntaxError("Expected return variable name");

        VariableDeclarationNode returnVar = new VariableDeclarationNode();
        returnVar.type = typeToken.get().getValue();
        returnVar.name = nameToken.get().getValue();
        return Optional.of(returnVar);
    }

    private SyntaxErrorException syntaxError(String message) {
        return new SyntaxErrorException(message, tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
    }
}