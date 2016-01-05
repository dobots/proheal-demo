# ProHeal Demo

Demonstrates to use of indoor localization and the crownstone cloud api to provide localized authentication.

# Dependencies

Depends on the following two library projects which need to be cloned and imported seperately:

* [bluenet-lib-android](https://github.com/dobots/bluenet-lib-android)

	This library provides the interface to the crownstone over bluetooth low energy and can be installed like:

		cd path/to/project/location
		git clone https://github.com/dobots/bluenet-lib-android.git bluenet

* [crownstone-loopback-sdk](https://github.com/dobots/crownstone-loopback-sdk)

	This library provides the interface to the crownstone cloud API and can be installed like:

		cd path/to/project/location
		git clone https://github.com/dobots/crownstone-loopback-sdk.git crownstone-loopback-sdk

Alternatively you can also clone them somewhere else and use symlinks, but make sure the name of the folder will be bluenet and crownstone-loopback-sdk respectively.

