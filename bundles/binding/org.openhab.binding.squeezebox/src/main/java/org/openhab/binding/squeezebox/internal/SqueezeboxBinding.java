/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.squeezebox.internal;

import java.util.ArrayList;
import java.util.List;

import org.openhab.binding.squeezebox.SqueezeboxBindingConfig;
import org.openhab.binding.squeezebox.SqueezeboxBindingProvider;
import org.openhab.core.binding.AbstractBinding;
import org.openhab.core.binding.BindingProvider;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.Command;
import org.openhab.io.squeezeserver.SqueezeAlarm;
import org.openhab.io.squeezeserver.SqueezePlayer;
import org.openhab.io.squeezeserver.SqueezePlayer.PlayerEvent;
import org.openhab.io.squeezeserver.SqueezePlayerEventListener;
import org.openhab.io.squeezeserver.SqueezeServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Binding which communicates with (one or many) Squeezeboxes. 
 * 
 * @author Markus Wolters
 * @author Ben Jones
 * @since 1.3.0
 */
public class SqueezeboxBinding extends AbstractBinding<SqueezeboxBindingProvider> implements SqueezePlayerEventListener {
	
	private static final Logger logger = LoggerFactory.getLogger(SqueezeboxBinding.class);

	private SqueezeServer squeezeServer;
		
	@Override
	public void bindingChanged(final BindingProvider provider, final String itemName) {
		super.bindingChanged(provider, itemName);
		
		if (provider instanceof SqueezeboxBindingProvider) {
			//Because config and item value is not initialized yet we delay loading for 5 seconds
			new java.util.Timer().schedule( 
			        new java.util.TimerTask() {
			            @Override
			            public void run() {
			            	SqueezeboxBindingConfig config = ((SqueezeboxBindingProvider)provider).getSqueezeboxBindingConfig(itemName);
			            	updateBinding(config, itemName);
			            }
			        }, 
			        5000
			);
		}
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void internalReceiveCommand(String itemName, Command command) {
		if (squeezeServer == null) {
			logger.warn("Squeeze Server not initialised or configured yet, ignoring command '{}' for item '{}'", command.toString(), itemName);
			return;
		}

		logger.trace("internalReceiveCommand(itemname = {}, command = {})", itemName, command.toString());
		for (SqueezeboxBindingProvider provider : providers) {
			SqueezeboxBindingConfig bindingConfig = provider.getSqueezeboxBindingConfig(itemName);
			String playerId = bindingConfig.getPlayerId();		
			
			try {
				switch (bindingConfig.getCommandType()) {
					case POWER:
						if (command.equals(OnOffType.ON))
							squeezeServer.powerOn(playerId);
						else if (command.equals(OnOffType.OFF))
							squeezeServer.powerOff(playerId);						
						break;
					
					case MUTE:
						if (command.equals(OnOffType.ON))
							squeezeServer.mute(playerId);
						else if (command.equals(OnOffType.OFF))
							squeezeServer.unMute(playerId);						
						break;
					
					case VOLUME: 
						if (command.equals(IncreaseDecreaseType.INCREASE))						
							squeezeServer.volumeUp(playerId);
						else if (command.equals(IncreaseDecreaseType.DECREASE))
							squeezeServer.volumeDown(playerId); 
						else if (command.equals(UpDownType.UP))						
							squeezeServer.volumeUp(playerId);
						else if (command.equals(UpDownType.DOWN))
							squeezeServer.volumeDown(playerId); 
						else if (command instanceof DecimalType)
							squeezeServer.setVolume(playerId, ((DecimalType)command).intValue()); 
						break;

					case PLAY:
						if (command.equals(OnOffType.ON))
							squeezeServer.play(playerId);
						else if (command.equals(OnOffType.OFF))
							squeezeServer.stop(playerId);
						break;
					case PAUSE:
						if (command.equals(OnOffType.ON))
							squeezeServer.pause(playerId);
						else if (command.equals(OnOffType.OFF))
							squeezeServer.unPause(playerId);
						break;
					case STOP:
						if (command.equals(OnOffType.ON))
							squeezeServer.stop(playerId);
						else if (command.equals(OnOffType.OFF))
							squeezeServer.play(playerId);
						break;
					case NEXT:
						if (command.equals(OnOffType.ON))
							squeezeServer.next(playerId);
						break;						
					case PREV:
						if (command.equals(OnOffType.ON))
							squeezeServer.prev(playerId);
						break;
						
					case HTTP: 
						if (command.equals(OnOffType.ON))
							squeezeServer.playUrl(playerId, "http://" + bindingConfig.getExtra()); 
						else if (command.equals(OnOffType.OFF))
							squeezeServer.stop(playerId);
						break;
					case FILE: 
						if (command.equals(OnOffType.ON))
							squeezeServer.playUrl(playerId, "file://" + bindingConfig.getExtra()); 
						else if (command.equals(OnOffType.OFF))
							squeezeServer.stop(playerId);
						break;
					
					case SYNC: 
						if (command.equals(OnOffType.ON))
							squeezeServer.syncPlayer(playerId, bindingConfig.getExtra()); 
						else if (command.equals(OnOffType.OFF))
							squeezeServer.unSyncPlayer(bindingConfig.getExtra());
						break;
					case ALARMSENABLED:
						if (command.equals(OnOffType.ON))
							squeezeServer.setAlarmsEnabled(playerId, 1); 
						else if (command.equals(OnOffType.OFF))
							squeezeServer.setAlarmsEnabled(playerId, 0);
						break;
					case ALARM:
						try {
							int index = Integer.parseInt(bindingConfig.getExtra());
							if (command.equals(OnOffType.ON))
								squeezeServer.setAlarmEnabled(playerId, index, 1);
							else if (command.equals(OnOffType.OFF))
								squeezeServer.setAlarmEnabled(playerId, index, 0);
							break;
						} catch (NumberFormatException e) {
							logger.warn("Unsupported alarm index '{}'", bindingConfig.getExtra()); 
						}
					case COMMAND:
					    if (command instanceof StringType)
					    	squeezeServer.playerCommand(playerId, command.toString());
					    else
					    	squeezeServer.playerCommand(playerId, bindingConfig.getExtra());
					    break;

					default:
						logger.warn("Unsupported command type '{}'", bindingConfig.getCommandType()); 
				}
			}
			catch (Exception e) {
				logger.warn("Error executing command type '" + bindingConfig.getCommandType() + "'", e);
			}	
		}
	}
		
	private void updateBinding(SqueezeboxBindingConfig config, String itemName) {
		if (squeezeServer == null) {
			logger.warn("Squeeze Server not initialised or configured yet, ignoring binding config for item '{}'", itemName);
			return;
		}
		
		logger.debug("Loading value for binding '{}'", itemName);
		
		SqueezePlayer player = squeezeServer.getPlayer(config.getPlayerId());
		
		switch (config.getCommandType()) {
			case POWER:
				booleanChangeEvent(player.getPlayerId(), CommandType.POWER, player.isPowered());
				break;
			case MUTE:
				booleanChangeEvent(player.getPlayerId(), CommandType.MUTE, player.isMuted());
				break;
			case VOLUME:
				numberChangeEvent(player.getPlayerId(), CommandType.VOLUME, player.getVolume());
				break;
			case CURRTRACK:
				numberChangeEvent(player.getPlayerId(), CommandType.CURRTRACK, player.getCurrentPlaylistIndex());
				break;
			case PLAYTIME:
				numberChangeEvent(player.getPlayerId(), CommandType.PLAYTIME, player.getCurrentPlayingTime());
				break;
			case NUMTRACKS:
				numberChangeEvent(player.getPlayerId(), CommandType.NUMTRACKS, player.getNumberPlaylistTracks());
				break;
			case SHUFFLE:
				numberChangeEvent(player.getPlayerId(), CommandType.SHUFFLE, player.getCurrentPlaylistShuffle());
				break;
			case REPEAT:
				numberChangeEvent(player.getPlayerId(), CommandType.REPEAT, player.getCurrentPlaylistRepeat());
				break;
			case PLAY:
				booleanChangeEvent(player.getPlayerId(), CommandType.PLAY, player.isPlaying());
				break;
			case PAUSE:
				booleanChangeEvent(player.getPlayerId(), CommandType.PAUSE, player.isPaused());
				break;
			case STOP:
				booleanChangeEvent(player.getPlayerId(), CommandType.STOP, player.isStopped());
				break;
			case TITLE:
				stringChangeEvent(player.getPlayerId(), CommandType.TITLE, player.getTitle());
				break;
			case ALBUM:
				stringChangeEvent(player.getPlayerId(), CommandType.ALBUM, player.getAlbum());
				break;
			case ARTIST:
				stringChangeEvent(player.getPlayerId(), CommandType.ARTIST, player.getArtist());
				break;
			case COVERART:
				stringChangeEvent(player.getPlayerId(), CommandType.COVERART, player.getCoverArt());
				break;
			case YEAR:
				stringChangeEvent(player.getPlayerId(), CommandType.YEAR, Integer.toString(player.getYear()));
				break;
			case GENRE:
				stringChangeEvent(player.getPlayerId(), CommandType.GENRE, player.getGenre());
				break;
			case REMOTETITLE:
				stringChangeEvent(player.getPlayerId(), CommandType.REMOTETITLE, player.getRemoteTitle());
				break;
			case IRCODE:
				stringChangeEvent(player.getPlayerId(), CommandType.IRCODE, player.getIrCode());
				break;
			case ALARMSENABLED:
				booleanChangeEvent(player.getPlayerId(), CommandType.ALARMSENABLED, player.getAlarmsEnabled());
				break;
			case ALARM:
				List<SqueezeAlarm> alarms = player.getAlarms();
				for (int i = 0 ; i < alarms.size() ; i++) {
					booleanChangeEvent(player.getPlayerId(), CommandType.ALARM, String.valueOf(i), alarms.get(i).isEnabled());
				}
				break;
			default:
				break;
		}
	}

	@Override
	public void powerChangeEvent(PlayerEvent event) {
		booleanChangeEvent(event.getPlayerId(), CommandType.POWER, event.getPlayer().isPowered());
	}

	@Override
	public void muteChangeEvent(PlayerEvent event) {
		booleanChangeEvent(event.getPlayerId(), CommandType.MUTE, event.getPlayer().isMuted());
	}
	
	@Override
	public void volumeChangeEvent(PlayerEvent event) {
		numberChangeEvent(event.getPlayerId(), CommandType.VOLUME, event.getPlayer().getVolume());
	}
	
	@Override
	public void currentPlaylistIndexEvent(PlayerEvent event) {
		numberChangeEvent(event.getPlayerId(), CommandType.CURRTRACK, event.getPlayer().getCurrentPlaylistIndex());
	}
	
	@Override
	public void currentPlayingTimeEvent(PlayerEvent event) {
		numberChangeEvent(event.getPlayerId(), CommandType.PLAYTIME, event.getPlayer().getCurrentPlayingTime());
	}
	
	@Override
	public void numberPlaylistTracksEvent(PlayerEvent event) {
		numberChangeEvent(event.getPlayerId(), CommandType.NUMTRACKS, event.getPlayer().getNumberPlaylistTracks());
	}
	@Override
	public void currentPlaylistShuffleEvent(PlayerEvent event) {
		numberChangeEvent(event.getPlayerId(), CommandType.SHUFFLE, event.getPlayer().getCurrentPlaylistShuffle());
	}
	@Override
	public void currentPlaylistRepeatEvent(PlayerEvent event) {
		numberChangeEvent(event.getPlayerId(), CommandType.REPEAT, event.getPlayer().getCurrentPlaylistRepeat());
	}
	@Override
	public void modeChangeEvent(PlayerEvent event) {
		booleanChangeEvent(event.getPlayerId(), CommandType.PLAY, event.getPlayer().isPlaying());
		booleanChangeEvent(event.getPlayerId(), CommandType.PAUSE, event.getPlayer().isPaused());
		booleanChangeEvent(event.getPlayerId(), CommandType.STOP, event.getPlayer().isStopped());
	}

	@Override
	public void titleChangeEvent(PlayerEvent event) {
		stringChangeEvent(event.getPlayerId(), CommandType.TITLE, event.getPlayer().getTitle());
	}
		
	@Override
	public void albumChangeEvent(PlayerEvent event) {
		stringChangeEvent(event.getPlayerId(), CommandType.ALBUM, event.getPlayer().getAlbum());
	}

	@Override
	public void artistChangeEvent(PlayerEvent event) {
		stringChangeEvent(event.getPlayerId(), CommandType.ARTIST, event.getPlayer().getArtist());
	}

	@Override
	public void coverArtChangeEvent(PlayerEvent event) {
		stringChangeEvent(event.getPlayerId(), CommandType.COVERART, event.getPlayer().getCoverArt());
	}

	@Override
	public void yearChangeEvent(PlayerEvent event) {
		stringChangeEvent(event.getPlayerId(), CommandType.YEAR, Integer.toString(event.getPlayer().getYear()));
	}

	@Override
	public void genreChangeEvent(PlayerEvent event) {
		stringChangeEvent(event.getPlayerId(), CommandType.GENRE, event.getPlayer().getGenre());
	}

	@Override
	public void remoteTitleChangeEvent(PlayerEvent event) {
		stringChangeEvent(event.getPlayerId(), CommandType.REMOTETITLE, event.getPlayer().getRemoteTitle());
	}
	
	@Override
	public void irCodeChangeEvent(PlayerEvent event) {
		stringChangeEvent(event.getPlayerId(), CommandType.IRCODE, event.getPlayer().getIrCode());
	}
	
	@Override
	public void alarmsEnabledChangeEvent(PlayerEvent event) {
		booleanChangeEvent(event.getPlayerId(), CommandType.ALARMSENABLED, event.getPlayer().getAlarmsEnabled());
	}
	
	@Override
	public void alarmsChangeEvent(PlayerEvent event) {
		List<SqueezeAlarm> alarms = event.getPlayer().getAlarms();
		for (int i = 0 ; i < alarms.size() ; i++) {
			booleanChangeEvent(event.getPlayerId(), CommandType.ALARM, String.valueOf(i), alarms.get(i).isEnabled());
		}
	}
	
	private void stringChangeEvent(String playerId, CommandType commandType, String newState) {
		logger.debug("SqueezePlayer " + playerId + " -> " + commandType.getCommand() + ": " + newState);
		for (String itemName : getItemNames(playerId, commandType)) {
			eventPublisher.postUpdate(itemName, StringType.valueOf(newState));
		}
	}
	
	private void numberChangeEvent(String playerId, CommandType commandType, int newState) {
		if (newState < 0) {
			logger.debug("SqueezePlayer " + playerId + " value not initialized -> " + commandType.getCommand() + ": " + Integer.toString(newState));
			return;
		}
		
		logger.debug("SqueezePlayer " + playerId + " -> " + commandType.getCommand() + ": " + Integer.toString(newState));
		for (String itemName : getItemNames(playerId, commandType)) {
			eventPublisher.postUpdate(itemName, new PercentType(newState));
		}
	}
	
	private void booleanChangeEvent(String playerId, CommandType commandType, boolean newState) {
		logger.debug("SqueezePlayer " + playerId + " -> " + commandType.getCommand() + ": " + Boolean.toString(newState));
		for (String itemName : getItemNames(playerId, commandType)) {
			if (newState) {
				eventPublisher.postUpdate(itemName, OnOffType.ON);
			} else {
				eventPublisher.postUpdate(itemName, OnOffType.OFF);
			}
		}
	}
	
	private void booleanChangeEvent(String playerId, CommandType commandType, String extra, boolean newState) {
		logger.debug("SqueezePlayer " + playerId + " -> " + commandType.getCommand() + ": " + Boolean.toString(newState));
		for (String itemName : getItemNames(playerId, commandType, extra)) {
			if (newState) {
				eventPublisher.postUpdate(itemName, OnOffType.ON);
			} else {
				eventPublisher.postUpdate(itemName, OnOffType.OFF);
			}
		}
	}

	private List<String> getItemNames(String playerId, CommandType commandType) {
		List<String> itemNames = new ArrayList<String>();
		for (SqueezeboxBindingProvider provider : this.providers) {
			for (String itemName : provider.getItemNames()) {
				SqueezeboxBindingConfig bindingConfig = provider.getSqueezeboxBindingConfig(itemName);
				if (!bindingConfig.getPlayerId().equals(playerId))
					continue;
				if (!bindingConfig.getCommandType().equals(commandType))
					continue;
				itemNames.add(itemName);
			}
		}
		return itemNames;
	}
	
	private List<String> getItemNames(String playerId, CommandType commandType, String extra) {
		List<String> itemNames = new ArrayList<String>();
		for (SqueezeboxBindingProvider provider : this.providers) {
			for (String itemName : provider.getItemNames()) {
				SqueezeboxBindingConfig bindingConfig = provider.getSqueezeboxBindingConfig(itemName);
				if (!bindingConfig.getPlayerId().equals(playerId))
					continue;
				if (!bindingConfig.getCommandType().equals(commandType))
					continue;
				if (!bindingConfig.getExtra().equals(extra))
					continue;
				itemNames.add(itemName);
			}
		}
		return itemNames;
	}
	
	/**
	 * Setter for Declarative Services. Adds the SqueezeServer instance.
	 * 
	 * @param squeezeServer
	 *            Service.
	 */
	public void setSqueezeServer(SqueezeServer squeezeServer) {
		this.squeezeServer = squeezeServer;
		setInitialValues();
		this.squeezeServer.addPlayerEventListener(this);
	}

	/**
	 * Unsetter for Declarative Services.
	 * 
	 * @param squeezeServer
	 *            Service to remove.
	 */
	public void unsetSqueezeServer(SqueezeServer squeezeServer) {
		this.squeezeServer.removePlayerEventListener(this);
		this.squeezeServer = null;
	}
	
	private void setInitialValues() {
		try {
			for (SqueezeboxBindingProvider provider : providers) {
				if (provider instanceof SqueezeboxBindingProvider) {
					for (String itemName : provider.getItemNames()) {
						SqueezeboxBindingConfig config = ((SqueezeboxBindingProvider)provider).getSqueezeboxBindingConfig(itemName);						
						updateBinding(config, itemName);
					}
				}
				
			}
		} catch (Exception e) {
			logger.error(e.toString());
		}
	}
}
