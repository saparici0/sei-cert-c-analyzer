import org.antlr.v4.runtime.tree.ParseTree;
import java.util.*;
import java.util.regex.Pattern;
import java.sql.SQLOutput;

public class CCertVisitor extends CBaseVisitor {
    // EXP33-C. Set de identificadores (variables) no declarados al inicializarse
    private final HashSet<String> undeclaredIdentifiers = new HashSet<String>();
    private final Pattern sensitivePattern = Pattern.compile(
        ".*(password|passwd|pwd|secret|key|token|apikey|api_key|private_key).*",
        Pattern.CASE_INSENSITIVE
    );
    // Diccionario de variables y tipos
    private final Dictionary<String, String> identifiersTypes = new Hashtable<>();
    // STR32-C. Set de identificadores (variables) string no null-terminadas
    private final HashSet<String> nonNullTerminatedCharSeqs = new HashSet<>();
    private final HashSet<String> dynamicallyAllocatedIdentifiers = new HashSet<>();
    // STR32-C
    private final HashSet<String> cStringFunctions = new HashSet<>(Arrays.asList(
            "strcpy", "strncpy", "strcat", "strncat", "strcmp", "strncmp", "strlen", "strchr", "strrchr", "strstr", "strpbrk", "strcspn", "strspn", "strtok",
            "printf", "scanf", "sprintf", "sscanf", "fgets", "fputs",
            "memcpy", "memmove", "memset",
            "strerror", "getenv"
    ));
    // SIG30-C
    private final HashSet<String> asyncSafeFunctions = new HashSet<>(Arrays.asList(
            "_Exit","fexecve","posix_trace_event","sigprocmask","_exit","fork","pselect","sigqueue","abort","fstat","pthread_kill","sigset","accept","fstatat","pthread_self","sigsuspend","access","fsync","pthread_sigmask","sleep","aio_error","ftruncate","raise","sockatmark","aio_return","futimens","read","socket","aio_suspend","getegid","readlink","socketpair","alarm","geteuid","readlinkat","stat","bind","getgid","recv","symlink","cfgetispeed","getgroups","recvfrom","symlinkat","cfgetospeed","getpeername","recvmsg","tcdrain","cfsetispeed","getpgrp","rename","tcflow","cfsetospeed","getpid","renameat","tcflush","chdir","getppid","rmdir","tcgetattr","chmod","getsockname","select","tcgetpgrp","chown","getsockopt","sem_post","tcsendbreak","clock_gettime","getuid","send","tcsetattr","close","kill","sendmsg","tcsetpgrp","connect","link","sendto","time","creat","linkat","setgid","timer_getoverrun","dup","listen","setpgid","timer_gettime","dup2","lseek","setsid","timer_settime","execl","lstat","setsockopt","times","execle","mkdir","setuid","umask","execv","mkdirat","shutdown","uname","execve","mkfifo","sigaction","unlink","faccessat","mkfifoat","sigaddset","unlinkat","fchdir","mknod","sigdelset","utime","fchmod","mknodat","sigemptyset","utimensat","fchmodat","open","sigfillset","utimes","fchown","openat","sigismember","wait","fchownat","pause","signal","waitpid","fcntl","pipe","sigpause","write","fdatasync","poll","sigpending"
    ));
    // check pseudorandom numbers MSC32-C
    private boolean srandom = false;
    // replace solutions for asctime MSC33-C
    private boolean strftime = false;
    private boolean asctime_c = false;

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
                        identifiers.add(ctx.initDeclaratorList().initDeclarator().get(i).declarator().directDeclarator().Identifier().getText());
                        // MEM34-C
                        if (ctx.initDeclaratorList().initDeclarator().get(i).initializer() != null && ctx.initDeclaratorList().initDeclarator().get(i).initializer().getText().contains("malloc")) {
                            dynamicallyAllocatedIdentifiers.add(ctx.initDeclaratorList().initDeclarator().get(i).declarator().directDeclarator().Identifier().getText());
                        }

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

                                // STR32-C
                                if (type != null && type.equals("char") && tempPostModifier.startsWith("[") && tempPostModifier.endsWith("]")) {
                                    if (ctx.initDeclaratorList().initDeclarator().get(i).initializer() != null) {
                                        try {
                                            if (ctx.initDeclaratorList().initDeclarator().get(i).initializer().getText().length() - 1 != Integer.parseInt(tempPostModifier.substring(1, tempPostModifier.length() - 1))) {
                                                if (!ctx.initDeclaratorList().initDeclarator().get(i).initializer().getText().startsWith("\\0", ctx.initDeclaratorList().initDeclarator().get(i).initializer().getText().length() - 3)) {
                                                    nonNullTerminatedCharSeqs.add(directDeclarator.Identifier().getText());
                                                }
                                            }
                                        } catch (Exception ignored) {
                                        }
                                    }
                                }

                                // MEM34-C
                                if (ctx.initDeclaratorList().initDeclarator().get(i).initializer() != null && ctx.initDeclaratorList().initDeclarator().get(i).initializer().getText().contains("malloc")) {
                                    dynamicallyAllocatedIdentifiers.add(directDeclarator.Identifier().getText());
                                }

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
                                    premodifiers.set(premodifiers.size() - 1, premodifiers.get(premodifiers.size() - 1).concat(directDeclarator.declarator().pointer().getText()));
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
            // EXP33-C
            undeclaredIdentifiers.remove(ctx.unaryExpression().getText());
            // MEM34-C
            if (!dynamicallyAllocatedIdentifiers.contains(ctx.unaryExpression().getText()) && (ctx.assignmentExpression().getText().contains("malloc") || ctx.assignmentExpression().getText().contains("realloc"))) {
                dynamicallyAllocatedIdentifiers.add(ctx.unaryExpression().getText());
            } else if (dynamicallyAllocatedIdentifiers.contains(ctx.unaryExpression().getText()) && !ctx.assignmentExpression().getText().contains("malloc") && !ctx.assignmentExpression().getText().contains("realloc")) {
                dynamicallyAllocatedIdentifiers.remove(ctx.unaryExpression().getText());
            }
        }
        // MSC41-C
        if (sensitivePattern.matcher(ctx.getText()).matches()) {
            System.out.printf("Error <%d,%d> ", ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine() + 1);
            System.out.println("MSC41-C. Never hard code sensitive information");
        }

        return super.visitAssignmentExpression(ctx);
    }

    @Override
    public Object visitPrimaryExpression(CParser.PrimaryExpressionContext ctx) {
        if (ctx.Identifier() != null) {
            // EXP33-C
            if (undeclaredIdentifiers.contains(ctx.Identifier().getText())) {
                System.out.printf("Error <%d,%d> ", ctx.Identifier().getSymbol().getLine(), ctx.Identifier().getSymbol().getCharPositionInLine() + 1);
                System.out.println("EXP33-C. Do not read uninitialized memory");
            }
        }

        return super.visitPrimaryExpression(ctx);
    }

    // FIO34-C
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

    private boolean isStringChecked(String variable, CParser.CompoundStatementContext block) {
        // Verificar si el bloque contiene una verificación de la longitud de la cadena
        for (ParseTree child : block.children) {
            if (child.getText().contains(variable) && child.getText().contains("strlen")) {
                return true;
            }
        }
        return false;
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
    public Object visitPostfixExpression(CParser.PostfixExpressionContext ctx) {
        // STR32-C
        if (ctx.primaryExpression() != null && ctx.primaryExpression().Identifier() != null && cStringFunctions.contains(ctx.primaryExpression().Identifier().getText())) {
            if (!ctx.argumentExpressionList().isEmpty()) {
                for (String charSeq: nonNullTerminatedCharSeqs) {
                    for (int i=0; i < ctx.argumentExpressionList().get(0).assignmentExpression().size(); i++) {
                        if (ctx.argumentExpressionList().get(0).assignmentExpression().get(i).getText().contains(charSeq)) {
                            System.out.printf("Error <%d,%d> ", ctx.argumentExpressionList().get(0).assignmentExpression().get(i).getStart().getLine(), ctx.argumentExpressionList().get(0).assignmentExpression().get(i).getStart().getCharPositionInLine() + 1);
                            System.out.println("STR32-C. Do not pass a non-null-terminated character sequence to a library function that expects a string");
                        }
                    }
                }
            }
        }
        // MEM34-C
        if (ctx.primaryExpression() != null && ctx.primaryExpression().Identifier() != null && ctx.primaryExpression().Identifier().getText().equals("free")) {
            if (!ctx.argumentExpressionList().isEmpty()) {
                for (int i=0; i < ctx.argumentExpressionList().get(0).assignmentExpression().size(); i++) {
                    if (!dynamicallyAllocatedIdentifiers.contains(ctx.argumentExpressionList().get(0).assignmentExpression().get(i).getText())) {
                        System.out.printf("Error <%d,%d> ", ctx.argumentExpressionList().get(0).assignmentExpression().get(i).getStart().getLine(), ctx.argumentExpressionList().get(0).assignmentExpression().get(i).getStart().getCharPositionInLine() + 1);
                        System.out.println("MEM34-C. Only free memory allocated dynamically");
                    }
                }
            }
        }
        // SIG30-C
        if (ctx.primaryExpression() != null && ctx.primaryExpression().Identifier() != null && ctx.primaryExpression().Identifier().getText().equals("signal")) {
            if (!ctx.argumentExpressionList().isEmpty()) {
                if (ctx.argumentExpressionList().get(0).assignmentExpression().size() == 2) {
                    if (!asyncSafeFunctions.contains(ctx.argumentExpressionList().get(0).assignmentExpression().get(1).getText())) {
                        System.out.printf("Error <%d,%d> ", ctx.argumentExpressionList().get(0).assignmentExpression().get(1).getStart().getLine(), ctx.argumentExpressionList().get(0).assignmentExpression().get(1).getStart().getCharPositionInLine() + 1);
                        System.out.println("SIG30-C. Call only asynchronous-safe functions within signal handlers");
                    }
                }
            }
        }
        // MSC32-C
        if(ctx.primaryExpression() != null && ctx.primaryExpression().getText().equals("srandom") && ctx.primaryExpression().Identifier() != null){
            srandom = true;
        }
        if(ctx.primaryExpression() != null && ctx.primaryExpression().getText().equals("random") && !srandom && ctx.primaryExpression().Identifier() != null){
            System.out.printf("Error <%d,%d> ", ctx.primaryExpression().Identifier().getSymbol().getLine(), ctx.primaryExpression().Identifier().getSymbol().getCharPositionInLine() + 1);
            System.out.println("MSC32-C. Properly seed pseudorandom number generators.");
        }
        // MSC3E3-C
        if(ctx.primaryExpression() != null && ctx.primaryExpression().getText().equals("asctime_s") && ctx.primaryExpression().getText().equals("strtime") && ctx.primaryExpression().Identifier() != null){
            strftime = true;
            asctime_c = true;
        }
        if(ctx.primaryExpression() != null && ctx.primaryExpression().getText().equals("asctime") && !asctime_c && !strftime && ctx.primaryExpression().Identifier() != null){
            System.out.printf("Error <%d,%d> ", ctx.primaryExpression().Identifier().getSymbol().getLine(), ctx.primaryExpression().Identifier().getSymbol().getCharPositionInLine() + 1);
            System.out.println("MSC33-C. The asctime() is deprecated or is an obsolescent funcion, use  asctime_s() or strftime() instead");
        }
        // FIO37-C
        if (ctx.getText().contains("fgets") || ctx.getText().contains("fgetws")) {
            String variable = ctx.primaryExpression().getText();
            ParseTree parent = ctx.getParent();
            while (parent != null && !(parent instanceof CParser.CompoundStatementContext)) {
                parent = parent.getParent();
            }

            if (parent != null && parent instanceof CParser.CompoundStatementContext) {
                CParser.CompoundStatementContext block = (CParser.CompoundStatementContext) parent;
                if (!isStringChecked(variable, block)) {
                    System.out.printf("Error <%d,%d> ", ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine() + 1);
                    System.out.println("FIO37-C. Do not assume that fgets() or fgetws() returns a nonempty string when successful");
                }
            }
        }

        return super.visitPostfixExpression(ctx);
    }

    @Override
    public Object visitFunctionDefinition(CParser.FunctionDefinitionContext ctx) {
        String funcIdentifier = ctx.declarator().directDeclarator().directDeclarator().getText();
        boolean asyncSafe = true;

        for (int i=0; i < ctx.compoundStatement().blockItemList().blockItem().size(); i++) {
            try {
                CParser.PostfixExpressionContext postFixExprCtx = ctx.compoundStatement().blockItemList().blockItem().get(i).statement().expressionStatement().expression().assignmentExpression().get(0).conditionalExpression().logicalOrExpression().logicalAndExpression().get(0).inclusiveOrExpression().get(0).exclusiveOrExpression().get(0).andExpression().get(0).equalityExpression().get(0).relationalExpression().get(0).shiftExpression().get(0).additiveExpression().get(0).multiplicativeExpression().get(0).castExpression().get(0).unaryExpression().postfixExpression();
                if (postFixExprCtx != null) {
                    if (postFixExprCtx.primaryExpression() != null && postFixExprCtx.primaryExpression().Identifier() != null) {
                        if (!asyncSafeFunctions.contains(postFixExprCtx.primaryExpression().Identifier().getText())) {
                            asyncSafe = false;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        if (asyncSafe) {
            asyncSafeFunctions.add(funcIdentifier);
        }

        return super.visitFunctionDefinition(ctx);
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
                System.out.println(
                    "STR38-C. Do not confuse narrow and wide character strings and functions: Using narrow string function with wide string");
            }
        }

        for (String func : wideStringFunctions) {
            if (text.contains(func) && text.contains("char")) {
                System.out.printf("Error <%d,%d> ", ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine() + 1);
                System.out.println(
                    "STR38-C. Do not confuse narrow and wide character strings and functions: Using wide string function with narrow string");
            }
        }

        return super.visitExpression(ctx);
    }

    // POS47-C: Do not use threads that can be canceled asynchronously
    @Override
    public Object visitExpressionStatement(CParser.ExpressionStatementContext ctx) {
        if (ctx.getText().contains("pthread_cancel")) {
            System.out.printf("Error <%d,%d> ", ctx.getStart().getLine(),
                ctx.getStart().getCharPositionInLine() + 1);
            System.out.println("POS47-C. Do not use threads that can be canceled asynchronously");
        }
        return super.visitExpressionStatement(ctx);
    }

}