import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GyhCompilerVisitor extends GyhGrammarBaseListener {
    private final SymbolTable symbolTable = new SymbolTable();
    private final StringBuilder codeBuffer = new StringBuilder();
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    @Override
    public void enterPrograma(GyhGrammarParser.ProgramaContext ctx) {
        System.out.println("Iniciando análise do programa...");
    }

    @Override
    public void exitPrograma(GyhGrammarParser.ProgramaContext ctx) {
        System.out.println("Análise do programa concluída!");
    }

    @Override
    public void exitDeclaracao(GyhGrammarParser.DeclaracaoContext ctx) {
        String varName = ctx.Var().getText();
        String typeText = ctx.tipoVar().PCInt() != null ? "INT" : "REAL";
        int type = "INT".equals(typeText) ? Symbol.INT : Symbol.REAL;

        if (symbolTable.exists(varName)) {
            errors.add("Erro Semântico: Variável '" + varName + "' já foi declarada!");
            return;
        }

        symbolTable.add(new Symbol(varName, type, "0"));
        System.out.println("[ANÁLISE] Variável declarada: " + varName + " (tipo: " + typeText + ")");
    }

    @Override
    public void exitComandoAtribuicao(GyhGrammarParser.ComandoAtribuicaoContext ctx) {
        String varName = ctx.Var().getText();
        String expr = validarEConverterExpressaoAritmetica(ctx.expressaoAritmetica());
        if (expr == null) {
            return;
        }

        if (!symbolTable.exists(varName)) {
            errors.add("Erro Semântico: Variável '" + varName + "' não foi declarada!");
            return;
        }

        codeBuffer.append("    ").append(varName).append(" = ").append(expr).append(";\n");
    }

    @Override
    public void exitComandoEntrada(GyhGrammarParser.ComandoEntradaContext ctx) {
        String varName = ctx.Var().getText();
        Symbol symbol = symbolTable.get(varName);
        if (symbol == null) {
            errors.add("Erro Semântico: Variável '" + varName + "' não foi declarada!");
            return;
        }

        if (symbol.getType() == Symbol.INT) {
            codeBuffer.append("    scanf(\"%d\", &").append(varName).append(");\n");
        } else {
            codeBuffer.append("    scanf(\"%f\", &").append(varName).append(");\n");
        }
    }

    @Override
    public void exitComandoSaida(GyhGrammarParser.ComandoSaidaContext ctx) {
        if (ctx.Var() != null) {
            String varName = ctx.Var().getText();
            Symbol symbol = symbolTable.get(varName);
            if (symbol == null) {
                errors.add("Erro Semântico: Variável '" + varName + "' não foi declarada!");
                return;
            }
            if (symbol.getType() == Symbol.INT) {
                codeBuffer.append("    printf(\"%d\\n\", ").append(varName).append(");\n");
            } else {
                codeBuffer.append("    printf(\"%f\\n\", ").append(varName).append(");\n");
            }
        } else if (ctx.STRING() != null) {
            codeBuffer.append("    printf(").append(ctx.STRING().getText()).append(");\n");
        }
    }

    @Override
    public void exitComandoCondicao(GyhGrammarParser.ComandoCondicaoContext ctx) {
        String cond = validarEConverterExpressaoRelacional(ctx.expressaoRelacional());
        if (cond == null) {
            return;
        }

        StringBuilder body = new StringBuilder();
        for (GyhGrammarParser.ComandoContext cmd : ctx.comando()) {
            String generated = gerarCodigoDoComando(cmd);
            if (generated != null && !generated.isEmpty()) {
                body.append(generated);
            }
        }

        codeBuffer.append("    if (").append(cond).append(") {\n");
        codeBuffer.append(body);
        codeBuffer.append("    }\n");
    }

    @Override
    public void exitComandoRepeticao(GyhGrammarParser.ComandoRepeticaoContext ctx) {
        String cond = validarEConverterExpressaoRelacional(ctx.expressaoRelacional());
        if (cond == null) {
            return;
        }

        StringBuilder body = new StringBuilder();
        String generated = gerarCodigoDoComando(ctx.comando());
        if (generated != null && !generated.isEmpty()) {
            body.append(generated);
        }

        codeBuffer.append("    while (").append(cond).append(") {\n");
        codeBuffer.append(body);
        codeBuffer.append("    }\n");
    }

    private String gerarCodigoDoComando(GyhGrammarParser.ComandoContext ctx) {
        if (ctx == null) {
            return null;
        }

        if (ctx.comandoAtribuicao() != null) {
            String varName = ctx.comandoAtribuicao().Var().getText();
            String expr = validarEConverterExpressaoAritmetica(ctx.comandoAtribuicao().expressaoAritmetica());
            return "    " + varName + " = " + expr + ";\n";
        }

        if (ctx.comandoEntrada() != null) {
            String varName = ctx.comandoEntrada().Var().getText();
            Symbol symbol = symbolTable.get(varName);
            if (symbol != null && symbol.getType() == Symbol.INT) {
                return "    scanf(\"%d\", &" + varName + ");\n";
            }
            return "    scanf(\"%f\", &" + varName + ");\n";
        }

        if (ctx.comandoSaida() != null) {
            if (ctx.comandoSaida().Var() != null) {
                String varName = ctx.comandoSaida().Var().getText();
                Symbol symbol = symbolTable.get(varName);
                if (symbol != null && symbol.getType() == Symbol.INT) {
                    return "    printf(\"%d\\n\", " + varName + ");\n";
                }
                return "    printf(\"%f\\n\", " + varName + ");\n";
            }
            return "    printf(" + ctx.comandoSaida().STRING().getText() + ");\n";
        }

        if (ctx.comandoCondicao() != null) {
            String cond = validarEConverterExpressaoRelacional(ctx.comandoCondicao().expressaoRelacional());
            StringBuilder body = new StringBuilder();
            for (GyhGrammarParser.ComandoContext child : ctx.comandoCondicao().comando()) {
                String nested = gerarCodigoDoComando(child);
                if (nested != null && !nested.isEmpty()) {
                    body.append(nested);
                }
            }
            return "    if (" + cond + ") {\n" + body + "    }\n";
        }

        if (ctx.comandoRepeticao() != null) {
            String cond = validarEConverterExpressaoRelacional(ctx.comandoRepeticao().expressaoRelacional());
            String nested = gerarCodigoDoComando(ctx.comandoRepeticao().comando());
            return "    while (" + cond + ") {\n" + (nested != null ? nested : "") + "    }\n";
        }

        return null;
    }

    private String validarEConverterExpressaoAritmetica(GyhGrammarParser.ExpressaoAritmeticaContext ctx) {
        if (ctx == null) {
            return null;
        }
        return ctx.getText();
    }

    private String validarEConverterExpressaoRelacional(GyhGrammarParser.ExpressaoRelacionalContext ctx) {
        if (ctx == null) {
            return null;
        }
        String processado = ctx.getText();
        processado = processado.replaceAll("([a-zA-Z0-9])E([a-zA-Z0-9])", "$1 && $2");
        processado = processado.replaceAll("([a-zA-Z0-9])OU([a-zA-Z0-9])", "$1 || $2");
        processado = processado.replaceAll("!=", "!=");
        processado = processado.replaceAll("==", "==");
        processado = processado.replace("OU", "||");
        processado = processado.replace("E", "&&");
        return processado;
    }

    private String extrairTextoDe(ParseTree tree) {
        if (tree == null) {
            return null;
        }
        String text = tree.getText();
        if (text == null || text.isEmpty()) {
            return null;
        }
        return text;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public StringBuilder getCodeBuffer() {
        return codeBuffer;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
