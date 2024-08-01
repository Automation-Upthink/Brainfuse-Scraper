package com.upthink;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class GmailService {

    private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static Gmail SERVICE;
    private final TemplateEngine templateEngine;

    public GmailService() throws GeneralSecurityException, IOException {
        SERVICE = new Gmail.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, GoogleAuthentication.getCredentials(GoogleNetHttpTransport.newTrustedTransport()))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Set up the Thymeleaf template engine
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
    }

    public void sendEmail(String fromEmail, String toEmail, List<String> ccEmails, String subject, String bodyText) throws MessagingException, IOException {
        MimeMessage email = createEmail(toEmail, fromEmail, ccEmails, subject, bodyText);
        sendMessage(SERVICE, "me", email);
    }

    public void sendEmailWithTemplate(String fromEmail, String toEmail, List<String> ccEmails, String subject, String templateName, Map<String, Object> templateVariables) throws MessagingException, IOException {
        // Prepare the email content using the template
        Context context = new Context();
        templateVariables.forEach(context::setVariable);
        String emailContent = templateEngine.process(templateName, context);
        System.out.println(emailContent);
        // Create and send the email
        MimeMessage email = createEmail(toEmail, fromEmail, ccEmails, subject, emailContent);
        sendMessage(SERVICE, "me", email);
    }

    public static MimeMessage createEmail(String to, String from, List<String> ccEmails, String subject, String bodyText) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));

        for (String cc : ccEmails) {
            email.addRecipient(javax.mail.Message.RecipientType.CC, new InternetAddress(cc));
        }

        email.setSubject(subject);
        email.setContent(bodyText, "text/html"); // Set the email content as HTML
        return email;
    }

    public static Message sendMessage(Gmail service, String userId, MimeMessage emailContent) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().encodeToString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        message = service.users().messages().send(userId, message).execute();
        System.out.println("Message id: " + message.getId());
        System.out.println(message.toPrettyString());
        return message;
    }
}



//package com.upthink;
//
//import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
//import com.google.api.client.json.JsonFactory;
//import com.google.api.client.json.gson.GsonFactory;
//import com.google.api.services.gmail.Gmail;
//import com.google.api.services.gmail.model.Message;
//
//import javax.mail.MessagingException;
//import javax.mail.Session;
//import javax.mail.internet.InternetAddress;
//import javax.mail.internet.MimeMessage;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.security.GeneralSecurityException;
//import java.util.List;
//import java.util.Properties;
//import java.util.Base64;
//
//public class GmailService {
//
//    private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
//    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
//    private static Gmail SERVICE;
//
//    public GmailService() throws GeneralSecurityException, IOException {
//        SERVICE = new Gmail.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, GoogleAuthentication.getCredentials(GoogleNetHttpTransport.newTrustedTransport()))
//                .setApplicationName(APPLICATION_NAME)
//                .build();
//    }
//
//    public void sendEmail(String fromEmail, String toEmail, List<String> ccEmails, String subject, String bodyText) throws MessagingException, IOException {
//        MimeMessage email = createEmail(toEmail, fromEmail, ccEmails, subject, bodyText);
//        sendMessage(SERVICE, "me", email);
//    }
//
//    public static MimeMessage createEmail(String to, String from, List<String> ccEmails, String subject, String bodyText) throws MessagingException {
//        Properties props = new Properties();
//        Session session = Session.getDefaultInstance(props, null);
//
//        MimeMessage email = new MimeMessage(session);
//        email.setFrom(new InternetAddress(from));
//        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
//
//        for (String cc : ccEmails) {
//            email.addRecipient(javax.mail.Message.RecipientType.CC, new InternetAddress(cc));
//        }
//
//        email.setSubject(subject);
//        email.setText(bodyText);
//        return email;
//    }
//
//    public static Message sendMessage(Gmail service, String userId, MimeMessage emailContent) throws MessagingException, IOException {
//        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//        emailContent.writeTo(buffer);
//        byte[] bytes = buffer.toByteArray();
//        String encodedEmail = Base64.getUrlEncoder().encodeToString(bytes);
//        Message message = new Message();
//        message.setRaw(encodedEmail);
//        message = service.users().messages().send(userId, message).execute();
//        System.out.println("Message id: " + message.getId());
//        System.out.println(message.toPrettyString());
//        return message;
//    }
//
//}
