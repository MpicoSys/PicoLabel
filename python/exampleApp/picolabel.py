#
# Copyright (c) 2018, MpicoSys Solutions B.V.
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without modification,
# are permitted provided that the following conditions are met:
# 
# 1. Redistributions of source code must retain the above copyright notice, 
#  this list of conditions and the following disclaimer.
#  
# 2. Redistributions in binary form must reproduce the above copyright notice, 
#  this list of conditions and the following disclaimer in the documentation 
#  and/or other materials provided with the distribution.
#  
# 3. Neither the name of the copyright holder nor the names of its contributors
#  may be used to endorse or promote products derived from this software without
#  specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
# GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
# HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT 
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT 
# OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

import sys
from backend.nfc_connection import NFCConnection

P1_NO_SAVE_TO_FLASH = (0 << 1)
P1_ANSWER = (0 << 2)
P1_NO_ANSWER = (1 << 2)
P1_DS_QUALITY_THROUGH_BLACK = 0x10
P1_DISPLAY_SHEME = 0xF0

P1_DS_NORMAL_THROUGH_WHITE_BLACK = 0x70
P1_DS_NORMAL_THROUGH_BLACK_WHITE = 0x80

P2_BORDER_WHITE = 0 << 3
P2_NO_COMPRESION = (0 << 4)
P2_COMPRESION_RLE7 = (1 << 4)
P2_NUMBER_SLOT = (0x7 << 0)
P2_BORDER_CONTROL = (1 << 3)
P2_CONSTANT_VALUE = (0x00)

BUFFER_SIZE = 246

EP_CLA = 0xB5
EP_CMD_IMAGE_SEND = 0x20
EP_CMD_DISP_UPDATE = 0x24
EP_CMD_GET_SYSTEM_INFO  = 0x31

#V1
GFX_RESET_COMMAND_BUFFER = 0x00
GFX_ADD_COMMAND_DATA	 = 0x01
GFX_ADD_COMMAND_AND_EXEC = 0x02
GFX_FONT_SAVE			 = 0x03
EP_CMD_CREATE_IMAGE_DATA = 0x41
P1_DS_FAST_DIRECT_INV = 0x90
P2_BORDER_CONTROL = (1<<3)

resolve_answer = {True: P1_ANSWER, False: P1_NO_ANSWER}


class NoReaderException(Exception):
    def __init__(self, msg):
        self.msg = msg


class NoCardException(Exception):

    def __init__(self, msg):
        self.msg = msg


class CardInterruptedException(Exception):
    def __init__(self, msg):
        self.msg = msg


class PicoLabel:
    def __init__(self, reader=None):
        self.connection = NFCConnection()
        self.reader = reader if reader else self.connection.default_reader()

    @staticmethod
    def toHEXTAB(data):
        if sys.version_info[0] < 3:
            return [ord(a) for a in data]
        else:
            return [a for a in data]

    def connect(self):
        if not self.reader:
            return {'error': 'Reader not found'}, False

        if not self.connection.connect(self.reader):
            return {'error': 'Impossible to initialize the card.'}, False

        return {'info': 'Card connected'}, True

    def authenticate(self):
        AID = "\x01\x02\x03\x04\x05"

        epd = [0, 0xA4, 4, 00] + [len(AID)] + [ord(i) for i in AID] + [0]
        response = self.connection.transceive(epd)
        
        epd = [0x12, 0, 0, 0]
        response = self.connection.transceive(epd)

        return response
    
    def upload_image_data(self, data, slot=0, buffer_size=246, bytes_to_send=-1, answer=False, compressed=False, version=2):

        compression = P2_COMPRESION_RLE7 if compressed else P2_NO_COMPRESION

        while True:
            imgdata = data.read(buffer_size)
            if len(imgdata) == 0:
                break
            if bytes_to_send == 0:
                break
            if bytes_to_send > 0:
                if len(imgdata) > bytes_to_send:
                    imgdata = imgdata[:bytes_to_send]
                bytes_to_send -= len(imgdata)

            response = self.upload_image_data_packet(imgdata, slot, answer, compression)

        if not answer:
            response = [0x90, 0x00]
        return response

    def upload_image_data_packet(self, data, slot=0, answer=False, compression=P2_NO_COMPRESION, P1=P1_NO_SAVE_TO_FLASH):
        epd = [EP_CLA, EP_CMD_IMAGE_SEND, P1 | resolve_answer[answer], compression | P2_NUMBER_SLOT & slot, len(data)] + self.toHEXTAB(data)
        response = self.connection.transceive(epd)
        return response
    
    def update_display(self, drivescheme=P1_DS_QUALITY_THROUGH_BLACK, slot=0, border=P2_BORDER_WHITE, answer=True):
        epd = [EP_CLA, EP_CMD_DISP_UPDATE, resolve_answer[answer] | P1_DISPLAY_SHEME & drivescheme, P2_NUMBER_SLOT & slot | P2_BORDER_CONTROL & border, 00]
        response = self.connection.transceive(epd)
        return response

    def get_system_info(self, answer=P1_ANSWER, LE=16):
        epd = [EP_CLA, EP_CMD_GET_SYSTEM_INFO, answer, P2_CONSTANT_VALUE, LE]
        response = self.connection.transceive(epd)
        return response
