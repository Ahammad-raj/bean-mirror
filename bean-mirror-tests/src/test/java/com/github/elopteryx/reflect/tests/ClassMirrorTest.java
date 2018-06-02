package com.github.elopteryx.reflect.tests;

import com.github.elopteryx.reflect.BeanMirror;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClassMirrorTest {

    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    private static class ForCreate {}

    @SuppressWarnings("unused")
    private static class ForCreateWithParams {
        private final String value;

        public ForCreateWithParams(String value) {
            this.value = value;
        }
    }

    @Test
    void create() {
        final var mirror = BeanMirror.of(ForCreate.class, lookup);
        assertNotNull(mirror.create().get());

        final var mirrorForParams = BeanMirror.of(ForCreateWithParams.class, lookup);
        assertNotNull(mirrorForParams.create("").get());
    }

    @SuppressWarnings("unused")
    private static class Parent {
        private static int i = 3;
    }

    @SuppressWarnings("unused")
    private static class Child extends Parent {
        private static int i = 4;
    }

    @Test
    void getStatic() {
        assertEquals(4, (int)BeanMirror.of(Child.class, lookup).getStatic("i", int.class));
    }

    @SuppressWarnings("unused")
    private static class GetField {
        private static String a = "a";
    }

    @Test
    void getStaticField() {
        assertNotNull(BeanMirror.of(GetField.class, lookup).getStatic("a", String.class));
    }

    @SuppressWarnings("unused")
    private static class SetField {
        private static String a = "a";
    }

    @Test
    void setStaticField() {
        final var mirror = BeanMirror.of(SetField.class, lookup);
        final var mirrorAfterSet = mirror.setStatic("a", "b");
        assertEquals(mirror, mirrorAfterSet);
    }

    @SuppressWarnings("unused")
    private static class FieldTarget {
        private static FieldTarget inner = new FieldTarget();
    }

    @Test
    void staticField() {
        final var mirror = BeanMirror.of(FieldTarget.class, lookup);
        final var fieldMirror = mirror.staticField("inner", FieldTarget.class);
        assertEquals(fieldMirror.get(), FieldTarget.inner);
    }

    @SuppressWarnings("unused")
    private static class GetterSetterTarget {
        private String value;

        private GetterSetterTarget() {
            this("");
        }

        private GetterSetterTarget(String value) {
            this.value = value;
        }

        private String getValue() {
            return value;
        }
    }

    @Test
    void createGetter() {
        final var getter = BeanMirror.of(GetterSetterTarget.class, lookup).createGetter("value", String.class);
        assertAll(
                () -> assertEquals(getter.apply(new GetterSetterTarget()), ""),
                () -> assertEquals(getter.apply(new GetterSetterTarget("a")), "a"),
                () -> assertEquals(getter.apply(new GetterSetterTarget("b")), "b")
        );
    }

    @Test
    void createSetter() {
        final var target = new GetterSetterTarget();
        final var setter = BeanMirror.of(GetterSetterTarget.class, lookup).createSetter("value", String.class);
        assertAll(
                () -> assertEquals("", target.getValue()),
                () -> {
                    setter.accept(target, "a");
                    assertEquals("a", target.getValue());
                },
                () -> {
                    setter.accept(target, "b");
                    assertEquals("b", target.getValue());
                },
                () -> {
                    setter.accept(target, "c");
                    assertEquals("c", target.getValue());
                }
        );
    }

    @Test
    void createGetterAndSetter() {
        final var target = new GetterSetterTarget();
        final var mirror = BeanMirror.of(target, lookup);
        final var getter = mirror.createGetter("value", String.class);
        final var setter = mirror.createSetter("value", String.class);
        assertAll(
                () -> assertEquals("", target.getValue()),
                () -> {
                    setter.accept(target, "a");
                    assertEquals("a", target.getValue());
                    assertEquals("a", getter.apply(target));
                },
                () -> {
                    setter.accept(target, "b");
                    assertEquals("b", target.getValue());
                    assertEquals("b", getter.apply(target));
                },
                () -> {
                    setter.accept(target, "c");
                    assertEquals("c", target.getValue());
                    assertEquals("c", getter.apply(target));
                }
        );
    }

    @SuppressWarnings("unused")
    private static class GetterSetterTargetStatic {
        private static String value;

        private static void init() {
            GetterSetterTargetStatic.value = "";
        }
    }

    @Test
    void createStaticGetterAndSetter() {
        final var mirror = BeanMirror.of(GetterSetterTargetStatic.class, lookup);
        final var getter = mirror.createStaticGetter("value", String.class);
        final var setter = mirror.createStaticSetter("value", String.class);
        assertAll(
                () -> {
                    GetterSetterTargetStatic.init();
                    assertEquals("", GetterSetterTargetStatic.value);
                    assertEquals("", getter.get());
                },
                () -> {
                    setter.accept("a");
                    assertEquals("a", GetterSetterTargetStatic.value);
                    assertEquals("a", getter.get());
                },
                () -> {
                    setter.accept("b");
                    assertEquals("b", GetterSetterTargetStatic.value);
                    assertEquals("b", getter.get());
                },
                () -> {
                    setter.accept("c");
                    assertEquals("c", GetterSetterTargetStatic.value);
                    assertEquals("c", getter.get());
                }
        );
    }

    @SuppressWarnings("unused")
    private static class RunTarget {

        public static void run(String param) {
            Objects.requireNonNull(param);
        }
    }

    @Test
    void runStatic() {
        final var mirror = BeanMirror.of(RunTarget.class, lookup);
        mirror.runStatic("run", "arg");
        final var exception = assertThrows(RuntimeException.class, () -> mirror.runStatic("run", (String) null));
        assertEquals(exception.getCause().getClass(), NullPointerException.class);
    }

    @SuppressWarnings("unused")
    private static class CallTarget {

        private static String call() {
            return "callable";
        }
    }

    @Test
    void callStatic() {
        final var mirror = BeanMirror.of(CallTarget.class, lookup);
        assertEquals(mirror.callStatic(String.class, "call").get(), "callable");
    }

    private static class StandardObjectMethods {}

    @Test
    void forHashCode() {
        final var mirror = BeanMirror.of(StandardObjectMethods.class, lookup);
        assertEquals(mirror.hashCode(), StandardObjectMethods.class.hashCode());
    }

    @Test
    void forEquals() {
        final var mirror = BeanMirror.of(StandardObjectMethods.class, lookup);
        final var otherMirror = BeanMirror.of(StandardObjectMethods.class, lookup);
        assertEquals(mirror, otherMirror);
    }

    @Test
    void forToString() {
        final var mirror = BeanMirror.of(StandardObjectMethods.class, lookup);
        assertEquals(mirror.toString(), StandardObjectMethods.class.toString());
    }
}