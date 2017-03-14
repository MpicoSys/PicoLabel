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
import javacard.security.ECPrivateKey;
import javacard.security.ECPublicKey;
import javacard.security.KeyBuilder;
import javacard.security.KeyPair;
import javacard.security.RSAPublicKey;
import javacard.security.Signature;

class RSATest {
	
	private static final short IN_BUFF_SIZE = (short) 256;

	private static final byte INS_GEN_KEY =     (byte)0xC2;
	private static final byte INS_GETPUB_KEY_RSA =  (byte)0xC4;
	private static final byte INS_GETPUB_KEY_EC =  (byte)0xC5;
	private static final byte INS_SIGN_DATA =   (byte)0xC7;

	
	private static final byte keyALG_RSA =      (byte)0x01;		// key ALG RSA-2048
	private static final byte key_ALG_ECDSA =   (byte)0x04;		// key ALG ECDSA-256
	private static final byte[] keyRSA2048 = new byte[] {0x08, 0x00};


	private byte[] in_buf;

	
	final static short MAX_PINS = (short) 10;
	final static short MAX_KEYS = (short) 8;
//	private static OwnerPIN[] pins = new OwnerPIN[MAX_PINS];
	
	
	private static byte[] DISPLAY_RSA_KEY_GEN_START = {(byte)'R',(byte)'S',(byte)'A',
											(byte)'K',(byte)'G',(byte)'E',(byte)'N'};
	private static byte[] DISPLAY_RSA_KEY_GEN_END = {(byte)'R',(byte)'S',(byte)'A',
											(byte)'-',(byte)'E',(byte)'N',(byte)'D'};

	private static byte[] DISPLAY_EC_KEY_GEN_START = {(byte)'E',(byte)'C',(byte)' ',
											(byte)'K',(byte)'G',(byte)'E',(byte)'N'};
	private static byte[] DISPLAY_EC_KEY_GEN_END = {(byte)'E',(byte)'C',(byte)'-',
											(byte)'-',(byte)'E',(byte)'N',(byte)'D'};


	private Signature signatureRSA;
//	private Signature signatureEC384;
	
    private static KeyPair RSAKey;
    private static KeyPair ECKey;

    
//static arrays with secp256r1 curve parameters
// ec_p_256    
private final static byte[] SecP256r1_P = {
(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01,
(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF
};
// ec_a_256
private final static byte[] SecP256r1_A = {
(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01,
(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFC
};
// ec_b_256
private final static byte[] SecP256r1_B = {
(byte)0x5A, (byte)0xC6, (byte)0x35, (byte)0xD8, (byte)0xAA, (byte)0x3A, (byte)0x93, (byte)0xE7,
(byte)0xB3, (byte)0xEB, (byte)0xBD, (byte)0x55, (byte)0x76, (byte)0x98, (byte)0x86, (byte)0xBC,
(byte)0x65, (byte)0x1D, (byte)0x06, (byte)0xB0, (byte)0xCC, (byte)0x53, (byte)0xB0, (byte)0xF6,
(byte)0x3B, (byte)0xCE, (byte)0x3C, (byte)0x3E, (byte)0x27, (byte)0xD2, (byte)0x60, (byte)0x4B
};

private final static byte[] SecP256r1_S = {
(byte)0xC4, (byte)0x9D, (byte)0x36, (byte)0x08, (byte)0x86, (byte)0xE7, (byte)0x04, (byte)0x93,
(byte)0x6A, (byte)0x66, (byte)0x78, (byte)0xE1, (byte)0x13, (byte)0x9D, (byte)0x26, (byte)0xB7,
(byte)0x81, (byte)0x9F, (byte)0x7E, (byte)0x90
};
// ec_gen_256
private final static byte[] SecP256r1_G = {    // uncompressed
(byte)0x04, (byte)0x6B, (byte)0x17, (byte)0xD1, (byte)0xF2, (byte)0xE1, (byte)0x2C, (byte)0x42,
(byte)0x47, (byte)0xF8, (byte)0xBC, (byte)0xE6, (byte)0xE5, (byte)0x63, (byte)0xA4, (byte)0x40,
(byte)0xF2, (byte)0x77, (byte)0x03, (byte)0x7D, (byte)0x81, (byte)0x2D, (byte)0xEB, (byte)0x33,
(byte)0xA0, (byte)0xF4, (byte)0xA1, (byte)0x39, (byte)0x45, (byte)0xD8, (byte)0x98, (byte)0xC2,
(byte)0x96, (byte)0x4F, (byte)0xE3, (byte)0x42, (byte)0xE2, (byte)0xFE, (byte)0x1A, (byte)0x7F,
(byte)0x9B, (byte)0x8E, (byte)0xE7, (byte)0xEB, (byte)0x4A, (byte)0x7C, (byte)0x0F, (byte)0x9E,
(byte)0x16, (byte)0x2B, (byte)0xCE, (byte)0x33, (byte)0x57, (byte)0x6B, (byte)0x31, (byte)0x5E,
(byte)0xCE, (byte)0xCB, (byte)0xB6, (byte)0x40, (byte)0x68, (byte)0x37, (byte)0xBF, (byte)0x51,
(byte)0xF5
};
// ec_order_256
private final static byte[] SecP256r1_N = {
(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
(byte)0xBC, (byte)0xE6, (byte)0xFA, (byte)0xAD, (byte)0xA7, (byte)0x17, (byte)0x9E, (byte)0x84,
(byte)0xF3, (byte)0xB9, (byte)0xCA, (byte)0xC2, (byte)0xFC, (byte)0x63, (byte)0x25, (byte)0x51
};

private final static short SecP256r1_H = 1;
/*
 * A = ec_a_256
 * B = ec_b_256
 * G = ec_gen_256
 * R = ec_order_256
 */

	public RSATest() {
		in_buf = JCSystem.makeTransientByteArray(IN_BUFF_SIZE, JCSystem.CLEAR_ON_DESELECT);

		signatureRSA = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
//		signatureEC384 = Signature.getInstance(Signature.ALG_ECDSA_SHA_384, false);
		RSAKey = new KeyPair(KeyPair.ALG_RSA, KeyBuilder.LENGTH_RSA_512);
	}


	
	
	public void process(APDU apdu) {

		byte[] buffer = apdu.getBuffer();

		switch (buffer[ISO7816.OFFSET_INS]) {

			case (byte) INS_GEN_KEY: 
				generateRSAKey(apdu); 
				return;
				
			case (byte) INS_GETPUB_KEY_RSA: 
				getRSAPublicKey(apdu); 
				return;
			
			case (byte) INS_GETPUB_KEY_EC:
				//FIXME: does not work yet
				getECPublicKey(apdu); 
				return;

			case (byte) INS_SIGN_DATA: 
					signData(apdu); 
				return;
			
			default:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
		
	}

/*
 *  genKey: 00 C2 ID AL 02 K1 K2
 *  AL = 01 is RSA
 *  AL = 04 is ECDSA
 *  K1 K2 = 08 00 means a 2048-bit key (RSA)
 *  K1 K2 = 01 00 means a 256-bit key (ECDSA NIST P256 with SHA256)
 *  K1 K2 = 01 80 means a 384-bit key (ECDSA NIST P384 with SHA384) not supported	
 */
	private void generateRSAKey(APDU apdu) {

		apdu.setIncomingAndReceive();
		byte[] buffer = apdu.getBuffer();
		switch (buffer[ISO7816.OFFSET_P1]) {
			case (byte) keyALG_RSA: 
				// implement RSA

					//Display.display_message(DISPLAY_RSA_KEY_GEN_START);
         
					//APDU.waitExtension();
					//here probably "new" creates new object, but old is not cleaned,
					//that causes memory full and 6F00 response
					//RSAKey = new KeyPair(KeyPair.ALG_RSA, KeyBuilder.LENGTH_RSA_2048); //AM: moved to constructor
					RSAKey.genKeyPair();
					

					//Display.display_message(DISPLAY_RSA_KEY_GEN_END);
            break;
			case (byte) key_ALG_ECDSA: 
//				// implement ECDSA
					// implement ECDSA_SHA256
					
					//Display.display_message(DISPLAY_EC_KEY_GEN_START);
						
					ECPublicKey ecPubKey = (ECPublicKey) KeyBuilder.buildKey(
					KeyBuilder.TYPE_EC_FP_PUBLIC, (short) 256, false);

					ECPrivateKey ecPrivKey = (ECPrivateKey) KeyBuilder.buildKey(
					KeyBuilder.TYPE_EC_FP_PRIVATE, (short) 256, false);

					// set EC Domain Parameters
					ecPubKey.setFieldFP(SecP256r1_P, (short)0, (short)SecP256r1_P.length);
					ecPubKey.setA(SecP256r1_A, (short)0, (short)SecP256r1_A.length);
					ecPubKey.setB(SecP256r1_B, (short)0, (short)SecP256r1_B.length);
					ecPubKey.setG(SecP256r1_G, (short)0, (short)SecP256r1_G.length);
					ecPubKey.setK(SecP256r1_H);
					ecPubKey.setR(SecP256r1_N, (short)0, (short)SecP256r1_N.length);

					ecPrivKey.setFieldFP(SecP256r1_P, (short)0, (short)SecP256r1_P.length);
					ecPrivKey.setA(SecP256r1_A, (short)0, (short)SecP256r1_A.length);
					ecPrivKey.setB(SecP256r1_B, (short)0, (short)SecP256r1_B.length);
					ecPrivKey.setG(SecP256r1_G, (short)0, (short)SecP256r1_G.length);
					ecPrivKey.setK(SecP256r1_H);
					ecPrivKey.setR(SecP256r1_N, (short)0, (short)SecP256r1_N.length);

					ECKey = new KeyPair(ecPubKey, ecPrivKey);
					APDU.waitExtension();
					ECKey.genKeyPair();

					//probably it does not reach this point
				

					//Display.display_message(DISPLAY_RSA_KEY_GEN_END);
					
			break;
			default:
				ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
			}
	}

		private void getRSAPublicKey(APDU apdu) {

			apdu.setIncomingAndReceive();

			RSAPublicKey public_key;

			short publicKeySize;
			//FIXME: check if key is generated
			public_key = (RSAPublicKey) RSAKey.getPublic();
				
			publicKeySize = (short)((public_key.getSize())/(short)8);
			public_key.getModulus(in_buf, (short)0);
	        apdu.setOutgoing();
	        apdu.setOutgoingLength(publicKeySize);
	        apdu.sendBytesLong(in_buf, (short) 0, publicKeySize);				
	       
		}

		private void getECPublicKey(APDU apdu) {
			ECPublicKey ec_public_key;
			apdu.setIncomingAndReceive();
			short publicKeySize;
			//FIXME: check if key is generated
			ec_public_key = (ECPublicKey) ECKey.getPublic();
			
			publicKeySize = ec_public_key.getW(in_buf, (short)0);
	        apdu.setOutgoing();
	        apdu.setOutgoingLength(publicKeySize);
	        apdu.sendBytesLong(in_buf, (short) 0, publicKeySize);				
		}
		
		
	private void signData(APDU apdu) throws ISOException {

			byte[] buffer = apdu.getBuffer();
			short Lc = (short) (buffer[ISO7816.OFFSET_LC] & (byte)0xFF);
		    byte[] sign_buffer;
			short len;

			//FIXME: return if key is not generated
	        signatureRSA.init(RSAKey.getPrivate(), Signature.MODE_SIGN);
		    sign_buffer = new byte[signatureRSA.getLength()];

		    APDU.waitExtension();
		    len = signatureRSA.sign(buffer, ISO7816.OFFSET_CDATA, Lc, sign_buffer, (short) 0);

	    	apdu.setOutgoing();
	    	apdu.setOutgoingLength(len);
	    	apdu.sendBytesLong(sign_buffer, (short) 0, len);							 

		}
	
}