/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.leserplus.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openhab.core.events.AbstractEventSubscriber;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.openhab.model.item.binding.BindingConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This class implements a binding of serial devices to openHAB.
 * The binding configurations are provided by the {@link GenericItemProvider}.</p>
 * 
 * <p>The format of the binding configuration is simple and looks like this:</p>
 * serial="&lt;port&gt;" where &lt;port&gt; is the identification of the serial port on the host system, e.g.
 * "COM1" on Windows, "/dev/ttyS0" on Linux or "/dev/tty.PL2303-0000103D" on Mac
 * <p>Switch items with this binding will receive an ON-OFF update on the bus, whenever data becomes available on the serial interface<br/>
 * String items will receive the submitted data in form of a string value as a status update, while openHAB commands to a Switch item is
 * sent out as data through the serial interface.</p>
 * 
 * @author Kai Kreuzer
 *
 */
public class LeserPlusBinding extends AbstractEventSubscriber implements BindingConfigReader {
	
	private static final Logger logger = LoggerFactory.getLogger(LeserPlusBinding.class);

	private Map<String, LeserPlusDevice> leserPlusDevices = new HashMap<String, LeserPlusDevice>();

	/** stores information about the which items are associated to which port. The map has this content structure: itemname -> port */ 
	private Map<String, String> itemMap = new HashMap<String, String>();

	/** stores information about the context of items. The map has this content structure: context -> Set of itemNames */ 
	private Map<String, Set<String>> contextMap = new HashMap<String, Set<String>>();

	/** stores information about the which items are associated to which command. The map has this content structure: itemname -> command */ 
	private Map<String, LeserPlusCommand> itemCommandMap = new HashMap<String, LeserPlusCommand>();
	
	private EventPublisher eventPublisher = null;

	private TransformationService transformationService;
	
	public void setEventPublisher(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;

		for(LeserPlusDevice leserPlusDevice : leserPlusDevices.values()) {
			leserPlusDevice.setEventPublisher(eventPublisher);
		}
	}

	public void unsetEventPublisher(EventPublisher eventPublisher) {
		this.eventPublisher = null;

		for(LeserPlusDevice leserPlusDevice : leserPlusDevices.values()) {
			leserPlusDevice.setEventPublisher(null);
		}
	}
	
	public void setTransformationService(TransformationService transformationService) {
		this.transformationService = transformationService;
		for(LeserPlusDevice leserPlusDevice : leserPlusDevices.values()) {
			leserPlusDevice.setTransformationService(transformationService);
		}
	}

	public void unsetTransformationService(TransformationService transformationService) {
		this.transformationService = null;
		for(LeserPlusDevice leserPlusDevice : leserPlusDevices.values()) {
			leserPlusDevice.setTransformationService(null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void receiveCommand(String itemName, Command command) {
		try {
			if(itemMap.keySet().contains(itemName)) {
				logger.debug(String.format("Received command %s on item %s", command.toString(), itemName));
				LeserPlusDevice leserPlusDevice = leserPlusDevices.get(itemMap.get(itemName));
				if(command instanceof OnOffType) {
					OnOffType item = ((OnOffType) command);
					if (itemCommandMap.containsKey(itemName)) {
						switch (itemCommandMap.get(itemName))
						{
							case OPENDOOR:
								if (item.equals(OnOffType.ON)) {
									logger.debug("Sending OpenDoor command on serial port {}", new Object[] { leserPlusDevice.getPort() });
									leserPlusDevice.OpenDoor();
									eventPublisher.postUpdate(itemName, OnOffType.OFF);
								}

								break;
							case DISABLETRANSPONDER:
								logger.debug("DisableTransponder command received for port {}: value {}",
										new Object[] { leserPlusDevice.getPort(), item.equals(OnOffType.ON) });
								leserPlusDevice.DisableTransponders(item.equals(OnOffType.ON));
								break;
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error(e.toString());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void receiveUpdate(String itemName, State newStatus) {
		logger.debug("Received update, doing nothing.");
	}

	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "leserplus";
	}

	/**
	 * {@inheritDoc}
	 */
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		if (!(item instanceof SwitchItem || item instanceof StringItem)) {
			throw new BindingConfigParseException("item '" + item.getName()
					+ "' is of type '" + item.getClass().getSimpleName()
					+ "', only SwitchItem and StringItems are allowed - please check your *.items configuration");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {

		String configParts[] = bindingConfig.split(",");

		String port = configParts[0];
		
		LeserPlusDevice leserPlusDevice = leserPlusDevices.get(port);
		if (leserPlusDevice == null) {
			leserPlusDevice = new LeserPlusDevice(port);

			leserPlusDevice.setTransformationService(transformationService);
			leserPlusDevice.setEventPublisher(eventPublisher);
			try {
				leserPlusDevice.initialize();
			} catch (InitializationException e) {
				throw new BindingConfigParseException(
						"Could not open serial port " + port + ": "
								+ e.getMessage());
			} catch (Throwable e) {
				throw new BindingConfigParseException(
						"Could not open serial port " + port + ": "
								+ e.getMessage());
			}
			itemMap.put(item.getName(), port);
			leserPlusDevices.put(port, leserPlusDevice);
		} else {
			itemMap.put(item.getName(), port);
		}
		
		if (item instanceof StringItem) {
			if (leserPlusDevice.getTransponderEventItemName() == null) {
				leserPlusDevice.setTransponderEventItemName(item.getName());
			} else {
				throw new BindingConfigParseException(
						"There is already another StringItem assigned to serial port "
								+ port);
			}
		} else if (item instanceof SwitchItem) {
			if (configParts.length == 1)
				throw new BindingConfigParseException("SwitchItems require additional argument.");
			
			String type = configParts[1];
						
			if (type.equals("OpenDoor")) {
				if (leserPlusDevice.getOpenDoorItemName() == null) {
					leserPlusDevice.setOpenDoorItemName(item.getName());
					itemCommandMap.put(item.getName(), LeserPlusCommand.OPENDOOR);
				} else {
					throw new BindingConfigParseException(
							"There is already another OpenDoor SwitchItem assigned to serial port "
									+ port);
				}
			} else if (type.equals("DisableTransponder")) {
				if (leserPlusDevice.getDisableTranspondersItemName() == null) {
					leserPlusDevice.setDisableTranspondersItemName(item.getName());
					itemCommandMap.put(item.getName(), LeserPlusCommand.DISABLETRANSPONDER);
				} else {
					throw new BindingConfigParseException(
							"There is already another DisableTransponder SwitchItem assigned to serial port "
									+ port);
				}
			}
		}
		
		Set<String> itemNames = contextMap.get(context);
		if (itemNames == null) {
			itemNames = new HashSet<String>();
			contextMap.put(context, itemNames);
		}
		itemNames.add(item.getName());
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeConfigurations(String context) {
		Set<String> itemNames = contextMap.get(context);
		if(itemNames!=null) {
			for(String itemName : itemNames) {
				// we remove all information in the serial devices
				LeserPlusDevice leserPlusDevice = leserPlusDevices.get(itemMap.get(itemName));
				itemMap.remove(itemName);
				itemCommandMap.remove(itemName);
				if(leserPlusDevice==null) {
					continue;
				}
				
				leserPlusDevice.close();
				leserPlusDevices.remove(leserPlusDevice.getPort());
			}
			contextMap.remove(context);
		}
	}

}
