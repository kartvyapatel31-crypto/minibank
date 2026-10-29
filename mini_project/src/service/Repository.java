package service;

import model.annotation.Id;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Generic repository implementation providing reusable, type-safe data access.
 * Practical 13 Requirements:
 * 1. Generic class holding a HashMap<String, T> with save, findById, findAll, delete.
 * 2. Reflective lookup of the field marked @Id to extract the key.
 * 3. @SafeVarargs saveAll(T... items) method.
 * 4. Bounded generic method: static <N extends Number> double average(N[] values).
 *
 * @param <T> entity type stored in this repository
 */
public class Repository<T> {

    private final HashMap<String, T> store = new HashMap<>();

    /**
     * Saves an entity by reading its @Id annotated field via reflection.
     * Traverses the class hierarchy up to Object to find inherited @Id fields.
     *
     * @param entity the entity to persist
     * @return the saved entity
     * @throws IllegalArgumentException if entity is null, has no @Id field, or id value is null
     */
    public synchronized T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Cannot save null entity.");
        }

        Field idField = findIdField(entity.getClass());
        if (idField == null) {
            throw new IllegalArgumentException("No field annotated with @Id found in " + entity.getClass().getName());
        }

        idField.setAccessible(true);
        try {
            Object idValue = idField.get(entity);
            if (idValue == null) {
                throw new IllegalArgumentException("Entity ID value cannot be null.");
            }
            String idKey = String.valueOf(idValue);
            store.put(idKey, entity);
            return entity;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access @Id field in " + entity.getClass().getName(), e);
        }
    }

    /**
     * Finds an entity by its identifier.
     *
     * @param id key identifier
     * @return the entity, or null if not found
     */
    public synchronized T findById(String id) {
        if (id == null) {
            return null;
        }
        return store.get(id);
    }

    /**
     * Retrieves all entities currently in the repository.
     *
     * @return a new ArrayList containing all entities
     */
    public synchronized ArrayList<T> findAll() {
        return new ArrayList<>(store.values());
    }

    /**
     * Deletes an entity by its identifier.
     *
     * @param id key identifier
     * @return true if an entity was removed, false otherwise
     */
    public synchronized boolean delete(String id) {
        if (id == null) {
            return false;
        }
        return store.remove(id) != null;
    }

    /**
     * Saves multiple entities at once.
     *
     * @param items varargs array of entities
     * @return an ArrayList of saved entities
     */
    @SafeVarargs
    public final synchronized ArrayList<T> saveAll(T... items) {
        ArrayList<T> result = new ArrayList<>();
        if (items != null) {
            for (T item : items) {
                if (item != null) {
                    result.add(save(item));
                }
            }
        }
        return result;
    }

    /**
     * Bounded generic method to compute the average of an array of numbers.
     *
     * @param <N> any type extending Number (Double, Integer, Long, Float, etc.)
     * @param values array of numbers
     * @return the arithmetic mean, or 0.0 if empty or null
     */
    public static <N extends Number> double average(N[] values) {
        if (values == null || values.length == 0) {
            return 0.0;
        }
        double sum = 0.0;
        int count = 0;
        for (N val : values) {
            if (val != null) {
                sum += val.doubleValue();
                count++;
            }
        }
        return count == 0 ? 0.0 : (sum / count);
    }

    /**
     * Overloaded bounded generic method for collections.
     */
    public static <N extends Number> double average(Iterable<N> values) {
        if (values == null) {
            return 0.0;
        }
        double sum = 0.0;
        int count = 0;
        for (N val : values) {
            if (val != null) {
                sum += val.doubleValue();
                count++;
            }
        }
        return count == 0 ? 0.0 : (sum / count);
    }

    /**
     * Checks if an entity with the given id exists.
     */
    public synchronized boolean existsById(String id) {
        return id != null && store.containsKey(id);
    }

    /**
     * Returns the total count of stored entities.
     */
    public synchronized int size() {
        return store.size();
    }

    /**
     * Clears all entities from the repository.
     */
    public synchronized void clear() {
        store.clear();
    }

    /**
     * Helper to find the field annotated with @Id across the class hierarchy.
     */
    private Field findIdField(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }
}

