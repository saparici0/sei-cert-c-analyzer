import java.util.HashSet;

public class CCertVisitor extends CBaseVisitor{
    private HashSet undeclaredIdentifier = new HashSet<String>();

    @Override
    public Object visitInitDeclarator(CParser.InitDeclaratorContext ctx) {
        if (ctx.Assign() == null) {
            undeclaredIdentifier.add(ctx.declarator().getText());
            System.out.println(undeclaredIdentifier.toString());
        }

        return super.visitInitDeclarator(ctx);
    }

    @Override
    public Object visitDeclarationSpecifier(CParser.DeclarationSpecifierContext ctx) {
        if (ctx.typeSpecifier() != null && ctx.typeSpecifier().typedefName() != null) {
            undeclaredIdentifier.add(ctx.typeSpecifier().typedefName().Identifier().getText());
            System.out.println(undeclaredIdentifier.toString());
        }

        return super.visitDeclarationSpecifier(ctx);
    }
}
