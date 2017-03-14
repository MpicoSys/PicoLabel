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

import javacard.framework.Applet;
import javacard.framework.ISOException;
import javacard.framework.ISO7816;
import javacard.framework.APDU;
import javacard.framework.JCSystem;
import PicoLabel.DESTest;
import PicoLabel.RNDTest;
import PicoLabel.RSATest;

/* Tom's info: private void generateKey(APDU apdu) {
private void getPublicKey(APDU apdu) {
private void signCert(APDU apdu) {
private void mutualChallengeResponse1(APDU apdu) {
private void generateRandomNum(APDU apdu) {
*/

public class CryptoTester extends Applet {
	
	private static byte VERSION = (byte)0x01;
	
	private static final byte CLA_DISPCFG = (byte)0xBF;
	private static final byte CLA_VERSION = (byte)0xBE;
	
	private static final byte CLA_RSA = (byte)0xB0;
	private static final byte CLA_DES = (byte)0xB1;
	private static final byte CLA_RND = (byte)0xB2;
	
	private static final byte CLA_AUTHCHECK = (byte)0xB5;
	private static final byte CLA_AUTHSET = (byte)0x12;
	
	private RSATest rsa_test;
	private DESTest des_test;
	private RNDTest rnd_test;
	
	private byte[] AuthenticationStatus;
	
	public static void install(byte[] bArray, short bOffset, byte bLength) {
		// GP-compliant JavaCard applet registration
        new CryptoTester(bArray, (short) (bOffset + 1), bArray[bOffset]);	
	}

	private CryptoTester(byte[] bArray, short bOffset, byte bLength) {
		rsa_test = new RSATest();
		des_test = new DESTest();
		rnd_test = new RNDTest();
		AuthenticationStatus = JCSystem.makeTransientByteArray((short) 2, JCSystem.CLEAR_ON_DESELECT);
		AuthenticationStatus[0] = (byte) 0x00;
		register(bArray, bOffset, bLength);
	}
	
	public void process(APDU apdu) {

		byte[] buffer = apdu.getBuffer();
		// Good practice: Return 9000 on SELECT
		if (selectingApplet()) {
			return;
		}		
		
		switch (buffer[ISO7816.OFFSET_CLA]) {
			case (byte) CLA_RSA:
				rsa_test.process(apdu);
				return;
			case (byte) CLA_DES:
				des_test.process(apdu);
				return;
			case (byte) CLA_RND:
				rnd_test.process(apdu);
				return;

			case (byte) CLA_VERSION:
				//return application version (form compatibility check)
				buffer[0] = VERSION;
				apdu.setOutgoingAndSend((short)0, (short)1);
				return;

			case (byte) CLA_AUTHSET:
				AuthenticationStatus[0] = (byte)0x01;
				return; //Return 9000
			case (byte) CLA_AUTHCHECK:
				if ( AuthenticationStatus[0] >0 ) {
					return;
				} else {
					ISOException.throwIt( (short) 0x9804 );
				}
				
			default: 
				ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		}
	}

}
	







