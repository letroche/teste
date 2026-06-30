import java.io.FileWriter;
import java.io.IOException;

public class GyhProgram {
    private SymbolTable varTable;
    private StringBuilder code;
    private String outputFileName;

    public GyhProgram(SymbolTable varTable, StringBuilder code, String outputFileName) {
        this.varTable = varTable;
        this.code = code;
        this.outputFileName = outputFileName;
    }

    public void generateTarget() throws IOException {
        StringBuilder cCode = new StringBuilder();

        // Cabeçalho do arquivo C
        cCode.append("#include <stdio.h>\n");
        cCode.append("#include <stdlib.h>\n");
        cCode.append("\n");
        cCode.append("int main() {\n");

        // Declarações de variáveis
        for (Symbol symbol : varTable.getAllSymbols()) {
            if (symbol.getType() == Symbol.REAL) {
                cCode.append("    float ");
            } else if (symbol.getType() == Symbol.INT) {
                cCode.append("    int ");
            }
            cCode.append(symbol.getName());
            cCode.append(" = 0;\n");
        }

        // Adiciona uma linha em branco se houver variáveis
        if (varTable.getAllSymbols().size() > 0) {
            cCode.append("\n");
        }

        // Código gerado
        cCode.append(code);

        // Fechamento da função main
        cCode.append("\n");
        cCode.append("    return 0;\n");
        cCode.append("}\n");

        // Escreve no arquivo
        writeToFile(cCode.toString());
    }

    private void writeToFile(String content) throws IOException {
        try (FileWriter writer = new FileWriter(outputFileName)) {
            writer.write(content);
            System.out.println("Código C gerado: " + outputFileName);
        }
    }

    public String getGeneratedCode() {
        StringBuilder cCode = new StringBuilder();

        cCode.append("#include <stdio.h>\n");
        cCode.append("#include <stdlib.h>\n");
        cCode.append("\n");
        cCode.append("int main() {\n");

        for (Symbol symbol : varTable.getAllSymbols()) {
            if (symbol.getType() == Symbol.REAL) {
                cCode.append("    float ");
            } else if (symbol.getType() == Symbol.INT) {
                cCode.append("    int ");
            }
            cCode.append(symbol.getName());
            cCode.append(" = 0;\n");
        }

        if (varTable.getAllSymbols().size() > 0) {
            cCode.append("\n");
        }

        cCode.append(code);
        cCode.append("\n");
        cCode.append("    return 0;\n");
        cCode.append("}\n");

        return cCode.toString();
    }
}
