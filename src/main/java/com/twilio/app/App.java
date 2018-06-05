package com.twilio.app;

import static spark.Spark.*;

import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;

import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Body;
import com.twilio.twiml.messaging.Message;

public class App {
    public static void main(String[] args) {
        post("/sms", (req, res) -> {
            Map<String, String> parameters = parseBody(req.body());
            String numMediaStr = parameters.get("NumMedia");
            int numMedia = Integer.parseInt(numMediaStr);

            if (numMedia > 0) {
                while (numMedia > 0) {
                    numMedia = numMedia - 1;

                    // Get all info
                    String mediaUrl = parameters.get(String.format("MediaUrl%d", numMedia));
                    String contentType = parameters.get(String.format("MediaContentType%d", numMedia));
                    String fileName = mediaUrl.substring(mediaUrl.lastIndexOf("/") + 1);
                    MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
                    MimeType mimeType = allTypes.forName(contentType);
                    String fileExtension = mimeType.getExtension();
                    File file = new File(fileName + fileExtension);

                    // Download file
                    URL url = new URL(mediaUrl);
                    CloseableHttpClient httpclient = HttpClients.custom()
                        .setRedirectStrategy(new LaxRedirectStrategy()) 
                        .build();
                    HttpGet get = new HttpGet(url.toURI());
                    HttpResponse response = httpclient.execute(get);
                    InputStream source = response.getEntity().getContent();
                    FileUtils.copyInputStreamToFile(source, file);
                }
            }

            // Send message back
            String message = (numMedia > 0) ? String.format("Thanks for sending us %s file(s)!", numMedia) : "Send us an image!";
            res.type("application/xml");
            Body body = new Body
                    .Builder(message)
                    .build();
            Message sms = new Message
                    .Builder()
                    .body(body)
                    .build();
            MessagingResponse twiml = new MessagingResponse
                    .Builder()
                    .message(sms)
                    .build();
            return twiml.toXml();

        });
    }

    // Body parser help
    public static Map<String, String> parseBody(String body) throws UnsupportedEncodingException {
      String[] unparsedParams = body.split("&");
      Map<String, String> parsedParams = new HashMap<String, String>();
      for (int i = 0; i < unparsedParams.length; i++) {
        String[] param = unparsedParams[i].split("=");
        if (param.length == 2) {
          parsedParams.put(urlDecode(param[0]), urlDecode(param[1]));
        } else if (param.length == 1) {
          parsedParams.put(urlDecode(param[0]), "");
        }
      }
      return parsedParams;
    }

    public static String urlDecode(String s) throws UnsupportedEncodingException {
      return URLDecoder.decode(s, "utf-8");
    }
}
