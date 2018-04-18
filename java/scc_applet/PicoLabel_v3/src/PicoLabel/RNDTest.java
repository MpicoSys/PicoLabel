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
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.security.CryptoException;
import javacard.security.RandomData;

class RNDTest {
	private static short RANDOM_NUMBER_SIZE = (short)20;
	private static RandomData random_num;
	
	private static byte[] DISPLAY_RND_START = {(byte)'R',(byte)'N',(byte)'D',
		(byte)'-',(byte)'B',(byte)'G',(byte)'N'};
	private static byte[] DISPLAY_RND_END = {(byte)'R',(byte)'N',(byte)'D',
		(byte)'-',(byte)'E',(byte)'N',(byte)'D'};

	
	public RNDTest() {
		random_num = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);

	}
	
	public void process(APDU apdu) {
		generateRandomNum(apdu);
	}
	
	private void generateRandomNum(APDU apdu) {

        byte[] out_buf;
        out_buf = JCSystem.makeTransientByteArray(RANDOM_NUMBER_SIZE, JCSystem.CLEAR_ON_DESELECT);

        //Display.display_message(DISPLAY_RND_START);
     	
        try {
     		random_num.generateData(out_buf, (short)0, RANDOM_NUMBER_SIZE);
     	}
 		catch(CryptoException c)
 		{ 
 			short reason = c.getReason();
 			ISOException.throwIt(reason);
 		}

 		//Display.display_message(DISPLAY_RND_END);

 		apdu.setOutgoing();
    	apdu.setOutgoingLength(RANDOM_NUMBER_SIZE);
    	apdu.sendBytesLong(out_buf, (short) 0, RANDOM_NUMBER_SIZE);							 

	}
}