import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestRunner {
    private static final Path BASE_DIR = Paths.get("src", "testes").toAbsolutePath();
    private static final Map<String, String> GROUPS = new LinkedHashMap<>();

    static {
        GROUPS.put("lexico", "testeLexico");
        GROUPS.put("semantico", "testeSemantico");
        GROUPS.put("sintatico", "testeSintatico");
    }

    public static void main(String[] args) throws Exception {
        String tipo = null;
        Path arquivo = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--tipo".equals(arg) && i + 1 < args.length) {
                tipo = args[++i];
            } else if ("--arquivo".equals(arg) && i + 1 < args.length) {
                arquivo = Paths.get(args[++i]).toAbsolutePath();
            }
        }

        if (arquivo != null) {
            if (!Files.exists(arquivo)) {
                System.out.println("Arquivo não encontrado: " + arquivo);
                return;
            }
            String esperado = readExpectedOutput(arquivo);
            System.out.println("Caso: " + arquivo);
            System.out.println("Saída esperada: " + esperado);
            System.out.println("Status esperado: " + classify(esperado));
            return;
        }

        if (tipo != null) {
            runGroup(tipo);
        } else {
            for (String nomeGrupo : GROUPS.keySet()) {
                runGroup(nomeGrupo);
            }
        }
    }

    private static void runGroup(String groupName) {
        String folderName = GROUPS.get(groupName);
        if (folderName == null) {
            System.out.println("Grupo inválido: " + groupName);
            return;
        }

        Path groupDir = BASE_DIR.resolve(folderName);
        if (!Files.exists(groupDir)) {
            System.out.println("Pasta não encontrada: " + groupDir);
            return;
        }

        List<Path> testFiles;
        try (Stream<Path> stream = Files.walk(groupDir)) {
            testFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".gyh"))
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Erro ao listar testes de " + groupDir, e);
        }

        if (testFiles.isEmpty()) {
            System.out.println("Nenhum teste encontrado em " + groupDir);
            return;
        }

        System.out.println("===== " + groupName.toUpperCase(Locale.ROOT) + " =====");
        for (Path testFile : testFiles) {
            try {
                String expectedOutput = readExpectedOutput(testFile);
                String relPath = BASE_DIR.relativize(testFile).toString().replace('\\', '/');
                System.out.println("- " + relPath + ": " + expectedOutput);
                System.out.println("  Status esperado: " + classify(expectedOutput));
            } catch (IOException e) {
                System.out.println("Erro ao ler " + testFile + ": " + e.getMessage());
            }
        }
        System.out.println();
    }

    private static String readExpectedOutput(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String cleaned = line.trim();
                if (!cleaned.isEmpty()) {
                    return cleaned.startsWith("#") ? cleaned.substring(1).trim() : cleaned;
                }
            }
        }
        return "";
    }

    private static String classify(String expected) {
        String lowered = expected.toLowerCase(Locale.ROOT);
        List<String> semErroPatterns = Arrays.asList(
                "nao tem erro",
                "nao tem erro",
                "nao possui erro",
                "nao possui erro",
                "sem erro",
                "sem erros",
                "programa nao tem erro",
                "programa nao tem erro",
                "programa nao possui erro",
                "programa nao possui erro",
                "programa sem erro",
                "programa sem erros"
        );

        if (containsAny(lowered, semErroPatterns)) {
            return "sem erro";
        }

        if (containsAny(lowered, Arrays.asList("erro", "inesperado", "esperado", "falha", "desconhecido", "incompatibilidade", "overflow"))) {
            return "com erro";
        }

        return "sem erro";
    }

    private static boolean containsAny(String text, List<String> patterns) {
        for (String pattern : patterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}
