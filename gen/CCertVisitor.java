import java.sql.SQLOutput;
import java.util.*;

public class CCertVisitor extends CBaseVisitor {
    // EXP33-C. Set de identificadores (variables) no declarados al inicializarse
    private final HashSet<String> undeclaredIdentifiers = new HashSet<String>();
    // Diccionario de variables y tipos
    private final Dictionary<String, String> identifiersTypes = new Hashtable<>();

    private boolean srandom = false;

    @Override
    public Object visitDeclaration(CParser.DeclarationContext ctx) {
        // Diccionario de variables y tipos

        ArrayList<String> identifiers = new ArrayList<>();
        ArrayList<String> premodifiers = new ArrayList<>();
        ArrayList<String> postmodifiers = new ArrayList<>();

        if (ctx.initDeclaratorList() == null) { // no hay pre o post modificadores
            String type = null;
            for (int i = 0; i < ctx.declarationSpecifiers().declarationSpecifier().size(); i++) {
                if (ctx.declarationSpecifiers().declarationSpecifier().get(i).typeSpecifier() != null) {
                    if (ctx.declarationSpecifiers().declarationSpecifier().get(i).typeSpecifier().structOrUnionSpecifier() != null) {
                        // if (ctx.declarationSpecifiers().declarationSpecifier().get(i).typeSpecifier().structOrUnionSpecifier().structDeclarationList() != null) {
                        type = ctx.declarationSpecifiers().declarationSpecifier().get(i).typeSpecifier().structOrUnionSpecifier().structOrUnion().getText();
                        identifiers.add(ctx.declarationSpecifiers().declarationSpecifier().get(i).typeSpecifier().structOrUnionSpecifier().Identifier().getText());
                    } else if (type == null) {
                        type = ctx.declarationSpecifiers().declarationSpecifier().get(i).typeSpecifier().getText();
                    } else {
                        identifiers.add(ctx.declarationSpecifiers().declarationSpecifier().get(i).typeSpecifier().getText());
                    }
                }
            }

            for (String identifier : identifiers) {
                identifiersTypes.put(identifier, type);
            }
        } else {
            String type = null;
            for (int i = 0; i < ctx.declarationSpecifiers().declarationSpecifier().size(); i++) {
                if (ctx.declarationSpecifiers().declarationSpecifier().get(i).typeSpecifier() != null) {
                    type = ctx.declarationSpecifiers().declarationSpecifier().get(i).typeSpecifier().getText();
                }
            }

            if (ctx.initDeclaratorList() != null) {
                for (int i = 0; i < ctx.initDeclaratorList().initDeclarator().size(); i++) {
                    if (ctx.initDeclaratorList().initDeclarator().get(i).declarator().pointer() != null) {
                        premodifiers.add(ctx.initDeclaratorList().initDeclarator().get(i).declarator().pointer().getText());
                    } else {
                        premodifiers.add("");
                    }
                    if (ctx.initDeclaratorList().initDeclarator().get(i).declarator().directDeclarator().Identifier() != null) {
                        identifiers.add(ctx.initDeclaratorList().initDeclarator().get(i).declarator().directDeclarator().getText());
                        postmodifiers.add("");
                    } else {
                        CParser.DirectDeclaratorContext directDeclarator = ctx.initDeclaratorList().initDeclarator().get(i).declarator().directDeclarator();
                        ArrayList<String> post = new ArrayList<>();
                        while (true) {
                            if (directDeclarator.Identifier() != null) {
                                identifiers.add(directDeclarator.Identifier().getText());
                                String tempPostModifier = "";
                                for (int j = post.size() - 1; j >= 0; j--) {
                                    tempPostModifier = tempPostModifier.concat(post.get(j));
                                }
                                postmodifiers.add(tempPostModifier);
                                break;
                            } else if (directDeclarator.directDeclarator() != null) {
                                String tempPost = "";
                                for (int j = 1; j < directDeclarator.getChildCount(); j++) {
                                    tempPost = tempPost.concat(directDeclarator.getChild(j).getText());
                                }
                                post.add(tempPost);
                                directDeclarator = directDeclarator.directDeclarator();
                            } else if (directDeclarator.declarator() != null) {
                                if (directDeclarator.declarator().pointer() != null) {
                                     premodifiers.set(premodifiers.size()-1, premodifiers.get(premodifiers.size()-1).concat(directDeclarator.declarator().pointer().getText()));
                                }
                                directDeclarator = directDeclarator.declarator().directDeclarator();
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < identifiers.size(); i++) {
                identifiersTypes.put(identifiers.get(i), premodifiers.get(i) + type + postmodifiers.get(i));
            }
        }

        return super.visitDeclaration(ctx);
    }

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
    public Object visitAssignmentExpression(CParser.AssignmentExpressionContext ctx) {
        if (ctx.assignmentOperator() != null && ctx.assignmentOperator().getText().equals("=")) {
            undeclaredIdentifiers.remove(ctx.unaryExpression().getText());
        }

        return super.visitAssignmentExpression(ctx);
    }

    @Override
    public Object visitPrimaryExpression(CParser.PrimaryExpressionContext ctx) {
        // EXP33-C
        System.out.println(identifiersTypes);
        if (ctx.Identifier() != null) {
            if (identifiersTypes.get(ctx.Identifier().getText()) != null) {
                // System.out.println(ctx.Identifier().getText() + ": " + identifiersTypes.get(ctx.Identifier().getText()));
            } if (undeclaredIdentifiers.contains(ctx.Identifier().getText())) {
                System.out.printf("Error <%d,%d> ", ctx.Identifier().getSymbol().getLine(), ctx.Identifier().getSymbol().getCharPositionInLine() + 1);
                System.out.println("EXP33-C. Do not read uninitialized memory");
            }
        }

        return super.visitPrimaryExpression(ctx);
    }

    @Override
    public Object visitParameterList(CParser.ParameterListContext ctx) {
        for (int i = 0; i < ctx.parameterDeclaration().size(); i++) {
            if (ctx.parameterDeclaration().get(i).declarationSpecifiers() != null) {
                String type = ctx.parameterDeclaration().get(i).declarationSpecifiers().declarationSpecifier(ctx.parameterDeclaration().get(i).declarationSpecifiers().declarationSpecifier().size() - 1).getText();
                String identifier = ctx.parameterDeclaration().get(i).declarator().getText();

                identifiersTypes.put(identifier, type);
            }
        }

        return super.visitParameterList(ctx);
    }

    @Override
    public Object visitPostfixExpression(CParser.PostfixExpressionContext ctx){
        if(ctx.primaryExpression() != null && ctx.primaryExpression().getText().equals("srandom") && ctx.primaryExpression().Identifier() != null){
            srandom = true;
        }
        if(ctx.primaryExpression() != null && ctx.primaryExpression().getText().equals("random") && !srandom && ctx.primaryExpression().Identifier() != null){
            System.out.printf("Error <%d,%d> ", ctx.primaryExpression().Identifier().getSymbol().getLine(), ctx.primaryExpression().Identifier().getSymbol().getCharPositionInLine() + 1);
            System.out.println("MSC32-C. Properly seed pseudorandom number generators.");
        }

        return super.visitPostfixExpression(ctx);
    }
}
