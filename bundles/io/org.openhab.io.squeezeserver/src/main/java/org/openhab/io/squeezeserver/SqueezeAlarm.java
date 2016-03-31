package org.openhab.io.squeezeserver;

public class SqueezeAlarm implements Comparable<SqueezeAlarm> {

	private String id;
	private boolean enabled;
	private int hours;
	private int minutes;
	private boolean monday;
	private boolean tuesday;
	private boolean wednesday;
	private boolean thursday;
	private boolean friday;
	private boolean saturday;
	private boolean sunday;

	public int getHours() {
		return hours;
	}

	public void setHours(int hours) {
		this.hours = hours;
	}

	public int getMinutes() {
		return minutes;
	}

	public void setMinutes(int minutes) {
		this.minutes = minutes;
	}

	public boolean isMonday() {
		return monday;
	}

	public void setMonday(boolean monday) {
		this.monday = monday;
	}

	public boolean isTuesday() {
		return tuesday;
	}

	public void setTuesday(boolean tuesday) {
		this.tuesday = tuesday;
	}

	public boolean isWednesday() {
		return wednesday;
	}

	public void setWednesday(boolean wednesday) {
		this.wednesday = wednesday;
	}

	public boolean isThursday() {
		return thursday;
	}

	public void setThursday(boolean thursday) {
		this.thursday = thursday;
	}

	public boolean isFriday() {
		return friday;
	}

	public void setFriday(boolean friday) {
		this.friday = friday;
	}

	public boolean isSaturday() {
		return saturday;
	}

	public void setSaturday(boolean saturday) {
		this.saturday = saturday;
	}

	public boolean isSunday() {
		return sunday;
	}

	public void setSunday(boolean sunday) {
		this.sunday = sunday;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public int compareTo(SqueezeAlarm alarm) {
		if (this.id != alarm.id)
			return -1;
		if (this.enabled != alarm.enabled)
			return -1;
		if (this.hours != alarm.hours)
			return -1;
		if (this.minutes != alarm.minutes)
			return -1;
		if (this.monday != alarm.monday)
			return -1;
		if (this.tuesday != alarm.tuesday)
			return -1;
		if (this.wednesday != alarm.wednesday)
			return -1;
		if (this.thursday != alarm.thursday)
			return -1;
		if (this.friday != alarm.friday)
			return -1;
		if (this.saturday != alarm.saturday)
			return -1;
		if (this.sunday != alarm.sunday)
			return -1;
		else
			return 0;
	}
}
