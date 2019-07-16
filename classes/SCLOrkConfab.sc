SCLOrkConfab {
	classvar confabPid;
	classvar addSerial = 1;

	classvar confab;
	classvar assetAddedFunc;

	classvar addCallbackMap;

	*start { |
		confabBindPort = 4248,
		scBindPort = 4249,
		pathToConfabBinary = nil,
		pathToConfabDataDir = nil |

		confab = NetAddr.new("127.0.0.1", confabBindPort);
		addCallbackMap = Dictionary.new;

		SCLOrkConfab.prBindResponseMessages(scBindPort);
		SCLOrkConfab.prStartConfab;
	}

	*addAssetFile { | type, filePath, addCallback |
//		if (SCLOrkConfab.isConfabRunning.not, {
//			"confab not running".postln;
//			^nil;
//		});
		addCallbackMap.put(addSerial, addCallback);
		confab.sendMsg('/assetAddFile', type, addSerial, filePath);
		addSerial = addSerial + 1;
	}

	*addAssetString { | type, assetString, addCallback |
//		if (SCLOrkConfab.isConfabRunning.not, {
//			"confab not running".postln;
//			^nil;
//		});
		addCallbackMap.put(addSerial, addCallback);
		confab.sendMsg('/assetAddString', type, addSerial, assetString);
		addSerial = addSerial + 1;
	}

	*isConfabRunning {
		if (confabPid.notNil, {
			^confabPid.pidRunning;
		});
		^false;
	}

	*prBindResponseMessages { | recvPort |
		assetAddedFunc = OSCFunc.new({ | msg, time, addr |
			var serial = msg[1];
			var key = msg[2];
			var callback = addCallbackMap.at(serial);
			if (callback.notNil, {
				addCallbackMap.removeAt(serial);
				callback.value(key);
			}, {
				"confab got add callback on missing serial %".format(serial).postln;
			});
		},
		'/assetAdded',
		recvPort: recvPort);
	}

	*prStartConfab { |
		confabBindPort = 4248,
		scBindPort = 4249,
		pathToConfabBinary = nil,
		pathToConfabDataDir = nil |
		var command;
		// Check if confab binary already running.
		if (SCLOrkConfab.isConfabRunning, { ^true; });

		// Construct default path to binary if path not provided.
		if (pathToConfabBinary.isNil or: { pathToConfabDataDir.isNil }, {
			var quarkPath;
			// Quarks.quarkNameAsLocalPath seems to fail if the Quark is installed manually, so
			// instead we search the list of installed Quarks for the directory that refers to
			// SCLOrkTools.
			Quarks.installedPaths.do({ | path, index |
				if (path.contains("SCLOrkTools"), {
					quarkPath = path;
				});
			});

			if (pathToConfabBinary.isNil, {
				pathToConfabBinary = quarkPath ++ "/build/src/confab/confab";
			});
			if (pathToConfabDataDir.isNil, {
				pathToConfabDataDir = quarkPath ++ "/data/confab";
			});
		});

		// Check if a confab process is already running by checking for pid sentinel file.
		if (File.exists(pathToConfabDataDir ++ "/pid"), {
			confabPid = File.readAllString(pathToConfabDataDir ++ "/pid").asInteger;
			^confabPid.pidRunning;
		});

		// For now we assume both that the binary and database exist.
		command = [
			pathToConfabBinary,
			"--chatty=true",
			"--data_directory=" ++ pathToConfabDataDir
		];
		command.postln;
		confabPid = command.unixCmd({ | exitCode, exitPid |
			SCLOrkConfab.prOnConfabExit(exitCode, exitPid)
		});

		^confabPid.pidRunning;
	}


	*prStopConfab {
		if (confabPid.notNil, {
			["kill", "-SIGINT", confabPid.asString].unixCmd;
			confabPid = nil;
		});
	}

	*prOnConfabExit { | exitCode, exitPid |
		if (exitCode != 0, {
			"*** confab abnormal exit, pid: % exit: %".format(exitPid, exitCode).postln;
		});
		confabPid = nil;
	}
}