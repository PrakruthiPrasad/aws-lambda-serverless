package com.neu.lambda;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

public class UserEvent implements RequestHandler<SNSEvent, Object> {
	
	static final String FROM = "Webapp@prod.csye6225dnsbagur.me";
    static final String SUBJECT = "Email Verification";
    String HTMLBODY = "";
    static String TEXTBODY = "";

    public Object handleRequest(SNSEvent request, Context context){

        try {
            AmazonDynamoDB dynamoclient = AmazonDynamoDBClientBuilder.standard().build();
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
            context.getLogger().log("Invocation started: " + timeStamp);
            String msg = request.getRecords().get(0).getSNS().getMessage();
            String split_msg[] = msg.split(":");
            String To_Email = split_msg[0];
            String token = split_msg[1];
            String ttl = split_msg[2];
            String verify_url = "http://prod.csye6225dnsbagur.me/v1/user/EmailVerification?email="+To_Email+"&token="+token+"&ttl="+ttl;
            HTMLBODY+="<h1>Click on below Link to Verify your email address:</h1>";
            HTMLBODY+= new URL(verify_url);
            HTMLBODY+="<br>";
            GetItemRequest req = new GetItemRequest();
            req.setTableName("MailSent");
            req.setConsistentRead(true);
            Map<String, AttributeValue> keysMap = new HashMap();
            keysMap = req.getKey();
            Boolean mailsent_flag = false;
            for (AttributeValue value : keysMap.values()) {
               if (value.equals(To_Email))
                { mailsent_flag = true;}
            }
            if(mailsent_flag==false) {
                AmazonSimpleEmailService client =  AmazonSimpleEmailServiceClientBuilder.standard().build();
                SendEmailRequest email_req = new SendEmailRequest()
                        .withDestination(
                                new Destination().withToAddresses(To_Email))
                        .withMessage(new Message()
                                .withBody(new Body()
                                        .withHtml(new Content()
                                                .withCharset("UTF-8").withData(HTMLBODY))
                                        .withText(new Content()
                                                .withCharset("UTF-8").withData(TEXTBODY)))
                                .withSubject(new Content()
                                        .withCharset("UTF-8").withData(SUBJECT)))
                        .withSource(FROM);
                client.sendEmail(email_req);
                System.out.println("Email sent!");
                context.getLogger().log(request.getRecords().get(0).getSNS().getMessage());
                Map<String, AttributeValue> map = new HashMap();
                map.put(To_Email, new AttributeValue(To_Email));
                PutItemRequest request11 = new PutItemRequest();
                request11.setTableName("MailSent");
                request11.setItem(map);
                PutItemResult result1 = dynamoclient.putItem(request11);
            }
            timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
            context.getLogger().log("Invocation completed: " + timeStamp);
        }catch(Exception e) {
            System.out.println("Email not sent");
        }
        return null;


}
}
