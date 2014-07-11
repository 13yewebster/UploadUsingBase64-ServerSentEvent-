package com.me.project;

import javax.servlet.*;
import javax.servlet.http.*;

import java.awt.Component;
import java.io.*;
import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.util.*;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

public class UploadServlet extends HttpServlet{
	
	private static final long serialVersionUID = -7717396648124634557L;
	
	private Component parentComponent;
	
	public static class Global {
		

		public static long getBytesTransferred;
		
		public static long getGetBytesTransferred() {
			return getBytesTransferred;
		}

		public static void setGetBytesTransferred(long getBytesTransferred) {
			Global.getBytesTransferred = getBytesTransferred;
		}

		static boolean isLocal = false;
		
        public static boolean isLocal() {
			return isLocal;
		}

        public static void setLocal(boolean isLocal) {
			Global.isLocal = isLocal;
		}

		public static int percentDone;

		public int getPercentDone() {
			return percentDone;
		}

		public void setPercentDone(int percentDone) {
			Global.percentDone = percentDone;
		}
		
		private static String imageBytes;

		public static String getImageBytes() {
			return imageBytes;
		}

		public static void setImageBytes(String imageBytes) {
			Global.imageBytes = imageBytes;
		}

		private static String fileName;

		public static String getFileName() {
			return fileName;
		}

		public static void setFileName(String fileName) {
			Global.fileName = fileName;
		}	
		
		private static String value;

		public static String getValue() {
			return value;
		}

		public static void setValue(String value) {
			Global.value = value;
		}

	}
		
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
	response.setContentType("text/event-stream");
	response.setCharacterEncoding("UTF-8");
	PrintWriter writer = response.getWriter();
	
	//Set Path !!
	String localPath = "/Users/ByeWebster/Desktop/TempFiles";
	String s3Path = "mobileweb/TestUpload(Bye)/";
	
	boolean isMultipart = ServletFileUpload.isMultipartContent(request);
	if(isMultipart){

	 try{

		 // Set percent before running & after done
		 Global.percentDone = 0;
		 
		 ServletFileUpload upload = new ServletFileUpload();
         FileItemIterator iter = upload.getItemIterator(request);
         FileItemStream item = null;
         String name = "";
         InputStream stream = null;
         
        while (iter.hasNext()){
        	
	        item = iter.next();
	        name = item.getFieldName();
	        stream = item.openStream();
	        Global.value = Streams.asString(stream);

	           if(item.isFormField()){

	        	    if(name.equals("toDirectory")) {

	        	    	if(Global.value.equals("saveToLocal")) {

	        	    		Global.isLocal = true;
	        		   		
	        	   		} else {
	        	   			
	        	   			Global.isLocal = false;
	        	   			
	        	   		}
	        	    	
	        	    } else if(name.equals("imageDatas")) {
	        	    	
	        	    	Global.imageBytes = Global.value;
	        	    	
	        	    } else if(name.equals("fileName")) {
	        	    	
	        	    	Global.fileName = Global.value;
	        	    }
	        	
	           	}
           
         }
        
        if(Global.isLocal) {
        	
      	    InputStream in = new ByteArrayInputStream(convertBase64ToByteArray(Global.imageBytes));
            OutputStream out = new FileOutputStream(localPath+"/"+Global.fileName);

            //Set buffer read to write
            byte[] buffer = new byte[5120];
    	    int read = 0;
    	    int byteTransferred = 0;
    	    
    	    while ((read = in.read(buffer)) != -1) {
  
			  	out.write(buffer, 0, read);
	        	byteTransferred += read;
	        	Global.percentDone = (int) Math.floor((100.00 * (double)byteTransferred/(double)convertBase64ToByteArray(Global.imageBytes).length));
	        	System.out.println("percentDone : " + Global.percentDone);
	        
    	    }
    	  
	        in.close();
	        out.flush();
	        out.close();
        
        } else {
        	
        	// Set credential for key aws
        	AmazonS3 s3Client = new AmazonS3Client(new PropertiesCredentials(UploadServlet.class.getResourceAsStream("AwsCredentials.properties")));

        	// Set for transferProgress
   			TransferManager tx = new TransferManager(s3Client);
   			
   			// Here's Proccessing Image
            byte[] bytes = convertBase64ToByteArray(Global.imageBytes);
   			Long contentLength = Long.valueOf(convertBase64ToByteArray(Global.imageBytes).length);
   		    ObjectMetadata metadata = new ObjectMetadata();
   		    metadata.setContentLength(contentLength);
   		    
   		    // Ready to Async
   	   		PutObjectRequest putObjectRequest = new PutObjectRequest("wpmedia-shared", s3Path+Global.fileName, new ByteArrayInputStream(bytes), metadata);
   	   		final Upload myUpload = tx.upload(putObjectRequest);
   	   		
   	   		myUpload.addProgressListener(new ProgressListener() {

   				public void progressChanged(ProgressEvent progressEvent) {
   					
   					Global.getBytesTransferred = myUpload.getProgress().getBytesTransferred();
   					Global.percentDone = (int)myUpload.getProgress().getPercentTransferred();
   		        	System.out.println("percentDone : " + Global.percentDone);
   					
   					if (progressEvent.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE) {
   			            System.out.println("Upload Complete !!!");
   			        } else if (progressEvent.getEventCode() == ProgressEvent.FAILED_EVENT_CODE) {
   			            System.out.println("Failed Upload !!!");
   			        }

   				}
   				
   			}); 
        	
        	
        }
        
     } catch(Exception e) {
    	 // TODO Auto-generated catch block
   	  	e.printStackTrace();
   	  	
     }
       
   } 
	
  }

	public static String getPercentDone() {
		return "" + Global.percentDone + "";
	}
		
	public static byte[] convertBase64ToByteArray(String base64String) {
 		return Base64.decodeBase64(base64String.replaceFirst("^data:image/[^;]*;base64,?","").getBytes());
    }
	 
	
} 
