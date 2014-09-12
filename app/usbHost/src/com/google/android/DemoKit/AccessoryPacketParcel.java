package com.google.android.DemoKit;

import android.os.Parcel;
import android.os.Parcelable;

public class AccessoryPacketParcel implements Parcelable {

	byte cmd;
	byte [] payld;
	int length;
	
	String hexfile;
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeByte(cmd);
		dest.writeInt(length);
		dest.writeByteArray(payld);
		if(cmd == MegaADKController.USB_CMD_SEND_HEX) {
			dest.writeString(hexfile);
		}
	}

}
