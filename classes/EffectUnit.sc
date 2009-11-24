EffectUnit {	// Abstract class
	var <synth;
	var win;

	*new {
		^super.new.init;
	}
	
	init {
	}
	
	createSynth {
		synth = Synth.basicNew(this.class.effectName);
		
		^synth;
	}
}

AutoWahEffect : EffectUnit {
	*initClass {
	}
	
	*effectName {
		^\autoWah;
	}
	
	*storeSynthDef {
		SynthDef(this.effectName, {
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
			output = Resonz.ar(LPF.ar(source, cutoff), cutoff, 0.25, 10);
			ReplaceOut.ar(out, output);
		}).memStore;
	}
	
	init {
		^super.init;
	}
	
	showGUI {
		// Load a GUI specific to this effect which can change the synth parameters.
	}
}

ClipEffect : EffectUnit {
	*initClass {
	}
	
	*effectName {
		^\clip;
	}
	
	*storeSynthDef {
		SynthDef(this.effectName, {
			arg in=0, out=0, level=1;
			var source, output;
			
			source = In.ar(in, 1);
			
			output = Clip.ar(source, -1*level, level) / level;
			ReplaceOut.ar(out, output);
		}).memStore;
	}
	
	init {
		^super.init;
	}
	
	showGUI {
		var levelSlider;
		
		win = Window.new(this.class.effectName, Rect(100, 100, 300, 300));
		win.front;
		
		levelSlider = Slider.new(win, Rect(50, 20, 200, 20));
		levelSlider.action = {
			synth.set(\level, levelSlider.value);
		};
	}
}

DistortEffect : EffectUnit {
	*initClass {
	}
	
	*effectName {
		^\distort;
	}
	
	*storeSynthDef {
		SynthDef(this.effectName, {
			arg in=0, out=0, amount=0.99;
			var source, coeff, output;
			
			source = In.ar(in);
			coeff = 2*amount/(1-amount);
			output = (1 + coeff) * source / (1 + (coeff * source.abs));
			ReplaceOut.ar(out, output);
		}).memStore;
	}
	
	init {
		^super.init;
	}

	showGUI {
		var levelSlider;
		
		win = Window.new(this.class.effectName, Rect(100, 100, 300, 300));
		win.front;
		
		levelSlider = Slider.new(win, Rect(50, 20, 200, 20));
		levelSlider.action = {
			synth.set(\amount, levelSlider.value);
		};
	}
}

FlangeEffect : EffectUnit {
	*initClass {
	}
	
	*effectName {
		^\flanger;
	}
	
	*storeSynthDef {
		SynthDef(this.effectName, {
			arg in=0, out=0, depth=1, rate=0.25, delay=0.001, width=0.01;
			var source, delayLFO, output, delayTime;
			
			source = In.ar(in, 1);
			// Delay LFO.
			delayLFO = SinOsc.kr(rate, 0, width/2, width/2+delay);
			
			// Flange. LPF gets rid of some high frequency noise.
			output = LPF.ar(DelayN.ar(source*depth, delayLFO, delayLFO, 1, source), 2000);
			
			ReplaceOut.ar(out, output);
		}).memStore;
	}
	
	init {
		^super.init;
	}

	showGUI {
		win = Window.new(this.class.effectName, Rect(100, 100, 300, 300));
		win.front;
	}
}

FoldEffect : EffectUnit {
	*initClass {
	}
	
	*effectName {
		^\fold;
	}
	
	*storeSynthDef {
		SynthDef(this.effectName, {
			arg in=0, out=0, level=1;
			var source, output;
			
			source = In.ar(in, 1);
			
			output = Fold.ar(source, -1*level, level) / level;
			ReplaceOut.ar(out, output);
		}).memStore;
	}
	
	init {
		^super.init;
	}
	
	showGUI {
		var levelSlider;
		
		win = Window.new(this.class.effectName, Rect(100, 100, 300, 300));
		win.front;
		
		levelSlider = Slider.new(win, Rect(50, 20, 200, 20));
		levelSlider.action = {
			synth.set(\level, levelSlider.value);
		};
	}
}

ReverbEffect : EffectUnit {
	*initClass {
	}
	
	*effectName {
		^\reverb;
	}
	
	*storeSynthDef {
		SynthDef(this.effectName, {
			arg in=0, out=0, mix=0.5, room=0.1, damp=0.1;
			var source, output;
			
			source = In.ar(in, 1);
			
			output = FreeVerb.ar(source, mix, room, damp);
			ReplaceOut.ar(out, output);
		}).memStore;
	}
	
	init {
		^super.init;
	}
	
	showGUI {
	}
}

WrapEffect : EffectUnit {
	*initClass {
	}
	
	*effectName {
		^\wrap;
	}
	
	*storeSynthDef {
		SynthDef(this.effectName, {
			arg in=0, out=0, level=1;
			var source, output;
			
			source = In.ar(in, 1);
			
			output = Wrap.ar(source, -1*level, level) / level;
			ReplaceOut.ar(out, output);
		}).memStore;
	}
	
	init {
		^super.init;
	}
	
	showGUI {
		var levelSlider;
		
		win = Window.new(this.class.effectName, Rect(100, 100, 300, 300));
		win.front;
		
		levelSlider = Slider.new(win, Rect(50, 20, 200, 20));
		levelSlider.action = {
			synth.set(\level, levelSlider.value);
		};
	}
}