package com.google.android.DemoKit;

public class AccessoryPacket {

	int cmd;
	byte [] payld;
	int length;
	int crc;
	String hexfile;
	
	public int checkCrc() {
		return 0;
	}
	
	public int calcCrc() {
		int local_crc;
		local_crc = cmd + length;
		for(int i = 0; i < length; i++) {
			local_crc += ((int) payld[i] & 0x00ff);
		}
		return local_crc;
	}
	
}
