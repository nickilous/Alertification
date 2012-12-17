package com.nickilous.alertification;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class TextMessage {
    private String mSender;
    private String mMessage;

    public static final String TEXT_MESSAGE_RECEIVED = "com.nickilous.TEXT_MESSAGE_RECEIVED";
    public static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    public TextMessage() {
        mSender = null;
        mMessage = null;
    }

    public TextMessage(String textMessage) {
        String[] textMessageParts = textMessage.split(";");
        mSender = textMessageParts[0];
        mMessage = textMessageParts[1];
    }

    public TextMessage(Intent intent) {
        // Get the SMS map from Intent
        Bundle extras = intent.getExtras();

        if (extras != null) {
            // Get received SMS array
            Object[] smsExtra = (Object[]) extras.get("pdus");

            for (Object element : smsExtra) {
                SmsMessage sms = SmsMessage.createFromPdu((byte[]) element);

                mMessage = sms.getMessageBody().toString();
                mSender = sms.getOriginatingAddress();

            }

        }

    }

    /**
     * @return the mSender
     */
    public String getSender() {
        return mSender;
    }

    /**
     * @param mSender the mSender to set
     */
    public void setSender(String mSender) {
        this.mSender = mSender;
    }

    /**
     * @return the mMessage
     */
    public String getMessage() {
        return mMessage;
    }

    /**
     * @param mMessage the mMessage to set
     */
    public void setMessage(String mMessage) {
        this.mMessage = mMessage;
    }

    @Override
    public String toString() {
        // TODO: turn text message into string
        return mSender + ";" + mMessage;
    }

}
