/**
 * Java 9 module declaration.
 */
module test.modern {
    requires java.base;
    requires transitive java.desktop;
    requires static java.sql;
    exports test.modern;
    opens test.modern to java.desktop;
    uses java.sql.Driver;
    provides java.lang.Runnable with test.modern.Lambdas;
}
