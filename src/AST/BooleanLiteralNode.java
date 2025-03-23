package AST;

public class BooleanLiteralNode implements ExpressionNode {
    public boolean value;
    public BooleanLiteralNode() {this.value = value;}

    @Override
    public String toString() {
        return value?"true" : "false";
    }
}
