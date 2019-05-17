package ext.appinventor.ThingSpeak.ClientThingSpeakAI2Ext;

/**
 * Simple Client ThingSpeak based on the Google Volley library
 * @author aluis.rcastro@bol.com.br
 * @Date 2019.05.16
 *
 * Copyright (c) 2019 andre luis ramos de castro
 *
 *  ### This code is provided "as-is", which means no implicit or explicit warranty ###
 */
 
//
//   Required copyright notices:
//   --------------------------
//   Copyright 2009-2011 Google, All Rights reserved
//   Copyright 2011-2012 MIT, All rights reserved
//

import com.google.appinventor.components.runtime.*;
	 
import android.os.Environment;
import android.os.AsyncTask;
	 
import javax.swing.JOptionPane;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.Map;
	 
import com.google.appinventor.components.runtime.util.RuntimeErrorAlert;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.appinventor.components.runtime.util.SdkLevel;
	 
import com.google.appinventor.components.runtime.errors.YailRuntimeError;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.os.StrictMode;

import java.io.ByteArrayOutputStream; 
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import java.lang.Enum;
	
import java.util.concurrent.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import com.jcraft.jsch.*;
import com.jcraft.jzlib.*;

import javax.swing.*;
import java.io.*;

/**
 * General config parameters
 */
@DesignerComponent(version = 2,
		description = "Non-visible component that provides client ssh connectivity.",
		category = ComponentCategory.EXTENSION,
		nonVisible = true,
		iconName = "https://www.edaboard.com/attachment.php?attachmentid=153147&d=1558054035")
@SimpleObject(external = true)
@UsesLibraries(libraries = "volley.jar , json-20180813.jar")
@UsesPermissions(permissionNames =	"android.permission.INTERNET," +
									"android.permission.CHANGE_NETWORK_STATE," +
									"android.permission.ACCESS_WIFI_STATE," +
									"android.permission.ACCESS_NETWORK_STATE," +
									"android.permission.WRITE_EXTERNAL_STORAGE," +
									"android.permission.READ_EXTERNAL_STORAGE," +									
									"android.permission.WRITE_SETTINGS," +
									"android.permission.WRITE_SYNC_SETTINGS," +
									"android.permission.PERSISTENT_ACTIVITY," +
									"android.permission.CHANGE_CONFIGURATION," +
									"android.permission.READ_PHONE_STATE")								

public class ClientThingSpeakAI2Ext extends AndroidNonvisibleComponent implements Component
{
    private static final String LOG_TAG = "ClientThingSpeakAI2Ext";

	public static final int commIDLE 		= 0;
	public static final int commWRITE 		= 1;
	public static final int commREAD		= 2;	
				
	private String 		sChannel 			= "";							// ThingSpeak channel used for reading values on cloud server
	private String 		sReadKey 			= "";							// ThingSpeak key used for reading values on cloud server
	private String 		sWriteKey 			= "";							// ThingSpeak key used for writing values on cloud server
	private String 		sReceivedMessage	= "";							// Stores the message just got from HTTP
	private String 		sJsonResponse 		= "";							// Used only when command issued was READ	( sJsonResponse <- sReceivedMessage )
	private String 		sTimeStamp 			= "";							// Informs tate/time of the last data just acquired from cloud
	private String 		sDebugText 			= "";							// Auxiliary variable, useful whenever debugging issues
	private String[]	sField				= {"","","","","","","",""};	// Store the sField[n] content, just parsed from sJsonResponse
	private int 		iField	 			= 0;							// Index of one of the 8 fields within each channel
	private boolean 	bConnectionState 	= false;						// Internal variable
	private int 		sCommandIssued;										// Need to be externally switched to IDLE after handling receiver events
	
    private final Activity activity;

    InputStream inputStream = null;
		

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Creates a new Client SSH component.
     *
     * @param container the Form that this component is contained in.
     */
    public ClientThingSpeakAI2Ext(ComponentContainer container) {
        super(container.$form());
        activity = container.$context();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);		
    }
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Methods that return Communication statuses
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "IDLE state: Should be switched to this state by end user whenever handling receiving events")
    public int IDLE() {
        return commIDLE;
    }
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Inform end user that last command issued were writing value to cloud server")
    public int WRTING() {
        return commWRITE;
    }
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Inform end user that last command issued were reading value to cloud server")
    public int READING() {
        return commREAD;
    }
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   /**
     * Receive data from the ThingSpeak server
     */
    @SimpleFunction(description = "Read value from the server")
    public void GetdData( final String sndChannel , final String sndReadKey , final String sndField , final boolean sndPrivate ) {
		SetDebugText("Start GetData function");
		SetCommandIssued ( commREAD );					
        if ( GetConnectionState() == true ) {
			SetDebugText("Already connected");
		}
		else{
			if ( GetCommandIssued() != commIDLE )
				try {	
					AsynchUtil.runAsynchronously(new Runnable() {
						@Override
						public void  run() {
												
							RequestQueue ThingSpeakRequestQueue = Volley.newRequestQueue(activity);
							
							String url;
							if ( sndPrivate == true )
								url = "https://api.thingspeak.com/channels/" + sndChannel + "/feeds.json?api_key=" + sndReadKey + "&results=1" ;
							else
								url = "https://api.thingspeak.com/channels/" + sndChannel + "/feeds.json?results=1" ;		
							
							StringRequest ThingSpeakStringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
									@Override
									public void onResponse(String response) {							
										SetReceivedMessage(response);
										sJsonResponse = GetReceivedMessage();
										
										if ( sJsonResponse == "0" )
											SetDebugText("Error while updating value (likely did not wait deadtime between consecutive reads)");
										else if ( sJsonResponse == "-1" )
											SetDebugText("Private channel");
										else {			
											int iSize 					= sJsonResponse.length(); 							// string length
											int iPointerEnd 			= 0;
											SetFieldIndex( Integer.parseInt(sndField) );
											
											int firstIndexTimeStamp		= sJsonResponse.lastIndexOf( "updated_at" ); 
											int iPointerStartTime 		= firstIndexTimeStamp + 12 ;						// "updated_at:"  => 11 characters 
											iPointerEnd = iPointerStartTime;
											for ( int i=iPointerEnd; i<iSize; i++ ) {
												if (sJsonResponse.charAt(i) == ',') {
													iPointerEnd = i;		// seek for comma position
													SetTimeStamp ( removeDoubleQuotes( sJsonResponse.substring( iPointerStartTime, iPointerEnd-1 ) ) ); 	
													break;
												}
											}
											
											int firstIndexField			= sJsonResponse.lastIndexOf( "field" + sndField ); 
											int iPointerStartField 		= firstIndexField + 8 ;								// "fieldn:"  => 7 characters 
											iPointerEnd = iPointerStartField;
											for ( int i=iPointerEnd; i<iSize; i++ ) {
												if (sJsonResponse.charAt(i) == ',') {
													iPointerEnd = i;		// seek for comma position
													SetField( removeDoubleQuotes ( sJsonResponse.substring( iPointerStartField, iPointerEnd-1 )) , GetFieldIndex() );
													SetDebugText("Successful read");
													NewIncomingMessage( GetField() );
													break;
												}
											}
										}								
									}
								}, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
									@Override
									public void onErrorResponse(VolleyError error) {
										SetDebugText("onErrorResponse error");
									}
								});
							ThingSpeakRequestQueue.add(ThingSpeakStringRequest);						
						}	
					});
					}
					catch(Exception err){
						SetConnectionState(false);
					}		
		}
	}	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   /**
     * Send cmd to the ThingSpeak server
     */
    @SimpleFunction(description = "Send cmd to the server")
    public void SendData( final String sndChannel , final String sndWriteKey , final String sndField , final String sndValue ) {
		SetDebugText("Start SendData function");
		SetCommandIssued ( commWRITE );
        if ( GetConnectionState() == true ) {
			SetDebugText("Already connected");
		}
		else {
			if ( GetCommandIssued() != commIDLE ) {
				{
				try {	
						AsynchUtil.runAsynchronously(new Runnable()
						{
							@Override
							public void run() {
								
								RequestQueue ThingSpeakRequestQueue = Volley.newRequestQueue(activity);
								
								String url = "https://api.thingspeak.com/update?api_key=" + sndWriteKey + "&field" + sndField + "=" + sndValue ;
								
								StringRequest ThingSpeakStringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
										@Override
										public void onResponse(String response) {	
											SetFieldIndex( Integer.parseInt(sndField) );
											SetReceivedMessage(response);							// It is expected either : 0 (invalid interval),
											if ( GetReceivedMessage() == "0" )						//   -1 (error), or any Unsigned >0
												SetDebugText("Too short interval between consecutive commands");		
											else if ( GetReceivedMessage() == "-1" )
												SetDebugText("Error updating value");
											else {
												SetDebugText("Successful write");
												NewIncomingMessage( "ID: " + GetReceivedMessage() );
											}
										}
									}, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
										@Override
										public void onErrorResponse(VolleyError error) {
											// SetDebugText("onErrorResponse error");
										}
									});

								ThingSpeakRequestQueue.add(ThingSpeakStringRequest);						
							}	
						});
					}
					catch(Exception err){
						SetConnectionState(false);
					}
				}
			}
		}
	}	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Event indicating that there is available new text line
     *
     */
	@SimpleEvent
    public void NewIncomingMessage(String msgIn)
    {
        // invoke the application's "NewIncomingMessage" event handler.
        EventDispatcher.dispatchEvent(this, "NewIncomingMessage", msgIn);
    }
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Method that returns Command Issued
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Get Command Issued")
    public int GetCommandIssued() {
        return sCommandIssued;
    }
    
    /**
     * Method that set Command Issued
     */
	@SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Set Command Issued")
    public void SetCommandIssued(int commandIssued) {
        sCommandIssued = commandIssued;
    }		
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Method that returns Field content
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Get Field content")
    public String GetField() {
        return sField[iField];
    }
    
    /**
     * Method that set Field content
     */
    public void SetField(String field, int ifield) {
        sField[ifield] = field;
    }
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Method that returns Field
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Get Field[n] index n")
    public int GetFieldIndex() {
        return iField;
    }
	
    /**
     * Method that set the Cmd
     */
    public void SetFieldIndex( int field ) {
        iField = field;
    }	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Method that returns TimeStamp
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Get TimeStamp")
    public String GetTimeStamp() {
        return sTimeStamp;
    }
	
    /**
     * Method that set the TimeStamp
     */
    public void SetTimeStamp( String timestamp ) {
        sTimeStamp = timestamp;
    }	
			
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Method that returns the connection state
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Get state of the connection - true = connected, false = disconnected")
    public boolean GetConnectionState() {
        return bConnectionState;
    }
	
    /**
     * Method that set the connection state
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Set state of the connection - true = connected, false = disconnected")
    public void SetConnectionState(boolean state) {
        bConnectionState = state;
    }	
			
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Method that returns Debug message Text
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Get Debug Text")
    public String GetDebugText() {
        return sDebugText;
    }
    
    /**
     * Method that set Debug message Text
     */
	@SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Set Debug Text")	
    public void SetDebugText(String text) {
        sDebugText = text;
    }

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Method that returns the HTTP Text from remote server
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "read text from remote HTTP server")
    public String GetReceivedMessage() {
        return sReceivedMessage;
    }
	
    public void SetReceivedMessage(String text) {
        sReceivedMessage = text;
    }
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static String removeDoubleQuotes(String input){

		StringBuilder sb = new StringBuilder();
		
		char[] tab = input.toCharArray();
		for( char current : tab ){
			if( current != '"' )
			sb.append( current );	
		}
		
		return sb.toString();
		}
}
