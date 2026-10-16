package test.modern;

/**
 * Java 17: sealed classes and interfaces.
 */
public sealed interface Shapes permits Shapes.Circle, Shapes.Square, Shapes.Polygon {
    double area();

    final class Circle implements Shapes {
        private final double radius;

        public Circle(double radius) {
            this.radius = radius;
        }

        @Override
        public double area() {
            return Math.PI * radius * radius;
        }
    }

    record Square(double side) implements Shapes {
        @Override
        public double area() {
            return side * side;
        }
    }

    non-sealed class Polygon implements Shapes {
        @Override
        public double area() {
            return 0;
        }
    }

    sealed abstract class Base permits Derived {
    }

    final class Derived extends Base {
    }
}
