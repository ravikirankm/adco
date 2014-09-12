package com.google.android.DemoKit;

public class AccessoryPacket {

	int cmd;
	int [] payld;
	int length;
	String hexfile;
	int crc;
	
	public int checkCrc() {
		return 0;
	}
	
	public int calcCrc() {
		return 0;
	}
	
}
