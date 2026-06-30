import java.util.Objects;

public class Symbol {
    private String name;
    private int type;
    private String value;

    public static final int REAL = 0;
    public static final int INT = 1;

    public String generateCode() {
        StringBuilder code = new StringBuilder();
        if (type == REAL) {
            code.append("float ");
        } else if (type == INT) {
            code.append("int ");
        }
        code.append(name);
        code.append(" = ");
        code.append(value);
        code.append(";\n");
        return code.toString();
    }

    public Symbol(String name, int type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    // gatters e setters para os atributos name, type e value
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Symbol{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", value='" + value + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symbol symbol = (Symbol) o;
        return Objects.equals(name, symbol.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}