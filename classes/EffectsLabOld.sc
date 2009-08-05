EffectsLabOld {
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
	var buttonPower;
	// GUI objects for debugging.
	var buttonShowNodeTree;
	// Synths.
	var synthGroup;
	var inSynth;
	var effectSynths;

	*initClass {
	}
	
	*new {
		arg debugMode = false;
		^super.newCopyArgs(debugMode).init;
	}
	
	init {
		this.storeIOSynthDefs;
		this.storeEffectSynthDefs;
		
		effectClasses = EffectUnit.subclasses;
		effectNames = Array.new(effectClasses.size);
		effectClasses.do({ |class|
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
		// Add the required synthdef names here.
		effectNames = [\none, \flanger, \autowah1, \autowah2];
		
		// Effect synths.
		SynthDef(\echo, {
			arg in=0, out=0, period=0.25, feedback=0.5, repeats=1;
			var source, output;

			source = In.ar(in, 1);
			output = source;

			repeats.do({
				arg i;

				output = output + DelayN.ar(source, i*period, i*period, feedback**i);
			});

			ReplaceOut.ar(out, output);
		}).memStore;
		
		/////////////////
		
		SynthDef(\flanger, {
			arg in=0, out=0, depth=1, rate=0.25, minDelay=0.001, maxDelay=0.01;
			var source, delay, output, delayTime;
			
			source = In.ar(in, 1);
			// Delay LFO.
			delay = SinOsc.kr(rate, 0, (maxDelay-minDelay)/2, (maxDelay+minDelay)/2);
			
			// Flange.
			output = DelayN.ar(source, delay, delay, 1, source);
			
			ReplaceOut.ar(out, output);
		}).memStore;

		/////////////////

		SynthDef(\autowah1, {
			arg in=0, out=0, attack=0.05, release=0.2, thresh=0.1, i_minCutoff=500, i_maxCutoff=1500;
			var source, sourceAmp, output, trig, envelope, cutoff;
			
			// Get input amplitude.
			source = In.ar(in);
			sourceAmp = Amplitude.kr(source, 0.05, 1);
			trig = sourceAmp - thresh;

			// Trigger wah envelope if input amplitude > thresh.
			envelope = EnvGen.kr(Env.asr(attack, 1, release, 1,curve:'sine'), trig);
			cutoff = (i_maxCutoff - i_minCutoff) * envelope + i_minCutoff;
			
			// Apply the wah envelope and output.
			output = RLPF.ar(source, cutoff, 0.25);
			ReplaceOut.ar(out, output);
		}).memStore;
		
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
		win = Window.new("Effects Lab", Rect.new(100, 600, 500, 500));
		win.onClose = {
			this.freeAll;
			synthGroup.free;
		};
		
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

			if ( buttonPower.value == 1, {
				this.freeAll;
				this.createSynthChain;
			});
		};
		
		effectSelectors = Array.new(maxEffects);
		guiButtons = Array.new(maxEffects);
		maxEffects.do({
			arg i;
			
			var y = i*20+10;

			effectSelectors = effectSelectors.add ( PopUpMenu.new(win, Rect.new(160, y, 140, 20)) );
			effectSelectors[i].items = effectNames;
			effectSelectors[i].action = {
				arg menu;

				[menu.value, menu.item].postln;
			};
			
			guiButtons = guiButtons.add ( Button.new(win, Rect(300, y, 40, 20)) );
			guiButtons[i].states = ([
				["edit", Color.black, Color.gray]
			]);
			guiButtons[i].action = {};
		});

		outputSelector = PopUpMenu.new(win, Rect.new(350, 240, 140, 20));
		outputSelector.items = outBusNames;
		outputSelector.action = {
			arg menu;
			
			[menu.value, menu.item].postln;

			if ( buttonPower.value == 1, {
				this.freeAll;
				this.createSynthChain;
			});
		};
		
		buttonPower = Button.new(win, Rect.new(200, 450, 100, 20));
		buttonPower.states_([
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
		// Create input synth.
		inSynth = Synth.new(\input, [\in, inputSelector.value, \out, outputSelector.value], synthGroup, \addToHead);

		// Create effect synths.
		effectSynths = Array.new(maxEffects);
		
		maxEffects.do({
			arg i;
			
			if ( effectSelectors[i].item == \none,
			{
				effectSynths = effectSynths.add(nil);
			},
			{
				effectSynths = effectSynths.add(Synth.new(effectSelectors[i].item, [\in, outputSelector.value, \out, outputSelector.value], synthGroup, \addToTail));
			});
		});
	}
	
	freeAll
	{
		synthGroup.freeAll;
	}
}