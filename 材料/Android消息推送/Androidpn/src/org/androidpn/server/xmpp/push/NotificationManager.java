/*
 * Copyright (C) 2010 Moduad Co., Ltd.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.androidpn.server.xmpp.push;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.androidpn.server.model.NotificationMO;
import org.androidpn.server.model.User;
import org.androidpn.server.service.NotificationService;
import org.androidpn.server.service.ServiceLocator;
import org.androidpn.server.service.UserService;
import org.androidpn.server.util.CopyMessageUtil;
import org.androidpn.server.xmpp.session.ClientSession;
import org.androidpn.server.xmpp.session.SessionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;
import org.xmpp.packet.IQ;

/**
 * This class is to manage sending the notifcations to the users.
 * 
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class NotificationManager {

	private static final String NOTIFICATION_NAMESPACE = "androidpn:iq:notification";

	private final Log log = LogFactory.getLog(getClass());

	private SessionManager sessionManager;
	
	private NotificationService notificationService;
	
	private UserService userService;

	/**
	 * Constructor.
	 */
	public NotificationManager() {
		sessionManager = SessionManager.getInstance();
		notificationService = ServiceLocator.getNotificationService();
		userService = ServiceLocator.getUserService();
	}
	
	

	/**
	 * Broadcasts a newly created notification message to all connected users.
	 * 
	 * @param apiKey
	 *            the API key
	 * @param title
	 *            the title
	 * @param message
	 *            the message details
	 * @param uri
	 *            the uri
	 */
	public void sendBroadcast(String apiKey, String title, String message,
			String uri) {
		log.debug("sendBroadcast()...");
		List<NotificationMO> notificationMOs = new ArrayList<NotificationMO>();
		IQ notificationIQ = createNotificationIQ(apiKey, title, message, uri);

		for (ClientSession session : sessionManager.getSessions()) {
			// ����֪ͨ����
			NotificationMO notificationMO = new NotificationMO(apiKey, title,
					message, uri);
			try {
				notificationMO.setUsername(session.getUsername());
				notificationMO.setClientIp(session.getHostAddress());
				notificationMO.setResource(session.getAddress().getResource());
			} catch (Exception e) {
				e.printStackTrace();
			}
			// ����Ϣ��ID��ӵ�֪ͨ����
			CopyMessageUtil.IQ2Message(notificationIQ, notificationMO);

			if (session.getPresence().isAvailable()) {
				notificationMO.setStatus(NotificationMO.STATUS_SEND);
				notificationIQ.setTo(session.getAddress());
				session.deliver(notificationIQ);
			} else {
				notificationMO.setStatus(NotificationMO.STATUS_NOT_SEND);
			}
			//��ÿ��֪ͨ���뼯����
			notificationMOs.add(notificationMO);
		}
		try{
			//�������
			notificationService.createNotifications(notificationMOs);
		}catch(Exception e){
			log.warn(" notifications insert to database failure!!");
		}
		
	}
	
	//�������û�����֪ͨ
	public void sendAllBroadcast(String apiKey, String title, String message,
			String uri) {
		//��������֪ͨ����֮�����Ƶ�����������Ϊ�˱�֤ͬһ�������͵�֪ͨID��һ���ġ�ûʲô����
		IQ notificationIQ = createNotificationIQ(apiKey, title, message, uri);
		List<User> list = userService.getUsers();
		for (User user : list) {
			this.sendNotifcationToUser(apiKey, user.getUsername(), title, message, uri,notificationIQ);
		}
		
	}

	/**
	 * Sends a newly created notification message to the specific user.
	 * 
	 * @param apiKey
	 *            the API key
	 * @param title
	 *            the title
	 * @param message
	 *            the message details
	 * @param uri
	 *            the uri
	 */
	public void sendNotifcationToUser(String apiKey, String username,
			String title, String message, String uri, IQ notificationIQ) {
		log.debug("sendNotifcationToUser()...");
		ClientSession session = sessionManager.getSession(username);
		// ����֪ͨ����
		NotificationMO notificationMO = new NotificationMO(apiKey, title,
				message, uri);
		//������
		notificationMO.setUsername(username);
		// ����Ϣ��ID��ӵ�֪ͨ����
		CopyMessageUtil.IQ2Message(notificationIQ, notificationMO);
		if (session != null && session.getPresence().isAvailable()) {
			notificationIQ.setTo(session.getAddress());
			//������Ϣ
			session.deliver(notificationIQ);
			//�ѷ���״̬
			notificationMO.setStatus(NotificationMO.STATUS_SEND);
			try {
				notificationMO.setClientIp(session.getHostAddress());
				notificationMO.setResource(session.getAddress().getResource());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else{
			//δ����
			notificationMO.setStatus(NotificationMO.STATUS_NOT_SEND);
		}
		try{
			//�������ݿ�
			notificationService.saveNotification(notificationMO);
		}catch(Exception e){
			log.warn(" notifications insert to database failure!!");
		}
	}

	/**
	 * Creates a new notification IQ and returns it.
	 */
	private IQ createNotificationIQ(String apiKey, String title,
			String message, String uri) {
		Random random = new Random();
		String id = Integer.toHexString(random.nextInt());
		// String id = String.valueOf(System.currentTimeMillis());

		Element notification = DocumentHelper.createElement(QName.get(
				"notification", NOTIFICATION_NAMESPACE));
		notification.addElement("id").setText(id);
		notification.addElement("apiKey").setText(apiKey);
		notification.addElement("title").setText(title);
		notification.addElement("message").setText(message);
		notification.addElement("uri").setText(uri);

		IQ iq = new IQ();
		iq.setType(IQ.Type.set);
		iq.setChildElement(notification);

		return iq;
	}
	
	public void sendNotifications(String apiKey, String username,
			String title, String message, String uri){
		//��������֪ͨ����֮�����Ƶ�����������Ϊ�˱�֤ͬһ�������͵�֪ͨID��һ���ġ�ûʲô����
		IQ notificationIQ = createNotificationIQ(apiKey, title, message, uri);
		//����Ƕ���û������ݡ�;���ָ� ��ѭ������
		if(username.indexOf(";")!=-1){
			String[] users = username.split(";");
			for (String user : users) {
				this.sendNotifcationToUser(apiKey, user, title, message, uri,notificationIQ);
			}
		}else{
			this.sendNotifcationToUser(apiKey, username, title, message, uri,notificationIQ);
		}
	}
	
	//����������Ϣ
	public void sendOfflineNotification(NotificationMO notificationMO ) {
		log.debug("sendOfflineNotifcation()...");
		IQ notificationIQ = createNotificationIQ(notificationMO.getApiKey(), notificationMO.getTitle(), notificationMO.getMessage(), notificationMO.getUri());
		//������֪ͨID�滻��ԭ����ID
		notificationIQ.setID(notificationMO.getMessageId());
		ClientSession session = sessionManager.getSession(notificationMO.getUsername());
		if (session != null && session.getPresence().isAvailable()) {
			notificationIQ.setTo(session.getAddress());
			session.deliver(notificationIQ);
			try{
				//�޸�֪ͨ״̬Ϊ�ѷ���
				notificationMO.setStatus(NotificationMO.STATUS_SEND);
				//IP
				notificationMO.setClientIp(session.getHostAddress());
				//��Դ
				notificationMO.setResource(session.getAddress().getResource());
				notificationService.updateNotification(notificationMO);
			}catch (Exception e) {
				log.warn(" update notification status failure !");
			}
		}
	}
}
