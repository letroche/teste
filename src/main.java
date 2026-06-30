import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import java.io.*;

public class main {
    public static void main(String[] args) {
        String inputFileName = args.length > 0 ? args[0] : "programa.gyh";
        String outputFileName = inputFileName.replace(".gyh", ".c");

        try {
            System.out.println("========================================");
            System.out.println("Compilador GYH");
            System.out.println("========================================");
            System.out.println("Arquivo de entrada: " + inputFileName);
            System.out.println();

            // ANÁLISE LÉXICA
            System.out.println("[1] ANÁLISE LÉXICA");
            System.out.println("-----------------------------------------");
            CharStream cs = CharStreams.fromFileName(inputFileName);
            GyhGrammarLexer lexer = new GyhGrammarLexer(cs);
            
            // Criar um listener customizado para erros léxicos
            LexerErrorListener lexerErrorListener = new LexerErrorListener();
            lexer.removeErrorListeners();
            lexer.addErrorListener(lexerErrorListener);

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            tokens.fill();

            System.out.println("Tokens encontrados: " + tokens.getNumberOfOnChannelTokens());
            System.out.println("✓ Análise Léxica concluída com sucesso!");
            System.out.println();

            // ANÁLISE SINTÁTICA
            System.out.println("[2] ANÁLISE SINTÁTICA");
            System.out.println("-----------------------------------------");
            GyhGrammarParser parser = new GyhGrammarParser(tokens);
            
            // Criar um listener customizado para erros sintáticos
            ParserErrorListener parserErrorListener = new ParserErrorListener();
            parser.removeErrorListeners();
            parser.addErrorListener(parserErrorListener);

            ParseTree tree = parser.programa();
            
            System.out.println("✓ Análise Sintática concluída com sucesso!");
            System.out.println();

            // ANÁLISE SEMÂNTICA E GERAÇÃO DE CÓDIGO
            System.out.println("[3] ANÁLISE SEMÂNTICA E GERAÇÃO DE CÓDIGO");
            System.out.println("-----------------------------------------");
            GyhCompilerVisitor visitor = new GyhCompilerVisitor();
            visitor.visit(tree);

            // Verificar erros
            if (visitor.hasErrors()) {
                System.out.println("✗ ERROS ENCONTRADOS:");
                for (String error : visitor.getErrors()) {
                    System.out.println("  - " + error);
                }
                System.out.println("\nCompilação FALHADA!");
                System.exit(1);
            }

            System.out.println("✓ Análise Semântica concluída com sucesso!");
            System.out.println();

            // GERAÇÃO DO CÓDIGO C
            System.out.println("[4] GERAÇÃO DO CÓDIGO");
            System.out.println("-----------------------------------------");
            GyhProgram gyhProgram = new GyhProgram(visitor.getSymbolTable(), 
                                                    visitor.getCodeBuffer(), 
                                                    outputFileName);
            gyhProgram.generateTarget();
            System.out.println();

            // EXIBIR CÓDIGO GERADO
            System.out.println("[5] CÓDIGO C GERADO");
            System.out.println("-----------------------------------------");
            String generatedCode = gyhProgram.getGeneratedCode();
            System.out.println(generatedCode);
            System.out.println();

            // COMPILAÇÃO DO CÓDIGO C
            System.out.println("[6] COMPILAÇÃO DO CÓDIGO C");
            System.out.println("-----------------------------------------");
            String executableName = inputFileName.replace(".gyh", "");
            ProcessBuilder pb = new ProcessBuilder("gcc", outputFileName, "-o", executableName);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            StringBuilder compileOutput = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                compileOutput.append(line).append("\n");
            }

            int exitCode = p.waitFor();
            if (exitCode != 0) {
                System.out.println("✗ Erro na compilação C:");
                System.out.println(compileOutput.toString());
                System.exit(1);
            }

            System.out.println("✓ Código compilado com sucesso!");
            System.out.println("Executável gerado: " + executableName);
            System.out.println();

            // RESUMO
            System.out.println("========================================");
            System.out.println("✓ COMPILAÇÃO CONCLUÍDA COM SUCESSO!");
            System.out.println("========================================");
            System.out.println("Entrada: " + inputFileName);
            System.out.println("Saída C: " + outputFileName);
            System.out.println("Executável: " + executableName);
            System.out.println("Variáveis: " + visitor.getSymbolTable().getAllSymbols().size());

        } catch (FileNotFoundException e) {
            System.err.println("✗ ERRO: Arquivo '" + inputFileName + "' não encontrado!");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("✗ ERRO: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("✗ ERRO: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
