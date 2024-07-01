import java.util.HashSet;
import org.antlr.v4.runtime.tree.ParseTree;

public class CCertVisitor extends CBaseVisitor {

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
        if (ctx.Identifier() != null && undeclaredIdentifiers.contains(
            ctx.Identifier().getText())) {
            System.out.printf("Error <%d,%d> ", ctx.Identifier().getSymbol().getLine(),
                ctx.Identifier().getSymbol().getCharPositionInLine() + 1);
            System.out.println("EXP33-C. Do not read uninitialized memory");
        }

        return super.visitPrimaryExpression(ctx);
    }

    // FIO34-C: Distinguish between characters read from a file and EOF or WEOF
    @Override
    public Object visitEqualityExpression(CParser.EqualityExpressionContext ctx) {
        // Verificar si la expresión contiene una llamada a fgetc o fgetwc y una comparación con EOF o WEOF
        if (ctx.getText().contains("fgetc") || ctx.getText().contains("fgetwc")) {
            if (ctx.getText().contains("==") || ctx.getText().contains("!=")) {
                if (ctx.getText().contains("EOF") || ctx.getText().contains("WEOF")) {
                    System.out.printf("Error <%d,%d> ", ctx.getStart().getLine(),
                        ctx.getStart().getCharPositionInLine() + 1);
                    System.out.println(
                        "FIO34-C. Distinguish between characters read from a file and EOF or WEOF");
                }
            }
        }
        return super.visitEqualityExpression(ctx);
    }

    // FIO37-C: Do not assume that fgets() or fgetws() returns a nonempty string when successful
    @Override
    public Object visitPostfixExpression(CParser.PostfixExpressionContext ctx) {
        if (ctx.getText().contains("fgets") || ctx.getText().contains("fgetws")) {
            String variable = ctx.primaryExpression().getText();
            ParseTree parent = ctx.getParent();
            while (parent != null && !(parent instanceof CParser.CompoundStatementContext)) {
                parent = parent.getParent();
            }

            if (parent != null && parent instanceof CParser.CompoundStatementContext) {
                CParser.CompoundStatementContext block = (CParser.CompoundStatementContext) parent;
                if (!isStringChecked(variable, block)) {
                    System.out.printf("Error <%d,%d> ", ctx.getStart().getLine(),
                        ctx.getStart().getCharPositionInLine() + 1);
                    System.out.println("FIO37-C. Do not assume that fgets() or fgetws() returns a nonempty string when successful");
                }
            }
        }
        return super.visitPostfixExpression(ctx);
    }

    private boolean isStringChecked(String variable, CParser.CompoundStatementContext block) {
        // Verificar si el bloque contiene una verificación de la longitud de la cadena
        for (ParseTree child : block.children) {
            if (child.getText().contains(variable) && child.getText().contains("strlen")) {
                return true;
            }
        }
        return false;
    }


    // STR38-C: Do not confuse narrow and wide character strings and functions
    @Override
    public Object visitExpression(CParser.ExpressionContext ctx) {
        String text = ctx.getText();
        String[] narrowStringFunctions = {"strlen", "strcpy", "strcat", "strcmp"};
        String[] wideStringFunctions = {"wcslen", "wcscpy", "wcscat", "wcscmp"};

        for (String func : narrowStringFunctions) {
            if (text.contains(func) && text.contains("wchar_t")) {
                System.out.printf("Error <%d,%d> ", ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine() + 1);
                System.out.println("STR38-C. Do not confuse narrow and wide character strings and functions: Using narrow string function with wide string");
            }
        }

        for (String func : wideStringFunctions) {
            if (text.contains(func) && text.contains("char")) {
                System.out.printf("Error <%d,%d> ", ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine() + 1);
                System.out.println("STR38-C. Do not confuse narrow and wide character strings and functions: Using wide string function with narrow string");
            }
        }

        return super.visitExpression(ctx);
    }
}