package experimental.reflection.nodes;

import org.instancio.util.ReflectionUtils;
import org.instancio.util.Verify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.stream.Collectors.joining;

public class FieldNode {
    private static final Logger LOG = LoggerFactory.getLogger(FieldNode.class);

    private static final String JAVA_PKG_PREFIX = "java";

    private final Field field;

    /**
     * Contains actual type of the field.
     * For generic classes {@code field.getType()} returns Object.
     */
    private final Class<?> actualFieldType;
    private final String[] typeVariables;
    private List<FieldNode> children = null;
    private final Map<String, Type> typeMap;
    private final Map<String, Class<?>> rootTypeMap;
    private final FieldNode parentFieldNode;  // TODO delete

    private final Class<?> classDeclaringTheTypeVariable; // e.g. List<E> => List.class (declares E)
    private final Type genericType;

    private final List<PType> nestedTypes = new ArrayList<>();
    private final Set<FieldNode> visited;

    public FieldNode(Field field, Class<?> classDeclaringTheTypeVariable, Type genericType, FieldNode parentFieldNode,
                     Map<String, Class<?>> rootTypeMap, Set<FieldNode> visited) {

        this.field = Verify.notNull(field, "Field must not be null");

        if (classDeclaringTheTypeVariable == null) {
            this.actualFieldType = field.getType();
        } else {
            LOG.debug("Setting field node for '{}' to '{}'", field.getName(), classDeclaringTheTypeVariable);
            this.actualFieldType = classDeclaringTheTypeVariable;
        }

        this.typeVariables = getTypeVariables(field);
        this.classDeclaringTheTypeVariable = classDeclaringTheTypeVariable;
        this.genericType = genericType;
        this.parentFieldNode = parentFieldNode;
        this.rootTypeMap = rootTypeMap;
        this.typeMap = getTypeMap(classDeclaringTheTypeVariable, genericType);
        this.visited = visited;
    }

    public FieldNode(Field field, Map<String, Class<?>> rootTypeMap) {
        this(field, field.getType(), field.getGenericType(), /* parent = */ null, rootTypeMap, new HashSet<>());
    }

    private List<FieldNode> makeChildren() {
        final Package fieldPackage = actualFieldType.getPackage();
        if (fieldPackage == null || fieldPackage.getName().startsWith(JAVA_PKG_PREFIX)) {
            return Collections.emptyList(); // Exclude JDK classes
        }

        final Field[] childFields = actualFieldType.getDeclaredFields();
        final List<FieldNode> childNodes = new ArrayList<>();

        for (Field childField : childFields) {
            final String typeParameter = childField.getGenericType().getTypeName();

            Class<?> resolvedTypeArg = null;
            Type typeArgument = typeMap.get(typeParameter);

            if (typeArgument instanceof TypeVariable) {
                resolvedTypeArg = rootTypeMap.get(((TypeVariable<?>) typeArgument).getName());
            } else if (typeArgument instanceof Class) {
                resolvedTypeArg = (Class<?>) typeArgument;
            }

            final Optional<Type> classParameterizedTypePType = nestedTypes.stream()
                    .filter(pType -> pType.getRawType().equals(typeArgument)).findAny()
                    .map(PType::getParameterizedType);

            Type genericType = classParameterizedTypePType.orElse(childField.getGenericType());
            FieldNode childNode = new FieldNode(childField, resolvedTypeArg, genericType, this, rootTypeMap, visited);
            if (!visited.contains(childNode)) {
                childNodes.add(childNode);
                visited.add(childNode);
            }
        }

        return childNodes;
    }

    public Field getField() {
        return field;
    }

    public String getFieldName() {
        return field.getName();
    }

    public Class<?> getActualFieldType() {
        return actualFieldType;
    }

    public List<FieldNode> getChildren() {
        if (children == null) {
            children = makeChildren();
        }
        return children;
    }

    /**
     * Returns type name of the declared field, e.g.
     *
     * <pre>{@code
     *   T someField;          // => T
     *   PType<L,R> PType;     // => org.example.PType<L, R>
     *   String str;           // => java.lang.String
     *   List<Phone> numbers;  // => java.util.List<org.example.Phone>
     * }</pre>
     */
    public String getTypeName() {
        return field.getGenericType().getTypeName();
    }

    public Map<String, Type> getTypeMap() {
        return typeMap;
    }

    public FieldNode getChildByTypeParameter(final String typeParameter) {
        return getChild(n -> Objects.equals(typeParameter, n.getTypeName()))
                .orElseThrow(() -> new NoSuchElementException("No child with type parameter: " + typeParameter));
    }

    public FieldNode getChildByFieldName(String name) {
        return getChild(n -> n.getFieldName().equals(name))
                .orElseThrow(() -> new NoSuchElementException("Field with name '" + name + "' not found"));
    }

    private Optional<FieldNode> getChild(final Predicate<FieldNode> predicate) {
        return children.stream().filter(predicate).findAny();
    }

    private static String[] getTypeVariables(Field field) {
        TypeVariable<?>[] typeParameters = field.getType().getTypeParameters();
        String[] typeVariables = new String[typeParameters.length];

        for (int i = 0; i < typeParameters.length; i++) {
            typeVariables[i] = typeParameters[i].getName();
        }
        return typeVariables;
    }

    private Map<String, Type> getTypeMap(Class<?> declaringClass, final Type genericType) {
        if (declaringClass == null) {
            return Collections.emptyMap();
        }

        final Map<String, Type> map = new HashMap<>();
        final TypeVariable<?>[] typeVars = declaringClass.getTypeParameters();

        if (genericType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) genericType;
            Type[] typeArgs = pType.getActualTypeArguments();

//            LOG.debug("pType: {}", pType);
//            LOG.debug("actualTypeArguments: {}", Arrays.toString(typeArgs));

            for (int i = 0; i < typeArgs.length; i++) {
                TypeVariable<?> tvar = typeVars[i];
                Type actualType = typeArgs[i];

                //LOG.debug(" --> tvar: {}, actualType: {}", tvar, actualType);

                if (actualType instanceof TypeVariable) {
                    map.put(tvar.getName(), actualType);
                } else if (actualType instanceof ParameterizedType) {
                    Class<?> c = (Class<?>) ((ParameterizedType) actualType).getRawType();
                    ParameterizedType nestedPType = (ParameterizedType) actualType;

                    map.put(tvar.getName(), c);

                    nestedTypes.add(new PType(c, nestedPType));
                } else if (actualType instanceof Class) {
                    map.put(tvar.getName(), actualType);
                } else {
                    throw new IllegalStateException("Unhandled type: " + actualType);
                }
            }
        } else {
            LOG.debug("No generic info for declaringClass: {}, genericType: {}", declaringClass, genericType);
        }

        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FieldNode)) return false;
        FieldNode fieldNode = (FieldNode) o;
        return field.equals(fieldNode.field) && actualFieldType.equals(fieldNode.actualFieldType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, actualFieldType);
    }

    @Override
    public String toString() {
        String s = "";
        s += "Field: " + field.getName() + " " + field.getType().getSimpleName() + ", actual type: " + actualFieldType.getSimpleName() + "\n"
                + " -> typeVars: " + Arrays.toString(typeVariables) + "\n"
                + " -> pTypes: " + ReflectionUtils.getParameterizedTypes(field) + "\n"
                + " -> typeName: " + getTypeName() + "\n"
                + " -> typeMap: " + typeMap + "\n"
                + " -> nestedTypes: " + nestedTypes + "\n"
                + " -> classDeclaringTheTypeVariable: " + classDeclaringTheTypeVariable + "\n";

        if (getChildren() != null)
            s += " -> children: " + getChildren().stream().map(f -> f.field.getName()).collect(joining(",")) + "\n";

        return s;
    }
}
