package org.springframework.ioc.bean;

import org.springframework.ioc.factory.BeanFactory;
import org.springframework.ioc.xml.XmlUtil;

public abstract class ApplicationContext extends BeanFactory {
	
	protected String configuration;
	protected XmlUtil xmlUtil;
	
	public ApplicationContext(String configuration){
		this.configuration = configuration;
		this.xmlUtil = new XmlUtil();
	}
}
