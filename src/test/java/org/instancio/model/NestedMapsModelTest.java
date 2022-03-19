package org.instancio.model;

import org.instancio.pojo.generics.NestedMaps;
import org.instancio.testsupport.fixtures.Types;
import org.instancio.testsupport.templates.ModelTestTemplate;
import org.instancio.testsupport.utils.NodeUtils;

import java.util.Map;

import static org.instancio.testsupport.asserts.NodeAssert.assertNode;

class NestedMapsModelTest extends ModelTestTemplate<NestedMaps<Long, String>> {

    @Override
    protected void verify(Node rootNode) {
        map1(rootNode);
        map2(rootNode);
    }

    private void map1(Node rootNode) {
        // Map<Long, Map<String, Boolean>> map1
        final String fieldName = "map1";

        final MapNode outerMap = assertNode(NodeUtils.getChildNode(rootNode, fieldName))
                .hasParent(rootNode)
                .hasFieldName(fieldName)
                .hasKlass(Map.class)
                .hasTypeMappedTo(Map.class, "K", Long.class)
                .hasTypeMappedTo(Map.class, "V", Types.MAP_STRING_BOOLEAN.get())
                .hasTypeMapWithSize(2)
                .hasNoChildren()
                .getAs(MapNode.class);

        assertNestedMap(outerMap);
    }

    private void map2(Node rootNode) {
        // Map<OKEY, Map<IKEY, Boolean>> map2
        final String fieldName = "map2";

        final MapNode outerMap = assertNode(NodeUtils.getChildNode(rootNode, fieldName))
                .hasParent(rootNode)
                .hasFieldName(fieldName)
                .hasKlass(Map.class)
                .hasTypeMappedTo(Map.class, "K", "OKEY")
                .hasTypeMappedTo(Map.class, "V", "java.util.Map<IKEY, java.lang.Boolean>")
                .hasTypeMapWithSize(2)
                .hasNoChildren()
                .getAs(MapNode.class);

        assertNestedMap(outerMap);
    }

    private void assertNestedMap(MapNode outerMap) {
        assertNode(outerMap.getKeyNode())
                .hasParent(outerMap)
                .hasNullField()
                .hasKlass(Long.class)
                .hasNoChildren();

        final MapNode innerMapNode = assertNode(outerMap.getValueNode())
                .hasParent(outerMap)
                .hasNullField()
                .hasKlass(Map.class)
                .hasNoChildren()
                .getAs(MapNode.class);

        assertNode(innerMapNode.getKeyNode())
                .hasParent(innerMapNode)
                .hasNullField()
                .hasKlass(String.class)
                .hasNoChildren();

        assertNode(innerMapNode.getValueNode())
                .hasParent(innerMapNode)
                .hasNullField()
                .hasKlass(Boolean.class)
                .hasNoChildren();
    }

}