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

import base64

import io
from PIL import Image
from io import BytesIO


def convert_to_binary(image):

    imgbytes = list(image.tobytes())
    palette = {0: 3, 85: 2, 170: 1, 255: 0}

    i = 0

    for byte in imgbytes:
        imgbytes[i] = palette[byte]
        i += 1

    imgnewbytes0 = list(int(len(imgbytes) / 8) * b'\x00')
    imgnewbytes1 = list(int(len(imgbytes) / 8) * b'\x00')

    pointer = 0
    i = 0
    loop_len = len(imgbytes)
    while i < loop_len:
        temp = (((imgbytes[i + 0] & 1) << 7) & 0x80) | \
               (((imgbytes[i + 1] & 1) << 6) & 0x40) | \
               (((imgbytes[i + 2] & 1) << 5) & 0x20) | \
               (((imgbytes[i + 3] & 1) << 4) & 0x10) | \
               (((imgbytes[i + 4] & 1) << 3) & 0x08) | \
               (((imgbytes[i + 5] & 1) << 2) & 0x04) | \
               (((imgbytes[i + 6] & 1) << 1) & 0x02) | \
               (((imgbytes[i + 7] & 1) << 0) & 0x01)

        imgnewbytes0[pointer] = temp & 0xFF

        pointer += 1
        i += 8

    pointer = 0
    i = 0
    loop_len = len(imgbytes)
    while i < loop_len:
        temp = (((imgbytes[i + 0] & 2) << 6) & 0x80) | \
               (((imgbytes[i + 1] & 2) << 5) & 0x40) | \
               (((imgbytes[i + 2] & 2) << 4) & 0x20) | \
               (((imgbytes[i + 3] & 2) << 3) & 0x10) | \
               (((imgbytes[i + 4] & 2) << 2) & 0x08) | \
               (((imgbytes[i + 5] & 2) << 1) & 0x04) | \
               (((imgbytes[i + 6] & 2) << 0) & 0x02) | \
               (((imgbytes[i + 7] & 2) >> 1) & 0x01)

        imgnewbytes1[pointer] = temp & 0xFF

        pointer += 1
        i += 8

    return bytes(imgnewbytes1), bytes(imgnewbytes0)


def preview(size, bit, dither, imagecode):
    r = convert(None, size, bit, dither, imagecode)
    return {"previewImage": r}


def prepare_binary(imagecode):

    image_data = imagecode.replace('data:image/png;base64,', '')
    image = Image.open(io.BytesIO(base64.b64decode(image_data)))

    image_buffer = BytesIO()
    header = b"\x32\x01\x08\x00\xB0\x02\x00" + 9 * b"\x00"

    imgnewbytes1, imgnewbytes0 = convert_to_binary(image)

    image_buffer.write(header)
    image_buffer.write(imgnewbytes1)
    image_buffer.write(imgnewbytes0)
    image_buffer.seek(0)

    return image_buffer


def convert(filename, size=(264, 176), bitcolor=1, dithering=True, imagecode=False):
    dither = Image.FLOYDSTEINBERG if dithering else Image.NONE

    try:
        if imagecode:
            image_data = imagecode.replace('data:image/png;base64,', '')
            image = Image.open(io.BytesIO(base64.b64decode(image_data)))
            image = image.convert('RGB')

        else:
            image = Image.open(open(filename, 'rb'))

        background = Image.new('L', size, 255)
        image.thumbnail(size, Image.ANTIALIAS)

        xo = int((size[0] - image.size[0]) / 2)
        yo = int((size[1] - image.size[1]) / 2)

        if bitcolor == 1:

            image = image.convert("1", dither=dither)
            background.paste(image, (xo, yo))

        elif bitcolor == 2:

            image = image.convert("P", None, dither, Image.WEB, 4)

            palette_2bit = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85,
                            85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85,
                            85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85,
                            85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85,
                            85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85,
                            85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85,
                            85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85,
                            85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85,
                            85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170,
                            170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170,
                            170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170,
                            170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170,
                            170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170,
                            170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170,
                            170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170,
                            170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170,
                            170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170,
                            170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170,
                            170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170, 170,
                            170, 170, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                            255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255]

            background.paste(image, (xo, yo))
            background.putpalette(palette_2bit)

        image = background.convert('L')

        in_mem_file = io.BytesIO()
        image.save(in_mem_file, format="PNG")
        in_mem_file.seek(0)
        img_bytes = in_mem_file.read()
        result = 'data:image/png;base64,' + base64.b64encode(img_bytes).decode('ascii')
        return result

    except IOError as e:
        return {"error": e}

