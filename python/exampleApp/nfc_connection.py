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

#!/usr/bin/env python3
import json
import os, sys
import logging
import binascii
from operator import itemgetter

from smartcard.scard import *
from smartcard.CardType import AnyCardType
from smartcard.CardRequest import CardRequest
from smartcard.CardConnectionObserver import CardConnectionObserver
from smartcard.util import toHexString
import smartcard.System
import time

SLOW_SPEED = 0
MEDIUM_SPEED = 1
MAX_SPEED = 2
CARD_TIMEOUT_S = 5


def format_hex(msg):
    return ' '.join(msg[i:i + 2].decode('utf-8') for i in range(0, len(msg), 2))


class NFCConnection(object):
    supported_readers = ['SCM Microsystems Inc. SCL010 Contactless Reader 0',
                         'SCM Microsystems Inc. SCL011G Contactless Reader 0', 'OMNIKEY CardMan 5x21-CL 0']

    def __init__(self, field_control=False):
        self.readerString = ""
        self.hcard = None
        self.hcontext = None
        self.ntagtype = "NTAG_1KMEMORY"
        self.CARD_SENSE_TIMEOUT_S = 0.5
        self.CARD_TIMEOUT_S = 5

        if field_control:
            self.CONNECT_MODE = SCARD_SHARE_DIRECT
        else:
            self.CONNECT_MODE = SCARD_SHARE_SHARED

    def sc_connect(self, rdr=None):
        if rdr:
            self.readerString = rdr
        start = time.time()
        hresult, self.hcard, dwActiveProtocol = SCardConnect(self.hcontext, self.readerString, self.CONNECT_MODE,
                                                             SCARD_PROTOCOL_T0 | SCARD_PROTOCOL_T1)
        if hresult != 0:
            print('Unable to connect: ' + SCardGetErrorMessage(hresult))
            while hresult != 0 and (time.time() - start) < self.CARD_SENSE_TIMEOUT_S:
                time.sleep(0.05)
                hresult, self.hcard, dwActiveProtocol = SCardConnect(self.hcontext, self.readerString,
                                                                     self.CONNECT_MODE,
                                                                     SCARD_PROTOCOL_T0 | SCARD_PROTOCOL_T1)
            if hresult == 0:
                print('Connected after: ' + str(time.time() - start) + "s.")

        if hresult != 0:
            return False

        print('Connected to: ' + self.readerString)
        return True

    def atr(self):
        hresult, reader, state, protocol, atr = SCardStatus(self.hcard)
        
        if hresult != 0:
            return 'failed to get status: ' + SCardGetErrorMessage(hresult)
        else:
            return toHexString(atr)

    def get_card_state(self):
        hresult, reader, state, protocol, atr = SCardStatus(self.hcard)
        if hresult != 0:
            return 'failed to get status: ' + SCardGetErrorMessage(hresult)
        return state

    def connect(self, reader):
        if reader:
            self.readerString = reader
        if self.hcontext is None:
            hresult, self.hcontext = SCardEstablishContext(SCARD_SCOPE_USER)

        if self.readerString == "":
            hresult, readers = SCardListReaders(self.hcontext, [])
            if hresult != SCARD_S_SUCCESS:
                return 'Failed to list readers:: ' + SCardGetErrorMessage(hresult)
            try:
                if len(readers) < 1:
                    return 'No smart card readers found'

                for r in readers:
                    r = str(r)
                    rc = self.sc_connect(r)
                    if rc:
                        self.atr()
                        return rc

                return False

            except:
                return False
        else:
            return self.sc_connect(reader)

    def detect_readers(self):
        if self.hcontext is None:
            hresult, self.hcontext = SCardEstablishContext(SCARD_SCOPE_USER)

        hresult, readers = SCardListReaders(self.hcontext, [])

        if hresult != SCARD_S_SUCCESS or len(readers) < 1:
            return False

        rsp = []

        for i, item in enumerate(readers):
            if item in self.supported_readers:
                rsp.append({'id': i, 'name': item, 'supported': 1})
            else:
                rsp.append({'id': i, 'name': item, 'supported': 0})

        return sorted(rsp, key=itemgetter('supported'), reverse=True)

    def avail_readers(self):
        r = self.detect_readers()
        if isinstance(r, list):
            return {'readers': r, 'default': r[0]['name']}
        else:
            return {'error': 'No readers found'}

    def default_reader(self):
        r = self.detect_readers()
        if isinstance(r, list):
            return r[0]['name']
        else:
            return json.dumps({'error': r})

    def transceive(self, capdu):
        start = time.time()
        result = 1
        response = []

        while result != 0 and (time.time() - start) < self.CARD_TIMEOUT_S:
            result, response = SCardTransmit(self.hcard, SCARD_PCI_T1, list(capdu))
            if result != 0:
                time.sleep(0.1)
            else:
                return response

        return response

