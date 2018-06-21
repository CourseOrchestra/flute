package ru.curs.flute.task;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TaskUnit {

    private final String qualifier;
    private final Type type;

    public TaskUnit(String qualifier, Type type) {
        this.qualifier = qualifier;
        this.type = type;
    }

    public String getQualifier() {
        return this.qualifier;
    }

    public Type getType() {
        return type;
    }

    public static TaskUnit fromJson(JsonObject o) {
        List<TaskUnit> taskUnits = Arrays.stream(Type.values())
                .map(Enum::name)
                .map(String::toLowerCase)
                .filter(el -> o.get(el) != null)
                .map(el -> new TaskUnit(o.get(el).getAsString(), Type.valueOf(el.toUpperCase())))
                .collect(Collectors.toList());

        if (taskUnits.isEmpty())
            throw new RuntimeException(String.format("No script/proc value found in json '%s'", o.toString()));
        if (taskUnits.size() > 1)
            throw new RuntimeException(String.format("Multiple script/proc values found in json '%s'", o.toString()));

        return taskUnits.get(0);
    }

    public enum Type {
        SCRIPT,
        PROC
    }
}
