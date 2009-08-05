EffectsLab {
	// Data.
	var debugMode;
	var effectClasses;
	var effectNames;
	var maxEffects = 8;
	// GUI objects.
	var win;
	var inputSelector, outputSelector;
	var effectSelectors;
	var guiButtons;
	var powerButtons;
	var mainPowerButton;
	// GUI objects for debugging.
	var buttonShowNodeTree;
	// Synths.
	var synthGroup;
	var inputSynth;
	var effectObjects;
	var effectSynths;

	*initClass {
	}
	
	*new {
		arg debugMode = false;
		^super.newCopyArgs(debugMode).init;
	}
	
	init {
		this.storeIOSynthDefs;
		
		effectClasses = EffectUnit.subclasses;
		effectNames = Array.new(effectClasses.size);
		effectClasses.do({ |class|
			class.storeSynthDef;
			effectNames = effectNames.add(class.effectName);
		});
	
		synthGroup = Group.new(Server.default, \addToHead);
	
		this.createGUI;
	}
	
	storeIOSynthDefs {
		// Audio input synth.
		SynthDef(\input, {
			arg in=0, out=0;
			
			var source;
			source = SoundIn.ar(in, 1);
			Out.ar(out, source);
		}).memStore;
	}

	storeEffectSynthDefs {
		// Put this somewhere.
		SynthDef(\autowah2, {
			arg in=0, out=0, i_minCutoff=500, i_maxCutoff=1500;
			var source, sourceAmp, cutoff, output;
			
			// Get input amplitude.
			source = In.ar(in);
			sourceAmp = Amplitude.kr(source, 0.05, 0.05);
			
			cutoff = (i_maxCutoff - i_minCutoff) * Clip.kr(sourceAmp*10, 0, 1) + i_minCutoff;
			
			output = RLPF.ar(source, cutoff, 0.25);	
			ReplaceOut.ar(out, output);
		}).memStore;
	}

	createGUI {
		var inBusCount, outBusCount;
		var inBusNames, outBusNames;
		
		// Main window.
		win = Window.new("Effects Lab", Rect.new(100, 600, 500, 500));
		win.onClose = {
			this.freeAll;
			synthGroup.free;
		};

		// Input and output bus selectors.
		inBusCount  = Server.local.options.numInputBusChannels;
		outBusCount = Server.local.options.numOutputBusChannels;
		
		inBusNames  = Array.new(inBusCount);
		outBusNames = Array.new(outBusCount);
		
		inBusCount.do({
			arg i;
			inBusNames = inBusNames.add ( "Input"+i );
		});
		
		outBusCount.do({
			arg i;
			outBusNames = outBusNames.add ( "Output"+i );
		});

		inputSelector = PopUpMenu.new(win, Rect.new(10, 240, 140, 20));
		inputSelector.items = inBusNames;
		inputSelector.action = {
			arg menu;

			[menu.value, menu.item].postln;

			if ( mainPowerButton.value == 1, {
				this.freeAll;
				this.createSynthChain;
			});
		};

		outputSelector = PopUpMenu.new(win, Rect.new(350, 240, 140, 20));
		outputSelector.items = outBusNames;
		outputSelector.action = {
			arg menu;
			
			[menu.value, menu.item].postln;

			if ( mainPowerButton.value == 1, {
				this.freeAll;
				this.createSynthChain;
			});
		};
		
		// Per-effect controls.
		effectSelectors = Array.new(maxEffects);
		powerButtons = Array.new(maxEffects);
		guiButtons = Array.new(maxEffects);
		
		maxEffects.do({
			arg i;
			
			var y = i*20+10;

			effectSelectors = effectSelectors.add ( PopUpMenu.new(win, Rect.new(160, y, 120, 20)) );
			effectSelectors[i].items = effectNames;
			effectSelectors[i].action = {
				arg menu;

				[menu.value, menu.item].postln;
			};
			
			powerButtons = powerButtons.add ( Button.new(win, Rect(280, y, 20, 20)) );
			powerButtons[i].states = [
				["0", Color.black, Color.gray],
				["1", Color.black, Color.red]
			];
			powerButtons[i].action = {};
			
			guiButtons = guiButtons.add ( Button.new(win, Rect(300, y, 40, 20)) );
			guiButtons[i].states = [
				["edit", Color.black, Color.gray]
			];
			guiButtons[i].action = {
				if ( effectObjects[i].notNil, {
					effectObjects[i].showGUI;
				});
			};
		});

		// Main power button.
		mainPowerButton = Button.new(win, Rect.new(200, 450, 100, 20));
		mainPowerButton.states_([
			["Off", Color.black, Color.gray],
			["On", Color.black, Color.red]
		])
		.action_({
			arg butt;
			
			if ( butt.value == 1, {
				this.createSynthChain;
			},
			{
				this.freeAll;
			});
				
		});
		
		// Debugging controls.
		if (debugMode, {
			buttonShowNodeTree = Button.new(win, Rect.new(300, 450, 100, 20));
			buttonShowNodeTree.states_([
				["Query node tree", Color.black, Color.gray]
			])
			.action_({
				synthGroup.dumpTree(true);
			});
		});
		
		win.front;
	}
	
	front {
		if ( win.notNil, {
			if ( win.isClosed == false, {
				win.front;
			});
		});
	}
	
	createSynthChain
	{
		var synth;
		// Create input synth.
		inputSynth = Synth.new(\input, [\in, inputSelector.value, \out, outputSelector.value], synthGroup, \addToHead);

		// Create effect synths.
		effectObjects = Array.new(maxEffects);
		
		maxEffects.do({
			arg i;
			
			if ( powerButtons[i].value == 1,
			{
				effectObjects = effectObjects.add(effectClasses[effectSelectors[i].value].new);
				
				synth = effectObjects[i].createSynth;
				Server.default.sendBundle(nil, synth.addToTailMsg(synthGroup, [\in, outputSelector.value, \out, outputSelector.value]));
				synth.set(\in, outputSelector.value, \out, outputSelector.value);
				effectSynths = effectSynths.add(synth);
			},
			{
				effectObjects = effectObjects.add(nil);
				effectSynths = effectSynths.add(nil);
			});
		});
	}
	
	freeAll
	{
		synthGroup.freeAll;
	}
}