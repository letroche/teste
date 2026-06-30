import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

public class main {
    private static String preprocessInput(String inputFileName) throws IOException {
        String content = Files.readString(Paths.get(inputFileName), StandardCharsets.UTF_8);
        return content.replaceAll("(\\d)\\s*\\.\\s*(\\d)", "$1.$2");
    }

    private static String formatUserMessage(String message) {
        if (message == null) {
            return "";
        }

        String formatted = message;
        formatted = formatted.replace("token recognition error at", "token irreconhecivel");
        formatted = formatted.replace("token recognition error", "token irreconhecivel");
        formatted = formatted.replace("Erro Léxico", "Erro lexico");
        formatted = formatted.replace("Erro Lexico", "Erro lexico");
        formatted = formatted.replace("no viable alternative", "nenhuma alternativa valida");
        formatted = formatted.replace("mismatched input", "entrada inesperada");
        formatted = formatted.replace("expecting", "esperando");
        formatted = formatted.replace("missing", "faltando");
        return formatted;
    }

    private static String checkForInvalidWord(String content) {
        String[] lines = content.split("\\R", -1);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            String[] words = line.split("[^A-Za-z]+", -1);
            for (String word : words) {
                if (word.isEmpty()) {
                    continue;
                }

                String normalized = word.toUpperCase(Locale.ROOT);
                if ("ENTAO".equals(normalized)) {
                    continue;
                }

                int distance = levenshteinDistance(normalized, "ENTAO");
                if (normalized.length() >= 4 && normalized.length() <= 6 && distance <= 1) {
                    int index = line.toUpperCase(Locale.ROOT).indexOf(normalized);
                    if (index >= 0) {
                        return "Erro lexico na linha " + (i + 1) + ", posicao " + (index + 1) + ": palavra '" + word + "' nao e reconhecida. Esperava 'ENTAO'.";
                    }
                }
            }
        }

        return null;
    }

    private static int levenshteinDistance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost);
            }

            int[] temp = previous;
            previous = current;
            current = temp;
        }

        return previous[b.length()];
    }
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
            String normalizedInput = preprocessInput(inputFileName);
            String lexicalError = checkForInvalidWord(normalizedInput);
            if (lexicalError != null) {
                System.out.println(formatUserMessage(lexicalError));
                System.out.println("\nCompilacao falhou!");
                System.exit(1);
            }

            CharStream cs = CharStreams.fromString(normalizedInput);
            GyhGrammarLexer lexer = new GyhGrammarLexer(cs);
            
            // Criar um listener customizado para erros léxicos
            LexerErrorListener lexerErrorListener = new LexerErrorListener();
            lexer.removeErrorListeners();
            lexer.addErrorListener(lexerErrorListener);

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            tokens.fill();

            if (lexerErrorListener.hasErrors()) {
                System.out.println("Erros encontrados:");
                for (String error : lexerErrorListener.getErrors()) {
                    System.out.println("  - " + formatUserMessage(error));
                }
                System.out.println("\nCompilacao falhou!");
                System.exit(1);
            }

            System.out.println("Tokens encontrados: " + tokens.getNumberOfOnChannelTokens());
            System.out.println("Analise lexica concluida com sucesso!");
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

            if (parserErrorListener.hasErrors()) {
                System.out.println("Erros encontrados:");
                for (String error : parserErrorListener.getErrors()) {
                    System.out.println("  - " + formatUserMessage(error));
                }
                System.out.println("\nCompilacao falhou!");
                System.exit(1);
            }
            
            System.out.println("Analise sintatica concluida com sucesso!");
            System.out.println();

            // ANÁLISE SEMÂNTICA E GERAÇÃO DE CÓDIGO
            System.out.println("[3] ANÁLISE SEMÂNTICA E GERAÇÃO DE CÓDIGO");
            System.out.println("-----------------------------------------");
            GyhCompilerVisitor visitor = new GyhCompilerVisitor();
            ParseTreeWalker.DEFAULT.walk(visitor, tree);

            // Verificar erros
            if (visitor.hasErrors()) {
                System.out.println("Erros encontrados:");
                for (String error : visitor.getErrors()) {
                    System.out.println("  - " + formatUserMessage(error));
                }
                System.out.println("\nCompilacao falhou!");
                System.exit(1);
            }

            System.out.println("Analise semantica concluida com sucesso!");
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
                System.out.println("Erro na compilacao C:");
                System.out.println(compileOutput.toString());
                System.exit(1);
            }

            System.out.println("Codigo compilado com sucesso!");
            System.out.println("Executável gerado: " + executableName);
            System.out.println();

            // RESUMO
            System.out.println("========================================");
            System.out.println("Compilacao concluida com sucesso!");
            System.out.println("========================================");
            System.out.println("Entrada: " + inputFileName);
            System.out.println("Saída C: " + outputFileName);
            System.out.println("Executável: " + executableName);
            System.out.println("Variáveis: " + visitor.getSymbolTable().getAllSymbols().size());

        } catch (FileNotFoundException e) {
            System.err.println("Erro: arquivo '" + inputFileName + "' nao encontrado!");
            System.exit(1);
        } catch (IOException e) {
            System.err.println(" ERRO: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
