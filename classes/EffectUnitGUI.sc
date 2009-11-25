EffectUnitGUI {
	classvar <allInstances;
	var <effectUnit;
	var window;
	
	*initClass {
		allInstances = Array.new;
	}
	
	*findGUI {
		// If a GUI already exists for this EffectUnit, find it instead of creating a new one.
		arg effectUnit;
		var foundInstance;
		
		foundInstance = allInstances.detect {
			arg instance;
			instance.effectUnit === effectUnit;
		};
		
		if ( foundInstance.notNil, {
			foundInstance.front;
			^foundInstance;
		},{
			var newGUI;
			
			newGUI = this.new(effectUnit);
			allInstances = allInstances.add(newGUI);
			^newGUI;
		});
	}
	
	*new {
		arg effectUnit;
		^super.newCopyArgs(effectUnit).init;
	}
	
	init {
		this.makeGUI;
	}
	
	makeGUI {	
		var yPos=0, ySize;
		
		ySize = 20 * effectUnit.settings.size + 40;
		window = Window(effectUnit.class.effectName, Rect(100, 100, 200, ySize)).front;
		window.onClose_({ allInstances = allInstances.removing(this) });
		
		effectUnit.settings.keys.do {
			arg key;
			var setting, spec, slider;
			
			yPos = yPos + 20;
			
			setting = effectUnit.settings.at(key);
			spec = ControlSpec.new(setting.at(\min), setting.at(\max), setting.at(\warp));
			
			slider = EZSlider(window, Rect(20, yPos, 160, 20), key, spec);
			slider.value_(setting.at(\value));
			slider.action_({|slider| effectUnit.setControlValue(key, slider.value) });
		}
	}
	
	front {
		window.front;
	}
}