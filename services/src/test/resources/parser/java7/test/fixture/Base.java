package test.fixture;

public abstract class Base {
    protected final String label;

    protected Base(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    protected abstract static class Helper implements Comparable<Helper> {
        public int compareTo(Helper other) {
            return 0;
        }
    }
}
