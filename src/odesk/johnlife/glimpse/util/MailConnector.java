package odesk.johnlife.glimpse.util;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.search.FlagTerm;

import odesk.johnlife.glimpse.Constants;
import odesk.johnlife.glimpse.app.GlimpseApp;
import odesk.johnlife.glimpse.data.FileHandler;

public class MailConnector implements Constants {

	public interface OnItemDownloadListener {
		void onItemDownload();
	}

	private static final String LOG_TAG = MailConnector.class.getSimpleName();

	private String user;
	private String pass;
	private String server;
	private OnItemDownloadListener onItemDownLoadListener;
	byte[] buf = new byte[4096];

	public MailConnector(String user, String pass, OnItemDownloadListener onItemDownLoadListener) {
		this.user = user;
		this.pass = pass;
		this.server = EMAIL_SERVER;
		this.onItemDownLoadListener = onItemDownLoadListener;
	}

	public void connect() {
		Log.d(LOG_TAG, String.format("Connecting to %s, user=%s, pass=%s", server, user, pass));
		Properties properties = System.getProperties();
		properties.setProperty("mail.store.protocol", "imaps");
		properties.setProperty("mail.imaps.socketFactory.class", SSL_FACTORY);
		Store store = null;
		Folder folder = null;
		try {
			Session session = Session.getDefaultInstance(properties, null);
			store = session.getStore("imaps");
			store.connect(server, user, pass);
			Log.d(LOG_TAG, "Connected to store");
			folder = store.getDefaultFolder().getFolder("INBOX");
			folder.open(Folder.READ_WRITE);
			Log.d(LOG_TAG, "Opened inbox");
			FileHandler fileHandler = GlimpseApp.getFileHandler();
			Message[] messages = fileHandler.isEmpty() ?
					folder.getMessages() :
					folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
			for(Message msg : messages) {
				try {
					List<File> attachments = getAttachments((Multipart) msg.getContent());
					if (!attachments.isEmpty()) {
						fileHandler.add(attachments, ((InternetAddress) msg.getFrom()[0]).getAddress());
						onItemDownLoadListener.onItemDownload();
					}
					msg.setFlag(Flags.Flag.DELETED, true);
				} catch (Exception e) {
					Log.e(LOG_TAG, "Error: ", e);
					//	PushLink.sendAsyncException(e);
				}
			}
			folder.setFlags(messages, new Flags(Flags.Flag.SEEN), true);
		} catch (MessagingException e) {
			Log.e(LOG_TAG, "Error: ", e);
//			PushLink.sendAsyncException(e);
		} finally {
			try {
				folder.close(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
			folder = null;
			try {
				store.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			store = null;
		}
	}

	private List<File> getAttachments(Multipart multipart) throws MessagingException {
		List<File> attachments = new ArrayList<File>();
		for (int i = 0; i < multipart.getCount(); i++) {
			FileOutputStream fos = null;
			InputStream is = null;
			try {
				BodyPart bodyPart = multipart.getBodyPart(i);
				if (!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) &&
						(bodyPart.getFileName() == null || bodyPart.getFileName().isEmpty())) {
					if (bodyPart.isMimeType("multipart/*")) {
						attachments.addAll(getAttachments((Multipart) bodyPart.getContent()));
					}
					continue; // dealing with attachments only
				}
				is = bodyPart.getInputStream();
				File f = new File(GlimpseApp.getTempDir(), bodyPart.getFileName());
				fos = new FileOutputStream(f);
				int bytesRead;
				while ((bytesRead=is.read(buf)) != -1) {
					fos.write(buf, 0, bytesRead);
				}
				attachments.add(f);
			} catch (IOException e) {
//				PushLink.sendAsyncException(e);
			} finally {
				try {
					is.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				is = null;
				try {
					fos.flush();
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					fos.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				fos = null;
			}
		}
		Log.d(LOG_TAG, "Found "+attachments.size()+" attachments.");
		return attachments;
	}

//	/**
//     * Return the primary text content of the message.
//     */
//    private String getText(Part p) throws MessagingException, IOException {
//        if (p.isMimeType("text/*")) {
//            String s = (String)p.getContent();
////            textIsHtml = p.isMimeType("text/html");
//            return s;
//        }
//
//        if (p.isMimeType("multipart/alternative")) {
//            // prefer html text over plain text
//            Multipart mp = (Multipart)p.getContent();
//            String text = null;
//            for (int i = 0; i < mp.getCount(); i++) {
//                Part bp = mp.getBodyPart(i);
//                if (bp.isMimeType("text/plain")) {
//                    if (text == null)
//                        text = getText(bp);
//                    continue;
//                } else if (bp.isMimeType("text/html")) {
//                    String s = getText(bp);
//                    if (s != null)
//                        return s;
//                } else {
//                    return getText(bp);
//                }
//            }
//            return text;
//        } else if (p.isMimeType("multipart/*")) {
//            Multipart mp = (Multipart)p.getContent();
//            for (int i = 0; i < mp.getCount(); i++) {
//                String s = getText(mp.getBodyPart(i));
//                if (s != null)
//                    return s;
//            }
//        }
//
//        return null;
//    }

}