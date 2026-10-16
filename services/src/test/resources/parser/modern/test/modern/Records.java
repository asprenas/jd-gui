package test.modern;

import java.util.List;
import java.util.Objects;

/**
 * Java 16: records.
 */
public class Records {
    public record Point(int x, int y) implements Comparable<Point> {
        public static final Point ORIGIN = new Point(0, 0);

        public Point {
            if (x < 0 || y < 0) {
                throw new IllegalArgumentException("negative");
            }
        }

        public Point(int value) {
            this(value, value);
        }

        public double distance(Point other) {
            int dx = x - other.x();
            int dy = y - other.y();
            return Math.sqrt(dx * dx + dy * dy);
        }

        @Override
        public int compareTo(Point other) {
            return Integer.compare(x * x + y * y, other.x * other.x + other.y * other.y);
        }

        static Point of(int x, int y) {
            return new Point(x, y);
        }
    }

    record Pair<A, B>(A first, B second) {
        Pair {
            Objects.requireNonNull(first);
        }
    }

    record Names(String... values) {
    }

    List<Point> points() {
        record Local(int index, Point point) {
        }
        Local local = new Local(0, Point.ORIGIN);
        return List.of(local.point(), Point.of(1, 2), new Point(3));
    }
}
