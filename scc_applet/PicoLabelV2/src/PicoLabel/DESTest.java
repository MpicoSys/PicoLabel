/*
 * Copyright (c) 2017, MpicoSys
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, 
 *  this list of conditions and the following disclaimer.
 *  
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 *  this list of conditions and the following disclaimer in the documentation 
 *  and/or other materials provided with the distribution.
 *  
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *  may be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT 
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT 
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package PicoLabel;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.CryptoException;
import javacard.security.DESKey;
import javacard.security.KeyBuilder;
import javacardx.crypto.Cipher;

class DESTest {
	/* http://stackoverflow.com/questions/30148089/java-card-des-generator-applet-output-is-different-from-online-tools-output
	 * 
	 * */
	
	private static byte[] DISPLAY_DES_START = {(byte)'D',(byte)'E',(byte)'S',
		(byte)'-',(byte)'B',(byte)'G',(byte)'N'};
	private static byte[] DISPLAY_DES_END = {(byte)'D',(byte)'E',(byte)'S',
		(byte)'-',(byte)'E',(byte)'N',(byte)'D'};

    // Array for the encryption/decryption key
    private byte[] TheDES_Key = { (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00 };

    // Defining required Keys
    DESKey MyDES1Key = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES,
            KeyBuilder.LENGTH_DES, false);
    DESKey MyDES2Key = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES,
            KeyBuilder.LENGTH_DES3_2KEY, false);
    DESKey MyDES3Key = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES,
            KeyBuilder.LENGTH_DES3_3KEY, false);

    byte ConfiguredKeyLength;

    // Defining required cipher
    Cipher MyCipher;

    // Defining switch case variables for supported instructions = INS in APDU command
    final byte SetKey = (byte) 0xC0;
    final byte OneKeyDES = (byte) 0xC1;
    final byte TwoKeyDES = (byte) 0xC2;
    final byte ThreeKeyDES = (byte) 0xC3;

    // Defining switch case variables for cipher algorithms = P1 in APDU command
    final byte DES_CBC_ISO9797_M1 = (byte) 0x00;
    final byte DES_CBC_ISO9797_M2 = (byte) 0x01;
    final byte DES_CBC_NOPAD = (byte) 0x02;
    final byte DES_CBC_PKCS5 = (byte) 0x03;
    final byte DES_ECB_ISO9797_M1 = (byte) 0x04;
    final byte DES_ECB_ISO9797_M2 = (byte) 0x05;
    final byte DES_ECB_NOPAD = (byte) 0x06;
    final byte DES_ECB_PKCS5 = (byte) 0x07;

    // Defining Proprietary Status Words
    final short KeyInNotSetGood = 0x6440;

    // A flag to be sure that the configured key has the same length that the
    // algorithm needs.

    public DESTest() {

    }

    public void process(APDU apdu) throws ISOException {

        byte[] buffer = apdu.getBuffer();
        // Checking the P1 and P2 fields in the APDU command.
        if (buffer[ISO7816.OFFSET_P1] > 7 || buffer[ISO7816.OFFSET_P2] > 1) {
            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }

        // Analyzing the command.
        try {

            switch (buffer[ISO7816.OFFSET_INS]) {

            case SetKey:
                SetCryptoKeyAndInitCipher(apdu);
                break;

            case OneKeyDES:
                OneKeyDESCrypto(apdu);
                DoEncryptDecrypt(apdu);
                break;

            case TwoKeyDES:
                TwoKeyDESCrypto(apdu);
                DoEncryptDecrypt(apdu);
                break;

            case (byte) ThreeKeyDES:
                ThreeKeyDESCrypto(apdu);
                DoEncryptDecrypt(apdu);
                break;

            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);

            }

        } catch (CryptoException e) {
            ISOException.throwIt(((CryptoException) e).getReason());
        }

    }

    public void SetCryptoKeyAndInitCipher(APDU apdu)
            throws ISOException {
        byte[] buffer = apdu.getBuffer();
        // Key must has a length of 8, 16 or 24 bytes
        if (buffer[ISO7816.OFFSET_LC] == 8 || buffer[ISO7816.OFFSET_LC] == 16
                || buffer[ISO7816.OFFSET_LC] == 24) {
            Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, TheDES_Key,
                    (short) 0, buffer[ISO7816.OFFSET_LC]);

            ConfiguredKeyLength = buffer[ISO7816.OFFSET_LC];

        } else {
            ISOException.throwIt(ISO7816.SW_DATA_INVALID);
        }

        switch (buffer[ISO7816.OFFSET_P1]) {
        case DES_CBC_ISO9797_M1:
            MyCipher = Cipher.getInstance(Cipher.ALG_DES_CBC_ISO9797_M1, false);
            break;
        case DES_CBC_ISO9797_M2:
            MyCipher = Cipher.getInstance(Cipher.ALG_DES_CBC_ISO9797_M2, false);
            break;
        case DES_CBC_NOPAD:
            MyCipher = Cipher.getInstance(Cipher.ALG_DES_CBC_NOPAD, false);
            break;
        case DES_CBC_PKCS5:
            MyCipher = Cipher.getInstance(Cipher.ALG_DES_CBC_PKCS5, false);
            break;
        case DES_ECB_ISO9797_M1:
            MyCipher = Cipher.getInstance(Cipher.ALG_DES_ECB_ISO9797_M1, false);
            break;
        case DES_ECB_ISO9797_M2:
            MyCipher = Cipher.getInstance(Cipher.ALG_DES_ECB_ISO9797_M2, false);
            break;
        case DES_ECB_NOPAD:
            MyCipher = Cipher.getInstance(Cipher.ALG_DES_ECB_NOPAD, false);
            break;
        case DES_ECB_PKCS5:
            MyCipher = Cipher.getInstance(Cipher.ALG_DES_ECB_PKCS5, false);
            break;

        }

    }

    public void OneKeyDESCrypto(APDU apdu)
            throws ISOException {
        byte[] buffer = apdu.getBuffer();
        // Check to see if the configured key is the required key for this ...
        // ... algorithm or not
        if (ConfiguredKeyLength != 8) {
            ISOException.throwIt(KeyInNotSetGood);
        }
        MyDES1Key.setKey(TheDES_Key, (short) 0);

        if (buffer[ISO7816.OFFSET_P2] == 1) {
            MyCipher.init(MyDES1Key, Cipher.MODE_ENCRYPT);
        } else {
            MyCipher.init(MyDES1Key, Cipher.MODE_DECRYPT);

        }

    }

    public void TwoKeyDESCrypto(APDU apdu)
            throws ISOException {
        byte[] buffer = apdu.getBuffer();
        // Check to see if the configured key is the required key for this ...
        // ... algorithm or not

        if (ConfiguredKeyLength != 16) {
            ISOException.throwIt(KeyInNotSetGood);
        }
        MyDES2Key.setKey(TheDES_Key, (short) 0);

        if (buffer[ISO7816.OFFSET_P2] == 1) {
            MyCipher.init(MyDES2Key, Cipher.MODE_ENCRYPT);
        } else {
            MyCipher.init(MyDES2Key, Cipher.MODE_DECRYPT);

        }

    }

    public void ThreeKeyDESCrypto(APDU apdu)
            throws ISOException {
        byte[] buffer = apdu.getBuffer();
        // Check to see if the configured key is the required key for this ...
        // ... algorithm or not
        if (ConfiguredKeyLength != 24) {
            ISOException.throwIt(KeyInNotSetGood);
        }

        MyDES3Key.setKey(TheDES_Key, (short) 0);

        if (buffer[ISO7816.OFFSET_P2] == 1) {
            MyCipher.init(MyDES3Key, Cipher.MODE_ENCRYPT);
        } else {
            MyCipher.init(MyDES3Key, Cipher.MODE_DECRYPT);

        }

    }

    public void DoEncryptDecrypt(APDU apdu) {
        byte[] buffer = apdu.getBuffer();

        byte[] CipheredData = JCSystem.makeTransientByteArray((short) 32,
                JCSystem.CLEAR_ON_DESELECT);

        short datalen = apdu.setIncomingAndReceive();
        if ((datalen % 8) != 0) {
            ISOException.throwIt(ISO7816.SW_DATA_INVALID);
        }

		//Display.display_message(DISPLAY_DES_START);
        
        MyCipher.doFinal(buffer, (short) 0, datalen, CipheredData, (short) 0);
        Util.arrayCopyNonAtomic(CipheredData, (short) 0, buffer, (short) 0,
                datalen);

		//Display.display_message(DISPLAY_DES_END);

        apdu.setOutgoingAndSend((short) 0, datalen);
    }

}