/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.leserplus.internal;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.TooManyListenersException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.library.types.StringType;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a serial device that is linked to exactly one String item and/or Switch item.
 * 
 * @author Kai Kreuzer
 *
 */
public class LeserPlusDevice implements SerialPortEventListener {

	private static final Logger logger = LoggerFactory.getLogger(LeserPlusDevice.class);

	private String port;
	private String readerType;
	private String transponderEventItemName;
	private String openDoorItemName;
	private String disableTranspondersItemName;

	private boolean disableTransponders;
	
	private EventPublisher eventPublisher;
	private TransformationService transformationService;
	
	private CommPortIdentifier portId;
	private SerialPort serialPort;

	private InputStream inputStream;

	private OutputStream outputStream;
	
	private Pattern leser7plusPattern;
	private Pattern leser9Pattern;
	
	private String leser7plusOpenRelaisCommand = "\u0002FFCR152\u0004";
	private String leser7plusCloseRelaisCommand = "\u0002FFCR051\u0004";
	private String leser9OpenRelaisCommand = "\u0002R1\u0004";
	private String leser9CloseRelaisCommand = "\u0002R0\u0004";
	private String leser9ReaderId;
	private long lastCommandParsed;
	
	public LeserPlusDevice(String port, String readerType, String readerId) {
		this.port = port;
		this.readerType = readerType;
		this.leser9ReaderId = readerId;
		
		leser7plusPattern = Pattern.compile("[\u0002]([A-Z0-9]{2})EM([A-Z0-9]{10}).*[\u0004]");
		leser9Pattern = Pattern.compile("[\u0002]R([A-Z0-9]{10})[\u0004].*");
		
		if (!readerType.equals("leser7plus") && !readerType.equals("leser9")) {
			logger.error("Invalid reader type: " + readerType);
			return;
		}
	}

	public void setEventPublisher(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	public void unsetEventPublisher(EventPublisher eventPublisher) {
		this.eventPublisher = null;
	}
	
	public void setTransformationService(TransformationService transformationService) {
		this.transformationService = transformationService;
	}

	public String getPort() {
		return port;
	}

	public String getTransponderEventItemName() {
		return transponderEventItemName;
	}

	public void setTransponderEventItemName(String transponderEventItemName) {
		this.transponderEventItemName = transponderEventItemName;
	}
	
	public String getOpenDoorItemName() {
		return openDoorItemName;
	}

	public void setOpenDoorItemName(String openDoorItemName) {
		this.openDoorItemName = openDoorItemName;
	}

	public String getDisableTranspondersItemName() {
		return disableTranspondersItemName;
	}

	public void setDisableTranspondersItemName(String disableTranspondersItemName) {
		this.disableTranspondersItemName = disableTranspondersItemName;
	}
	
	/**
	 * Initialize this device and open the serial port
	 * 
	 * @throws InitializationException if port can not be opened
	 */
	@SuppressWarnings("rawtypes")
	public void initialize() throws InitializationException {
		// parse ports and if the default port is found, initialized the reader
		Enumeration portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			CommPortIdentifier id = (CommPortIdentifier) portList.nextElement();
			if (id.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if (id.getName().equals(port)) {
					logger.debug("Serial port '{}' has been found.", port);
					portId = id;
				}
			}
		}
		
		if (portId != null) {
			// initialize serial port
			try {
				serialPort = (SerialPort) portId.open("openHAB", 2000);
			} catch (PortInUseException e) {
				throw new InitializationException(e);
			}

			try {
				inputStream = serialPort.getInputStream();
			} catch (IOException e) {
				throw new InitializationException(e);
			}

			try {
				serialPort.addEventListener(this);
			} catch (TooManyListenersException e) {
				throw new InitializationException(e);
			}

			// activate the DATA_AVAILABLE notifier
			serialPort.notifyOnDataAvailable(true);

			try {
				int baud;
				if (readerType.equals("leser7plus")) {
					baud = 57600;
				} else if (readerType.equals("leser9")) {
					baud = 9600;
				} else {
					logger.error("Invalid reader type: " + readerType);
					return;
				}
				// set port parameters
				serialPort.setSerialPortParams(baud, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);
			} catch (UnsupportedCommOperationException e) {
				throw new InitializationException(e);
			}

			try {
				// get the output stream
				outputStream = serialPort.getOutputStream();
			} catch (IOException e) {
				throw new InitializationException(e);
			}
			
			logger.debug("Serial port '{}' has been initialized.", port);
		} else {
			StringBuilder sb = new StringBuilder();
			portList = CommPortIdentifier.getPortIdentifiers();
			while (portList.hasMoreElements()) {
				CommPortIdentifier id = (CommPortIdentifier) portList.nextElement();
				if (id.getPortType() == CommPortIdentifier.PORT_SERIAL) {
					sb.append(id.getName() + "\n");
				}
			}
			throw new InitializationException("Serial port '" + port + "' could not be found. Available ports are:\n" + sb.toString());
		}
	}

	public void serialEvent(SerialPortEvent event) {
		switch (event.getEventType()) {
		case SerialPortEvent.BI:
		case SerialPortEvent.OE:
		case SerialPortEvent.FE:
		case SerialPortEvent.PE:
		case SerialPortEvent.CD:
		case SerialPortEvent.CTS:
		case SerialPortEvent.DSR:
		case SerialPortEvent.RI:
		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
			break;
		case SerialPortEvent.DATA_AVAILABLE:
			try {
				logger.debug("DATA_AVAILABLE event: {} bytes on serial port {}", new Object[] { inputStream.available(), port });
				
				do {
					StringBuilder sb = new StringBuilder();
					byte b = 0;
	                
                    // read data from serial device
                    while (inputStream.available() > 0 && b != 4 ) {
                    	b = (byte)inputStream.read();
                    	sb.append((char)b);
                    }
	                
	                String result = sb.toString();
	                logger.debug("Received message '{}' on serial port {}", new Object[] { result, port });
	                                
	    			if (transponderEventItemName != null) {
	    				Matcher matcher;
	    					
	    				if (readerType.equals("leser7plus")) {
	    					matcher = leser7plusPattern.matcher(result);
	    				} else if (readerType.equals("leser9")) {
	    					matcher = leser9Pattern.matcher(result);
	    				} else {
	    					logger.error("Invalid reader type: " + readerType);
	    					return;
	    				}
	    				
	                    if (matcher.matches()) {
	                    	//Only parse one transponder each 15 seconds
	                        long currentTime = System.currentTimeMillis();
	                        if (currentTime - lastCommandParsed <= 15000) {
	                        	logger.debug("Wait 15 seconds until next transponder is checked.");
	                        	return;
	                        }

	                    	lastCommandParsed = currentTime;
	                    	
	                    	String readerId;
	                    	String transponderId;
	                    	
	                    	if (readerType.equals("leser7plus")) {
	                    		readerId = matcher.group(1);
	                        	transponderId = matcher.group(2);
	        				} else if (readerType.equals("leser9")) {
	        					readerId = leser9ReaderId;
	                        	transponderId = matcher.group(1);
	        				} else {
	        					logger.error("Invalid reader type: " + readerType);
	        					return;
	        				}
	                    	
	                    	if(transformationService == null) {
	    						logger.error("No transformation service available!");
	                    	} else {
	                    		String userInfo = transformationService.transform("transponders.map", transponderId);
	                    		
	                    		//Check if user was found in the map file
	                    		if (!userInfo.equals("") && !userInfo.equals(transponderId)) {
	                    			String userInfos[] = userInfo.split("\\|");
	                    			
	                    			//Check if user has at least set one reader
	                    			if (userInfos.length < 2) {
	                        			logger.debug("Transponder {} has no authrorized readers configured (reader {}, userInfo {}, userInfos.length {}).",
	                        					new Object[] { transponderId, readerId, userInfo, userInfos.length });
	                        			eventPublisher.sendCommand(transponderEventItemName, new StringType(String.format("NOK|%s|%s", transponderId, readerId)));
	                        		} else {
	                        			String userName = userInfos[0];
	                            		String authorizedReaders[] = userInfos[1].split(",");
	                            		
	                            		//Check if user is authorized on this reader
	                            		if (ArrayUtils.contains(authorizedReaders, readerId)) {
	                            			//Check if DisableTransponders switch is on
	                            			if (!disableTransponders) {
	                            				OpenDoor();
	                            				logger.debug("Transponder {} is authrorized for reader {}.", new Object[] { transponderId, readerId });
	                                            eventPublisher.sendCommand(transponderEventItemName, new StringType(String.format("OK|%s|%s|%s", transponderId, userName, readerId)));
	                            			} else {
	                            				logger.debug("Transponder {} is authrorized for reader {} but transponder access is disabled.",
	                            						new Object[] { transponderId, readerId });
	                            				eventPublisher.sendCommand(transponderEventItemName, new StringType(String.format("DISABLED|%s|%s|%s", transponderId, userName, readerId)));
	                            			}
	                            		} else {
	                            			logger.debug("Transponder {} is not authrorized for reader {}.", new Object[] { transponderId, readerId });
	                            			eventPublisher.sendCommand(transponderEventItemName, new StringType(String.format("NOK|%s|%s", transponderId, readerId)));
	                            		}
	                        		}
	                    		} else {
	                    			logger.debug("Transponder {} is not registred (reader {}).", new Object[] { transponderId, readerId });
	                    			eventPublisher.sendCommand(transponderEventItemName, new StringType(String.format("NOK|%s|%s", transponderId, readerId)));
	                    		}
	                    	}
	                    }
	    			}
                } while (inputStream.available() > 0);
			} catch (IOException e) {
				logger.error("Error receiving data on serial port {}: {}", new Object[] { port, e.getMessage() });
			} catch (TransformationException e) {
				logger.error("Error transforming data on serial port {}: {}", new Object[] { port, e.getMessage() });
			} catch (Exception e) {
				logger.error("Error on serial port {}: {}", new Object[] { port, e.getMessage() });
			}
			
			break;
		}
	}
	
	public void OpenDoor() {
		Thread openDoorThread = new Thread() {
		    public void run() {
		        try {
		        	String relaisOpenCommand;
		        	String relaisCloseCommand;
		        	
		        	if (readerType.equals("leser7plus")) {
		        		relaisOpenCommand = leser7plusOpenRelaisCommand;
		        		relaisCloseCommand = leser7plusCloseRelaisCommand;
    				} else if (readerType.equals("leser9")) {
    					relaisOpenCommand = leser9OpenRelaisCommand;
		        		relaisCloseCommand = leser9CloseRelaisCommand;
    				} else {
    					logger.error("Invalid reader type: " + readerType);
    					return;
    				}
		        	
		        	outputStream.write(relaisOpenCommand.getBytes());
		            outputStream.flush();
		            logger.debug("Relais open command sent.");
		            
		            try { Thread.sleep(1000);
		    		} catch (InterruptedException e) {}
		            
		            outputStream.write(relaisCloseCommand.getBytes());
		            outputStream.flush();
		            logger.debug("Relais close command sent.");
		            
		            if (readerType.equals("leser7plus")) {
			            //Send close command a second time to be sure the relays is closed
			            try { Thread.sleep(1000);
			    		} catch (InterruptedException e) {}
			            
			            outputStream.write(relaisCloseCommand.getBytes());
			            outputStream.flush();
			            logger.debug("Relais close command sent.");
		            }
		        } catch(Exception e) {
		        	logger.error("Error sending OpenDoor command on serial port {}: {}", new Object[] { getPort(), e.getMessage() });
		        }
		    }  
		};

		openDoorThread.start();
	}
		
	public void DisableTransponders(boolean disable) {
		disableTransponders = disable;
	}

	/**
	 * Close this serial device
	 */
	public void close() {
		serialPort.removeEventListener();
		IOUtils.closeQuietly(inputStream);
		IOUtils.closeQuietly(outputStream);
		serialPort.close();
	}
}
