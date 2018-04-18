This is an example python module that allows communication with PicoLabel v2 and v3 devices.
NFC connection is realised through pyscard library along with PicoLabel proprietary commands.
The module allows image conversion with two-bit palette and upload to PicoLabel device.
This module is cross-platform and works on Windows, Linux and Mac.

Installation instructions for Windows:

1. create virtualenv (optional)

python3.6 -m venv exampleApp
cd example
Scripts\activate.bat

2. install requirements

pip install -r requirements.txt

3. run

python test.py

External Requirements:

python (tested with 3.6)
pyscard (https://github.com/sekimura/pyscard)
Pillow (https://github.com/python-pillow/Pillow)