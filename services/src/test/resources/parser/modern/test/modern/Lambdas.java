package test.modern;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Java 8-11: lambdas, method references, default/static interface methods, var.
 */
public class Lambdas implements Runnable {
    private final List<String> names = new ArrayList<>();

    interface Converter<F, T> {
        T convert(F from);

        default Converter<F, T> logged() {
            return from -> {
                System.out.println("convert " + from);
                return convert(from);
            };
        }

        static <T> Converter<T, T> identity() {
            return t -> t;
        }

        private void helper() {
        }
    }

    @Override
    public void run() {
        Runnable r = () -> System.out.println("run");
        r.run();

        Function<String, Integer> length = String::length;
        BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;
        BiFunction<Integer, Integer, Integer> max = (final Integer a, final Integer b) -> Math.max(a, b);
        Supplier<List<String>> factory = ArrayList::new;
        Function<Integer, String[]> arrays = String[]::new;

        names.sort(Comparator.comparing(String::length).thenComparing(Function.identity()));
        List<Integer> lengths = names.stream()
                .filter(n -> !n.isEmpty())
                .map(length)
                .collect(Collectors.toList());

        var total = add.apply(lengths.size(), max.apply(1, 2));
        for (var name : names) {
            System.out.println(name + total);
        }

        Optional.ofNullable(factory.get()).ifPresent(this::print);
        System.out.println(arrays.apply(3).length);
    }

    void print(List<String> values) {
        values.forEach(System.out::println);
    }
}
