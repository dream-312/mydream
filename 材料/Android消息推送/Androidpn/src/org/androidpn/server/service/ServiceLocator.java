package org.androidpn.server.service;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

 
public class ServiceLocator implements BeanFactoryAware {
    private static BeanFactory beanFactory = null;

    private static ServiceLocator servlocator = null;

    public static String USER_SERVICE = "userService";

    public static String NOTIFICATION_SERVICE = "notificationService";

    public void setBeanFactory(BeanFactory factory) throws BeansException {
	this.beanFactory = factory;
    }

    public BeanFactory getBeanFactory() {
	return beanFactory;
    }

    public static ServiceLocator getInstance() {
	if (servlocator == null)
	    servlocator = (ServiceLocator) beanFactory.getBean("serviceLocator");
	return servlocator;
    }

    /**
     * �����ṩ��bean���Ƶõ���Ӧ�ķ�����
     * 
     * @param servName
     *            bean����
     */
    public static Object getService(String servName) {
	return beanFactory.getBean(servName);
    }

    /**
     * �����ṩ��bean���Ƶõ���Ӧ��ָ�����͵ķ�����
     * 
     * @param servName
     *            bean����
     * @param clazz
     *            ���ص�bean����,�����Ͳ�ƥ��,���׳��쳣
     */
    public static Object getService(String servName, Class clazz) {
	return beanFactory.getBean(servName, clazz);
    }

    /**
     * Obtains the user service.
     * 
     * @return the user service
     */
    public static UserService getUserService() {
	return (UserService) getService(USER_SERVICE);
    }

    public static NotificationService getNotificationService() {
	return (NotificationService) getService(NOTIFICATION_SERVICE);
    }
}
