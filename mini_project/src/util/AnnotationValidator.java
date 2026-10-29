package util;

import model.annotation.Positive;
import model.annotation.MaxLength;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class AnnotationValidator {

    public static String[] validate(Object obj) {
        List<String> errors = new ArrayList<>();
        if (obj == null) return new String[0];

        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    Object value = field.get(obj);
                    if (field.isAnnotationPresent(Positive.class)) {
                        if (value instanceof Number) {
                            double num = ((Number) value).doubleValue();
                            if (num <= 0) {
                                Positive pos = field.getAnnotation(Positive.class);
                                errors.add(field.getName() + " " + pos.message());
                            }
                        }
                    }
                    if (field.isAnnotationPresent(MaxLength.class)) {
                        if (value instanceof String) {
                            String str = (String) value;
                            MaxLength maxLen = field.getAnnotation(MaxLength.class);
                            if (str.length() > maxLen.value()) {
                                errors.add(field.getName() + " length must be <= " + maxLen.value());
                            }
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            clazz = clazz.getSuperclass();
        }

        return errors.toArray(new String[0]);
    }
}
