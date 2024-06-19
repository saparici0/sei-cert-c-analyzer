import java.util.HashSet;

public class CCertVisitor extends CBaseVisitor{
    // EXP33-C. Set de identificadores (variables) no declarados al inicializarse
    private final HashSet<String> undeclaredIdentifiers = new HashSet<String>();

    @Override
    public Object visitInitDeclarator(CParser.InitDeclaratorContext ctx) {
        // EXP33-C
        if (ctx.Assign() == null) {
            undeclaredIdentifiers.add(ctx.declarator().getText());
        }

        return super.visitInitDeclarator(ctx);
    }

    @Override
    public Object visitDeclarationSpecifier(CParser.DeclarationSpecifierContext ctx) {
        // EXP33-C
        if (ctx.typeSpecifier() != null && ctx.typeSpecifier().typedefName() != null) {
            undeclaredIdentifiers.add(ctx.typeSpecifier().typedefName().Identifier().getText());
        }

        return super.visitDeclarationSpecifier(ctx);
    }

    @Override
    public Object visitPrimaryExpression(CParser.PrimaryExpressionContext ctx) {
        // EXP33-C
        if (ctx.Identifier() != null && undeclaredIdentifiers.contains(ctx.Identifier().getText())) {
            System.out.printf("Error <%d,%d> ",ctx.Identifier().getSymbol().getLine(),ctx.Identifier().getSymbol().getCharPositionInLine() + 1);
            System.out.println("EXP33-C. Do not read uninitialized memory");
        }

        return super.visitPrimaryExpression(ctx);
    }
}
