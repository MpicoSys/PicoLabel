if __name__ == '__main__':
	from exampleApp.image_convert import convert, prepare_binary
	from exampleApp.picolabel import PicoLabel
	imagecode = convert('test_image.png', size=(264, 176), bitcolor=2)
	binary_image = prepare_binary(imagecode)
	picolabel = PicoLabel()
	picolabel.connect()
	picolabel.authenticate()
	picolabel.upload_image_data(binary_image)
	picolabel.update_display()
