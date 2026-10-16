package test.fixture;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Java 7 parser fixture: classes, generics, members, inner types and expressions.
 */
public class Sample<T extends Comparable<T>> extends Base implements Runnable, Shape {
    public static final int MAX = 10;
    private static final String NAME = "sample";
    protected int[] values = new int[MAX];
    private String[][] matrix;
    private List<String> names = new ArrayList<>();
    private Map<String, List<Integer>> index = new HashMap<String, List<Integer>>();
    T current;
    volatile boolean running, stopped;

    static {
        System.out.println("static init");
    }

    {
        names.add("instance init");
    }

    public Sample() {
        this(0);
    }

    public Sample(int size) {
        super("base");
        values = new int[size];
    }

    @Override
    public void run() {
        running = true;
        for (int i = 0; i < values.length; i++) {
            values[i] = i * 2;
        }
        for (String name : names) {
            System.out.println(name.length());
        }
    }

    @Override
    public double area() {
        return Math.PI * MAX;
    }

    public static int sum(int... numbers) {
        int total = 0;
        for (int n : numbers) {
            total += n;
        }
        return total;
    }

    protected synchronized T getCurrent() {
        return current;
    }

    public <E extends Number> List<E> filter(List<E> input, E min) {
        List<E> result = new ArrayList<E>();
        for (E e : input) {
            if (e.doubleValue() >= min.doubleValue()) {
                result.add(e);
            }
        }
        return result;
    }

    String[] split(String text, char separator) throws IOException {
        if (text == null) {
            throw new IllegalArgumentException("text is null");
        }
        Object o = text;
        if (o instanceof String) {
            String s = (String) o;
            return s.split(String.valueOf(separator));
        }
        return new String[0];
    }

    long read(InputStream input) {
        long count = 0;
        try (InputStream in = input) {
            while (in.read() != -1) {
                count++;
            }
        } catch (IOException | RuntimeException e) {
            count = -1;
        } finally {
            stopped = true;
        }
        return count;
    }

    String describe(Color color) {
        switch (color) {
            case RED:
                return "red";
            case GREEN:
                return "green";
            default:
                return "other: " + color.name();
        }
    }

    Runnable task() {
        return new Runnable() {
            @Override
            public void run() {
                System.out.println(NAME + current);
            }
        };
    }

    int ternary(int a, int b) {
        int c = a > b ? a : b;
        c <<= 2;
        c = ~c & 0xFF | 07 ^ 0b101;
        return c >>> 1;
    }

    public static class Nested {
        int nestedValue = 'x';

        Nested copy() {
            return new Nested();
        }
    }

    class Inner extends Base {
        Inner() {
            super("inner");
        }

        T value() {
            return Sample.this.current;
        }
    }

    enum Color {
        RED, GREEN("g"), BLUE {
            @Override
            String code() {
                return "b";
            }
        };

        private final String label;

        Color() {
            this("none");
        }

        Color(String label) {
            this.label = label;
        }

        String code() {
            return label;
        }
    }

    @interface Marker {
        String value() default "";
        int priority() default 1;
        Class<?>[] types() default {};
    }

    interface Callback<R> {
        int TIMEOUT = 30;

        R call(String argument) throws Exception;
    }
}
