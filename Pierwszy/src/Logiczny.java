import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class Logiczny extends ExprParserBaseVisitor<Boolean> {

    Map<String, Boolean> memory = new HashMap<>();

    // Store function definitions: name -> context
    Map<String, ExprParser.DefContext> functions = new HashMap<>();

    @Override
    public Boolean visitProgram(ExprParser.ProgramContext ctx) {
        Boolean result = false;
        for (int i = 0; i < ctx.getChildCount() - 1; i++) {
            result = visit(ctx.getChild(i));
        }
        return result;
    }

    @Override
    public Boolean visitDef(ExprParser.DefContext ctx) {
        // Just register the function, don't execute it yet
        String funcName = ctx.ID(0).getText();
        functions.put(funcName, ctx);
        return false;
    }

    @Override
    public Boolean visitFunc(ExprParser.FuncContext ctx) {
        String funcName = ctx.ID().getText();
        ExprParser.DefContext def = functions.get(funcName);

        if (def == null) {
            throw new RuntimeException("Undefined function: " + funcName);
        }

        // Map parameter names to argument values
        // def.ID(0) is the function name, def.ID(1), ID(2)... are params
        List<ExprParser.ExprContext> args = ctx.expr();

        // Save current memory scope, create new one for function
        Map<String, Boolean> savedMemory = new HashMap<>(memory);

        for (int i = 0; i < args.size(); i++) {
            String paramName = def.ID(i + 1).getText(); // +1 to skip function name
            Boolean argValue = visit(args.get(i));
            memory.put(paramName, argValue);
        }

        // Execute all statements in the function body
        Boolean result = false;
        for (ExprParser.StatContext stat : def.stat()) {
            result = visit(stat);
        }

        // Restore outer scope
        memory = savedMemory;
        return result;
    }

    @Override
    public Boolean visitStat(ExprParser.StatContext ctx) {
        if (ctx.ID() != null && ctx.expr() != null) {
            Boolean value = visit(ctx.expr());
            memory.put(ctx.ID().getText(), value);
            return value;
        }
        return visit(ctx.expr());
    }

    @Override
    public Boolean visitExpr(ExprParser.ExprContext ctx) {
        if (ctx.INT() != null) {
            return !ctx.INT().getText().equals("0");
        }
        if (ctx.ID() != null && ctx.getChildCount() == 1) {
            return memory.getOrDefault(ctx.ID().getText(), false);
        }
        if (ctx.func() != null) {
            return visit(ctx.func());
        }
        if (ctx.NOT() != null) {
            return !visit(ctx.expr(0));
        }
        if (ctx.AND() != null) {
            return visit(ctx.expr(0)) && visit(ctx.expr(1));
        }
        if (ctx.OR() != null) {
            return visit(ctx.expr(0)) || visit(ctx.expr(1));
        }
        return false;
    }
}