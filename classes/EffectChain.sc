EffectChain {
	var maxEffects, <inputBus, <outputBus;
	var <synthGroup;
	var <effectUnits;
	var inputSynth, outputSynth;
	var effectBus;
	
	*initClass {
	}
	
	*new {
		arg maxEffects, inputBus, outputBus;		
		^super.newCopyArgs(maxEffects, inputBus, outputBus).init;
	}
	
	init {
		effectUnits = Array.fill(maxEffects, nil);

		this.storeIOSynthDefs;
	}
	
	storeIOSynthDefs {
		// Audio input synth.
		SynthDef(\input, {
			arg in=0, out=0;
			var source;
			source = SoundIn.ar(in, 1);
			Out.ar(out, source);
		}).memStore;
		
		// Audio output synth.
		SynthDef(\output, {
			arg in=0, out=0;
			var source;
			source = In.ar(in, 1);
			Out.ar(out, source);
		}).memStore;
	}
	
	createEmptyChain {
		synthGroup = Group.new(Server.default, \addToHead);
		effectBus = Bus.audio(Server.default);
		inputSynth = Synth.new(\input, [\in, inputBus, \out, effectBus], synthGroup, \addToHead);
		outputSynth = Synth.new(\output, [\in, effectBus, \out, outputBus], synthGroup, \addToTail);
	}
	
	freeAll {
		// Use this when you're finished with the chain.
		synthGroup.freeAll;
		synthGroup.free;
		effectBus.free;
	}
	
	reset {
		// Use this to remove all effects from the chain and restart.
		this.freeAll;
		this.createEmptyChain;
	}
	
	inputBus_ {
		arg in;
		
		inputBus = in;
		inputSynth.set(\in, inputBus);
	}
	
	outputBus_ {
		arg out;
		
		outputBus = out;
		outputSynth.set(\out, outputBus);
	}
	
	addEffect {
		// Add a new EffectUnit at the given position. Free any synth already in this position.
		// You still need to call startEffect to start the actual synth.
		arg position, effectClass;
		var newEffectUnit, nextSynthPosition;
		
		newEffectUnit = effectClass.new;
		
		if ( effectUnits[position].notNil, {
			effectUnits[position].synth.free;
		});

		effectUnits[position] = newEffectUnit;
	}
	
	removeEffect {
		arg position;
		if ( effectUnits[position].notNil, {
			effectUnits[position].synth.free;
			effectUnits[position] = nil;	
		},{
			this.warnNoEffect(position);
		});
	}
	
	startEffect {
		arg position;
		var newSynth, nextSynthPosition;

		if ( effectUnits[position].notNil, {
			newSynth = effectUnits[position].createSynth;
			
			nextSynthPosition = this.getNextSynthPosition(position);
			
			if ( nextSynthPosition == -1, {
				// If the synth is to be created in the last position, place it before the output synth.
				"adding at end of chain".postln;
				Server.default.sendBundle(0.1, newSynth.addBeforeMsg(outputSynth, [\in, effectBus, \out, effectBus]));
			},{
				// Otherwise, place it before the next synth in the chain.
				"adding into chain".postln;
				Server.default.sendBundle(0.1, newSynth.addBeforeMsg(effectUnits[nextSynthPosition].synth, [\in, effectBus, \out, effectBus]));
			});
		},{
			this.warnNoEffect(position);
		});
	}
	
	stopEffect {
		arg position;
		if ( effectUnits[position].notNil, {
			effectUnits[position].synth.free;
		},{
			this.warnNoEffect(position);
		});
	}
		
	
	getSynth {
		arg position;
		if ( effectUnits[position].notNil, {
			^effectUnits[position].synth;
		},{
			^nil;
		});
	}
	
	getNextSynthPosition {
		arg position;
		// Find the index of the next synth in the chain.
		for ( (position+1), (maxEffects-1), {
			arg i;
			if ( effectUnits[i].notNil, { ^i });
		});
		
		// No synth was found in the chain past the supplied position.
		^(-1);
	}
	
	warnNoEffect {
		arg position;
		("EffectChain warning - no effect exists in position " ++ position).postln;
	}
}