EffectsLab {
	// Data.
	var debugMode;
	var effectClassDict;
	var maxEffects = 8;
	var effectChain;
	// GUI objects.
	var win;
	var inputSelector, outputSelector;
	var effectSelectors;
	var guiButtons;
	var powerButtons;
	var mainPowerButton;
	// GUI objects for debugging.
	var buttonShowNodeTree;

	*initClass {
	}
	
	*new {
		arg debugMode = false;
		^super.newCopyArgs(debugMode).init;
	}
	
	init {
		var effectClasses;
		
		effectClassDict = IdentityDictionary.new;
		effectClasses = EffectUnit.subclasses;
		effectClasses.do {
			arg effectClass;
			effectClassDict.put(effectClass.effectName, effectClass);
			effectClass.storeSynthDef;
		};
		
		effectChain = EffectChain.new(maxEffects, 0, 0);
	
		this.createGUI;
	}

	createGUI {
		var inBusCount, outBusCount;
		var inBusNames, outBusNames;
		var effectNames;
		
		// Main window.
		win = Window.new("Effects Lab", Rect.new(100, 600, 500, 500));
		win.onClose = {
			if ( mainPowerButton.value == 1, {
				effectChain.freeAll;
			});
		};

		// Input and output bus selectors.
		inBusCount  = Server.local.options.numInputBusChannels;
		outBusCount = Server.local.options.numOutputBusChannels;
		
		inBusNames  = Array.new(inBusCount);
		outBusNames = Array.new(outBusCount);
		
		inBusCount.do({
			arg i;
			inBusNames = inBusNames.add ( "Input "+i );
		});
		
		outBusCount.do({
			arg i;
			outBusNames = outBusNames.add ( "Output "+i );
		});

		inputSelector = PopUpMenu.new(win, Rect.new(10, 240, 140, 20));
		inputSelector.items = inBusNames;
		inputSelector.action = {
			arg menu;

			effectChain.inputBus = menu.value;
		};

		outputSelector = PopUpMenu.new(win, Rect.new(350, 240, 140, 20));
		outputSelector.items = outBusNames;
		outputSelector.action = {
			arg menu;

			effectChain.outputBus = menu.value;
		};
		
		// Per-effect controls.
		effectSelectors = Array.new(maxEffects);
		powerButtons = Array.new(maxEffects);
		guiButtons = Array.new(maxEffects);
		
		effectNames = effectClassDict.keys.asArray.insert(0, \none);
		
		maxEffects.do({
			arg i;
			
			var y = i*20+10;

			effectSelectors = effectSelectors.add ( PopUpMenu.new(win, Rect.new(160, y, 120, 20)) );
			effectSelectors[i].items = effectNames;
			effectSelectors[i].action = {
				arg menu;
				var effectClass;
				
				effectClass = effectClassDict[menu.item];
				
				if ( effectClass.notNil, {
					effectChain.addEffect(i, effectClass);
					if ( mainPowerButton.value == 1 && powerButtons[i].value == 1, {
						effectChain.startEffect(i);
					});
				},{
					effectChain.removeEffect(i);
				});
			};
			
			powerButtons = powerButtons.add ( Button.new(win, Rect(280, y, 20, 20)) );
			powerButtons[i].states = [
				["0", Color.black, Color.gray],
				["1", Color.black, Color.red]
			];
			powerButtons[i].action = {
				var effectClass;
				effectClass = effectClassDict[effectSelectors[i].item];
				
				if ( effectClass.notNil, {
					if ( mainPowerButton.value == 1, {
						if ( powerButtons[i].value == 1, {
							effectChain.startEffect(i);
						},{
							effectChain.stopEffect(i);
						});
					});
				});
			};
			
			guiButtons = guiButtons.add ( Button.new(win, Rect(300, y, 40, 20)) );
			guiButtons[i].states = [
				["edit", Color.black, Color.gray]
			];
			guiButtons[i].action = {
				var effectUnit;
				
				effectUnit = effectChain.effectUnits[i];
				if ( effectUnit.notNil, {
					EffectUnitGUI.findGUI(effectUnit);
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
				effectChain.createEmptyChain;
				
				maxEffects.do {
					arg i;
					
					if ( powerButtons[i].value == 1, {
						effectChain.startEffect(i);
					});
				}
			},
			{
				effectChain.freeAll;
			});
				
		});
		
		// Debugging controls.
		if (debugMode, {
			buttonShowNodeTree = Button.new(win, Rect.new(300, 450, 100, 20));
			buttonShowNodeTree.states_([
				["Query node tree", Color.black, Color.gray]
			])
			.action_({
				effectChain.synthGroup.dumpTree(true);
			});
		});
		
		win.front;
	}
}