RingMod {
	var <input, <modFreq;
	var >output;
	
	modFreq_ { arg argModFreq;
		modFreq = argModFreq;
	}
	
	input_ { arg argInput;
		input = argInput;
	}
	
	output {
		output = input * SinOsc.ar(modFreq);
		^output;
	}
}