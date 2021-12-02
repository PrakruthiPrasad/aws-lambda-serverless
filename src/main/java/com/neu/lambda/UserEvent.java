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
    static final String SUBJECT = "Email Verification for Cloud Native Web Application - CSYE6225";
    String HTMLBODY = "";
    static String TEXTBODY = "";

    public Object handleRequest(SNSEvent request, Context context){

    	try {
            String HTMLBODY = "";
            String TEXTBODY = "";
            AmazonDynamoDB dynamoclient = AmazonDynamoDBClientBuilder.standard().build();
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
            context.getLogger().log("Invocation started: " + timeStamp);
            String msg = request.getRecords().get(0).getSNS().getMessage();
            System.out.println("Msg recieved: "+msg);
            String split_msg[] = msg.split("::");
            System.out.println("Msg split done");
            String To_Email = split_msg[0];
            System.out.println("email: " + To_Email);
            String token = split_msg[1];
            System.out.println("Token: " +token);
            String ttl = split_msg[2];
            System.out.println("ttl: " + ttl);
            System.out.println("to email: "+split_msg[0]);
            System.out.println("token "+split_msg[1]);
            String verify_url = "http://prod.csye6225dnsbagur.me/v1/user/EmailVerification?email="+To_Email+"&token="+token+"&ttl="+ttl;
            HTMLBODY+="<h1>Click on below Link to Verify your email address:</h1>";
            HTMLBODY+= new URL(verify_url);
            HTMLBODY+="<br>";
            GetItemRequest req = new GetItemRequest();
            req.setTableName("csye6225_Email");
            req.setConsistentRead(true);
            Map<String, AttributeValue> keysMap = new HashMap();
            keysMap.put("username",new AttributeValue(To_Email));
            req.setKey(keysMap);
            GetItemResult result = dynamoclient.getItem(req);
            if (result.getItem() != null)
                {  System.out.println("Email already sent to: "+To_Email);}
            else {
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
                System.out.println("Email sent!"+ To_Email);
                context.getLogger().log(request.getRecords().get(0).getSNS().getMessage());
                Map<String, AttributeValue> map = new HashMap();
                map.put("username", new AttributeValue(To_Email));
                PutItemRequest request11 = new PutItemRequest();
                request11.setTableName("csye6225_Email");
                request11.setItem(map);
                PutItemResult result1 = dynamoclient.putItem(request11);
            }
            timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
            context.getLogger().log("Invocation completed: " + timeStamp);
        }catch(Exception e) {
            System.out.println("Email not sent");
            System.out.println( e.getMessage());
            System.out.println("Stack trace: ");
            e.printStackTrace();
        }
     
        return null;


}
}
