package org.springframework.ioc.xml;

import java.io.InputStream;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class XmlUtil {
		
	public String handlerXMLForScanPackage(String configuration){
		System.out.println(System.getProperty("user.dir"));//user.dir指定了当前的路径 
		System.out.println(configuration);
		InputStream ins = this.getClass().getClassLoader().getResourceAsStream(configuration);
		SAXReader reader = new SAXReader();
		try{
			Document document = reader.read(ins);
			Element root = document.getRootElement();
			Element ele = root.element("package-scan");
			String res = ele.attributeValue("component-scan");
			return res;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}

}
