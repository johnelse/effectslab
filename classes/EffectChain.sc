EffectChain {
	var maxEffects, <inputBus, <outputBus;
	var <synthGroup;
	var <effectClasses;
	var <effectNames;
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
		this.storeIOSynthDefs;
		
		effectClasses = EffectUnit.subclasses;
		effectNames = Array.new(effectClasses.size);
		effectClasses.do({
			arg effectClass;
			effectClass.storeSynthDef;
			effectNames = effectNames.add(effectClass.effectName);
		});

		effectUnits = Array.fill(maxEffects, nil);
		synthGroup = Group.new(Server.default, \addToHead);
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
	
	activate {
		this.freeAll;
		
		effectBus = Bus.audio(Server.default);
		inputSynth = Synth.new(\input, [\in, inputBus, \out, effectBus], synthGroup, \addToHead);
		outputSynth = Synth.new(\output, [\in, effectBus, \out, outputBus], synthGroup, \addToTail);
	}
	
	addEffect {
		arg position, effectClass;
		var newEffectUnit, newSynth, nextSynthPosition;
		
		newEffectUnit = effectClass.new;
		newSynth = newEffectUnit.createSynth;
		
		if( effectUnits[position].notNil,
		{
			// Replace the synth currently in this position.
			Server.default.sendBundle(0.1, newSynth.addReplaceMsg(effectUnits[position].synth, [\in, effectBus, \out, effectBus]));
		},
		{
			nextSynthPosition = this.getNextSynthPosition(position).postln;
			
			if ( nextSynthPosition == -1,
			{
				// If the synth is to be created in the last position, place it before the output synth.
				"adding at end of chain".postln;
				Server.default.sendBundle(0.1, newSynth.addBeforeMsg(outputSynth, [\in, effectBus, \out, effectBus]));
			},
			{
				// Otherwise, place it before the next synth in the chain.
				"adding into chain".postln;
				Server.default.sendBundle(0.1, newSynth.addBeforeMsg(effectUnits[nextSynthPosition].synth, [\in, effectBus, \out, effectBus]));
			});
		});

		effectUnits[position] = newEffectUnit;
	}
	
	removeEffect {
		arg position;
		if ( effectUnits[position].notNil,
		{
			effectUnits[position].synth.free;
			effectUnits[position] = nil;	
		});
	}
	
	getSynth {
		arg position;
		^effectUnits[position].synth;
	}
	
	freeAll {
		synthGroup.freeAll;
		effectBus.free;
		effectUnits = Array.fill(maxEffects, nil);
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
}