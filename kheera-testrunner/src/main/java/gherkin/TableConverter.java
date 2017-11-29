package gherkin;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.jodamob.reflect.SuperReflect;
import gherkin.pickles.PickleCell;
import gherkin.pickles.PickleRow;
import gherkin.pickles.PickleTable;

public class TableConverter {

    public static <T> T convert(PickleTable dataTable, Type type, JsonElement testData) {

        if (type == null || (type instanceof Class && ((Class) type).isAssignableFrom(PickleTable.class))) {
            return (T) dataTable;
        }

        Type itemType = listItemType(type);
        if (itemType == null) {
            throw new ParserException("Not a List type: " + type);
        }

        Type mapKeyType = mapKeyType(itemType);
        if (mapKeyType != null) {
            Type mapValueType = mapValueType(itemType);
            return (T) toMaps(dataTable);
        }

        Type listitemType = listItemType(itemType);
        if (listitemType != null && ((Class)listitemType).isAssignableFrom(String.class)) {
            return (T) raw(dataTable);
        } else {
            if (((Class)itemType).isAssignableFrom(String.class)) {
                return (T) toStringList(dataTable, testData);
            } else {
                return (T) toList(dataTable, itemType, testData);
            }
        }
    }

    public static Type listItemType(Type type) {
        return typeArg(type, List.class, 0);
    }

    public static Type mapKeyType(Type type) {
        return typeArg(type, Map.class, 0);
    }

    public static Type mapValueType(Type type) {
        return typeArg(type, Map.class, 1);
    }

    static Type typeArg(Type type, Class<?> wantedRawType, int index) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class && wantedRawType.isAssignableFrom((Class) rawType)) {
                Type result = parameterizedType.getActualTypeArguments()[index];
                if (result instanceof TypeVariable) {
                    throw new ParserException("Generic types must be explicit");
                }
                return result;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    static <T> List<T> toList(PickleTable dataTable, Type itemType, JsonElement testData) {
        List<T> result = new ArrayList<T>();
        List<String> keys = convertTopCellsToFieldNames(raw(dataTable.getRows().get(0)));
        int count = dataTable.getRows().size();
        for (int i = 1; i < count; i++) {
            List<String> valueRow = raw(dataTable.getRows().get(i));
            T item = (T) SuperReflect.on((Class) itemType).create().get();
            int j = 0;
            for (String cell : valueRow) {
                SuperReflect.on(item).set(keys.get(j), cell);
                j++;
            }
            result.add(item);
        }
        return Collections.unmodifiableList(result);
    }

    static List<String> toStringList(PickleTable dataTable, JsonElement testData) {
        List<String> result = new ArrayList<String>();
        int count = dataTable.getRows().size();
        if (count == 1) {
            for (PickleCell cell : dataTable.getRows().get(0).getCells()) {
                result.add(replaceWithTestData(testData, cell.getValue()));
            }
        } else {
            for (PickleRow row : dataTable.getRows()) {
                result.add(replaceWithTestData(testData, row.getCells().get(0).getValue()));
            }
        }
        return Collections.unmodifiableList(result);
    }

    static List<Map<String, String>> toMaps(PickleTable dataTable) {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        List<String> keys = raw(dataTable.getRows().get(0));
        int count = dataTable.getRows().size();
        for (int i = 1; i < count; i++) {
            List<String> valueRow = raw(dataTable.getRows().get(i));
            Map<String, String> map = new LinkedHashMap<String, String>();
            int j = 0;
            for (String cell : valueRow) {
                map.put(keys.get(j), cell);
                j++;
            }
            result.add(Collections.unmodifiableMap(map));
        }
        return Collections.unmodifiableList(result);
    }

    private static List<List<String>> raw(PickleTable dataTable) {

        int columns = dataTable.getRows().isEmpty() ? 0 : dataTable.getRows().get(0).getCells().size();
        List<List<String>> raw = new ArrayList<List<String>>();
        for (PickleRow row : dataTable.getRows()) {
            if (columns != row.getCells().size()) {
                throw new ParserException(String.format("Table is unbalanced: expected %s column(s) but found %s.", columns, row.getCells().size()));
            }
            raw.add(Collections.unmodifiableList(raw(row)));
        }
        return Collections.unmodifiableList(raw);
    }

    private static List<String> raw(PickleRow row) {

        List<String> raw = new ArrayList<String>();
        for (PickleCell cell : row.getCells()) {
            raw.add(cell.getValue());
        }
        return Collections.unmodifiableList(raw);
    }

    static List<String> convertTopCellsToFieldNames(List<String> row) {
        final CamelCaseStringConverter mapper = new CamelCaseStringConverter();

        List<String> result = new ArrayList<>(row.size());
        for (String s : row) {
            result.add(mapper.map(s));
        }
        return result;
    }

    private static String replaceWithTestData(final JsonElement sourceTestData, String inputString) {
        if(sourceTestData != null) {
                if(inputString != null) {
                    if(inputString.startsWith("$(") && inputString.endsWith(")")) {
                        inputString = inputString.substring("$(".length(), inputString.length() - ")".length());

                        String[] pathElements = inputString.split("\\.");
                        if(pathElements != null && pathElements.length > 0) {
                            JsonElement elemPointer = sourceTestData;
                            for(String element : pathElements) {
                                if(elemPointer.isJsonObject() && elemPointer.getAsJsonObject().has(element)) {
                                    elemPointer = elemPointer.getAsJsonObject().get(element);
                                    continue;
                                }
                                else {
                                    elemPointer = null;
                                    break;
                                }
                            }
                            if(elemPointer != null && elemPointer.isJsonPrimitive()) {
                                JsonPrimitive primitive = elemPointer.getAsJsonPrimitive();
                                if(primitive.isString() || primitive.isNumber())
                                    return primitive.getAsString();
                                else if(primitive.isBoolean())
                                    return primitive.getAsBoolean()?"true":"false";
                            }
                        }
                    }
                }
            }
            return inputString;
        }
}