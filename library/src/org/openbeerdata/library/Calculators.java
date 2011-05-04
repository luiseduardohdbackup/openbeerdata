package org.openbeerdata.library;


public class Calculators {
	public static double roundToDecimals(double value, int numOfDecimals) {
		int temp = (int) ((value * Math.pow(10, numOfDecimals)));
		return (((double) temp) / Math.pow(10, numOfDecimals));
	}

	public static double boilingPointAtAltitude(double altitude) {
		double pressure;
		double boilingPoint;

		pressure = 29.921 * Math
				.pow((1 - 6.8753 * 0.000001 * altitude), 5.2559);
		boilingPoint = 49.161 * Math.log(pressure) + 44.932;

		// todo add C/F
		return roundToDecimals(boilingPoint, 1);
	}

	public static double hydrometerCorrection(double gravity, double temperature) {
		double correction = 1.313454 - (0.132674 * temperature)
				+ (0.002057793 * Math.pow(temperature, 2))
				- (0.000002627634 * Math.pow(temperature, 3));

		return roundToDecimals(gravity + (correction * 0.001), 3);
	}

	public static double initialInfusion(double ratio, double grainTemperature,
			double targetTemperature) {
		double thermoConstant = 0.192;
		// if (scale == 'C')
		// thermo_constant = 0.41;
		return roundToDecimals(targetTemperature + thermoConstant
				* (targetTemperature - grainTemperature) / ratio + 3, 1);
	}

	public static MashInfusionResults mashInfusion(double ratio,
			double startTemperature, double targetTemperature,
			double strikeTemperature, double grainWeight) {
		double thermoConstant = 0.2;

		double mashWater = grainWeight * ratio;

		MashInfusionResults results = new MashInfusionResults();

		results.volume = roundToDecimals((targetTemperature - startTemperature)
				* (thermoConstant * grainWeight + mashWater)
				/ (strikeTemperature - targetTemperature), 2);
		results.ratio = roundToDecimals((results.volume + mashWater)
				/ grainWeight, 2);

		return results;
	}

	public static GrainBedResults grainBedRect(double ratio,
			double grainWeight, double tunWidth, double tunLength) {
		double volume = grainWeight * (42 + (ratio - 1) * 32);

		GrainBedResults results = new GrainBedResults();
		results.mashVolume = roundToDecimals(volume / 128, 2);
		results.grainBedDepth = roundToDecimals(volume * 1.8
				/ (tunWidth * tunLength), 2);

		return results;
	}

	public static GrainBedResults grainBedCircle(double ratio,
			double grainWeight, double tunDiameter) {
		double volume = grainWeight * (42 + (ratio - 1) * 32);

		GrainBedResults results = new GrainBedResults();
		results.mashVolume = roundToDecimals(volume / 128, 2);
		results.grainBedDepth = roundToDecimals(
				volume * 1.8 / (Math.PI * Math.pow(tunDiameter / 2, 2)), 2);

		return results;
	}

	public static double AbsorptionLoss(double grainWeight, double constant) {
		return roundToDecimals(grainWeight * constant, 2);
	}
}
