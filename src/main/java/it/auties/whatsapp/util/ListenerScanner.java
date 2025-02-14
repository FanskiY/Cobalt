package it.auties.whatsapp.util;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.listener.Listener;
import it.auties.whatsapp.listener.RegisterListener;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.NoSuchElementException;

public final class ListenerScanner {
    private static final List<Class<?>> cache;

    static {
        cache = loadListeners();
    }

    private static List<Class<?>> loadListeners() {
        try (var scanner = createScanner()) {
            return scanner.getClassesWithAnnotation(RegisterListener.class).loadClasses();
        }
    }

    public static List<Listener> scan(Whatsapp whatsapp, boolean useCache) {
        var listeners = useCache ? cache : loadListeners();
        return listeners.stream()
                .map(listener -> initialize(listener, whatsapp))
                .toList();
    }

    private static ScanResult createScanner() {
        return new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .scan();
    }

    private static Listener initialize(Class<?> listener, Whatsapp whatsapp) {
        Validate.isTrue(Listener.class.isAssignableFrom(listener), "Cannot initialize listener at %s: cannot register classes that don't implement WhatsappListener", listener.getName(), IllegalArgumentException.class);
        try {
            return (Listener) listener.getConstructor(whatsapp == null ? new Class[0] : new Class[]{Whatsapp.class})
                    .newInstance(whatsapp == null ? new Object[0] : new Object[]{whatsapp});
        } catch (NoSuchMethodException noArgsConstructorException) {
            if (whatsapp != null) {
                return initialize(listener, null);
            }
            throw new NoSuchElementException("Cannot initialize listener at %s: no applicable constructor was found. Create a public no args constructor or a Whatsapp constructor".formatted(listener.getName()), noArgsConstructorException);
        } catch (IllegalAccessException accessException) {
            throw new IllegalArgumentException("Cannot initialize listener at %s: inaccessible module. Mark module %s as open in order to allow registration".formatted(listener.getName(), listener.getModule()
                    .getName()), accessException);
        } catch (InvocationTargetException invocationException) {
            throw new IllegalArgumentException("Cannot initialize listener at %s: an error occurred while initializing the class(check its static initializers)".formatted(listener.getName()), invocationException);
        } catch (InstantiationException instantiationException) {
            throw new IllegalArgumentException("Cannot initialize listener at %s: an error occurred while initializing the class(check its constructor)".formatted(listener.getName()), instantiationException);
        }
    }
}
