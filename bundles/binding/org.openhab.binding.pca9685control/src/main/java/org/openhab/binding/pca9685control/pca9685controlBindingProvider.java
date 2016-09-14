/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.pca9685control;

import java.util.TreeMap;

import org.openhab.binding.pca9685control.internal.PCA9685PwmControl;
import org.openhab.binding.pca9685control.internal.pca9685controlGenericBindingProvider.pca9685controlConfig;
import org.openhab.core.binding.BindingProvider;


/**
 * @author MichaelP
 * @since 1.0
 */
public interface pca9685controlBindingProvider extends BindingProvider {
	/**
	 * Map of all configured PCA9685 boards. 
	 * Key=I2CAddress, Value=PCA9685PwmControl Object.
	 * If all given items have the same I2C Address, it should exist only one Map-Entry).    
	 * @return Returns the map with all configured Boards.
	 */
	public TreeMap<Integer, PCA9685PwmControl> getPCA9685Map();
	
	/**
	 * Get the I2C Address of the given Item
	 * @param itemName Name of the Item 
	 * @return Returns the I2C Address of the Item.
	 */
	public pca9685controlConfig getConfig(String itemName);
	
	/**
	 * Is the given Item already configured?
	 * @param itemName Name of the Item 
	 * @return Returns if the Item is configured. (true = is configured)
	 */
	public boolean isItemConfigured(String itemName);
}
