package org.eclipse.dirigible.components.data.store;

import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.utils.xml2json.Xml2Json2;

public class EntityTransformer {

    public static String toEntity(String xml) throws Exception {
        xml = xml.replace("hibernate-mapping>", "entity-mapping>");
        return Xml2Json2.toJson(xml);
    }

    public static String fromEntity(String json) throws Exception {
        json = json.replace("entity-mapping", "hibernate-mapping");
        return Xml2Json2.toXml(json);
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            String xml = IOUtils.toString(new FileInputStream(args[0]));
            System.out.println(EntityTransformer.toEntity(xml));
        } else {
            System.err.println("Missing file argument");
        }
    }

}
