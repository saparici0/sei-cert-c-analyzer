import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class Main {
    public static void main(String[] args) throws Exception {
        // create a CharStream that reads from standard input / file
        // create a lexer that feeds off of input CharStream
        CLexer lexer;

        if (args.length>0)
            lexer = new CLexer(CharStreams.fromFileName(args[0]));
        else
            lexer = new CLexer(CharStreams.fromStream(System.in));

        // create a buffer of tokens pulled from the lexer
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        // create a parser that feeds off the tokens buffer
        CParser parser = new CParser(tokens);
        ParseTree tree = parser.compilationUnit(); // begin parsing at init rule

        CCertVisitor loader = new CCertVisitor();
        loader.visit(tree);
    }
}