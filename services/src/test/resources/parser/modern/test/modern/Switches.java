package test.modern;

/**
 * Java 14-21: switch expressions, yield, text blocks, pattern matching.
 */
public class Switches {
    enum Day { MONDAY, TUESDAY, SATURDAY, SUNDAY }

    sealed interface Expr permits Num, Add, Neg {}
    record Num(int value) implements Expr {}
    record Add(Expr left, Expr right) implements Expr {}
    record Neg(Expr operand) implements Expr {}

    static final String JSON = """
            {
              "name": "jd-gui",
              "escaped": "\""",
              "version": %d, \
              "continued": true
            }
            """;

    int letters(Day day) {
        return switch (day) {
            case MONDAY, SUNDAY -> 6;
            case TUESDAY -> 7;
            default -> {
                int length = day.name().length();
                yield length;
            }
        };
    }

    String classic(int code) {
        String result = switch (code) {
            case 1:
                yield "one";
            case 2:
                yield "two";
            default:
                yield "many";
        };
        return result;
    }

    String describe(Object o) {
        if (o instanceof String s && !s.isEmpty()) {
            return "string " + s.length();
        }
        if (!(o instanceof Number n)) {
            return "other";
        }
        return "number " + n.intValue();
    }

    int eval(Expr expr) {
        return switch (expr) {
            case Num n -> n.value();
            case Add(Expr left, Expr right) -> eval(left) + eval(right);
            case Neg(Num(var value)) when value == 0 -> 0;
            case Neg neg -> -eval(neg.operand());
        };
    }

    String format(Object o) {
        return switch (o) {
            case null -> "null";
            case Integer i when i > 10 -> "big " + i;
            case Integer i -> "int " + i;
            case String s -> String.format(JSON, s.length());
            default -> o.toString();
        };
    }
}
