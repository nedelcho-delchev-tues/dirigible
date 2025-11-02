package org.eclipse.dirigible.components.engine.di.parser;

import java.util.HashMap;
import java.util.Map;

public class ComponentMetadata {
    private String componentName;
    private String className;
    private Map<String, String> propertyTypes = new HashMap<>();
    private String key;

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public void addPropertyType(String property, String type) {
        propertyTypes.put(property, type);
    }

    public Map<String, String> getPropertyTypes() {
        return propertyTypes;
    }

    // Generate the key dynamically
    public String getKey() {
        if (key == null) {
            key = className;
        }
        return key;
    }
}


